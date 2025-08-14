package api.v2.common.crud.exception;

import api.v2.common.exception.CustomBaseException;
import api.v2.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.HashMap;

/**
 * CRUD 입력 데이터 검증 실패 시 발생하는 예외
 * 
 * 사용 예시:
 * - 필수 필드 누락
 * - 형식 불일치 (이메일, 전화번호 등)
 * - 비즈니스 규칙 위반 (중복, 범위 초과 등)
 */
public class CrudValidationException extends CustomBaseException {

    private final Map<String, String> validationErrors;

    /**
     * 기본 에러 코드로 예외 생성
     */
    public CrudValidationException() {
        super(ErrorCode.CRUD_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        this.validationErrors = new HashMap<>();
    }

    /**
     * 커스텀 메시지로 예외 생성
     * 
     * @param message 사용자 친화적 메시지
     */
    public CrudValidationException(String message) {
        super(message, ErrorCode.CRUD_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        this.validationErrors = new HashMap<>();
    }

    /**
     * 단일 필드 검증 오류로 예외 생성
     * 
     * @param fieldName    필드명
     * @param errorMessage 오류 메시지
     */
    public CrudValidationException(String fieldName, String errorMessage) {
        super("입력 데이터 검증에 실패했습니다.", ErrorCode.CRUD_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        this.validationErrors = new HashMap<>();
        this.validationErrors.put(fieldName, errorMessage);
    }

    /**
     * 여러 필드 검증 오류로 예외 생성
     * 
     * @param validationErrors 필드별 오류 메시지 맵
     */
    public CrudValidationException(Map<String, String> validationErrors) {
        super("입력 데이터 검증에 실패했습니다.", ErrorCode.CRUD_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        this.validationErrors = new HashMap<>(validationErrors);
    }

    /**
     * 커스텀 메시지와 검증 오류로 예외 생성
     * 
     * @param message          사용자 친화적 메시지
     * @param validationErrors 필드별 오류 메시지 맵
     */
    public CrudValidationException(String message, Map<String, String> validationErrors) {
        super(message, ErrorCode.CRUD_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        this.validationErrors = new HashMap<>(validationErrors);
    }

    /**
     * 원인 예외와 함께 예외 생성
     * 
     * @param message 사용자 친화적 메시지
     * @param cause   원인 예외
     */
    public CrudValidationException(String message, Throwable cause) {
        super(message, ErrorCode.CRUD_VALIDATION_FAILED, HttpStatus.BAD_REQUEST, cause);
        this.validationErrors = new HashMap<>();
    }

    /**
     * 검증 오류 맵 반환
     * 
     * @return 필드별 오류 메시지 맵
     */
    public Map<String, String> getValidationErrors() {
        return new HashMap<>(validationErrors);
    }

    /**
     * 검증 오류 추가
     * 
     * @param fieldName    필드명
     * @param errorMessage 오류 메시지
     */
    public void addValidationError(String fieldName, String errorMessage) {
        this.validationErrors.put(fieldName, errorMessage);
    }

    /**
     * 검증 오류가 있는지 확인
     * 
     * @return 검증 오류 존재 여부
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }
}
