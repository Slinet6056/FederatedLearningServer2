package com.slinet.flserver

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class HtmlController {
    @RequestMapping("info")
    fun info(model: Model): String {
        model.addAttribute("connectionList", SocketServer.connectionList)
        return "info"
    }
}