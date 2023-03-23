package com.slinet.flserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File

@SpringBootApplication
class FlserverApplication

fun main(args: Array<String>) {
    runApplication<FlserverApplication>(*args)

    //创建模型存储目录
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

    //连接数据库
    DeviceManager.connectDatabase()

    //启动Socket服务器
    val socketServer = SocketServer()
    socketServer.startServer(12345)     //可以在这里修改端口号
    ModelAggregation.createModel()
    ModelAggregation.startWebUI()
}
