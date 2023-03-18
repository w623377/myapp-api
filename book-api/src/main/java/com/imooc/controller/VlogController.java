package com.imooc.controller;

import com.imooc.base.BaseInfoProperties;
import com.imooc.bo.VlogBO;
import com.imooc.enums.YesOrNo;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.service.VlogService;
import com.imooc.utils.PagedGridResult;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@Api(tags = "VlogController 短视频增删改查接口模块")
@RequestMapping("vlog")
@RestController
public class VlogController extends BaseInfoProperties {

    @Autowired
    private VlogService vlogService;

    @PostMapping("publish")
    public GraceJSONResult publish(@Valid @RequestBody VlogBO VlogBo){
        vlogService.createVlog(VlogBo);
        return GraceJSONResult.ok();
    }

    @GetMapping("indexList")
    public GraceJSONResult indexList(@RequestParam(defaultValue = "") String search,
                                     @RequestParam(defaultValue = "") String userId,
                                     @RequestParam Integer page,
                                     @RequestParam Integer pageSize){

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }
        PagedGridResult vlogerList = vlogService.findVlogerList(search,userId,page,pageSize);
        System.out.println(vlogerList);
        return GraceJSONResult.ok(vlogerList);
    }

    @GetMapping("detail")
    public GraceJSONResult detail(@RequestParam(defaultValue = "") String userId,
                                  @RequestParam String vlogId) {
        return GraceJSONResult.ok(vlogService.getVlogDetailById(userId, vlogId));
    }

    @PostMapping("changeToPrivate")
    public GraceJSONResult changeToPrivate(@RequestParam String userId,
                                     @RequestParam String vlogId){
        vlogService.changeToPrivateOrPublic(userId,vlogId, YesOrNo.YES.type);
        return GraceJSONResult.ok();
    }

    @GetMapping("myPublicList")
    public GraceJSONResult myPublicList(@RequestParam String userId,
                                           @RequestParam Integer page,
                                           @RequestParam Integer pageSize){
        PagedGridResult list = vlogService.queryMyVlogList(userId, page, pageSize, YesOrNo.NO.type);
        return GraceJSONResult.ok(list);
    }

    @GetMapping("myPrivateList")
    public GraceJSONResult myPrivateList(@RequestParam String userId,
                                          @RequestParam Integer page,
                                          @RequestParam Integer pageSize){
        PagedGridResult list = vlogService.queryMyVlogList(userId, page, pageSize, YesOrNo.YES.type);
        return GraceJSONResult.ok(list);
    }

    @PostMapping("like")
    public GraceJSONResult like(@RequestParam String userId,
                                         @RequestParam String  vlogerId,
                                         @RequestParam String vlogId){

        vlogService.userLikeVlog(userId,vlogId,vlogerId);
        return GraceJSONResult.ok();
    }

    @PostMapping("unlike")
    public GraceJSONResult unlike(@RequestParam String userId,
                                @RequestParam String  vlogerId,
                                @RequestParam String vlogId){

        vlogService.userUnLikeVlog(userId,vlogId,vlogerId);
        return GraceJSONResult.ok();
    }

    @PostMapping("totalLikedCounts")
    public GraceJSONResult totalLikeCounts(@RequestParam String vlogId){

        Integer belikedCount = vlogService.getVlogerBelikedCount(vlogId);
        return GraceJSONResult.ok(belikedCount);
    }

    @GetMapping("myLikedList")
    public GraceJSONResult myLikedList(@RequestParam String userId,
                                       @RequestParam Integer page,
                                       @RequestParam Integer pageSize) {

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = vlogService.getMyLikedVlogList(userId,
                                                                    page,
                                                                    pageSize);
        return GraceJSONResult.ok(gridResult);
    }

    @GetMapping("followList")
    public GraceJSONResult followList(@RequestParam String myId,
                                      @RequestParam Integer page,
                                      @RequestParam Integer pageSize) {

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = vlogService.getMyFollowVlogList(myId,
                                                                    page,
                                                                    pageSize);
        return GraceJSONResult.ok(gridResult);
    }

    @GetMapping("friendList")
    public GraceJSONResult friendList(@RequestParam String myId,
                                      @RequestParam Integer page,
                                      @RequestParam Integer pageSize) {

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = vlogService.getMyFriendVlogList(myId,
                page,
                pageSize);
        return GraceJSONResult.ok(gridResult);
    }

}

