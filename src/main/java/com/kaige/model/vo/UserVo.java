package com.kaige.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserVo implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String userName;
    private String userAccount;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private Date createTime;
}
