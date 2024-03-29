package com.slinet.flserver

import java.sql.Connection
import java.sql.DriverManager

//用于设备连接信息数据库的管理
object DeviceManager {

    //一个数据类，用于存储设备的详细信息
    data class Detail(
        val id: Int,
        val deviceName: String,
        val fingerprint: String,
        val trainingTimes: Int,
        val averageDuration: Double,
        val totalDuration: Double
    )

    private var connection: Connection? = null

    //连接数据库，如果数据库不存在则创建
    fun connectDatabase() {
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:res/database/main.sqlite3")

            val statement = connection!!.createStatement()
            try {
                statement.executeQuery("SELECT * FROM device;")
                Utils.log("Table already exists, skip creation")
            } catch (e: Exception) {
                Utils.log("Table does not exist, automatically creating")
                val sql = """
                    CREATE TABLE device (
                        id               INTEGER PRIMARY KEY ASC AUTOINCREMENT
                                                 UNIQUE
                                                 NOT NULL,
                        device_name      TEXT,
                        fingerprint      TEXT    UNIQUE
                                                 NOT NULL,
                        training_times   INT     NOT NULL
                                                 DEFAULT (0),
                        average_duration REAL,
                        total_duration   REAL    NOT NULL
                                                 DEFAULT (0.0) 
                    );
                """.trimIndent()
                statement.executeUpdate(sql)
                Utils.log("Table created successfully")
            } finally {
                statement.close()
            }
        } catch (e: Exception) {
            Utils.log(e.message.toString())
        }
        Utils.log("Opened database successfully")
    }

    //接收到设备训练后的模型，更新数据库中训练次数与时长信息
    fun receive(deviceName: String, fingerprint: String, duration: Double) {
        if (duration == -1.0) return
        checkDevice(fingerprint, deviceName)
        try {
            val statement = connection!!.createStatement()
            val resultSet = statement.executeQuery("SELECT * FROM device where fingerprint = \'$fingerprint\'")
            var trainingTimes = resultSet.getInt("training_times")
            var totalDuration = resultSet.getDouble("total_duration")
            trainingTimes++
            totalDuration += duration
            statement.executeUpdate("UPDATE device set training_times = $trainingTimes where fingerprint = '$fingerprint'")
            statement.executeUpdate("UPDATE device set average_duration = ${1.0 * totalDuration / trainingTimes} where fingerprint = '$fingerprint'")
            statement.executeUpdate("UPDATE device set total_duration = $totalDuration where fingerprint = '$fingerprint'")
            statement.close()
            Utils.log("Successfully update device $fingerprint in database")
        } catch (e: Exception) {
            Utils.log(e.message.toString())
        }
    }

    //获取数据库中设备的详细信息
    fun getDetails(fingerprint: String): Detail? {
        if (!checkDevice(fingerprint)) return null
        val statement = connection!!.createStatement()
        val resultSet = statement.executeQuery("SELECT * FROM device where fingerprint = \'$fingerprint\'")
        return with(resultSet) {
            Detail(
                getInt("id"),
                getString("device_name"),
                getString("fingerprint"),
                getInt("training_times"),
                getDouble("average_duration"),
                getDouble("total_duration")
            )
        }
    }

    //检查设备是否已经存在于数据库中，如果不存在则添加
    fun checkDevice(fingerprint: String, deviceName: String = ""): Boolean {
        try {
            val statement = connection!!.createStatement()
            val resultSet = statement.executeQuery("SELECT * FROM device where fingerprint = '$fingerprint'")
            var rowCnt = 0
            while (resultSet.next()) rowCnt++
            if (rowCnt == 0) {
                statement.executeUpdate("INSERT INTO device (device_name, fingerprint) VALUES ('$deviceName', '$fingerprint')")
                Utils.log("Device $fingerprint does not exist in database, added")
                return false
            }
        } catch (e: Exception) {
            Utils.log(e.message.toString())
        }
        return true
    }

    //关闭数据库连接，没有用到捏
    fun closeDatabase() {
        connection?.close()
        Utils.log("Database closed")
    }
}