package api.v2.cms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 테넌트 식별 및 컨텍스트 설정 인터셉터
 * - URL, 헤더, 서브도메인 등을 통한 테넌트 식별
 * - 동적 데이터소스 라우팅을 위한 컨텍스트 설정
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);

    @Value("${app.multi-tenant.tenant-resolution.default-tenant:integrated}")
    private String defaultTenant;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = resolveTenantId(request);

        if (tenantId == null) {
            tenantId = defaultTenant;
        }

        DynamicDataSourceConfiguration.TenantContext.setCurrentTenant(tenantId);
        logger.debug("Set tenant context: {} for request: {}", tenantId, request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        // 요청 완료 후 컨텍스트 정리
        DynamicDataSourceConfiguration.TenantContext.clear();
        logger.debug("Cleared tenant context for request: {}", request.getRequestURI());
    }

    /**
     * 다양한 방법으로 테넌트 ID 식별
     */
    private String resolveTenantId(HttpServletRequest request) {
        // 1. 헤더에서 확인
        String tenantFromHeader = request.getHeader("X-Tenant-ID");
        if (tenantFromHeader != null && !tenantFromHeader.trim().isEmpty()) {
            logger.debug("Tenant resolved from header: {}", tenantFromHeader);
            return tenantFromHeader.trim();
        }

        // 2. URL 패턴에서 확인
        String tenantFromPath = resolveTenantFromPath(request.getRequestURI());
        if (tenantFromPath != null) {
            logger.debug("Tenant resolved from path: {}", tenantFromPath);
            return tenantFromPath;
        }

        // 3. 서브도메인에서 확인
        String tenantFromSubdomain = resolveTenantFromSubdomain(request);
        if (tenantFromSubdomain != null) {
            logger.debug("Tenant resolved from subdomain: {}", tenantFromSubdomain);
            return tenantFromSubdomain;
        }

        // 4. Origin 헤더에서 확인 (프론트엔드 도메인 기반)
        String tenantFromOrigin = resolveTenantFromOrigin(request);
        if (tenantFromOrigin != null) {
            logger.debug("Tenant resolved from origin: {}", tenantFromOrigin);
            return tenantFromOrigin;
        }

        logger.debug("No specific tenant found, using default: {}", defaultTenant);
        return defaultTenant;
    }

    /**
     * URL 패턴에서 테넌트 추출
     * 예: /api/v1/arpina/*, /tenant/douzone/*, /cms/arpina/*
     */
    private String resolveTenantFromPath(String requestURI) {
        if (requestURI.startsWith("/api/v1/")) {
            String[] segments = requestURI.split("/");
            if (segments.length > 3) {
                String possibleTenant = segments[3];
                if (isValidTenant(possibleTenant)) {
                    return possibleTenant;
                }
            }
        }

        if (requestURI.startsWith("/tenant/")) {
            String[] segments = requestURI.split("/");
            if (segments.length > 2) {
                String possibleTenant = segments[2];
                if (isValidTenant(possibleTenant)) {
                    return possibleTenant;
                }
            }
        }

        if (requestURI.startsWith("/cms/")) {
            String[] segments = requestURI.split("/");
            if (segments.length > 2) {
                String possibleTenant = segments[2];
                if (isValidTenant(possibleTenant)) {
                    return possibleTenant;
                }
            }
        }

        return null;
    }

    /**
     * 서브도메인에서 테넌트 추출
     * 예: arpina.cms.example.com -> arpina
     */
    private String resolveTenantFromSubdomain(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (serverName != null && serverName.contains(".")) {
            String[] parts = serverName.split("\\.");
            if (parts.length > 2) {
                String possibleTenant = parts[0];
                if (isValidTenant(possibleTenant) && !possibleTenant.equals("www") && !possibleTenant.equals("api")) {
                    return possibleTenant;
                }
            }
        }
        return null;
    }

    /**
     * Origin 헤더에서 테넌트 추출
     * 프론트엔드 도메인에 따른 매핑
     */
    private String resolveTenantFromOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null) {
            origin = request.getHeader("Referer");
        }

        if (origin != null) {
            // 도메인별 테넌트 매핑
            if (origin.contains("arpina-cms") || origin.contains("arpina.")) {
                return "arpina";
            }
            if (origin.contains("douzone-cms") || origin.contains("douzone.")) {
                return "douzone";
            }
            if (origin.contains("localhost:3000") || origin.contains("admin")) {
                return "integrated";
            }
        }

        return null;
    }

    /**
     * 유효한 테넌트인지 검증
     * 나중에 데이터베이스나 설정에서 동적으로 로드 가능
     */
    private boolean isValidTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return false;
        }

        // 기본 테넌트들
        return tenantId.matches("^[a-zA-Z0-9_-]+$") &&
                (tenantId.equals("integrated") ||
                        tenantId.equals("arpina") ||
                        tenantId.equals("douzone") ||
                        tenantId.equals("cms"));
    }
}
