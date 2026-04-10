package com.gezicoding.geligeli.concurrentLearning;

import java.sql.Connection;

public class PooledConnection {

    private final Connection connection;

    /**
     * 标记该连接是否被借出（业务使用中）。
     */
    private volatile boolean busy;

    public PooledConnection(Connection connection) {
        this.connection = connection;
        this.busy = false;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isBusy() {
        return busy;
    }

    public void markBusy() {
        this.busy = true;
    }

    public void markIdle() {
        this.busy = false;
    }
}
