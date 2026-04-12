package com.gezicoding.geligeli.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import com.gezicoding.geligeli.common.BaseResponse;
import com.gezicoding.geligeli.common.ResultUtils;
import com.gezicoding.geligeli.model.dto.video.DeleteBulletRequest;
import com.gezicoding.geligeli.service.BulletService;
import com.gezicoding.geligeli.service.VideoService;

@Slf4j
@RestController
public class VideoController {

    @Resource
    private VideoService videoService;

    @Resource
    private BulletService bulletService;

    @PostMapping("/video/delete/bullet")
    public BaseResponse<Boolean> deleteBullet(@RequestBody DeleteBulletRequest request) {
        boolean result = bulletService.deleteBullet(request);
        return ResultUtils.success(result);
    }

    

}
