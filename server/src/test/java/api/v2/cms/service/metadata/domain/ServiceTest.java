package api.v2.cms.service.metadata.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Service 엔티티 단위 테스트
 * 
 * 서비스 메타데이터 엔티티의 핵심 기능을 검증:
 * - 기본 정보 관리
 * - 연결 정보 관리
 * - JSON 설정 처리
 * - 상태 관리
 */
class ServiceTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("서비스 엔티티가 올바르게 생성된다")
    void 서비스_엔티티가_올바르게_생성된다() {
        System.out.println("\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mService Entity Creation\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 서비스 엔티티 생성 정보 준비");
        String serviceId = "test-uuid";
        String serviceCode = "douzone";
        String serviceName = "두존 CMS";
        String serviceDomain = "https://douzone.example.com";
        String apiBaseUrl = "https://api.douzone.example.com";

        System.out.println("    \033[90m→\033[0m Service ID: \033[36m" + serviceId + "\033[0m");
        System.out.println("    \033[90m→\033[0m Code: \033[36m" + serviceCode + "\033[0m");
        System.out.println("    \033[90m→\033[0m Name: \033[36m" + serviceName + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 서비스 엔티티 생성");
        Service service = Service.builder()
                .serviceId(serviceId)
                .serviceCode(serviceCode)
                .serviceName(serviceName)
                .serviceDomain(serviceDomain)
                .apiBaseUrl(apiBaseUrl)
                .status(ServiceStatus.ACTIVE)
                .description("두존 그룹웨어 CMS 시스템")
                .createdBy("admin-uuid")
                .createdIp("127.0.0.1")
                .build();

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 엔티티 속성 검증");
        assertNotNull(service);
        assertEquals(serviceId, service.getServiceId());
        assertEquals(serviceCode, service.getServiceCode());
        assertEquals(serviceName, service.getServiceName());
        assertEquals(serviceDomain, service.getServiceDomain());
        assertEquals(apiBaseUrl, service.getApiBaseUrl());
        assertEquals(ServiceStatus.ACTIVE, service.getStatus());
        assertNotNull(service.getCreatedAt());

        System.out.println("    \033[32m✓\033[0m \033[90mEntity created:\033[0m \033[32mSuccess\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mProperties:\033[0m \033[32mValidated\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mStatus:\033[0m \033[32m" + service.getStatus() + "\033[0m\n");
    }

    @Test
    @DisplayName("서비스 설정이 JSON으로 올바르게 처리된다")
    void 서비스_설정이_JSON으로_올바르게_처리된다() throws Exception {
        System.out
                .println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mService Config JSON Processing\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 서비스 설정 JSON 준비");
        Service service = Service.builder()
                .serviceId("test-uuid")
                .serviceCode("douzone")
                .serviceName("더존 CMS")
                .status(ServiceStatus.ACTIVE)
                .build();

        Map<String, Object> features = new HashMap<>();
        features.put("board", true);
        features.put("gallery", true);
        features.put("schedule", false);

        Map<String, Object> config = new HashMap<>();
        config.put("maxFileSize", "100MB");
        config.put("allowedFileTypes", new String[] { "jpg", "png", "pdf" });
        config.put("features", features);

        System.out.println("    \033[90m→\033[0m Config: \033[36m" + config + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m JSON 설정 저장 및 조회");
        service.setConfig(objectMapper.writeValueAsString(config));

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m JSON 파싱 및 값 검증");
        Map<String, Object> parsedConfig = objectMapper.readValue(service.getConfig(), Map.class);

        assertEquals("100MB", parsedConfig.get("maxFileSize"));
        assertTrue(((Map) parsedConfig.get("features")).containsKey("board"));
        assertEquals(true, ((Map) parsedConfig.get("features")).get("board"));

        System.out.println("    \033[32m✓\033[0m \033[90mJSON serialization:\033[0m \033[32mSuccess\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mJSON deserialization:\033[0m \033[32mSuccess\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mConfig values:\033[0m \033[32mValidated\033[0m\n");
    }

    @Test
    @DisplayName("하이브리드 연결 정보가 올바르게 처리된다")
    void 하이브리드_연결_정보가_올바르게_처리된다() throws Exception {
        System.out.println(
                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mHybrid Connection Info Processing\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 하이브리드 연결 정보 준비");
        Service service = Service.builder()
                .serviceId("test-uuid")
                .serviceCode("douzone")
                .serviceName("더존 CMS")
                .status(ServiceStatus.ACTIVE)
                .build();

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("url", "jdbc:mysql://localhost:3306/douzone");
        fallback.put("username", "admin");
        fallback.put("password", "<encrypted>");

        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("mode", "hybrid");
        connectionInfo.put("envKeyPattern", "DOUZONE_DB_*");
        connectionInfo.put("fallback", fallback);

        System.out.println("    \033[90m→\033[0m Connection Mode: \033[36mhybrid\033[0m");
        System.out.println("    \033[90m→\033[0m Env Pattern: \033[36mDOUZONE_DB_*\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 연결 정보 저장 및 조회");
        service.setDbConnectionInfo(objectMapper.writeValueAsString(connectionInfo));

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 연결 정보 파싱 및 검증");
        Map<String, Object> parsedInfo = objectMapper.readValue(service.getDbConnectionInfo(), Map.class);

        assertEquals("hybrid", parsedInfo.get("mode"));
        assertEquals("DOUZONE_DB_*", parsedInfo.get("envKeyPattern"));
        assertNotNull(((Map) parsedInfo.get("fallback")).get("url"));

        System.out.println("    \033[32m✓\033[0m \033[90mConnection info stored:\033[0m \033[32mSuccess\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mHybrid mode:\033[0m \033[32mConfigured\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mFallback config:\033[0m \033[32mValidated\033[0m\n");
    }

    @Test
    @DisplayName("서비스 상태 변경이 올바르게 처리된다")
    void 서비스_상태_변경이_올바르게_처리된다() {
        System.out.println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mService Status Management\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 활성 상태의 서비스 준비");
        Service service = Service.builder()
                .serviceId("test-uuid")
                .serviceCode("douzone")
                .serviceName("두존 CMS")
                .status(ServiceStatus.ACTIVE)
                .build();

        System.out.println("    \033[90m→\033[0m Initial Status: \033[32mACTIVE\033[0m");

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m 상태 변경 시나리오 실행");

        // 1. MAINTENANCE로 변경
        service.setStatus(ServiceStatus.MAINTENANCE);
        assertEquals(ServiceStatus.MAINTENANCE, service.getStatus());
        System.out.println("    \033[32m✓\033[0m \033[90mStatus change to MAINTENANCE:\033[0m \033[33mSuccess\033[0m");

        // 2. INACTIVE로 변경
        service.setStatus(ServiceStatus.INACTIVE);
        assertEquals(ServiceStatus.INACTIVE, service.getStatus());
        System.out.println("    \033[32m✓\033[0m \033[90mStatus change to INACTIVE:\033[0m \033[31mSuccess\033[0m");

        // 3. 다시 ACTIVE로 변경
        service.setStatus(ServiceStatus.ACTIVE);
        assertEquals(ServiceStatus.ACTIVE, service.getStatus());
        System.out.println("    \033[32m✓\033[0m \033[90mStatus change to ACTIVE:\033[0m \033[32mSuccess\033[0m\n");
    }
}
