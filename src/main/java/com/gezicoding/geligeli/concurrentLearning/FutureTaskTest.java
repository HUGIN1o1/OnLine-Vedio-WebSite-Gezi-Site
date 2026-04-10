package com.gezicoding.geligeli.concurrentLearning;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FutureTaskTest {


    public static List<User> users = new ArrayList<>();

    /**
     * 内部用户类
     */
    public static class User {
        private String name;
        private String age;

        public User(String name, String age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }
    }

    public static void main(String[] args) {
        users.add(new User("张三", "18"));
        users.add(new User("李四", "20"));
        users.add(new User("王五", "22"));
        users.add(new User("赵六", "24"));
        users.add(new User("孙七", "26"));
        users.add(new User("周八", "32"));
        users.add(new User("吴九", "30"));
        users.add(new User("郑十", "32"));

        CompletableFuture<User> future = getUser("张三");
        CompletableFuture<User> future2 = getUser("李四");

        CompletableFuture<User> result = future.thenCombineAsync(future2, (user1, user2) -> {
            User newUser = new User(user1.getName() + user2.getName(), user1.getAge() + user2.getAge());
            return newUser;
        });

        result.thenAccept(user -> {
            System.out.println(user.getName() + " " + user.getAge());
        });
        result.handle((user, throwable) -> {
            if (throwable != null) {
                System.out.println(throwable.getMessage());
            } else {
                System.out.println(user.getName() + " " + user.getAge());
            }
            return user;
        });
        
    }

    public static CompletableFuture<User> getUser(String name) {
        return CompletableFuture.supplyAsync(() -> {
            return users.stream().filter(user -> user.getName().equals(name)).findFirst().orElse(null);
        });
    }

    


}
