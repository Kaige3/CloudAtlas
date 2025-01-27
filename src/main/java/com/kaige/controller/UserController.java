package com.kaige.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kaige.Result.BaseResponse;
import com.kaige.Result.ResultUtils;
import com.kaige.annotation.AuthCheck;
import com.kaige.constant.UserConstant;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.DeleteDto;
import com.kaige.model.dto.user.*;
import com.kaige.model.entity.User;
import com.kaige.model.vo.LoginUserVo;
import com.kaige.model.vo.UserVo;
import com.kaige.service.UserService;
import com.kaige.utils.EncryptUtils;
import com.kaige.utils.ThrowUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterDto
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterDto userRegisterDto) {
        ThrowUtils.throwIf(userRegisterDto == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterDto.getUserAccount();
        String userPassword = userRegisterDto.getUserPassword();
        String checkPassword = userRegisterDto.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     *
     **/
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVo> getLoginUserVo(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVo(loginUser));
    }

    /**
     * 用户登录
     *
     * @param userLoginDto
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginDto userLoginDto,HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginDto == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginDto.getUserAccount();
        String userPassword = userLoginDto.getUserPassword();
        LoginUserVo loginUserVo = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddDto userAddDto,HttpServletRequest request){
        ThrowUtils.throwIf(userAddDto == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddDto,user);

        // 初始化密码
        final String DEFAULT_PASSWORD = "12345678";
        String encrypt = EncryptUtils.encrypt(DEFAULT_PASSWORD);
        user.setUserPassword(encrypt);
        boolean save = userService.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());

    }
    /**
     * 根据id获取用户
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取 包装类
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<LoginUserVo> getUserVoById(long id){

        BaseResponse<User> userById = getUserById(id);
        User user = userById.getData();
        LoginUserVo loginUserVo = userService.getLoginUserVo(user);
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUserById(@RequestBody DeleteDto deleteDto){

        if (deleteDto == null || deleteDto.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteDto.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateDto user){
        if (user == null || user.getId() == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user1 = new User();
        BeanUtil.copyProperties(user,user1);

        boolean b = userService.updateById(user1);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取分页用户封装列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVo>> listUserByPage(@RequestBody UserQueryDto userQueryDto) {
        ThrowUtils.throwIf(userQueryDto == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryDto.getCurrent();
        long size = userQueryDto.getPageSize();


        Page<User> userPage = userService.page(new Page<>(current, size), userService.getQueryWrapper(userQueryDto));
        Page<UserVo> UserVoPage = new Page<>(current, size, userPage.getTotal());
        List<UserVo> userVoList = userService.getUserVoList(userPage.getRecords());
        UserVoPage.setRecords(userVoList);
        return ResultUtils.success(UserVoPage);
    }
}


