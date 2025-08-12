package api.v2.cms.admin.controller;

import api.v2.cms.config.DynamicDataSourceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 동적 테넌트 관리 API
 * - 런타임에 테넌트 추가/제거
 * - 데이터소스 상태 모니터링
 * - 테넌트별 설정 관리
 */
@RestController
@RequestMapping("/admin/tenants")
public class TenantManagementController {
    private static final Logger logger = LoggerFactory.getLogger(TenantManagementController.class);

    @Autowired
    private DynamicDataSourceConfiguration.DynamicDataSourceManager dataSourceManager;

    /**
     * 새 테넌트 추가
     */
    @PostMapping("/{tenantId}")
    public ResponseEntity<?> createTenant(
            @PathVariable String tenantId,
            @RequestBody TenantCreateRequest request) {

        try {
            // 데이터소스 생성
            dataSourceManager.createTenantDataSource(
                    tenantId,
                    request.getUrl(),
                    request.getUsername(),
                    request.getPassword());

            logger.info("Successfully created tenant: {}", tenantId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tenant created successfully");
            response.put("tenantId", tenantId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to create tenant: {}", tenantId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create tenant: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 테넌트 제거
     */
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<?> removeTenant(@PathVariable String tenantId) {

        if ("integrated".equals(tenantId)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Cannot remove default tenant");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            dataSourceManager.removeDataSource(tenantId);

            logger.info("Successfully removed tenant: {}", tenantId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tenant removed successfully");
            response.put("tenantId", tenantId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to remove tenant: {}", tenantId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to remove tenant: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 모든 테넌트 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<?> getTenantsStatus() {
        try {
            Map<String, String> dataSourceInfo = dataSourceManager.getDataSourceInfo();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tenants", dataSourceInfo);
            response.put("totalTenants", dataSourceInfo.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get tenants status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get tenants status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 특정 테넌트 정보 조회
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<?> getTenantInfo(@PathVariable String tenantId) {
        try {
            boolean exists = dataSourceManager.hasDataSource(tenantId);

            if (!exists) {
                return ResponseEntity.notFound().build();
            }

            Map<String, String> allInfo = dataSourceManager.getDataSourceInfo();
            String tenantInfo = allInfo.get(tenantId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tenantId", tenantId);
            response.put("info", tenantInfo);
            response.put("exists", true);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get tenant info: {}", tenantId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get tenant info: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 테넌트 헬스체크
     */
    @GetMapping("/{tenantId}/health")
    public ResponseEntity<?> checkTenantHealth(@PathVariable String tenantId) {
        try {
            // 실제로는 데이터베이스 연결 테스트를 수행
            boolean isHealthy = dataSourceManager.hasDataSource(tenantId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tenantId", tenantId);
            response.put("healthy", isHealthy);
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to check tenant health: {}", tenantId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Health check failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 테넌트 생성 요청 DTO
     */
    public static class TenantCreateRequest {
        private String url;
        private String username;
        private String password;
        private String description;

        // Getters and Setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
