package com.imooc.service.impl;

import com.github.pagehelper.PageHelper;
import com.imooc.base.BaseInfoProperties;
import com.imooc.base.RabbitMQConfig;
import com.imooc.bo.CommentBO;
import com.imooc.enums.MessageEnum;
import com.imooc.enums.YesOrNo;
import com.imooc.mapper.CommentMapper;
import com.imooc.mapper.CommentMapperCustom;
import com.imooc.mo.MessageMO;
import com.imooc.pojo.Comment;
import com.imooc.pojo.Vlog;
import com.imooc.service.CommentService;
import com.imooc.service.MessageService;
import com.imooc.service.UserService;
import com.imooc.service.VlogService;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.CommentVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.n3r.idworker.Sid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CommenServiceImpl extends BaseInfoProperties implements CommentService {

    @Autowired
    private CommentMapper   commentMapper;

    @Autowired
    private CommentMapperCustom    commentMapperCustom;

    @Autowired
    private MessageService messageService;

    @Autowired
    private VlogService vlogService;

    @Autowired
    private Sid  sid;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Override
    public CommentVO createComment(CommentBO commentBO) {
        String commentId = sid.nextShort();

        Comment comment = new Comment();
        comment.setId(commentId);

        comment.setVlogId(commentBO.getVlogId());
        comment.setVlogerId(commentBO.getVlogerId());

        comment.setCommentUserId(commentBO.getCommentUserId());
        comment.setFatherCommentId(commentBO.getFatherCommentId());
        comment.setContent(commentBO.getContent());

        comment.setLikeCounts(0);
        comment.setCreateTime(new Date());

        commentMapper.insert(comment);
        // redis操作放在service中，评论总数的累加
        redis.increment(REDIS_VLOG_COMMENT_COUNTS + ":" + commentBO.getVlogId(), 1);

        // 留言后的最新评论需要返回给前端进行展示
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(comment, commentVO);

        // 系统消息：评论/回复
        Vlog vlog = vlogService.getVlog(commentBO.getVlogId());
        Map msgContent = new HashMap();
        msgContent.put("vlogId", vlog.getId());
        msgContent.put("vlogCover", vlog.getCover());
        msgContent.put("commentId", commentId);
        msgContent.put("commentContent", commentBO.getContent());

//        Integer type = MessageEnum.COMMENT_VLOG.type;
        String routeType = MessageEnum.COMMENT_VLOG.enValue;
        if (StringUtils.isNotBlank(commentBO.getFatherCommentId()) &&
                !commentBO.getFatherCommentId().equalsIgnoreCase("0") ) {
//            type = MessageEnum.REPLY_YOU.type;
            routeType = MessageEnum.REPLY_YOU.enValue;
        }

//        messageService.createMsg(commentBO.getCommentUserId(),
//                commentBO.getVlogerId(),
//                type,
//                msgContent);

        // MQ异步解耦
        MessageMO messageMO = new MessageMO();
        messageMO.setFromUserId(commentBO.getCommentUserId());
        messageMO.setToUserId(commentBO.getVlogerId());
        messageMO.setMsgContent(msgContent);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_MSG,
                "sys.msg." + routeType,
                JsonUtils.objectToJson(messageMO));

        return commentVO;

    }

    @Override
    public Comment getComment(String commentId) {
        return commentMapper.selectByPrimaryKey(commentId);
    }

    @Override
    public PagedGridResult queryVlogCommnets(String vlogId,
                                             String userId,
                                             Integer page,
                                             Integer pageSize) {

        PageHelper.startPage(page, pageSize);
        HashMap<String, Object> map = new HashMap<>();
        map.put("vlogId", vlogId);
        List<CommentVO> list = commentMapperCustom.getCommentList(map);

        for (CommentVO commentVO : list) {
            String commentId = commentVO.getCommentId();
            //获取评论的点赞数
            String countsStr = redis.get(REDIS_VLOG_COMMENT_LIKED_COUNTS + ":" + commentId);
            Integer counts = 0;
            if (StringUtils.isNotBlank(countsStr)) {
                counts = Integer.valueOf(countsStr);
            }
            commentVO.setLikeCounts(counts);

            // 判断当前用户是否点赞
            String doLike = redis.get(REDIS_USER_LIKE_COMMENT + ":" +userId + ":" + commentId);
            if (StringUtils.isNotBlank(doLike) && doLike.equalsIgnoreCase("1")) {
                commentVO.setIsLike(YesOrNo.YES.type);
            }
        }
            return setterPagedGrid(list, page);
        }


    @Override
    public void deleteCommnets(String commentUserId, String commetnId, String vlogId) {
        Comment pendingDelete = new Comment();
        pendingDelete.setId(commetnId);
        pendingDelete.setCommentUserId(commentUserId);
        commentMapper.delete(pendingDelete);

        // redis操作放在service中，评论总数的累减
        redis.decrement(REDIS_VLOG_COMMENT_COUNTS + ":" + vlogId, 1);
    }
}
