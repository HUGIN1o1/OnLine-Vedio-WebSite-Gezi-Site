package com.gezicoding.geligeli.concurrentLearning;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class SemaphoreDemo {

    public static void main(String[] args) {
        int permits = 3;
        int tasks = 6;
        Semaphore semaphore = new Semaphore(permits);
        ExecutorService pool = Executors.newFixedThreadPool(tasks);

        for (int i = 1; i <= tasks; i++) {
            int workerId = i;
            pool.submit(() -> {
                boolean acquired = false;
                try {
                    System.out.println("Worker-" + workerId + " 尝试获取许可");
                    semaphore.acquire();
                    acquired = true;
                    System.out.println("Worker-" + workerId + " 获取许可，正在执行任务");
                    Thread.sleep(1200);
                    System.out.println("Worker-" + workerId + " 任务执行完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Worker-" + workerId + " 被中断");
                } finally {
                    if (acquired) {
                        semaphore.release();
                        System.out.println("Worker-" + workerId + " 已释放许可");
                    }
                }
            });
        }

        pool.shutdown();
    }
}
