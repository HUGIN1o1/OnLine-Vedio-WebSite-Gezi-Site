package com.gezicoding.geligeli.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.gezicoding.geligeli.dao.VideoEsDao;
import com.gezicoding.geligeli.constants.RedisConstant;
import com.gezicoding.geligeli.constants.VideoConstant;
import com.gezicoding.geligeli.dao.UserEsDao;
import com.gezicoding.geligeli.model.es.UserEs;
import com.gezicoding.geligeli.model.es.VideoEs;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.hutool.core.date.DateUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class CanalClient implements CommandLineRunner {


    @Autowired
    private CanalConnector canalConnector;
    @Autowired
    private UserEsDao userEsDao;
    @Autowired
    private VideoEsDao videoEsDao;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final int BATCH_SIZE = 500; // 每次拉取的消息条数

    private static final int MAX_RETRY_TIMES = 5; // 发生错误后最大重试次数

    private static final long INITIAL_RETRY_DELAY = 1000; // 初始重试延迟1秒

    private static final long MAX_RETRY_DELAY = 60000; // 最大重试延迟60秒

    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒发送一次心跳

    private static final long IDLE_CHECK_INTERVAL = 5000; // 5秒检查一次空闲状态

    private static final Set<String> MONITOR_TABLES = Set.of("user", "video", "video_stats", "user_stats", "bullet");


    @Override
    public void run(String... args) {
        new Thread(this::process).start();
    }

    private void process() {
        int batchSize = BATCH_SIZE;
        int retryTimes = 0;
        long retryDelay = INITIAL_RETRY_DELAY;
        long lastActiveTime = System.currentTimeMillis();

        // 每次查询Connector的健康状态
        while (true) {
            try {
                // 如果Connector不可用，则重新连接
                if (!canalConnector.checkValid()) {
                    reconnectCanal();
                    retryTimes = 0;
                    retryDelay = INITIAL_RETRY_DELAY;
                    lastActiveTime = System.currentTimeMillis();
                }

                // 通过健康检测来判断需不需要建立连接
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastActiveTime > HEARTBEAT_INTERVAL) {
                    if (probeHealth()) {
                        lastActiveTime = currentTime;
                    }
                    continue;
                }

                // 拉取消息 BatchSize 条
                Message message = canalConnector.getWithoutAck(batchSize);
                long batchId = message.getId();
                int size = message.getEntries().size();

                if (batchId == -1 || size == 0) {
                    Thread.sleep(IDLE_CHECK_INTERVAL);
                    continue;
                }

                // 完成消息取出操作进行更新活跃时间
                lastActiveTime = System.currentTimeMillis(); 

                try {
                    handleMessage(message.getEntries());
                    canalConnector.ack(batchId);
                    retryTimes = 0;
                    retryDelay = INITIAL_RETRY_DELAY;
                } catch (Exception e) {
                    log.error("处理消息内容出错，尝试回滚", e);
                    safeRollback(batchId);
                    throw e;
                }

            } catch (Exception e) {
                log.error("处理canal消息出错", e);

                if (retryTimes++ >= MAX_RETRY_TIMES) {
                    log.error("达到最大重试次数{}，等待后重新尝试", MAX_RETRY_TIMES);
                    retryTimes = 0;
                    try {
                        Thread.sleep(MAX_RETRY_DELAY);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }

                // 重试机制是二倍重试
                long sleepTime = Math.min(retryDelay * 2, MAX_RETRY_DELAY);
                log.warn("{}秒后尝试第{}次重连...", sleepTime / 1000, retryTimes);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                retryDelay = sleepTime;
            }
        }
    }

    /**
     * 重新连接Canal服务器
     * 
     * @throws Exception 如果连接失败，则抛出异常
     */
    private void reconnectCanal() throws Exception {
        try {
            canalConnector.disconnect();
            canalConnector.connect();
            canalConnector.subscribe();
            log.info("成功重新连接到Canal服务器");
        } catch (Exception e) {
            log.error("连接Canal服务器失败", e);
            throw e;
        }
    }

    /**
     * 健康探测（不进行ack位点操作）
     * 
     * @return true 表示当前连接健康，false 表示已触发重连但仍不可用
     */
    private boolean probeHealth() {
        try {
            if (canalConnector.checkValid()) {
                log.debug("Canal 健康探测通过");
                log.info("Canal 健康探测通过");
                return true;
            }
            log.warn("Canal 连接不可用，开始重连");
            reconnectCanal();
            return canalConnector.checkValid();
        } catch (Exception e) {
            log.error("Canal 健康探测失败", e);
            try {
                if (canalConnector.checkValid()) {
                    canalConnector.disconnect();
                }
            } catch (Exception ex) {
                log.error("断开连接出错", ex);
            }
            return false;
        }
    }

    private void handleMessage(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry == null || entry.getHeader() == null) {
                continue;
            }
            String tableName = entry.getHeader().getTableName();
            if (!MONITOR_TABLES.contains(tableName)) {
                continue;
            }
            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (InvalidProtocolBufferException e) {
                log.error("解析RowChange失败", e);
                continue;
            }

            System.out.println("表名：" + tableName);
            CanalEntry.EventType eventType = rowChange.getEventType();
            String schemaName = entry.getHeader().getSchemaName();
            // 例如：解析 rowChange 后按 eventType 分发到具体业务方法
            log.info("======> binlog[{}:{}], name[{},{}], eventType: {}", entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(), schemaName, tableName, eventType);

            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                switch (rowChange.getEventType()) {
                    case INSERT -> handleInsert(rowData.getAfterColumnsList(), tableName);
                    case UPDATE -> handleUpdate(rowData.getAfterColumnsList(), tableName);
                    case DELETE -> handleDelete(rowData.getBeforeColumnsList(), tableName);
                    default -> throw new IllegalStateException("Unknown event type: " + rowChange.getEventType());
                }
            }
        }
    }

    private void safeRollback(long batchId) {
        try {
            if (batchId > 0) {
                canalConnector.rollback(batchId);
            } else {
                canalConnector.rollback();
            }
        } catch (Exception rollbackEx) {
            log.error("回滚 batch 失败，batchId={}", batchId, rollbackEx);
        }
    }

    private void handleInsert(List<CanalEntry.Column> columns, String tableName) {
        Map<String, String> map = columns.stream().collect(Collectors.toMap(CanalEntry.Column::getName, CanalEntry.Column::getValue));
        log.info("======> 表名：{}，更新数据：{}", tableName, map);
        switch (tableName) {
            case "user" -> insertUser(map);
            case "video" -> insertVideo(map);
            case "bullet" -> insertBulletToRedis(map);
        }
    }

    private void insertBulletToRedis(Map<String, String> map) {

        String key = RedisConstant.VIDEO_KEY + map.get("video_id") + RedisConstant.BULLET_KEY;
        String uid = map.get("user_id");
        String id = map.get("bullet_id");
        String content = map.get("content");
        Double timePoint = Double.valueOf(map.get("playback_time"));
        String value = uid + ":" + id + ":" + content;
        try {
            stringRedisTemplate.opsForZSet().add(key, value, timePoint);
            stringRedisTemplate.expire(key, 72 * 3600 + ThreadLocalRandom.current().nextInt(3600), TimeUnit.SECONDS);
            log.info("Redis 插入弹幕成功: {}", value);
        } catch (Exception e) {
            log.error("Redis 插入失败: ", e);
        }
    }



    private void insertUser(Map<String, String> map) {
        try {
            UserEs userEs = new UserEs();
            userEs.setId(Long.valueOf(map.get("user_id")));
            userEs.setNickname(map.get("nickname"));
            userEs.setAvatar(map.get("avatar"));
            userEs.setDescription(map.get("description"));
            userEs.setFollowers(0);
            userEs.setVideoCount(0);
            UserEs save = userEsDao.save(userEs);
            log.info("ES 更新用户成功: {}", save);
        } catch (Exception e) {
            log.error("ES 更新用户失败: ", e);
        }
    }

    private void insertVideo(Map<String, String> map) {
        try {
            VideoEs videoEs = populateVideoEs(map);
            videoEs.setBulletCount(0);
            videoEs.setViewCount(0);
            VideoEs save = videoEsDao.save(videoEs);
            log.info("ES 更新视频成功: {}", save);
        } catch (Exception e) {
            log.error("ES 更新视频失败: ", e);
        }
    }

    
    private void handleUpdate(List<CanalEntry.Column> columns, String tableName) {
        Map<String, String> map = columns.stream().collect(Collectors.toMap(CanalEntry.Column::getName, CanalEntry.Column::getValue));
        log.info("======> 表名：{}，更新数据：{}", tableName, map);
        switch (tableName) {
            case "user" -> updateUser(map);
            case "video" -> updateVideo(map);
            case "user_stats" -> updateUserStats(map);
            case "video_stats" -> updateVideoStats(map);
        }
    }


    private void updateUser(Map<String, String> map) {
        try {
            UserEs userEs = new UserEs();
            userEs.setId(Long.valueOf(map.get("user_id")));
            userEs.setNickname(map.get("nickname"));
            userEs.setAvatar(map.get("avatar"));
            userEs.setDescription(map.get("description"));
            UserEs save = userEsDao.save(userEs);
            log.info("ES 更新用户成功: {}", save);
        } catch (Exception e) {
            log.error("ES 更新失败: ", e);
        }
    }


    private void updateVideo(Map<String, String> map) {
        try {
            VideoEs videoEs = populateVideoEs(map);
            VideoEs save = videoEsDao.save(videoEs);
            log.info("ES 更新视频成功: {}", save);
        } catch (Exception e) {
            log.error("ES 更新视频失败: ", e);
        }
    }


    /**
     * 更新视频统计到ES
     * @param map
     */
    private void updateVideoStats(Map<String, String> map) {
        try {
            Long videoId = Long.valueOf(map.get("video_id"));
            Integer viewCount = Integer.valueOf(map.get("view_count"));
            Integer bulletCount = Integer.valueOf(map.get("bullet_count"));

            Optional<VideoEs> optional = videoEsDao.findById(videoId);
            if (optional.isEmpty()) {
                log.warn("未找到 videoId={} 的文档，跳过更新", videoId);
                return;
            }

            VideoEs videoEs = optional.get();
            videoEs.setViewCount(viewCount);
            videoEs.setBulletCount(bulletCount);

            VideoEs saved = videoEsDao.save(videoEs);
            log.info("ES 局部更新视频统计成功: {}", saved);
        } catch (Exception e) {
            log.error("ES 更新视频统计失败", e);
        }
    }

    /**
     * 更新视频详情到Redis
     * @param map
     */
    private void updateVideoToRedis(Map<String, String> map) {
        try {
            String cacheKey = "videoDetails:" + map.get("video_id");
            if (stringRedisTemplate.hasKey(cacheKey)) {
                Map<String, String> videoDetails = stringRedisTemplate.opsForHash().entries(cacheKey).entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString(), (a, b) -> b, HashMap::new));
                String viewCount = map.get("view_count");
                String bulletCount = map.get("bullet_count");
                String likeCount = map.get("like_count");
                String coinCount = map.get("coin_count");
                String favoriteCount = map.get("favorite_count");
                String commentCount = map.get("comment_count");
                videoDetails.put("viewCount", viewCount);
                videoDetails.put("bulletCount", bulletCount);
                videoDetails.put("likeCount", likeCount);
                videoDetails.put("coinCount", coinCount);
                videoDetails.put("favoriteCount", favoriteCount);
                videoDetails.put("commentCount", commentCount);
                stringRedisTemplate.opsForHash().putAll(cacheKey, videoDetails);
                stringRedisTemplate.expire(cacheKey, VideoConstant.VIDEO_DETAIL_DAYS, TimeUnit.DAYS);
            }
    
            log.info("Redis 更新视频详情成功");
        } catch (Exception e) {
            log.error("Redis 更新视频详情失败", e);
        }
    }

    private void updateVideoToEs(Map<String, String> map) {
        try {
            Long videoId = Long.valueOf(map.get("video_id"));
            Integer viewCount = Integer.valueOf(map.get("view_count"));
            Integer bulletCount = Integer.valueOf(map.get("bullet_count"));

            Optional<VideoEs> optional = videoEsDao.findById(videoId);
            if (optional.isEmpty()) {
                log.warn("未找到 videoId={} 的文档，跳过更新", videoId);
                return;
            }
    
            VideoEs videoEs = optional.get();
            videoEs.setViewCount(viewCount);
            videoEs.setBulletCount(bulletCount);
    
            VideoEs saved = videoEsDao.save(videoEs);
            log.info("ES 局部更新视频统计成功: {}", saved);
        } catch (Exception e) {
            log.error("ES 更新视频统计失败", e);
        }
    }


    private void updateUserStats(Map<String, String> map) {
        updateVideoToEs(map);
        updateVideoToRedis(map);
    }

    private VideoEs populateVideoEs(Map<String, String> map) {
        VideoEs videoEs = new VideoEs();
        videoEs.setId(Long.valueOf(map.get("video_id")));
        videoEs.setCoverUrl(map.get("cover_url"));
        videoEs.setCreateTime(DateUtil.parse(map.get("create_time")));
        videoEs.setDuration(Double.valueOf(map.get("duration")));
        videoEs.setFileUrl(map.get("file_url"));
        videoEs.setNickName(map.get("nickname"));
        videoEs.setTitle(map.get("title"));
        videoEs.setUserId(Long.valueOf(map.get("user_id")));
        return videoEs;
    }




    private void handleDelete(List<CanalEntry.Column> columns, String tableName) {
        Map<String, String> map = new HashMap<>();
        for (CanalEntry.Column column : columns) {
            map.put(column.getName(), column.getValue());
        }
        switch (tableName) {
            case "bullet" -> deleteBulletToRedis(map);
            case "user" -> deleteUserToEs(map);
            case "video" -> deleteVideoToEs(map);
        }
    }

    private void deleteBulletToRedis(Map<String, String> map) {
        String key = RedisConstant.VIDEO_KEY + map.get("video_id") + RedisConstant.BULLET_KEY;
        String vid = map.get("video_id");
        String uid = map.get("user_id");
        String id = map.get("bullet_id");
        String content = map.get("content");
        String value = uid + ":" + id + ":" + content;
        stringRedisTemplate.opsForZSet().remove(key, value);
        log.info("Redis 删除弹幕成功: {}", map.get("bullet_id"));
    }

    private void deleteUserToEs(Map<String, String> map) {
        try {
            userEsDao.deleteById(Long.valueOf(map.get("user_id")));
            log.info("ES 删除用户成功: {}", map.get("user_id"));
        } catch (Exception e) {
            log.error("ES 删除用户失败: ", e);
        }
    }

    private void deleteVideoToEs(Map<String, String> map) {
        try {
            videoEsDao.deleteById(Long.valueOf(map.get("video_id")));
            log.info("ES 删除视频成功: {}", map.get("video_id"));
        } catch (Exception e) {
            log.error("ES 删除视频成功: ", e);
        }
    }
}
