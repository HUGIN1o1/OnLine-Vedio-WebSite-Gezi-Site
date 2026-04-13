package com.gezicoding.geligeli.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("`like`")
public class Like implements Serializable {

    /**
     * 点赞 ID
     */
    @TableId
    private Long likeId;

    /**
     * 视频 ID
     */
    private Long videoId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
