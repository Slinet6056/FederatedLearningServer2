package com.slinet.flserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File

@SpringBootApplication
class FlserverApplication

fun main(args: Array<String>) {
    runApplication<FlserverApplication>(*args)

    val path = File("res/model")
    if (path.isDirectory) {
        Utils.log("Resource directory already exists")
    } else {
        if (path.mkdirs()) {
            Utils.log("Resource directory created successfully")
        } else {
            Utils.log("Resource directory creation failed")
        }
    }

    val socketServer = SocketServer()
    socketServer.startServer(12345)
    ModelAggregation.createModel()
    ModelAggregation.startWebUI()
}
