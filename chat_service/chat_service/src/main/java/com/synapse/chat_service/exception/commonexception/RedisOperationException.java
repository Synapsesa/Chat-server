package com.synapse.chat_service.exception.commonexception;

import com.synapse.chat_service.exception.domain.ExceptionType;

public class RedisOperationException extends BusinessException {

    /**
     * 커스텀 메시지와 원인 예외를 포함한 Redis 작업 예외 생성자
     * 
     * @param exceptionType Redis 관련 예외 타입
     * @param customMessage 사용자 정의 메시지
     * @param cause         원인 예외
     */
    private RedisOperationException(ExceptionType exceptionType, String customMessage, Throwable cause) {
        super(exceptionType, customMessage, cause);
    }

    /**
     * Redis 작업 오류 예외 생성 팩토리 메소드
     * 
     * @param operation 실패한 작업명
     * @param cause     원인 예외
     * @return RedisOperationException 인스턴스
     */
    public static RedisOperationException operationError(String operation, Throwable cause) {
        return new RedisOperationException(
                ExceptionType.REDIS_OPERATION_ERROR,
                String.format("Redis 작업 실패: %s", operation),
                cause);
    }
}
