package com.kaige.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUpdateDto implements Serializable {


    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String introduction;
    private String category;
    private List<String> tags;


}
