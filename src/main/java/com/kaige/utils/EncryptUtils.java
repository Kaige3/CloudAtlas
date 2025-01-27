package com.kaige.utils;

import org.springframework.util.DigestUtils;

public class EncryptUtils {

    public static String encrypt(String password){
        // 混淆盐值
        final String SALT = "kaigee";

        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }
}
