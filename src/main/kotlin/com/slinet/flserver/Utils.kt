package com.slinet.flserver

import java.util.*

object Utils {
    private val logConfig = LogConfig()

    //Base64编码
    val encodeBash64 = fun(str: String): String {
        val bytes = str.toByteArray()
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(bytes)
    }

    //Base64解码
    val decodeBash64 = fun(str: String): String {
        val decoder = Base64.getDecoder()
        return String(decoder.decode(str))
    }

    ////与LogConfig类配合，用于更方便美观地显示日志，这样在其他地方想要显示日志的时候只需要调用Utils.log函数即可
    fun log(msg: String) {
        logConfig.log(msg)
    }
}