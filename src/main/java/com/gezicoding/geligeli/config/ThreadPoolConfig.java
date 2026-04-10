package com.gezicoding.geligeli.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gezicoding.geligeli.constants.ThreadPoolConstant;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService fileThreadPool() {
        return new ThreadPoolExecutor(
            ThreadPoolConstant.CORE_POOL_SIZE, 
            ThreadPoolConstant.MAX_POOL_SIZE, 
            ThreadPoolConstant.KEEP_ALIVE_TIME, 
            TimeUnit.SECONDS, 
            new LinkedBlockingQueue<>(ThreadPoolConstant.QUEUE_CAPACITY),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
