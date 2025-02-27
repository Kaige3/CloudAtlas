package com.kaige.model.dto.space.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceAnalyzeDto implements Serializable {

    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 查询公共图库
     */
    private boolean queryPublic;  // 无需设置where条件
    /**
     * 查询所有图库
     */
    private boolean queryAll;// 将spaceId设置为null

    private static final long serialVersionUID = 1L;

}
