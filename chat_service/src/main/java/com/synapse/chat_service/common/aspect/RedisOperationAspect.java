package com.synapse.chat_service.common.aspect;

import com.synapse.chat_service.common.annotation.RedisOperation;
import com.synapse.chat_service.exception.commonexception.RedisOperationException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class RedisOperationAspect {
    
    @Around("@annotation(redisOperation)")
    public Object handleRedisOperation(ProceedingJoinPoint joinPoint, RedisOperation redisOperation) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String operation = redisOperation.value().isEmpty() ? methodName : redisOperation.value();
        
        try {
            Object result = joinPoint.proceed();
            log.debug("Redis 작업 성공: {}.{}", className, operation);
            return result;
            
        } catch (Exception e) {
            log.error("Redis 작업 실패: {}.{} - 원인: {}", className, operation, e.getMessage(), e);
            
            if (redisOperation.returnDefaultOnError()) {
                log.debug("Redis 작업 실패 시 기본값 반환: {}.{}", className, operation);
                MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                return getDefaultValue(methodSignature.getReturnType());
            }
            
            if (redisOperation.rethrowException()) {
                // 예외 체이닝을 통해 원본 예외의 스택 트레이스 보존
                String operationDescription = String.format("%s.%s", className, operation);
                throw RedisOperationException.operationError(operationDescription, e);
            }
            
            log.debug("Redis 작업 실패 시 null 반환: {}.{}", className, operation);
            return null;
        }
    }
    
    private Object getDefaultValue(Class<?> returnType) {
        if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        }
        if (returnType == int.class || returnType == Integer.class) {
            return 0;
        }
        if (returnType == long.class || returnType == Long.class) {
            return 0L;
        }
        return null;
    }
}
