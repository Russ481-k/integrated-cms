package api.v2.common.crud.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.common.crud.dto.CrudContext;
import api.v2.common.crud.service.CommonAuditService;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 공통 감사 로그 서비스 구현체
 * 
 * 현재는 로깅 기반 구현이며, 향후 데이터베이스 또는 전용 로그 시스템으로 확장 가능
 * 비동기 로깅으로 성능 영향 최소화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonAuditServiceImpl implements CommonAuditService {

    private static final String AUDIT_LOG_PREFIX = "AUDIT";

    @Override
    public void logCrudOperation(CrudContext context, String operation, String resourceId,
            Object oldValue, Object newValue, boolean success, String errorMessage) {

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = new HashMap<>();
                auditData.put("eventType", "CRUD_OPERATION");
                auditData.put("operation", operation);
                auditData.put("resourceType", context.getResourceName());
                auditData.put("resourceId", resourceId);
                auditData.put("serviceContext", context.getServiceId());
                auditData.put("username", context.getUsername());
                auditData.put("userRole", context.getUserRole());
                auditData.put("clientIp", context.getClientIp());
                auditData.put("userAgent", context.getUserAgent());
                auditData.put("success", success);
                auditData.put("timestamp", LocalDateTime.now());

                if (!success && errorMessage != null) {
                    auditData.put("errorMessage", errorMessage);
                }

                if (oldValue != null) {
                    auditData.put("oldValue", sanitizeValue(oldValue));
                }

                if (newValue != null) {
                    auditData.put("newValue", sanitizeValue(newValue));
                }

                log.info("{}_CRUD: {}", AUDIT_LOG_PREFIX, formatAuditLog(auditData));

            } catch (Exception e) {
                log.error("Failed to log CRUD operation audit: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public void logPermissionCheck(CrudContext context, String operation, String resourceId,
            boolean permissionGranted, String reason, Long checkDurationMs) {

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = new HashMap<>();
                auditData.put("eventType", "PERMISSION_CHECK");
                auditData.put("operation", operation);
                auditData.put("resourceType", context.getResourceName());
                auditData.put("resourceId", resourceId);
                auditData.put("serviceContext", context.getServiceId());
                auditData.put("username", context.getUsername());
                auditData.put("userRole", context.getUserRole());
                auditData.put("clientIp", context.getClientIp());
                auditData.put("permissionGranted", permissionGranted);
                auditData.put("reason", reason);
                auditData.put("checkDurationMs", checkDurationMs);
                auditData.put("timestamp", LocalDateTime.now());

                String severity = permissionGranted ? "INFO" : "WARN";
                log.info("{}_PERMISSION[{}]: {}", AUDIT_LOG_PREFIX, severity, formatAuditLog(auditData));

            } catch (Exception e) {
                log.error("Failed to log permission check audit: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public void logAuthenticationEvent(String username, String action, boolean success,
            String clientIp, String userAgent, String errorMessage) {

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = new HashMap<>();
                auditData.put("eventType", "AUTHENTICATION");
                auditData.put("action", action);
                auditData.put("username", username);
                auditData.put("clientIp", clientIp);
                auditData.put("userAgent", userAgent);
                auditData.put("success", success);
                auditData.put("timestamp", LocalDateTime.now());

                if (!success && errorMessage != null) {
                    auditData.put("errorMessage", errorMessage);
                }

                String severity = success ? "INFO" : "WARN";
                log.info("{}_AUTH[{}]: {}", AUDIT_LOG_PREFIX, severity, formatAuditLog(auditData));

            } catch (Exception e) {
                log.error("Failed to log authentication event audit: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public void logSystemEvent(String eventType, String source, String description,
            String severity, Map<String, Object> metadata) {

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = new HashMap<>();
                auditData.put("eventType", "SYSTEM_EVENT");
                auditData.put("systemEventType", eventType);
                auditData.put("source", source);
                auditData.put("description", description);
                auditData.put("severity", severity);
                auditData.put("timestamp", LocalDateTime.now());

                if (metadata != null) {
                    auditData.put("metadata", metadata);
                }

                log.info("{}_SYSTEM[{}]: {}", AUDIT_LOG_PREFIX, severity, formatAuditLog(auditData));

            } catch (Exception e) {
                log.error("Failed to log system event audit: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public void logDataAccess(CrudContext context, String dataType, Integer recordCount,
            String queryInfo, Long executionTime) {

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = new HashMap<>();
                auditData.put("eventType", "DATA_ACCESS");
                auditData.put("dataType", dataType);
                auditData.put("recordCount", recordCount);
                auditData.put("queryInfo", queryInfo);
                auditData.put("executionTime", executionTime);
                auditData.put("serviceContext", context.getServiceId());
                auditData.put("username", context.getUsername());
                auditData.put("userRole", context.getUserRole());
                auditData.put("clientIp", context.getClientIp());
                auditData.put("timestamp", LocalDateTime.now());

                log.info("{}_DATA_ACCESS: {}", AUDIT_LOG_PREFIX, formatAuditLog(auditData));

            } catch (Exception e) {
                log.error("Failed to log data access audit: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public void logSecurityEvent(String eventType, String username, String clientIp,
            String description, String severity, String threatLevel) {

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = new HashMap<>();
                auditData.put("eventType", "SECURITY_EVENT");
                auditData.put("securityEventType", eventType);
                auditData.put("username", username);
                auditData.put("clientIp", clientIp);
                auditData.put("description", description);
                auditData.put("severity", severity);
                auditData.put("threatLevel", threatLevel);
                auditData.put("timestamp", LocalDateTime.now());

                log.warn("{}_SECURITY[{}]: {}", AUDIT_LOG_PREFIX, severity, formatAuditLog(auditData));

            } catch (Exception e) {
                log.error("Failed to log security event audit: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public void logBusinessRuleViolation(CrudContext context, String ruleName, String ruleType,
            String description, Object inputData) {

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = new HashMap<>();
                auditData.put("eventType", "BUSINESS_RULE_VIOLATION");
                auditData.put("ruleName", ruleName);
                auditData.put("ruleType", ruleType);
                auditData.put("description", description);
                auditData.put("inputData", sanitizeValue(inputData));
                auditData.put("serviceContext", context.getServiceId());
                auditData.put("username", context.getUsername());
                auditData.put("userRole", context.getUserRole());
                auditData.put("clientIp", context.getClientIp());
                auditData.put("timestamp", LocalDateTime.now());

                log.warn("{}_BUSINESS_RULE: {}", AUDIT_LOG_PREFIX, formatAuditLog(auditData));

            } catch (Exception e) {
                log.error("Failed to log business rule violation audit: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public void logPerformanceMetrics(CrudContext context, String operationType, Long duration,
            Long memoryUsage, Integer dbQueries) {

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> auditData = new HashMap<>();
                auditData.put("eventType", "PERFORMANCE_METRICS");
                auditData.put("operationType", operationType);
                auditData.put("duration", duration);
                auditData.put("memoryUsage", memoryUsage);
                auditData.put("dbQueries", dbQueries);
                auditData.put("serviceContext", context.getServiceId());
                auditData.put("username", context.getUsername());
                auditData.put("resourceType", context.getResourceName());
                auditData.put("timestamp", LocalDateTime.now());

                log.info("{}_PERFORMANCE: {}", AUDIT_LOG_PREFIX, formatAuditLog(auditData));

            } catch (Exception e) {
                log.error("Failed to log performance metrics audit: {}", e.getMessage(), e);
            }
        });
    }

    // == 조회 및 분석 메서드들 (향후 구현) ==

    @Override
    public AuditLogSearchResult searchAuditLogs(LocalDateTime startTime, LocalDateTime endTime,
            String eventType, String username, Integer limit) {
        // TODO: 향후 데이터베이스 또는 로그 검색 시스템으로 구현
        log.info("Audit log search requested: {} to {}, eventType: {}, username: {}",
                startTime, endTime, eventType, username);

        return new SimpleAuditLogSearchResult(java.util.Arrays.asList(), 0, false);
    }

    @Override
    public AuditLogExportResult exportAuditLogs(LocalDateTime startTime, LocalDateTime endTime,
            String format, Map<String, Object> filters) {
        // TODO: 향후 구현
        log.info("Audit log export requested: {} to {}, format: {}", startTime, endTime, format);

        return new SimpleAuditLogExportResult("export-" + System.currentTimeMillis(),
                "/download/audit-export", format, 0, LocalDateTime.now());
    }

    @Override
    public Long cleanupOldAuditLogs(Integer retentionDays) {
        // TODO: 향후 구현
        log.info("Audit log cleanup requested: retention {} days", retentionDays);
        return 0L;
    }

    @Override
    public AuditLogStatistics getAuditLogStatistics(LocalDateTime startTime, LocalDateTime endTime, String groupBy) {
        // TODO: 향후 구현
        log.info("Audit log statistics requested: {} to {}, groupBy: {}", startTime, endTime, groupBy);

        return new SimpleAuditLogStatistics(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    @Override
    public String startAuditLogStream(Map<String, Object> filters) {
        // TODO: 향후 구현
        String streamId = "stream-" + System.currentTimeMillis();
        log.info("Audit log stream started: {}", streamId);
        return streamId;
    }

    @Override
    public void stopAuditLogStream(String streamId) {
        // TODO: 향후 구현
        log.info("Audit log stream stopped: {}", streamId);
    }

    // == 유틸리티 메서드들 ==

    /**
     * 민감한 정보 제거/마스킹
     */
    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }

        String valueStr = value.toString();

        // 비밀번호 등 민감한 정보 마스킹
        if (valueStr.toLowerCase().contains("password") ||
                valueStr.toLowerCase().contains("secret") ||
                valueStr.toLowerCase().contains("token")) {
            return "***MASKED***";
        }

        // 긴 문자열 잘라내기 (로그 크기 제한)
        if (valueStr.length() > 1000) {
            return valueStr.substring(0, 1000) + "...[TRUNCATED]";
        }

        return value;
    }

    /**
     * 감사 로그 포맷팅
     */
    private String formatAuditLog(Map<String, Object> auditData) {
        StringBuilder sb = new StringBuilder();

        // 주요 필드들을 먼저 출력
        appendIfPresent(sb, "eventType", auditData);
        appendIfPresent(sb, "username", auditData);
        appendIfPresent(sb, "operation", auditData);
        appendIfPresent(sb, "resourceType", auditData);
        appendIfPresent(sb, "resourceId", auditData);
        appendIfPresent(sb, "success", auditData);
        appendIfPresent(sb, "clientIp", auditData);

        // 나머지 필드들 출력
        auditData.entrySet().stream()
                .filter(entry -> !isMainField(entry.getKey()))
                .forEach(entry -> appendIfPresent(sb, entry.getKey(), auditData));

        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String key, Map<String, Object> data) {
        Object value = data.get(key);
        if (value != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(key).append("=").append(value);
        }
    }

    private boolean isMainField(String key) {
        return java.util.Arrays.asList("eventType", "username", "operation", "resourceType",
                "resourceId", "success", "clientIp").contains(key);
    }

    // == 간단한 구현체들 ==

    private static class SimpleAuditLogSearchResult implements AuditLogSearchResult {
        private final List<AuditLogEntry> entries;
        private final Integer totalCount;
        private final boolean hasMore;

        public SimpleAuditLogSearchResult(List<AuditLogEntry> entries, Integer totalCount, boolean hasMore) {
            this.entries = entries;
            this.totalCount = totalCount;
            this.hasMore = hasMore;
        }

        @Override
        public List<AuditLogEntry> getEntries() {
            return entries;
        }

        @Override
        public Integer getTotalCount() {
            return totalCount;
        }

        @Override
        public boolean hasMore() {
            return hasMore;
        }
    }

    private static class SimpleAuditLogExportResult implements AuditLogExportResult {
        private final String exportId;
        private final String downloadUrl;
        private final String format;
        private final Integer recordCount;
        private final LocalDateTime generatedAt;

        public SimpleAuditLogExportResult(String exportId, String downloadUrl, String format,
                Integer recordCount, LocalDateTime generatedAt) {
            this.exportId = exportId;
            this.downloadUrl = downloadUrl;
            this.format = format;
            this.recordCount = recordCount;
            this.generatedAt = generatedAt;
        }

        @Override
        public String getExportId() {
            return exportId;
        }

        @Override
        public String getDownloadUrl() {
            return downloadUrl;
        }

        @Override
        public String getFormat() {
            return format;
        }

        @Override
        public Integer getRecordCount() {
            return recordCount;
        }

        @Override
        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }
    }

    private static class SimpleAuditLogStatistics implements AuditLogStatistics {
        private final Map<String, Long> eventCounts;
        private final Map<String, Long> userCounts;
        private final Map<String, Long> hourlyCounts;
        private final Map<String, Object> additionalMetrics;

        public SimpleAuditLogStatistics(Map<String, Long> eventCounts, Map<String, Long> userCounts,
                Map<String, Long> hourlyCounts, Map<String, Object> additionalMetrics) {
            this.eventCounts = eventCounts;
            this.userCounts = userCounts;
            this.hourlyCounts = hourlyCounts;
            this.additionalMetrics = additionalMetrics;
        }

        @Override
        public Map<String, Long> getEventCounts() {
            return eventCounts;
        }

        @Override
        public Map<String, Long> getUserCounts() {
            return userCounts;
        }

        @Override
        public Map<String, Long> getHourlyCounts() {
            return hourlyCounts;
        }

        @Override
        public Map<String, Object> getAdditionalMetrics() {
            return additionalMetrics;
        }
    }
}
