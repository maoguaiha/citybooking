package com.citybooking.server.common;

public class BizException extends RuntimeException {
    private final int code;

    public BizException(ResultCode code) {
        super(code.getMessage());
        this.code = code.getCode();
    }

    public BizException(ResultCode code, String message) {
        super(message);
        this.code = code.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
