package api.v2.integrated_cms.service.repository;

import api.v2.integrated_cms.service.domain.ServiceEntity;
import api.v2.integrated_cms.service.domain.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 서비스 메타데이터 Repository
 * 
 * 서비스 정보의 데이터 접근을 담당합니다.
 * JPA 쿼리 메서드와 커스텀 쿼리를 제공합니다.
 * 
 * @author CMS Team
 * @since v2.0
 */
@Repository
public interface ServiceRepository
        extends JpaRepository<ServiceEntity, String>, JpaSpecificationExecutor<ServiceEntity> {

    /**
     * 서비스 코드로 서비스를 조회합니다.
     * 
     * @param serviceCode 조회할 서비스 코드
     * @return 서비스 엔티티 (Optional)
     */
    Optional<ServiceEntity> findByServiceCode(String serviceCode);

    /**
     * 서비스 코드의 존재 여부를 확인합니다.
     * 
     * @param serviceCode 확인할 서비스 코드
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByServiceCode(String serviceCode);

    /**
     * 서비스 상태별로 서비스 목록을 조회합니다.
     * 
     * @param status 조회할 서비스 상태
     * @return 해당 상태의 서비스 목록
     */
    List<ServiceEntity> findByStatus(ServiceStatus status);

    /**
     * 서비스 이름으로 부분 검색합니다 (대소문자 무시).
     * 
     * @param serviceName 검색할 서비스 이름 패턴
     * @return 이름에 패턴이 포함된 서비스 목록
     */
    List<ServiceEntity> findByServiceNameContainingIgnoreCase(String serviceName);

    /**
     * 서비스 도메인으로 서비스를 조회합니다.
     * 
     * @param serviceDomain 조회할 서비스 도메인
     * @return 해당 도메인의 서비스 엔티티 (Optional)
     */
    Optional<ServiceEntity> findByServiceDomain(String serviceDomain);

    /**
     * 활성 상태의 서비스만 조회합니다.
     * 
     * @return 활성 상태의 서비스 목록 (생성일시 순)
     */
    @Query("SELECT s FROM ServiceEntity s WHERE s.status = 'ACTIVE' ORDER BY s.createdAt ASC")
    List<ServiceEntity> findActiveServices();

    /**
     * 서비스 이름이나 서비스 코드로 통합 검색합니다.
     * 
     * @param searchTerm 검색어
     * @return 검색 결과 서비스 목록
     */
    @Query("SELECT s FROM ServiceEntity s WHERE " +
            "LOWER(s.serviceName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.serviceCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY s.createdAt DESC")
    List<ServiceEntity> searchByNameOrCode(@Param("searchTerm") String searchTerm);

    /**
     * 특정 상태가 아닌 서비스들을 조회합니다.
     * 
     * @param status 제외할 서비스 상태
     * @return 해당 상태가 아닌 서비스 목록
     */
    List<ServiceEntity> findByStatusNot(ServiceStatus status);

    /**
     * 생성일시 기준으로 서비스 목록을 조회합니다 (최신순).
     * 
     * @return 서비스 목록 (최신 생성순)
     */
    @Query("SELECT s FROM ServiceEntity s ORDER BY s.createdAt DESC")
    List<ServiceEntity> findAllByCreatedAtDesc();

    /**
     * 점검 중인 서비스들을 조회합니다.
     * 
     * @return 점검 중 상태의 서비스 목록
     */
    @Query("SELECT s FROM ServiceEntity s WHERE s.status = 'MAINTENANCE' ORDER BY s.updatedAt DESC")
    List<ServiceEntity> findServicesUnderMaintenance();

    /**
     * 특정 사용자가 생성한 서비스들을 조회합니다.
     * 
     * @param createdBy 생성자 ID
     * @return 해당 사용자가 생성한 서비스 목록
     */
    List<ServiceEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * DB 연결 정보가 설정된 서비스들을 조회합니다.
     * 
     * @return DB 연결 정보가 있는 서비스 목록
     */
    @Query("SELECT s FROM ServiceEntity s WHERE s.dbConnectionInfo IS NOT NULL")
    List<ServiceEntity> findServicesWithDbConnection();

    /**
     * 서비스 상태별 개수를 조회합니다.
     * 
     * @param status 조회할 서비스 상태
     * @return 해당 상태의 서비스 개수
     */
    @Query("SELECT COUNT(s) FROM ServiceEntity s WHERE s.status = :status")
    long countByStatus(@Param("status") ServiceStatus status);

    /**
     * 전체 활성 서비스 개수를 조회합니다.
     * 
     * @return 활성 상태 서비스 개수
     */
    @Query("SELECT COUNT(s) FROM ServiceEntity s WHERE s.status = 'ACTIVE'")
    long countActiveServices();
}
