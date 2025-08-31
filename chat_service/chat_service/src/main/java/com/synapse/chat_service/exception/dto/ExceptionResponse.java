package com.synapse.chat_service.exception.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.synapse.chat_service.exception.commonexception.BusinessException;
import com.synapse.chat_service.exception.domain.ExceptionType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExceptionResponse {

    private final String code;
    private final String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    public static ExceptionResponse from(BusinessException exception) {
        return ExceptionResponse.builder()
                .code(exception.getExceptionType().getCode())
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ExceptionResponse of(ExceptionType exceptionType) {
        return ExceptionResponse.builder()
                .code(exceptionType.getCode())
                .message(exceptionType.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ExceptionResponse of(ExceptionType exceptionType, String customMessage) {
        return ExceptionResponse.builder()
                .code(exceptionType.getCode())
                .message(customMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}