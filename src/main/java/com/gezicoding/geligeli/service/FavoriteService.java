package com.gezicoding.geligeli.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.dto.video.CancelVideoActionRequest;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.entity.Favorite;

public interface FavoriteService extends IService<Favorite> {


    /**
     * 收藏视频
     * @param videoActionRequest
     * @return
     */
    Long favoriteVideo(VideoActionRequest videoActionRequest);


    /**
     * 取消收藏视频
     * @param cancelVideoActionRequest
     * @return
     */
    Boolean cancelFavoriteVideo(CancelVideoActionRequest cancelVideoActionRequest);
}
