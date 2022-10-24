package com.slinet.flserver

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HtmlController {
    @GetMapping("info")
    fun info(model: Model): String {
        model.addAttribute("deviceNumber", SocketServer.connectionList.size)
        model.addAttribute("connectionList", SocketServer.connectionList)
        model.addAttribute("encodeBash64", Utils.encodeBash64)
        return "info"
    }

    @GetMapping("detail")
    fun detail(model: Model, @RequestParam(name = "fingerprint") fingerprint: String): String {
        val detail = DeviceManager.getDetails(Utils.decodeBash64(fingerprint)) ?: return "error"
        model.addAttribute("id", detail.id)
        model.addAttribute("deviceName", detail.deviceName)
        model.addAttribute("fingerPrint", detail.fingerprint)
        model.addAttribute("trainingTimes", detail.trainingTimes)
        model.addAttribute("averageDuration", detail.averageDuration)
        model.addAttribute("totalDuration", detail.totalDuration)
        return "detail"
    }

    @GetMapping("error")
    fun error(): String {
        return "error"
    }
}