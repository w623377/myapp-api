package com.imooc.controller;

import com.imooc.grace.result.GraceJSONResult;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
@Api(tags = "测试的接口")
public class testcontroller {

    @Value("${nacos.counts}")
    private Integer nacosCounts;

    @GetMapping("nacosCounts")
    public Object nacosCounts() {
        return GraceJSONResult.ok("nacosCounts的数值为：" + nacosCounts);
    }

    @RequestMapping("/hello")
    public Object hello() {
        return GraceJSONResult.ok("hello world");
    }
}
