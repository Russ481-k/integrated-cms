package api.v2.integrated_cms.service.repository;

import api.v2.integrated_cms.service.domain.ServiceEntity;
import api.v2.integrated_cms.service.domain.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceRepository 순수 JPA 테스트
 * 
 * Spring 컨텍스트 로딩 문제를 피하기 위해 최소한의 설정으로 JPA Repository만 테스트합니다.
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest(showSql = false)
class ServiceRepositorySimpleTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ServiceRepository serviceRepository;

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    @BeforeEach
    void setUp() {
        // 각 테스트 전 데이터 정리
        serviceRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("서비스 저장 및 조회가 정상적으로 작동한다")
    void 서비스_저장_및_조회가_정상적으로_작동한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mService Save and Find Basic Operations\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 테스트용 서비스 엔티티 생성");
        ServiceEntity service = ServiceEntity.builder()
                .serviceCode("test-service")
                .serviceName("테스트 서비스")
                .status(ServiceStatus.ACTIVE)
                .description("테스트용 서비스입니다")
                .createdBy("test-admin")
                .createdIp("127.0.0.1")
                .build();

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 서비스 저장 및 조회");
        ServiceEntity savedService = serviceRepository.save(service);
        entityManager.flush();
        entityManager.clear();

        Optional<ServiceEntity> foundService = serviceRepository.findById(savedService.getServiceId());

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 저장 및 조회 결과 검증");
        assertThat(foundService).isPresent();
        assertThat(foundService.get().getServiceCode()).isEqualTo("test-service");
        assertThat(foundService.get().getServiceName()).isEqualTo("테스트 서비스");
        assertThat(foundService.get().getStatus()).isEqualTo(ServiceStatus.ACTIVE);
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m서비스 저장 및 조회 성공\033[0m\n");
    }

    @Test
    @DisplayName("서비스 코드로 서비스를 조회할 수 있다")
    void 서비스_코드로_서비스를_조회할_수_있다() {
        System.out.println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mFind Service By Service Code\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 테스트용 서비스 엔티티 저장");
        ServiceEntity service = createTestService("douzone", "더존 비즈온", ServiceStatus.ACTIVE);
        serviceRepository.save(service);
        entityManager.flush();

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
                .println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mFind Non-Existent Service Code\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 빈 데이터베이스 상태");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 존재하지 않는 서비스 코드로 조회");
        Optional<ServiceEntity> found = serviceRepository.findByServiceCode("non-existent");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 빈 Optional 반환 검증");
        assertThat(found).isEmpty();
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mEmpty Optional\033[0m 반환됨\n");
    }

    @Test
    @DisplayName("서비스 상태별로 서비스 목록을 조회할 수 있다")
    void 서비스_상태별로_서비스_목록을_조회할_수_있다() {
        System.out.println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mFind Services By Status\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 다양한 상태의 서비스들 저장");
        ServiceEntity activeService1 = createTestService("douzone", "더존 비즈온", ServiceStatus.ACTIVE);
        ServiceEntity activeService2 = createTestService("service1", "서비스1", ServiceStatus.ACTIVE);
        ServiceEntity inactiveService = createTestService("service2", "서비스2", ServiceStatus.INACTIVE);

        serviceRepository.save(activeService1);
        serviceRepository.save(activeService2);
        serviceRepository.save(inactiveService);
        entityManager.flush();

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m ACTIVE 상태 서비스 조회");
        List<ServiceEntity> activeServices = serviceRepository.findByStatus(ServiceStatus.ACTIVE);

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m ACTIVE 서비스 2개 조회되는지 검증");
        assertThat(activeServices).hasSize(2);
        assertThat(activeServices).extracting(ServiceEntity::getServiceCode)
                .containsExactlyInAnyOrder("douzone", "service1");
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m2개\033[0m의 ACTIVE 서비스 조회됨\n");
    }

    @Test
    @DisplayName("서비스 코드 존재 여부를 확인할 수 있다")
    void 서비스_코드_존재_여부를_확인할_수_있다() {
        System.out.println("\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mCheck Service Code Existence\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 기존 서비스 저장");
        ServiceEntity existingService = createTestService("douzone", "더존 비즈온", ServiceStatus.ACTIVE);
        serviceRepository.save(existingService);
        entityManager.flush();

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
                "\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mSearch Services By Name Pattern\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 다양한 이름의 서비스들 저장");
        ServiceEntity douzoneService = createTestService("douzone", "더존 비즈온", ServiceStatus.ACTIVE);
        ServiceEntity bizwareService = createTestService("bizware", "더존 비즈웨어", ServiceStatus.ACTIVE);
        ServiceEntity otherService = createTestService("other", "기타 서비스", ServiceStatus.ACTIVE);

        serviceRepository.save(douzoneService);
        serviceRepository.save(bizwareService);
        serviceRepository.save(otherService);
        entityManager.flush();

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m '더존'으로 부분 검색");
        List<ServiceEntity> foundServices = serviceRepository.findByServiceNameContainingIgnoreCase("더존");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m '더존'이 포함된 서비스 2개 조회되는지 검증");
        assertThat(foundServices).hasSize(2);
        assertThat(foundServices).extracting(ServiceEntity::getServiceCode)
                .containsExactlyInAnyOrder("douzone", "bizware");
        System.out
                .println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m2개\033[0m의 '더존' 관련 서비스 조회됨\n");
    }

    /**
     * 테스트용 서비스 엔티티 생성 헬퍼 메서드
     */
    private ServiceEntity createTestService(String serviceCode, String serviceName, ServiceStatus status) {
        return ServiceEntity.builder()
                .serviceCode(serviceCode)
                .serviceName(serviceName)
                .status(status)
                .description("테스트용 " + serviceName)
                .createdBy("test-admin")
                .createdIp("127.0.0.1")
                .build();
    }
}
