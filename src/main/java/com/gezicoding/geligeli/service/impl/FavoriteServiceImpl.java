package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.mapper.FavoriteMapper;
import com.gezicoding.geligeli.mapper.VideoMapper;
import com.gezicoding.geligeli.model.entity.Favorite;
import com.gezicoding.geligeli.service.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gezicoding.geligeli.model.dto.video.CancelVideoActionRequest;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.entity.VideoStats;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.exception.BusinessException;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import com.gezicoding.geligeli.service.UserService;
import com.gezicoding.geligeli.service.VideoStatsService;
import com.gezicoding.geligeli.utils.CounterUtil;

import java.util.concurrent.TimeUnit;


@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {


    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private VideoStatsService videoStatsService;

    @Autowired
    private CounterUtil counterUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long favoriteVideo(VideoActionRequest videoActionRequest) {
        // 检测收藏频率是否过快
        crawlerFavoriteDetect(videoActionRequest);

        // 校验判断视频是否存在
        if (videoMapper.selectById(videoActionRequest.getVideoId()) == null) {
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

        // 视频收藏数+1
        boolean updated = videoStatsService.lambdaUpdate()
                .setSql("favorite_count = favorite_count + 1")
                .eq(VideoStats::getVideoId, videoActionRequest.getVideoId())
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新视频统计失败");
        }

        return favoriteVideo.getFavoriteId();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelFavoriteVideo(CancelVideoActionRequest cancelVideoActionRequest) {

        // 查询是否存在
        if (!this.lambdaQuery().eq(Favorite::getFavoriteId, cancelVideoActionRequest.getId()).exists()) {
            throw new BusinessException(ErrorCode.VIDEO_FAVORITE_NOT_EXISTS);
        }

        // 删除收藏记录
        boolean remove = this.removeById(cancelVideoActionRequest.getId());
        if (!remove) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        // 视频点赞数 -1
        boolean updated = videoStatsService.lambdaUpdate()
                .setSql("favorite_count = favorite_count - 1")
                .eq(VideoStats::getVideoId, cancelVideoActionRequest.getVideoId())
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新视频统计失败");
        }

        return true;
    }

        
        
    private void crawlerFavoriteDetect(VideoActionRequest videoActionRequest) {
        // 调用多少次时告警
        final int WARN_COUNT = 2;
        // 拼接访问 key
        String key = String.format("favorite:%s:%s", videoActionRequest.getUserId(), videoActionRequest.getVideoId());
        // 统计一分钟内访问次数，180 秒过期
        long count = counterUtil.incrAndGetCounter(key, 1, TimeUnit.MINUTES, 80);
        // 是否告警
        if (count > WARN_COUNT) {
            throw new BusinessException(ErrorCode.ACCESS_TOO_FREQUENTLY);
        }
   
    }
}
