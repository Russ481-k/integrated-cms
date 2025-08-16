package api.v2.integrated_cms.service.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 서비스 메타데이터 엔티티
 * 
 * 통합 CMS에서 관리하는 각 서비스의 메타데이터를 저장합니다.
 * 서비스 코드, 이름, 도메인, API URL, 상태 등의 정보를 관리합니다.
 * 
 * @author CMS Team
 * @since v2.0
 */
@Entity
@Table(name = "service", uniqueConstraints = @UniqueConstraint(name = "unique_service_code", columnNames = "service_code"), indexes = {
        @Index(name = "idx_service_code", columnList = "service_code"),
        @Index(name = "idx_status_created", columnList = "status, created_at"),
        @Index(name = "idx_domain", columnList = "service_domain")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ServiceEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "service_id", length = 36)
    private String serviceId;

    @Column(name = "service_code", nullable = false, length = 50, unique = true)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "service_domain", length = 255)
    private String serviceDomain;

    @Column(name = "api_base_url", length = 255)
    private String apiBaseUrl;

    @Column(name = "db_connection_info", columnDefinition = "TEXT")
    private String dbConnectionInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "config", columnDefinition = "JSON")
    @Type(type = "com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType")
    private JsonNode config;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_ip", length = 45)
    private String createdIp;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "updated_ip", length = 45)
    private String updatedIp;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 서비스 정보를 업데이트합니다.
     * 
     * @param serviceName 새로운 서비스 이름
     * @param description 새로운 설명
     * @param updatedBy   수정자 ID
     * @param updatedIp   수정자 IP
     */
    public void updateServiceInfo(String serviceName, String description, String updatedBy, String updatedIp) {
        if (serviceName != null && !serviceName.trim().isEmpty()) {
            this.serviceName = serviceName.trim();
        }
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedIp = updatedIp;
    }

    /**
     * 서비스 연결 정보를 업데이트합니다.
     * 
     * @param serviceDomain 서비스 도메인
     * @param apiBaseUrl    API 기본 URL
     * @param updatedBy     수정자 ID
     * @param updatedIp     수정자 IP
     */
    public void updateConnectionInfo(String serviceDomain, String apiBaseUrl, String updatedBy, String updatedIp) {
        this.serviceDomain = serviceDomain;
        this.apiBaseUrl = apiBaseUrl;
        this.updatedBy = updatedBy;
        this.updatedIp = updatedIp;
    }

    /**
     * 서비스 상태를 변경합니다.
     * 
     * @param status    새로운 상태
     * @param updatedBy 수정자 ID
     * @param updatedIp 수정자 IP
     */
    public void changeStatus(ServiceStatus status, String updatedBy, String updatedIp) {
        this.status = status;
        this.updatedBy = updatedBy;
        this.updatedIp = updatedIp;
    }

    /**
     * 서비스 설정 정보를 업데이트합니다.
     * 
     * @param config    설정 정보 Map
     * @param updatedBy 수정자 ID
     * @param updatedIp 수정자 IP
     */
    public void updateConfig(String configKey, String configValue) {
        // 간단한 JSON 설정 업데이트 (실제로는 ObjectMapper를 통한 JSON 조작 필요)
        // 여기서는 기본 구현만 제공
    }

    /**
     * DB 연결 정보를 업데이트합니다 (암호화된 형태).
     * 
     * @param encryptedConnectionInfo 암호화된 DB 연결 정보
     * @param updatedBy               수정자 ID
     * @param updatedIp               수정자 IP
     */
    public void updateDbConnectionInfo(String encryptedConnectionInfo, String updatedBy, String updatedIp) {
        this.dbConnectionInfo = encryptedConnectionInfo;
        this.updatedBy = updatedBy;
        this.updatedIp = updatedIp;
    }

    /**
     * 서비스 코드의 유효성을 검증합니다.
     * 
     * @throws IllegalArgumentException 서비스 코드가 null이거나 빈 문자열인 경우
     */
    public void validateServiceCode() {
        if (this.serviceCode == null || this.serviceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("서비스 코드는 필수값입니다.");
        }

        // 서비스 코드 형식 검증 (영문, 숫자, -, _ 만 허용)
        if (!this.serviceCode.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("서비스 코드는 영문, 숫자, -, _ 만 가능합니다.");
        }
    }

    /**
     * 서비스 코드가 유효한지 확인합니다.
     * 
     * @return 유효하면 true, 아니면 false
     */
    public boolean isValidServiceCode() {
        try {
            validateServiceCode();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 서비스가 활성 상태인지 확인합니다.
     * 
     * @return 활성 상태이면 true, 아니면 false
     */
    public boolean isActive() {
        return this.status == ServiceStatus.ACTIVE;
    }

    /**
     * 서비스 기본 정보를 업데이트합니다.
     */
    public void updateBasicInfo(String serviceName, ServiceStatus status, String serviceDomain,
            String apiBaseUrl, String dbConnectionInfo, String description, JsonNode config) {
        this.serviceName = serviceName;
        this.status = status;
        this.serviceDomain = serviceDomain;
        this.apiBaseUrl = apiBaseUrl;
        this.dbConnectionInfo = dbConnectionInfo;
        this.description = description;
        this.config = config;
    }

    /**
     * 감사 필드를 설정합니다.
     */
    public void setAuditFields(String createdBy, String createdIp, String updatedBy, String updatedIp) {
        if (createdBy != null)
            this.createdBy = createdBy;
        if (createdIp != null)
            this.createdIp = createdIp;
        if (updatedBy != null)
            this.updatedBy = updatedBy;
        if (updatedIp != null)
            this.updatedIp = updatedIp;
    }

    /**
     * 상태를 변경합니다.
     */
    public void changeStatus(ServiceStatus status) {
        this.status = status;
    }

    /**
     * 서비스가 점검 중인지 확인합니다.
     * 
     * @return 점검 중이면 true, 아니면 false
     */
    public boolean isUnderMaintenance() {
        return this.status == ServiceStatus.MAINTENANCE;
    }

    /**
     * 서비스가 운영 가능한 상태인지 확인합니다.
     * 
     * @return 운영 가능하면 true, 아니면 false
     */
    public boolean isOperational() {
        return this.status.isOperational();
    }

    /**
     * 빌더에서 생성 시 서비스 코드 검증을 위한 후처리
     */
    @PostLoad
    @PostPersist
    @PostUpdate
    private void validateEntity() {
        // 엔티티 로드/저장 후 검증 로직 필요 시 사용
    }
}
