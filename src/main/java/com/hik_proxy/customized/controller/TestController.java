package com.hik_proxy.customized.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.google.gson.JsonObject;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/test")
@RestController
public class TestController {
    @PostMapping("/callback")
    public void downloadVideo(@Valid @RequestBody JsonObject jObj) {
        log.info("callback:{}", JSON.toJSONString(jObj, JSONWriter.Feature.PrettyFormat));
    }


}
