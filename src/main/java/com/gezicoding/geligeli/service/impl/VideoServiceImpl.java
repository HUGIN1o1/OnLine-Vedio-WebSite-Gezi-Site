package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.constants.VideoConstant;
import com.gezicoding.geligeli.exception.BusinessException;
import com.gezicoding.geligeli.mapper.VideoMapper;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.entity.Category;
import com.gezicoding.geligeli.model.entity.Coin;
import com.gezicoding.geligeli.model.entity.File;
import com.gezicoding.geligeli.model.entity.Favorite;
import com.gezicoding.geligeli.model.entity.Like;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.entity.UserStats;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.entity.VideoStats;
import com.gezicoding.geligeli.model.vo.video.OnlineBulletResponse;
import com.gezicoding.geligeli.model.vo.video.TripleActionResponse;
import com.gezicoding.geligeli.model.vo.video.VideoDetailsResponse;
import com.gezicoding.geligeli.model.vo.video.VideoListResponse;
import com.gezicoding.geligeli.model.vo.video.VideoResponse;
import com.gezicoding.geligeli.model.vo.video.VideoSubmitRequest;
import com.gezicoding.geligeli.service.CoinService;
import com.gezicoding.geligeli.service.FileService;
import com.gezicoding.geligeli.service.CategoryService;
import com.gezicoding.geligeli.service.FavoriteService;
import com.gezicoding.geligeli.service.BulletService;
import com.gezicoding.geligeli.service.LikeService;
import com.gezicoding.geligeli.service.UserService;
import com.gezicoding.geligeli.service.UserStatsService;
import com.gezicoding.geligeli.service.VideoService;
import com.gezicoding.geligeli.service.VideoStatsService;
import com.gezicoding.geligeli.utils.BitMapBloomUtil;
import com.gezicoding.geligeli.utils.MinIOUtil;
import com.gezicoding.geligeli.service.FollowService;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private FollowService followService;

    @Autowired
    private MinIOUtil minIOUtil;

    @Autowired
    private FileService fileService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private BulletService bulletService;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoStatsService videoStatsService;

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private UserService userService;

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
        boolean updated = userStatsService.lambdaUpdate().setSql("video_count = video_count + 1")
                .eq(UserStats::getUserId, videoSubmitRequest.getUserId()).update();

        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户投稿统计失败");
        }

        // 完成布隆过滤器添加视频ID
        BitMapBloomUtil.add(video.getVideoId().toString());

        return saveResult;
    }

    @Override
    public List<VideoListResponse> getVideoList(Integer current, Integer pageSize) {
        // return videoMapper.getVideoList(current, pageSize);
        if (current == null || pageSize == null) {
            return java.util.Collections.emptyList();
        }
        int dynamicPageSize = (current == 1) ? 11 : 15;

        int offset = (current == 1) ? 0 : 11 + (current - 2) * 15;
        System.out.println("offset: " + offset + ", dynamicPageSize: " + dynamicPageSize);
        return videoMapper.selectVideoWithStats(offset, dynamicPageSize);

    }

    @Override
    public List<VideoListResponse> getSubmitVideoList(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return videoMapper.getSubmitVideoList(userId);
    }

    @Override
    public List<VideoListResponse> getCategoryVideoList(Integer categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return videoMapper.getCategoryVideoList(categoryId);
    }

    @Override
    public VideoResponse videoDetail(VideoActionRequest videoActionRequest) {

        if (!BitMapBloomUtil.contains(videoActionRequest.getVideoId().toString())) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND_ERROR);
        }

        if (videoActionRequest.getVideoId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<Video> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("video_id", videoActionRequest.getVideoId());
        Video video = this.getOne(queryWrapper);

        boolean updated = videoStatsService.lambdaUpdate().setSql("view_count = view_count + 1")
                .eq(VideoStats::getVideoId, videoActionRequest.getVideoId()).update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新视频浏览量失败");
        }

        if (stringRedisTemplate.hasKey("videoDetails:" + videoActionRequest.getVideoId().toString())) {
            stringRedisTemplate.expire("videoDetails:" + videoActionRequest.getVideoId().toString(),
                    VideoConstant.VIDEO_DETAIL_DAYS, TimeUnit.DAYS);
            return hotVideoDetail(videoActionRequest, video);
        }

        return publicVideoDetail(videoActionRequest, video);

    }

    public VideoResponse publicVideoDetail(VideoActionRequest videoActionRequest, Video video) {

        // 获取视频详情
        VideoDetailsResponse videoDetails = videoMapper.getVideoDetails(videoActionRequest.getVideoId());

        // 封装响应对象
        VideoResponse videoResponse = new VideoResponse();
        videoResponse.setVideoDetailsResponse(videoDetails);
        videoResponse.setTripleActionResponse(getTripleActionResponse(videoActionRequest));
        videoResponse.setOnlineBulletList(getVideoBullets(videoActionRequest.getVideoId()));
        videoResponse.setVideoRecommendListResponse(
                getRecommendVideos(video.getCategoryId(), videoActionRequest.getVideoId()));
        videoResponse.setFollow(followService.getFollowType(videoActionRequest.getUserId(), video.getUserId()));

        // 判断热点视频
        QueryWrapper<VideoStats> videoStatsQueryWrapper = new QueryWrapper<>();
        videoStatsQueryWrapper.eq("video_id", videoActionRequest.getVideoId());
        VideoStats videoStats = videoStatsService.getOne(videoStatsQueryWrapper);
        if (videoStats.getViewCount() >= VideoConstant.HOT_VIDEO_VIEW_COUNT) {
            Map<String, String> redisVideoDetails = new HashMap<>();
            redisVideoDetails.put("videoId", String.valueOf(videoDetails.getVideoId()));
            redisVideoDetails.put("fileUrl", videoDetails.getFileUrl());
            redisVideoDetails.put("userId", String.valueOf(videoDetails.getUserId()));
            redisVideoDetails.put("title", videoDetails.getTitle());
            redisVideoDetails.put("type", String.valueOf(videoDetails.getType()));
            redisVideoDetails.put("duration", String.valueOf(videoDetails.getDuration()));
            redisVideoDetails.put("tags", videoDetails.getTags());
            redisVideoDetails.put("description", videoDetails.getDescription());
            redisVideoDetails.put("createTime", String.valueOf(videoDetails.getCreateTime().getTime()));
            redisVideoDetails.put("viewCount", String.valueOf(videoDetails.getViewCount()));
            redisVideoDetails.put("bulletCount", String.valueOf(videoDetails.getBulletCount()));
            redisVideoDetails.put("likeCount", String.valueOf(videoDetails.getLikeCount()));
            redisVideoDetails.put("coinCount", String.valueOf(videoDetails.getCoinCount()));
            redisVideoDetails.put("favoriteCount", String.valueOf(videoDetails.getFavoriteCount()));
            redisVideoDetails.put("commentCount", String.valueOf(videoDetails.getCommentCount()));
            redisVideoDetails.put("nickname", videoDetails.getNickname());
            redisVideoDetails.put("avatar", videoDetails.getAvatar());
            stringRedisTemplate.opsForHash().putAll("videoDetails:" + videoActionRequest.getVideoId().toString(),
                    redisVideoDetails);
            stringRedisTemplate.expire("videoDetails:" + videoActionRequest.getVideoId().toString(),
                    VideoConstant.VIDEO_DETAIL_DAYS, TimeUnit.DAYS);

        }
        return videoResponse;
    }

    /**
     * 获取热门视频详情, 视频在redis中
     * 
     * @param videoActionRequest
     * @param video
     * @return
     */
    public VideoResponse hotVideoDetail(VideoActionRequest videoActionRequest, Video video) {

        // 获取视频详情
        Map<String, String> redisVideoDetails = stringRedisTemplate
                .opsForHash()
                .entries("videoDetails:" + videoActionRequest.getVideoId())
                .entrySet()
                .stream()
                .collect(
                        Collectors.toMap(e -> e.getKey().toString(),
                                e -> e.getValue().toString(), (a, b) -> b, HashMap::new)
                );

        VideoDetailsResponse videoDetails = new VideoDetailsResponse();
        // 基本视频信息
        videoDetails.setVideoId(Long.parseLong(redisVideoDetails.get("videoId")));
        videoDetails.setFileUrl(redisVideoDetails.get("fileUrl"));
        videoDetails.setUserId(Long.parseLong(redisVideoDetails.get("userId")));
        videoDetails.setTitle(redisVideoDetails.get("title"));
        videoDetails.setType(Integer.parseInt(redisVideoDetails.get("type")));
        videoDetails.setDuration(Double.parseDouble(redisVideoDetails.get("duration")));
        videoDetails.setTags(redisVideoDetails.get("tags"));
        videoDetails.setDescription(redisVideoDetails.get("description"));
        long timestamp = Long.parseLong(redisVideoDetails.get("createTime"));
        videoDetails.setCreateTime(new Date(timestamp));
        videoDetails.setViewCount(Integer.parseInt(redisVideoDetails.get("viewCount")));
        videoDetails.setBulletCount(Integer.parseInt(redisVideoDetails.get("bulletCount")));
        videoDetails.setLikeCount(Integer.parseInt(redisVideoDetails.get("likeCount")));
        videoDetails.setCoinCount(Integer.parseInt(redisVideoDetails.get("coinCount")));
        videoDetails.setFavoriteCount(Integer.parseInt(redisVideoDetails.get("favoriteCount")));
        videoDetails.setCommentCount(Integer.parseInt(redisVideoDetails.get("commentCount")));
        videoDetails.setNickname(redisVideoDetails.get("nickname"));
        videoDetails.setAvatar(redisVideoDetails.get("avatar"));

        // 封装响应对象
        VideoResponse videoResponse = new VideoResponse();
        videoResponse.setVideoDetailsResponse(videoDetails);
        videoResponse.setTripleActionResponse(getTripleActionResponse(videoActionRequest));
        videoResponse.setOnlineBulletList(getVideoBullets(videoActionRequest.getVideoId()));
        videoResponse.setVideoRecommendListResponse(getRecommendVideos(video.getCategoryId(), video.getVideoId()));
        videoResponse.setFollow(followService.getFollowType(videoActionRequest.getUserId(), video.getUserId()));

        return videoResponse;
    }

    public TripleActionResponse getTripleActionResponse(VideoActionRequest videoActionRequest) {
        TripleActionResponse tripleActionResponse = new TripleActionResponse();
        // 是否点赞
        if (videoActionRequest.getUserId() != null) {
            // 判断是否点赞
            Like likeVideo = likeService.lambdaQuery().eq(Like::getVideoId, videoActionRequest.getVideoId())
                    .eq(Like::getUserId, videoActionRequest.getUserId()).one();
            if (likeVideo != null) {
                tripleActionResponse.setLikeId(likeVideo.getLikeId());
            }

            // 是否收藏
            Favorite favoriteVideo = favoriteService.lambdaQuery()
                    .eq(Favorite::getVideoId, videoActionRequest.getVideoId())
                    .eq(Favorite::getUserId, videoActionRequest.getUserId()).one();
            if (favoriteVideo != null) {
                tripleActionResponse.setFavoriteId(favoriteVideo.getFavoriteId());
            }

            // 是否投币
            Coin coinVideo = coinService.lambdaQuery().eq(Coin::getVideoId, videoActionRequest.getVideoId())
                    .eq(Coin::getUserId, videoActionRequest.getUserId()).one();
            if (coinVideo != null) {
                tripleActionResponse.setCoin(true);
            }
        }
        return tripleActionResponse;
    }

    /**
     * 获取在线时评列表
     * 
     * @param videoId
     * @return
     */
    public List<OnlineBulletResponse> getVideoBullets(Long videoId) {
        return bulletService.getBulletList(videoId);
    }

    /**
     * 获取推荐时评列表
     * 
     * @param categoryId
     * @param videoId
     * @return
     */
    public List<VideoListResponse> getRecommendVideos(Integer categoryId, Long videoId) {
        return videoMapper.recommendVideoList(categoryId, videoId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TripleActionResponse tripleAction(VideoActionRequest videoActionRequest) {

        Long vid = videoActionRequest.getVideoId();
        Long uid = videoActionRequest.getUserId();

        boolean videoExists = this.lambdaQuery().eq(Video::getVideoId, vid).exists();
        if (!videoExists) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND_ERROR);
        }
        User user = userService.lambdaQuery().eq(User::getUserId, uid).one();

        UserStats userStats = userStatsService.lambdaQuery().eq(UserStats::getUserId, uid).one();

        if (user == null || userStats == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        boolean isLiked = likeService.lambdaQuery().eq(Like::getUserId, uid).eq(Like::getVideoId, vid).exists();

        boolean isFavorited = favoriteService.lambdaQuery().eq(Favorite::getUserId, uid).eq(Favorite::getVideoId, vid)
                .exists();

        boolean isCoin = coinService.lambdaQuery().eq(Coin::getUserId, uid).eq(Coin::getVideoId, vid).exists();
        // 开始三连
        TripleActionResponse response = new TripleActionResponse();
        Snowflake snowflake = IdUtil.getSnowflake(SnowFlakeConstants.MACHINE_ID, SnowFlakeConstants.DATA_CENTER_ID);
        LambdaUpdateWrapper<VideoStats> statsUpdate = new LambdaUpdateWrapper<VideoStats>().eq(VideoStats::getVideoId,
                vid);

        if (!isLiked) {
            Like like = new Like();
            like.setUserId(uid);
            like.setVideoId(vid);
            like.setLikeId(snowflake.nextId());
            boolean save = likeService.save(like);
            if (!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            response.setLikeId(like.getLikeId());
            statsUpdate.setSql("like_count = like_count + 1");
        }

        if (!isFavorited) {
            Favorite favorite = new Favorite();
            favorite.setVideoId(vid);
            favorite.setUserId(uid);
            favorite.setFavoriteId(snowflake.nextId());
            boolean save = favoriteService.save(favorite);
            if (!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            response.setFavoriteId(favorite.getFavoriteId());
            statsUpdate.setSql("favorite_count = favorite_count + 1");
        }

        if (!isCoin) {
            // 投币扣减并增加
            Coin coin = new Coin();
            coin.setVideoId(vid);
            coin.setUserId(uid);
            coin.setCoinId(snowflake.nextId());
            boolean save = coinService.save(coin);
            if (!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            boolean coinDeducted = userStatsService.lambdaUpdate().setSql("coin_count = coin_count - 1")
                    .eq(UserStats::getUserId, uid).update();
            if (!coinDeducted) {
                throw new BusinessException(ErrorCode.USER_COIN_ERROR, "投币失败，硬币不足");
            }

            response.setCoin(true);
            statsUpdate.setSql("coin_count = coin_count + 1");
        }

        if (isLiked & isFavorited & isCoin) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已经三连过了");
        }

        boolean statsUpdated = videoStatsService.update(statsUpdate);
        if (!statsUpdated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新视频统计信息失败");
        }

        return response;
    }

}
