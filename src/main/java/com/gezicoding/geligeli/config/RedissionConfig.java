package com.gezicoding.geligeli.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.Redisson;
    
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedissionConfig {

    private String host;

    private Integer port;

    private Integer database;

    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setPassword(password);
        return Redisson.create(config);
    }

}
