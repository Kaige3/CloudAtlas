package com.kaige.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;
}
