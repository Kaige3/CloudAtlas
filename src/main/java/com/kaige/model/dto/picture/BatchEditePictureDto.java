package com.kaige.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchEditePictureDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> PictureIdList;

    private Long spaceId;

    private String category;

    private List<String> tags;

    /**
     * 命名规则
     */
    private String nameRule;
}
