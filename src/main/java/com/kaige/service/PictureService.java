package com.kaige.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kaige.config.CosClientConfig;
import com.kaige.model.dto.picture.*;
import com.kaige.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kaige.model.entity.User;
import com.kaige.model.vo.PictureVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
* @author 15336
* @description 针对表【picture】的数据库操作Service
* @createDate 2025-01-24 21:43:58
*/
public interface PictureService extends IService<Picture> {


    PictureVO uploadPicture(Object inputSource, PictureUploadDto pictureUploadDto, User loginUser);

    // 将查询请求转换为QueryWrapper对象
    QueryWrapper<Picture> getQueryWrapper(PictureQueryDto pictureQueryDto);

    // 获取 单张图片封装类
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    // 获取 多张图片封装类
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    // 图片数据校验
    void validPicture(Picture picture);

    // 图片审核
    void doPictureReview(PictureReviewDto pictureReviewDto,User loginUser);

    // 填充审核信息
    void fillReviewInfo(Picture picture, User loginUser);

    Integer uploadPictureByBatch(PictureUploadByBatchDto pictureUploadByBatchDto
    ,User loginUser);


    void deletePicture(long id, User loginUser);


    void editPicture(PictureEditDto editDto, HttpServletRequest request);

    void clearPictureFile(Picture picture);

    void checkPictureAuth(Picture pictureOld, User loginUser);
}
