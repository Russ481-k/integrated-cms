package api.v2.integrated_cms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.common.config.ServiceContextHolder;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.common.auth.annotation.RequireIntegratedCmsAccess;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 통합 CMS 관리 컨트롤러
 * 
 * 하이브리드 보안 시스템 적용:
 * 1. SecurityConfig: /api/v2/integrated-cms/** → SUPER_ADMIN, SERVICE_ADMIN
 * 2. 클래스 레벨: @RequireIntegratedCmsAccess (커스텀 비즈니스 로직)
 * 3. 메서드 레벨: @PreAuthorize (세밀한 권한 제어)
 * 
 * integrated_cms DB에 접근하여 통합 관리 기능 제공:
 * - 시스템 관리자 관리 (ADMIN_USER 테이블)
 * - 서비스 등록/관리 (SERVICE 테이블)
 * - 통합 권한 관리 (SERVICE_PERMISSION 테이블)
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@RestController
@RequestMapping("/integrated-cms")
@RequireIntegratedCmsAccess // 2계층: 커스텀 비즈니스 권한 로직
@RequiredArgsConstructor
public class IntegratedCmsController {

    /**
     * 통합 CMS 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> health() {
        log.info("Health check requested for integrated CMS");

        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "integrated-cms");
        healthInfo.put("serviceContext", ServiceContextHolder.getCurrentServiceId());
        healthInfo.put("message", "통합 CMS가 정상적으로 동작중입니다.");

        return ResponseEntity.ok(ApiResponseSchema.success(healthInfo));
    }

    /**
     * 통합 관리자 목록 조회
     * 3계층: 세밀한 권한 제어 - SUPER_ADMIN만 가능
     */
    @GetMapping("/admins")
    @PreAuthorize("@integratedCmsAccessChecker.canManageAdmins(authentication)")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> getAdminUsers() {
        log.info("Admin users list requested with hybrid authentication");

        // TODO: AdminUserService를 통한 실제 데이터 조회 구현
        Map<String, Object> response = new HashMap<>();
        response.put("message", "통합 관리자 목록 조회 기능 (하이브리드 인증 적용)");
        response.put("serviceContext", ServiceContextHolder.getCurrentServiceId());
        response.put("endpoint", "/integrated-cms/admins");
        response.put("table", "integrated_cms.ADMIN_USER");
        response.put("authenticationLayers", "SecurityConfig + @RequireIntegratedCmsAccess + @PreAuthorize");

        return ResponseEntity.ok(ApiResponseSchema.success(response, "하이브리드 인증 시스템 적용됨"));
    }

    // 서비스 관리 기능은 ServiceManagementController에서 처리
    // /api/v2/integrated-cms/services/** 경로는 ServiceManagementController가 담당

    /**
     * 시스템 권한 목록 조회 (임시 구현)
     */
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> getPermissions() {
        log.info("System permissions list requested");

        // TODO: SystemService를 통한 실제 권한 데이터 조회 구현
        Map<String, Object> response = new HashMap<>();
        response.put("message", "시스템 권한 목록 조회 기능 (구현 예정)");
        response.put("serviceContext", ServiceContextHolder.getCurrentServiceId());
        response.put("endpoint", "/integrated-cms/permissions");
        response.put("table", "integrated_cms.SERVICE_PERMISSION");

        return ResponseEntity.ok(ApiResponseSchema.success(response, "임시 응답"));
    }

}
