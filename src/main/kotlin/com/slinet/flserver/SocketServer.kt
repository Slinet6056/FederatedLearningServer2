package com.slinet.flserver

import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import kotlin.concurrent.thread

//Socket服务器
class SocketServer {
    private var mainThread: Thread? = null
    private var checkConnectionThread = CheckConnection()
    private var server: ServerSocket? = null
    private var running = false
    private var sendFilePath: String? = null
    private val ipAddressReceiveFile = ArrayList<String>()
    private val ipAddressSendFile = ArrayList<String>()
    var autoMode = true

    //用于存储客户端连接信息的列表
    companion object {
        val connectionList = ArrayList<Connection>()
    }

    //启动Socket服务器
    fun startServer(port: Int) {
        if (mainThread != null) {
            Utils.log("Server has already started")
            return
        }
        try {
            server = ServerSocket(port)
        } catch (e: Exception) {
            Utils.log(e.message.toString())
            return
        }
        Utils.log("Server started on port $port")
        running = true

        mainThread = Thread {
            while (running) {
                try {
                    Utils.log("New socket waiting for client")
                    val socket = server!!.accept()

                    //根据客户端IP地址判断是接收文件还是发送文件，还是新的客户端连接
                    when (val ipAddress = with(socket.remoteSocketAddress.toString()) { substring(1, indexOf(':')) }) {
                        in ipAddressReceiveFile -> {
                            SocketThread(socket, 1).start()
                            ipAddressReceiveFile.remove(ipAddress)
                        }

                        in ipAddressSendFile -> {
                            SocketThread(socket, 2).start()
                            ipAddressSendFile.remove(ipAddress)
                        }

                        else -> {
                            SocketThread(socket).apply {
                                val connection = Connection(this, ipAddress)
                                connectionList.add(connection)
                                this.connection = connection
                                start()
                            }
                        }
                    }
                    Thread.sleep(50)
                } catch (e: Exception) {
                    Utils.log(e.message.toString())
                }
            }
        }
        mainThread!!.start()
        checkConnectionThread.start()
    }

    //停止Socket服务器，没用到，现在关闭服务器是直接关闭程序
    //其实应该是要先通知客户端，并且删除res/model中的模型文件的
    fun stopServer() {
        if (mainThread == null) {
            Utils.log("Server has already stopped")
            return
        }
        running = false
        try {
            server?.close()
            Utils.log("Sever stopped")
        } catch (e: Exception) {
            Utils.log(e.message.toString())
        }
        mainThread = null
    }

    //Socket连接，每个连接都在一个单独的线程中运行
    //type=0表示新的客户端连接，type=1表示接收文件，type=2表示发送文件
    inner class SocketThread(private val socket: Socket, private val type: Int = 0) : Thread() {

        var connection: Connection? = null
        var ipAddress: String = with(socket.remoteSocketAddress.toString()) { substring(1, indexOf(':')) }
        var output: PrintWriter? = null

        override fun run() {
            when (type) {
                //新的客户端连接
                0 -> try {
                    Utils.log("Socket get connected from $ipAddress")
                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                    output = PrintWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
                    var stringData: String?
                    while (true) {
                        stringData = input.readLine()
                        if (stringData == null) continue
                        try {
                            Utils.log("Received from $ipAddress: $stringData")

                            //解析JSON数据，根据statusCode的值判断是否准备接收文件
                            val json = JSONObject(stringData)
                            when (json.getInt("statusCode")) {
                                -1 -> break
                                0 -> {
                                    val deviceName = json.getString("deviceName")
                                    val deviceFingerprint = json.getString("deviceFingerprint")
                                    checkConnectionThread.responseIp.add(ipAddress)
                                    connection?.touch(deviceName, deviceFingerprint)
                                    DeviceManager.checkDevice(deviceFingerprint, deviceName)
                                }

                                1 -> {
                                    val trainingDuration = json.getDouble("trainingDuration")
                                    val deviceName = json.getString("deviceName")
                                    val deviceFingerprint = json.getString("deviceFingerprint")
                                    DeviceManager.receive(deviceName, deviceFingerprint, trainingDuration)
                                    ipAddressReceiveFile.add(ipAddress)
                                    connection?.touch()
                                    Utils.log("Waiting to receive file from $ipAddress")
                                }
                            }
                        } catch (e: Exception) {
                            Utils.log(e.message.toString())
                        }
                    }
                    input.close()
                    output?.close()
                    socket.close()
                    Utils.log("$ipAddress disconnected")
                } catch (e: Exception) {
                    Utils.log(e.message.toString())
                }

                //接收文件，接收到文件后会自动聚合模型并发送给客户端
                1 -> try {
                    Utils.log("Receive socket get connected from $ipAddress")
                    val inputStream = socket.getInputStream()
                    val filePath = "res/model/received${Instant.now().epochSecond}.zip"
                    val file = File(filePath)
                    val fileOutputStream = FileOutputStream(file, false)
                    val buffer = ByteArray(1024)
                    var size: Int
                    while (inputStream.read(buffer).also { size = it } != -1) {
                        fileOutputStream.write(buffer, 0, size)
                    }
                    fileOutputStream.close()
                    inputStream.close()
                    ModelAggregation.filePathList.add(filePath)
                    Utils.log("Received file from $ipAddress, socket closed")

                    connection?.touch()

                    //聚合模型并发送给客户端
                    if (autoMode) {
                        ModelAggregation.aggregation(3, 0.3)
                        sendFile("res/model/trained_model.zip")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                //发送文件
                2 -> try {
                    Utils.log("Send socket get connected from $ipAddress")
                    if (sendFilePath == null) return
                    val outputData = socket.getOutputStream()
                    val file = File(sendFilePath!!)
                    val fileInputStream = FileInputStream(file)
                    var size: Int
                    val buffer = ByteArray(1024)
                    while (fileInputStream.read(buffer, 0, 1024).also { size = it } != -1) {
                        outputData.write(buffer, 0, size)
                    }
                    fileInputStream.close()
                    outputData.close()
                    Utils.log("Sent file to $ipAddress, socket closed")

                    connection?.touch()
                } catch (e: Exception) {
                    Utils.log(e.message.toString())
                }
            }
        }
    }

    //请求客户端接收文件
    fun sendFile(filePath: String) {
        sendFilePath = filePath
        thread {
            for (connection in connectionList) {
                val socket = connection.socketThread
                try {
                    val msg = """{"statusCode":1}"""
                    socket.output?.println(msg)
                    ipAddressSendFile.add(socket.ipAddress)
                    Utils.log("Request to send file to ${socket.ipAddress}")
                } catch (e: Exception) {
                    Utils.log(e.message.toString())
                }
            }
        }
    }

    //检查客户端是否还在线，30秒后没有响应则认为客户端已经断开连接
    inner class CheckConnection : Thread() {
        val responseIp = ArrayList<String>()
        override fun run() {
            while (running) {
                responseIp.clear()
                val connectionList = ArrayList<Connection>(SocketServer.connectionList)
                for (connection in connectionList) {
                    val socket = connection.socketThread
                    try {
                        val msg = """{"statusCode":0}"""
                        socket.output?.println(msg)
                        Utils.log("Check connection to ${socket.ipAddress}")
                    } catch (e: Exception) {
                        Utils.log(e.message.toString())
                    }
                }
                sleep(30000)
                for (connection in connectionList) {
                    if (connection.ipAddress !in responseIp) {
                        Utils.log("${connection.ipAddress} disconnected")
                        SocketServer.connectionList.remove(connection)
                    }
                }
            }
        }
    }
}