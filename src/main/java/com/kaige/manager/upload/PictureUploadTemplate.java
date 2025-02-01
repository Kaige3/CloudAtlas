package com.kaige.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.kaige.config.CosClientConfig;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.manager.CosManager;
import com.kaige.model.dto.file.UploadPictureDto;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;

@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;


    public final UploadPictureDto uploadPicture(Object inputSource,String uploadPathPrefix){
        // 1.校验图片
        validPicture(inputSource);
        // 2.图片上传地址
        String uuid = RandomUtil.randomString(8);
        String orinFilename = getOrinFileName(inputSource);

        // TODO 感觉有一个bug
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(orinFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        // 3.创建临时文件
        try {
            file = File.createTempFile(uploadPath,null);
            // 处理文件来源（本地或者URL）
            processFile(inputSource,file);
            // 4.上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 5.封装返回结果
            return buildResult(orinFilename,file,uploadPath,imageInfo);
        } catch (Exception e) {
            log.error("上传图片失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传图片失败");
        }finally {
            // 6.清理临时文件
           deleteTimeFile(file);
        }
    }

    // 清理临时文件
    private void deleteTimeFile(File file) {
        if(file != null){
            boolean delete = file.delete();
            if(!delete){
                log.error("文件删除失败，路径：{}",file.getPath());
            }
        }
    }

    // 封装返回结果
    private UploadPictureDto buildResult(String orinFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureDto uploadPictureDto = new UploadPictureDto();
        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        uploadPictureDto.setPicWidth(width);
        uploadPictureDto.setPicHeight(height);
        uploadPictureDto.setPicName(FileUtil.mainName(orinFilename));
        uploadPictureDto.setPicScale(picScale);
        uploadPictureDto.setPicFormat(imageInfo.getFormat());
        uploadPictureDto.setPicSize(FileUtil.size(file));
        uploadPictureDto.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureDto;
    }

    // 处理文件来源（本地或者URL）
    protected abstract void processFile(Object inputSource, File file) throws IOException;

    // 获取原始文件名
    protected abstract String getOrinFileName(Object inputSource);

    // 校验图片
    protected abstract void validPicture(Object inputSource);

}
