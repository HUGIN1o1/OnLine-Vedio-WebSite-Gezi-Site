package com.gezicoding.geligeli.model.dto.video;

import lombok.Data;

@Data
public class DeleteBulletRequest {

    private Long videoId;

    private Long userId;

    private Long bulletId;

}
