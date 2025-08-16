package api.v2.integrated_cms.service.controller;

import api.v2.common.dto.ApiResponseSchema;
import api.v2.integrated_cms.service.ServiceMetadataService;
import api.v2.integrated_cms.service.domain.ServiceStatus;
import api.v2.integrated_cms.service.dto.ServiceDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

/**
 * 서비스 메타데이터 관리 Controller
 * 
 * 통합 CMS의 서비스 메타데이터 관리를 위한 REST API를 제공합니다.
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@RestController
@RequestMapping("/integrated-cms/service-metadata")
@RequiredArgsConstructor
@Tag(name = "Service Metadata", description = "서비스 메타데이터 관리 API")
public class ServiceMetadataController {

    private final ServiceMetadataService serviceMetadataService;

    /**
     * 새로운 서비스를 생성합니다.
     * 
     * @param request     서비스 생성 요청
     * @param principal   인증된 사용자 정보
     * @param httpRequest HTTP 요청 정보
     * @return 생성된 서비스 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN')")
    @Operation(summary = "서비스 생성", description = "새로운 서비스를 생성합니다.")
    public ResponseEntity<ApiResponseSchema<ServiceDto.Response>> createService(
            @Valid @RequestBody ServiceDto.CreateRequest request,
            Principal principal,
            HttpServletRequest httpRequest) {

        log.info("서비스 생성 API 호출: serviceCode={}, user={}",
                request.getServiceCode(), principal.getName());

        try {
            String clientIp = getClientIp(httpRequest);
            ServiceDto.Response response = serviceMetadataService.createService(
                    request, principal.getName(), clientIp);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseSchema.success(response, "서비스가 성공적으로 생성되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("서비스 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("ERR_SERVICE_001", e.getMessage()));
        } catch (Exception e) {
            log.error("서비스 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseSchema.error("ERR_SYS_001", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 서비스 ID로 서비스를 조회합니다.
     * 
     * @param serviceId 서비스 ID
     * @return 서비스 정보
     */
    @GetMapping("/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN') or hasRole('SITE_ADMIN')")
    @Operation(summary = "서비스 조회", description = "서비스 ID로 서비스 정보를 조회합니다.")
    public ResponseEntity<ApiResponseSchema<ServiceDto.Response>> getService(
            @Parameter(description = "서비스 ID") @PathVariable String serviceId) {

        log.debug("서비스 조회 API 호출: serviceId={}", serviceId);

        return serviceMetadataService.getServiceById(serviceId)
                .map(service -> ResponseEntity.ok(
                        ApiResponseSchema.success(service, "서비스 조회가 완료되었습니다.")))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 서비스 코드로 서비스를 조회합니다.
     * 
     * @param serviceCode 서비스 코드
     * @return 서비스 정보
     */
    @GetMapping("/code/{serviceCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN') or hasRole('SITE_ADMIN')")
    @Operation(summary = "서비스 코드로 조회", description = "서비스 코드로 서비스 정보를 조회합니다.")
    public ResponseEntity<ApiResponseSchema<ServiceDto.Response>> getServiceByCode(
            @Parameter(description = "서비스 코드") @PathVariable String serviceCode) {

        log.debug("서비스 코드 조회 API 호출: serviceCode={}", serviceCode);

        return serviceMetadataService.getServiceByCode(serviceCode)
                .map(service -> ResponseEntity.ok(
                        ApiResponseSchema.success(service, "서비스 조회가 완료되었습니다.")))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 모든 서비스 목록을 조회합니다.
     * 
     * @return 서비스 목록
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN') or hasRole('SITE_ADMIN')")
    @Operation(summary = "서비스 목록 조회", description = "모든 서비스 목록을 조회합니다.")
    public ResponseEntity<ApiResponseSchema<List<ServiceDto.SummaryResponse>>> getAllServices() {

        log.debug("서비스 목록 조회 API 호출");

        List<ServiceDto.SummaryResponse> services = serviceMetadataService.getAllServices();
        return ResponseEntity.ok(
                ApiResponseSchema.success(services, "서비스 목록 조회가 완료되었습니다."));
    }

    /**
     * 활성 상태의 서비스 목록을 조회합니다.
     * 
     * @return 활성 서비스 목록
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN') or hasRole('SITE_ADMIN')")
    @Operation(summary = "활성 서비스 목록 조회", description = "활성 상태의 서비스 목록을 조회합니다.")
    public ResponseEntity<ApiResponseSchema<List<ServiceDto.SummaryResponse>>> getActiveServices() {

        log.debug("활성 서비스 목록 조회 API 호출");

        List<ServiceDto.SummaryResponse> services = serviceMetadataService.getActiveServices();
        return ResponseEntity.ok(
                ApiResponseSchema.success(services, "활성 서비스 목록 조회가 완료되었습니다."));
    }

    /**
     * 서비스 상태별 목록을 조회합니다.
     * 
     * @param status 서비스 상태
     * @return 해당 상태의 서비스 목록
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN') or hasRole('SITE_ADMIN')")
    @Operation(summary = "상태별 서비스 조회", description = "특정 상태의 서비스 목록을 조회합니다.")
    public ResponseEntity<ApiResponseSchema<List<ServiceDto.SummaryResponse>>> getServicesByStatus(
            @Parameter(description = "서비스 상태") @PathVariable ServiceStatus status) {

        log.debug("서비스 상태별 조회 API 호출: status={}", status);

        List<ServiceDto.SummaryResponse> services = serviceMetadataService.getServicesByStatus(status);
        return ResponseEntity.ok(
                ApiResponseSchema.success(services, "상태별 서비스 목록 조회가 완료되었습니다."));
    }

    /**
     * 서비스를 검색합니다.
     * 
     * @param request 검색 요청
     * @return 검색 결과 (페이징)
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN') or hasRole('SITE_ADMIN')")
    @Operation(summary = "서비스 검색", description = "검색 조건에 따라 서비스를 검색합니다.")
    public ResponseEntity<ApiResponseSchema<Page<ServiceDto.SummaryResponse>>> searchServices(
            @Valid @RequestBody ServiceDto.SearchRequest request) {

        log.debug("서비스 검색 API 호출: searchTerm={}, status={}",
                request.getSearchTerm(), request.getStatus());

        Page<ServiceDto.SummaryResponse> services = serviceMetadataService.searchServices(request);
        return ResponseEntity.ok(
                ApiResponseSchema.success(services, "서비스 검색이 완료되었습니다."));
    }

    /**
     * 서비스 정보를 수정합니다.
     * 
     * @param serviceId   서비스 ID
     * @param request     수정 요청
     * @param principal   인증된 사용자 정보
     * @param httpRequest HTTP 요청 정보
     * @return 수정된 서비스 정보
     */
    @PutMapping("/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN')")
    @Operation(summary = "서비스 수정", description = "서비스 정보를 수정합니다.")
    public ResponseEntity<ApiResponseSchema<ServiceDto.Response>> updateService(
            @Parameter(description = "서비스 ID") @PathVariable String serviceId,
            @Valid @RequestBody ServiceDto.UpdateRequest request,
            Principal principal,
            HttpServletRequest httpRequest) {

        log.info("서비스 수정 API 호출: serviceId={}, user={}",
                serviceId, principal.getName());

        try {
            String clientIp = getClientIp(httpRequest);
            ServiceDto.Response response = serviceMetadataService.updateService(
                    serviceId, request, principal.getName(), clientIp);

            return ResponseEntity.ok(
                    ApiResponseSchema.success(response, "서비스가 성공적으로 수정되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("서비스 수정 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("ERR_SERVICE_002", e.getMessage()));
        } catch (Exception e) {
            log.error("서비스 수정 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseSchema.error("ERR_SYS_001", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 서비스 상태를 변경합니다.
     * 
     * @param serviceId   서비스 ID
     * @param request     상태 변경 요청
     * @param principal   인증된 사용자 정보
     * @param httpRequest HTTP 요청 정보
     * @return 수정된 서비스 정보
     */
    @PatchMapping("/{serviceId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN')")
    @Operation(summary = "서비스 상태 변경", description = "서비스 상태를 변경합니다.")
    public ResponseEntity<ApiResponseSchema<ServiceDto.Response>> updateServiceStatus(
            @Parameter(description = "서비스 ID") @PathVariable String serviceId,
            @Valid @RequestBody ServiceDto.StatusUpdateRequest request,
            Principal principal,
            HttpServletRequest httpRequest) {

        log.info("서비스 상태 변경 API 호출: serviceId={}, status={}, user={}",
                serviceId, request.getStatus(), principal.getName());

        try {
            String clientIp = getClientIp(httpRequest);
            ServiceDto.Response response = serviceMetadataService.updateServiceStatus(
                    serviceId, request, principal.getName(), clientIp);

            return ResponseEntity.ok(
                    ApiResponseSchema.success(response, "서비스 상태가 성공적으로 변경되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("서비스 상태 변경 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("ERR_SERVICE_003", e.getMessage()));
        } catch (Exception e) {
            log.error("서비스 상태 변경 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseSchema.error("ERR_SYS_001", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 서비스를 삭제합니다.
     * 
     * @param serviceId 서비스 ID
     * @param principal 인증된 사용자 정보
     * @return 삭제 결과
     */
    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "서비스 삭제", description = "서비스를 삭제합니다. (SUPER_ADMIN만 가능)")
    public ResponseEntity<ApiResponseSchema<Void>> deleteService(
            @Parameter(description = "서비스 ID") @PathVariable String serviceId,
            Principal principal) {

        log.info("서비스 삭제 API 호출: serviceId={}, user={}",
                serviceId, principal.getName());

        try {
            serviceMetadataService.deleteService(serviceId);
            return ResponseEntity.ok(
                    ApiResponseSchema.success(null, "서비스가 성공적으로 삭제되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("서비스 삭제 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("ERR_SERVICE_004", e.getMessage()));
        } catch (Exception e) {
            log.error("서비스 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseSchema.error("ERR_SYS_001", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 서비스 코드 가용성을 확인합니다.
     * 
     * @param serviceCode 서비스 코드
     * @return 가용성 확인 결과
     */
    @GetMapping("/check-code/{serviceCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN')")
    @Operation(summary = "서비스 코드 가용성 확인", description = "서비스 코드 사용 가능 여부를 확인합니다.")
    public ResponseEntity<ApiResponseSchema<ServiceDto.CodeAvailabilityResponse>> checkServiceCodeAvailability(
            @Parameter(description = "서비스 코드") @PathVariable String serviceCode) {

        log.debug("서비스 코드 가용성 확인 API 호출: serviceCode={}", serviceCode);

        ServiceDto.CodeAvailabilityResponse response = serviceMetadataService.checkServiceCodeAvailability(serviceCode);

        return ResponseEntity.ok(
                ApiResponseSchema.success(response, "서비스 코드 가용성 확인이 완료되었습니다."));
    }

    /**
     * 서비스 상태별 통계를 조회합니다.
     * 
     * @return 상태별 통계
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN')")
    @Operation(summary = "서비스 상태별 통계", description = "서비스 상태별 통계를 조회합니다.")
    public ResponseEntity<ApiResponseSchema<ServiceDto.StatusStatisticsResponse>> getStatusStatistics() {

        log.debug("서비스 상태별 통계 조회 API 호출");

        ServiceDto.StatusStatisticsResponse response = serviceMetadataService.getStatusStatistics();
        return ResponseEntity.ok(
                ApiResponseSchema.success(response, "서비스 상태별 통계 조회가 완료되었습니다."));
    }

    /**
     * 서비스 설정을 업데이트합니다.
     * 
     * @param serviceId   서비스 ID
     * @param request     설정 업데이트 요청
     * @param principal   인증된 사용자 정보
     * @param httpRequest HTTP 요청 정보
     * @return 수정된 서비스 정보
     */
    @PatchMapping("/{serviceId}/config")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SERVICE_ADMIN')")
    @Operation(summary = "서비스 설정 업데이트", description = "서비스 설정을 업데이트합니다.")
    public ResponseEntity<ApiResponseSchema<ServiceDto.Response>> updateServiceConfig(
            @Parameter(description = "서비스 ID") @PathVariable String serviceId,
            @Valid @RequestBody ServiceDto.ConfigUpdateRequest request,
            Principal principal,
            HttpServletRequest httpRequest) {

        log.info("서비스 설정 업데이트 API 호출: serviceId={}, configKey={}, user={}",
                serviceId, request.getConfigKey(), principal.getName());

        try {
            String clientIp = getClientIp(httpRequest);
            ServiceDto.Response response = serviceMetadataService.updateServiceConfig(
                    serviceId, request, principal.getName(), clientIp);

            return ResponseEntity.ok(
                    ApiResponseSchema.success(response, "서비스 설정이 성공적으로 업데이트되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("서비스 설정 업데이트 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseSchema.error("ERR_SERVICE_005", e.getMessage()));
        } catch (Exception e) {
            log.error("서비스 설정 업데이트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseSchema.error("ERR_SYS_001", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * 
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
