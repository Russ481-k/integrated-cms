package api.v2.common.crud.dto;

import lombok.Builder;
import lombok.Data;
import api.v2.cms.user.domain.UserRoleType;

/**
 * CRUD 컨텍스트 정보
 * CMS와 integrated_cms 간 공통으로 사용되는 CRUD 관련 정보를 담는 DTO
 * AuthContext를 확장하여 CRUD 특화 기능 추가
 */
@Data
@Builder
public class CrudContext {

    /**
     * 서비스 타입 (cms | integrated_cms)
     */
    private String serviceType;

    /**
     * 서비스 ID (douzone, service1, etc. - cms 타입에서만 사용)
     */
    private String serviceId;

    /**
     * 리소스 이름 (menu, content, popup, user 등)
     */
    private String resourceName;

    /**
     * 현재 사용자 UUID
     */
    private String userUuid;

    /**
     * 현재 사용자 이름
     */
    private String username;

    /**
     * 현재 사용자 역할
     */
    private UserRoleType userRole;

    /**
     * 요청 IP 주소
     */
    private String clientIp;

    /**
     * User-Agent 정보
     */
    private String userAgent;

    /**
     * 요청 시간 (추적용)
     */
    private String requestTime;

    /**
     * 추가 메타데이터 (확장 가능)
     */
    private String metadata;

    /**
     * 통합 CMS 컨텍스트인지 확인
     */
    public boolean isIntegratedCms() {
        return "integrated_cms".equals(serviceType);
    }

    /**
     * 서비스별 CMS 컨텍스트인지 확인
     */
    public boolean isServiceCms() {
        return "cms".equals(serviceType);
    }

    /**
     * 로그 출력용 컨텍스트 정보
     */
    public String getLogContext() {
        if (isIntegratedCms()) {
            return String.format("[IntegratedCMS:%s] %s (%s)", resourceName, username, userRole);
        } else {
            return String.format("[ServiceCMS:%s:%s] %s (%s)", serviceId, resourceName, username, userRole);
        }
    }

    /**
     * 감사 로그용 정보
     */
    public String getAuditInfo() {
        return String.format("User: %s, Role: %s, IP: %s, Service: %s, Resource: %s",
                username, userRole, clientIp, serviceType, resourceName);
    }
}
