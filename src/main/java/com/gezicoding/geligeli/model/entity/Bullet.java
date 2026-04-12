package com.gezicoding.geligeli.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("bullet")
public class Bullet implements Serializable {

    /**
     * 弹幕 ID
     */
    @TableId
    private Long bulletId;

    /**
     * 视频 ID
     */
    private Long videoId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 弹幕内容
     */
    private String content;

    /**
     * 弹幕颜色，6 位十六进制标准格式
     */
    private String color;

    /**
     * 弹幕所在视频时间点
     */
    private Double playbackTime;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
