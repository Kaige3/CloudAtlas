package com.kaige.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadDto implements Serializable {

    /**
     * 图片id
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;



    private static final long serialVersionUID = 1L;

}
