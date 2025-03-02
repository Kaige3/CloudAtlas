package com.kaige.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kaige.manager.auth.SpaceUserAuthContext;
import com.kaige.manager.auth.SpaceUserAuthManager;
import com.kaige.manager.auth.StpKit;
import com.kaige.manager.auth.annotation.SaSpaceCheckPermission;
import com.kaige.manager.auth.constant.SpaceUserPermissionsConstant;
import com.kaige.result.BaseResponse;
import com.kaige.result.DeleteRequest;
import com.kaige.result.ResultUtils;
import com.kaige.annotation.AuthCheck;
import com.kaige.api.aliyunai.AliYunApi;
import com.kaige.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.kaige.api.searchPicby360.SoImageSearchApiFacade;
import com.kaige.api.searchPicby360.model.SoImageSearchDto;
import com.kaige.constant.UserConstant;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.PictureTagCategory;
import com.kaige.model.dto.picture.*;
import com.kaige.model.entity.Picture;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.User;
import com.kaige.model.enums.PictureReviewStatusEnum;
import com.kaige.model.vo.PictureVO;
import com.kaige.service.PictureService;
import com.kaige.service.SpaceService;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunApi aliYunApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    private final Cache<String,String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();


    // 以图搜图
    @PostMapping("/search/picture")
    public BaseResponse<List<SoImageSearchDto>> searchPicture(@RequestBody SearchPictureByPictureDto pictureQueryDto){
        Picture picture = pictureService.getById(pictureQueryDto.getId());
        String url = picture.getUrl();
        List<SoImageSearchDto> soImageSearchDtos = SoImageSearchApiFacade.searchImage(url, 0);
        return ResultUtils.success(soImageSearchDtos);
    }
    // 多级缓存
    @PostMapping("/list/page/vo/Caffeinecache3")
    public BaseResponse<Page<PictureVO>> listPictureVoByPageWithCaffeineCacheAndRedis(
            @RequestBody PictureQueryDto pictureQueryDto,HttpServletRequest request){

        long current = pictureQueryDto.getCurrent();
        long pageSize = pictureQueryDto.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20,ErrorCode.PARAMS_ERROR);
        // 普通用户只能查看过审的数据
        pictureQueryDto.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 构建缓存 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryDto);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());

        // 构建缓存key
        String CacheKey = "kaige:listPictureVOByPage:" + hashKey;

        // 1.从本地缓存中查询(Caffeine)
        String  cacheValue = LOCAL_CACHE.getIfPresent(CacheKey);
        if(cacheValue != null){
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(pictureVOPage);
        }

        // 2.从redis中查询
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
         cacheValue = valueOps.get(CacheKey);
        if(cacheValue!= null){
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(pictureVOPage);
        }


        // 从数据查询
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryDto));
        // 转换为VO
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(page, request);
        // 更新缓存
        String cachedValue = JSONUtil.toJsonStr(pictureVOPage);
        LOCAL_CACHE.put(CacheKey,cachedValue);
        // 3.存入redis
        valueOps.set(CacheKey,cachedValue,5L, TimeUnit.MINUTES);
        return ResultUtils.success(pictureVOPage);
    }

    // 本地缓存
    @PostMapping("/list/page/vo/Caffeinecache2")
    public BaseResponse<Page<PictureVO>> listPictureVoByPageWithCaffeineCache(
            @RequestBody PictureQueryDto pictureQueryDto,HttpServletRequest request){

        long current = pictureQueryDto.getCurrent();
        long pageSize = pictureQueryDto.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20,ErrorCode.PARAMS_ERROR);
        // 普通用户只能查看过审的数据
        pictureQueryDto.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 构建缓存 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryDto);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());

        // 本地缓存天然隔离服务器，可以移除前缀 kaige:

        String CacheKey = "listPictureVOByPage:" + hashKey;

        // 从本地缓存中查询
        String  cacheValue= LOCAL_CACHE.getIfPresent(CacheKey);

        if(cacheValue != null){
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(pictureVOPage);
        }

        // 从数据查询
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryDto));
        // 转换为VO
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(page, request);
        // 存入本地缓存
        String cachedValue = JSONUtil.toJsonStr(pictureVOPage);

        return ResultUtils.success(pictureVOPage);
    }

    /**
     * redis缓存
     * @param pictureQueryDto
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVoByPageWithCache(
            @RequestBody PictureQueryDto pictureQueryDto,HttpServletRequest request){
        long current = pictureQueryDto.getCurrent();
        long pageSize = pictureQueryDto.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20,ErrorCode.PARAMS_ERROR);
        // 普通用户只能查看过审的数据
        pictureQueryDto.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 构建缓存 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryDto);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = "kaige:listPictureVOByPage:" + hashKey;

        // 从redis中查询
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        String cacheValue = valueOps.get(redisKey);
        if(cacheValue != null){
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(pictureVOPage);
        }

        // 从数据查询
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryDto));
        // 转换为VO
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(page, request);
        // 存入redis
        String redisValue = JSONUtil.toJsonStr(pictureVOPage);
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        valueOps.set(redisKey,redisValue,cacheExpireTime);

        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 上传图片
     * @param file
     * @param uploadDto
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionsConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile file
    ,@ModelAttribute  PictureUploadDto uploadDto, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(file, uploadDto, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionsConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadDto uploadDto, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        String fileUrl = uploadDto.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, uploadDto, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionsConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        if(deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        
        // 获取要删除的图片id
        pictureService.deletePicture(id, loginUser);
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
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null,ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
//        if (picture.getSpaceId() != null){
//            User loginUser = userService.getLoginUser(request);
//            pictureService.checkPictureAuth(picture,loginUser);
//        }
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null){

            boolean permission = StpKit.SPACE.hasPermission(SpaceUserPermissionsConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!permission,ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        }
        //获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 封装返回信息
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
    }

    // 分页获取图片列表 （管理员）
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryDto queryDto){
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
        Long spaceId = queryDto.getSpaceId();
        // 公开图库
        if (spaceId == null){
            queryDto.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            queryDto.setSpaceIdIsNull(true);
        }else {
        // 私有空间
//            User loginUser = userService.getLoginUser(request);
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
//            if(!loginUser.getId().equals(space.getUserId())){
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
//            }
            boolean permission = StpKit.SPACE.hasPermission(SpaceUserPermissionsConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!permission,ErrorCode.NO_AUTH_ERROR);

        }
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(queryDto));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(page, request);
        return ResultUtils.success(pictureVOPage);
    }

    // 编辑图片-用户使用
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionsConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditDto editDto, HttpServletRequest request) {
        if (editDto == null || editDto.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (editDto.getTags() == null  || editDto.getCategory() == null || editDto.getIntroduction() == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请填充信息");
        }
        pictureService.editPicture(editDto, request);
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

    /**
     * 审核图片
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewDto reviewDto, HttpServletRequest request){
        ThrowUtils.throwIf(reviewDto == null,ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取要审核的图片id
        pictureService.doPictureReview(reviewDto,loginUser);
        return ResultUtils.success(true);
    }

    // 批量抓取图片
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchDto pictureUploadByBatchDto, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        Integer count = pictureService.uploadPictureByBatch(pictureUploadByBatchDto,loginUser);
        return ResultUtils.success(count);
    }

    // 按照图片颜色搜索
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionsConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorDto searchPictureByColorDto,HttpServletRequest request){
        ThrowUtils.throwIf(searchPictureByColorDto == null,ErrorCode.PARAMS_ERROR,"缺少参数");
        Long spaceId = searchPictureByColorDto.getSpaceId();
        String picColor = searchPictureByColorDto.getPicColor();
        ThrowUtils.throwIf(picColor == null || spaceId == null,ErrorCode.PARAMS_ERROR,"颜色格式错误");
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> pictureVOS = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOS);
    }

    // 批量修改图片信息
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionsConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> batchEditePicture(@RequestBody BatchEditePictureDto batchEditePictureDto,HttpServletRequest request){
        ThrowUtils.throwIf(batchEditePictureDto == null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null,ErrorCode.NOT_LOGIN_ERROR);
        pictureService.batchEditePicture(batchEditePictureDto,loginUser);
        return ResultUtils.success(true);
    }

    // ai阔图
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionsConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest createOutPaintingTaskRequest, HttpServletRequest request){
        if (createOutPaintingTaskRequest == null || createOutPaintingTaskRequest.getPictureId() == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createOutPaintingTask(createOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    // 查询阔图任务
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> GetOutPaintingTask(@RequestParam("taskId") String taskId, HttpServletRequest request){
        if (taskId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        GetOutPaintingTaskResponse outPaintingTask = aliYunApi.createOutPaintingTask(taskId);
        return ResultUtils.success(outPaintingTask);
    }



}
