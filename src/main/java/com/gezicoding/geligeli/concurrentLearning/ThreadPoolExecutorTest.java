package com.gezicoding.geligeli.concurrentLearning;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutorTest {
    public static class TaskWorker implements Runnable {
        public TaskWorker() {
            // this.name = name;
        }
        @Override
        public void run() {
            ThreadLocal<String> threadLocal = new ThreadLocal<>();
            System.out.println(Thread.currentThread().getName() + " is running");
            try {
                Thread.sleep(100);
                System.out.println(Thread.currentThread().getName() + " is finished");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } 
    }

    public static void main(String[] args) {
        
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            10, 
            20, 
            0L, 
            TimeUnit.MILLISECONDS, 
            new ArrayBlockingQueue<Runnable>(30),
            new ThreadPoolExecutor.AbortPolicy()
        );

        try{

            for (int i = 0; i < 100; i++) {
                // CompletableFuture completableFuture 
                threadPoolExecutor.submit(new TaskWorker());
            }
        } finally {
                threadPoolExecutor.shutdown();
        }
    }
}
