package com.imooc.controller;

import com.imooc.base.BaseInfoProperties;
import com.imooc.base.RabbitMQConfig;
import com.imooc.bo.CommentBO;
import com.imooc.enums.MessageEnum;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.mo.MessageMO;
import com.imooc.pojo.Comment;
import com.imooc.pojo.Vlog;
import com.imooc.service.CommentService;
import com.imooc.service.MessageService;
import com.imooc.service.VlogService;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.CommentVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Api(tags = "CommentController 评论相关业务功能的接口")
@RequestMapping("comment")
@RestController
public class CommentController extends BaseInfoProperties {

    @Autowired
    private CommentService commentService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private VlogService vlogService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("create")
    public GraceJSONResult create(@Valid @RequestBody CommentBO commentBO)throws Exception {
        CommentVO commentVO = commentService.createComment(commentBO);
        return GraceJSONResult.ok(commentVO);
    }

    @GetMapping("counts")
    public GraceJSONResult counts(@RequestParam String vlogId)throws Exception {
        String count = redis.get(REDIS_VLOG_COMMENT_COUNTS + ":" + vlogId);
        if (StringUtils.isBlank(count)) {
            count= "0";
        }
        return GraceJSONResult.ok(Integer.valueOf(count));
    }

    @GetMapping("list")
    public GraceJSONResult list(@RequestParam String vlogId,
                                @RequestParam(defaultValue = "") String userId,
                                @RequestParam Integer page,
                                @RequestParam Integer pageSize) {
        PagedGridResult pagedGridResult = commentService.queryVlogCommnets(vlogId, userId, page, pageSize);
        return GraceJSONResult.ok(pagedGridResult);
    }

    @DeleteMapping("delete")
    public GraceJSONResult delete(@RequestParam String commentUserId,
                                  @RequestParam String commentId,
                                  @RequestParam String vlogId) {
         commentService.deleteCommnets(commentUserId, commentId, vlogId);
        return GraceJSONResult.ok();
    }

    @PostMapping("like")
    public GraceJSONResult like(@RequestParam String commentId,
                                @RequestParam String userId) {
         redis.increment(REDIS_VLOG_COMMENT_LIKED_COUNTS + ":" + commentId, 1);// 点赞数+1
         redis.set(REDIS_USER_LIKE_COMMENT + ":" +userId + ":" + commentId, "1");// 记录用户点赞的评论

        Comment comment = commentService.getComment(commentId);
        // 系统消息：点赞评论
        Vlog vlog = vlogService.getVlog(comment.getVlogId());
        Map msgContent = new HashMap();
        msgContent.put("vlogId", vlog.getId());
        msgContent.put("vlogCover", vlog.getCover());
        msgContent.put("commentId", commentId);



//        messageService.createMsg(userId,
//                comment.getCommentUserId(),
//                MessageEnum.LIKE_COMMENT.type,
//                msgContent);

        // MQ异步解耦
        MessageMO messageMO = new MessageMO();
        messageMO.setFromUserId(userId);
        messageMO.setToUserId(comment.getCommentUserId());
        messageMO.setMsgContent(msgContent);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_MSG,
                "sys.msg."+MessageEnum.LIKE_COMMENT.enValue,
                JsonUtils.objectToJson(messageMO)
        );

        return GraceJSONResult.ok();
    }

    @PostMapping("unlike")
    public GraceJSONResult unlike(@RequestParam String commentId,
                                @RequestParam String userId) {
        redis.decrement(REDIS_VLOG_COMMENT_LIKED_COUNTS + ":" + commentId, 1);// 点赞数-1
        redis.del(REDIS_USER_LIKE_COMMENT + ":" +userId + ":" + commentId);//删除用户点赞信息
        return GraceJSONResult.ok();
    }
}
