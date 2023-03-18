package com.imooc.service;

import com.imooc.bo.VlogBO;
import com.imooc.pojo.Vlog;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.IndexVlogVO;
import com.imooc.vo.VlogerVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface VlogService {

    /**
     * 上传视频
     */
    public void createVlog(VlogBO vlogBO);

    /**
     * 获取视频列表
     */
    public PagedGridResult findVlogerList(String search,
                                          String userId,
                                          Integer page,
                                          Integer pageSize);

    /**
     * 获取视频详情
     */

    public IndexVlogVO getVlogDetailById(String userId, String vlogId);

    /**
     * 修改私密或者公开
     * @param userId
     * @param vlogId
     * @param yesOrNo
     */
    public void changeToPrivateOrPublic(String userId,
                                        String vlogId,
                                        Integer yesOrNo);




    /**
     * 用户点赞/喜欢视频
     */
    public void userLikeVlog(String userId, String vlogId, String vlogerId);

    /**
     * 用户取消点赞/喜欢视频
     */
    public void userUnLikeVlog(String userId, String vlogId, String vlogerId);

    /**
     * 获取视频点赞总数
     * @param vlogId
     * @return
     */
    public Integer getVlogerBelikedCount(String vlogId);

    /**
     * 查询用户的私密或者公开的视频列表(分页)
     */
    public PagedGridResult queryMyVlogList(String userId,
                                           Integer page,
                                           Integer pageSize,
                                           Integer yesOrNo);

    /**
     *分页查询用户点赞过的视频
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    public PagedGridResult getMyLikedVlogList(String userId,
                                                 Integer page,
                                                 Integer pageSize);


    /**
     * 分页查询用户关注的视频
     * @param myId
     * @param page
     * @param pageSize
     * @return
     */
    public PagedGridResult getMyFollowVlogList(String myId,
                                              Integer page,
                                              Integer pageSize);


    /**
     * 分页查询我的朋友的视频
     */
    public PagedGridResult getMyFriendVlogList(String myId,
                                               Integer page,
                                               Integer pageSize);

    /**
     * 根据主键查询vlog
     */
    public Vlog getVlog(String id);

    /**
     * 把counts刷新到数据库中
     */
    public void  flushCounts(String vlogId, Integer counts);
}
