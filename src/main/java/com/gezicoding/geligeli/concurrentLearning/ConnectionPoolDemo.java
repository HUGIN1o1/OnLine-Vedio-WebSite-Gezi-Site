package com.gezicoding.geligeli.concurrentLearning;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionPoolDemo {

    public static void main(String[] args) throws SQLException {
        String url = "jdbc:mysql://localhost:49152/geligeli?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "123456";
        int poolSize = 2;

        ConnectionPool pool = new ConnectionPool(url, username, password, poolSize);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (int i = 1; i <= 4; i++) {
            int workerId = i;
            executor.submit(() -> {
                PooledConnection pooledConnection = null;
                try {
                    System.out.println("Worker-" + workerId + " 尝试借连接");
                    pooledConnection = pool.borrowConnection();
                    System.out.println("Worker-" + workerId + " 借到连接, busy=" + pooledConnection.isBusy());
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (pooledConnection != null) {
                        try {
                            pool.returnConnection(pooledConnection);
                            System.out.println("Worker-" + workerId + " 已归还连接");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            });
        }

        executor.shutdown();
    }
}
