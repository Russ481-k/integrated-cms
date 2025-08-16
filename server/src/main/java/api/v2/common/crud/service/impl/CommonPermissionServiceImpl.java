package api.v2.common.crud.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.common.crud.dto.CrudContext;
import api.v2.common.crud.dto.CrudPermissionResponse;
import api.v2.common.crud.service.CommonPermissionService;
import api.v2.common.user.domain.UserRoleType;
import api.v2.common.auth.service.IntegratedCmsAccessChecker;
import api.v2.common.auth.service.ServiceAccessChecker;
import api.v2.common.auth.service.ContentPermissionChecker;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 공통 권한 검증 서비스 구현체
 * 
 * 기존의 개별 권한 체커들을 통합하여 일관된 권한 검증을 제공
 * 권한 캐싱, 감사 로깅, 상세 권한 정보 제공 등의 기능 포함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonPermissionServiceImpl implements CommonPermissionService {

    private final IntegratedCmsAccessChecker integratedCmsAccessChecker;
    private final ServiceAccessChecker serviceAccessChecker;
    private final ContentPermissionChecker contentPermissionChecker;

    @Override
    public CrudPermissionResponse checkCrudPermission(CrudContext context, String operation, String resourceId) {
        long startTime = System.currentTimeMillis();

        try {
            // 기본 권한 확인
            boolean hasPermission;
            switch (operation.toUpperCase()) {
                case "READ":
                    hasPermission = hasReadPermission(context, resourceId);
                    break;
                case "CREATE":
                    hasPermission = hasCreatePermission(context);
                    break;
                case "UPDATE":
                    hasPermission = hasUpdatePermission(context, resourceId);
                    break;
                case "DELETE":
                    hasPermission = hasDeletePermission(context, resourceId);
                    break;
                case "LIST":
                    hasPermission = hasListPermission(context);
                    break;
                default:
                    hasPermission = false;
                    break;
            }

            // 권한 확인 이유 수집
            List<String> reasons = gatherPermissionReasons(context, operation, hasPermission);

            // 상세 응답 생성
            return CrudPermissionResponse.builder()
                    .hasPermission(hasPermission)
                    .user(CrudPermissionResponse.UserInfo.builder()
                            .uuid(context.getUserUuid())
                            .username(context.getUsername())
                            .role(context.getUserRole() != null ? context.getUserRole().name() : "UNKNOWN")
                            .permissionLevel(getPermissionLevel(context.getUserRole()))
                            .build())
                    .resource(CrudPermissionResponse.ResourceInfo.builder()
                            .resourceType(context.getResourceName())
                            .resourceId(resourceId)
                            .serviceContext(context.getServiceId())
                            .build())
                    .permissions(CrudPermissionResponse.PermissionDetails.builder()
                            .requestedOperation(operation.toUpperCase())
                            .canRead(hasReadPermission(context, resourceId))
                            .canCreate(hasCreatePermission(context))
                            .canUpdate(hasUpdatePermission(context, resourceId))
                            .canDelete(hasDeletePermission(context, resourceId))
                            .grantedReasons(hasPermission ? reasons : null)
                            .deniedReasons(!hasPermission ? reasons : null)
                            .build())
                    .metadata(CrudPermissionResponse.PermissionMetadata.builder()
                            .checkedAt(LocalDateTime.now())
                            .checkDurationMs(System.currentTimeMillis() - startTime)
                            .checkMethod("HYBRID_RBAC")
                            .appliedRulesCount(countAppliedRules(context))
                            .build())
                    .build();

        } finally {
            // 권한 확인 감사 로그 기록
            logPermissionCheck(context, operation, resourceId, false, "Permission check completed");
        }
    }

    @Override
    public boolean hasListPermission(CrudContext context) {
        // 기본적으로 모든 인증된 사용자는 목록 조회 가능
        return context.getUserRole() != null;
    }

    @Override
    public boolean hasReadPermission(CrudContext context, String resourceId) {
        // 기본적으로 모든 인증된 사용자는 읽기 가능
        return context.getUserRole() != null;
    }

    @Override
    public boolean hasCreatePermission(CrudContext context) {
        // ADMIN 이상 권한 필요
        UserRoleType role = context.getUserRole();
        return hasRolePermission(role, UserRoleType.ADMIN);
    }

    @Override
    public boolean hasUpdatePermission(CrudContext context, String resourceId) {
        // ADMIN 이상 권한 필요
        UserRoleType role = context.getUserRole();
        return hasRolePermission(role, UserRoleType.ADMIN);
    }

    @Override
    public boolean hasDeletePermission(CrudContext context, String resourceId) {
        // SITE_ADMIN 이상 권한 필요 (일반 ADMIN은 삭제 불가)
        UserRoleType role = context.getUserRole();
        return hasRolePermission(role, UserRoleType.SITE_ADMIN);
    }

    @Override
    public boolean hasIntegratedCmsAccess(CrudContext context) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return integratedCmsAccessChecker.hasAccess(auth);
    }

    @Override
    public boolean hasServiceCmsAccess(CrudContext context, String serviceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return serviceAccessChecker.hasAccess(auth, serviceId);
    }

    @Override
    public boolean hasRolePermission(UserRoleType userRole, UserRoleType requiredRole) {
        if (userRole == null || requiredRole == null) {
            return false;
        }

        // 역할 계층: SUPER_ADMIN > SERVICE_ADMIN > SITE_ADMIN > ADMIN > USER > GUEST
        switch (userRole) {
            case SUPER_ADMIN:
                return true; // 모든 권한
            case SERVICE_ADMIN:
                return requiredRole != UserRoleType.SUPER_ADMIN;
            case SITE_ADMIN:
                return requiredRole == UserRoleType.SITE_ADMIN ||
                        requiredRole == UserRoleType.ADMIN ||
                        requiredRole == UserRoleType.USER ||
                        requiredRole == UserRoleType.GUEST;
            case ADMIN:
                return requiredRole == UserRoleType.ADMIN ||
                        requiredRole == UserRoleType.USER ||
                        requiredRole == UserRoleType.GUEST;
            case USER:
                return requiredRole == UserRoleType.USER || requiredRole == UserRoleType.GUEST;
            case GUEST:
                return requiredRole == UserRoleType.GUEST;
            default:
                return false;
        }
    }

    @Override
    public boolean hasOwnerPermission(CrudContext context, String resourceId, String ownerId) {
        // 리소스 소유자이거나 ADMIN 이상 권한이 있으면 허용
        return context.getUserUuid().equals(ownerId) ||
                hasRolePermission(context.getUserRole(), UserRoleType.ADMIN);
    }

    @Override
    public boolean hasConditionalPermission(CrudContext context, String operation, String resourceId,
            Object conditions) {
        // 기본 권한 확인 후 추가 조건 확인
        boolean hasBasicPermission;
        switch (operation.toUpperCase()) {
            case "READ":
                hasBasicPermission = hasReadPermission(context, resourceId);
                break;
            case "CREATE":
                hasBasicPermission = hasCreatePermission(context);
                break;
            case "UPDATE":
                hasBasicPermission = hasUpdatePermission(context, resourceId);
                break;
            case "DELETE":
                hasBasicPermission = hasDeletePermission(context, resourceId);
                break;
            default:
                hasBasicPermission = false;
                break;
        }

        if (!hasBasicPermission) {
            return false;
        }

        // 추가 조건 확인 (구현체에서 확장 가능)
        return evaluateAdditionalConditions(context, operation, resourceId, conditions);
    }

    @Override
    public void refreshPermissionCache(String userUuid) {
        // 권한 캐시 갱신 로직 (향후 구현)
        log.debug("Refreshing permission cache for user: {}", userUuid);
    }

    @Override
    public void logPermissionCheck(CrudContext context, String operation, String resourceId, boolean granted,
            String reason) {
        log.info("PERMISSION_CHECK: {} - User: {}, Role: {}, Resource: {}:{}, Operation: {}, Granted: {}, Reason: {}",
                context.getLogContext(),
                context.getUsername(),
                context.getUserRole(),
                context.getResourceName(),
                resourceId,
                operation,
                granted,
                reason);
    }

    @Override
    public CrudPermissionResponse getUserPermissionInfo(CrudContext context) {
        return CrudPermissionResponse.builder()
                .hasPermission(true)
                .user(CrudPermissionResponse.UserInfo.builder()
                        .uuid(context.getUserUuid())
                        .username(context.getUsername())
                        .role(context.getUserRole() != null ? context.getUserRole().name() : "UNKNOWN")
                        .permissionLevel(getPermissionLevel(context.getUserRole()))
                        .build())
                .permissions(CrudPermissionResponse.PermissionDetails.builder()
                        .canRead(hasReadPermission(context, null))
                        .canCreate(hasCreatePermission(context))
                        .canUpdate(hasUpdatePermission(context, null))
                        .canDelete(hasDeletePermission(context, null))
                        .build())
                .metadata(CrudPermissionResponse.PermissionMetadata.builder()
                        .checkedAt(LocalDateTime.now())
                        .checkMethod("USER_INFO")
                        .build())
                .build();
    }

    @Override
    public CrudPermissionResponse getResourcePermissionMatrix(CrudContext context, String resourceType) {
        // 리소스 타입별 권한 매트릭스 생성 (향후 구현)
        return CrudPermissionResponse.builder()
                .hasPermission(true)
                .resource(CrudPermissionResponse.ResourceInfo.builder()
                        .resourceType(resourceType)
                        .serviceContext(context.getServiceId())
                        .build())
                .metadata(CrudPermissionResponse.PermissionMetadata.builder()
                        .checkedAt(LocalDateTime.now())
                        .checkMethod("MATRIX")
                        .build())
                .build();
    }

    /**
     * 권한 레벨 결정
     */
    private String getPermissionLevel(UserRoleType role) {
        if (role == null)
            return "NONE";

        switch (role) {
            case SUPER_ADMIN:
                return "HIGHEST";
            case SERVICE_ADMIN:
                return "HIGH";
            case SITE_ADMIN:
                return "MEDIUM_HIGH";
            case ADMIN:
                return "MEDIUM";
            case USER:
                return "LOW";
            case GUEST:
                return "LOWEST";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 권한 결정 이유 수집
     */
    private List<String> gatherPermissionReasons(CrudContext context, String operation, boolean granted) {
        List<String> reasons = new ArrayList<>();

        if (granted) {
            reasons.add("Role-based permission: " + context.getUserRole());
            if (context.isIntegratedCms()) {
                reasons.add("Integrated CMS access granted");
            } else {
                reasons.add("Service CMS access granted: " + context.getServiceId());
            }
        } else {
            reasons.add("Insufficient role: " + context.getUserRole());
            reasons.add("Operation not permitted: " + operation);
        }

        return reasons;
    }

    /**
     * 적용된 규칙 수 계산
     */
    private Integer countAppliedRules(CrudContext context) {
        int count = 1; // 기본 역할 확인

        if (context.isIntegratedCms()) {
            count++; // 통합 CMS 규칙
        } else {
            count++; // 서비스 CMS 규칙
        }

        return count;
    }

    /**
     * 추가 조건 평가
     */
    private boolean evaluateAdditionalConditions(CrudContext context, String operation, String resourceId,
            Object conditions) {
        // 기본적으로 추가 조건 없이 허용
        // 구현체에서 필요에 따라 확장
        return true;
    }
}
