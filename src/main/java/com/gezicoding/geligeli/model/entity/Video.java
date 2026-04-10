package com.gezicoding.geligeli.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("video")
public class Video implements Serializable {

    /**
     * 视频 id
     */
    @TableId
    private Long videoId;

    /**
     * 文件 URL
     */
    private String fileUrl;

    /**
     * 封面 URL
     */
    private String coverUrl;

    /**
     * 投稿用户 ID
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 类型(1:自制 2:转载)
     */
    private Integer type;

    /**
     * 播放时长(秒)
     */
    private Double duration;

    /**
     * 分类 ID
     */
    private Integer categoryId;

    /**
     * 标签
     */
    private String tags;

    /**
     * 简介
     */
    private String description;

    /**
     * 状态(0:下架 1:审核中 2:已发布)
     */
    private Integer status;

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
