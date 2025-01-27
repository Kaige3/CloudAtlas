package com.kaige.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginDto implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户账户
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;

}
