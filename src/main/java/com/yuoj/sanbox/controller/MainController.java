package com.yuoj.sanbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 李京霖
 * @version 2024/3/3 20:13 1.0
 */

@RestController
@RequestMapping("/sandbox")
public class MainController {


    @GetMapping("/health")
    public String healthCheck() {
        return "Ok";
    }
}
