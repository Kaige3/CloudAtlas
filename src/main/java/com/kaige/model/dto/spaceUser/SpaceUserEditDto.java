package com.kaige.model.dto.spaceUser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserEditDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long  id;
    // 冗余字段
    private Long spaceId;
    private String spaceRole;
}
