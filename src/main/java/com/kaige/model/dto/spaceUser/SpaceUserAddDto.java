package com.kaige.model.dto.spaceUser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserAddDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long spaceId;

    private Integer spaceRole;
}
