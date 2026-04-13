package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.exception.BusinessException;
import com.gezicoding.geligeli.mapper.VideoMapper;
import com.gezicoding.geligeli.model.entity.Category;
import com.gezicoding.geligeli.model.entity.File;
import com.gezicoding.geligeli.model.entity.UserStats;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.entity.VideoStats;
import com.gezicoding.geligeli.model.vo.video.VideoSubmitRequest;
import com.gezicoding.geligeli.service.FileService;
import com.gezicoding.geligeli.service.CategoryService;
import com.gezicoding.geligeli.service.UserStatsService;
import com.gezicoding.geligeli.service.VideoService;
import com.gezicoding.geligeli.service.VideoStatsService;
import com.gezicoding.geligeli.utils.MinIOUtil;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {


    @Autowired
    private MinIOUtil minIOUtil;

    @Autowired
    private FileService fileService;

    @Autowired
    private CategoryService categoryService;


    @Autowired
    private VideoStatsService videoStatsService;

    @Autowired
    private UserStatsService userStatsService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(VideoSubmitRequest videoSubmitRequest) throws Exception {

        Long size = videoSubmitRequest.getFile().getSize();
        // 如果文件大于1GB，则抛出异常
        if (size > 1024 * 1024 * 1) {
            throw new BusinessException(ErrorCode.FILE_SIZE_ERROR);
        }

        String coverUrl = minIOUtil.updateCover(videoSubmitRequest.getFile());

        // 获取参数
        String fileUrl = videoSubmitRequest.getFileUrl();
        Long userId = videoSubmitRequest.getUserId();
        String title = videoSubmitRequest.getTitle();
        Integer type = videoSubmitRequest.getType();
        Double duration = videoSubmitRequest.getDuration();
        Integer categoryId = videoSubmitRequest.getCategoryId();
        String tags = videoSubmitRequest.getTags();
        String description = videoSubmitRequest.getDescription();

        // 校验参数
        if (fileUrl == null 
            || fileUrl.trim().isEmpty() 
            || !fileService.lambdaQuery().eq(File::getFileUrl, videoSubmitRequest.getFileUrl()).exists()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (title == null || title.trim().isEmpty() || StringUtils.isEmpty(title)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (type == null || (!type.equals(1) && !type.equals(2))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (duration == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (categoryId == null || !categoryService.lambdaQuery().eq(Category::getCategoryId, categoryId).exists()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (tags == null || tags.trim().isEmpty() || StringUtils.isEmpty(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 使用SnowFlake算法生成视频编号（假设有对应的SnowFlake生成器Bean，如果没有可适当替换）
        Snowflake snowflake = IdUtil.getSnowflake(SnowFlakeConstants.MACHINE_ID, SnowFlakeConstants.DATA_CENTER_ID);
        Video video = new Video();
        video.setUserId(userId);
        video.setTitle(title);
        video.setType(type);
        video.setDuration(duration);
        video.setCategoryId(categoryId);
        video.setCoverUrl(coverUrl);
        video.setFileUrl(fileUrl);
        video.setTags(tags);
        video.setDescription(description);
        video.setVideoId(snowflake.nextId());

        // 存入数据库
        boolean saveResult = this.save(video);

        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        

        VideoStats videoStats = new VideoStats();

        videoStats.setVideoId(video.getVideoId());

        boolean resultVideoStats = videoStatsService.save(videoStats);
        if (!resultVideoStats) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    
        // 更新用户投稿统计
        boolean updated = userStatsService.lambdaUpdate().setSql("video_count = video_count + 1").eq(UserStats::getUserId, videoSubmitRequest.getUserId()).update();

        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户投稿统计失败");
        }

        // TODO 布隆过滤器添加视频ID
        return saveResult;
    }
}
