package com.imooc.service;

import com.imooc.mo.MessageMO;
import com.imooc.utils.PagedGridResult;

import java.util.List;
import java.util.Map;

public interface MessageService {

    /**
     * 新增消息
     */
    public void createMsg(String fromUserId,
                          String toUserId,
                          Integer type,
                          Map msgContent);

    /**
     * 查询消息列表
     */
    public List<MessageMO> queryList(String toUserId,
                                     Integer page,
                                     Integer pageSize);

}
