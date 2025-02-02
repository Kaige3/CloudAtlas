package com.kaige;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.kaige.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class KPictureApplication {

    public static void main(String[] args) {
        SpringApplication.run(KPictureApplication.class, args);
    }

}
