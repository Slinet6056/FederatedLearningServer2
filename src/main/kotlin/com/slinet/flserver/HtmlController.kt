package com.slinet.flserver

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HtmlController {

    //设备列表页面
    @GetMapping("info")
    fun info(model: Model): String {
        model.addAttribute("deviceNumber", SocketServer.connectionList.size)
        model.addAttribute("connectionList", SocketServer.connectionList)
        model.addAttribute("encodeBash64", Utils.encodeBash64)
        return "info"
    }

    //设备详情页面
    //这里的@RequestParam注解用于接收前端传来的参数，这里的参数名为fingerprint，前端传来的参数值为设备的fingerprint
    //由于fingerprint中可能包含特殊字符，所以传输前需要先进行Base64编码，接收后再进行解码
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