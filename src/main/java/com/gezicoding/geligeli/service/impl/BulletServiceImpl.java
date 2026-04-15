package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.exception.BusinessException;
import com.gezicoding.geligeli.mapper.BulletMapper;
import com.gezicoding.geligeli.mapper.VideoMapper;
import com.gezicoding.geligeli.model.dto.video.DeleteBulletRequest;
import com.gezicoding.geligeli.model.dto.video.SendBulletRequest;
import com.gezicoding.geligeli.model.entity.Bullet;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.entity.VideoStats;
import com.gezicoding.geligeli.model.vo.video.OnlineBulletResponse;
import com.gezicoding.geligeli.service.BulletService;
import com.gezicoding.geligeli.service.UserService;
import com.gezicoding.geligeli.service.VideoStatsService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulletServiceImpl extends ServiceImpl<BulletMapper, Bullet> implements BulletService {

    @Autowired
    private UserService userService;
    
    @Autowired
    private VideoMapper videoMapper;


    @Autowired
    private VideoStatsService videoStatsService;

    /**
     * 保存弹幕到MySQL
     * @param sendBulletRequest 发送弹幕请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBulletToMySQL(SendBulletRequest sendBulletRequest) {
        Long videoId = sendBulletRequest.getVideoId();
        Long userId = sendBulletRequest.getUserId();

        if (!userService.lambdaQuery().eq(User::getUserId, userId).exists()) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        // 保存弹幕
        Bullet bullet = new Bullet();
        bullet.setVideoId(videoId);
        bullet.setUserId(userId);
        bullet.setContent(sendBulletRequest.getContent());
        bullet.setPlaybackTime(sendBulletRequest.getPlaybackTime());
        bullet.setBulletId(sendBulletRequest.getBulletId());
        boolean saved = this.save(bullet);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存弹幕失败");
        }

        // 更新视频弹幕统计
        boolean updated = videoStatsService.lambdaUpdate().setSql("bullet_count = bullet_count + 1").eq(VideoStats::getVideoId, videoId).update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "弹幕统计更新失败");
        }
    }

    /**
     * 删除弹幕
     * @param deleteBulletRequest 删除弹幕请求
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBullet(DeleteBulletRequest deleteBulletRequest) {

        Long videoId = deleteBulletRequest.getVideoId();
        Long userId = deleteBulletRequest.getUserId();
        Long bulletId = deleteBulletRequest.getBulletId();

        QueryWrapper<Video> videoQueryWrapper = new QueryWrapper<>();
        videoQueryWrapper.eq("video_id", videoId).eq("user_id", userId).eq("is_delete", 0);
        Long videoCount = videoMapper.selectCount(videoQueryWrapper);
        if (videoCount == null || videoCount == 0) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND_ERROR);
        }

        // 验证弹幕是否存在
        if (!this.lambdaQuery().eq(Bullet::getBulletId, bulletId).exists()) {
            throw new BusinessException(ErrorCode.BULLET_NOT_EXISTS);
        }


        // 用户是否存在
        if (!userService.lambdaQuery().eq(User::getUserId, userId).exists()) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        // 删除视频弹幕统计，但是现在是通过websocket来更新
        boolean updated = videoStatsService.lambdaUpdate().setSql("bullet_count = bullet_count - 1").eq(VideoStats::getVideoId, videoId).update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "弹幕统计更新失败");
        }

        // 删除弹幕
        boolean deleted = this.removeById(bulletId);

        if (!deleted) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "弹幕删除失败");
        }

        return true;
    }

    /**
     * 获取视频弹幕列表
     * @param videoId 视频ID
     * @return 视频弹幕列表
     */
    @Override
    public List<OnlineBulletResponse> getBulletList(Long videoId) {
        List<OnlineBulletResponse> onlineBulletResponses = new ArrayList<>();
        QueryWrapper<Bullet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("video_id", videoId);

        List<Bullet> bulletList = this.list(queryWrapper);

        for (Bullet bullet : bulletList) {
            OnlineBulletResponse onlineBulletResponse = new OnlineBulletResponse();
            onlineBulletResponse.setText(bullet.getContent());
            onlineBulletResponse.setPlaybackTime(bullet.getPlaybackTime());
            onlineBulletResponse.setBulletId(bullet.getBulletId().toString());
            onlineBulletResponse.setUserId(bullet.getUserId().toString());
            onlineBulletResponses.add(onlineBulletResponse);
        }
        // 对弹幕按时间排序
        onlineBulletResponses.sort(Comparator.comparingDouble(OnlineBulletResponse::getPlaybackTime));
        return onlineBulletResponses;
    }
}
