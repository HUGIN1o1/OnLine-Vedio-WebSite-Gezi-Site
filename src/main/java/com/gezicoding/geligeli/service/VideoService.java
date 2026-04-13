package com.gezicoding.geligeli.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.vo.video.VideoSubmitRequest;

public interface VideoService extends IService<Video> {

    boolean submit(VideoSubmitRequest videoSubmitRequest) throws Exception;
}
