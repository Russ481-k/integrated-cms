package api.v2.common.config;

import api.v2.common.config.ServiceContextHolder;
import api.v2.common.config.ServiceContextInterceptor;
import api.v2.common.config.DynamicServiceDataSourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 전체 라우팅 플로우 통합 테스트
 * 
 * URL → ServiceContextInterceptor → ServiceContextHolder →
 * EnhancedContextRoutingDataSource
 * 전체 플로우의 통합 동작을 검증:
 * - End-to-End 라우팅 플로우
 * - 여러 서비스 동시 접근
 * - 컨텍스트 생명주기 관리
 * - 메모리 누수 방지 및 정리
 */
@ExtendWith(MockitoExtension.class)
class FullRoutingFlowIntegrationTest {

    // Java 8 호환 문자열 반복 유틸리티 (필수)
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private ServiceContextInterceptor interceptor;
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
        // 전체 플로우 컴포넌트 초기화
        interceptor = new ServiceContextInterceptor();
        routingDataSource = new EnhancedContextRoutingDataSource();
        routingDataSource.setDataSourceManager(dataSourceManager);

        // 기본 정적 데이터소스 설정
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
    @DisplayName("통합 CMS URL 요청 시 전체 플로우가 올바르게 동작한다")
    void 통합_CMS_URL_요청_시_전체_플로우가_올바르게_동작한다() throws Exception {
        System.out.println("\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mIntegrated CMS Full Flow\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 통합 CMS URL 요청 준비");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/v2/integrated-cms/services");
        System.out.println("    \033[90m→\033[0m request URI: \033[35m/api/v2/integrated-cms/services\033[0m");

        // When - 전체 플로우 실행
        System.out.println("  \033[2m⚡ Action:\033[0m 전체 라우팅 플로우 실행");

        // Step 1: Interceptor가 URL에서 서비스 컨텍스트 설정
        boolean interceptorResult = interceptor.preHandle(request, response, null);
        System.out.println(
                "    \033[90m→\033[0m Step 1: interceptor.preHandle() = \033[32m" + interceptorResult + "\033[0m");
        String contextAfterInterceptor = ServiceContextHolder.getCurrentServiceId();
        System.out.println(
                "    \033[90m→\033[0m Step 2: ServiceContext = \033[35m'" + contextAfterInterceptor + "'\033[0m");

        // Step 2: RoutingDataSource가 컨텍스트 기반으로 라우팅 키 결정
        Object lookupKey = routingDataSource.determineCurrentLookupKey();
        System.out.println("    \033[90m→\033[0m Step 3: routing lookup key = \033[35m'" + lookupKey + "'\033[0m");

        // Step 3: 실제 데이터소스 결정
        DataSource targetDataSource = routingDataSource.determineTargetDataSource();
        System.out.println("    \033[90m→\033[0m Step 4: target DataSource = \033[32mintegratedCmsDataSource\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 전체 플로우가 integrated_cms로 라우팅되어야 함");
        assertTrue(interceptorResult);
        assertEquals("integrated_cms", contextAfterInterceptor);
        assertEquals("integrated_cms", lookupKey);
        assertSame(integratedCmsDataSource, targetDataSource);
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAll assertions passed:\033[0m \033[35mintegrated_cms\033[0m \033[90mfull flow success\033[0m\n");
    }

    @Test
    @DisplayName("Douzone 서비스 URL 요청 시 전체 플로우가 올바르게 동작한다")
    void Douzone_서비스_URL_요청_시_전체_플로우가_올바르게_동작한다() throws Exception {
        System.out.println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mDouzone Service Full Flow\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m Douzone 서비스 URL 요청 준비");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/v2/cms/douzone/content/list");
        System.out.println("    \033[90m→\033[0m request URI: \033[36m/api/v2/cms/douzone/content/list\033[0m");

        when(dataSourceManager.getServiceDataSource("douzone")).thenReturn(null); // 정적 라우팅 사용

        // When - 전체 플로우 실행
        System.out.println("  \033[2m⚡ Action:\033[0m 전체 라우팅 플로우 실행");

        // Step 1: Interceptor가 URL에서 douzone 서비스 ID 추출
        boolean interceptorResult = interceptor.preHandle(request, response, null);
        System.out.println(
                "    \033[90m→\033[0m Step 1: interceptor.preHandle() = \033[32m" + interceptorResult + "\033[0m");
        String contextAfterInterceptor = ServiceContextHolder.getCurrentServiceId();
        System.out.println(
                "    \033[90m→\033[0m Step 2: ServiceContext = \033[36m'" + contextAfterInterceptor + "'\033[0m");

        // Step 2: RoutingDataSource가 컨텍스트 기반으로 라우팅
        Object lookupKey = routingDataSource.determineCurrentLookupKey();
        System.out.println("    \033[90m→\033[0m Step 3: routing lookup key = \033[36m'" + lookupKey + "'\033[0m");

        // Step 3: 실제 데이터소스 결정
        DataSource targetDataSource = routingDataSource.determineTargetDataSource();
        System.out.println("    \033[90m→\033[0m Step 4: target DataSource = \033[32mdouzoneDataSource\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 전체 플로우가 douzone으로 라우팅되어야 함");
        assertTrue(interceptorResult);
        assertEquals("douzone", contextAfterInterceptor);
        assertEquals("douzone", lookupKey);
        assertSame(douzoneDataSource, targetDataSource);
        verify(dataSourceManager).getServiceDataSource("douzone");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAll assertions passed:\033[0m \033[36mdouzone\033[0m \033[90mfull flow success\033[0m\n");
    }

    @Test
    @DisplayName("동적 서비스 URL 요청 시 동적 데이터소스가 우선 사용된다")
    void 동적_서비스_URL_요청_시_동적_데이터소스가_우선_사용된다() throws Exception {
        System.out.println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mDynamic Service Full Flow\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 동적 서비스 URL과 동적 데이터소스 준비");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/v2/cms/service1/user/profile");
        when(dataSourceManager.getServiceDataSource("service1")).thenReturn(dynamicDataSource);
        System.out.println("    \033[90m→\033[0m request URI: \033[36m/api/v2/cms/service1/user/profile\033[0m");
        System.out.println("    \033[90m→\033[0m mock: dynamic data source available for service1");

        // When - 전체 플로우 실행
        System.out.println("  \033[2m⚡ Action:\033[0m 전체 라우팅 플로우 실행");

        // Step 1: Interceptor가 URL에서 service1 추출
        boolean interceptorResult = interceptor.preHandle(request, response, null);
        System.out.println(
                "    \033[90m→\033[0m Step 1: interceptor.preHandle() = \033[32m" + interceptorResult + "\033[0m");
        String contextAfterInterceptor = ServiceContextHolder.getCurrentServiceId();
        System.out.println(
                "    \033[90m→\033[0m Step 2: ServiceContext = \033[36m'" + contextAfterInterceptor + "'\033[0m");

        // Step 2: RoutingDataSource가 동적 데이터소스 확인
        DataSource targetDataSource = routingDataSource.determineTargetDataSource();
        System.out.println("    \033[90m→\033[0m Step 3: target DataSource = \033[32mdynamicDataSource\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 동적 데이터소스가 우선 사용되어야 함");
        assertTrue(interceptorResult);
        assertEquals("service1", contextAfterInterceptor);
        assertSame(dynamicDataSource, targetDataSource);
        verify(dataSourceManager).getServiceDataSource("service1");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAll assertions passed:\033[0m \033[32mdynamic data source priority\033[0m\n");
    }

    @Test
    @DisplayName("요청 완료 후 컨텍스트가 올바르게 정리된다")
    void 요청_완료_후_컨텍스트가_올바르게_정리된다() throws Exception {
        System.out
                .println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mContext Cleanup After Request\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 요청 처리 및 완료 시뮬레이션");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/v2/cms/douzone/test");
        System.out.println("    \033[90m→\033[0m request URI: \033[36m/api/v2/cms/douzone/test\033[0m");

        // When - 요청 처리 및 완료 플로우
        System.out.println("  \033[2m⚡ Action:\033[0m 요청 처리 → 완료 → 정리 플로우");

        // Step 1: 요청 시작 (preHandle)
        interceptor.preHandle(request, response, null);
        String contextDuringRequest = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m Step 1: 요청 중 context = \033[36m'" + contextDuringRequest + "'\033[0m");

        // Step 2: 요청 완료 (afterCompletion)
        interceptor.afterCompletion(request, response, null, null);
        String contextAfterCompletion = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m Step 2: 완료 후 context = \033[33m" + contextAfterCompletion + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 컨텍스트가 정리되어야 함");
        assertEquals("douzone", contextDuringRequest);
        assertNull(contextAfterCompletion);
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mcontext cleaned after completion\033[0m\n");
    }

    @Test
    @DisplayName("여러 서비스에 대한 동시 요청이 독립적으로 처리된다")
    void 여러_서비스에_대한_동시_요청이_독립적으로_처리된다() throws Exception {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("🧪  TEST #5 │ \033[1;36m동시 다중 서비스 요청 독립성 검증\033[0m");
        System.out.println(repeat("=", 80));

        // Given
        System.out.println("📋 GIVEN    │ 3개 서비스에 대한 동시 요청 준비");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        String[] serviceIds = { "douzone", "service1", "integrated_cms" };
        String[] requestUris = {
                "/api/v2/cms/douzone/content",
                "/api/v2/cms/service1/user",
                "/api/v2/integrated-cms/services"
        };

        // Note: 이 테스트는 determineCurrentLookupKey()만 사용하므로 mock 설정 불필요

        // When
        System.out.println("⚡ WHEN     │ 각 스레드에서 독립적인 라우팅 플로우 실행");
        System.out.println("           │");

        CompletableFuture<String>[] futures = new CompletableFuture[3];

        for (int i = 0; i < 3; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    MockHttpServletRequest request = new MockHttpServletRequest();
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    request.setRequestURI(requestUris[index]);

                    // 전체 플로우 실행
                    ServiceContextInterceptor threadInterceptor = new ServiceContextInterceptor();
                    threadInterceptor.preHandle(request, response, null);

                    String context = ServiceContextHolder.getCurrentServiceId();
                    Object lookupKey = routingDataSource.determineCurrentLookupKey();

                    // 정리
                    threadInterceptor.afterCompletion(request, response, null, null);

                    System.out.println("           │ \033[34m🧵 Thread-" + (index + 1) + "\033[0m: URI=\033[36m"
                            + requestUris[index] + "\033[0m → context=\033[32m" + context + "\033[0m");

                    return context;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        // 모든 결과 수집
        String[] results = new String[3];
        for (int i = 0; i < 3; i++) {
            results[i] = futures[i].get();
        }

        executor.shutdown();

        // Then
        System.out.println("           │");
        System.out.println("✅ THEN     │ 각 스레드의 컨텍스트가 독립적으로 처리되는지 검증");

        assertEquals("douzone", results[0]);
        assertEquals("service1", results[1]);
        assertEquals("integrated_cms", results[2]);

        System.out.println("🎯 RESULT   │ \033[1;32m동시 다중 서비스 요청 테스트 성공! 각 요청이 독립적으로 올바른 라우팅 수행\033[0m");
        System.out.println(repeat("=", 80) + "\n");
    }

    @Test
    @DisplayName("잘못된 URL 패턴은 기본 라우팅으로 처리된다")
    void 잘못된_URL_패턴은_기본_라우팅으로_처리된다() throws Exception {
        System.out.println(
                "\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mInvalid URL Pattern Default Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 잘못된 URL 패턴 요청");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/v1/legacy/endpoint"); // v2가 아닌 패턴
        System.out
                .println("    \033[90m→\033[0m request URI: \033[33m/api/v1/legacy/endpoint\033[0m (invalid pattern)");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 인터셉터 처리 및 라우팅 확인");
        boolean interceptorResult = interceptor.preHandle(request, response, null);
        String contextAfterInterceptor = ServiceContextHolder.getCurrentServiceId();
        Object lookupKey = routingDataSource.determineCurrentLookupKey();
        DataSource targetDataSource = routingDataSource.determineTargetDataSource();

        System.out.println("    \033[90m→\033[0m interceptor result = \033[32m" + interceptorResult + "\033[0m");
        System.out.println("    \033[90m→\033[0m context = \033[33m" + contextAfterInterceptor + "\033[0m");
        System.out.println("    \033[90m→\033[0m lookup key = \033[35m'" + lookupKey + "'\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 패턴 매칭 실패 시 기본 처리되어야 함");
        assertTrue(interceptorResult); // 인터셉터는 통과하지만 컨텍스트 설정 안함
        assertNull(contextAfterInterceptor); // 컨텍스트 설정되지 않음
        assertEquals("integrated_cms", lookupKey); // 기본값으로 라우팅
        assertSame(integratedCmsDataSource, targetDataSource);
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAll assertions passed:\033[0m \033[35mdefault routing for invalid patterns\033[0m\n");
    }
}
