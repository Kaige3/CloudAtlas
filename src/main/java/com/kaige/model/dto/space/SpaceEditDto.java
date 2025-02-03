package com.kaige.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceEditDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String spaceName;

}
