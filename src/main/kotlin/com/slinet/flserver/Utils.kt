package com.slinet.flserver

import java.util.*

object Utils {
    private val logConfig = LogConfig()

    val encodeBash64 = fun(str: String): String {
        val bytes = str.toByteArray()
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(bytes)
    }

    val decodeBash64 = fun(str: String): String {
        val decoder = Base64.getDecoder()
        return String(decoder.decode(str))
    }

    fun log(msg: String) {
        logConfig.log(msg)
    }
}