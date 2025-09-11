package com.synapse.chat_service.exception.commonexception;

import com.synapse.chat_service.exception.domain.ExceptionType;

public class BadRequestException extends BusinessException {

    public BadRequestException(ExceptionType exceptionType) {
        super(exceptionType);
    }

    public BadRequestException(ExceptionType exceptionType, String customMessage) {
        super(exceptionType, customMessage);
    }

    public BadRequestException(ExceptionType exceptionType, Throwable cause) {
        super(exceptionType, cause);
    }

    public BadRequestException(ExceptionType exceptionType, String customMessage, Throwable cause) {
        super(exceptionType, customMessage, cause);
    }
}
