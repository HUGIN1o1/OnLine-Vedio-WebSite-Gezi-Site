package com.gezicoding.geligeli.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.entity.Favorite;

public interface FavoriteService extends IService<Favorite> {

    Long favoriteVideo(VideoActionRequest videoActionRequest);
}
