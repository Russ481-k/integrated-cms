package api.v2.common.crud.service.impl;

import lombok.extern.slf4j.Slf4j;
import api.v2.common.crud.dto.CrudContext;
import api.v2.common.crud.service.CommonCrudService;
import api.v2.common.user.domain.UserRoleType;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 공통 CRUD 서비스 기본 구현체
 * 
 * 기본적인 컨텍스트 생성과 권한 확인 로직을 제공
 * 구체적인 CRUD 로직은 각 서비스에서 구현
 * 
 * @param <T>             엔티티 타입
 * @param <ID>            엔티티 ID 타입
 * @param <CreateRequest> 생성 요청 DTO 타입
 * @param <UpdateRequest> 수정 요청 DTO 타입
 * @param <ResponseDto>   응답 DTO 타입
 */
@Slf4j
public abstract class AbstractCommonCrudServiceImpl<T, ID, CreateRequest, UpdateRequest, ResponseDto>
        implements CommonCrudService<T, ID, CreateRequest, UpdateRequest, ResponseDto> {

    @Override
    public CrudContext createCrudContext(HttpServletRequest request, String serviceType, String serviceId,
            String resourceName) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        return CrudContext.builder()
                .serviceType(serviceType)
                .serviceId(serviceId)
                .resourceName(resourceName)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .requestTime(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public boolean hasListPermission(CrudContext context) {
        // 기본적으로 모든 인증된 사용자는 목록 조회 가능
        // 각 서비스에서 필요시 오버라이드
        return context.getUserRole() != null;
    }

    @Override
    public boolean hasReadPermission(ID id, CrudContext context) {
        // 기본적으로 모든 인증된 사용자는 읽기 가능
        // 각 서비스에서 필요시 오버라이드
        return context.getUserRole() != null;
    }

    @Override
    public boolean hasCreatePermission(CrudContext context) {
        // 기본적으로 ADMIN 이상 권한 필요
        UserRoleType role = context.getUserRole();
        return role != null && (role == UserRoleType.SUPER_ADMIN ||
                role == UserRoleType.SERVICE_ADMIN ||
                role == UserRoleType.SITE_ADMIN ||
                role == UserRoleType.ADMIN);
    }

    @Override
    public boolean hasUpdatePermission(ID id, CrudContext context) {
        // 기본적으로 ADMIN 이상 권한 필요
        UserRoleType role = context.getUserRole();
        return role != null && (role == UserRoleType.SUPER_ADMIN ||
                role == UserRoleType.SERVICE_ADMIN ||
                role == UserRoleType.SITE_ADMIN ||
                role == UserRoleType.ADMIN);
    }

    @Override
    public boolean hasDeletePermission(ID id, CrudContext context) {
        // 기본적으로 SITE_ADMIN 이상 권한 필요 (일반 ADMIN은 삭제 불가)
        UserRoleType role = context.getUserRole();
        return role != null && (role == UserRoleType.SUPER_ADMIN ||
                role == UserRoleType.SERVICE_ADMIN ||
                role == UserRoleType.SITE_ADMIN);
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    protected String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 감사 로그 기록
     */
    protected void logAuditEvent(String action, ID id, CrudContext context) {
        log.info("AUDIT: {} {} - ID: {}, {}", action, context.getResourceName(), id, context.getAuditInfo());
    }

    /**
     * 권한 검사 실패 시 로그
     */
    protected void logPermissionDenied(String action, ID id, CrudContext context) {
        log.warn("PERMISSION_DENIED: {} {} - ID: {}, {}", action, context.getResourceName(), id,
                context.getAuditInfo());
    }
}
