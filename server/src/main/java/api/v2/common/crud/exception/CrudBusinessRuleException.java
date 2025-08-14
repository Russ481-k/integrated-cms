package api.v2.common.crud.exception;

import api.v2.common.exception.CustomBaseException;
import api.v2.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * CRUD 비즈니스 규칙 위반 시 발생하는 예외
 * 
 * 사용 예시:
 * - 중복 데이터 생성 시도
 * - 사용 중인 리소스 삭제 시도
 * - 상태 변경 규칙 위반
 * - 관계 무결성 위반
 */
public class CrudBusinessRuleException extends CustomBaseException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public CrudBusinessRuleException() {
        super(ErrorCode.CRUD_BUSINESS_RULE_VIOLATION, HttpStatus.BAD_REQUEST);
    }

    /**
     * 커스텀 메시지로 예외 생성
     * 
     * @param message 사용자 친화적 메시지
     */
    public CrudBusinessRuleException(String message) {
        super(message, ErrorCode.CRUD_BUSINESS_RULE_VIOLATION, HttpStatus.BAD_REQUEST);
    }

    /**
     * 특정 에러 코드로 예외 생성
     * 
     * @param errorCode 특정 에러 코드
     */
    public CrudBusinessRuleException(ErrorCode errorCode) {
        super(errorCode, errorCode.getHttpStatus());
    }

    /**
     * 특정 에러 코드와 커스텀 메시지로 예외 생성
     * 
     * @param message   커스텀 메시지
     * @param errorCode 특정 에러 코드
     */
    public CrudBusinessRuleException(String message, ErrorCode errorCode) {
        super(message, errorCode, errorCode.getHttpStatus());
    }

    /**
     * 비즈니스 규칙과 리소스 정보를 포함한 구체적인 메시지로 예외 생성
     * 
     * @param rule         위반된 비즈니스 규칙
     * @param resourceType 리소스 타입
     * @param resourceId   리소스 ID
     */
    public CrudBusinessRuleException(String rule, String resourceType, Object resourceId) {
        super(String.format("Business rule '%s' violated for %s with ID '%s'",
                rule, resourceType, resourceId),
                ErrorCode.CRUD_BUSINESS_RULE_VIOLATION, HttpStatus.BAD_REQUEST);
    }

    /**
     * 원인 예외와 함께 예외 생성
     * 
     * @param message 사용자 친화적 메시지
     * @param cause   원인 예외
     */
    public CrudBusinessRuleException(String message, Throwable cause) {
        super(message, ErrorCode.CRUD_BUSINESS_RULE_VIOLATION, HttpStatus.BAD_REQUEST, cause);
    }

    /**
     * 상세 정보와 함께 예외 생성
     * 
     * @param message       사용자 친화적 메시지
     * @param detailMessage 상세 메시지 (로깅용)
     */
    public CrudBusinessRuleException(String message, String detailMessage) {
        super(message, ErrorCode.CRUD_BUSINESS_RULE_VIOLATION, HttpStatus.BAD_REQUEST, detailMessage);
    }

    /**
     * HTTP 상태 코드를 지정하여 예외 생성
     * 
     * @param message    사용자 친화적 메시지
     * @param httpStatus HTTP 상태 코드
     */
    public CrudBusinessRuleException(String message, HttpStatus httpStatus) {
        super(message, ErrorCode.CRUD_BUSINESS_RULE_VIOLATION, httpStatus);
    }

    // == 팩토리 메서드들 ==

    /**
     * 중복 리소스 예외 생성
     */
    public static CrudBusinessRuleException duplicateResource(String resourceType, String identifier) {
        return new CrudBusinessRuleException(
                String.format("%s '%s' already exists", resourceType, identifier),
                ErrorCode.CRUD_DUPLICATE_RESOURCE);
    }

    /**
     * 사용 중인 리소스 삭제 시도 예외 생성
     */
    public static CrudBusinessRuleException resourceInUse(String resourceType, Object resourceId) {
        return new CrudBusinessRuleException(
                String.format("%s with ID '%s' is currently in use and cannot be deleted", resourceType, resourceId),
                ErrorCode.CRUD_RESOURCE_IN_USE);
    }

    /**
     * 잘못된 상태 변경 시도 예외 생성
     */
    public static CrudBusinessRuleException invalidStateTransition(String fromStatus, String toStatus) {
        return new CrudBusinessRuleException(
                String.format("Cannot change status from '%s' to '%s'", fromStatus, toStatus));
    }
}
