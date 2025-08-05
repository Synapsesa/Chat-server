package com.synapse.chat_service.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis 작업에 대한 공통 예외 처리를 위한 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisOperation {
    
    /**
     * 작업 설명 (로깅용)
     */
    String value() default "";
    
    /**
     * 예외 발생 시 기본값 반환 여부
     */
    boolean returnDefaultOnError() default false;
    
    /**
     * 예외를 다시 던질지 여부
     */
    boolean rethrowException() default true;
}
