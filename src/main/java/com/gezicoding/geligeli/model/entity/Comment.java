package com.gezicoding.geligeli.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("comment")
public class Comment implements Serializable {

    /**
     * 评论 ID
     */
    @TableId
    private Long commentId;

    /**
     * 视频 ID
     */
    private Long videoId;

    /**
     * 评论用户 ID
     */
    private Long userId;

    /**
     * 父评论 ID（为空表示一级评论）
     */
    private Long parentCommentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
