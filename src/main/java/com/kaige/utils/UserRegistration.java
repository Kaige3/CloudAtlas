package com.kaige.utils;

import com.kaige.model.dto.user.UserRegisterDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;

@Deprecated
public class UserRegistration {

    private static final Logger logger = LoggerFactory.getLogger(UserRegistration.class);
    private static final String REGISTER_URL = "http://localhost:8123/api/user/register";  // 本地服务地址
    private static final int TOTAL_USERS = 1_000_000;  // 总用户数：100 万
    private static final int THREAD_POOL_SIZE = 50;   // 线程池大小（根据机器性能调整）

    public static void main(String[] args) {
        // 使用异步 HTTP 客户端
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();

        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // 开始时间
        long startTime = System.currentTimeMillis();

        // 提交注册任务
        for (int i = 23168; i < TOTAL_USERS; i++) {
            int userId = i;
            executorService.submit(() -> {
                UserRegisterDto userRegisterDto = generateUserDto(userId);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<UserRegisterDto> requestEntity = new HttpEntity<>(userRegisterDto, headers);

                // 发送异步请求
                asyncRestTemplate.postForEntity(REGISTER_URL, requestEntity, String.class)
                        .addCallback(
                                response -> logger.info("注册成功，用户ID: {}", response.getBody()),
                                ex -> logger.error("请求失败，用户账号: user{}", userId, ex)
                        );
            });
        }

        // 关闭线程池并等待任务完成
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.HOURS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        // 结束时间
        long endTime = System.currentTimeMillis();
        logger.info("所有注册任务完成，总耗时: {} 秒", (endTime - startTime) / 1000);
    }

    // 生成用户注册数据
    private static UserRegisterDto generateUserDto(int userId) {
        UserRegisterDto userRegisterDto = new UserRegisterDto();
        userRegisterDto.setUserAccount("user" + userId+"ge");
        userRegisterDto.setUserPassword("password" + userId);
        userRegisterDto.setCheckPassword("password" + userId);  // 密码和确认密码一致
        return userRegisterDto;
    }
}