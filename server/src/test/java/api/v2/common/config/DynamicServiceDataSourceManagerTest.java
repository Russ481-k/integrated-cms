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
 * DynamicServiceDataSourceManager 실제 DB 연결 테스트
 * 
 * TDD 커서룰에 따라 작성된 실제 Docker DB 환경 테스트:
 * - 실제 MariaDB 연결 (db:3306)
 * - 환경변수 기반 데이터소스 생성 테스트
 * - 폴백 메커니즘 테스트
 * - 간결한 로그 출력으로 가독성 향상
 */
@ExtendWith(MockitoExtension.class)
class DynamicServiceDataSourceManagerTest {

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
        System.out.println("\n" + repeat("=", 80));
        System.out.println("\033[1;96m🧪 DynamicServiceDataSourceManager 실제 DB 테스트 시작\033[0m");
        System.out.println(repeat("=", 80));

        dataSourceManager = new DynamicServiceDataSourceManager();
    }

    @AfterEach
    void tearDown() {
        System.out.println(repeat("-", 80));
        System.out.println("\033[1;92m✅ 테스트 정리 완료\033[0m");
        System.out.println(repeat("=", 80) + "\n");

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

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 생성 실패해야 함");
        assertFalse(created, "환경변수가 없으면 서비스 생성이 실패해야 합니다");
        assertFalse(dataSourceManager.hasServiceDataSource(unknownServiceId));
        System.out.println("    \033[32m✓\033[0m \033[90m결과:\033[0m \033[33m생성 실패 (예상대로)\033[0m");
    }

    @Test
    @DisplayName("integrated_cms 데이터소스를 생성할 수 있다")
    void integrated_cms_데이터소스를_생성할_수_있다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mIntegrated CMS DataSource Creation\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m integrated_cms 데이터소스 설정");
        String serviceId = "integrated_cms";
        String url = "jdbc:mariadb://db:3306/integrated_cms?useSSL=false&serverTimezone=Asia/Seoul";
        String username = "root";
        String password = "root123!";

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 실제 DB 연결로 데이터소스 생성");
        try {
            DataSource dataSource = dataSourceManager.createServiceDataSource(serviceId, url, username, password);

            // Then
            System.out.println("  \033[2m✨ Verify:\033[0m 데이터소스가 성공적으로 생성됨");
            assertNotNull(dataSource, "데이터소스가 생성되어야 합니다");
            assertTrue(dataSourceManager.hasServiceDataSource(serviceId));
            System.out.println("    \033[32m✓\033[0m \033[90m결과:\033[0m \033[32m데이터소스 생성 성공\033[0m");

        } catch (Exception e) {
            System.out.println(
                    "    \033[31m❌\033[0m \033[90m에러:\033[0m \033[31m" + e.getClass().getSimpleName() + "\033[0m");
            System.out.println("    \033[90m메시지:\033[0m " + e.getMessage());
            fail("데이터소스 생성 중 예외 발생: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("존재하지 않는 서비스 요청 시 integrated_cms로 폴백된다")
    void 존재하지_않는_서비스_요청_시_integrated_cms로_폴백된다() {
        System.out.println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mFallback to Integrated CMS\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m integrated_cms 데이터소스 먼저 생성");
        try {
            dataSourceManager.createServiceDataSource("integrated_cms",
                    "jdbc:mariadb://db:3306/integrated_cms?useSSL=false&serverTimezone=Asia/Seoul",
                    "root", "root123!");

            // When
            System.out.println("  \033[2m⚡ Action:\033[0m 존재하지 않는 서비스 요청");
            DataSource dataSource = dataSourceManager.getServiceDataSource("non_existent_service");

            // Then
            System.out.println("  \033[2m✨ Verify:\033[0m integrated_cms 데이터소스 반환");
            assertNotNull(dataSource, "폴백 데이터소스가 반환되어야 합니다");
            DataSource integratedCmsDataSource = dataSourceManager.getServiceDataSource("integrated_cms");
            assertSame(integratedCmsDataSource, dataSource, "같은 integrated_cms 데이터소스여야 합니다");
            System.out.println(
                    "    \033[32m✓\033[0m \033[90m결과:\033[0m \033[35mintegrated_cms\033[0m \033[90m폴백 성공\033[0m");

        } catch (Exception e) {
            System.out.println(
                    "    \033[31m❌\033[0m \033[90m에러:\033[0m \033[31m" + e.getClass().getSimpleName() + "\033[0m");
            fail("폴백 테스트 중 예외 발생: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("중복된 서비스 ID로 데이터소스 생성 시 기존 것을 반환한다")
    void 중복된_서비스_ID로_데이터소스_생성_시_기존_것을_반환한다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mDuplicate Service ID Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 첫 번째 데이터소스 생성");
        String serviceId = "duplicate_test";
        try {
            DataSource firstDataSource = dataSourceManager.createServiceDataSource(serviceId,
                    "jdbc:mariadb://db:3306/test1?useSSL=false&serverTimezone=Asia/Seoul",
                    "root", "root123!");

            // When
            System.out.println("  \033[2m⚡ Action:\033[0m 같은 ID로 두 번째 데이터소스 생성 시도");
            DataSource secondDataSource = dataSourceManager.createServiceDataSource(serviceId,
                    "jdbc:mariadb://db:3306/test2?useSSL=false&serverTimezone=Asia/Seoul",
                    "root", "root123!");

            // Then
            System.out.println("  \033[2m✨ Verify:\033[0m 첫 번째 데이터소스와 동일한 인스턴스 반환");
            assertSame(firstDataSource, secondDataSource, "기존 데이터소스를 반환해야 합니다");
            assertEquals(1, dataSourceManager.getAllServiceDataSources().entrySet().stream()
                    .mapToInt(entry -> entry.getKey().equals(serviceId) ? 1 : 0).sum(),
                    "서비스 ID별로 하나의 데이터소스만 존재해야 합니다");
            System.out.println("    \033[32m✓\033[0m \033[90m결과:\033[0m \033[32m기존 데이터소스 재사용\033[0m");

        } catch (Exception e) {
            System.out.println(
                    "    \033[31m❌\033[0m \033[90m에러:\033[0m \033[31m" + e.getClass().getSimpleName() + "\033[0m");
            fail("중복 ID 테스트 중 예외 발생: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("integrated_cms 데이터소스는 제거할 수 없다")
    void integrated_cms_데이터소스는_제거할_수_없다() {
        System.out.println("\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mIntegrated CMS Protection\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m integrated_cms 데이터소스 생성");
        try {
            dataSourceManager.createServiceDataSource("integrated_cms",
                    "jdbc:mariadb://db:3306/integrated_cms?useSSL=false&serverTimezone=Asia/Seoul",
                    "root", "root123!");

            // When
            System.out.println("  \033[2m⚡ Action:\033[0m integrated_cms 데이터소스 제거 시도");
            dataSourceManager.removeServiceDataSource("integrated_cms");

            // Then
            System.out.println("  \033[2m✨ Verify:\033[0m integrated_cms가 여전히 존재");
            assertTrue(dataSourceManager.hasServiceDataSource("integrated_cms"),
                    "integrated_cms는 제거되지 않아야 합니다");
            assertNotNull(dataSourceManager.getServiceDataSource("integrated_cms"));
            System.out.println(
                    "    \033[32m✓\033[0m \033[90m결과:\033[0m \033[35mintegrated_cms\033[0m \033[90m보호됨\033[0m");

        } catch (Exception e) {
            System.out.println(
                    "    \033[31m❌\033[0m \033[90m에러:\033[0m \033[31m" + e.getClass().getSimpleName() + "\033[0m");
            fail("보호 테스트 중 예외 발생: " + e.getMessage());
        }
    }
}