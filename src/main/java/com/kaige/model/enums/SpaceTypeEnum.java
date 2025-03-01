package com.kaige.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpaceTypeEnum {

    PRIVATE("私有空间", 1),
    PUBLIC("团队空间", 2);

    private final String text;
    private final int value;


    SpaceTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    // 根据值获取对应的枚举
    public static SpaceTypeEnum getEnumByValue(int value) {

        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()) {
            if (spaceTypeEnum.getValue() == value) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}
