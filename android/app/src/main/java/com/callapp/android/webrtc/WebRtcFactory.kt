package com.callapp.android.webrtc

import android.content.Context
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory

/**
 * App-scoped singleton that owns [PeerConnectionFactory] and [EglBase].
 *
 * These resources are expensive to create and must NOT be disposed between calls,
 * because [PeerConnectionFactory.initialize] is a global static call that cannot
 * be safely re-invoked after [PeerConnectionFactory.dispose].
 *
 * Call [init] once from Application/Activity onCreate.
 */
object WebRtcFactory {

    val eglBase: EglBase by lazy { EglBase.create() }

    val factory: PeerConnectionFactory by lazy { createFactory() }

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
    }

    private fun createFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            )
            .createPeerConnectionFactory()
    }
}
