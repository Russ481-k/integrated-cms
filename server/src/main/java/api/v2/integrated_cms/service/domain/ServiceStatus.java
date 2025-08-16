package api.v2.integrated_cms.service.domain;

/**
 * 서비스 상태 열거형
 * 
 * 서비스의 운영 상태를 정의합니다.
 * 
 * @author CMS Team
 * @since v2.0
 */
public enum ServiceStatus {
    /**
     * 활성 상태 - 정상적으로 서비스 운영 중
     */
    ACTIVE("활성"),

    /**
     * 비활성 상태 - 서비스 일시 중단
     */
    INACTIVE("비활성"),

    /**
     * 점검 중 상태 - 유지보수나 업그레이드 진행 중
     */
    MAINTENANCE("점검중");

    private final String description;

    ServiceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 문자열로부터 ServiceStatus 변환
     * 
     * @param value 변환할 문자열
     * @return ServiceStatus 열거형 값
     * @throws IllegalArgumentException 유효하지 않은 값인 경우
     */
    public static ServiceStatus fromString(String value) {
        if (value == null) {
            return ACTIVE; // 기본값
        }

        try {
            return ServiceStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 서비스 상태: " + value);
        }
    }

    /**
     * 서비스가 운영 가능한 상태인지 확인
     * 
     * @return 운영 가능하면 true, 아니면 false
     */
    public boolean isOperational() {
        return this == ACTIVE;
    }

    /**
     * 서비스가 점검 중인지 확인
     * 
     * @return 점검 중이면 true, 아니면 false
     */
    public boolean isUnderMaintenance() {
        return this == MAINTENANCE;
    }
}
