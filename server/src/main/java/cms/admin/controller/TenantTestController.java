package cms.admin.controller;

import cms.config.DynamicDataSourceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * 테넌트 동적 라우팅 테스트 컨트롤러
 * 인증 없이 테스트 가능한 엔드포인트 제공
 */
@RestController
@RequestMapping("/api/v1/test")
public class TenantTestController {
    private static final Logger logger = LoggerFactory.getLogger(TenantTestController.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DynamicDataSourceConfiguration.DynamicDataSourceManager dataSourceManager;

    /**
     * 현재 테넌트 정보 조회 (인증 불필요)
     */
    @GetMapping("/tenant")
    public ResponseEntity<?> getCurrentTenant() {
        try {
            String currentTenant = DynamicDataSourceConfiguration.TenantContext.getCurrentTenant();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("currentTenant", currentTenant);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get current tenant", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get tenant: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 데이터베이스 연결 테스트 (인증 불필요)
     */
    @GetMapping("/db")
    public ResponseEntity<?> testDatabaseConnection() {
        try {
            String currentTenant = DynamicDataSourceConfiguration.TenantContext.getCurrentTenant();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("currentTenant", currentTenant);

            // 데이터베이스 연결 테스트
            try (Connection connection = dataSource.getConnection()) {
                String dbUrl = connection.getMetaData().getURL();
                String dbName = connection.getCatalog();
                
                // 간단한 쿼리 실행
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT DATABASE() as current_db, NOW() as current_time")) {
                    if (rs.next()) {
                        response.put("database", rs.getString("current_db"));
                        response.put("serverTime", rs.getString("current_time"));
                    }
                }
                
                response.put("dbUrl", dbUrl);
                response.put("dbName", dbName);
                response.put("connectionValid", true);
                
            } catch (Exception dbError) {
                response.put("connectionValid", false);
                response.put("dbError", dbError.getMessage());
            }

            // 등록된 데이터소스 목록
            Map<String, String> allDataSources = dataSourceManager.getDataSourceInfo();
            response.put("availableDataSources", allDataSources);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to test database connection", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Database test failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 모든 테넌트 정보 조회 (인증 불필요)
     */
    @GetMapping("/tenants")
    public ResponseEntity<?> getAllTenants() {
        try {
            Map<String, String> allDataSources = dataSourceManager.getDataSourceInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tenants", allDataSources);
            response.put("totalCount", allDataSources.size());
            response.put("currentTenant", DynamicDataSourceConfiguration.TenantContext.getCurrentTenant());

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get all tenants", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get tenants: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
