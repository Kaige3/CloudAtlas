package com.kaige.Result;
import com.kaige.exception.ErrorCode;

public class ResultUtils {
    /**
     * 成功
     * @param data 数据
     * @return 结果
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0,data);
    }

    /**
     * 成功
     * @param data 数据
     * @param message 信息
     * @return 结果
     */
    public static <T> BaseResponse<T> success(T data,String message){
        return new BaseResponse<>(0,message,data);
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @return 结果
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * @param code 错误码
     * @param message 错误信息
     * @return 结果
     */
    public static <T> BaseResponse<T> error(int code,String message){
        return new BaseResponse<>(code,message,null);
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @param message 错误信息
     * @return 结果
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode,String message){
        return new BaseResponse<>(errorCode.getCode(),message,null);
    }


}
