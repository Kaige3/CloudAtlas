package com.kaige.model.dto.space;

import com.kaige.Result.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceQueryDto extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private String spaceName;
    private Integer spaceLevel;
}
