package com.kaige.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceAddDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private String spaceName;
    private Integer spaceLevel;


}
