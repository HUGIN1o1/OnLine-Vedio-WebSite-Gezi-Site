package com.gezicoding.geligeli.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.vo.video.VideoListResponse;
import com.gezicoding.geligeli.model.vo.video.VideoResponse;
import com.gezicoding.geligeli.model.vo.video.VideoSubmitRequest;

public interface VideoService extends IService<Video> {

    boolean submit(VideoSubmitRequest videoSubmitRequest) throws Exception;


    List<VideoListResponse> getVideoList(Integer current, Integer pageSize);

    List<VideoListResponse> getSubmitVideoList(Long userId);

    List<VideoListResponse> getCategoryVideoList(Integer categoryId);

    VideoResponse videoDetail(VideoActionRequest videoActionRequest);

    

    
}
