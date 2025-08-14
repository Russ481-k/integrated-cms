package api.v2.common.crud.exception;

import api.v2.common.exception.CustomBaseException;
import api.v2.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * CRUD 작업에 대한 권한이 없을 때 발생하는 예외
 * 
 * 사용 예시:
 * - 읽기 권한 없는 리소스 조회 시도
 * - 수정/삭제 권한 없는 리소스 변경 시도
 * - 서비스별 접근 권한 부족
 */
public class CrudPermissionDeniedException extends CustomBaseException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public CrudPermissionDeniedException() {
        super(ErrorCode.CRUD_PERMISSION_DENIED, HttpStatus.FORBIDDEN);
    }

    /**
     * 커스텀 메시지로 예외 생성
     * 
     * @param message 사용자 친화적 메시지
     */
    public CrudPermissionDeniedException(String message) {
        super(message, ErrorCode.CRUD_PERMISSION_DENIED, HttpStatus.FORBIDDEN);
    }

    /**
     * 작업과 리소스 정보를 포함한 구체적인 메시지로 예외 생성
     * 
     * @param operation    작업 타입 (예: "READ", "UPDATE", "DELETE")
     * @param resourceType 리소스 타입 (예: "Menu", "Content")
     * @param resourceId   리소스 ID
     */
    public CrudPermissionDeniedException(String operation, String resourceType, Object resourceId) {
        super(String.format("Permission denied for %s operation on %s with ID '%s'",
                operation, resourceType, resourceId),
                ErrorCode.CRUD_PERMISSION_DENIED, HttpStatus.FORBIDDEN);
    }

    /**
     * 사용자 정보와 함께 예외 생성
     * 
     * @param operation    작업 타입
     * @param resourceType 리소스 타입
     * @param username     사용자명
     * @param userRole     사용자 역할
     */
    public CrudPermissionDeniedException(String operation, String resourceType, String username, String userRole) {
        super(String.format("User '%s' with role '%s' has no permission for %s operation on %s",
                username, userRole, operation, resourceType),
                ErrorCode.CRUD_PERMISSION_DENIED, HttpStatus.FORBIDDEN);
    }

    /**
     * 원인 예외와 함께 예외 생성
     * 
     * @param message 사용자 친화적 메시지
     * @param cause   원인 예외
     */
    public CrudPermissionDeniedException(String message, Throwable cause) {
        super(message, ErrorCode.CRUD_PERMISSION_DENIED, HttpStatus.FORBIDDEN, cause);
    }

    /**
     * 상세 정보와 함께 예외 생성
     * 
     * @param message       사용자 친화적 메시지
     * @param detailMessage 상세 메시지 (로깅용)
     */
    public CrudPermissionDeniedException(String message, String detailMessage) {
        super(message, ErrorCode.CRUD_PERMISSION_DENIED, HttpStatus.FORBIDDEN, detailMessage);
    }
}
