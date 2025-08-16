package api.v2.integrated_cms.service.dto;

import api.v2.integrated_cms.service.domain.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 서비스 관련 DTO 클래스들
 * 
 * 서비스 메타데이터 관리를 위한 요청/응답 DTO를 정의합니다.
 * 
 * @author CMS Team
 * @since v2.0
 */
public class ServiceDto {

    /**
     * 서비스 생성 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "서비스 코드는 필수입니다")
        @Pattern(regexp = "^[a-z0-9_-]+$", message = "서비스 코드는 영문 소문자, 숫자, _, - 만 사용 가능합니다")
        @Size(min = 2, max = 50, message = "서비스 코드는 2-50자 사이여야 합니다")
        private String serviceCode;

        @NotBlank(message = "서비스 이름은 필수입니다")
        @Size(min = 2, max = 100, message = "서비스 이름은 2-100자 사이여야 합니다")
        private String serviceName;

        @NotNull(message = "서비스 상태는 필수입니다")
        private ServiceStatus status;

        @Size(max = 255, message = "서비스 도메인은 255자를 초과할 수 없습니다")
        private String serviceDomain;

        @Size(max = 255, message = "API Base URL은 255자를 초과할 수 없습니다")
        private String apiBaseUrl;

        @Size(max = 1000, message = "DB 연결 정보는 1000자를 초과할 수 없습니다")
        private String dbConnectionInfo;

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
        private String description;

        @Size(max = 2000, message = "설정 정보는 2000자를 초과할 수 없습니다")
        private String config;
    }

    /**
     * 서비스 수정 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        @NotBlank(message = "서비스 이름은 필수입니다")
        @Size(min = 2, max = 100, message = "서비스 이름은 2-100자 사이여야 합니다")
        private String serviceName;

        @NotNull(message = "서비스 상태는 필수입니다")
        private ServiceStatus status;

        @Size(max = 255, message = "서비스 도메인은 255자를 초과할 수 없습니다")
        private String serviceDomain;

        @Size(max = 255, message = "API Base URL은 255자를 초과할 수 없습니다")
        private String apiBaseUrl;

        @Size(max = 1000, message = "DB 연결 정보는 1000자를 초과할 수 없습니다")
        private String dbConnectionInfo;

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
        private String description;

        @Size(max = 2000, message = "설정 정보는 2000자를 초과할 수 없습니다")
        private String config;
    }

    /**
     * 서비스 상태 변경 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {

        @NotNull(message = "서비스 상태는 필수입니다")
        private ServiceStatus status;

        @Size(max = 200, message = "상태 변경 사유는 200자를 초과할 수 없습니다")
        private String reason;
    }

    /**
     * 서비스 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {

        private String serviceId;
        private String serviceCode;
        private String serviceName;
        private ServiceStatus status;
        private String serviceDomain;
        private String apiBaseUrl;
        private String dbConnectionInfo;
        private String description;
        private String config;
        private LocalDateTime createdAt;
        private String createdBy;
        private String createdIp;
        private LocalDateTime updatedAt;
        private String updatedBy;
        private String updatedIp;
    }

    /**
     * 서비스 요약 응답 DTO (목록용)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse {

        private String serviceId;
        private String serviceCode;
        private String serviceName;
        private ServiceStatus status;
        private String serviceDomain;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 서비스 검색 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {

        @Size(max = 100, message = "검색어는 100자를 초과할 수 없습니다")
        private String searchTerm;

        private ServiceStatus status;

        @Builder.Default
        private int page = 0;

        @Builder.Default
        private int size = 10;

        @Builder.Default
        private String sortBy = "createdAt";

        @Builder.Default
        private String sortDirection = "DESC";
    }

    /**
     * 서비스 코드 가용성 확인 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeAvailabilityResponse {

        private String serviceCode;
        private boolean available;
        private String message;
    }

    /**
     * 서비스 상태 통계 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusStatisticsResponse {

        private long activeCount;
        private long inactiveCount;
        private long maintenanceCount;
        private long totalCount;
    }

    /**
     * 서비스 설정 업데이트 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigUpdateRequest {

        @NotBlank(message = "설정 키는 필수입니다")
        @Size(max = 100, message = "설정 키는 100자를 초과할 수 없습니다")
        private String configKey;

        @Size(max = 1000, message = "설정 값은 1000자를 초과할 수 없습니다")
        private String configValue;
    }
}
