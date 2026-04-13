package com.gezicoding.geligeli.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.entity.Coin;

public interface CoinService extends IService<Coin> {

    Boolean coinVideo(VideoActionRequest videoActionRequest);
}
