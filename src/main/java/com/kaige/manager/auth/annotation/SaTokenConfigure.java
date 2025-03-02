package com.kaige.manager.auth.annotation;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    // 注册Sa-Token的拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
         registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

    @PostConstruct
    public void rewriteSaStrategy() {
        // 重写 Sa-Token 策略,增加注解合并功能
        SaAnnotationStrategy.instance.getAnnotation = (element, annotationClass) -> {
           return AnnotatedElementUtils.getMergedAnnotation(element, annotationClass);
        };
    }
}
