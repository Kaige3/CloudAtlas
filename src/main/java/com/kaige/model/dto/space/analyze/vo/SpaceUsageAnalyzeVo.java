package com.kaige.model.dto.space.analyze.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间使用情况，返回视图
 * @author kaige
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUsageAnalyzeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 已用大小
     */
    private Long usedSize;
    /**
     * 总大小
     */
    private Long maxSize;
    /**
     * 空间大小使用比例
     */
    private Double sizeUsageRatio;
    /**
     * 当前图片数量
     */
    private Long usedCount;
    /**
     * 最图片数量
     */
    private Long maxCount;
    /**
     * 图片使用比例
     */
    private Double countUsageRatio;
}
