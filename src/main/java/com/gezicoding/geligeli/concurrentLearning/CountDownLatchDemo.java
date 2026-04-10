package com.gezicoding.geligeli.concurrentLearning;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CountDownLatchDemo   
 */
public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        int workerCount = 3;
        CountDownLatch latch = new CountDownLatch(workerCount);
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);

        for (int i = 1; i <= workerCount; i++) {
            int workerId = i;
            pool.submit(() -> {
                try {
                    System.out.println("Worker-" + workerId + " 开始执行");
                    Thread.sleep(1000L * workerId); // 模拟耗时任务
                    System.out.println("Worker-" + workerId + " 执行完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Worker-" + workerId + " 被中断");
                } finally {
                    latch.countDown(); // 无论成功失败都要减 1
                }
            });
        }

        System.out.println("主线程等待子任务...");
        latch.await(); // 阻塞直到计数变成 0
        System.out.println("所有子任务完成，主线程继续执行");

        pool.shutdown();
    }
}