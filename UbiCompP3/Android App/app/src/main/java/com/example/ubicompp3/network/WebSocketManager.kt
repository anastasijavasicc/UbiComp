package com.example.ubicompp3.network

import okhttp3.*
import okio.ByteString

class WebSocketManager(
    private val url: String,
    private val onMessageReceived: (String) -> Unit,
    private val onStatusChanged: (Boolean) -> Unit
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onStatusChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessageReceived(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessageReceived(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onStatusChanged(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onStatusChanged(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onStatusChanged(false)
            }
        })
    }

    fun send(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "Closed by user")
        onStatusChanged(false)
    }
}