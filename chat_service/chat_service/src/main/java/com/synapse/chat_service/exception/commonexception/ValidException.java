package com.synapse.chat_service.exception.commonexception;

import com.synapse.chat_service.exception.domain.ExceptionType;

public class ValidException extends BusinessException {
    public ValidException(ExceptionType exceptionType) {
        super(exceptionType);
    }

    public ValidException(ExceptionType exceptionType, String customMessage) {
        super(exceptionType, customMessage);
    }
}
