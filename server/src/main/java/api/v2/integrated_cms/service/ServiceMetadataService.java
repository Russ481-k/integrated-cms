package api.v2.integrated_cms.service;

import api.v2.integrated_cms.service.domain.ServiceEntity;
import api.v2.integrated_cms.service.domain.ServiceStatus;
import api.v2.integrated_cms.service.dto.ServiceDto;
import api.v2.integrated_cms.service.repository.ServiceRepository;
import api.v2.integrated_cms.service.mapper.ServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 서비스 메타데이터 관리 Service
 * 
 * 서비스 정보의 생성, 조회, 수정, 삭제 등 비즈니스 로직을 처리합니다.
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceMetadataService {

        private final ServiceRepository serviceRepository;
        private final ServiceMapper serviceMapper;

        /**
         * 새로운 서비스를 생성합니다.
         * 
         * @param request   서비스 생성 요청
         * @param createdBy 생성자 ID
         * @param createdIp 생성자 IP
         * @return 생성된 서비스 정보
         * @throws IllegalArgumentException 서비스 코드가 이미 존재하는 경우
         */
        @Transactional
        public ServiceDto.Response createService(ServiceDto.CreateRequest request, String createdBy, String createdIp) {
                log.info("서비스 생성 요청: serviceCode={}, serviceName={}, createdBy={}",
                                request.getServiceCode(), request.getServiceName(), createdBy);

                // 서비스 코드 중복 검증
                if (serviceRepository.existsByServiceCode(request.getServiceCode())) {
                        throw new IllegalArgumentException("이미 존재하는 서비스 코드입니다: " + request.getServiceCode());
                }

                // 엔티티 생성 및 저장
                ServiceEntity entity = serviceMapper.toEntity(request);
                // TODO: 실제 인증된 사용자 정보로 대체 필요
                // 현재는 외래키 제약조건을 우회하기 위해 null로 설정
                entity.setAuditFields(null, createdIp, null, null);

                ServiceEntity savedEntity = serviceRepository.save(entity);

                log.info("서비스 생성 완료: serviceId={}, serviceCode={}",
                                savedEntity.getServiceId(), savedEntity.getServiceCode());

                return serviceMapper.toResponse(savedEntity);
        }

        /**
         * 서비스 ID로 서비스 정보를 조회합니다.
         * 
         * @param serviceId 서비스 ID
         * @return 서비스 정보 (Optional)
         */
        public Optional<ServiceDto.Response> getServiceById(String serviceId) {
                log.debug("서비스 조회: serviceId={}", serviceId);

                return serviceRepository.findById(serviceId)
                                .map(serviceMapper::toResponse);
        }

        /**
         * 서비스 코드로 서비스 정보를 조회합니다.
         * 
         * @param serviceCode 서비스 코드
         * @return 서비스 정보 (Optional)
         */
        public Optional<ServiceDto.Response> getServiceByCode(String serviceCode) {
                log.debug("서비스 조회: serviceCode={}", serviceCode);

                return serviceRepository.findByServiceCode(serviceCode)
                                .map(serviceMapper::toResponse);
        }

        /**
         * 모든 서비스 목록을 조회합니다.
         * 
         * @return 서비스 요약 목록
         */
        public List<ServiceDto.SummaryResponse> getAllServices() {
                log.debug("전체 서비스 목록 조회");

                List<ServiceEntity> services = serviceRepository.findAllByCreatedAtDesc();
                return services.stream()
                                .map(serviceMapper::toSummaryResponse)
                                .collect(Collectors.toList());
        }

        /**
         * 활성 상태의 서비스 목록을 조회합니다.
         * 
         * @return 활성 서비스 목록
         */
        public List<ServiceDto.SummaryResponse> getActiveServices() {
                log.debug("활성 서비스 목록 조회");

                List<ServiceEntity> services = serviceRepository.findActiveServices();
                return services.stream()
                                .map(serviceMapper::toSummaryResponse)
                                .collect(Collectors.toList());
        }

        /**
         * 서비스 상태별 목록을 조회합니다.
         * 
         * @param status 서비스 상태
         * @return 해당 상태의 서비스 목록
         */
        public List<ServiceDto.SummaryResponse> getServicesByStatus(ServiceStatus status) {
                log.debug("서비스 상태별 조회: status={}", status);

                List<ServiceEntity> services = serviceRepository.findByStatus(status);
                return services.stream()
                                .map(serviceMapper::toSummaryResponse)
                                .collect(Collectors.toList());
        }

        /**
         * 서비스를 검색합니다.
         * 
         * @param request 검색 요청
         * @return 검색 결과 (페이징)
         */
        public Page<ServiceDto.SummaryResponse> searchServices(ServiceDto.SearchRequest request) {
                log.debug("서비스 검색: searchTerm={}, status={}, page={}, size={}",
                                request.getSearchTerm(), request.getStatus(), request.getPage(), request.getSize());

                // 정렬 설정
                Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
                Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

                Page<ServiceEntity> services;

                if (request.getSearchTerm() != null && !request.getSearchTerm().trim().isEmpty()) {
                        // 검색어가 있는 경우 통합 검색
                        services = serviceRepository.findAll(
                                        ServiceSpecifications.hasSearchTerm(request.getSearchTerm())
                                                        .and(ServiceSpecifications.hasStatus(request.getStatus())),
                                        pageable);
                } else if (request.getStatus() != null) {
                        // 상태별 검색
                        services = serviceRepository.findAll(
                                        ServiceSpecifications.hasStatus(request.getStatus()),
                                        pageable);
                } else {
                        // 전체 조회
                        services = serviceRepository.findAll(pageable);
                }

                return services.map(serviceMapper::toSummaryResponse);
        }

        /**
         * 서비스 정보를 수정합니다.
         * 
         * @param serviceId 서비스 ID
         * @param request   수정 요청
         * @param updatedBy 수정자 ID
         * @param updatedIp 수정자 IP
         * @return 수정된 서비스 정보
         * @throws IllegalArgumentException 서비스가 존재하지 않는 경우
         */
        @Transactional
        public ServiceDto.Response updateService(String serviceId, ServiceDto.UpdateRequest request,
                        String updatedBy, String updatedIp) {
                log.info("서비스 수정 요청: serviceId={}, serviceName={}, updatedBy={}",
                                serviceId, request.getServiceName(), updatedBy);

                ServiceEntity entity = serviceRepository.findById(serviceId)
                                .orElseThrow(() -> new IllegalArgumentException("서비스를 찾을 수 없습니다: " + serviceId));

                // 엔티티 업데이트
                serviceMapper.updateEntity(entity, request);
                // TODO: 실제 인증된 사용자 정보로 대체 필요
                entity.setAuditFields(null, null, null, updatedIp);

                ServiceEntity savedEntity = serviceRepository.save(entity);

                log.info("서비스 수정 완료: serviceId={}, serviceCode={}",
                                savedEntity.getServiceId(), savedEntity.getServiceCode());

                return serviceMapper.toResponse(savedEntity);
        }

        /**
         * 서비스 상태를 변경합니다.
         * 
         * @param serviceId 서비스 ID
         * @param request   상태 변경 요청
         * @param updatedBy 수정자 ID
         * @param updatedIp 수정자 IP
         * @return 수정된 서비스 정보
         * @throws IllegalArgumentException 서비스가 존재하지 않는 경우
         */
        @Transactional
        public ServiceDto.Response updateServiceStatus(String serviceId, ServiceDto.StatusUpdateRequest request,
                        String updatedBy, String updatedIp) {
                log.info("서비스 상태 변경 요청: serviceId={}, status={}, reason={}, updatedBy={}",
                                serviceId, request.getStatus(), request.getReason(), updatedBy);

                ServiceEntity entity = serviceRepository.findById(serviceId)
                                .orElseThrow(() -> new IllegalArgumentException("서비스를 찾을 수 없습니다: " + serviceId));

                // 상태 변경
                entity.changeStatus(request.getStatus());
                // TODO: 실제 인증된 사용자 정보로 대체 필요
                entity.setAuditFields(null, null, null, updatedIp);

                ServiceEntity savedEntity = serviceRepository.save(entity);

                log.info("서비스 상태 변경 완료: serviceId={}, status={}",
                                savedEntity.getServiceId(), savedEntity.getStatus());

                return serviceMapper.toResponse(savedEntity);
        }

        /**
         * 서비스를 삭제합니다.
         * 
         * @param serviceId 서비스 ID
         * @throws IllegalArgumentException 서비스가 존재하지 않는 경우
         */
        @Transactional
        public void deleteService(String serviceId) {
                log.info("서비스 삭제 요청: serviceId={}", serviceId);

                if (!serviceRepository.existsById(serviceId)) {
                        throw new IllegalArgumentException("서비스를 찾을 수 없습니다: " + serviceId);
                }

                serviceRepository.deleteById(serviceId);

                log.info("서비스 삭제 완료: serviceId={}", serviceId);
        }

        /**
         * 서비스 코드 가용성을 확인합니다.
         * 
         * @param serviceCode 서비스 코드
         * @return 가용성 확인 결과
         */
        public ServiceDto.CodeAvailabilityResponse checkServiceCodeAvailability(String serviceCode) {
                log.debug("서비스 코드 가용성 확인: serviceCode={}", serviceCode);

                boolean exists = serviceRepository.existsByServiceCode(serviceCode);

                return ServiceDto.CodeAvailabilityResponse.builder()
                                .serviceCode(serviceCode)
                                .available(!exists)
                                .message(exists ? "이미 사용 중인 서비스 코드입니다" : "사용 가능한 서비스 코드입니다")
                                .build();
        }

        /**
         * 서비스 상태별 통계를 조회합니다.
         * 
         * @return 상태별 통계
         */
        public ServiceDto.StatusStatisticsResponse getStatusStatistics() {
                log.debug("서비스 상태별 통계 조회");

                long activeCount = serviceRepository.countByStatus(ServiceStatus.ACTIVE);
                long inactiveCount = serviceRepository.countByStatus(ServiceStatus.INACTIVE);
                long maintenanceCount = serviceRepository.countByStatus(ServiceStatus.MAINTENANCE);
                long totalCount = serviceRepository.count();

                return ServiceDto.StatusStatisticsResponse.builder()
                                .activeCount(activeCount)
                                .inactiveCount(inactiveCount)
                                .maintenanceCount(maintenanceCount)
                                .totalCount(totalCount)
                                .build();
        }

        /**
         * 서비스 설정을 업데이트합니다.
         * 
         * @param serviceId 서비스 ID
         * @param request   설정 업데이트 요청
         * @param updatedBy 수정자 ID
         * @param updatedIp 수정자 IP
         * @return 수정된 서비스 정보
         * @throws IllegalArgumentException 서비스가 존재하지 않는 경우
         */
        @Transactional
        public ServiceDto.Response updateServiceConfig(String serviceId, ServiceDto.ConfigUpdateRequest request,
                        String updatedBy, String updatedIp) {
                log.info("서비스 설정 업데이트 요청: serviceId={}, configKey={}, updatedBy={}",
                                serviceId, request.getConfigKey(), updatedBy);

                ServiceEntity entity = serviceRepository.findById(serviceId)
                                .orElseThrow(() -> new IllegalArgumentException("서비스를 찾을 수 없습니다: " + serviceId));

                // JSON 설정 업데이트
                entity.updateConfig(request.getConfigKey(), request.getConfigValue());
                // TODO: 실제 인증된 사용자 정보로 대체 필요
                entity.setAuditFields(null, null, null, updatedIp);

                ServiceEntity savedEntity = serviceRepository.save(entity);

                log.info("서비스 설정 업데이트 완료: serviceId={}, configKey={}",
                                savedEntity.getServiceId(), request.getConfigKey());

                return serviceMapper.toResponse(savedEntity);
        }
}
