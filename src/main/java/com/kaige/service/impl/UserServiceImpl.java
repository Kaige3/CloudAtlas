package com.kaige.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;
import com.kaige.model.dto.user.UserQueryDto;
import com.kaige.model.entity.User;
import com.kaige.model.enums.UserRoleEnum;
import com.kaige.model.vo.LoginUserVo;
import com.kaige.model.vo.UserVo;
import com.kaige.service.UserService;
import com.kaige.mapper.UserMapper;
import com.kaige.utils.EncryptUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.kaige.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 15336
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-01-21 18:54:46
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount == null || userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }

        // 2. 检查是否重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
        // 3. 加密
            String encryptPassword = EncryptUtils.encrypt(userPassword);
        // 4. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setUserName("凯歌");
            user.setUserRole(UserRoleEnum.USER.getValue());
            boolean save = this.save(user);
            if (!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
            }
            return user.getId();
    }

    @Override
    public LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount == null || userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能过短");
        }
        // 2. 加密
        String encrypt = EncryptUtils.encrypt(userPassword);
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encrypt);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 返回脱敏信息
        return this.getLoginUserVo(user);

    }

    @Override
    public LoginUserVo getLoginUserVo(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVo loginUserVo = new LoginUserVo();
        BeanUtil.copyProperties(user, loginUserVo);
        return loginUserVo;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断是否登录
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User)attribute;

        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 查询用户是否存在
        Long userId = currentUser.getId();
        User byId = this.getById(userId);
        if (byId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否登录
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (attribute == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"未登录吗");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVo getUserVo(User user) {
        if (user == null) {
            return null;
        }
        UserVo userVo = new UserVo();
        BeanUtil.copyProperties(user, userVo);
        return userVo;
    }

    @Override
    public List<UserVo> getUserVoList(List<User> userList) {
        if(CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVo).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryDto query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        Long id = query.getId();
        String userName = query.getUserName();
        String userAccount = query.getUserAccount();
        String userProfile = query.getUserProfile();
        String sortField = query.getSortField();
        String sortOrder = query.getSortOrder();
        String userRole = query.getUserRole();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole),"userRole",userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isEmpty(sortField),sortOrder.equals("ascend"),sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {

        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

}




