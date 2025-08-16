package api.v2.integrated_cms.service.repository;

import api.v2.integrated_cms.service.domain.ServiceEntity;
import api.v2.integrated_cms.service.domain.ServiceStatus;
import testutils.base.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceRepository 통합 테스트 (표준 컨벤션)
 * 
 * 실제 integrated_cms 데이터베이스와 연결하여 Repository 계층을 테스트합니다.
 * TDD 커서룰에 따른 표준 테스트 컨벤션을 적용했습니다.
 */
class ServiceRepositoryStandardTest extends BaseIntegrationTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    @DisplayName("서비스 저장 및 조회가 정상적으로 작동한다")
    void 서비스_저장_및_조회가_정상적으로_작동한다() {
        printTestHeader("Service Save and Find Basic Operations");

        // Given
        printGiven("테스트용 서비스 엔티티 생성");
        ServiceEntity service = ServiceEntity.builder()
                .serviceCode("test-service")
                .serviceName("테스트 서비스")
                .description("테스트용 서비스입니다")
                .status(ServiceStatus.ACTIVE)
                .createdBy("admin-uuid-0000-0000-000000000004")
                .createdIp("127.0.0.1")
                .build();

        // When
        printWhen("서비스 저장 및 조회");
        ServiceEntity savedService = serviceRepository.save(service);
        Optional<ServiceEntity> foundService = serviceRepository.findByServiceCode("test-service");

        // Then
        printThen("저장된 서비스가 올바르게 조회되는지 검증");
        assertThat(savedService).isNotNull();
        assertThat(foundService).isPresent();
        assertThat(foundService.get().getServiceCode()).isEqualTo("test-service");
        assertThat(foundService.get().getServiceName()).isEqualTo("테스트 서비스");
        printSuccess("서비스 저장 및 조회 성공");
    }

    @Test
    @DisplayName("서비스 코드로 서비스를 조회할 수 있다")
    void 서비스_코드로_서비스를_조회할_수_있다() {
        printTestHeader("Find Service By Service Code");

        // Given
        printGiven("테스트용 서비스 엔티티 저장");
        ServiceEntity service = ServiceEntity.builder()
                .serviceCode("douzone")
                .serviceName("더존 비즈온")
                .description("테스트용 더존 비즈 온")
                .status(ServiceStatus.ACTIVE)
                .createdBy("admin-uuid-0000-0000-000000000004")
                .createdIp("127.0.0.1")
                .build();
        serviceRepository.save(service);

        // When
        printWhen("서비스 코드로 조회");
        Optional<ServiceEntity> result = serviceRepository.findByServiceCode("douzone");

        // Then
        printThen("조회 결과 검증");
        assertThat(result).isPresent();
        assertThat(result.get().getServiceCode()).isEqualTo("douzone");
        assertThat(result.get().getServiceName()).isEqualTo("더존 비즈온");
        printSuccess("'douzone' 서비스 조회 성공");
    }

    @Test
    @DisplayName("서비스 상태별로 서비스 목록을 조회할 수 있다")
    void 서비스_상태별로_서비스_목록을_조회할_수_있다() {
        printTestHeader("Find Services By Status");

        // Given
        printGiven("다양한 상태의 서비스 엔티티들 저장");
        ServiceEntity activeService1 = ServiceEntity.builder()
                .serviceCode("douzone")
                .serviceName("더존 비즈온")
                .status(ServiceStatus.ACTIVE)
                .createdBy("admin-uuid-0000-0000-000000000004")
                .createdIp("127.0.0.1")
                .build();

        ServiceEntity activeService2 = ServiceEntity.builder()
                .serviceCode("service1")
                .serviceName("서비스 1")
                .status(ServiceStatus.ACTIVE)
                .createdBy("admin-uuid-0000-0000-000000000004")
                .createdIp("127.0.0.1")
                .build();

        ServiceEntity inactiveService = ServiceEntity.builder()
                .serviceCode("inactive-service")
                .serviceName("비활성 서비스")
                .status(ServiceStatus.INACTIVE)
                .createdBy("admin-uuid-0000-0000-000000000004")
                .createdIp("127.0.0.1")
                .build();

        serviceRepository.save(activeService1);
        serviceRepository.save(activeService2);
        serviceRepository.save(inactiveService);

        // When
        printWhen("ACTIVE 상태 서비스 조회");
        List<ServiceEntity> activeServices = serviceRepository.findByStatus(ServiceStatus.ACTIVE);

        // Then
        printThen("테스트 추가한 ACTIVE 서비스 포함 확인");
        // 기존 DB 데이터 + 테스트 추가 데이터 = 최소 2개 이상이어야 함
        assertThat(activeServices.size()).isGreaterThanOrEqualTo(2);

        // 테스트에서 추가한 서비스 코드들이 포함되어 있는지 확인
        List<String> serviceCodes = activeServices.stream()
                .map(ServiceEntity::getServiceCode)
                .collect(java.util.stream.Collectors.toList());

        assertThat(serviceCodes).contains("douzone", "service1");
        printSuccess("테스트 추가 서비스(douzone, service1) 모두 조회됨 (총 " + activeServices.size() + "개)");
    }

    @Test
    @DisplayName("서비스 코드 존재 여부를 확인할 수 있다")
    void 서비스_코드_존재_여부를_확인할_수_있다() {
        printTestHeader("Check Service Code Existence");

        // Given
        printGiven("기존 서비스와 새로운 서비스 코드 준비");
        ServiceEntity existingService = ServiceEntity.builder()
                .serviceCode("existing-service")
                .serviceName("기존 서비스")
                .status(ServiceStatus.ACTIVE)
                .createdBy("admin-uuid-0000-0000-000000000004")
                .createdIp("127.0.0.1")
                .build();
        serviceRepository.save(existingService);

        // When & Then
        printWhen("기존 서비스 코드 존재 여부 확인");
        boolean existingExists = serviceRepository.existsByServiceCode("existing-service");
        assertThat(existingExists).isTrue();
        printSuccess("기존 서비스 코드 존재 확인됨");

        printWhen("존재하지 않는 서비스 코드 확인");
        boolean nonExistingExists = serviceRepository.existsByServiceCode("non-existing-service");
        assertThat(nonExistingExists).isFalse();
        printSuccess("존재하지 않는 서비스 코드 정확히 판별됨");
    }

    @Test
    @DisplayName("유효한 서비스 ID로 변환할 수 있다")
    void 유효한_서비스_ID로_변환할_수_있다() {
        printTestHeader("Service ID Validation and Conversion");

        // Given
        printGiven("다양한 서비스 코드 패턴 준비");
        String[] validCodes = { "douzone", "service1", "integrated_cms" };
        String[] invalidCodes = { "", "   ", "123-invalid", "tool.long.service.name.here" };

        // When & Then
        printWhen("유효한 서비스 코드들 검증");
        for (String code : validCodes) {
            ServiceEntity service = ServiceEntity.builder()
                    .serviceCode(code)
                    .serviceName("테스트 서비스 - " + code)
                    .status(ServiceStatus.ACTIVE)
                    .createdBy("admin-uuid-0000-0000-000000000004")
                    .createdIp("127.0.0.1")
                    .build();

            ServiceEntity saved = serviceRepository.save(service);
            assertThat(saved.getServiceCode()).isEqualTo(code);
        }
        printSuccess("모든 유효한 서비스 코드 저장 성공");

        printWhen("서비스 코드 중복 확인");
        List<ServiceEntity> allServices = serviceRepository.findAll();
        long uniqueServiceCodes = allServices.stream()
                .map(ServiceEntity::getServiceCode)
                .distinct()
                .count();
        assertThat(uniqueServiceCodes).isEqualTo(allServices.size());
        printSuccess("서비스 코드 중복 없음 확인");
    }
}
