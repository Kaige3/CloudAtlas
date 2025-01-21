package com.kaige.utils;
import com.kaige.exception.BusinessException;
import com.kaige.exception.ErrorCode;

public class ThrowUtils {
    /**
     * 条件成立则抛出异常
     * @param condition 条件
     * @param runtimeException 异常
     */
    public static void throwIf(boolean condition,RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛出异常
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition,ErrorCode errorCode) {
        throwIf(condition,new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛出异常
     * @param condition 条件
     * @param errorCode 错误码
     * @param message 异常信息
     */
    public static void throwIf(boolean condition,ErrorCode errorCode,String message) {
        throwIf(condition,new BusinessException(errorCode,message));
    }
}
