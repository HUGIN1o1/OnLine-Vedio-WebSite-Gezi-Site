package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.mapper.CoinMapper;
import com.gezicoding.geligeli.model.entity.Coin;
import com.gezicoding.geligeli.service.CoinService;
import com.gezicoding.geligeli.service.VideoStatsService;
import com.gezicoding.geligeli.service.VideoService;
import com.gezicoding.geligeli.service.UserService;
import com.gezicoding.geligeli.service.UserStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.entity.VideoStats;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.entity.UserStats;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.exception.BusinessException;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
@Service
public class CoinServiceImpl extends ServiceImpl<CoinMapper, Coin> implements CoinService {

    @Autowired
    private VideoService videoService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private VideoStatsService videoStatsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean coinVideo(VideoActionRequest videoActionRequest) {

        // 校验判断用户是否已经投币
        if (this.lambdaQuery()
                .eq(Coin::getVideoId, videoActionRequest.getVideoId())
                .eq(Coin::getUserId, videoActionRequest.getUserId())
                .exists()) {
            throw new BusinessException(ErrorCode.VIDEO_COIN_ERROR);
        }

        // 校验判断视频是否存在
        if (!videoService.lambdaQuery()
                .eq(Video::getVideoId, videoActionRequest.getVideoId())
                .exists()) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND_ERROR);
        }

        // 校验判断用户是否存在
        User user = userService.lambdaQuery()
                .eq(User::getUserId, videoActionRequest.getUserId())
                .one();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        // 获取用户详细信息
        UserStats userStats = userStatsService.getById(user.getUserId());

        // 校验判断用户硬币是否足够
        if (userStats.getCoinCount() < 1) {
            throw new BusinessException(ErrorCode.USER_COIN_ERROR);
        }

        // 保存投币记录
        Coin coin = new Coin();
        coin.setVideoId(videoActionRequest.getVideoId());
        coin.setUserId(videoActionRequest.getUserId());
        Snowflake snowflake = IdUtil.getSnowflake(SnowFlakeConstants.MACHINE_ID, SnowFlakeConstants.DATA_CENTER_ID);
        coin.setCoinId(snowflake.nextId());
        boolean save = this.save(coin);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        // 视频投币数 +1
        boolean updatedVideoStats = videoStatsService.lambdaUpdate()
                .setSql("coin_count = coin_count + 1")
                .eq(VideoStats::getVideoId, videoActionRequest.getVideoId())
                .update();
        if (!updatedVideoStats) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新视频统计失败");
        }

        // 用户硬币数 -1
        boolean updatedUserCoin = userStatsService.lambdaUpdate()
                .setSql("coin_count = coin_count - 1")
                .eq(UserStats::getUserId, videoActionRequest.getUserId())
                .update();
        if (!updatedUserCoin) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户硬币数失败");
        }

        return true;

    }
}
