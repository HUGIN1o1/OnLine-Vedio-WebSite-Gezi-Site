package com.gezicoding.geligeli.concurrentLearning;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 * SemaphareConnectionPool    
 */
public class SemaphareConnectionPool {
    int capacity;
    Semaphore semaphore;
    Queue<PooledConnection> connections;
        
    public SemaphareConnectionPool(String url, String username, String password, int capacity) {
        this.capacity = capacity;
        this.semaphore = new Semaphore(capacity);
        this.connections = new LinkedList<>();
        for (int i = 0; i < capacity; i++) {
            try {
                Connection connection = DriverManager.getConnection(url, username, password);
                connections.add(new PooledConnection(connection));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public PooledConnection getConnection() throws InterruptedException {
        semaphore.acquire();
        return connections.poll();
    }

    public void returnConnection(PooledConnection connection) {
        connections.offer(connection);
        semaphore.release();
    }

}
