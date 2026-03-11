package com.example.android.network.signaling

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class ConnectionState { Disconnected, Connecting, Connected, Error }

class SignalingClient(
    private val serverAddress: String,
    private val sessionToken: String,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableSharedFlow<SignalMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<SignalMessage> = _messages

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var autoReconnect = true
    private var retryDelay = INITIAL_RETRY_DELAY

    fun connect() {
        if (_connectionState.value == ConnectionState.Connecting) return
        autoReconnect = true
        retryDelay = INITIAL_RETRY_DELAY
        openWebSocket()
    }

    fun send(message: SignalMessage) {
        webSocket?.send(message.toJson())
    }

    fun disconnect() {
        autoReconnect = false
        webSocket?.close(NORMAL_CLOSURE, null)
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun reconnect() {
        webSocket?.close(NORMAL_CLOSURE, null)
        webSocket = null
        retryDelay = INITIAL_RETRY_DELAY
        openWebSocket()
    }

    private fun openWebSocket() {
        _connectionState.value = ConnectionState.Connecting

        val url = buildString {
            append("ws://")
            append(serverAddress.removePrefix("http://").removePrefix("https://").trimEnd('/'))
            append("/ws?token=")
            append(sessionToken)
        }

        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, Listener())
    }

    private fun scheduleReconnect() {
        if (!autoReconnect) return
        val currentDelay = retryDelay
        retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY)
        scope.launch {
            delay(currentDelay)
            if (autoReconnect) openWebSocket()
        }
    }

    private inner class Listener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.value = ConnectionState.Connected
            retryDelay = INITIAL_RETRY_DELAY
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val message = SignalMessage.fromJson(text)
                _messages.tryEmit(message)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse signal message", e)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSURE, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            this@SignalingClient.webSocket = null
            _connectionState.value = ConnectionState.Disconnected
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "WebSocket failure", t)
            this@SignalingClient.webSocket = null
            _connectionState.value = ConnectionState.Error
            scheduleReconnect()
        }
    }

    companion object {
        private const val TAG = "SignalingClient"
        private const val NORMAL_CLOSURE = 1000
        private const val INITIAL_RETRY_DELAY = 1_000L
        private const val MAX_RETRY_DELAY = 30_000L
    }
}
