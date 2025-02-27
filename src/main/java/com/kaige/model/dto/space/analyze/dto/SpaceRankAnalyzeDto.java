package com.kaige.model.dto.space.analyze.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceRankAnalyzeDto implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 空间排名前十
     */
    private Integer topN = 10;
}
