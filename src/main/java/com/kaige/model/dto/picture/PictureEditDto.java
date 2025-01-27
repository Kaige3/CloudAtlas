package com.kaige.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditDto implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;
    private Long id;
    private String name;
    private String introduction;
    private String category;
    private List<String> tags;
}
