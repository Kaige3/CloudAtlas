package com.kaige.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterDto implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 用户昵称
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;
    /**
     * 校验密码
     */
    private String checkPassword;

}
