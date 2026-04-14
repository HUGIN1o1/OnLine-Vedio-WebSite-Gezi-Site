package com.gezicoding.geligeli.utils;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.IntegerCodec;

import java.time.Instant;
import java.util.Collections;

@Service
public class CounterUtil {

    @Resource
    private RedissonClient redissonClient;
    

    /**
     * 增加并返回计数器
     * @param key
     * @param timeInterval
     * @param timeUnit
     * @param expirationTimeInSeconds
     * @return
     */
    public long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit, long expirationTimeInSeconds) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        // 根据时间粒度生成 Redis Key
        long timeFactor;
        switch (timeUnit) {
            case SECONDS:
                timeFactor = Instant.now().getEpochSecond() / timeInterval;
                break;
            case MINUTES:
                timeFactor = Instant.now().getEpochSecond() / timeInterval / 60;
                break;
            case HOURS:
                timeFactor = Instant.now().getEpochSecond() / timeInterval / 3600;
                break;
            default:
                throw new IllegalArgumentException("不支持的单位");
        }

        String redisKey = key + ":" + timeFactor;

        // Lua 脚本
        String luaScript =
                "if redis.call('exists', KEYS[1]) == 1 then " +
                        "  return redis.call('incr', KEYS[1]); " +
                        "else " +
                        "  redis.call('set', KEYS[1], 1); " +
                        "  redis.call('expire', KEYS[1], ARGV[1]); " +
                        "  return 1; " +
                        "end";

        // 执行 Lua 脚本
        RScript script = redissonClient.getScript(IntegerCodec.INSTANCE);
        Object countObj = script.eval(
                RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(redisKey), expirationTimeInSeconds
        );
        return (long) countObj;
    }
}