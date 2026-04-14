package com.gezicoding.geligeli.model.dto.comment;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CancelVideoActionRequest implements Serializable {


    /**
     * 视频点赞，收藏，评论的 id
     */
    @NotNull(message = "id不能为空")
    private Long id;


    /**
     * 视频 id
     */
    @NotNull(message = "视频id不能为空")
    private Long videoId;
}