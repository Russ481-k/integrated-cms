package api.v2.integrated_cms.service.repository;

import api.v2.integrated_cms.service.domain.ServiceEntity;
import api.v2.integrated_cms.service.domain.ServiceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceRepository 데이터 접근 계층 테스트
 * 
 * JPA 쿼리 메서드와 커스텀 쿼리의 동작을 검증합니다.
 */
@SpringBootTest
@Transactional
@Import({ ServiceRepositoryTest.TestConfig.class, testutils.config.TestMailConfiguration.class })
@TestPropertySource(properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"
})
class ServiceRepositoryTest {

        @Autowired
        private ServiceRepository serviceRepository;

        @BeforeEach
        void setUp() {
                // 각 테스트 전 테스트용 데이터만 정리 (트랜잭션 롤백으로 자동 정리됨)
                // 실제 DB 데이터는 건드리지 않음
        }

        @Test
        @DisplayName("서비스 코드로 서비스를 조회할 수 있다")
        void 서비스_코드로_서비스를_조회할_수_있다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mFind Service By Service Code\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 테스트용 서비스 엔티티 저장");
                ServiceEntity service = ServiceEntity.builder()
                                .serviceCode("douzone")
                                .serviceName("더존 비즈온")
                                .status(ServiceStatus.ACTIVE)
                                .createdBy("admin-uuid-0000-0000-000000000004")
                                .createdIp("127.0.0.1")
                                .build();

                serviceRepository.save(service);

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 서비스 코드로 조회");
                Optional<ServiceEntity> found = serviceRepository.findByServiceCode("douzone");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 조회 결과 검증");
                assertThat(found).isPresent();
                assertThat(found.get().getServiceCode()).isEqualTo("douzone");
                assertThat(found.get().getServiceName()).isEqualTo("더존 비즈온");
                System.out
                                .println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m'douzone'\033[0m 서비스 조회 성공\n");
        }

        @Test
        @DisplayName("존재하지 않는 서비스 코드로 조회하면 빈 Optional을 반환한다")
        void 존재하지_않는_서비스_코드로_조회하면_빈_Optional을_반환한다() {
                System.out
                                .println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mFind Non-Existent Service Code\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 빈 데이터베이스 상태");

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 존재하지 않는 서비스 코드로 조회");
                Optional<ServiceEntity> found = serviceRepository.findByServiceCode("non-existent");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 빈 Optional 반환 검증");
                assertThat(found).isEmpty();
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mEmpty Optional\033[0m 반환됨\n");
        }

        @Test
        @DisplayName("서비스 상태별로 서비스 목록을 조회할 수 있다")
        void 서비스_상태별로_서비스_목록을_조회할_수_있다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mFind Services By Status\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 다양한 상태의 서비스들 저장");
                ServiceEntity activeService1 = createTestService("douzone", "더존 비즈온", ServiceStatus.ACTIVE);
                ServiceEntity activeService2 = createTestService("service1", "서비스1", ServiceStatus.ACTIVE);
                ServiceEntity inactiveService = createTestService("service2", "서비스2", ServiceStatus.INACTIVE);
                ServiceEntity maintenanceService = createTestService("service3", "서비스3", ServiceStatus.MAINTENANCE);

                serviceRepository.save(activeService1);
                serviceRepository.save(activeService2);
                serviceRepository.save(inactiveService);
                serviceRepository.save(maintenanceService);

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m ACTIVE 상태 서비스 조회");
                List<ServiceEntity> activeServices = serviceRepository.findByStatus(ServiceStatus.ACTIVE);

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 테스트 추가한 ACTIVE 서비스 포함 확인");
                // 기존 DB 데이터 + 테스트 추가 데이터 = 최소 2개 이상이어야 함
                assertThat(activeServices.size()).isGreaterThanOrEqualTo(2);

                // 테스트에서 추가한 서비스 코드들이 포함되어 있는지 확인
                assertThat(activeServices).extracting(ServiceEntity::getServiceCode)
                                .contains("douzone", "service1");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m 테스트 추가 서비스(\033[32mdouzone, service1\033[0m) 모두 조회됨 (총 \033[32m"
                                                + activeServices.size() + "개\033[0m)\n");
        }

        @Test
        @DisplayName("서비스 코드가 이미 존재하는지 확인할 수 있다")
        void 서비스_코드가_이미_존재하는지_확인할_수_있다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mCheck Service Code Existence\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 기존 서비스 저장");
                ServiceEntity existingService = createTestService("douzone", "더존 비즈온", ServiceStatus.ACTIVE);
                serviceRepository.save(existingService);

                // When & Then
                System.out.println("  \033[2m⚡ Action:\033[0m 서비스 코드 존재 여부 확인");
                System.out.println("  \033[2m✨ Verify:\033[0m 존재하는 코드와 존재하지 않는 코드 검증");

                boolean existingCodeExists = serviceRepository.existsByServiceCode("douzone");
                boolean nonExistingCodeExists = serviceRepository.existsByServiceCode("non-existent");

                assertThat(existingCodeExists).isTrue();
                assertThat(nonExistingCodeExists).isFalse();
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m'douzone'\033[0m 존재, \033[33m'non-existent'\033[0m 존재하지 않음\n");
        }

        @Test
        @DisplayName("서비스 이름으로 부분 검색할 수 있다")
        void 서비스_이름으로_부분_검색할_수_있다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mSearch Services By Name Pattern\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 다양한 이름의 서비스들 저장");
                ServiceEntity douzoneService = createTestService("douzone", "더존 비즈온", ServiceStatus.ACTIVE);
                ServiceEntity bizwareService = createTestService("bizware", "더존 비즈웨어", ServiceStatus.ACTIVE);
                ServiceEntity otherService = createTestService("other", "기타 서비스", ServiceStatus.ACTIVE);

                serviceRepository.save(douzoneService);
                serviceRepository.save(bizwareService);
                serviceRepository.save(otherService);

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m '더존'으로 부분 검색");
                List<ServiceEntity> foundServices = serviceRepository.findByServiceNameContainingIgnoreCase("더존");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 테스트 추가한 '더존' 포함 서비스들 확인");
                // 테스트에서 추가한 서비스 확인 (기존 DB에는 '더존'이 포함된 서비스가 없을 수 있음)
                assertThat(foundServices.size()).isGreaterThanOrEqualTo(2);
                assertThat(foundServices).extracting(ServiceEntity::getServiceCode)
                                .contains("douzone", "bizware");
                System.out
                                .println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m2개\033[0m의 '더존' 관련 서비스 조회됨\n");
        }

        @Test
        @DisplayName("활성 상태의 서비스만 조회할 수 있다")
        void 활성_상태의_서비스만_조회할_수_있다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mFind Only Active Services\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 활성/비활성 서비스 혼재 저장");
                ServiceEntity activeService1 = createTestService("active1", "활성 서비스1", ServiceStatus.ACTIVE);
                ServiceEntity activeService2 = createTestService("active2", "활성 서비스2", ServiceStatus.ACTIVE);
                ServiceEntity inactiveService = createTestService("inactive1", "비활성 서비스", ServiceStatus.INACTIVE);

                serviceRepository.save(activeService1);
                serviceRepository.save(activeService2);
                serviceRepository.save(inactiveService);

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 활성 서비스만 조회");
                List<ServiceEntity> activeServices = serviceRepository.findActiveServices();

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 테스트 추가한 활성 서비스들 확인");
                // 기존 DB 데이터 + 테스트 추가 데이터 = 최소 2개 이상이어야 함
                assertThat(activeServices.size()).isGreaterThanOrEqualTo(2);
                assertThat(activeServices).allMatch(ServiceEntity::isActive);
                assertThat(activeServices).extracting(ServiceEntity::getServiceCode)
                                .contains("active1", "active2");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m2개\033[0m의 활성 서비스만 조회됨\n");
        }

        /**
         * 테스트용 서비스 엔티티 생성 헬퍼 메서드
         */
        private ServiceEntity createTestService(String serviceCode, String serviceName, ServiceStatus status) {
                return ServiceEntity.builder()
                                .serviceCode(serviceCode)
                                .serviceName(serviceName)
                                .status(status)
                                .createdBy("admin-uuid-0000-0000-000000000004")
                                .createdIp("127.0.0.1")
                                .build();
        }

        /**
         * 테스트용 설정 - 필요한 Bean들만 제공
         */
        @TestConfiguration
        static class TestConfig {

                @Bean
                public ObjectMapper objectMapper() {
                        return new ObjectMapper();
                }
        }
}
