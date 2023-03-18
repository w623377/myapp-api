package com.imooc.controller;

import com.imooc.base.BaseInfoProperties;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.mo.MessageMO;
import com.imooc.pojo.Users;
import com.imooc.service.FanService;
import com.imooc.service.MessageService;
import com.imooc.service.UserService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "MessageController 粉丝相关业务功能的接口")
@RequestMapping("msg")
@RestController
public class MessageController extends BaseInfoProperties {

    @Autowired
    private MessageService messageService;



    @GetMapping("list")
    public GraceJSONResult follow(@RequestParam String userId,
                                  @RequestParam Integer page,
                                  @RequestParam Integer pageSize) {

        // mongodb 从0分页，区别于数据库
        if (page == null) {
            page = COMMON_START_PAGE_ZERO;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        List<MessageMO> list = messageService.queryList(userId, page, pageSize);
        return GraceJSONResult.ok(list);
    }

}
