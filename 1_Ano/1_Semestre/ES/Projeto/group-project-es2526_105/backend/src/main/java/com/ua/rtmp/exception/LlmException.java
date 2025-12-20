package com.ua.rtmp.exception;

import lombok.Getter;

@Getter
public class LlmException extends RuntimeException {
    private final LlmErrorType errorType;
    private final Integer retryAfterSeconds;

    public LlmException(String message, LlmErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.retryAfterSeconds = null;
    }

    public LlmException(String message, LlmErrorType errorType, Integer retryAfterSeconds) {
        super(message);
        this.errorType = errorType;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public LlmException(String message, Throwable cause, LlmErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
        this.retryAfterSeconds = null;
    }

    public enum LlmErrorType {
        QUOTA_EXCEEDED,
        RATE_LIMIT,
        INVALID_API_KEY,
        SERVICE_UNAVAILABLE,
        TIMEOUT,
        PROCESSING_ERROR
    }
}
