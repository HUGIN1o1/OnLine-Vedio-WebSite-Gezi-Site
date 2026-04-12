package com.gezicoding.geligeli.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.dto.video.DeleteBulletRequest;
import com.gezicoding.geligeli.model.dto.video.SendBulletRequest;
import com.gezicoding.geligeli.model.entity.Bullet;

public interface BulletService extends IService<Bullet> {

    boolean deleteBullet(DeleteBulletRequest deleteBulletRequest);

    void saveBulletToMySQL(SendBulletRequest sendBulletRequest);
}
