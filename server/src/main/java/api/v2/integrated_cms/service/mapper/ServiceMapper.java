package api.v2.integrated_cms.service.mapper;

import api.v2.integrated_cms.service.domain.ServiceEntity;
import api.v2.integrated_cms.service.dto.ServiceDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 서비스 엔티티와 DTO 간 매핑을 담당하는 Mapper
 * 
 * @author CMS Team
 * @since v2.0
 */
@Component
@RequiredArgsConstructor
public class ServiceMapper {

    private final ObjectMapper objectMapper;

    /**
     * CreateRequest DTO를 ServiceEntity로 변환
     * 
     * @param request 생성 요청 DTO
     * @return ServiceEntity
     */
    public ServiceEntity toEntity(ServiceDto.CreateRequest request) {
        return ServiceEntity.builder()
                .serviceCode(request.getServiceCode())
                .serviceName(request.getServiceName())
                .status(request.getStatus())
                .serviceDomain(request.getServiceDomain())
                .apiBaseUrl(request.getApiBaseUrl())
                .dbConnectionInfo(request.getDbConnectionInfo())
                .description(request.getDescription())
                .config(parseConfigToJsonNode(request.getConfig()))
                .build();
    }

    /**
     * ServiceEntity를 Response DTO로 변환
     * 
     * @param entity ServiceEntity
     * @return Response DTO
     */
    public ServiceDto.Response toResponse(ServiceEntity entity) {
        ServiceDto.Response response = ServiceDto.Response.builder()
                .serviceId(entity.getServiceId())
                .serviceCode(entity.getServiceCode())
                .serviceName(entity.getServiceName())
                .status(entity.getStatus())
                .serviceDomain(entity.getServiceDomain())
                .apiBaseUrl(entity.getApiBaseUrl())
                .dbConnectionInfo(entity.getDbConnectionInfo())
                .description(entity.getDescription())
                .config(parseJsonNodeToString(entity.getConfig()))
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .createdIp(entity.getCreatedIp())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedIp(entity.getUpdatedIp())
                .build();
        return response;
    }

    /**
     * ServiceEntity를 SummaryResponse DTO로 변환
     * 
     * @param entity ServiceEntity
     * @return SummaryResponse DTO
     */
    public ServiceDto.SummaryResponse toSummaryResponse(ServiceEntity entity) {
        return ServiceDto.SummaryResponse.builder()
                .serviceId(entity.getServiceId())
                .serviceCode(entity.getServiceCode())
                .serviceName(entity.getServiceName())
                .status(entity.getStatus())
                .serviceDomain(entity.getServiceDomain())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * UpdateRequest DTO로 기존 ServiceEntity 업데이트
     * 
     * @param entity  업데이트할 엔티티
     * @param request 업데이트 요청 DTO
     */
    public void updateEntity(ServiceEntity entity, ServiceDto.UpdateRequest request) {
        entity.updateBasicInfo(
                request.getServiceName(),
                request.getStatus(),
                request.getServiceDomain(),
                request.getApiBaseUrl(),
                request.getDbConnectionInfo(),
                request.getDescription(),
                parseConfigToJsonNode(request.getConfig()));
    }

    /**
     * String config를 JsonNode로 변환
     */
    private JsonNode parseConfigToJsonNode(String config) {
        if (config == null || config.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(config);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * JsonNode config를 String으로 변환
     */
    private String parseJsonNodeToString(JsonNode config) {
        if (config == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return null;
        }
    }
}
