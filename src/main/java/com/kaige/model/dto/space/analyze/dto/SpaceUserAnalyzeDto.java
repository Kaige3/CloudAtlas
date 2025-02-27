package com.kaige.model.dto.space.analyze.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeDto extends SpaceAnalyzeDto {

    /**
     * 用户Id
     */
    private Long userId;
    /**
     * 时间维度 ：day / month / year
     */
    private String timeDimension;
}
