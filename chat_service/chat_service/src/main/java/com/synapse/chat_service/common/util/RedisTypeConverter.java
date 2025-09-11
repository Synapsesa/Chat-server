package com.synapse.chat_service.common.util;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTypeConverter {

    private final ObjectMapper objectMapper;

    /**
     * Redis에서 조회한 원시 값을 지정된 타입으로 안전하게 변환
     * 
     * @param rawValue   Redis에서 조회한 원시 값
     * @param targetType 변환할 대상 타입
     * @return 변환된 객체 (실패 시 null)
     */
    public <T> T convertValue(Object rawValue, Class<T> targetType) {
        if (rawValue == null) {
            return null;
        }

        try {
            // 이미 올바른 타입인 경우
            if (targetType.isInstance(rawValue)) {
                return targetType.cast(rawValue);
            }

            // ObjectMapper를 사용한 타입 변환
            return objectMapper.convertValue(rawValue, targetType);

        } catch (Exception e) {
            log.warn("Redis 값 타입 변환 실패: rawValue={}, targetType={}",
                    rawValue.getClass().getSimpleName(), targetType.getSimpleName(), e);
            return null;
        }
    }

    /**
     * String 타입으로 안전하게 변환
     */
    public String convertToString(Object rawValue) {
        return convertValue(rawValue, String.class);
    }

    /**
     * 객체를 byte 배열로 변환 (Redis 트랜잭션에서 사용)
     * 
     * @param value 변환할 객체
     * @return byte 배열 (실패 시 빈 배열)
     */
    public byte[] convertToBytes(Object value) {
        if (value == null) {
            return new byte[0];
        }

        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            log.warn("객체를 byte 배열로 변환 실패: value={}", value.getClass().getSimpleName(), e);
            return new byte[0];
        }
    }
}
