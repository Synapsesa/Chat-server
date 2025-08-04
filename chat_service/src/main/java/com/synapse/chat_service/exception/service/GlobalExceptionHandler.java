package com.synapse.chat_service.exception.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.synapse.chat_service.exception.commonexception.BadRequestException;
import com.synapse.chat_service.exception.commonexception.BusinessException;
import com.synapse.chat_service.exception.domain.ExceptionType;
import com.synapse.chat_service.exception.dto.ExceptionResponse;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 로그 포맷 상수
    private static final String INFO_LOG_FORMAT = "INFO - {} {} - Status: {} - Exception: {} - Message: {}";
    private static final String WARN_LOG_FORMAT = "WARN - {} {} - Status: {} - Exception: {} - Message: {}";
    private static final String ERROR_LOG_FORMAT = "ERROR - {} {} - Status: {} - Exception: {} - Message: {}";
    
    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ExceptionResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        logWarn(request, e, e.getExceptionType().getStatus());
        return ResponseEntity.status(e.getExceptionType().getStatus()).body(ExceptionResponse.from(e));
    }
    
    /**
     * BadRequest 예외 처리
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ExceptionResponse> handleBadRequestException(BadRequestException e, HttpServletRequest request) {
        logWarn(request, e, e.getExceptionType().getStatus());
        return ResponseEntity.status(e.getExceptionType().getStatus()).body(ExceptionResponse.from(e));
    }
    
    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        logInfo(request, e, HttpStatus.BAD_REQUEST);
        
        ExceptionResponse response = ExceptionResponse.of(ExceptionType.INVALID_INPUT_VALUE, errorMessage);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 필수 파라미터 누락 예외 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ExceptionResponse> handleMissingParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        // ExceptionType에 정의된 기본 메시지에 구체적인 파라미터 정보 추가
        String detailedMessage = String.format("%s (파라미터: %s)", 
                ExceptionType.MISSING_REQUEST_PARAMETER.getMessage(), 
                e.getParameterName());
        
        logInfo(request, e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.badRequest()
                .body(ExceptionResponse.of(ExceptionType.MISSING_REQUEST_PARAMETER, detailedMessage));
    }
    
    /**
     * 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        // ExceptionType에 정의된 기본 메시지에 구체적인 파라미터 정보 추가
        String detailedMessage = String.format("%s (파라미터: %s)", 
                ExceptionType.INVALID_TYPE_VALUE.getMessage(), 
                e.getName());
        
        logInfo(request, e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.badRequest()
                .body(ExceptionResponse.of(ExceptionType.INVALID_TYPE_VALUE, detailedMessage));
    }
    
    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        logWarn(request, e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.badRequest()
                .body(ExceptionResponse.of(ExceptionType.INVALID_INPUT_VALUE, e.getMessage()));
    }
    
    /**
     * 정적 리소스 없음 예외 처리 (INFO 레벨로 처리)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        logInfo(request, e, HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ExceptionResponse.of(ExceptionType.RESOURCE_NOT_FOUND));
    }
    
    /**
     * 일반적인 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGeneralException(Exception e, HttpServletRequest request) {
        logError(request, e, HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExceptionResponse.of(ExceptionType.INTERNAL_SERVER_ERROR));
    }
    
    // 로깅 메서드들
    private void logInfo(HttpServletRequest request, Exception e, HttpStatus status) {
        log.info(INFO_LOG_FORMAT, 
                request.getMethod(), 
                request.getRequestURI(), 
                status.value(), 
                e.getClass().getSimpleName(), 
                e.getMessage());
    }
    

    
    private void logWarn(HttpServletRequest request, Exception e, HttpStatus status) {
        log.warn(WARN_LOG_FORMAT, 
                request.getMethod(), 
                request.getRequestURI(), 
                status.value(), 
                e.getClass().getSimpleName(), 
                e.getMessage());
    }
    
    private void logError(HttpServletRequest request, Exception e, HttpStatus status) {
        log.error(ERROR_LOG_FORMAT, 
                request.getMethod(), 
                request.getRequestURI(), 
                status.value(), 
                e.getClass().getSimpleName(), 
                e.getMessage(), 
                e);
    }
}
