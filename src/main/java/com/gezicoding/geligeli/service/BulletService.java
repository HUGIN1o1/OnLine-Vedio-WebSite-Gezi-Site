package com.gezicoding.geligeli.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.dto.video.DeleteBulletRequest;
import com.gezicoding.geligeli.model.dto.video.SendBulletRequest;
import com.gezicoding.geligeli.model.entity.Bullet;
import com.gezicoding.geligeli.model.vo.video.OnlineBulletResponse;

public interface BulletService extends IService<Bullet> {

    boolean deleteBullet(DeleteBulletRequest deleteBulletRequest);

    void saveBulletToMySQL(SendBulletRequest sendBulletRequest);
    
    List<OnlineBulletResponse> getBulletList(Long videoId);

}
