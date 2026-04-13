package com.gezicoding.geligeli.model.vo.video;

import java.io.Serializable;

import lombok.Data;


/**
 * 三连操作响应
 */
@Data
public class TripleActionResponse implements Serializable {

    /**
     * 点赞ID
     */
    private Long LikeId;


    /**
     * 投币
     */
    private boolean coin;


    /**
     * 收藏ID
     */
    private Long favoriteId;
}
