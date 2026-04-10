package com.gezicoding.geligeli.concurrentLearning;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConnectionPool {

    private final BlockingQueue<PooledConnection> availableConnections;
    private final List<PooledConnection> allConnections;

    public ConnectionPool(String url, String username, String password, int poolSize) throws SQLException {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize 必须大于 0");
        }
        this.availableConnections = new LinkedBlockingQueue<>(poolSize);
        this.allConnections = new ArrayList<>(poolSize);

        for (int i = 0; i < poolSize; i++) {
            Connection connection = DriverManager.getConnection(url, username, password);
            PooledConnection pooled = new PooledConnection(connection);
            allConnections.add(pooled);
            availableConnections.offer(pooled);
        }
    }

    /**
     * 阻塞借连接：无可用连接时等待。
     */
    public PooledConnection borrowConnection() throws InterruptedException {
        PooledConnection pooledConnection = availableConnections.take();
        pooledConnection.markBusy();
        return pooledConnection;
    }

    /**
     * 带超时借连接：超时返回 null。
     */
    public PooledConnection borrowConnection(long timeout, TimeUnit unit) throws InterruptedException {
        PooledConnection pooledConnection = availableConnections.poll(timeout, unit);
        if (pooledConnection != null) {
            pooledConnection.markBusy();
        }
        return pooledConnection;
    }

    public void returnConnection(PooledConnection pooledConnection) throws InterruptedException {
        if (pooledConnection == null) {
            return;
        }
        pooledConnection.markIdle();
        availableConnections.put(pooledConnection);
    }

    public int getAvailableCount() {
        return availableConnections.size();
    }

    public int getTotalCount() {
        return allConnections.size();
    }

    public void shutdown() {
        for (PooledConnection pooledConnection : allConnections) {
            try {
                pooledConnection.getConnection().close();
            } catch (SQLException e) {
            } finally {
                pooledConnection.markIdle();
            }
        }
        availableConnections.clear();
    }
}
