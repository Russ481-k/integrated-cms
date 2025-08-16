package api.v2.common.config;

/**
 * 요청별 서비스 컨텍스트를 관리하는 ThreadLocal 기반 홀더
 * 
 * v2 통합 CMS에서 serviceId 기반 동적 DB 라우팅을 위해 사용
 * 
 * @author CMS Team
 * @since v2.0
 */
public final class ServiceContextHolder {

    private static final ThreadLocal<String> CURRENT_SERVICE_ID = new ThreadLocal<>();

    /**
     * 현재 요청의 서비스 ID 설정
     * 
     * @param serviceId 서비스 식별자 (integrated_cms, douzone, service1 등)
     */
    public static void setCurrentServiceId(String serviceId) {
        // null은 명시적 정리로 처리
        if (serviceId == null) {
            CURRENT_SERVICE_ID.remove();
            return;
        }
        
        // 빈 문자열이나 공백만 있는 문자열도 정리로 처리
        if (serviceId.trim().isEmpty()) {
            CURRENT_SERVICE_ID.remove();
            return;
        }

        CURRENT_SERVICE_ID.set(serviceId.trim());
    }

    /**
     * 현재 요청의 서비스 ID 조회
     * 
     * @return 현재 설정된 서비스 ID, 없으면 null
     */
    public static String getCurrentServiceId() {
        return CURRENT_SERVICE_ID.get();
    }

    /**
     * 현재 스레드의 서비스 컨텍스트 정리
     * 요청 완료 후 메모리 누수 방지를 위해 호출 필수
     */
    public static void clear() {
        CURRENT_SERVICE_ID.remove();
    }

    /**
     * 현재 컨텍스트가 통합 CMS인지 확인
     * 
     * @return 통합 CMS 컨텍스트 여부
     */
    public static boolean isIntegratedCmsContext() {
        return "integrated_cms".equals(getCurrentServiceId());
    }

    /**
     * 서비스 컨텍스트 유효성 검증
     * 
     * @throws IllegalStateException 서비스 컨텍스트가 설정되지 않은 경우
     */
    public static void validateServiceContext() {
        String serviceId = getCurrentServiceId();
        if (serviceId == null || serviceId.trim().isEmpty()) {
            throw new IllegalStateException("서비스 컨텍스트가 설정되지 않았습니다.");
        }
    }
}
