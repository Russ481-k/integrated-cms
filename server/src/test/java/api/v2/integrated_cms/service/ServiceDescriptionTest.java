package api.v2.integrated_cms.service;

import api.v2.integrated_cms.service.domain.ServiceEntity;
import api.v2.integrated_cms.service.domain.ServiceStatus;
import api.v2.integrated_cms.service.dto.ServiceDto;
import api.v2.integrated_cms.service.mapper.ServiceMapper;
import api.v2.integrated_cms.service.repository.ServiceRepository;
import testutils.base.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static testutils.logging.TestLoggingUtils.*;

@DisplayName("서비스 Description 필드 처리 테스트")
public class ServiceDescriptionTest extends BaseIntegrationTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceMapper serviceMapper;

    @Autowired
    private ServiceMetadataService serviceMetadataService;

    @Test
    @DisplayName("서비스 생성 시 description이 정상적으로 저장되고 조회된다")
    void 서비스_생성시_description이_정상적으로_저장되고_조회된다() {
        printTestHeader("Service Description Save and Retrieve Test");

        printGiven("description이 포함된 서비스 생성 요청 준비");
        String testDescription = "이것은 테스트용 서비스 설명입니다. 한글과 특수문자(!@#$%)도 포함됩니다.";
        ServiceDto.CreateRequest createRequest = ServiceDto.CreateRequest.builder()
                .serviceCode("test-desc-service")
                .serviceName("테스트 설명 서비스")
                .status(ServiceStatus.ACTIVE)
                .description(testDescription)
                .serviceDomain("https://test-desc.com")
                .build();
        printGivenDetail("Original description: " + colorValue(testDescription));

        printWhen("서비스 생성 및 조회");
        ServiceDto.Response createdService = serviceMetadataService.createService(
                createRequest, "test-admin", "127.0.0.1");
        ServiceDto.Response retrievedService = serviceMetadataService.getServiceById(
                createdService.getServiceId()).orElse(null);

        printThen("생성된 서비스와 조회된 서비스의 description 필드 검증");
        assertThat(createdService).isNotNull();
        assertThat(retrievedService).isNotNull();

        assertThat(createdService.getDescription()).isEqualTo(testDescription);
        assertThat(retrievedService.getDescription()).isEqualTo(testDescription);

        printSuccess("Description 필드가 생성/조회 모두에서 정확히 처리됨");
        printAssertionSuccess("Created: " + colorValue(createdService.getDescription()));
        printAssertionSuccess("Retrieved: " + colorValue(retrievedService.getDescription()));
    }

    @Test
    @DisplayName("서비스 수정 시 description이 정상적으로 업데이트된다")
    void 서비스_수정시_description이_정상적으로_업데이트된다() {
        printTestHeader("Service Description Update Test");

        printGiven("기존 서비스 생성");
        ServiceEntity entity = ServiceEntity.builder()
                .serviceCode("test-update-desc")
                .serviceName("업데이트 테스트 서비스")
                .status(ServiceStatus.ACTIVE)
                .description("원래 설명")
                .createdBy("admin-uuid-0000-0000-000000000004")
                .createdIp("127.0.0.1")
                .build();
        ServiceEntity savedEntity = serviceRepository.save(entity);
        printGivenDetail("Original description: " + colorValue("원래 설명"));

        printWhen("description 업데이트 요청");
        String newDescription = "업데이트된 새로운 설명입니다. 더 자세한 내용을 포함합니다.";
        ServiceDto.UpdateRequest updateRequest = ServiceDto.UpdateRequest.builder()
                .serviceName("업데이트된 서비스")
                .status(ServiceStatus.ACTIVE)
                .description(newDescription)
                .build();
        printWhenMethodCall("updateService() with new description: " + colorValue(newDescription));

        ServiceDto.Response updatedService = serviceMetadataService.updateService(
                savedEntity.getServiceId(), updateRequest, "test-admin", "127.0.0.1");

        printThen("업데이트된 description 확인");
        assertThat(updatedService.getDescription()).isEqualTo(newDescription);

        // DB에서 직접 조회하여 재확인
        ServiceEntity dbEntity = serviceRepository.findById(savedEntity.getServiceId()).orElse(null);
        assertThat(dbEntity).isNotNull();
        assertThat(dbEntity.getDescription()).isEqualTo(newDescription);

        printSuccess("Description 업데이트 성공");
        printAssertionSuccess("Updated description: " + colorValue(updatedService.getDescription()));
        printAssertionSuccess("DB description: " + colorValue(dbEntity.getDescription()));
    }

    @Test
    @DisplayName("Mapper가 description 필드를 올바르게 변환한다")
    void Mapper가_description_필드를_올바르게_변환한다() {
        printTestHeader("ServiceMapper Description Conversion Test");

        printGiven("description이 포함된 ServiceEntity 생성");
        String testDescription = "매퍼 테스트용 설명 - 한글, 영문, 숫자123, 특수문자!@#";
        ServiceEntity entity = ServiceEntity.builder()
                .serviceId("mapper-test-id")
                .serviceCode("mapper-test")
                .serviceName("매퍼 테스트")
                .status(ServiceStatus.ACTIVE)
                .description(testDescription)
                .build();
        printGivenDetail("Entity description: " + colorValue(testDescription));

        printWhen("Entity를 Response DTO로 변환");
        ServiceDto.Response response = serviceMapper.toResponse(entity);
        printWhenMethodCall("serviceMapper.toResponse(entity)");

        printThen("변환된 Response DTO의 description 검증");
        assertThat(response.getDescription()).isEqualTo(testDescription);
        printSuccess("Mapper 변환 성공");
        printAssertionSuccess("Response description: " + colorValue(response.getDescription()));

        printWhen("Entity를 SummaryResponse DTO로 변환");
        ServiceDto.SummaryResponse summaryResponse = serviceMapper.toSummaryResponse(entity);
        printWhenMethodCall("serviceMapper.toSummaryResponse(entity)");

        printThen("변환된 SummaryResponse DTO의 description 검증");
        assertThat(summaryResponse.getDescription()).isEqualTo(testDescription);
        printSuccess("SummaryResponse 변환 성공");
        printAssertionSuccess("Summary description: " + colorValue(summaryResponse.getDescription()));
    }

    @Test
    @DisplayName("빈 description과 null description 처리 확인")
    void 빈_description과_null_description_처리_확인() {
        printTestHeader("Empty and Null Description Handling Test");

        printGiven("빈 문자열 description으로 서비스 생성");
        ServiceDto.CreateRequest emptyRequest = ServiceDto.CreateRequest.builder()
                .serviceCode("empty-desc-test")
                .serviceName("빈 설명 테스트")
                .status(ServiceStatus.ACTIVE)
                .description("")
                .build();

        ServiceDto.Response emptyDescService = serviceMetadataService.createService(
                emptyRequest, "test-admin", "127.0.0.1");

        printThen("빈 문자열 description 처리 확인");
        assertThat(emptyDescService.getDescription()).isEqualTo("");
        printSuccess("빈 문자열 description 정상 처리");

        printGiven("null description으로 서비스 생성");
        ServiceDto.CreateRequest nullRequest = ServiceDto.CreateRequest.builder()
                .serviceCode("null-desc-test")
                .serviceName("null 설명 테스트")
                .status(ServiceStatus.ACTIVE)
                .description(null)
                .build();

        ServiceDto.Response nullDescService = serviceMetadataService.createService(
                nullRequest, "test-admin", "127.0.0.1");

        printThen("null description 처리 확인");
        assertThat(nullDescService.getDescription()).isNull();
        printSuccess("null description 정상 처리");
    }
}
