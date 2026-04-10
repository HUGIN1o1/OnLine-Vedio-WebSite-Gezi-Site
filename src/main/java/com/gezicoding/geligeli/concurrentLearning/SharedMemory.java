package com.gezicoding.geligeli.concurrentLearning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import cn.hutool.core.util.RandomUtil;

public class SharedMemory {
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReadLock readLock = readWriteLock.readLock();
    private final WriteLock writeLock = readWriteLock.writeLock();
    HashSet<Object> set = new HashSet<>();

    public void read() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " 读取数据");
            System.out.println(set);
        } finally {
            readLock.unlock();
        }
    }
    
    public void writePutObject(Object object) {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " 写入数据");
            set.add(object);
            System.out.println(set);
        } finally {
            writeLock.unlock();
        }
    }

    public void writeRemoveObject(Object object) {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " 删除数据");
            set.remove(object);
            System.out.println(set);
        } finally {
            writeLock.unlock();
        }
    }

    public static void main(String[] args) {
        // 创建共享内存
        SharedMemory sharedMemory = new SharedMemory();
        List<Thread> threads = new ArrayList<Thread>();
        // 创建生产者
        for (int i = 0; i < 10; i++) {
            Thread producer = new Thread(() -> {
                sharedMemory.writePutObject(RandomUtil.randomInt(1, 100));
            });
            threads.add(producer);
            Thread consumer = new Thread(() -> {
                sharedMemory.read();
            });
            threads.add(consumer);
        }
        // 创建消费者

        for (Thread thread : threads) {
            thread.start();
        }
        try {
        for (Thread thread : threads) {
            thread.join();
        } 
        } catch (InterruptedException e) {
            System.out.println("线程中断");
        }
    }
}
