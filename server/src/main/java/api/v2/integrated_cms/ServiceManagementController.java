package api.v2.integrated_cms;

import api.v2.common.config.DynamicServiceDataSourceManager;
import api.v2.common.dto.ApiResponseSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 서비스 관리 API 컨트롤러
 * 
 * 통합 CMS에서 동적 서비스 데이터소스 관리
 * - 새로운 서비스 추가/제거
 * - 서비스 데이터소스 상태 모니터링
 * - 환경변수 기반 서비스 자동 감지
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@RestController
@RequestMapping("/integrated-cms/services")
@RequiredArgsConstructor
public class ServiceManagementController {

    private final DynamicServiceDataSourceManager dataSourceManager;

    /**
     * 모든 서비스 목록 및 상태 조회
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN')")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> getAllServices() {
        try {
            Map<String, String> serviceInfo = dataSourceManager.getServiceDataSourceInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("services", serviceInfo);
            response.put("totalCount", serviceInfo.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponseSchema.success(response, "서비스 목록 조회 성공"));

        } catch (Exception e) {
            log.error("Failed to get services", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("서비스 목록 조회 실패: " + e.getMessage(), "SERVICE_LIST_ERROR"));
        }
    }

    /**
     * 특정 서비스 정보 조회
     */
    @GetMapping("/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN')")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> getServiceInfo(@PathVariable String serviceId) {
        try {
            boolean exists = dataSourceManager.hasServiceDataSource(serviceId);
            
            if (!exists) {
                return ResponseEntity.notFound().build();
            }

            Map<String, String> allInfo = dataSourceManager.getServiceDataSourceInfo();
            String serviceInfo = allInfo.get(serviceId);

            Map<String, Object> response = new HashMap<>();
            response.put("serviceId", serviceId);
            response.put("status", serviceInfo);
            response.put("exists", true);

            return ResponseEntity.ok(ApiResponseSchema.success(response, "서비스 정보 조회 성공"));

        } catch (Exception e) {
            log.error("Failed to get service info: {}", serviceId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("서비스 정보 조회 실패: " + e.getMessage(), "SERVICE_INFO_ERROR"));
        }
    }

    /**
     * 환경변수에서 새로운 서비스 자동 감지 및 추가
     */
    @PostMapping("/{serviceId}/auto-detect")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> autoDetectService(@PathVariable String serviceId) {
        try {
            boolean created = dataSourceManager.tryCreateServiceFromEnvironment(serviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("serviceId", serviceId);
            response.put("created", created);

            if (created) {
                response.put("message", "환경변수에서 서비스 데이터소스 생성 성공");
                return ResponseEntity.ok(ApiResponseSchema.success(response, "서비스 자동 감지 성공"));
            } else {
                response.put("message", "환경변수에서 서비스 설정을 찾을 수 없음");
                return ResponseEntity.badRequest()
                        .body(ApiResponseSchema.error(response, "서비스 자동 감지 실패", "SERVICE_ENV_NOT_FOUND"));
            }

        } catch (Exception e) {
            log.error("Failed to auto-detect service: {}", serviceId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("서비스 자동 감지 실패: " + e.getMessage(), "SERVICE_AUTO_DETECT_ERROR"));
        }
    }

    /**
     * 서비스 데이터소스 수동 생성
     */
    @PostMapping("/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> createService(
            @PathVariable String serviceId,
            @RequestBody CreateServiceRequest request) {
        try {
            dataSourceManager.createServiceDataSource(serviceId, request.getUrl(), 
                    request.getUsername(), request.getPassword());
            
            Map<String, Object> response = new HashMap<>();
            response.put("serviceId", serviceId);
            response.put("created", true);
            response.put("url", request.getUrl());

            return ResponseEntity.ok(ApiResponseSchema.success(response, "서비스 생성 성공"));

        } catch (Exception e) {
            log.error("Failed to create service: {}", serviceId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("서비스 생성 실패: " + e.getMessage(), "SERVICE_CREATE_ERROR"));
        }
    }

    /**
     * 서비스 데이터소스 제거
     */
    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> removeService(@PathVariable String serviceId) {
        try {
            if ("integrated_cms".equals(serviceId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseSchema.error("integrated_cms 서비스는 제거할 수 없습니다.", "SERVICE_REMOVE_FORBIDDEN"));
            }

            dataSourceManager.removeServiceDataSource(serviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("serviceId", serviceId);
            response.put("removed", true);

            return ResponseEntity.ok(ApiResponseSchema.success(response, "서비스 제거 성공"));

        } catch (Exception e) {
            log.error("Failed to remove service: {}", serviceId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("서비스 제거 실패: " + e.getMessage(), "SERVICE_REMOVE_ERROR"));
        }
    }

    /**
     * 서비스 생성 요청 DTO
     */
    public static class CreateServiceRequest {
        private String url;
        private String username;
        private String password;

        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
