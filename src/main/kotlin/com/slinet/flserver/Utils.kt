package com.slinet.flserver

object Utils {
    private val logConfig = LogConfig()

    fun log(msg: String) {
        logConfig.log(msg)
    }
}