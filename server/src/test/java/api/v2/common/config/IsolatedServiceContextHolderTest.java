package api.v2.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServiceContextHolder 독립 단위 테스트
 * 
 * TDD 커서룰에 따라 작성된 격리된 테스트:
 * - 외부 의존성 없음
 * - ThreadLocal 격리 테스트
 * - 동시성 안전성 테스트
 * - 컨텍스트 생명주기 테스트
 * - null/empty 값 처리 테스트
 * - isIntegratedCmsContext() 검증
 */
class IsolatedServiceContextHolderTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 컨텍스트 정리 (메모리 누수 방지)
        ServiceContextHolder.clear();
    }

    @Test
    @DisplayName("서비스 컨텍스트를 설정하면 동일한 쓰레드에서 조회할 수 있다")
    void 서비스_컨텍스트를_설정하면_동일한_쓰레드에서_조회할_수_있다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mServiceContext Basic Operations\033[0m");

        // Given
        String expected = "douzone";
        System.out.println("  \033[2m🔍 Setup:\033[0m Expected service ID = \033[36m'" + expected + "'\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m Setting and retrieving service context");
        ServiceContextHolder.setCurrentServiceId(expected);
        String actual = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\033[36m\"" + expected + "\"\033[0m)");
        System.out
                .println("    \033[90m→\033[0m getCurrentServiceId() \033[90m=\033[0m \033[32m'" + actual + "'\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m Values should match");
        assertEquals(expected, actual);
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m" + expected
                + "\033[0m \033[90m==\033[0m \033[32m" + actual + "\033[0m\n");
    }

    @Test
    @DisplayName("서비스 컨텍스트가 설정되지 않으면 null을 반환한다")
    void 서비스_컨텍스트가_설정되지_않으면_null을_반환한다() {
        System.out.println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mInitial State Returns Null\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m ServiceContextHolder in initial state");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m Calling getCurrentServiceId() without setup");
        String result = ServiceContextHolder.getCurrentServiceId();
        System.out.println("    \033[90m→\033[0m getCurrentServiceId() \033[90m=\033[0m \033[33m" + result + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m Should return null");
        assertNull(result);
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mresult == null\033[0m\n");
    }

    @Test
    @DisplayName("clear 메서드로 서비스 컨텍스트를 정리할 수 있다")
    void clear_메서드로_서비스_컨텍스트를_정리할_수_있다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("douzone");

        // When
        ServiceContextHolder.clear();
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("서비스 컨텍스트가 없을 때 검증하면 예외가 발생한다")
    void 서비스_컨텍스트가_없을_때_검증하면_예외가_발생한다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mValidation Exception Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m ServiceContextHolder in initial state");

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m Calling validateServiceContext() without context");
        System.out.println("  \033[2m✨ Verify:\033[0m Should throw IllegalStateException");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                ServiceContextHolder::validateServiceContext);

        System.out.println("    \033[31m⚠️\033[0m Exception thrown: \033[31mIllegalStateException\033[0m");
        System.out.println("    \033[90m📝\033[0m Message: \033[31m'" + exception.getMessage() + "'\033[0m");
        assertEquals("서비스 컨텍스트가 설정되지 않았습니다.", exception.getMessage());
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mExpected exception and message match\033[0m\n");
    }

    @Test
    @DisplayName("서비스 컨텍스트가 있을 때 검증하면 예외가 발생하지 않는다")
    void 서비스_컨텍스트가_있을_때_검증하면_예외가_발생하지_않는다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("douzone");

        // When & Then
        assertDoesNotThrow(ServiceContextHolder::validateServiceContext);
    }

    @Test
    @DisplayName("통합 CMS 컨텍스트인지 정확히 판단할 수 있다")
    void 통합_CMS_컨텍스트인지_정확히_판단할_수_있다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mIntegrated CMS Context Detection\033[0m");

        // Given & When & Then - 통합 CMS인 경우
        System.out.println("  \033[2m🎯 Scenario 1:\033[0m \033[35mintegrated_cms\033[0m service");
        ServiceContextHolder.setCurrentServiceId("integrated_cms");
        boolean isIntegratedCms1 = ServiceContextHolder.isIntegratedCmsContext();
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\033[35m\"integrated_cms\"\033[0m)");
        System.out.println("    \033[90m→\033[0m isIntegratedCmsContext() \033[90m=\033[0m \033[32m" + isIntegratedCms1
                + "\033[0m");
        assertTrue(isIntegratedCms1);
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[35mintegrated_cms\033[0m \033[90mis recognized as integrated CMS\033[0m");

        System.out.println();

        // Given & When & Then - 일반 서비스인 경우
        System.out.println("  \033[2m🎯 Scenario 2:\033[0m \033[36mdouzone\033[0m service");
        ServiceContextHolder.setCurrentServiceId("douzone");
        boolean isIntegratedCms2 = ServiceContextHolder.isIntegratedCmsContext();
        System.out.println("    \033[90m→\033[0m setCurrentServiceId(\033[36m\"douzone\"\033[0m)");
        System.out.println("    \033[90m→\033[0m isIntegratedCmsContext() \033[90m=\033[0m \033[33m" + isIntegratedCms2
                + "\033[0m");
        assertFalse(isIntegratedCms2);
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[36mdouzone\033[0m \033[90mis recognized as regular service\033[0m");

        System.out.println();

        // Given & When & Then - 컨텍스트가 없는 경우
        System.out.println("  \033[2m🎯 Scenario 3:\033[0m No context");
        ServiceContextHolder.clear();
        boolean isIntegratedCms3 = ServiceContextHolder.isIntegratedCmsContext();
        System.out.println("    \033[90m→\033[0m clear() called");
        System.out.println("    \033[90m→\033[0m isIntegratedCmsContext() \033[90m=\033[0m \033[33m" + isIntegratedCms3
                + "\033[0m");
        assertFalse(isIntegratedCms3);
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mno context\033[0m \033[90mmeans not integrated CMS\033[0m\n");
    }

    @Test
    @DisplayName("null 서비스 ID 설정 시 컨텍스트가 정리된다")
    void null_서비스_ID_설정_시_컨텍스트가_정리된다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("douzone");

        // When
        ServiceContextHolder.setCurrentServiceId(null);
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("빈 문자열 서비스 ID 설정 시 컨텍스트가 정리된다")
    void 빈_문자열_서비스_ID_설정_시_컨텍스트가_정리된다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("douzone");

        // When
        ServiceContextHolder.setCurrentServiceId("");
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("공백 문자열 서비스 ID 설정 시 컨텍스트가 정리된다")
    void 공백_문자열_서비스_ID_설정_시_컨텍스트가_정리된다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("douzone");

        // When
        ServiceContextHolder.setCurrentServiceId("   ");
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("여러 쓰레드에서 각각 독립적인 서비스 컨텍스트를 가진다")
    void 여러_쓰레드에서_각각_독립적인_서비스_컨텍스트를_가진다() throws Exception {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("🧪  TEST #10 │ \033[1;36m동시성 안전성 검증 - ThreadLocal 독립성\033[0m");
        System.out.println(repeat("=", 80));

        // Given
        System.out.println("📋 GIVEN    │ ExecutorService(3 threads) 생성");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            // When
            System.out.println("⚡ WHEN     │ 각 쓰레드에서 서로 다른 서비스 ID 설정");
            System.out.println("           │");

            CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
                System.out.println(
                        "           │ \033[34m🧵 Thread-1\033[0m: setCurrentServiceId(\"\033[36mdouzone\033[0m\")");
                ServiceContextHolder.setCurrentServiceId("douzone");
                String result = ServiceContextHolder.getCurrentServiceId();
                System.out.println("           │ \033[34m🧵 Thread-1\033[0m: getCurrentServiceId() → '\033[32m" + result
                        + "\033[0m'");
                return result;
            }, executor);

            CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
                System.out.println(
                        "           │ \033[35m🧵 Thread-2\033[0m: setCurrentServiceId(\"\033[33mservice1\033[0m\")");
                ServiceContextHolder.setCurrentServiceId("service1");
                String result = ServiceContextHolder.getCurrentServiceId();
                System.out.println("           │ \033[35m🧵 Thread-2\033[0m: getCurrentServiceId() → '\033[32m" + result
                        + "\033[0m'");
                return result;
            }, executor);

            CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
                System.out.println(
                        "           │ \033[31m🧵 Thread-3\033[0m: setCurrentServiceId(\"\033[95mintegrated_cms\033[0m\")");
                ServiceContextHolder.setCurrentServiceId("integrated_cms");
                String result = ServiceContextHolder.getCurrentServiceId();
                System.out.println("           │ \033[31m🧵 Thread-3\033[0m: getCurrentServiceId() → '\033[32m" + result
                        + "\033[0m'");
                return result;
            }, executor);

            // Then
            System.out.println("           │");
            System.out.println("✅ THEN     │ 각 쓰레드의 결과가 독립적으로 유지되는지 검증");
            String result1 = future1.get(1, TimeUnit.SECONDS);
            String result2 = future2.get(1, TimeUnit.SECONDS);
            String result3 = future3.get(1, TimeUnit.SECONDS);

            assertEquals("douzone", result1);
            assertEquals("service1", result2);
            assertEquals("integrated_cms", result3);

            System.out.println(
                    "           │ ✓ \033[34mThread-1\033[0m: \033[36mdouzone\033[0m == \033[32m" + result1 + "\033[0m");
            System.out.println("           │ ✓ \033[35mThread-2\033[0m: \033[33mservice1\033[0m == \033[32m" + result2
                    + "\033[0m");
            System.out.println("           │ ✓ \033[31mThread-3\033[0m: \033[95mintegrated_cms\033[0m == \033[32m"
                    + result3 + "\033[0m");
            System.out.println("           │");
            System.out.println("🎯 RESULT   │ \033[1;32m동시성 테스트 성공! ThreadLocal이 쓰레드별로 완전히 독립적으로 동작\033[0m");
            System.out.println(repeat("=", 80) + "\n");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("동시에 같은 쓰레드에서 서비스 컨텍스트 변경이 안전하다")
    void 동시에_같은_쓰레드에서_서비스_컨텍스트_변경이_안전하다() {
        // Given
        String[] services = { "douzone", "service1", "service2", "integrated_cms" };

        // When
        for (String service : services) {
            ServiceContextHolder.setCurrentServiceId(service);
            String result = ServiceContextHolder.getCurrentServiceId();

            // Then
            assertEquals(service, result);
        }
    }

    @Test
    @DisplayName("대소문자가 다른 서비스 ID도 정확히 저장된다")
    void 대소문자가_다른_서비스_ID도_정확히_저장된다() {
        // Given & When & Then
        ServiceContextHolder.setCurrentServiceId("DOUZONE");
        assertEquals("DOUZONE", ServiceContextHolder.getCurrentServiceId());

        ServiceContextHolder.setCurrentServiceId("douzone");
        assertEquals("douzone", ServiceContextHolder.getCurrentServiceId());

        ServiceContextHolder.setCurrentServiceId("DouZone");
        assertEquals("DouZone", ServiceContextHolder.getCurrentServiceId());
    }

    @Test
    @DisplayName("특수문자가 포함된 서비스 ID도 처리할 수 있다")
    void 특수문자가_포함된_서비스_ID도_처리할_수_있다() {
        // Given
        String serviceWithSpecialChars = "service-1_test.example";

        // When
        ServiceContextHolder.setCurrentServiceId(serviceWithSpecialChars);
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertEquals(serviceWithSpecialChars, result);
    }
}
