package api.v2.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * ServiceContextHolder 단위 테스트
 * 
 * TDD 커서룰에 따라 작성된 테스트:
 * - ThreadLocal 격리 테스트
 * - 동시성 안전성 테스트
 * - 컨텍스트 생명주기 테스트
 * - null/empty 값 처리 테스트
 * - isIntegratedCmsContext() 검증
 */
@ExtendWith(MockitoExtension.class)
class ServiceContextHolderTest {

    @AfterEach
    void tearDown() {
        // 각 테스트 후 컨텍스트 정리 (메모리 누수 방지)
        ServiceContextHolder.clear();
    }

    @Test
    @DisplayName("서비스 ID가 올바르게 설정되고 조회될 수 있다")
    void 서비스_ID가_올바르게_설정되고_조회될_수_있다() {
        // Given (준비)
        String expectedServiceId = "douzone";

        // When (실행)
        ServiceContextHolder.setCurrentServiceId(expectedServiceId);
        String actualServiceId = ServiceContextHolder.getCurrentServiceId();

        // Then (검증)
        assertThat(actualServiceId).isEqualTo(expectedServiceId);
    }

    @Test
    @DisplayName("서비스 컨텍스트가 정리되면 null이 반환된다")
    void 서비스_컨텍스트가_정리되면_null이_반환된다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("test");

        // When
        ServiceContextHolder.clear();
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("null 서비스 ID를 설정하면 컨텍스트가 정리된다")
    void null_서비스_ID를_설정하면_컨텍스트가_정리된다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("initial");

        // When
        ServiceContextHolder.setCurrentServiceId(null);
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("빈 문자열 서비스 ID를 설정하면 컨텍스트가 정리된다")
    void 빈_문자열_서비스_ID를_설정하면_컨텍스트가_정리된다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("initial");

        // When
        ServiceContextHolder.setCurrentServiceId("");
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("공백만 있는 서비스 ID를 설정하면 컨텍스트가 정리된다")
    void 공백만_있는_서비스_ID를_설정하면_컨텍스트가_정리된다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("initial");

        // When
        ServiceContextHolder.setCurrentServiceId("   ");
        String result = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("서비스 ID 앞뒤 공백이 제거되어 설정된다")
    void 서비스_ID_앞뒤_공백이_제거되어_설정된다() {
        // Given
        String serviceIdWithSpaces = "  douzone  ";
        String expectedServiceId = "douzone";

        // When
        ServiceContextHolder.setCurrentServiceId(serviceIdWithSpaces);
        String actualServiceId = ServiceContextHolder.getCurrentServiceId();

        // Then
        assertThat(actualServiceId).isEqualTo(expectedServiceId);
    }

    @Test
    @DisplayName("integrated_cms 컨텍스트를 올바르게 식별한다")
    void integrated_cms_컨텍스트를_올바르게_식별한다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("integrated_cms");

        // When
        boolean isIntegratedCms = ServiceContextHolder.isIntegratedCmsContext();

        // Then
        assertThat(isIntegratedCms).isTrue();
    }

    @Test
    @DisplayName("integrated_cms가 아닌 컨텍스트를 올바르게 식별한다")
    void integrated_cms가_아닌_컨텍스트를_올바르게_식별한다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("douzone");

        // When
        boolean isIntegratedCms = ServiceContextHolder.isIntegratedCmsContext();

        // Then
        assertThat(isIntegratedCms).isFalse();
    }

    @Test
    @DisplayName("컨텍스트가 설정되지 않은 경우 integrated_cms가 아닌 것으로 식별한다")
    void 컨텍스트가_설정되지_않은_경우_integrated_cms가_아닌_것으로_식별한다() {
        // Given (컨텍스트가 설정되지 않은 상태)
        ServiceContextHolder.clear();

        // When
        boolean isIntegratedCms = ServiceContextHolder.isIntegratedCmsContext();

        // Then
        assertThat(isIntegratedCms).isFalse();
    }

    @Test
    @DisplayName("서로 다른 스레드는 독립적인 서비스 컨텍스트를 가진다")
    void 서로_다른_스레드는_독립적인_서비스_컨텍스트를_가진다() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // When & Then
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            ServiceContextHolder.setCurrentServiceId("douzone");
            try {
                Thread.sleep(100); // 다른 스레드 작업 시뮬레이션
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ServiceContextHolder.getCurrentServiceId();
        }, executor);

        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            ServiceContextHolder.setCurrentServiceId("service1");
            try {
                Thread.sleep(100); // 다른 스레드 작업 시뮬레이션
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ServiceContextHolder.getCurrentServiceId();
        }, executor);

        String result1 = future1.get(5, TimeUnit.SECONDS);
        String result2 = future2.get(5, TimeUnit.SECONDS);

        assertThat(result1).isEqualTo("douzone");
        assertThat(result2).isEqualTo("service1");

        executor.shutdown();
    }

    @Test
    @DisplayName("동시에 여러 스레드에서 접근해도 안전하게 동작한다")
    void 동시에_여러_스레드에서_접근해도_안전하게_동작한다() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(10);
        int numberOfThreads = 100;

        // When
        CompletableFuture<Void>[] futures = new CompletableFuture[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                String serviceId = "service_" + threadIndex;
                ServiceContextHolder.setCurrentServiceId(serviceId);

                // 컨텍스트가 올바르게 설정되었는지 확인
                String retrievedServiceId = ServiceContextHolder.getCurrentServiceId();
                assertThat(retrievedServiceId).isEqualTo(serviceId);

                ServiceContextHolder.clear();
                assertThat(ServiceContextHolder.getCurrentServiceId()).isNull();
            }, executor);
        }

        // Then
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    @DisplayName("서비스 컨텍스트가 설정되지 않은 상태에서 검증하면 예외가 발생한다")
    void 서비스_컨텍스트가_설정되지_않은_상태에서_검증하면_예외가_발생한다() {
        // Given
        ServiceContextHolder.clear();

        // When & Then
        assertThatThrownBy(() -> ServiceContextHolder.validateServiceContext())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("서비스 컨텍스트가 설정되지 않았습니다.");
    }

    @Test
    @DisplayName("서비스 컨텍스트가 설정된 상태에서 검증하면 예외가 발생하지 않는다")
    void 서비스_컨텍스트가_설정된_상태에서_검증하면_예외가_발생하지_않는다() {
        // Given
        ServiceContextHolder.setCurrentServiceId("douzone");

        // When & Then
        assertThatCode(() -> ServiceContextHolder.validateServiceContext())
                .doesNotThrowAnyException();
    }
}
