package com.kaige.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kaige.model.dto.user.UserQueryDto;
import com.kaige.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kaige.model.vo.LoginUserVo;
import com.kaige.model.vo.UserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 15336
* @description 针对表【user】的数据库操作Service
* @createDate 2025-01-21 18:54:46
*/
public interface UserService extends IService<User> {


    long userRegister(String userAccount, String userPassword, String checkPassword);

    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 数据脱敏
     * @param user
     * @return
     */
    LoginUserVo getLoginUserVo(User user);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     *
     */
    User getLoginUser(HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    // 数据脱敏
    UserVo getUserVo(User user);
    List<UserVo> getUserVoList(List<User> userList);

    //将查询转为QueryWrapper
    QueryWrapper<User> getQueryWrapper(UserQueryDto query);

    // 是否为管理员
    boolean isAdmin(User user);
}
