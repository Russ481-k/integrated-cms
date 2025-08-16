package api.v2.integrated_cms.service;

import api.v2.integrated_cms.service.domain.ServiceEntity;
import api.v2.integrated_cms.service.domain.ServiceStatus;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * 서비스 엔티티 검색을 위한 JPA Specification 유틸리티
 * 
 * @author CMS Team
 * @since v2.0
 */
public class ServiceSpecifications {

    /**
     * 검색어로 서비스 이름 또는 서비스 코드를 검색하는 Specification
     * 
     * @param searchTerm 검색어
     * @return Specification
     */
    public static Specification<ServiceEntity> hasSearchTerm(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // 항상 true를 반환하는 조건
            }

            String likePattern = "%" + searchTerm.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("serviceName")),
                            likePattern),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("serviceCode")),
                            likePattern));
        };
    }

    /**
     * 서비스 상태로 필터링하는 Specification
     * 
     * @param status 서비스 상태
     * @return Specification
     */
    public static Specification<ServiceEntity> hasStatus(ServiceStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction(); // 항상 true를 반환하는 조건
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * 서비스 도메인으로 필터링하는 Specification
     * 
     * @param serviceDomain 서비스 도메인
     * @return Specification
     */
    public static Specification<ServiceEntity> hasServiceDomain(String serviceDomain) {
        return (root, query, criteriaBuilder) -> {
            if (serviceDomain == null || serviceDomain.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("serviceDomain"), serviceDomain);
        };
    }

    /**
     * 특정 생성자로 필터링하는 Specification
     * 
     * @param createdBy 생성자 ID
     * @return Specification
     */
    public static Specification<ServiceEntity> hasCreatedBy(String createdBy) {
        return (root, query, criteriaBuilder) -> {
            if (createdBy == null || createdBy.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("createdBy"), createdBy);
        };
    }

    /**
     * 복합 검색 조건을 구성하는 Specification
     * 
     * @param searchTerm    검색어
     * @param status        서비스 상태
     * @param serviceDomain 서비스 도메인
     * @param createdBy     생성자 ID
     * @return Specification
     */
    public static Specification<ServiceEntity> buildSearchSpecification(String searchTerm,
            ServiceStatus status,
            String serviceDomain,
            String createdBy) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 검색어 조건
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String likePattern = "%" + searchTerm.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("serviceName")),
                                likePattern),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("serviceCode")),
                                likePattern));
                predicates.add(searchPredicate);
            }

            // 상태 조건
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // 서비스 도메인 조건
            if (serviceDomain != null && !serviceDomain.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("serviceDomain"), serviceDomain));
            }

            // 생성자 조건
            if (createdBy != null && !createdBy.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("createdBy"), createdBy));
            }

            // 모든 조건을 AND로 결합
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * DB 연결 정보가 있는 서비스를 찾는 Specification
     * 
     * @return Specification
     */
    public static Specification<ServiceEntity> hasDbConnection() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("dbConnectionInfo"));
    }

    /**
     * 설정 정보가 있는 서비스를 찾는 Specification
     * 
     * @return Specification
     */
    public static Specification<ServiceEntity> hasConfig() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("config"));
    }

    /**
     * 활성 상태가 아닌 서비스를 찾는 Specification
     * 
     * @return Specification
     */
    public static Specification<ServiceEntity> isNotActive() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("status"), ServiceStatus.ACTIVE);
    }

    /**
     * 특정 상태가 아닌 서비스를 찾는 Specification
     * 
     * @param excludeStatus 제외할 상태
     * @return Specification
     */
    public static Specification<ServiceEntity> statusNotEqual(ServiceStatus excludeStatus) {
        return (root, query, criteriaBuilder) -> {
            if (excludeStatus == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.notEqual(root.get("status"), excludeStatus);
        };
    }
}
