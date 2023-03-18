package com.imooc.controller;

import com.imooc.base.BaseInfoProperties;
import com.imooc.bo.UpdatedUserBO;
import com.imooc.enums.FileTypeEnum;
import com.imooc.enums.UserInfoModifyType;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.pojo.Users;
import com.imooc.service.QiniuService;
import com.imooc.service.UserService;
import com.imooc.utils.SMSUtils;
import com.imooc.vo.UsersVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Api(tags = "UserInfoController 用户信息模块")
@RequestMapping("userInfo")
@RestController
public class UserInfoController extends BaseInfoProperties {

    @Autowired
    private SMSUtils smsUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private QiniuService qiniuService;


    @ApiOperation(value = "用户信息查询", notes = "用户信息查询", httpMethod = "GET")
    @GetMapping("query")
    public GraceJSONResult query(@RequestParam String userId){
        Users user = userService.getUser(userId);
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(user, usersVO);

        // 我的关注博主总数量
        String myFollowsCountsStr = redis.get(REDIS_MY_FOLLOWS_COUNTS + ":" + userId);
        // 我的粉丝总数
        String myFansCountsStr = redis.get(REDIS_MY_FANS_COUNTS + ":" + userId);
        // 用户获赞总数，视频博主（点赞/喜欢）总和
//        String likedVlogCountsStr = redis.get(REDIS_VLOG_BE_LIKED_COUNTS + ":" + userId);
        String likedVlogerCountsStr = redis.get(REDIS_VLOGER_BE_LIKED_COUNTS + ":" + userId);
        Integer myFollowsCounts = 0;
        Integer myFansCounts = 0;
        Integer likedVlogCounts = 0;
        Integer likedVlogerCounts = 0;
        Integer totalLikeMeCounts = 0;
        if (StringUtils.isNotBlank(myFollowsCountsStr)) {
            myFollowsCounts = Integer.valueOf(myFollowsCountsStr);
        }
        if (StringUtils.isNotBlank(myFansCountsStr)) {
            myFansCounts = Integer.valueOf(myFansCountsStr);
        }
//        if (StringUtils.isNotBlank(likedVlogCountsStr)) {
//            likedVlogCounts = Integer.valueOf(likedVlogCountsStr);
//        }
        if (StringUtils.isNotBlank(likedVlogerCountsStr)) {
            likedVlogerCounts = Integer.valueOf(likedVlogerCountsStr);
        }
        totalLikeMeCounts = likedVlogCounts + likedVlogerCounts;
        usersVO.setMyFollowsCounts(myFollowsCounts);
        usersVO.setMyFansCounts(myFansCounts);
        usersVO.setTotalLikeMeCounts(totalLikeMeCounts);
        return GraceJSONResult.ok(usersVO);
    }

    @ApiOperation(value = "用户信息修改", notes = "用户信息修改", httpMethod = "POST")
    @PostMapping("modifyUserInfo")
    public GraceJSONResult modifyUserInfo(@RequestBody UpdatedUserBO updatedUserBO,
                                          @RequestParam Integer type)throws Exception{
        // 0. 判断用户信息修改类型是否合法
        UserInfoModifyType.checkUserInfoTypeIsRight(type);

        Users userInfo = userService.updateUserInfo(updatedUserBO, type);

        return GraceJSONResult.ok(userInfo);
    }




    @ApiOperation(value = "用户上传头像", notes = "用户上传头像", httpMethod = "POST")
    @PostMapping(value="/modifyImage")

    public GraceJSONResult modifyImage(@RequestParam String userId,
                                       @RequestParam Integer type,
                                       MultipartFile file) throws Exception {


        if (type != FileTypeEnum.BGIMG.type && type != FileTypeEnum.FACE.type) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_UPLOAD_FAILD);
        }
        String imgUrl = "http://";
        try {
            imgUrl=imgUrl+qiniuService.saveImage(file);//上传到七牛云
        } catch (IOException e) {
            e.printStackTrace();
        }
        //
        UpdatedUserBO updatedUserBO=new UpdatedUserBO();
        updatedUserBO.setId(userId);

        if (type == FileTypeEnum.BGIMG.type) {
            updatedUserBO.setBgImg(imgUrl);//设置背景图片
            System.out.println("背景图片地址："+imgUrl);
        } else {
            updatedUserBO.setFace(imgUrl);//设置头像
            System.out.println("头像地址："+imgUrl);
        }
        Users userInfo = userService.updateUserInfo(updatedUserBO);
        return GraceJSONResult.ok(userInfo);
    }
}

