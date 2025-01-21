package com.kaige;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kaige.mapper")
public class KPictureApplication {

    public static void main(String[] args) {
        SpringApplication.run(KPictureApplication.class, args);
    }

}
