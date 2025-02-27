package com.kaige.model.dto.space.analyze.vo;

import com.kaige.model.dto.space.analyze.dto.SpaceAnalyzeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceCategoryAnalyzeVo implements Serializable {

    /**
     * 图片分类
     */
    private String category;
    /**
     * 图片数量
     */
    private Long count;
    /**
     * 图片总大小
     */
    private Long totalSize;

    private static final long serialVersionUID = 1L;
}
