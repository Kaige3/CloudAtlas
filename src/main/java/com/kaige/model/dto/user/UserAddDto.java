package com.kaige.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserAddDto implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 用户头像
     */
    private String userAvatar;
    /**
     * 简介
     */
    private String userProfile;
    /**
     * 角色
     */
    private String userRole;

}
