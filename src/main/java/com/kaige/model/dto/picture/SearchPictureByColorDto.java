package com.kaige.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByColorDto implements Serializable {

    private final static long serialVersionUID = 1L;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 图片颜色
     */
    private String picColor;
}
