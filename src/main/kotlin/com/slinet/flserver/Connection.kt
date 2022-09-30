package com.slinet.flserver

import java.text.SimpleDateFormat
import java.util.*

class Connection(
    val socketThread: SocketServer.SocketThread,
    val ipAddress: String,
    var deviceName: String = "",
    var deviceModel: String = ""
) {
    lateinit var lastConnection: String

    init {
        touch()
    }

    fun touch(deviceName: String = "", deviceModel: String = "") {
        lastConnection = with(SimpleDateFormat()) {
            applyPattern("yyyy-MM-dd HH:mm:ss")
            format(Date())
        }
        if (deviceName.isNotEmpty()) {
            this.deviceName = deviceName
            this.deviceModel = deviceModel
        }
    }
}