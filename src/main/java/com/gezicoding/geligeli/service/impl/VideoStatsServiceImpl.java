package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.mapper.VideoStatsMapper;
import com.gezicoding.geligeli.model.entity.VideoStats;
import com.gezicoding.geligeli.service.VideoStatsService;
import org.springframework.stereotype.Service;

@Service
public class VideoStatsServiceImpl extends ServiceImpl<VideoStatsMapper, VideoStats> implements VideoStatsService {
}
