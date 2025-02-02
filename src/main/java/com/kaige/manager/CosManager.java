package com.kaige.manager;

import cn.hutool.core.io.FileUtil;
import com.kaige.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传文件
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putObject(String key, File file){
        // 指定文件将要存放的存储桶
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucketName(), key, file);
        // 1.对图片进行处理（获取基本信息） 返回原图信息
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);

        List<PicOperations.Rule> rules = new ArrayList<>();
//        putObjectRequest.setPicOperations(picOperations);
        // 图片压缩 转为webp格式
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setBucket(cosClientConfig.getBucketName());
        rule.setRule("imageMogr2/format/webp");
        rule.setFileId(webpKey);
        rules.add(rule);
        // 缩略图处理
        PicOperations.Rule thumbnailUrlRule = new PicOperations.Rule();
        thumbnailUrlRule.setBucket(cosClientConfig.getBucketName());
        String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
        thumbnailUrlRule.setFileId(thumbnailKey);
        // 缩放规则 thumbnail/<Width>x<Height>> 如果大于原图宽高，就不处理
        thumbnailUrlRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>",128,128));

        rules.add(thumbnailUrlRule);


        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);

        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载文件
     * @param key
     * @return
     */
    public COSObject getObject(String key){
        return cosClient.getObject(cosClientConfig.getBucketName(), key);
    }

    /**
     * 删除文件
     * @param key
     */
    public void deleteObject(String key){
        cosClient.deleteObject(cosClientConfig.getBucketName(),key);
    }
}
