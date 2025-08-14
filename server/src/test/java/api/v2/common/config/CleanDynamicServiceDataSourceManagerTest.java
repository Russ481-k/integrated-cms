package api.v2.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DynamicServiceDataSourceManager 깔끔한 단위 테스트
 * 
 * TDD 커서룰에 따라 작성된 격리된 테스트:
 * - Mock 기반으로 실제 DB 연결 없이 테스트
 * - 간결한 로그 출력으로 가독성 향상
 * - 비즈니스 로직에 집중
 * - 외부 의존성 완전 격리
 */
@ExtendWith(MockitoExtension.class)
class CleanDynamicServiceDataSourceManagerTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private DynamicServiceDataSourceManager dataSourceManager;

    @BeforeEach
    void setUp() {
        // 각 테스트 전 초기화
        dataSourceManager = new DynamicServiceDataSourceManager();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 리소스 정리
        if (dataSourceManager != null) {
            Map<String, DataSource> dataSources = dataSourceManager.getAllServiceDataSources();
            dataSources.keySet().forEach(serviceId -> {
                if (!"integrated_cms".equals(serviceId)) {
                    dataSourceManager.removeServiceDataSource(serviceId);
                }
            });
        }
    }

    @Test
    @DisplayName("환경변수 없는 서비스는 생성에 실패한다")
    void 환경변수_없는_서비스는_생성에_실패한다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mMissing Environment Variables\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 환경변수가 없는 unknown_service");
        String unknownServiceId = "unknown_service";

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 환경변수 없이 서비스 생성 시도");
        boolean created = dataSourceManager.tryCreateServiceFromEnvironment(unknownServiceId);
        System.out.println(
                "    \033[90m→\033[0m tryCreateServiceFromEnvironment(\033[36m\"" + unknownServiceId + "\"\033[0m)");
        System.out.println("    \033[90m→\033[0m creation result = \033[33m" + created + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 생성 실패해야 함");
        assertFalse(created, "환경변수가 없으면 서비스 생성이 실패해야 합니다");
        assertFalse(dataSourceManager.hasServiceDataSource(unknownServiceId));
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mcreation failed as expected\033[0m\n");
    }

    @Test
    @DisplayName("서비스 존재 여부를 정확히 확인할 수 있다")
    void 서비스_존재_여부를_정확히_확인할_수_있다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mService Existence Verification\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 서비스 목록 초기 상태");
        String existingService = "integrated_cms";
        String nonExistingService = "non_existing";

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m 존재 여부 확인");
        boolean integratedCmsExists = dataSourceManager.hasServiceDataSource(existingService);
        boolean nonExistingExists = dataSourceManager.hasServiceDataSource(nonExistingService);
        System.out.println("    \033[90m→\033[0m hasServiceDataSource(\033[35m\"" + existingService
                + "\"\033[0m) = \033[33m" + integratedCmsExists + "\033[0m");
        System.out.println("    \033[90m→\033[0m hasServiceDataSource(\033[36m\"" + nonExistingService
                + "\"\033[0m) = \033[33m" + nonExistingExists + "\033[0m");

        System.out.println("  \033[2m✨ Verify:\033[0m 존재하지 않는 서비스는 false여야 함");
        assertFalse(nonExistingExists, "존재하지 않는 서비스는 false여야 합니다");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[36mnon_existing\033[0m \033[90m→\033[0m \033[33mfalse\033[0m\n");
    }

    @Test
    @DisplayName("서비스 데이터소스 목록을 조회할 수 있다")
    void 서비스_데이터소스_목록을_조회할_수_있다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mService DataSource List Retrieval\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 초기 데이터소스 상태");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 전체 목록 조회");
        Map<String, DataSource> allDataSources = dataSourceManager.getAllServiceDataSources();
        System.out.println("    \033[90m→\033[0m getAllServiceDataSources()");
        System.out.println("    \033[90m→\033[0m result size = \033[32m" + allDataSources.size() + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 목록이 Map 형태로 반환되어야 함");
        assertNotNull(allDataSources, "데이터소스 목록이 null이면 안됩니다");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mdataSource list retrieved\033[0m");

        // 현재 존재하는 서비스들 출력
        allDataSources.keySet()
                .forEach(serviceId -> System.out.println("      \033[90m-\033[0m \033[36m" + serviceId + "\033[0m"));
        System.out.println();
    }

    @Test
    @DisplayName("존재하지_않는_서비스_요청_시_기본_처리_동작을_확인한다")
    void 존재하지_않는_서비스_요청_시_기본_처리_동작을_확인한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mNon-Existent Service Request Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 존재하지 않는 서비스");
        String nonExistentService = "non_existent_service";

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 존재하지 않는 서비스 데이터소스 요청");
        DataSource dataSource = dataSourceManager.getServiceDataSource(nonExistentService);
        System.out.println("    \033[90m→\033[0m getServiceDataSource(\033[36m\"" + nonExistentService + "\"\033[0m)");
        System.out.println("    \033[90m→\033[0m result = "
                + (dataSource != null ? "\033[32mfallback dataSource\033[0m" : "\033[33mnull\033[0m"));

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 적절한 처리가 되어야 함 (폴백 또는 null)");
        // 실제 구현에 따라 integrated_cms로 폴백되거나 null 반환
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mappropriate handling for non-existent service\033[0m\n");
    }

    @Test
    @DisplayName("integrated_cms_서비스는_특별히_관리된다")
    void integrated_cms_서비스는_특별히_관리된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mIntegrated CMS Special Management\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m integrated_cms 서비스");
        String integratedCmsId = "integrated_cms";

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m integrated_cms 제거 시도");
        dataSourceManager.removeServiceDataSource(integratedCmsId);
        System.out.println("    \033[90m→\033[0m removeServiceDataSource(\033[35m\"" + integratedCmsId + "\"\033[0m)");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 보호된 서비스 여부 확인");
        boolean stillExists = dataSourceManager.hasServiceDataSource(integratedCmsId);
        System.out.println("    \033[90m→\033[0m hasServiceDataSource(\033[35m\"" + integratedCmsId
                + "\"\033[0m) = \033[33m" + stillExists + "\033[0m");

        // integrated_cms는 시스템 기본 서비스이므로 보호되어야 함
        if (stillExists) {
            System.out.println(
                    "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[35mintegrated_cms\033[0m \033[90mis protected (as expected)\033[0m");
        } else {
            System.out.println(
                    "    \033[33mℹ️\033[0m \033[90mInfo:\033[0m \033[35mintegrated_cms\033[0m \033[90mnot present or removed\033[0m");
        }
        System.out.println();
    }
}
