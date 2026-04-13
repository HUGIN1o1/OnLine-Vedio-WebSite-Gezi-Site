package com.gezicoding.geligeli.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.dto.video.CancelVideoActionRequest;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.entity.Like;

public interface LikeService extends IService<Like> {

    Long likeVideo(VideoActionRequest likeVideoRequest);

    Boolean cancelLikeVideo(CancelVideoActionRequest cancelVideoActionRequest);
}
