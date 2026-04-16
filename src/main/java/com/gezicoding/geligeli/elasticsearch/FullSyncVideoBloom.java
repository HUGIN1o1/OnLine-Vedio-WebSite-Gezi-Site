package com.gezicoding.geligeli.elasticsearch;

import java.util.List;

import com.gezicoding.geligeli.model.entity.Video;
import com.gezicoding.geligeli.service.VideoService;
import com.gezicoding.geligeli.utils.BitMapBloomUtil;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class FullSyncVideoBloom implements CommandLineRunner {

    @Resource
    private VideoService videoService;

    @Override
    public void run(String... args) {
        List<Video> videoList = videoService.list();

        log.info("FullSyncVideoToBloom start");
        for (Video video : videoList) {
            BitMapBloomUtil.add(video.getVideoId().toString());
            log.info("videoId {}", video.getVideoId());

        }
        log.info("FullSyncQuestionToEs end");
    }

}
