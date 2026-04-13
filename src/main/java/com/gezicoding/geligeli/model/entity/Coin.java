package com.gezicoding.geligeli.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("coin")
public class Coin implements Serializable {

    /**
     * 投币 ID
     */
    @TableId
    private Long coinId;

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
