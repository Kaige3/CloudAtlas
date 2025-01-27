package com.kaige.model.dto.file;

import lombok.Data;

@Data
public class UploadPictureDto {

    /**
     * 图片地址
     */
    private String url;
    /**
     * 图片名称
     */
    private String picName;
    /**
     * 图片大小
     */
    private long picSize;
    /**
     * 图片宽度
     */
    private Integer picWidth;
    /**
     * 图片高度
     */
    private Integer picHeight;
    /**
     * 图片缩放比例
     */
    private Double picScale;
    /**
     * 图片格式
     */
    private String picFormat;
}
