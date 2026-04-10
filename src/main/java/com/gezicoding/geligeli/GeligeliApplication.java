package com.gezicoding.geligeli;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gezicoding.geligeli.mapper")
public class GeligeliApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeligeliApplication.class, args);
    }

}
