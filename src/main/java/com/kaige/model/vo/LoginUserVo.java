package com.kaige.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class LoginUserVo implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String userAccount;
    private String userAvatar;
    private String userRole;
    private String userName;
    private String userProfile;
    private Date createTime;
    private Date updateTime;


}
