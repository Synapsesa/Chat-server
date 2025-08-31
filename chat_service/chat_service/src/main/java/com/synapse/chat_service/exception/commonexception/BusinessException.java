package com.synapse.chat_service.exception.commonexception;

import com.synapse.chat_service.exception.domain.ExceptionType;

import lombok.Getter;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final ExceptionType exceptionType;

    public BusinessException(ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
    }

    public BusinessException(ExceptionType exceptionType, String customMessage) {
        super(customMessage);
        this.exceptionType = exceptionType;
    }

    public BusinessException(ExceptionType exceptionType, Throwable cause) {
        super(exceptionType.getMessage(), cause);
        this.exceptionType = exceptionType;
    }

    public BusinessException(ExceptionType exceptionType, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.exceptionType = exceptionType;
    }
}
