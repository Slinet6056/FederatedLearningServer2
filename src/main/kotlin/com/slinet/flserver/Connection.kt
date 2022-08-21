package com.slinet.flserver

import java.text.SimpleDateFormat
import java.util.*

class Connection(val socketThread: SocketServer.SocketThread, val ipAddress: String) {
    lateinit var lastConnection: String

    init {
        touch()
    }

    fun touch() {
        lastConnection = with(SimpleDateFormat()) {
            applyPattern("yyyy-MM-dd HH:mm:ss")
            format(Date())
        }
    }
}