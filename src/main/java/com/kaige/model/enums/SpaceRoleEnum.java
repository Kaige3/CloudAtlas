package com.kaige.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum SpaceRoleEnum {

    VIEWER("浏览者","1"),
    EDITOR("编辑者","2"),
    ADMIN("管理员","3");

    private final String text;

    private final String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    // 枚举类的静态方法
    public static SpaceRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
            if (spaceRoleEnum.getValue().equals(value)) {
                return spaceRoleEnum;
            }
        }
        return null;
    }

    // 获取所有枚举的文本列表
    public static List<String> getAllText() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText)
                .collect(Collectors.toList());
    }
    // 获取所有枚举的值列表

    public static List<String> getAllValue() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .collect(Collectors.toList());
    }
}
