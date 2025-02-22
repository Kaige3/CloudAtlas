package com.kaige.manager.upload;

import cn.hutool.core.collection.CollUtil;
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
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;


    /**
     *
     * @param inputSource 图片来源(文件类型 & URL字符串)
     * @param uploadPathPrefix 上传路径前缀统一为：public/userId
     * @return
     */
    public final UploadPictureDto uploadPicture(Object inputSource,String uploadPathPrefix){
        // 1.校验图片
        validPicture(inputSource);
        // 2.图片上传地址
        String uuid = RandomUtil.randomString(8);
        String orinFilename = getOrinFileName(inputSource);

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
            log.info("===================="+imageInfo.getAve());
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if(CollUtil.isNotEmpty(objectList)){

                CIObject compressedCiObject = objectList.get(0);
                //默认为 压缩图
                CIObject thumbailCiObject = compressedCiObject;
                if (objectList.size() > 1){
                    // 获取缩略图
                     thumbailCiObject = objectList.get(1);
                }
                // 封装压缩图返回结果
                return buildResult(orinFilename,compressedCiObject,thumbailCiObject,imageInfo);

            }
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

    // 封装缩略图返回结果
    private UploadPictureDto buildResult(String orinFilename, CIObject compressedCiObject, CIObject thumbailCiObject, ImageInfo imageInfo) {
        UploadPictureDto uploadPictureDto = new UploadPictureDto();
        int width = compressedCiObject.getWidth();
        int height = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        uploadPictureDto.setPicWidth(width);
        uploadPictureDto.setPicColor(imageInfo.getAve());
        uploadPictureDto.setPicHeight(height);
        uploadPictureDto.setPicName(FileUtil.mainName(orinFilename));
        uploadPictureDto.setPicScale(picScale);
        uploadPictureDto.setPicFormat(compressedCiObject.getFormat());
        uploadPictureDto.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureDto.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        // 缩略图
        uploadPictureDto.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbailCiObject.getKey());
        return uploadPictureDto;
    }

    // 封装压缩图返回结果-已启用，thumbail更小
    private UploadPictureDto buildResult(String orinFilename, CIObject compressedCiObject) {
        UploadPictureDto uploadPictureDto = new UploadPictureDto();
        int width = compressedCiObject.getWidth();
        int height = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        uploadPictureDto.setPicWidth(width);
        uploadPictureDto.setPicHeight(height);
        uploadPictureDto.setPicName(FileUtil.mainName(orinFilename));
        uploadPictureDto.setPicScale(picScale);
        uploadPictureDto.setPicFormat(compressedCiObject.getFormat());
        uploadPictureDto.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureDto.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        return uploadPictureDto;
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

        uploadPictureDto.setPicColor(imageInfo.getAve());

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
