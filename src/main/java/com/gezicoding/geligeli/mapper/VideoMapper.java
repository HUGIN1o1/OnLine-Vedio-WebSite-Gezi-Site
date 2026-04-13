package com.gezicoding.geligeli.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.vo.video.VideoDetailsResponse;
import com.gezicoding.geligeli.model.vo.video.VideoListResponse;

public interface VideoMapper extends BaseMapper<Video> {

    List<VideoListResponse> selectVideoWithStats(@Param("current") Integer current, @Param("pageSize") Integer pageSize);

    List<VideoListResponse> recommendVideoList(@Param("categoryId") Integer categoryId, @Param("videoId") Long vid);

    VideoDetailsResponse getVideoDetails(Long videoId);

    List<VideoListResponse> getSubmitVideoList(@Param("userId") Long userId);

    List<VideoListResponse> getCategoryVideoList(@Param("categoryId") Integer categoryId);
}
