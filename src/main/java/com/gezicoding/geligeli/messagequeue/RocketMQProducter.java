package com.gezicoding.geligeli.messagequeue;

import org.springframework.stereotype.Component;

import jakarta.annotation.Resource; 
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RocketMQProducter {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /*
     * 发送消息
     * @param topic 主题
     * @param message 消息 JSON序列化的类
     */
    public void sendMessage(String topic, String message) {
        rocketMQTemplate.convertAndSend(topic, message);
    }
}
