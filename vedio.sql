DROP TABLE IF EXISTS `file`;
CREATE TABLE `file`
(
    `file_id`     BIGINT                             NOT NULL COMMENT '文件 id',
    `file_hash`   VARCHAR(512)                       NOT NULL COMMENT '文件哈希值',
    `file_url`    VARCHAR(512)                       NOT NULL COMMENT '文件URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`   TINYINT  DEFAULT 0                 NOT NULL COMMENT '删除标记',
    PRIMARY KEY (`file_id`),
    UNIQUE KEY `idx_file_hash` (`file_hash`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文件表';


  DROP TABLE IF EXISTS `video`;
CREATE TABLE `video`
(
    `video_id`    BIGINT       NOT NULL COMMENT '视频 id',
    `file_url`    VARCHAR(512) NOT NULL COMMENT '文件URL',
    `cover_url`   VARCHAR(512) NOT NULL COMMENT '封面 url',
    `user_id`     BIGINT       NOT NULL COMMENT '投稿用户ID',
    `title`       VARCHAR(512) NOT NULL COMMENT '标题',
    `type`        TINYINT      NOT NULL DEFAULT 1 COMMENT '类型(1:自制 2:转载)',
    `duration`    DOUBLE       NOT NULL DEFAULT 0 COMMENT '播放时长(秒)',
    `category_id` INT UNSIGNED NOT NULL COMMENT '分类ID',
    `tags`        VARCHAR(512)          DEFAULT NULL COMMENT '标签',
    `description` TEXT                  DEFAULT NULL COMMENT '简介',
    `status`      TINYINT               DEFAULT 2 COMMENT '状态(0:下架 1:审核中 2:已发布)',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`   TINYINT               DEFAULT 0 NOT NULL COMMENT '删除标记',
    PRIMARY KEY (`video_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    KEY           `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='视频表';

DROP TABLE IF EXISTS `video_stats`;
CREATE TABLE `video_stats`
(
    `video_id`         BIGINT  NOT NULL COMMENT '视频ID',
    `view_count`       int(11) NOT NULL DEFAULT '0' COMMENT '播放量',
    `bullet_count`     int(11) NOT NULL DEFAULT '0' COMMENT '弹幕数',
    `like_count`       int(11) NOT NULL DEFAULT '0' COMMENT '点赞数',
    `coin_count`       int(11) NOT NULL DEFAULT '0' COMMENT '投币数',
    `collection_count` int(11) NOT NULL DEFAULT '0' COMMENT '收藏数',
    `comment_count`    int(11) NOT NULL DEFAULT '0' COMMENT '评论量',
    `create_time`      DATETIME         DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `update_time`      DATETIME         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`        TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`video_id`),
    UNIQUE KEY `video_id` (`video_id`),
    FOREIGN KEY (`video_id`) REFERENCES `video` (`video_id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='视频数据统计表';


  INSERT INTO `file` (
    `file_id`, 
    `file_hash`, 
    `file_url`, 
    `create_time`, 
    `update_time`, 
    `is_delete`
) VALUES (
    10001,  -- file_id：唯一标识，使用大于0的整数
    'a1b2c3d4e5f67890abcdef1234567890',  -- file_hash：符合唯一键约束的哈希值
    'https://example.com/files/report.mp4',  -- file_url：有效的文件URL
    '2025-08-02 15:30:45',  -- 创建时间
    '2025-08-02 15:30:45',  -- 更新时间（初始与创建时间一致）
    0  -- 未删除状态（0表示未删除，1表示已删除）
);


DROP TABLE IF EXISTS `bullet`;
CREATE TABLE `bullet`
(
    `bullet_id`     BIGINT       NOT NULL COMMENT '弹幕ID',
    `video_id`      BIGINT       NOT NULL COMMENT '视频ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `content`       VARCHAR(128) NOT NULL COMMENT '弹幕内容',
    `color`         VARCHAR(7)   NOT NULL DEFAULT '#FFFFFF' COMMENT '弹幕颜色 6位十六进制标准格式',
    `playback_time` DOUBLE       NOT NULL COMMENT '弹幕所在视频的时间点',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`bullet_id`),
    FOREIGN KEY (`video_id`) REFERENCES `video` (`video_id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='弹幕表';

DROP TABLE IF EXISTS `category`;
CREATE TABLE category
(
    `category_id`   INT(11) NOT NULL AUTO_INCREMENT COMMENT '分区id',
    `category_name` VARCHAR(64) NOT NULL COMMENT '分区名',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`category_id`),
    KEY             `idx_category` (`category_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='分区表';

insert into `category` (category_name)
values ('番剧'),
       ('国创'),
       ('综艺'),
       ('动画'),
       ('鬼畜'),
       ('舞蹈'),
       ('娱乐'),
       ('美食'),
       ('汽车'),
       ('体育'),
       ('电影'),
       ('电视剧'),
       ('游戏'),
       ('音乐'),
       ('动物'),
       ('情感'),
       ('户外'),
       ('时尚'),
       ('绘画'),
       ('健身'),
       ('风景'),
       ('亲情'),
       ('生活'),
       ('手工'),
       ('健康'),
       ('小剧场'),
       ('纪录片'),
       ('家装房产'),
       ('公益'),
       ('二次元');


       
       
DROP TABLE IF EXISTS `like`;
CREATE TABLE `like`
(
    `like_id`     BIGINT   NOT NULL COMMENT '点赞ID',
    `video_id`    BIGINT   NOT NULL COMMENT '视频ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`like_id`),
    UNIQUE KEY unique_video_user (video_id, user_id),
    FOREIGN KEY (`video_id`) REFERENCES `video` (`video_id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='点赞表';