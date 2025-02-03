package com.kaige.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUpdateDto implements Serializable {


    private static final long serialVersionUID = 1L;
    private Long id;
    private String spaceName;
    private Integer spaceLevel;
    private Long maxSize;
    private Long maxCount;
}
