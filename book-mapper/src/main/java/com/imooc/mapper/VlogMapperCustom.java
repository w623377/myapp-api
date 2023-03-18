package com.imooc.mapper;

import com.imooc.vo.IndexVlogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
public interface VlogMapperCustom {

    // 获取首页视频列表
    public List<IndexVlogVO> getIndexVlogList(@Param("paramMap")Map<String, Object> map);

    // 获取视频详情
    public List<IndexVlogVO> getVlogDetailById(@Param("paramMap")Map<String, Object> map);

    // 获取视频详情
    public List<IndexVlogVO> getMyLikedVlogList(@Param("paramMap")Map<String, Object> map);

    // 获取视频详情
    public List<IndexVlogVO> getMyFollowVlogList(@Param("paramMap")Map<String, Object> map);

    // 获取视频详情
    public List<IndexVlogVO> getMyFriendVlogList(@Param("paramMap")Map<String, Object> map);

}
