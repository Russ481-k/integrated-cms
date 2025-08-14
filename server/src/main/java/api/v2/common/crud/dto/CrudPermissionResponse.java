package api.v2.common.crud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CRUD 권한 확인 응답
 * 
 * 사용자의 리소스별 권한 정보를 상세히 제공하는 응답 형식
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CRUD 권한 확인 응답")
public class CrudPermissionResponse {

    @Schema(description = "전체 권한 확인 결과", example = "true")
    private boolean hasPermission;

    @Schema(description = "사용자 정보")
    private UserInfo user;

    @Schema(description = "리소스 정보")
    private ResourceInfo resource;

    @Schema(description = "권한 상세 정보")
    private PermissionDetails permissions;

    @Schema(description = "권한 확인 메타데이터")
    private PermissionMetadata metadata;

    /**
     * 사용자 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 정보")
    public static class UserInfo {
        @Schema(description = "사용자 UUID", example = "user-uuid-123")
        private String uuid;

        @Schema(description = "사용자명", example = "admin")
        private String username;

        @Schema(description = "사용자 역할", example = "ADMIN")
        private String role;

        @Schema(description = "권한 레벨", example = "HIGH")
        private String permissionLevel;

        @Schema(description = "소속 그룹", example = "admin-group")
        private String groupId;
    }

    /**
     * 리소스 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리소스 정보")
    public static class ResourceInfo {
        @Schema(description = "리소스 타입", example = "Menu")
        private String resourceType;

        @Schema(description = "리소스 ID", example = "123")
        private String resourceId;

        @Schema(description = "서비스 컨텍스트", example = "douzone")
        private String serviceContext;

        @Schema(description = "리소스 소유자", example = "creator-uuid")
        private String ownerId;

        @Schema(description = "리소스 상태", example = "ACTIVE")
        private String status;
    }

    /**
     * 권한 상세 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "권한 상세 정보")
    public static class PermissionDetails {
        @Schema(description = "읽기 권한", example = "true")
        private boolean canRead;

        @Schema(description = "생성 권한", example = "true")
        private boolean canCreate;

        @Schema(description = "수정 권한", example = "true")
        private boolean canUpdate;

        @Schema(description = "삭제 권한", example = "false")
        private boolean canDelete;

        @Schema(description = "관리 권한", example = "false")
        private boolean canManage;

        @Schema(description = "요청된 작업", example = "UPDATE")
        private String requestedOperation;

        @Schema(description = "권한 부여 이유")
        private List<String> grantedReasons;

        @Schema(description = "권한 거부 이유")
        private List<String> deniedReasons;

        @Schema(description = "추가 권한 정보")
        private Map<String, Object> additionalPermissions;
    }

    /**
     * 권한 확인 메타데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "권한 확인 메타데이터")
    public static class PermissionMetadata {
        @Schema(description = "확인 시간", example = "2024-01-15T10:30:00")
        private LocalDateTime checkedAt;

        @Schema(description = "확인 소요 시간 (ms)", example = "12")
        private Long checkDurationMs;

        @Schema(description = "적용된 권한 규칙 수", example = "5")
        private Integer appliedRulesCount;

        @Schema(description = "권한 확인 방법", example = "ROLE_BASED")
        private String checkMethod;

        @Schema(description = "캐시 사용 여부", example = "true")
        private Boolean cacheUsed;

        @Schema(description = "권한 만료 시간", example = "2024-01-15T18:30:00")
        private LocalDateTime expiresAt;

        @Schema(description = "세션 ID", example = "session-123")
        private String sessionId;
    }

    // == 팩토리 메서드들 ==

    /**
     * 권한 허용 응답 생성
     */
    public static CrudPermissionResponse allowed(String username, String role, String resourceType, String operation) {
        return CrudPermissionResponse.builder()
                .hasPermission(true)
                .user(UserInfo.builder()
                        .username(username)
                        .role(role)
                        .build())
                .resource(ResourceInfo.builder()
                        .resourceType(resourceType)
                        .build())
                .permissions(PermissionDetails.builder()
                        .requestedOperation(operation)
                        .grantedReasons(java.util.Arrays.asList("Role-based permission granted"))
                        .build())
                .metadata(PermissionMetadata.builder()
                        .checkedAt(LocalDateTime.now())
                        .checkMethod("ROLE_BASED")
                        .build())
                .build();
    }

    /**
     * 권한 거부 응답 생성
     */
    public static CrudPermissionResponse denied(String username, String role, String resourceType, String operation,
            List<String> reasons) {
        return CrudPermissionResponse.builder()
                .hasPermission(false)
                .user(UserInfo.builder()
                        .username(username)
                        .role(role)
                        .build())
                .resource(ResourceInfo.builder()
                        .resourceType(resourceType)
                        .build())
                .permissions(PermissionDetails.builder()
                        .requestedOperation(operation)
                        .deniedReasons(reasons)
                        .build())
                .metadata(PermissionMetadata.builder()
                        .checkedAt(LocalDateTime.now())
                        .checkMethod("ROLE_BASED")
                        .build())
                .build();
    }

    /**
     * 상세 권한 정보와 함께 응답 생성
     */
    public static CrudPermissionResponse detailed(UserInfo user, ResourceInfo resource, PermissionDetails permissions) {
        return CrudPermissionResponse.builder()
                .hasPermission(determineOverallPermission(permissions))
                .user(user)
                .resource(resource)
                .permissions(permissions)
                .metadata(PermissionMetadata.builder()
                        .checkedAt(LocalDateTime.now())
                        .checkMethod("DETAILED")
                        .build())
                .build();
    }

    /**
     * 전체 권한 여부 판단
     */
    private static boolean determineOverallPermission(PermissionDetails permissions) {
        String operation = permissions.getRequestedOperation();
        if (operation == null)
            return false;

        switch (operation.toUpperCase()) {
            case "READ":
                return permissions.isCanRead();
            case "CREATE":
                return permissions.isCanCreate();
            case "UPDATE":
                return permissions.isCanUpdate();
            case "DELETE":
                return permissions.isCanDelete();
            case "MANAGE":
                return permissions.isCanManage();
            default:
                return false;
        }
    }
}
