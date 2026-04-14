package com.gezicoding.geligeli.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("follow")
public class Follow implements Serializable {

    /**
     * 关注 ID
     */
    @TableId
    private Long followId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 被关注用户 ID
     */
    private Long creatorId;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
