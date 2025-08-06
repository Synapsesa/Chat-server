package com.synapse.chat_service.exception.service;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.chat_service.exception.commonexception.BusinessException;
import com.synapse.chat_service.exception.domain.ExceptionType;
import com.synapse.chat_service.exception.dto.ExceptionResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * StompSubProtocolErrorHandler를 상속받아서 프로토콜 레벨 오류 처리 역할을 명확히 분리합니다.
 * 클라이언트에게 안전하고 일관된 오류 피드백을 제공하며, 서버 측에는 상세한 로그를 남깁니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomStompErrorHandler extends StompSubProtocolErrorHandler {
    
    private final ObjectMapper objectMapper;
    
    /**
     * @MessageMapping 메소드 실행 중 또는 인바운드 채널 인터셉터에서 발생한 예외를 처리합니다.
     * 
     * @param clientMessage 클라이언트에서 보낸 원본 메시지
     * @param ex 발생한 예외
     * @return 클라이언트에게 전송할 ERROR 프레임
     */
    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        // 예외 분석 및 로깅
        Throwable rootCause = getRootCause(ex);
        
        log.error("STOMP message processing error occurred. Client message: {}, Exception: {}", 
                 clientMessage, ex.getMessage(), ex);
        
        // 클라이언트용 에러 페이로드 생성
        ExceptionResponse errorResponse = createErrorResponse(rootCause);
        
        // ERROR 프레임 생성 및 반환
        return createErrorFrame(errorResponse);
    }
    
    /**
     * 브로커 자체 오류 등 서버에서 클라이언트로 보내는 다른 에러 메시지를 처리합니다.
     * handleClientMessageProcessingError와 동일한 포맷으로 에러 메시지를 표준화합니다.
     * 
     * @param errorMessage 원본 에러 메시지
     * @param clientMessage 클라이언트 메시지 (nullable)
     * @return 표준화된 ERROR 프레임
     */
    @Override
    public Message<byte[]> handleErrorMessageToClient(Message<byte[]> errorMessage) {
        // 표준화된 에러 응답 생성
        ExceptionResponse errorResponse = ExceptionResponse.of(
            ExceptionType.INTERNAL_SERVER_ERROR, 
            "WebSocket 통신 중 오류가 발생했습니다."
        );
        
        return createErrorFrame(errorResponse);
    }
    
    /**
     * 예외의 근본 원인을 찾습니다.
     * 
     * @param ex 분석할 예외
     * @return 근본 원인 예외
     */
    private Throwable getRootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
    
    /**
     * 예외를 분석하여 적절한 ExceptionResponse를 생성합니다.
     * 
     * @param ex 분석할 예외
     * @return 클라이언트에게 전송할 에러 응답
     */
    private ExceptionResponse createErrorResponse(Throwable ex) {
        if (ex instanceof BusinessException businessException) {
            // 비즈니스 예외인 경우 해당 ExceptionType 사용
            return ExceptionResponse.from(businessException);
        } else if (ex instanceof IllegalArgumentException) {
            // IllegalArgumentException인 경우 잘못된 입력값으로 처리
            return ExceptionResponse.of(ExceptionType.INVALID_INPUT_VALUE, ex.getMessage());
        } else if (ex instanceof JWTVerificationException) {
            // JWT 관련 예외는 인증 실패로 처리합니다.
            return ExceptionResponse.of(ExceptionType.INVALID_TOKEN, "유효하지 않은 토큰입니다.");
        } else {
            // 그 외의 경우 내부 서버 오류로 처리
            return ExceptionResponse.of(ExceptionType.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * ExceptionResponse를 사용하여 STOMP ERROR 프레임을 생성합니다.
     * 
     * @param errorResponse 에러 응답 객체
     * @return STOMP ERROR 프레임
     */
    private Message<byte[]> createErrorFrame(ExceptionResponse errorResponse) {
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException e) {
            // 직렬화 실패 시 비상용 메시지
            log.error("Failed to serialize error response: {}", e.getMessage(), e);
            payload = "{\"code\":\"E999\",\"message\":\"Error response serialization failed.\"}".getBytes(StandardCharsets.UTF_8);
        }
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setLeaveMutable(true);
        accessor.setMessage(errorResponse.getMessage()); // 헤더 메시지 설정
        accessor.setContentType(MediaType.APPLICATION_JSON); // 콘텐츠 타입 명시
        
        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }
}
