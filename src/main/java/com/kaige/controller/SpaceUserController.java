package com.kaige.controller;

import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.spaceUser.SpaceUserAddDto;
import com.kaige.model.dto.spaceUser.SpaceUserEditDto;
import com.kaige.model.dto.spaceUser.SpaceUserQueryDto;
import com.kaige.model.entity.SpaceUser;
import com.kaige.model.entity.User;
import com.kaige.model.vo.SpaceUserVo;
import com.kaige.result.BaseResponse;
import com.kaige.result.DeleteRequest;
import com.kaige.result.ResultUtils;
import com.kaige.service.SpaceUserService;
import com.kaige.service.UserService;
import com.kaige.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 添加成员到空间
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddDto spaceUserAddDto, HttpServletRequest request){
        ThrowUtils.throwIf(spaceUserAddDto == null, ErrorCode.PARAMS_ERROR,"参数为空");
        long result = spaceUserService.addSpaceUser(spaceUserAddDto);
        return ResultUtils.success(result);
    }

    /**
     * 删除成员
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if(deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 校验是否存在
        SpaceUser spaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(spaceUser == null,ErrorCode.NOT_FOUND_ERROR);
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员的信息
     */
    @GetMapping("/get")
    public BaseResponse<SpaceUser> getSpaceUserById(@RequestBody SpaceUserQueryDto spaceUserQueryDto){
        if(spaceUserQueryDto == null || spaceUserQueryDto.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = spaceUserQueryDto.getUserId();
        Long spaceId = spaceUserQueryDto.getSpaceId();
        ThrowUtils.throwIf(userId == null || spaceId == null,ErrorCode.PARAMS_ERROR);

        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryDto));
        ThrowUtils.throwIf(spaceUser == null,ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员列表
     */
    @PostMapping("/list")
    public BaseResponse<List<SpaceUserVo>> listSpaceUserByPage(@RequestBody SpaceUserQueryDto spaceUserQueryDto, HttpServletRequest request){
        ThrowUtils.throwIf(spaceUserQueryDto == null,ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryDto));
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditDto spaceUserEditDto, HttpServletRequest request){

        // 参数校验
        ThrowUtils.throwIf(spaceUserEditDto == null,ErrorCode.PARAMS_ERROR);
        Long id = spaceUserEditDto.getId();
        ThrowUtils.throwIf(id == null || id <= 0,ErrorCode.PARAMS_ERROR);

        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditDto,spaceUser);
        // 校验是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null,ErrorCode.NOT_FOUND_ERROR);
        // 编辑
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的空间列表
     */
    @PostMapping("/my/list")
    public BaseResponse<List<SpaceUserVo>> listMyJoinSpace(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        SpaceUserQueryDto spaceUserQueryDto = new SpaceUserQueryDto();
        spaceUserQueryDto.setUserId(userId);

        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryDto));
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));

    }
}
