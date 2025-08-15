package api.v2.cms.service.metadata.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 서비스 메타데이터 엔티티
 */
@Entity
@Table(name = "SERVICE")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @Column(name = "SERVICE_ID", length = 36, nullable = false)
    private String serviceId;

    @Column(name = "SERVICE_CODE", length = 50, nullable = false, unique = true)
    private String serviceCode;

    @Column(name = "SERVICE_NAME", length = 100, nullable = false)
    private String serviceName;

    @Column(name = "SERVICE_DOMAIN")
    private String serviceDomain;

    @Column(name = "API_BASE_URL")
    private String apiBaseUrl;

    @Column(name = "DB_CONNECTION_INFO", columnDefinition = "TEXT")
    private String dbConnectionInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private ServiceStatus status;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

    @Column(name = "CONFIG", columnDefinition = "JSON")
    private String config;

    @Column(name = "CREATED_BY", length = 36)
    private String createdBy;

    @Column(name = "CREATED_IP", length = 45)
    private String createdIp;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UPDATED_BY", length = 36)
    private String updatedBy;

    @Column(name = "UPDATED_IP", length = 45)
    private String updatedIp;

    @Builder.Default
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}