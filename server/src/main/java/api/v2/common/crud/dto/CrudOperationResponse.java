package api.v2.common.crud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * CRUD 작업 결과 응답 래퍼
 * 
 * 생성, 수정, 삭제 등의 작업 결과와 메타데이터를 포함한 표준화된 응답 형식
 * 
 * @param <T> 결과 데이터 타입
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CRUD 작업 결과 응답")
public class CrudOperationResponse<T> {

    @Schema(description = "작업 결과 데이터")
    private T data;

    @Schema(description = "작업 정보")
    private OperationInfo operation;

    @Schema(description = "메타데이터")
    private OperationMetadata metadata;

    /**
     * 작업 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "작업 정보")
    public static class OperationInfo {
        @Schema(description = "작업 타입", example = "CREATE", allowableValues = { "CREATE", "UPDATE", "DELETE", "READ" })
        private String operationType;

        @Schema(description = "리소스 타입", example = "Menu")
        private String resourceType;

        @Schema(description = "리소스 ID", example = "123")
        private String resourceId;

        @Schema(description = "작업 성공 여부", example = "true")
        private boolean success;

        @Schema(description = "작업 메시지", example = "메뉴가 성공적으로 생성되었습니다.")
        private String message;

        @Schema(description = "작업 수행 시간", example = "2024-01-15T10:30:00")
        private LocalDateTime timestamp;

        @Schema(description = "영향받은 행 수", example = "1")
        private Integer affectedRows;
    }

    /**
     * 작업 메타데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "작업 메타데이터")
    public static class OperationMetadata {
        @Schema(description = "작업 수행자", example = "admin")
        private String performedBy;

        @Schema(description = "작업 수행자 역할", example = "ADMIN")
        private String performerRole;

        @Schema(description = "클라이언트 IP", example = "192.168.1.100")
        private String clientIp;

        @Schema(description = "서비스 컨텍스트", example = "douzone")
        private String serviceContext;

        @Schema(description = "실행 시간 (ms)", example = "156")
        private Long executionTimeMs;

        @Schema(description = "검증 규칙 적용 수", example = "3")
        private Integer validationRulesApplied;

        @Schema(description = "권한 검증 수행 여부", example = "true")
        private Boolean permissionCheckPerformed;

        @Schema(description = "감사 로그 ID", example = "audit-log-001")
        private String auditLogId;

        @Schema(description = "추가 정보")
        private Object additionalInfo;
    }

    // == 팩토리 메서드들 ==

    /**
     * 생성 작업 응답 생성
     */
    public static <T> CrudOperationResponse<T> created(T data, String resourceType, String resourceId, String message) {
        return CrudOperationResponse.<T>builder()
                .data(data)
                .operation(OperationInfo.builder()
                        .operationType("CREATE")
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .success(true)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .affectedRows(1)
                        .build())
                .build();
    }

    /**
     * 수정 작업 응답 생성
     */
    public static <T> CrudOperationResponse<T> updated(T data, String resourceType, String resourceId, String message) {
        return CrudOperationResponse.<T>builder()
                .data(data)
                .operation(OperationInfo.builder()
                        .operationType("UPDATE")
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .success(true)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .affectedRows(1)
                        .build())
                .build();
    }

    /**
     * 삭제 작업 응답 생성
     */
    public static CrudOperationResponse<Void> deleted(String resourceType, String resourceId, String message) {
        return CrudOperationResponse.<Void>builder()
                .operation(OperationInfo.builder()
                        .operationType("DELETE")
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .success(true)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .affectedRows(1)
                        .build())
                .build();
    }

    /**
     * 조회 작업 응답 생성
     */
    public static <T> CrudOperationResponse<T> read(T data, String resourceType, String resourceId) {
        return CrudOperationResponse.<T>builder()
                .data(data)
                .operation(OperationInfo.builder()
                        .operationType("READ")
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .success(true)
                        .message("조회 완료")
                        .timestamp(LocalDateTime.now())
                        .build())
                .build();
    }

    /**
     * 메타데이터와 함께 응답 생성
     */
    public static <T> CrudOperationResponse<T> withMetadata(CrudOperationResponse<T> response,
            OperationMetadata metadata) {
        response.setMetadata(metadata);
        return response;
    }

    /**
     * 간단한 성공 응답 생성
     */
    public static <T> CrudOperationResponse<T> success(T data, String operationType, String resourceType,
            String message) {
        return CrudOperationResponse.<T>builder()
                .data(data)
                .operation(OperationInfo.builder()
                        .operationType(operationType.toUpperCase())
                        .resourceType(resourceType)
                        .success(true)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build())
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static <T> CrudOperationResponse<T> failure(String operationType, String resourceType, String errorMessage) {
        return CrudOperationResponse.<T>builder()
                .operation(OperationInfo.builder()
                        .operationType(operationType.toUpperCase())
                        .resourceType(resourceType)
                        .success(false)
                        .message(errorMessage)
                        .timestamp(LocalDateTime.now())
                        .build())
                .build();
    }
}
