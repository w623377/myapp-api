package com.imooc.service;

import com.imooc.bo.CommentBO;
import com.imooc.pojo.Comment;
import com.imooc.pojo.Vlog;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.CommentVO;


public interface CommentService {
    public CommentVO createComment(CommentBO commentBO);

    public PagedGridResult queryVlogCommnets(String vlogId,
                                             String userId,
                                             Integer page,
                                             Integer pageSize);

    public void deleteCommnets(String commentUserId,//评论人的id
                               String commetnId,//评论的id
                               String vlogId);//视频的id


    /**
     * 根据主键查询comment
     */
    public Comment getComment(String commentId);
}
