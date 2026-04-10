package com.gezicoding.geligeli.concurrentLearning;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import cn.hutool.core.util.RandomUtil;

public class LockRry {
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    private final Queue<Object> queue = new LinkedList<>();

    private int capacity = 0;

    private int maxCapacity = 10;

    public LockRry(int maxCapacity) {
        // this.capacity = capacity;
        this.maxCapacity = maxCapacity;
    }

    
    public void put(Object object) throws InterruptedException {
        lock.lock();

        try {
            while (capacity >= maxCapacity) {
                notFull.await();
            }
            queue.add(object);
            capacity++;
            System.out.println("生产者生产了: " + Thread.currentThread().getName() + " 生产了: " + object);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }


    public Object take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            Object object = queue.poll();
            System.out.println("消费者消费了: " + Thread.currentThread().getName() + " 消费了: " + object);
            capacity--;
            notFull.signal();
            return object;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LockRry lockRry = new LockRry(10);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // 生产者
            Thread producer = new Thread(() -> {
                try {
                    lockRry.put(RandomUtil.randomInt(1, 100));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            // 消费者
            Thread consumer = new Thread(() -> {
                try {
                    lockRry.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            threads.add(producer);
            threads.add(consumer);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        Thread.sleep(1000);
        return ;
    }

}
