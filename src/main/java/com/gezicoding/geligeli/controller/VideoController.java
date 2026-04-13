package com.gezicoding.geligeli.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gezicoding.geligeli.common.BaseResponse;
import com.gezicoding.geligeli.common.ResultUtils;
import com.gezicoding.geligeli.model.vo.video.VideoListResponse;
import com.gezicoding.geligeli.model.vo.video.VideoSubmitRequest;
import com.gezicoding.geligeli.service.VideoService;

@RestController
@RequestMapping("/api/video")       
public class VideoController {


    @Autowired
    private VideoService videoService;  

    @PostMapping("/submit")
    public BaseResponse<Boolean> submit(
         @RequestParam("fileUrl") String fileUrl,
         @RequestParam("userId") Long userId,
         @RequestParam("file") MultipartFile file,
         @RequestParam("title") String title,
         @RequestParam("type") Integer type,
         @RequestParam("duration") Double duration,
         @RequestParam("categoryId") Integer categoryId,
         @RequestParam("tags") String tags,
         @RequestParam("description") String description) throws Exception {

        VideoSubmitRequest videoSubmitRequest = new VideoSubmitRequest();
        videoSubmitRequest.setFileUrl(fileUrl);
        videoSubmitRequest.setUserId(userId);
        videoSubmitRequest.setFile(file);
        videoSubmitRequest.setTitle(title);
        videoSubmitRequest.setType(type);
        videoSubmitRequest.setDuration(duration);
        videoSubmitRequest.setCategoryId(categoryId);
        videoSubmitRequest.setTags(tags);
        videoSubmitRequest.setDescription(description);
        boolean result = videoService.submit(videoSubmitRequest);
        return ResultUtils.success(result);
    }


    @GetMapping("/video/list")
    public BaseResponse<List<VideoListResponse>> videoList(@RequestParam Integer current, @RequestParam Integer pageSize) {
        return null;
    }

}
