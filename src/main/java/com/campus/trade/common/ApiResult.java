package com.campus.trade.common;

import lombok.Data;

@Data
public class ApiResult<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 200; r.message = "success"; r.data = data;
        return r;
    }

    public static <T> ApiResult<T> success(String msg, T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 200; r.message = msg; r.data = data;
        return r;
    }

    public static <T> ApiResult<T> error(int code, String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = code; r.message = msg;
        return r;
    }
}
