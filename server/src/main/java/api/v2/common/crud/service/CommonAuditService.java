package api.v2.common.crud.service;

import api.v2.common.crud.dto.CrudContext;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 공통 감사 로그 서비스 인터페이스
 * 
 * CRUD 작업, 권한 확인, 시스템 이벤트 등의 감사 로그를 일관된 형식으로 기록
 * 보안 감사, 규정 준수, 트러블슈팅 등을 위한 포괄적인 로깅 시스템
 */
public interface CommonAuditService {

    /**
     * CRUD 작업 감사 로그 기록
     * 
     * @param context      CRUD 컨텍스트
     * @param operation    작업 타입 (CREATE, READ, UPDATE, DELETE)
     * @param resourceId   리소스 ID
     * @param oldValue     변경 전 값 (UPDATE, DELETE 시)
     * @param newValue     변경 후 값 (CREATE, UPDATE 시)
     * @param success      작업 성공 여부
     * @param errorMessage 실패 시 오류 메시지
     */
    void logCrudOperation(CrudContext context, String operation, String resourceId,
            Object oldValue, Object newValue, boolean success, String errorMessage);

    /**
     * 권한 확인 감사 로그 기록
     * 
     * @param context           CRUD 컨텍스트
     * @param operation         요청된 작업
     * @param resourceId        대상 리소스 ID
     * @param permissionGranted 권한 부여 여부
     * @param reason            권한 결정 이유
     * @param checkDurationMs   권한 확인 소요 시간
     */
    void logPermissionCheck(CrudContext context, String operation, String resourceId,
            boolean permissionGranted, String reason, Long checkDurationMs);

    /**
     * 인증 관련 감사 로그 기록
     * 
     * @param username     사용자명
     * @param action       인증 액션 (LOGIN, LOGOUT, TOKEN_REFRESH 등)
     * @param success      성공 여부
     * @param clientIp     클라이언트 IP
     * @param userAgent    User-Agent
     * @param errorMessage 실패 시 오류 메시지
     */
    void logAuthenticationEvent(String username, String action, boolean success,
            String clientIp, String userAgent, String errorMessage);

    /**
     * 시스템 이벤트 감사 로그 기록
     * 
     * @param eventType   이벤트 타입
     * @param source      이벤트 발생 소스
     * @param description 이벤트 설명
     * @param severity    심각도 (LOW, MEDIUM, HIGH, CRITICAL)
     * @param metadata    추가 메타데이터
     */
    void logSystemEvent(String eventType, String source, String description,
            String severity, Map<String, Object> metadata);

    /**
     * 데이터 접근 감사 로그 기록
     * 
     * @param context       CRUD 컨텍스트
     * @param dataType      접근한 데이터 타입
     * @param recordCount   처리된 레코드 수
     * @param queryInfo     쿼리 정보
     * @param executionTime 실행 시간 (ms)
     */
    void logDataAccess(CrudContext context, String dataType, Integer recordCount,
            String queryInfo, Long executionTime);

    /**
     * 보안 이벤트 감사 로그 기록
     * 
     * @param eventType   보안 이벤트 타입
     * @param username    관련 사용자명
     * @param clientIp    클라이언트 IP
     * @param description 이벤트 설명
     * @param severity    심각도
     * @param threatLevel 위협 수준
     */
    void logSecurityEvent(String eventType, String username, String clientIp,
            String description, String severity, String threatLevel);

    /**
     * 비즈니스 규칙 위반 감사 로그 기록
     * 
     * @param context     CRUD 컨텍스트
     * @param ruleName    위반된 규칙 이름
     * @param ruleType    규칙 타입
     * @param description 위반 내용 설명
     * @param inputData   입력 데이터
     */
    void logBusinessRuleViolation(CrudContext context, String ruleName, String ruleType,
            String description, Object inputData);

    /**
     * 성능 메트릭 감사 로그 기록
     * 
     * @param context       CRUD 컨텍스트
     * @param operationType 작업 타입
     * @param duration      실행 시간 (ms)
     * @param memoryUsage   메모리 사용량 (bytes)
     * @param dbQueries     DB 쿼리 수
     */
    void logPerformanceMetrics(CrudContext context, String operationType, Long duration,
            Long memoryUsage, Integer dbQueries);

    /**
     * 감사 로그 조회
     * 
     * @param startTime 조회 시작 시간
     * @param endTime   조회 종료 시간
     * @param eventType 이벤트 타입 필터
     * @param username  사용자명 필터
     * @param limit     조회 제한 수
     * @return 감사 로그 목록
     */
    AuditLogSearchResult searchAuditLogs(LocalDateTime startTime, LocalDateTime endTime,
            String eventType, String username, Integer limit);

    /**
     * 감사 로그 내보내기
     * 
     * @param startTime 내보내기 시작 시간
     * @param endTime   내보내기 종료 시간
     * @param format    내보내기 형식 (JSON, CSV, XML)
     * @param filters   추가 필터
     * @return 내보내기 결과
     */
    AuditLogExportResult exportAuditLogs(LocalDateTime startTime, LocalDateTime endTime,
            String format, Map<String, Object> filters);

    /**
     * 감사 로그 정리 (보존 정책에 따른)
     * 
     * @param retentionDays 보존 기간 (일)
     * @return 정리된 로그 수
     */
    Long cleanupOldAuditLogs(Integer retentionDays);

    /**
     * 감사 로그 통계 조회
     * 
     * @param startTime 통계 시작 시간
     * @param endTime   통계 종료 시간
     * @param groupBy   그룹핑 기준 (USER, EVENT_TYPE, HOUR, DAY)
     * @return 감사 로그 통계
     */
    AuditLogStatistics getAuditLogStatistics(LocalDateTime startTime, LocalDateTime endTime, String groupBy);

    /**
     * 실시간 감사 로그 스트림 시작
     * 
     * @param filters 필터 조건
     * @return 스트림 ID
     */
    String startAuditLogStream(Map<String, Object> filters);

    /**
     * 실시간 감사 로그 스트림 중지
     * 
     * @param streamId 스트림 ID
     */
    void stopAuditLogStream(String streamId);

    // == 내부 데이터 클래스들 ==

    /**
     * 감사 로그 검색 결과
     */
    interface AuditLogSearchResult {
        java.util.List<AuditLogEntry> getEntries();

        Integer getTotalCount();

        boolean hasMore();
    }

    /**
     * 감사 로그 내보내기 결과
     */
    interface AuditLogExportResult {
        String getExportId();

        String getDownloadUrl();

        String getFormat();

        Integer getRecordCount();

        LocalDateTime getGeneratedAt();
    }

    /**
     * 감사 로그 통계
     */
    interface AuditLogStatistics {
        Map<String, Long> getEventCounts();

        Map<String, Long> getUserCounts();

        Map<String, Long> getHourlyCounts();

        Map<String, Object> getAdditionalMetrics();
    }

    /**
     * 감사 로그 엔트리
     */
    interface AuditLogEntry {
        String getId();

        String getEventType();

        String getUsername();

        String getResourceType();

        String getResourceId();

        String getAction();

        boolean isSuccess();

        String getClientIp();

        LocalDateTime getTimestamp();

        Map<String, Object> getMetadata();

        String getDescription();
    }
}
