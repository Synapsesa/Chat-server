package com.synapse.chat_service.exception.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionType {

    // 400 Bad Request
    INVALID_INPUT_VALUE(BAD_REQUEST, "E001", "잘못된 입력값입니다."),
    MISSING_REQUEST_PARAMETER(BAD_REQUEST, "E002", "필수 요청 파라미터가 누락되었습니다."),
    INVALID_TYPE_VALUE(BAD_REQUEST, "E003", "잘못된 타입의 값입니다."),

    // 401 Unauthorized
    TOKEN_UNAUTHORIZED(UNAUTHORIZED, "E101", "인증이 필요합니다."),
    INVALID_TOKEN(UNAUTHORIZED, "E102", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(UNAUTHORIZED, "E103", "만료된 토큰입니다."),

    // 403 Forbidden
    ACCESS_DENIED(FORBIDDEN, "E201", "접근이 거부되었습니다."),
    INSUFFICIENT_PERMISSION(FORBIDDEN, "E202", "권한이 부족합니다."),

    // 404 Not Found
    CONVERSATION_NOT_FOUND(NOT_FOUND, "E301", "대화를 찾을 수 없습니다."),
    MESSAGE_NOT_FOUND(NOT_FOUND, "E302", "메시지를 찾을 수 없습니다."),
    USER_NOT_FOUND(NOT_FOUND, "E303", "사용자를 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(NOT_FOUND, "E304", "요청한 리소스를 찾을 수 없습니다."),

    // 409 Conflict
    DUPLICATE_RESOURCE(CONFLICT, "E401", "이미 존재하는 리소스입니다."),
    DUPLICATE_USERNAME(CONFLICT, "E402", "이미 사용 중인 사용자명입니다."),
    DUPLICATE_EMAIL(CONFLICT, "E403", "이미 사용 중인 이메일입니다."),

    // 422 Unprocessable Entity
    BUSINESS_LOGIC_ERROR(UNPROCESSABLE_ENTITY, "E501", "비즈니스 로직 오류가 발생했습니다."),
    INVALID_STATE(UNPROCESSABLE_ENTITY, "E502", "유효하지 않은 상태입니다."),

    // 429 Too Many Requests
    TOO_MANY_REQUEST(TOO_MANY_REQUESTS, "E601", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERRORS(INTERNAL_SERVER_ERROR, "E901", "서버 내부 오류가 발생했습니다."),
    DATABASE_ERRORS(INTERNAL_SERVER_ERROR, "E902", "데이터베이스 오류가 발생했습니다."),
    EXTERNAL_SERVICE_ERROR(INTERNAL_SERVER_ERROR, "E903", "외부 서비스 연동 중 오류가 발생했습니다."),
    REDIS_CONNECTION_ERROR(INTERNAL_SERVER_ERROR, "E904", "Redis 연결 오류가 발생했습니다."),
    REDIS_OPERATION_ERROR(INTERNAL_SERVER_ERROR, "E905", "Redis 작업 중 오류가 발생했습니다."),
    REDIS_TRANSACTION_ERROR(INTERNAL_SERVER_ERROR, "E906", "Redis 트랜잭션 처리 중 오류가 발생했습니다."),

    // 502 Bad Gateway
    BAD_GATEWAYS(BAD_GATEWAY, "E951", "게이트웨이 오류가 발생했습니다."),

    // 503 Service Unavailable
    CHAT_SERVICE_UNAVAILABLE(SERVICE_UNAVAILABLE, "E961", "서비스를 사용할 수 없습니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
