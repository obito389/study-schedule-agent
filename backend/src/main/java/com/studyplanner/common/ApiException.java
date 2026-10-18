package com.studyplanner.common;
public class ApiException extends RuntimeException {
    public final int httpStatus;
    public final String code;
    public ApiException(int httpStatus, String code, String message) {
        super(message); this.httpStatus = httpStatus; this.code = code;
    }
}
