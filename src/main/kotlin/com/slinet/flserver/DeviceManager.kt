package com.slinet.flserver

object DeviceManager {
    class Device(
        var fingerprint: String = "",
        var connectionTime: Int = 0,
        var uploadNum: Int = 0,
        var contribution: Double = 0.0
    )

    private val deviceList = ArrayList<Device>()

    fun receive(fingerprint: String) {
        val device = findDevice(fingerprint)
        device.uploadNum++
        Utils.log("Device: ${device.fingerprint}, uploadNum: ${device.uploadNum}")
    }

    fun touch(fingerprint: String) {
        val device = findDevice(fingerprint)
        device.connectionTime++
        Utils.log("Device: ${device.fingerprint}, connectionTime: ${device.connectionTime}")
    }

    private fun findDevice(fingerprint: String): Device {
        for (device in deviceList) {
            if (device.fingerprint == fingerprint) {
                return device
            }
        }
        val device = Device(fingerprint)
        deviceList.add(device)
        return device
    }
}