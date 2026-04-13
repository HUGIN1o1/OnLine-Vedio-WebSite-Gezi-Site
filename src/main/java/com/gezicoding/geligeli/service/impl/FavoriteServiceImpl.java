package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.mapper.FavoriteMapper;
import com.gezicoding.geligeli.model.entity.Favorite;
import com.gezicoding.geligeli.service.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;    
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.entity.VideoStats;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.exception.BusinessException;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import com.gezicoding.geligeli.service.VideoService;
import com.gezicoding.geligeli.service.UserService;
import com.gezicoding.geligeli.service.VideoStatsService;


@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {


    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    @Autowired
    private VideoStatsService videoStatsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long favoriteVideo(VideoActionRequest videoActionRequest) {
        // 检测收藏频率是否过快
        // crawlerFavoriteDetect(videoActionRequest);

        // 校验判断视频是否存在
        if (!videoService.lambdaQuery().eq(Video::getVideoId, videoActionRequest.getVideoId()).exists()) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND_ERROR);
        }

        // 校验判断用户是否存在
        if (!userService.lambdaQuery().eq(User::getUserId, videoActionRequest.getUserId()).exists()) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        // 查询是否已经收藏
        if (this.lambdaQuery()
                .eq(Favorite::getVideoId, videoActionRequest.getVideoId())
                .eq(Favorite::getUserId, videoActionRequest.getUserId())
                .exists()) {
            throw new BusinessException(ErrorCode.VIDEO_FAVORITE_ERROR);
        }

        // 保存收藏记录
        Favorite favoriteVideo = new Favorite();
        favoriteVideo.setVideoId(videoActionRequest.getVideoId());
        favoriteVideo.setUserId(videoActionRequest.getUserId());
        Snowflake snowflake = IdUtil.getSnowflake(SnowFlakeConstants.MACHINE_ID, SnowFlakeConstants.DATA_CENTER_ID);
        favoriteVideo.setFavoriteId(snowflake.nextId());
        boolean save = this.save(favoriteVideo);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        // 视频点赞数+1
        boolean updated = videoStatsService.lambdaUpdate()
                .setSql("favorite_count = favorite_count + 1")
                .eq(VideoStats::getVideoId, videoActionRequest.getVideoId())
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新视频统计失败");
        }

        return favoriteVideo.getFavoriteId();
    }
}
