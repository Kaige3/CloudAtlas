package com.kaige.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kaige.Result.BaseResponse;
import com.kaige.Result.DeleteRequest;
import com.kaige.Result.ResultUtils;
import com.kaige.annotation.AuthCheck;
import com.kaige.constant.UserConstant;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.PictureTagCategory;
import com.kaige.model.dto.picture.PictureEditDto;
import com.kaige.model.dto.picture.PictureQueryDto;
import com.kaige.model.dto.picture.PictureUpdateDto;
import com.kaige.model.dto.picture.PictureUploadDto;
import com.kaige.model.entity.Picture;
import com.kaige.model.entity.User;
import com.kaige.model.vo.PictureVO;
import com.kaige.service.PictureService;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping
@Slf4j
public class PictureController {

    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;

    /**
     * 上传图片
     * @param file
     * @param uploadDto
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile file
    , PictureUploadDto uploadDto, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(file, uploadDto, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        if(deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取要删除的图片id
        Long id = deleteRequest.getId();
        Picture pictureOld = pictureService.getById(id);
        ThrowUtils.throwIf(pictureOld == null,ErrorCode.NOT_FOUND_ERROR);

        // 只有本人 和 管理员可以删除
        if(!pictureOld.getUserId().equals(loginUser.getId()) &&!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 操作数据库 删除
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（只有管理员可以使用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() <= 0){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 将实体类 和 封装类进行 转换
        // 接受的是 封装类，入库的是实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(updateDto,picture);

        // 将list 转为 string
        picture.setTags(JSONUtil.toJsonStr(updateDto.getTags()));

        // 数据校验
        pictureService.validPicture(picture);

        Long id = updateDto.getId();
        log.error(picture.toString());

        Picture byId = pictureService.getById(id);
        ThrowUtils.throwIf(byId == null,ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean b = pictureService.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片信息（管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id,HttpServletRequest request) {
        ThrowUtils.throwIf(id == null,ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture byId = pictureService.getById(id);
        ThrowUtils.throwIf(byId == null,ErrorCode.NOT_FOUND_ERROR);
        // 封装返回信息
        return ResultUtils.success(byId);
    }


    // 根据id获取图片信息，返回封装类
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id,HttpServletRequest request){
        ThrowUtils.throwIf(id == null,ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture byId = pictureService.getById(id);
        ThrowUtils.throwIf(byId == null,ErrorCode.NOT_FOUND_ERROR);
        // 封装返回信息
        PictureVO pictureVO = pictureService.getPictureVO(byId, request);
        return ResultUtils.success(pictureVO);
    }

    // 分页获取图片列表 （管理员）
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureVOByPage(@RequestBody PictureQueryDto queryDto){
        long current = queryDto.getCurrent();
        long pageSize = queryDto.getPageSize();
        // 查询数据库
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(queryDto));
        return ResultUtils.success(page);
    }

    // 分页获取封装图片列表
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryDto queryDto,HttpServletRequest request){
        long current = queryDto.getCurrent();
        long pageSize = queryDto.getPageSize();
        // 查询数据库
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(queryDto));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(page, request);
        return ResultUtils.success(pictureVOPage);
    }

    // 编辑图片
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditDto editDto, HttpServletRequest request) {
        if (editDto == null || editDto.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类 和 封装类进行 转换
        Picture picture = new Picture();
        BeanUtil.copyProperties(editDto, picture);
        // 接受的是 封装类，入库的是实体类
        // 将list 转为 string
        picture.setTags(JSONUtil.toJsonStr(editDto.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);

        // 判断是否存在
        Long id = editDto.getId();
        Picture byId = pictureService.getById(id);
        ThrowUtils.throwIf(byId == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        // 只有本人 和 管理员可以编辑
        if (!byId.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean b = pictureService.updateById(picture);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // 获取 标签和分类 先写死
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> getTagAndCategory(){
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "建筑", "卡通", "动物", "天空");
        List<String> categoryList = Arrays.asList("模板", "随机", "风景");
        pictureTagCategory.setTags(tagList);
        pictureTagCategory.setCategories(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

}
