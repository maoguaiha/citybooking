package com.citybooking.server.common;

import java.util.UUID;

public record ApiResponse<T>(int code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, tid());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null, tid());
    }

    public static <T> ApiResponse<T> error(ResultCode code, String message) {
        return new ApiResponse<>(code.getCode(), message, null, tid());
    }

    public static <T> ApiResponse<T> error(ResultCode code) {
        return new ApiResponse<>(code.getCode(), code.getMessage(), null, tid());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, tid());
    }

    private static String tid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
