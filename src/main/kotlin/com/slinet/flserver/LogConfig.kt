package com.slinet.flserver

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

//与Utils类中的log函数配合，用于更方便美观地显示日志，这样在其他地方想要显示日志的时候只需要调用Utils.log函数即可
@Configuration
class LogConfig {
    private val log = LoggerFactory.getLogger(LogConfig::class.java)

    fun log(msg: String) {
        log.info(msg)
    }
}