package com.kaige.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpaceLevelEnum {
    COMMON("普通版",0,100L,1024L*1024L*100L),
    PROFESSIONAL("专业版",1,1000L,1024L*1024L*1000L),
    ENTERPRISE("旗舰版",2,10000L,1024L*1024L*10000L);

    private final String text;
    private final int value;
    private final long maxCount;
    private final long maxSize;


    /**
     * @param text 文本
     * @param value 值
     * @param maxCount 最大数量
     * @param maxSize 最大大小
     */
    SpaceLevelEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    // 根据value获取对应的枚举值
    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            if (spaceLevelEnum.getValue() == value) {
                return spaceLevelEnum;
            }
        }
        return null;
    }
}
