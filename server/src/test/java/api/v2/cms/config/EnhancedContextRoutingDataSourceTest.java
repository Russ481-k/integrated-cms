package api.v2.cms.config;

import api.v2.common.config.ServiceContextHolder;
import api.v2.common.config.DynamicServiceDataSourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * EnhancedContextRoutingDataSource 통합 테스트
 * 
 * ServiceContextHolder + DynamicServiceDataSourceManager와의 통합 동작을 검증:
 * - 서비스별 DB 라우팅 키 결정 로직
 * - 동적 데이터소스 관리자와의 연동
 * - 기본 폴백 처리 및 예외 상황 대응
 * - 전체 라우팅 플로우 통합 테스트
 */
@ExtendWith(MockitoExtension.class)
class EnhancedContextRoutingDataSourceTest {

    // Java 8 호환 문자열 반복 유틸리티 (필수)
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private EnhancedContextRoutingDataSource routingDataSource;

    @Mock
    private DynamicServiceDataSourceManager dataSourceManager;

    @Mock
    private DataSource integratedCmsDataSource;

    @Mock
    private DataSource douzoneDataSource;

    @Mock
    private DataSource dynamicDataSource;

    @BeforeEach
    void setUp() {
        // 각 테스트 전 초기화
        routingDataSource = new EnhancedContextRoutingDataSource();
        routingDataSource.setDataSourceManager(dataSourceManager);

        // 기본 정적 데이터소스 설정 (실제 구성과 동일)
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("integrated_cms", integratedCmsDataSource);
        targetDataSources.put("douzone", douzoneDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(integratedCmsDataSource);
        routingDataSource.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 컨텍스트 정리
        ServiceContextHolder.clear();
    }

    @Test
    @DisplayName("서비스 컨텍스트가 없으면 integrated_cms로 라우팅된다")
    void 서비스_컨텍스트가_없으면_integrated_cms로_라우팅된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mDefault Routing to Integrated CMS\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 서비스 컨텍스트 없음 (null 상태)");
        // ServiceContextHolder가 초기 상태 (null)

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 라우팅 키 결정 로직 실행");
        Object lookupKey = routingDataSource.determineCurrentLookupKey();
        System.out.println("    \033[90m→\033[0m determineCurrentLookupKey()");
        System.out.println("    \033[90m→\033[0m lookup key = \033[35m'" + lookupKey + "'\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m integrated_cms로 기본 라우팅되어야 함");
        assertEquals("integrated_cms", lookupKey);
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[35mintegrated_cms\033[0m \033[90mdefault routing\033[0m\n");
    }

    @Test
    @DisplayName("douzone 서비스 컨텍스트로 올바르게 라우팅된다")
    void douzone_서비스_컨텍스트로_올바르게_라우팅된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mDouzone Service Context Routing\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m douzone 서비스 컨텍스트 설정");
        ServiceContextHolder.setCurrentServiceId("douzone");
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\033[36m\"douzone\"\033[0m)");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 라우팅 키 결정 로직 실행");
        Object lookupKey = routingDataSource.determineCurrentLookupKey();
        System.out.println("    \033[90m→\033[0m determineCurrentLookupKey()");
        System.out.println("    \033[90m→\033[0m lookup key = \033[36m'" + lookupKey + "'\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m douzone으로 라우팅되어야 함");
        assertEquals("douzone", lookupKey);
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[36mdouzone\033[0m \033[90mservice routing\033[0m\n");
    }

    @Test
    @DisplayName("동적 데이터소스가 있으면 우선 사용된다")
    void 동적_데이터소스가_있으면_우선_사용된다() {
        System.out.println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mDynamic DataSource Priority\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m service1 컨텍스트와 동적 데이터소스 모킹");
        ServiceContextHolder.setCurrentServiceId("service1");
        when(dataSourceManager.getServiceDataSource("service1")).thenReturn(dynamicDataSource);
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\033[36m\"service1\"\033[0m)");
        System.out.println(
                "    \033[90m→\033[0m mock: dataSourceManager.getServiceDataSource(\033[36m\"service1\"\033[0m) = dynamicDataSource");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 타겟 데이터소스 결정 로직 실행");
        DataSource targetDataSource = routingDataSource.determineTargetDataSource();
        System.out.println("    \033[90m→\033[0m determineTargetDataSource()");
        System.out.println("    \033[90m→\033[0m target = \033[32mdynamicDataSource\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 동적 데이터소스가 반환되어야 함");
        assertSame(dynamicDataSource, targetDataSource);
        verify(dataSourceManager).getServiceDataSource("service1");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mdynamic data source used\033[0m");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mVerification passed:\033[0m \033[32mdataSourceManager called\033[0m\n");
    }

    @Test
    @DisplayName("동적 데이터소스가 없으면 정적 데이터소스로 폴백된다")
    void 동적_데이터소스가_없으면_정적_데이터소스로_폴백된다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mFallback to Static DataSource\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m douzone 컨텍스트, 동적 데이터소스 없음 (null)");
        ServiceContextHolder.setCurrentServiceId("douzone");
        when(dataSourceManager.getServiceDataSource("douzone")).thenReturn(null);
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\033[36m\"douzone\"\033[0m)");
        System.out.println(
                "    \033[90m→\033[0m mock: dataSourceManager.getServiceDataSource(\033[36m\"douzone\"\033[0m) = \033[33mnull\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 타겟 데이터소스 결정 로직 실행");
        DataSource targetDataSource = routingDataSource.determineTargetDataSource();
        System.out.println("    \033[90m→\033[0m determineTargetDataSource()");
        System.out.println("    \033[90m→\033[0m fallback to static routing");
        System.out.println("    \033[90m→\033[0m target = \033[32mdouzoneDataSource\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 정적 douzone 데이터소스로 폴백되어야 함");
        assertSame(douzoneDataSource, targetDataSource);
        verify(dataSourceManager).getServiceDataSource("douzone");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mstatic data source fallback\033[0m");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mVerification passed:\033[0m \033[32mdynamic lookup attempted first\033[0m\n");
    }

    @Test
    @DisplayName("존재하지 않는 서비스는 기본 데이터소스로 처리된다")
    void 존재하지_않는_서비스는_기본_데이터소스로_처리된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mUnknown Service Default Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 존재하지 않는 서비스 ID");
        ServiceContextHolder.setCurrentServiceId("unknown_service");
        when(dataSourceManager.getServiceDataSource("unknown_service")).thenReturn(null);
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\033[36m\"unknown_service\"\033[0m)");
        System.out.println(
                "    \033[90m→\033[0m mock: dataSourceManager.getServiceDataSource(\033[36m\"unknown_service\"\033[0m) = \033[33mnull\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 타겟 데이터소스 결정 로직 실행");
        DataSource targetDataSource = routingDataSource.determineTargetDataSource();
        System.out.println("    \033[90m→\033[0m determineTargetDataSource()");
        System.out.println("    \033[90m→\033[0m fallback to default target");
        System.out.println("    \033[90m→\033[0m target = \033[35mintegratedCmsDataSource\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 기본 데이터소스(integrated_cms)로 처리되어야 함");
        assertSame(integratedCmsDataSource, targetDataSource);
        verify(dataSourceManager).getServiceDataSource("unknown_service");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[35mdefault data source used\033[0m");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mVerification passed:\033[0m \033[32mdynamic lookup attempted\033[0m\n");
    }

    @Test
    @DisplayName("빈 문자열 서비스 ID는 integrated_cms로 처리된다")
    void 빈_문자열_서비스_ID는_integrated_cms로_처리된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mEmpty String Service ID Handling\033[0m");

        // When & Then
        System.out.println("  \033[2m🎯 Scenario 1:\033[0m 빈 문자열 (\"\")");
        ServiceContextHolder.setCurrentServiceId("");
        Object lookupKey1 = routingDataSource.determineCurrentLookupKey();
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\"\")");
        System.out.println("    \033[90m→\033[0m lookup key = \033[35m'" + lookupKey1 + "'\033[0m");
        assertEquals("integrated_cms", lookupKey1);

        System.out.println();

        System.out.println("  \033[2m🎯 Scenario 2:\033[0m 공백 문자열 (\"   \")");
        ServiceContextHolder.setCurrentServiceId("   ");
        Object lookupKey2 = routingDataSource.determineCurrentLookupKey();
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\"   \")");
        System.out.println("    \033[90m→\033[0m lookup key = \033[35m'" + lookupKey2 + "'\033[0m");
        assertEquals("integrated_cms", lookupKey2);

        System.out.println(
                "    \033[32m✓\033[0m \033[90mAll scenarios passed:\033[0m \033[35mintegrated_cms\033[0m \033[90mfor empty/blank strings\033[0m\n");
    }

    @Test
    @DisplayName("DynamicServiceDataSourceManager가 없어도 정상 동작한다")
    void DynamicServiceDataSourceManager가_없어도_정상_동작한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #7\033[0m \033[90m│\033[0m \033[1mGraceful Handling Without Dynamic Manager\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m DynamicServiceDataSourceManager 없음 (null)");
        routingDataSource.setDataSourceManager(null);
        ServiceContextHolder.setCurrentServiceId("douzone");
        System.out.println("    \033[90m→\033[0m setDataSourceManager(\033[33mnull\033[0m)");
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\033[36m\"douzone\"\033[0m)");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 타겟 데이터소스 결정 로직 실행");
        DataSource targetDataSource = routingDataSource.determineTargetDataSource();
        System.out.println("    \033[90m→\033[0m determineTargetDataSource()");
        System.out.println("    \033[90m→\033[0m skip dynamic lookup (manager is null)");
        System.out.println("    \033[90m→\033[0m target = \033[32mdouzoneDataSource\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 정적 라우팅만으로도 정상 동작해야 함");
        assertSame(douzoneDataSource, targetDataSource);
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mstatic routing works without dynamic manager\033[0m\n");
    }

    @Test
    @DisplayName("멀티 스레드 환경에서 서비스별 라우팅이 독립적으로 동작한다")
    void 멀티_스레드_환경에서_서비스별_라우팅이_독립적으로_동작한다() throws InterruptedException {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("🧪  TEST #8 │ \033[1;36m멀티 스레드 라우팅 독립성 검증\033[0m");
        System.out.println(repeat("=", 80));

        // Given
        System.out.println("📋 GIVEN    │ 각 스레드별로 다른 서비스 ID 설정 (라우팅 키 결정만 테스트)");

        // When
        System.out.println("⚡ WHEN     │ 3개 스레드에서 동시에 라우팅 키 조회");
        System.out.println("           │");

        Thread[] threads = new Thread[3];
        String[] results = new String[3];
        String[] serviceIds = { "douzone", "service1", "integrated_cms" };

        for (int i = 0; i < 3; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                ServiceContextHolder.setCurrentServiceId(serviceIds[index]);
                results[index] = (String) routingDataSource.determineCurrentLookupKey();
                System.out.println("           │ \033[34m🧵 Thread-" + (index + 1) + "\033[0m: serviceId=\033[36m"
                        + serviceIds[index] + "\033[0m → lookupKey=\033[32m" + results[index] + "\033[0m");
            });
        }

        // 스레드 시작 및 대기
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        System.out.println("           │");
        System.out.println("✅ THEN     │ 각 스레드의 라우팅 키가 독립적으로 유지되는지 검증");

        assertEquals("douzone", results[0], "Thread-1 should route to douzone");
        assertEquals("service1", results[1], "Thread-2 should route to service1");
        assertEquals("integrated_cms", results[2], "Thread-3 should route to integrated_cms");

        System.out.println("🎯 RESULT   │ \033[1;32m멀티 스레드 라우팅 테스트 성공! 각 스레드가 독립적으로 올바른 라우팅 키 사용\033[0m");
        System.out.println(repeat("=", 80) + "\n");
    }
}
