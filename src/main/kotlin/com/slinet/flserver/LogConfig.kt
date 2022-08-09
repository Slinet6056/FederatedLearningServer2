package com.slinet.flserver

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
class LogConfig {
    private val log = LoggerFactory.getLogger(LogConfig::class.java)

    fun log(msg: String) {
        log.info(msg)
    }
}