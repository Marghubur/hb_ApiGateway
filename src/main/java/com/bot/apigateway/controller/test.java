package com.bot.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/gateway/")
public class test {
    @GetMapping("get")
    public String get(){
        return  "Successfully";
    }
}




