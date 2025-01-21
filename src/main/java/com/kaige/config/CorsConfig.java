package com.kaige.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CorsConfig implements WebMvcConfigurer {


    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        // 允许所有请求
        registry.addMapping("/**")
                // 放行一下域名
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许发送Cookie信息
                .allowCredentials(true)
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}
