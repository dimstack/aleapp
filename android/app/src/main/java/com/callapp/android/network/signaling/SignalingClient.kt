package com.callapp.android.network.signaling

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

enum class ConnectionState { Disconnected, Connecting, Connected, Error }

class SignalingClient(
    private val serverAddress: String,
    @Volatile var sessionToken: String,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val initialRetryDelayMillis: Long = INITIAL_RETRY_DELAY,
    private val maxRetryDelayMillis: Long = MAX_RETRY_DELAY,
    private val maxReconnectAttempts: Int = Int.MAX_VALUE,
    private val onReconnectScheduled: (delayMillis: Long, attempt: Int) -> Unit = { _, _ -> },
) {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableSharedFlow<SignalMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<SignalMessage> = _messages

    private var webSocket: WebSocket? = null
    private var autoReconnect = true
    private var retryDelay = initialRetryDelayMillis
    private var reconnectAttempts = 0
    private val pendingMessages = ConcurrentLinkedQueue<String>()

    fun connect() {
        if (_connectionState.value == ConnectionState.Connecting) return
        autoReconnect = true
        retryDelay = initialRetryDelayMillis
        reconnectAttempts = 0
        openWebSocket()
    }

    fun send(message: SignalMessage) {
        val payload = message.toJson()
        val sent = webSocket?.send(payload) == true
        if (sent) return

        pendingMessages += payload
        if (_connectionState.value == ConnectionState.Disconnected || _connectionState.value == ConnectionState.Error) {
            connect()
        }
    }

    fun disconnect() {
        autoReconnect = false
        scope.coroutineContext.cancelChildren()
        pendingMessages.clear()
        webSocket?.close(NORMAL_CLOSURE, null)
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /** Полное уничтожение — после вызова объект нельзя переиспользовать. */
    fun destroy() {
        disconnect()
        scope.cancel()
    }

    fun reconnect() {
        webSocket?.close(NORMAL_CLOSURE, null)
        webSocket = null
        retryDelay = initialRetryDelayMillis
        reconnectAttempts = 0
        openWebSocket()
    }

    private fun openWebSocket() {
        _connectionState.value = ConnectionState.Connecting

        val wsScheme = if (serverAddress.startsWith("https://")) "wss://" else "ws://"
        val url = buildString {
            append(wsScheme)
            append(serverAddress.removePrefix("http://").removePrefix("https://").trimEnd('/'))
            append("/ws?token=")
            append(sessionToken)
        }

        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, Listener())
    }

    private fun scheduleReconnect() {
        if (!autoReconnect) return
        if (reconnectAttempts >= maxReconnectAttempts) {
            _connectionState.value = ConnectionState.Disconnected
            return
        }
        reconnectAttempts += 1
        val currentDelay = retryDelay
        retryDelay = (retryDelay * 2).coerceAtMost(maxRetryDelayMillis)
        onReconnectScheduled(currentDelay, reconnectAttempts)
        scope.launch {
            delay(currentDelay)
            if (autoReconnect) openWebSocket()
        }
    }

    private inner class Listener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.value = ConnectionState.Connected
            flushPendingMessages(webSocket)
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

    private fun flushPendingMessages(webSocket: WebSocket) {
        while (true) {
            val payload = pendingMessages.poll() ?: return
            if (!webSocket.send(payload)) {
                pendingMessages.add(payload)
                return
            }
        }
    }

    companion object {
        private const val TAG = "SignalingClient"
        private const val NORMAL_CLOSURE = 1000
        private const val INITIAL_RETRY_DELAY = 1_000L
        private const val MAX_RETRY_DELAY = 30_000L
    }
}
