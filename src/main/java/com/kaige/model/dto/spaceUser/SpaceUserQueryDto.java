package com.kaige.model.dto.spaceUser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserQueryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long  id;

    private Long spaceId;

    private Long userId;

    // viewer/editor/admin
    private Integer spaceRole;
}
