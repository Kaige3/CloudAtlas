package com.kaige.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureQueryDto implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;
    private Long id;
    private String name;
    private String introduction;
    private String category;
    private Long picSize;
    private Integer picWidth;
    private Integer picHeight;
    private Double picScale;
    private String picFormat;

    private String searchText;
    private Long UserId;
    private String sortField;
    private String sortOrder;

    private long pageNum;
    private long current;

    private long pageSize;
    private List<String> tags;
}
