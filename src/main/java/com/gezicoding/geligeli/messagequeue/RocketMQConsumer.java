package com.gezicoding.geligeli.messagequeue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Component;

import com.gezicoding.geligeli.model.dto.video.SendBulletRequest;
import com.gezicoding.geligeli.service.BulletService;

import cn.hutool.json.JSONUtil;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
@SuppressWarnings("all")
@RocketMQMessageListener(topic = "geligeli-topic", consumerGroup = "geligeli-consumer")
public class RocketMQConsumer implements RocketMQListener<String> {

    @Autowired
    private BulletService bulletService;

    @Override
    public void onMessage(String message) {
        SendBulletRequest request = JSONUtil.toBean(message, SendBulletRequest.class);
        log.info("收到消息: {}", request);

        if (bulletService.bulletExists(request.getBulletId())) {
            return;
        }

        try {
            bulletService.saveBulletToMySQL(request);
        } catch (Exception e) {
            log.error("保存到MySQL失败，消息ID: {}", request.getBulletId(), e);
            throw new RuntimeException("保存到MySQL失败", e);
        }
    }
}
