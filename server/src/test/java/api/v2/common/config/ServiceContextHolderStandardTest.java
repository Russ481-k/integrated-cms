package api.v2.common.config;

import testutils.base.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServiceContextHolder 단위 테스트 (표준 컨벤션)
 * 
 * ThreadLocal 기반 서비스 컨텍스트 관리 기능을 테스트합니다.
 * TDD 커서룰에 따른 표준 테스트 컨벤션을 적용했습니다.
 */
class ServiceContextHolderStandardTest extends BaseUnitTest {

    @Test
    @DisplayName("서비스 컨텍스트를 설정하면 동일한 쓰레드에서 조회할 수 있다")
    void 서비스_컨텍스트를_설정하면_동일한_쓰레드에서_조회할_수_있다() {
        printTestHeader("ServiceContext Basic Operations");

        // Given
        printGiven("서비스 ID 'douzone' 준비");
        String serviceId = "douzone";

        // When
        printWhen("서비스 컨텍스트 설정 및 조회");
        ServiceContextHolder.setCurrentServiceId(serviceId);
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        printThen("설정한 서비스 ID가 정확히 조회되는지 검증");
        assertEquals(serviceId, result);
        printSuccess("서비스 컨텍스트 설정/조회 성공: " + result);
    }

    @Test
    @DisplayName("컨텍스트를 정리하면 null이 반환된다")
    void 컨텍스트를_정리하면_null이_반환된다() {
        printTestHeader("ServiceContext Clear Operations");

        // Given
        printGiven("서비스 컨텍스트가 설정된 상태");
        ServiceContextHolder.setCurrentServiceId("douzone");
        assertNotNull(ServiceContextHolder.getCurrentServiceId());

        // When
        printWhen("컨텍스트 정리");
        ServiceContextHolder.clear();
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        printThen("null이 반환되는지 검증");
        assertNull(result);
        printSuccess("컨텍스트 정리 성공: null 반환됨");
    }

    @Test
    @DisplayName("integrated_cms 컨텍스트 판별이 정확하다")
    void integrated_cms_컨텍스트_판별이_정확하다() {
        printTestHeader("Integrated CMS Context Detection");

        // Given & When & Then
        printGiven("integrated_cms 서비스 설정");
        ServiceContextHolder.setCurrentServiceId("integrated_cms");
        assertTrue(ServiceContextHolder.isIntegratedCmsContext());
        printSuccess("integrated_cms 컨텍스트 정확히 인식됨");

        printGiven("일반 서비스 설정");
        ServiceContextHolder.setCurrentServiceId("douzone");
        assertFalse(ServiceContextHolder.isIntegratedCmsContext());
        printSuccess("일반 서비스는 integrated_cms가 아님을 정확히 판별");

        printGiven("컨텍스트가 없는 상태");
        ServiceContextHolder.clear();
        assertFalse(ServiceContextHolder.isIntegratedCmsContext());
        printSuccess("컨텍스트 없음 상태도 정확히 판별");
    }

    @Test
    @DisplayName("멀티쓰레드 환경에서 ThreadLocal이 독립적으로 동작한다")
    void 멀티쓰레드_환경에서_ThreadLocal이_독립적으로_동작한다() throws Exception {
        printTestHeader("Multi-thread ThreadLocal Independence");

        // Given
        printGiven("3개의 쓰레드로 동시성 테스트 환경 구성");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            // When
            printWhen("각 쓰레드에서 서로 다른 서비스 ID 설정");
            CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
                ServiceContextHolder.setCurrentServiceId("douzone");
                return ServiceContextHolder.getCurrentServiceId();
            }, executor);

            CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
                ServiceContextHolder.setCurrentServiceId("service1");
                return ServiceContextHolder.getCurrentServiceId();
            }, executor);

            CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
                ServiceContextHolder.setCurrentServiceId("integrated_cms");
                return ServiceContextHolder.getCurrentServiceId();
            }, executor);

            // 모든 쓰레드 완료 대기
            String result1 = future1.get();
            String result2 = future2.get();
            String result3 = future3.get();

            // Then
            printThen("각 쓰레드의 결과가 독립적인지 검증");
            assertEquals("douzone", result1);
            assertEquals("service1", result2);
            assertEquals("integrated_cms", result3);
            printSuccess("ThreadLocal 독립성 확인: " + result1 + ", " + result2 + ", " + result3);

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("null과 빈 문자열 처리가 올바르다")
    void null과_빈_문자열_처리가_올바르다() {
        printTestHeader("Null and Empty String Handling");

        // Given & When & Then
        printGiven("null 값 설정");
        ServiceContextHolder.setCurrentServiceId(null);
        assertNull(ServiceContextHolder.getCurrentServiceId());
        assertFalse(ServiceContextHolder.isIntegratedCmsContext());
        printSuccess("null 값 처리 정상");

        printGiven("빈 문자열 설정");
        ServiceContextHolder.setCurrentServiceId("");
        String emptyResult = ServiceContextHolder.getCurrentServiceId();
        // 빈 문자열은 null로 변환될 수 있음 (ServiceContextHolder 구현에 따라)
        assertTrue(emptyResult == null || emptyResult.equals(""));
        assertFalse(ServiceContextHolder.isIntegratedCmsContext());
        printSuccess("빈 문자열 처리 정상");

        printGiven("공백 문자열 설정");
        ServiceContextHolder.setCurrentServiceId("   ");
        String whiteSpaceResult = ServiceContextHolder.getCurrentServiceId();
        // 공백 문자열도 null로 변환될 수 있음 (ServiceContextHolder 구현에 따라)
        assertTrue(whiteSpaceResult == null || whiteSpaceResult.equals("   "));
        assertFalse(ServiceContextHolder.isIntegratedCmsContext());
        printSuccess("공백 문자열 처리 정상");
    }

    @Test
    @DisplayName("대소문자가 다른 서비스 ID도 정확히 저장된다")
    void 대소문자가_다른_서비스_ID도_정확히_저장된다() {
        printTestHeader("Case Sensitive Service ID Handling");

        // Given & When & Then
        printGiven("대문자 서비스 ID");
        ServiceContextHolder.setCurrentServiceId("DOUZONE");
        assertEquals("DOUZONE", ServiceContextHolder.getCurrentServiceId());
        printSuccess("대문자 서비스 ID 정확히 저장됨");

        printGiven("소문자 서비스 ID");
        ServiceContextHolder.setCurrentServiceId("douzone");
        assertEquals("douzone", ServiceContextHolder.getCurrentServiceId());
        printSuccess("소문자 서비스 ID 정확히 저장됨");

        printGiven("혼합 케이스 서비스 ID");
        ServiceContextHolder.setCurrentServiceId("DouZone");
        assertEquals("DouZone", ServiceContextHolder.getCurrentServiceId());
        printSuccess("혼합 케이스 서비스 ID 정확히 저장됨");
    }
}
