package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.mapper.LikeMapper;
import com.gezicoding.geligeli.model.dto.video.CancelVideoActionRequest;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.entity.Like;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.entity.VideoStats;
import com.gezicoding.geligeli.service.LikeService;
import com.gezicoding.geligeli.service.UserService;
import com.gezicoding.geligeli.service.VideoService;
import com.gezicoding.geligeli.service.VideoStatsService;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.exception.BusinessException;

@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    @Autowired
    private VideoService videoService;


    @Autowired
    private UserService userService;


    @Autowired
    private VideoStatsService videoStatsService;

    @Override
    @Transactional(rollbackFor = Exception.class)   
    public Long likeVideo(VideoActionRequest videoActionRequest) {
        // 判断视频是否存在，如果不存在抛出异常
        if (videoService.lambdaQuery().eq(Video::getVideoId, videoActionRequest.getVideoId()).exists()) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND_ERROR);
        }

        
        if (!userService.lambdaQuery().eq(User::getUserId, videoActionRequest.getUserId()).exists()) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        // 是否已经点过赞
        if (this.lambdaQuery().eq(Like::getVideoId, videoActionRequest.getVideoId()).eq(Like::getUserId, videoActionRequest.getUserId()).exists()) {
            throw new BusinessException(ErrorCode.VIDEO_LIKED_ERROR);
        }

        Like likeVideo = new Like();
        likeVideo.setVideoId(videoActionRequest.getVideoId());
        likeVideo.setUserId(videoActionRequest.getUserId());
        Snowflake snowflake = IdUtil.getSnowflake(SnowFlakeConstants.MACHINE_ID, SnowFlakeConstants.DATA_CENTER_ID);
        likeVideo.setLikeId(snowflake.nextId());
        boolean save = this.save(likeVideo);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "点赞失败");
        }
        

        boolean updated = videoStatsService.lambdaUpdate().setSql("like_count = like_count + 1").eq(VideoStats::getVideoId, videoActionRequest.getVideoId()).update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新点赞数失败");
        }

        // 返回点赞数
        return likeVideo.getLikeId();
    }

    /** 
     * 取消点赞
     * @param cancelVideoActionRequest
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelLikeVideo(CancelVideoActionRequest cancelVideoActionRequest) {

        if (!this.lambdaQuery().eq(Like::getLikeId, cancelVideoActionRequest.getId()).exists()) {
            throw new BusinessException(ErrorCode.VIDEO_LIKED_NOT_EXISTS);
        }

        boolean remove = this.removeById(cancelVideoActionRequest.getId());
        if (!remove) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消点赞失败");
        }

        boolean updated = videoStatsService.lambdaUpdate().setSql("like_count = like_count - 1").eq(VideoStats::getVideoId, cancelVideoActionRequest.getVideoId()).update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新视频统计失败");
        }

        return true;
    }
}
