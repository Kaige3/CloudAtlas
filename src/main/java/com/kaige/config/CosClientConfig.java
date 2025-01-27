package com.kaige.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {

    /**
     * 域名
     */
    private String host;
    /**
     *
     */
    private String secretId;
    /**
     *  密鑰
     */
    private String secretKey;
    /**
     * 地域
     */
    private String region;
    /**
     * 存储桶名称
     */
    private String bucketName;

    @Bean
    public COSClient cosClient() {
        // 设置用户身份信息。
        BasicCOSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 设置bucket的区域
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 生成cos客户端
        return new COSClient(cred, clientConfig);
    }
}
