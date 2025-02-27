package com.kaige.model.dto.space.analyze.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUserAnalyzeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 时间区间
     */
    private String period;

    /**
     * 上传数量
     */
    private Long count;
}
