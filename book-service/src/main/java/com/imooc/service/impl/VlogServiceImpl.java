package com.imooc.service.impl;

import com.github.pagehelper.PageHelper;
import com.imooc.base.BaseInfoProperties;
import com.imooc.base.RabbitMQConfig;
import com.imooc.bo.VlogBO;
import com.imooc.enums.MessageEnum;
import com.imooc.enums.YesOrNo;
import com.imooc.mapper.MyLikedVlogMapper;
import com.imooc.mapper.VlogMapper;
import com.imooc.mapper.VlogMapperCustom;
import com.imooc.mo.MessageMO;
import com.imooc.pojo.MyLikedVlog;
import com.imooc.pojo.Vlog;
import com.imooc.service.FanService;
import com.imooc.service.MessageService;
import com.imooc.service.QiniuService;
import com.imooc.service.VlogService;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.IndexVlogVO;
import com.imooc.vo.IndexVlogVO2;
import com.imooc.vo.VlogerVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.n3r.idworker.Sid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Slf4j

@Service
public class VlogServiceImpl extends BaseInfoProperties implements VlogService {

    @Autowired
    private VlogMapper vlogMapper;

    @Autowired
    private VlogMapperCustom vlogMapperCustom;

    @Autowired
    private MyLikedVlogMapper myLikedVlogMapper;

    @Autowired
    private FanService fanService;

    @Autowired
    private MessageService messageService;


    @Value("${nacos.counts}")
    private Integer nacosCounts;
    @Autowired
    private Sid sid;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Override
    public Vlog getVlog(String id) {
        return vlogMapper.selectByPrimaryKey(id);
    }

    // 上传视频
    @Transactional
    @Override
    public void createVlog(VlogBO vlogBO) {
        String vlogId = sid.nextShort();
        Vlog vlog = new Vlog();
        BeanUtils.copyProperties(vlogBO,vlog);
        vlog.setId(vlogId);
        vlog.setLikeCounts(0);
        vlog.setCommentsCounts(0);
        vlog.setIsPrivate(YesOrNo.NO.type);
        vlog.setCreatedTime(new Date());
        vlog.setUpdatedTime(new Date());
        vlogMapper.insert(vlog);
    }

    // 获取视频详情
    @Override
    public IndexVlogVO getVlogDetailById(String userId, String vlogId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("vlogId",vlogId);
        List<IndexVlogVO> list = vlogMapperCustom.getVlogDetailById(map);
        if(list!=null&&list.size()>0){
            IndexVlogVO indexVlogVO = list.get(0);
            return setterVO(indexVlogVO, userId);
        }
        return  null;
    }

    private IndexVlogVO setterVO(IndexVlogVO v, String userId) {
        String vlogerId = v.getVlogerId();
        String vlogId = v.getVlogId();

        if (StringUtils.isNotBlank(userId)) {
            // 用户是否关注该博主
            boolean doIFollowVloger = fanService.queryDoIFollowVloger(userId, vlogerId);
            v.setDoIFollowVloger(doIFollowVloger);

            // 判断当前用户是否点赞过视频
            v.setDoILikeThisVlog(doILikeVlog(userId, vlogId));
        }

        // 获得当前视频被点赞过的总数
        v.setLikeCounts(getVlogerBelikedCount(vlogId));

        return v;
    }


    // 获取视频列表
    @Override
    public PagedGridResult findVlogerList(String search,
                                          String userId,
                                          Integer page,
                                          Integer pageSize) {
        PageHelper.startPage(page,pageSize);

        HashMap<String, Object> map = new HashMap<>();
        if(StringUtils.isNotBlank(search)){
            map.put("search",search);
        }
        List<IndexVlogVO> VlogList = vlogMapperCustom.getIndexVlogList(map);

        for(IndexVlogVO v :VlogList){
            String vlogerId = v.getVlogerId();
            String vlogId = v.getVlogId();
            setterVO(v,userId);
//            if (StringUtils.isNotBlank(userId)) {
//                // 用户是否关注该博主
//                boolean doIFollowVloger = fanService.queryDoIFollowVloger(userId, vlogerId);
//                v.setDoIFollowVloger(doIFollowVloger);
//
//                // 判断当前用户是否点赞过视频
//                boolean doILikeVlog=doILikeVlog(userId, vlogId);
//                v.setDoILikeThisVlog(doILikeVlog);
//                v.setLikeCounts(getVlogerBelikedCount(vlogId));
//
//            }
        }

        return setterPagedGrid(VlogList,page);
    }

    // 判断当前用户是否点赞过视频
    private boolean doILikeVlog(String myId,String vlogId){
        String doILike= redis.get(REDIS_USER_LIKE_VLOG + ":" + myId + ":" + vlogId);
        boolean isLike=false;
        if(StringUtils.isNotBlank(doILike)&&doILike.equalsIgnoreCase("1")){
            isLike=true;
        }
        return isLike;
    }

    // 获取视频被点赞数
    @Override
    public Integer getVlogerBelikedCount(String vlogId){
        String count = redis.get(REDIS_VLOG_BE_LIKED_COUNTS+":"+vlogId);
        if(StringUtils.isBlank(count)){
            count="0";
        }
        return Integer.valueOf(count);
    }


    //改变视频的私密状态
    @Override
    public void changeToPrivateOrPublic(String userId, String vlogId, Integer yesOrNo) {
        Example example = new Example(Vlog.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("id", vlogId);
        criteria.andEqualTo("vlogerId", userId);

        Vlog pendingVlog = new Vlog();
        pendingVlog.setIsPrivate(yesOrNo);

        vlogMapper.updateByExampleSelective(pendingVlog, example);
    }

    // 分页查询获取用户的视频列表
    @Override
    public PagedGridResult queryMyVlogList(String userId,
                                           Integer page,
                                           Integer pageSize,
                                           Integer yesOrNo) {
        Example example = new Example(Vlog.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("isPrivate", yesOrNo);
        criteria.andEqualTo("vlogerId", userId);

        PageHelper.startPage(page,pageSize);
        List<Vlog> vlogList = vlogMapper.selectByExample(example);
        return setterPagedGrid(vlogList,page);
    }


    //用户点赞视频
    @Transactional
    @Override
    public void userLikeVlog(String userId, String vlogId, String vlogerId) {
        String rid = sid.nextShort();

        MyLikedVlog likedVlog = new MyLikedVlog();
        likedVlog.setId(rid);
        likedVlog.setVlogId(vlogId);
        likedVlog.setUserId(userId);

        // 保存点赞记录
        myLikedVlogMapper.insert(likedVlog);

        // 点赞后，视频和视频发布者的获赞都会 +1
        redis.increment(REDIS_VLOGER_BE_LIKED_COUNTS + ":" + vlogerId, 1);
        redis.increment(REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlogId, 1);

        // 我点赞的视频，需要在redis中保存关联关系
        redis.set(REDIS_USER_LIKE_VLOG + ":" + userId + ":" + vlogId, "1");

        // 点赞完毕，获得当前在redis中的总数
        // 比如获得总计数为 1k/1w/10w，假定阈值（配置）为2000
        // 此时1k满足2000，则触发入库

        String countsStr = redis.get(REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlogId);
        log.info("======" + REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlogId + "======");
        Integer counts = 0;
        if (StringUtils.isNotBlank(countsStr)) {
            counts = Integer.valueOf(countsStr);
            if (counts >= nacosCounts) {
                this.flushCounts(vlogId, counts);
            }
        }


        HashMap<String, Object> msgContent = new HashMap<>();
        Vlog vlog = vlogMapper.selectByPrimaryKey(vlogId);
        msgContent.put("vlogId",vlogId);
        msgContent.put("vlogCover",vlog.getCover());
        //messageService.createMsg(userId,vlogerId, MessageEnum.LIKE_VLOG.type,msgContent);

        // MQ异步解耦
        MessageMO messageMO = new MessageMO();
        messageMO.setFromUserId(userId);
        messageMO.setToUserId(vlog.getVlogerId());
        messageMO.setMsgContent(msgContent);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_MSG,
                "sys.msg."+MessageEnum.LIKE_VLOG.enValue,
                JsonUtils.objectToJson(messageMO)
        );

    }
    @Transactional
    @Override
    public void userUnLikeVlog(String userId, String vlogId, String vlogerId) {

        MyLikedVlog likedVlog = new MyLikedVlog();
        likedVlog.setVlogId(vlogId);
        likedVlog.setUserId(userId);
        // 删除点赞记录
        myLikedVlogMapper.delete(likedVlog);

        // 取消点赞后，视频和视频发布者的获赞都会 -1
        redis.decrement(REDIS_VLOGER_BE_LIKED_COUNTS + ":" + vlogerId, 1);
        redis.decrement(REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlogId, 1);

        redis.del(REDIS_USER_LIKE_VLOG + ":" + userId + ":" + vlogId);
    }

    //分页查询用户点赞的视频
    @Override
    public PagedGridResult getMyLikedVlogList(String userId,
                                              Integer page,
                                              Integer pageSize) {
        PageHelper.startPage(page, pageSize);
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        List<IndexVlogVO> list = vlogMapperCustom.getMyLikedVlogList(map);
        System.out.println(list);

        List<IndexVlogVO2> vlogList = new ArrayList<>();

        for(IndexVlogVO v: list){
            IndexVlogVO2 indexVlogVO2 = new IndexVlogVO2();
            BeanUtils.copyProperties(v,indexVlogVO2);
            indexVlogVO2.setId(v.getVlogId());
            vlogList.add(indexVlogVO2);
        }
        return setterPagedGrid(vlogList, page);
    }

    private void setIndexVlogVOFollowAndLiked(String myId, List<IndexVlogVO> VlogList) {
        for(IndexVlogVO v : VlogList){
            String vlogerId = v.getVlogerId();
            String vlogId = v.getVlogId();
            if (StringUtils.isNotBlank(myId)) {
                // 用户肯定关注博主
                v.setDoIFollowVloger(true);
                // 判断当前用户是否点赞过视频
                boolean doILikeVlog=doILikeVlog(myId, vlogId);
                v.setDoILikeThisVlog(doILikeVlog);
            }
            // 设置视频的点赞数
            v.setLikeCounts(getVlogerBelikedCount(vlogId));
        }
    }


    //分页查询用户关注的人的视频

    @Override
    public PagedGridResult getMyFollowVlogList(String myId, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);

        Map<String, Object> map = new HashMap<>();
        map.put("myId", myId);

        List<IndexVlogVO> list = vlogMapperCustom.getMyFollowVlogList(map);

        for (IndexVlogVO v : list) {
            String vlogerId = v.getVlogerId();
            String vlogId = v.getVlogId();

            if (StringUtils.isNotBlank(myId)) {
                // 用户必定关注该博主
                v.setDoIFollowVloger(true);

                // 判断当前用户是否点赞过视频
                v.setDoILikeThisVlog(doILikeVlog(myId, vlogId));
            }

            // 获得当前视频被点赞过的总数
            v.setLikeCounts(getVlogerBelikedCount(vlogId));
        }
        System.out.println("follows"+setterPagedGrid(list, page).toString());
        return setterPagedGrid(list, page);
    }



    //分页查询用户的朋友的视频

    @Override
    public PagedGridResult getMyFriendVlogList(String myId, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);

        Map<String, Object> map = new HashMap<>();
        map.put("myId", myId);

        List<IndexVlogVO> list = vlogMapperCustom.getMyFriendVlogList(map);


        for (IndexVlogVO v : list) {
            String vlogerId = v.getVlogerId();
            String vlogId = v.getVlogId();

            if (StringUtils.isNotBlank(myId)) {
                // 用户必定关注该博主
                v.setDoIFollowVloger(true);

                // 判断当前用户是否点赞过视频
                v.setDoILikeThisVlog(doILikeVlog(myId, vlogId));
            }

            // 获得当前视频被点赞过的总数
            v.setLikeCounts(getVlogerBelikedCount(vlogId));
        }
        System.out.println("friends"+setterPagedGrid(list, page).toString());
        return setterPagedGrid(list, page);
    }

    private void extracted(String myId, List<IndexVlogVO> VlogList) {
        setIndexVlogVOFollowAndLiked(myId, VlogList);
    }

    @Transactional
    @Override
    public void flushCounts(String vlogId, Integer counts) {
        Vlog vlog = new Vlog();
        vlog.setId(vlogId);
        vlog.setLikeCounts(counts);
        vlogMapper.updateByPrimaryKeySelective(vlog);
    }
}
