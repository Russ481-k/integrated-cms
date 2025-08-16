package api.v2.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * ServiceContextInterceptor 단위 테스트
 * 
 * TDD 커서룰에 따라 작성된 URL 패턴 매칭 및 서비스 컨텍스트 설정 테스트:
 * - 통합 CMS 패턴 매칭 테스트
 * - 동적 서비스 ID 추출 테스트
 * - 컨텍스트 정리 동작 테스트
 * - 에러 처리 시나리오 테스트
 */
@ExtendWith(MockitoExtension.class)
class ServiceContextInterceptorTest {

    private ServiceContextInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @BeforeEach
    void setUp() {
        // 각 테스트 전 초기화
        interceptor = new ServiceContextInterceptor();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 컨텍스트 정리
        ServiceContextHolder.clear();
    }

    @Test
    @DisplayName("통합 CMS API 패턴 매칭 시 integrated_cms 컨텍스트가 설정된다")
    void 통합_CMS_API_패턴_매칭_시_integrated_cms_컨텍스트가_설정된다() throws Exception {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mIntegrated CMS Pattern Matching\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 통합 CMS API 경로 요청");
        String integratedCmsPath = "/api/v2/integrated-cms/services";
        when(request.getRequestURI()).thenReturn(integratedCmsPath);
        when(request.getMethod()).thenReturn("GET");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m preHandle 호출");
        boolean result = interceptor.preHandle(request, response, handler);
        System.out.println("    \033[90m→\033[0m preHandle(\033[36m\"" + integratedCmsPath + "\"\033[0m)");
        System.out.println("    \033[90m→\033[0m result = \033[32m" + result + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m integrated_cms 컨텍스트 설정 확인");
        assertTrue(result, "preHandle should return true");
        String currentServiceId = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m getCurrentServiceId() = \033[35m'" + currentServiceId + "'\033[0m");

        assertEquals("integrated_cms", currentServiceId);
        assertTrue(ServiceContextHolder.isIntegratedCmsContext());
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[35mintegrated_cms\033[0m \033[90mcontext set correctly\033[0m\n");
    }

    @Test
    @DisplayName("서비스별 CMS API 패턴에서 동적 서비스 ID를 추출한다")
    void 서비스별_CMS_API_패턴에서_동적_서비스_ID를_추출한다() throws Exception {
        System.out
                .println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mDynamic Service ID Extraction\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 서비스별 CMS API 경로들");

        // When & Then - 다중 시나리오 테스트
        System.out.println("  \033[2m🎯 Scenario 1:\033[0m \033[36mdouzone\033[0m service");
        testServiceIdExtraction("/api/v2/cms/douzone/content", "douzone");

        System.out.println();

        System.out.println("  \033[2m🎯 Scenario 2:\033[0m \033[36mservice1\033[0m service");
        testServiceIdExtraction("/api/v2/cms/service1/users", "service1");

        System.out.println();

        System.out.println("  \033[2m🎯 Scenario 3:\033[0m \033[36mtest_service\033[0m service");
        testServiceIdExtraction("/api/v2/cms/test_service/files/upload", "test_service");

        System.out.println(
                "    \033[32m✓\033[0m \033[90mAll scenarios passed:\033[0m \033[32mdynamic service ID extraction works correctly\033[0m\n");
    }

    private void testServiceIdExtraction(String path, String expectedServiceId) throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");

        // When
        boolean result = interceptor.preHandle(request, response, handler);
        System.out.println("    \033[90m→\033[0m preHandle(\033[36m\"" + path + "\"\033[0m)");

        // Then
        assertTrue(result);
        String actualServiceId = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m extracted serviceId = \033[36m'" + actualServiceId + "'\033[0m");
        assertEquals(expectedServiceId, actualServiceId);
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[36m" + expectedServiceId
                + "\033[0m \033[90m==\033[0m \033[36m" + actualServiceId + "\033[0m");

        // 다음 시나리오를 위해 컨텍스트 초기화
        ServiceContextHolder.clear();
    }

    @Test
    @DisplayName("v2 API 패턴이 맞지 않는 경우 컨텍스트를 설정하지 않는다")
    void v2_API_패턴이_맞지_않는_경우_컨텍스트를_설정하지_않는다() throws Exception {
        System.out.println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mUnmatched Pattern Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 패턴이 맞지 않는 v2 API 경로");
        String unmatchedPath = "/api/v2/unknown/endpoint";
        when(request.getRequestURI()).thenReturn(unmatchedPath);
        when(request.getMethod()).thenReturn("GET");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 패턴 매칭되지 않는 경로로 preHandle 호출");
        boolean result = interceptor.preHandle(request, response, handler);
        System.out.println("    \033[90m→\033[0m preHandle(\033[36m\"" + unmatchedPath + "\"\033[0m)");
        System.out.println("    \033[90m→\033[0m result = \033[32m" + result + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 컨텍스트가 설정되지 않아야 함");
        assertTrue(result, "Should continue processing even with unmatched pattern");
        String currentServiceId = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m getCurrentServiceId() = \033[33m" + currentServiceId + "\033[0m");

        assertNull(currentServiceId, "Service context should not be set for unmatched patterns");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mno context set for unmatched pattern\033[0m\n");
    }

    @Test
    @DisplayName("afterCompletion에서 서비스 컨텍스트가 정리된다")
    void afterCompletion에서_서비스_컨텍스트가_정리된다() throws Exception {
        System.out.println(
                "\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mContext Cleanup After Completion\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 서비스 컨텍스트가 설정된 상태");
        ServiceContextHolder.setCurrentServiceId("douzone");
        String beforeCleanup = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m context before cleanup = \033[36m'" + beforeCleanup + "'\033[0m");

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v2/cms/douzone/content");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m afterCompletion 호출");
        interceptor.afterCompletion(request, response, handler, null);
        System.out.println("    \033[90m→\033[0m afterCompletion() called");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 컨텍스트가 정리되어야 함");
        String afterCleanup = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m context after cleanup = \033[33m" + afterCleanup + "\033[0m");

        assertNull(afterCleanup, "Service context should be cleared after completion");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mcontext cleared successfully\033[0m\n");
    }

    @Test
    @DisplayName("비 v2 API 경로는 처리하지 않고 통과시킨다")
    void 비_v2_API_경로는_처리하지_않고_통과시킨다() throws Exception {
        System.out.println("\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mNon-V2 API Path Passthrough\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m v2가 아닌 API 경로들");

        // When & Then - 다중 시나리오 테스트
        System.out.println("  \033[2m🎯 Scenario 1:\033[0m v1 API 경로");
        testNonV2ApiPath("/api/v1/users", "v1 API");

        System.out.println();

        System.out.println("  \033[2m🎯 Scenario 2:\033[0m 일반 경로");
        testNonV2ApiPath("/health", "health check");

        System.out.println();

        System.out.println("  \033[2m🎯 Scenario 3:\033[0m 정적 리소스");
        testNonV2ApiPath("/static/css/main.css", "static resource");

        System.out.println(
                "    \033[32m✓\033[0m \033[90mAll scenarios passed:\033[0m \033[32mnon-v2 API paths handled correctly\033[0m\n");
    }

    private void testNonV2ApiPath(String path, String description) throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("GET");

        // When
        boolean result = interceptor.preHandle(request, response, handler);
        System.out.println("    \033[90m→\033[0m preHandle(\033[36m\"" + path + "\"\033[0m) for " + description);

        // Then
        assertTrue(result, "Should always allow non-v2 API requests to continue");
        String serviceId = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m serviceId = \033[33m" + serviceId + "\033[0m");
        assertNull(serviceId, "Should not set service context for non-v2 API paths");
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m" + description
                + " passed through correctly\033[0m");

        // 다음 시나리오를 위해 컨텍스트 확실히 정리
        ServiceContextHolder.clear();
    }
}
