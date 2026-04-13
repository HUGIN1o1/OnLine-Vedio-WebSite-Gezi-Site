package com.gezicoding.geligeli.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.model.vo.video.VideoListResponse;

public interface VideoMapper extends BaseMapper<Video> {

    List<VideoListResponse> getVideoList(@Param("current") Integer current, @Param("pageSize") Integer pageSize);
}
