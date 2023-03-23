package com.slinet.flserver

import java.text.SimpleDateFormat
import java.util.*

//用于记录客户端连接信息的类，每个实例对应一个客户端
class Connection(
    val socketThread: SocketServer.SocketThread,
    val ipAddress: String,
    var deviceName: String = "",
    var deviceFingerprint: String = ""
) {
    lateinit var lastConnection: String

    init {
        touch()
    }

    //更新最后连接时间
    fun touch(deviceName: String = "", deviceFingerprint: String = "") {
        lastConnection = with(SimpleDateFormat()) {
            applyPattern("yyyy-MM-dd HH:mm:ss")
            format(Date())
        }
        if (deviceName.isNotEmpty()) {
            this.deviceName = deviceName
            this.deviceFingerprint = deviceFingerprint
        }
    }
}