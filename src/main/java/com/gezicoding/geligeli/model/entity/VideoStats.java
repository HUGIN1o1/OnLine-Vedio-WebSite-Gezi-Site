package com.gezicoding.geligeli.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("video_stats")
public class VideoStats implements Serializable {

    /**
     * 视频 ID
     */
    @TableId
    private Long videoId;

    /**
     * 播放量
     */
    private Integer viewCount;

    /**
     * 弹幕数
     */
    private Integer bulletCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 投币数
     */
    private Integer coinCount;

    /**
     * 收藏数
     */
    private Integer favoriteCount;

    /**
     * 评论量
     */
    private Integer commentCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标记
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
