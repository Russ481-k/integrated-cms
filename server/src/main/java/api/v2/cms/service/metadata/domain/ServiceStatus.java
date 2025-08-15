package api.v2.cms.service.metadata.domain;

/**
 * 서비스 상태를 나타내는 열거형
 */
public enum ServiceStatus {
    /**
     * 활성 상태 - 정상적으로 서비스 이용 가능
     */
    ACTIVE,
    
    /**
     * 비활성 상태 - 서비스 이용 불가
     */
    INACTIVE,
    
    /**
     * 점검 상태 - 일시적인 서비스 중단
     */
    MAINTENANCE
}
