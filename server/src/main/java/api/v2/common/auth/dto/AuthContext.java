package api.v2.common.auth.dto;

import lombok.Builder;
import lombok.Data;
import api.v2.cms.user.domain.UserRoleType;

/**
 * 인증 컨텍스트 정보
 * CMS와 integrated_cms 간 공통으로 사용되는 인증 관련 정보를 담는 DTO
 */
@Data
@Builder
public class AuthContext {

    /**
     * 서비스 타입 (cms | integrated_cms)
     */
    private String serviceType;

    /**
     * 서비스 ID (douzone, service1, etc. - cms 타입에서만 사용)
     */
    private String serviceId;

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
     * 액세스 토큰
     */
    private String accessToken;

    /**
     * 리프레시 토큰
     */
    private String refreshToken;

    /**
     * 요청 IP 주소
     */
    private String clientIp;

    /**
     * User-Agent 정보
     */
    private String userAgent;

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
            return String.format("[IntegratedCMS] %s (%s)", username, userRole);
        } else {
            return String.format("[ServiceCMS:%s] %s (%s)", serviceId, username, userRole);
        }
    }
}
