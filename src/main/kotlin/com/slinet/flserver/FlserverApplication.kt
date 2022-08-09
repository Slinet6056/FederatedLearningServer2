package com.slinet.flserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FlserverApplication

fun main(args: Array<String>) {
    runApplication<FlserverApplication>(*args)
    val socketServer = SocketServer()
    socketServer.startServer(12345)
    ModelAggregation.createModel()
    ModelAggregation.startWebUI()
}
