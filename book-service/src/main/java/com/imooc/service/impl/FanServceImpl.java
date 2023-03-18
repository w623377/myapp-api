package com.imooc.service.impl;

import com.github.pagehelper.PageHelper;
import com.imooc.base.BaseInfoProperties;
import com.imooc.base.RabbitMQConfig;
import com.imooc.enums.MessageEnum;
import com.imooc.enums.YesOrNo;
import com.imooc.mapper.FansMapper;
import com.imooc.mapper.FansMapperCustom;
import com.imooc.mo.MessageMO;
import com.imooc.pojo.Fans;
import com.imooc.service.FanService;
import com.imooc.service.MessageService;
import com.imooc.service.VlogService;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.FansVO;
import com.imooc.vo.VlogerVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.n3r.idworker.Sid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class FanServceImpl extends BaseInfoProperties implements FanService {

    @Autowired
    private FansMapper fansMapper;

    @Autowired
    private Sid sid;

    @Autowired
    private FansMapperCustom fansMapperCustom;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Transactional
    @Override
    public void doFollow(String myId, String vlogerId) {
        String fid = sid.nextShort();

        Fans fans = new Fans();
        fans.setId(fid);
        fans.setFanId(myId);
        fans.setVlogerId(vlogerId);

        // 判断对方是否关注我，如果关注我，那么双方都要互为朋友关系
        Fans vloger = queryFansRelationship(vlogerId, myId);
        if (vloger != null) {
            fans.setIsFanFriendOfMine(YesOrNo.YES.type);// 我关注的人是我的朋友
            vloger.setIsFanFriendOfMine(YesOrNo.YES.type);// 我的朋友关注了我
            fansMapper.updateByPrimaryKeySelective(vloger);// 更新粉丝的朋友关系
        } else {
            fans.setIsFanFriendOfMine(YesOrNo.NO.type);// 我关注的人不是我的朋友
        }
        fansMapper.insert(fans);

        //我的关注数加一，博主的粉丝数加一
        redis.increment(REDIS_MY_FOLLOWS_COUNTS+":"+myId,1);
        redis.increment(REDIS_MY_FANS_COUNTS+":"+vlogerId,1);

        // 我和博主的关联关系，依赖redis，不要存储数据库，避免db的性能瓶颈
        redis.set(REDIS_FANS_AND_VLOGGER_RELATIONSHIP + ":" + myId + ":" + vlogerId, "1");

        // 系统消息：关注
//        messageService.createMsg(myId,vlogerId, MessageEnum.FOLLOW_YOU.type, null);
        MessageMO messageMO = new MessageMO();
        messageMO.setFromUserId(myId);
        messageMO.setToUserId(vlogerId);
        // 优化：使用mq异步解耦
        log.info("发送消息：{}", JsonUtils.objectToJson(messageMO));
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_MSG,
                "sys.msg." + MessageEnum.FOLLOW_YOU.enValue,
                JsonUtils.objectToJson(messageMO));

    }

    @Transactional
    @Override
    public void doCancel(String myId, String vlogerId) {
        // 判断我们是否朋友关系，如果是，则需要取消双方的关系
        Fans fan = queryFansRelationship(myId, vlogerId);
        if (fan != null && fan.getIsFanFriendOfMine() == YesOrNo.YES.type) {
            // 抹除双方的朋友关系，自己的关系删除即可
            Fans pendingFan = queryFansRelationship(vlogerId, myId);
            pendingFan.setIsFanFriendOfMine(YesOrNo.NO.type);
            fansMapper.updateByPrimaryKeySelective(pendingFan);
        }
        // 删除自己的关注关联表记录
        fansMapper.delete(fan);

        // 博主的粉丝-1，我的关注-1
        redis.decrement(REDIS_MY_FOLLOWS_COUNTS + ":" + myId, 1);
        redis.decrement(REDIS_MY_FANS_COUNTS + ":" + vlogerId, 1);

        // 我和博主的关联关系，依赖redis，不要存储数据库，避免db的性能瓶颈
        redis.del(REDIS_FANS_AND_VLOGGER_RELATIONSHIP + ":" + myId + ":" + vlogerId);


    }

    //查询我是否关注了某个博主
    @Override
    public boolean queryDoIFollowVloger(String myId, String vlogerId) {
        String s = redis.get(REDIS_FANS_AND_VLOGGER_RELATIONSHIP + ":" + myId + ":" + vlogerId);
        return StringUtils.isNotBlank(s);
    }

    @Override
    public PagedGridResult queryMyFollows(String myId, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);
        HashMap<String, Object> map = new HashMap<>();
        map.put("myId", myId);
        List<VlogerVO> myFollows = fansMapperCustom.queryMyFollows(map);
        return setterPagedGrid(myFollows,page);
    }

    @Override
    public PagedGridResult queryMyFans(String myId,
                                       Integer page,
                                       Integer pageSize) {

        /**
         * <判断粉丝是否是我的朋友（互粉互关）>
         * 普通做法：
         * 多表关联+嵌套关联查询，这样会违反多表关联的规范，不可取，高并发下回出现性能问题
         *
         * 常规做法：
         * 1. 避免过多的表关联查询，先查询我的粉丝列表，获得fansList
         * 2. 判断粉丝关注我，并且我也关注粉丝 -> 循环fansList，获得每一个粉丝，再去数据库查询我是否关注他
         * 3. 如果我也关注他（粉丝），说明，我俩互为朋友关系（互关互粉），则标记flag为true，否则false
         *
         * 高端做法：
         * 1. 关注/取关的时候，关联关系保存在redis中，不要依赖数据库
         * 2. 数据库查询后，直接循环查询redis，避免第二次循环查询数据库的尴尬局面
         */

        Map<String, Object> map = new HashMap<>();
        map.put("myId", myId);
        PageHelper.startPage(page, pageSize);

        List<FansVO> list = fansMapperCustom.queryMyFans(map);
        for (FansVO f : list) {
            String relationship = redis.get(REDIS_FANS_AND_VLOGGER_RELATIONSHIP + ":" + myId + ":" + f.getFanId());
            if (StringUtils.isNotBlank(relationship) && relationship.equalsIgnoreCase("1")) {
                f.setFriend(true);
            }
        }
        return setterPagedGrid(list, page);
    }

    //判断是否已经关注, 查询粉丝关系表, 如果有记录则已经关注, 没有则未关注, 有记录则返回粉丝对象
    private Fans queryFansRelationship(String fanId, String vlogerId) {

        Example example = new Example(Fans.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("vlogerId", vlogerId);
        criteria.andEqualTo("fanId", fanId);

        List list =  fansMapper.selectByExample(example);

        Fans fan = null;
        if (list != null && list.size() > 0 && !list.isEmpty()) {
            fan = (Fans)list.get(0);
        }

        return fan;

    }




}
