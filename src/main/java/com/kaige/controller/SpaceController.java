package com.kaige.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kaige.manager.auth.SpaceUserAuthManager;
import com.kaige.result.BaseResponse;
import com.kaige.result.DeleteRequest;
import com.kaige.result.ResultUtils;
import com.kaige.annotation.AuthCheck;
import com.kaige.constant.UserConstant;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.space.*;
import com.kaige.model.entity.Space;
import com.kaige.model.entity.User;
import com.kaige.model.enums.SpaceLevelEnum;
import com.kaige.model.vo.SpaceLevel;
import com.kaige.model.vo.SpaceVO;
import com.kaige.service.SpaceService;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
@Slf4j
public class SpaceController {

    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


//    private final Cache<String,String> LOCAL_CACHE =
//            Caffeine.newBuilder().initialCapacity(1024)
//                    .maximumSize(10000L)
//                    .expireAfterWrite(5L, TimeUnit.MINUTES)
//                    .build();

    // 新增空间
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddDto spaceAddDto,HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddDto == null, ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用service
        long newId = spaceService.addSpace(spaceAddDto, loginUser);
        return ResultUtils.success(newId);
    }

    /**
     * redis缓存
     * @param spaceQueryDto
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<SpaceVO>> listSpaceVoByPageWithCache(
            @RequestBody SpaceQueryDto spaceQueryDto,HttpServletRequest request){

        long current = spaceQueryDto.getCurrent();
        long pageSize = spaceQueryDto.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20,ErrorCode.PARAMS_ERROR);
        // 普通用户只能查看过审的数据
//        spaceQueryDto.setReviewStatus(SpaceReviewStatusEnum.PASS.getValue());

        // 构建缓存 key
        String queryCondition = JSONUtil.toJsonStr(spaceQueryDto);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = "kaige:listSpaceVOByPage:" + hashKey;

        // 从redis中查询
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        String cacheValue = valueOps.get(redisKey);
        if(cacheValue != null){
            Page<SpaceVO> spaceVOPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(spaceVOPage);
        }

        // 从数据查询
        Page<Space> page = spaceService.page(new Page<>(current,pageSize), spaceService.getQueryWrapper(spaceQueryDto));
        // 转换为VO
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(page, request);
        // 存入redis
        String redisValue = JSONUtil.toJsonStr(spaceVOPage);
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        valueOps.set(redisKey,redisValue,cacheExpireTime);

        return ResultUtils.success(spaceVOPage);
    }

    /**
     * 删除空间
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        if(deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取要删除的空间id
        Long id = deleteRequest.getId();
        Space spaceOld = spaceService.getById(id);
        ThrowUtils.throwIf(spaceOld == null,ErrorCode.NOT_FOUND_ERROR);

        // 只有本人 和 管理员可以删除
        if(!spaceOld.getUserId().equals(loginUser.getId()) &&!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 操作数据库 删除
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新空间（只有管理员可以使用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateDto updateDto,HttpServletRequest request) {

        if (updateDto == null || updateDto.getId() <= 0){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 将实体类 和 封装类进行 转换
        // 接受的是 封装类，入库的是实体类
        Space space = new Space();
        BeanUtil.copyProperties(updateDto,space);

        // 将list 转为 string
//        space.setTags(JSONUtil.toJsonStr(updateDto.getTags()));

        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space,false);

        Long id = updateDto.getId();
        log.error(space.toString());

        Space byId = spaceService.getById(id);
        ThrowUtils.throwIf(byId == null,ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        boolean b = spaceService.updateById(space);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取空间信息（管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(Long id,HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0,ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space byId = spaceService.getById(id);
        ThrowUtils.throwIf(byId == null,ErrorCode.NOT_FOUND_ERROR);
        // 封装返回信息
        return ResultUtils.success(byId);
    }


    // 根据id获取空间信息，返回封装类
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(Long id,HttpServletRequest request){
        ThrowUtils.throwIf(id <= 0,ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null,ErrorCode.NOT_FOUND_ERROR);
        // 封装返回信息
        SpaceVO spaceVO = spaceService.getSpaceVO(space);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }

    // 分页获取空间列表 （管理员）
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryDto queryDto){
        int current = queryDto.getCurrent();
        int pageSize = queryDto.getPageSize();
        // 查询数据库
        Page<Space> page = spaceService.page(new Page<>(current,pageSize), spaceService.getQueryWrapper(queryDto));
        return ResultUtils.success(page);
    }

    // 分页获取封装空间列表
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryDto queryDto,HttpServletRequest request){
        int current = queryDto.getCurrent();
        int pageSize = queryDto.getPageSize();
        // 查询数据库
        Page<Space> page = spaceService.page(new Page<>(current,pageSize), spaceService.getQueryWrapper(queryDto));
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(page, request);
        return ResultUtils.success(spaceVOPage);
    }

    // 编辑空间
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditDto editDto, HttpServletRequest request) {

        if (editDto == null || editDto.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类 和 封装类进行 转换
        Space space = new Space();
        BeanUtil.copyProperties(editDto, space);

        // 自动填充信息
        spaceService.fillSpaceBySpaceLevel(space);

        // 接受的是 封装类，入库的是实体类
        // 设置编辑时间
        space.setEditTime(new Date());
        // 数据校验
        spaceService.validSpace(space,false);
        User loginUser = userService.getLoginUser(request);

        // 判断是否存在
        Long id = editDto.getId();
        Space byId = spaceService.getById(id);
        ThrowUtils.throwIf(byId == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        // 只有本人 和 管理员可以编辑
        spaceService.checkSpaceAuth(byId,loginUser);

        // 操作数据库
        boolean b = spaceService.updateById(space);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    //查询空间等级
    @GetMapping("/lsit/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel(){
        List<SpaceLevel> collect = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                )).collect(Collectors.toList());
        return ResultUtils.success(collect);
    }

}
