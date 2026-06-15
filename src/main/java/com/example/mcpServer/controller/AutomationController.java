package com.example.mcpServer.controller;

import com.example.mcpServer.service.AutomationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AutomationController {

    private final AutomationService service;

    public AutomationController(AutomationService service) {
        this.service = service;
    }

    @PostMapping("/fix")
    public String fixRepo() {
        return service.runAutomation();
    }
}