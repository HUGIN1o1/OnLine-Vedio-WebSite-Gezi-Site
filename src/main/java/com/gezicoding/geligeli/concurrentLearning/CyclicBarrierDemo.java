package com.gezicoding.geligeli.concurrentLearning;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CyclicBarrierDemo    
 */
public class CyclicBarrierDemo {

    public static void main(String[] args) {
        int parties = 3;
        CyclicBarrier barrier = new CyclicBarrier(parties, () ->
                System.out.println("=== 所有线程已到达屏障，开始下一阶段 ==="));
        ExecutorService pool = Executors.newFixedThreadPool(parties);
        for (int i = 1; i <= parties; i++) {
            int workerId = i;
            pool.submit(() -> doWork(workerId, barrier));
        }
        pool.shutdown();
    }

    /**
     * doWork 方法
     * @param workerId
     * @param barrier
     */
    private static void doWork(int workerId, CyclicBarrier barrier) {
        try {
            System.out.println("Worker-" + workerId + " 第一阶段开始");
            Thread.sleep(500L * workerId);
            System.out.println("Worker-" + workerId + " 到达屏障点，等待其他线程");
            barrier.await();
            System.out.println("Worker-" + workerId + " 第二阶段开始");
            Thread.sleep(500L * workerId);
            System.out.println("Worker-" + workerId + " 到达第二屏障点，等待其他线程");
            barrier.await();

        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("Worker-" + workerId + " 完成任务");
        }
    }
}
