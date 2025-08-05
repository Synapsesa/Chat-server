package com.synapse.chat_service.exception.commonexception;

import com.synapse.chat_service.exception.domain.ExceptionType;

public class NotFoundException extends BusinessException {
    
    public NotFoundException(ExceptionType exceptionType) {
        super(exceptionType);
    }
    
    public NotFoundException(ExceptionType exceptionType, String customMessage) {
        super(exceptionType, customMessage);
    }
}
