package api.v2.common.crud.exception;

import api.v2.common.exception.CustomBaseException;
import api.v2.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * CRUD 리소스를 찾을 수 없을 때 발생하는 예외
 * 
 * 사용 예시:
 * - 존재하지 않는 ID로 조회/수정/삭제 시도
 * - 서비스별 DB에서 리소스가 존재하지 않는 경우
 */
public class CrudResourceNotFoundException extends CustomBaseException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public CrudResourceNotFoundException() {
        super(ErrorCode.CRUD_RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    /**
     * 커스텀 메시지로 예외 생성
     * 
     * @param message 사용자 친화적 메시지
     */
    public CrudResourceNotFoundException(String message) {
        super(message, ErrorCode.CRUD_RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    /**
     * 리소스 타입과 ID를 포함한 구체적인 메시지로 예외 생성
     * 
     * @param resourceType 리소스 타입 (예: "Menu", "Content")
     * @param resourceId   리소스 ID
     */
    public CrudResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s with ID '%s' not found", resourceType, resourceId),
                ErrorCode.CRUD_RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    /**
     * 원인 예외와 함께 예외 생성
     * 
     * @param message 사용자 친화적 메시지
     * @param cause   원인 예외
     */
    public CrudResourceNotFoundException(String message, Throwable cause) {
        super(message, ErrorCode.CRUD_RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, cause);
    }

    /**
     * 상세 정보와 함께 예외 생성
     * 
     * @param resourceType  리소스 타입
     * @param resourceId    리소스 ID
     * @param detailMessage 상세 메시지 (로깅용)
     */
    public CrudResourceNotFoundException(String resourceType, Object resourceId, String detailMessage) {
        super(String.format("%s with ID '%s' not found", resourceType, resourceId),
                ErrorCode.CRUD_RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, detailMessage);
    }
}
