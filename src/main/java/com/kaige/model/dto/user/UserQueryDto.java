package com.kaige.model.dto.user;

import com.kaige.Result.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserQueryDto extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userName;
    private String userAccount;
    private String userProfile;
    private String userRole;


}
