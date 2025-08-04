# 통합 CMS 모니터링 및 로깅 가이드

## 1. 애플리케이션 모니터링

### 1.1 Spring Boot Actuator 설정

#### 1.1.1 기본 설정

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers,env,configprops
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      show-components: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: unified-cms
      service: api-server
  health:
    redis:
      enabled: true
    diskspace:
      enabled: true
    db:
      enabled: true

logging:
  level:
    com.zaxxer.hikari: DEBUG
    org.springframework.cache: DEBUG
    org.springframework.security: INFO
    cms: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
  file:
    name: logs/unified-cms.log
    max-size: 100MB
    max-history: 30
```

#### 1.1.2 커스텀 메트릭스

```java
@Component
@RequiredArgsConstructor
public class UnifiedCmsMetrics {

    private final MeterRegistry meterRegistry;
    private final ServiceRepository serviceRepository;
    private final DynamicDataSourceManager dataSourceManager;

    private final Counter serviceRequestCounter;
    private final Timer serviceRequestTimer;
    private final Gauge activeServicesGauge;
    private final Counter dataSourceCreationCounter;

    public UnifiedCmsMetrics(MeterRegistry meterRegistry,
                           ServiceRepository serviceRepository,
                           DynamicDataSourceManager dataSourceManager) {
        this.meterRegistry = meterRegistry;
        this.serviceRepository = serviceRepository;
        this.dataSourceManager = dataSourceManager;

        // 서비스 요청 카운터
        this.serviceRequestCounter = Counter.builder("unified.service.requests")
            .description("Number of service requests")
            .register(meterRegistry);

        // 서비스 요청 처리 시간
        this.serviceRequestTimer = Timer.builder("unified.service.request.duration")
            .description("Service request processing time")
            .register(meterRegistry);

        // 활성 서비스 수
        this.activeServicesGauge = Gauge.builder("unified.services.active")
            .description("Number of active services")
            .register(meterRegistry, this, UnifiedCmsMetrics::getActiveServiceCount);

        // 데이터소스 생성 카운터
        this.dataSourceCreationCounter = Counter.builder("unified.datasource.created")
            .description("Number of data sources created")
            .register(meterRegistry);
    }

    public void incrementServiceRequest(String serviceCode, String operation) {
        serviceRequestCounter.increment(
            Tags.of(
                Tag.of("service", serviceCode),
                Tag.of("operation", operation)
            )
        );
    }

    public Timer.Sample startServiceRequestTimer(String serviceCode) {
        return Timer.start(meterRegistry)
            .tags("service", serviceCode);
    }

    public void recordServiceRequestDuration(Timer.Sample sample, String serviceCode, String status) {
        sample.stop(serviceRequestTimer.tags(
            "service", serviceCode,
            "status", status
        ));
    }

    public void incrementDataSourceCreation(String serviceCode) {
        dataSourceCreationCounter.increment(Tags.of(Tag.of("service", serviceCode)));
    }

    private Double getActiveServiceCount() {
        return (double) serviceRepository.countByStatus(ServiceStatus.ACTIVE);
    }

    @EventListener
    public void handleServiceRequest(ServiceRequestEvent event) {
        incrementServiceRequest(event.getServiceCode(), event.getOperation());
    }

    @EventListener
    public void handleDataSourceCreated(DataSourceCreatedEvent event) {
        incrementDataSourceCreation(event.getServiceCode());
    }

    // 데이터소스 연결 풀 메트릭스
    @Scheduled(fixedRate = 30000) // 30초마다
    public void recordDataSourceMetrics() {
        CacheStats stats = dataSourceManager.getCacheStats();

        Gauge.builder("unified.datasource.cache.hit.ratio")
            .description("DataSource cache hit ratio")
            .register(meterRegistry, stats, CacheStats::hitRate);

        Gauge.builder("unified.datasource.cache.size")
            .description("DataSource cache size")
            .register(meterRegistry, stats, CacheStats::requestCount);
    }
}

// 성능 모니터링 인터셉터
@Component
@RequiredArgsConstructor
@Slf4j
public class ServicePerformanceInterceptor implements HandlerInterceptor {

    private final UnifiedCmsMetrics metrics;
    private final ThreadLocal<Timer.Sample> timerSample = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) {
        String serviceCode = extractServiceCode(request.getRequestURI());
        if (serviceCode != null) {
            Timer.Sample sample = metrics.startServiceRequestTimer(serviceCode);
            timerSample.set(sample);

            // MDC에 서비스 코드 추가
            MDC.put("serviceCode", serviceCode);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler,
                              Exception ex) {
        String serviceCode = extractServiceCode(request.getRequestURI());
        Timer.Sample sample = timerSample.get();

        if (serviceCode != null && sample != null) {
            String status = ex != null ? "error" :
                          response.getStatus() >= 400 ? "error" : "success";

            metrics.recordServiceRequestDuration(sample, serviceCode, status);
            metrics.incrementServiceRequest(serviceCode, request.getMethod());
        }

        timerSample.remove();
        MDC.clear();
    }

    private String extractServiceCode(String uri) {
        // /api/service1/board -> service1
        Pattern pattern = Pattern.compile("/api/(service\\d+)/.*");
        Matcher matcher = pattern.matcher(uri);
        return matcher.matches() ? matcher.group(1) : null;
    }
}
```

### 1.2 헬스체크 구현

#### 1.2.1 커스텀 헬스 인디케이터

```java
@Component("serviceHealthIndicator")
public class ServiceHealthIndicator implements HealthIndicator {

    private final ServiceRepository serviceRepository;
    private final DynamicDataSourceManager dataSourceManager;

    public ServiceHealthIndicator(ServiceRepository serviceRepository,
                                DynamicDataSourceManager dataSourceManager) {
        this.serviceRepository = serviceRepository;
        this.dataSourceManager = dataSourceManager;
    }

    @Override
    public Health health() {
        try {
            List<Service> activeServices = serviceRepository.findByStatus(ServiceStatus.ACTIVE);
            Map<String, Object> details = new HashMap<>();

            details.put("totalActiveServices", activeServices.size());
            details.put("services", checkServicesHealth(activeServices));

            return Health.up()
                .withDetails(details)
                .build();

        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }

    private Map<String, Map<String, Object>> checkServicesHealth(List<Service> services) {
        Map<String, Map<String, Object>> serviceHealthMap = new HashMap<>();

        for (Service service : services) {
            Map<String, Object> healthInfo = new HashMap<>();

            try {
                // 데이터베이스 연결 확인
                DataSource dataSource = dataSourceManager.getDataSource(service.getServiceCode());
                try (Connection connection = dataSource.getConnection()) {
                    boolean dbHealthy = connection.isValid(5);
                    healthInfo.put("database", dbHealthy ? "UP" : "DOWN");
                } catch (Exception e) {
                    healthInfo.put("database", "DOWN");
                    healthInfo.put("databaseError", e.getMessage());
                }

                // API 엔드포인트 확인 (헬스체크 URL이 있는 경우)
                if (service.getHealthCheckUrl() != null) {
                    boolean apiHealthy = checkApiHealth(service.getHealthCheckUrl());
                    healthInfo.put("api", apiHealthy ? "UP" : "DOWN");
                }

                healthInfo.put("status", service.getStatus().toString());
                healthInfo.put("lastChecked", Instant.now().toString());

            } catch (Exception e) {
                healthInfo.put("status", "ERROR");
                healthInfo.put("error", e.getMessage());
            }

            serviceHealthMap.put(service.getServiceCode(), healthInfo);
        }

        return serviceHealthMap;
    }

    private boolean checkApiHealth(String healthCheckUrl) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getForEntity(healthCheckUrl, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

// Redis 헬스체크
@Component("redisHealthIndicator")
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            if ("PONG".equals(pong)) {
                return Health.up()
                    .withDetail("redis", "Available")
                    .withDetail("ping", pong)
                    .build();
            } else {
                return Health.down()
                    .withDetail("redis", "Ping failed")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("redis", "Connection failed")
                .withException(e)
                .build();
        }
    }
}
```

## 2. 로깅 전략

### 2.1 구조화된 로깅

#### 2.1.1 로깅 설정

```java
// 로깅 설정
@Configuration
public class LoggingConfig {

    @Bean
    public Logger structuredLogger() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // JSON 형식으로 로그 출력
        JsonEncoder jsonEncoder = new JsonEncoder();
        jsonEncoder.setContext(context);
        jsonEncoder.start();

        // 파일 어펜더 설정
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setFile("logs/unified-cms-structured.log");
        fileAppender.setEncoder(jsonEncoder);

        // 롤링 정책
        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern("logs/unified-cms-structured.%d{yyyy-MM-dd}.%i.log.gz");
        rollingPolicy.setMaxHistory(30);

        SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
        triggeringPolicy.setMaxFileSize(FileSize.valueOf("100MB"));

        rollingPolicy.setTriggeringPolicy(triggeringPolicy);
        rollingPolicy.start();

        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();

        // 루트 로거에 추가
        ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(fileAppender);

        return rootLogger;
    }
}

// 구조화된 로그 유틸리티
@Component
@Slf4j
public class StructuredLogger {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void logServiceRequest(String serviceCode, String operation,
                                String adminId, String clientIp,
                                Map<String, Object> details) {
        try {
            Map<String, Object> logData = Map.of(
                "eventType", "SERVICE_REQUEST",
                "serviceCode", serviceCode,
                "operation", operation,
                "adminId", adminId,
                "clientIp", clientIp,
                "timestamp", Instant.now().toString(),
                "details", details
            );

            log.info("SERVICE_REQUEST: {}", objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.error("Failed to write structured log", e);
        }
    }

    public void logSecurityEvent(String eventType, String adminId,
                               String clientIp, boolean success,
                               String details) {
        try {
            Map<String, Object> logData = Map.of(
                "eventType", "SECURITY_EVENT",
                "securityEventType", eventType,
                "adminId", adminId,
                "clientIp", clientIp,
                "success", success,
                "details", details,
                "timestamp", Instant.now().toString()
            );

            log.warn("SECURITY_EVENT: {}", objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.error("Failed to write security log", e);
        }
    }

    public void logPerformanceMetric(String serviceCode, String operation,
                                   long duration, boolean success) {
        try {
            Map<String, Object> logData = Map.of(
                "eventType", "PERFORMANCE_METRIC",
                "serviceCode", serviceCode,
                "operation", operation,
                "duration", duration,
                "success", success,
                "timestamp", Instant.now().toString()
            );

            log.info("PERFORMANCE_METRIC: {}", objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.error("Failed to write performance log", e);
        }
    }

    public void logDataSourceEvent(String serviceCode, String event,
                                 Map<String, Object> details) {
        try {
            Map<String, Object> logData = Map.of(
                "eventType", "DATASOURCE_EVENT",
                "serviceCode", serviceCode,
                "event", event,
                "details", details,
                "timestamp", Instant.now().toString()
            );

            log.info("DATASOURCE_EVENT: {}", objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.error("Failed to write datasource log", e);
        }
    }
}

// 로그 수집을 위한 이벤트 리스너
@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingEventListener {

    private final StructuredLogger structuredLogger;

    @EventListener
    @Async
    public void handleServiceRequest(ServiceRequestEvent event) {
        structuredLogger.logServiceRequest(
            event.getServiceCode(),
            event.getOperation(),
            event.getAdminId(),
            event.getClientIp(),
            event.getDetails()
        );
    }

    @EventListener
    @Async
    public void handleSecurityEvent(SecurityEvent event) {
        structuredLogger.logSecurityEvent(
            event.getEventType(),
            event.getAdminId(),
            event.getClientIp(),
            event.isSuccess(),
            event.getDetails()
        );
    }

    @EventListener
    @Async
    public void handleDataSourceEvent(DataSourceEvent event) {
        structuredLogger.logDataSourceEvent(
            event.getServiceCode(),
            event.getEventType(),
            Map.of(
                "connectionInfo", event.getConnectionInfo(),
                "success", event.isSuccess(),
                "errorMessage", event.getErrorMessage()
            )
        );
    }
}
```

### 2.2 보안 감사 로깅

#### 2.2.1 보안 이벤트 로깅

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditLogger {

    private final StructuredLogger structuredLogger;

    public void logLoginAttempt(String username, String clientIp, boolean success, String reason) {
        structuredLogger.logSecurityEvent(
            "LOGIN_ATTEMPT",
            username,
            clientIp,
            success,
            reason
        );
    }

    public void logPermissionDenied(String adminId, String resource, String action, String clientIp) {
        structuredLogger.logSecurityEvent(
            "PERMISSION_DENIED",
            adminId,
            clientIp,
            false,
            String.format("Access denied to %s for action %s", resource, action)
        );
    }

    public void logServiceAccess(String adminId, String serviceCode, String operation, String clientIp) {
        structuredLogger.logSecurityEvent(
            "SERVICE_ACCESS",
            adminId,
            clientIp,
            true,
            String.format("Accessed service %s for operation %s", serviceCode, operation)
        );
    }

    public void logDataModification(String adminId, String serviceCode, String entityType,
                                  String entityId, String operation, String clientIp) {
        Map<String, Object> details = Map.of(
            "serviceCode", serviceCode,
            "entityType", entityType,
            "entityId", entityId,
            "operation", operation
        );

        structuredLogger.logServiceRequest(serviceCode, operation, adminId, clientIp, details);
    }

    public void logSuspiciousActivity(String adminId, String activityType, String description, String clientIp) {
        structuredLogger.logSecurityEvent(
            "SUSPICIOUS_ACTIVITY",
            adminId,
            clientIp,
            false,
            String.format("%s: %s", activityType, description)
        );
    }
}

// 보안 이벤트 인터셉터
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditInterceptor implements HandlerInterceptor {

    private final SecurityAuditLogger securityAuditLogger;

    @Override
    public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) {
        String adminId = extractAdminId(request);
        String serviceCode = extractServiceCode(request.getRequestURI());
        String clientIp = getClientIp(request);

        if (adminId != null && serviceCode != null) {
            securityAuditLogger.logServiceAccess(
                adminId,
                serviceCode,
                request.getMethod(),
                clientIp
            );
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler,
                              Exception ex) {
        if (response.getStatus() == 403) {
            String adminId = extractAdminId(request);
            String resource = request.getRequestURI();
            String action = request.getMethod();
            String clientIp = getClientIp(request);

            securityAuditLogger.logPermissionDenied(adminId, resource, action, clientIp);
        }
    }

    private String extractAdminId(HttpServletRequest request) {
        return request.getHeader("X-Admin-ID");
    }

    private String extractServiceCode(String uri) {
        Pattern pattern = Pattern.compile("/api/(service\\d+)/.*");
        Matcher matcher = pattern.matcher(uri);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
```

## 3. Prometheus 및 Grafana 설정

### 3.1 Prometheus 설정

#### 3.1.1 Prometheus 기본 설정

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "unified-cms-rules.yml"

scrape_configs:
  - job_name: "unified-cms-api"
    static_configs:
      - targets: ["localhost:8080"]
    metrics_path: "/actuator/prometheus"
    scrape_interval: 10s

  - job_name: "unified-cms-services"
    static_configs:
      - targets: ["localhost:8081", "localhost:8082"]
    metrics_path: "/actuator/prometheus"
    scrape_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - localhost:9093

# unified-cms-rules.yml
groups:
  - name: unified-cms-alerts
    rules:
      - alert: HighServiceRequestFailureRate
        expr: rate(unified_service_requests_total{status="error"}[5m]) / rate(unified_service_requests_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High service request failure rate"
          description: "Service {{ $labels.service }} has {{ $value | humanizePercentage }} failure rate"

      - alert: DataSourceConnectionFailure
        expr: unified_datasource_created_total{success="false"} > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "DataSource connection failure"
          description: "Failed to create DataSource for service {{ $labels.service }}"

      - alert: LowCacheHitRatio
        expr: unified_datasource_cache_hit_ratio < 0.7
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Low cache hit ratio"
          description: "DataSource cache hit ratio is {{ $value | humanizePercentage }}"

      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "High JVM memory usage"
          description: "JVM heap memory usage is {{ $value | humanizePercentage }}"

      - alert: ServiceDown
        expr: up{job="unified-cms-api"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Unified CMS API is down"
          description: "The Unified CMS API service is not responding"
```

### 3.2 Grafana 대시보드

#### 3.2.1 메인 대시보드

```json
{
  "dashboard": {
    "id": null,
    "title": "Unified CMS Monitoring",
    "tags": ["unified-cms"],
    "timezone": "Asia/Seoul",
    "refresh": "5s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "panels": [
      {
        "id": 1,
        "title": "Service Request Rate",
        "type": "graph",
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 0
        },
        "targets": [
          {
            "expr": "rate(unified_service_requests_total[5m])",
            "legendFormat": "{{service}} - {{operation}}",
            "refId": "A"
          }
        ],
        "yAxes": [
          {
            "label": "Requests/sec",
            "min": 0
          }
        ],
        "xAxis": {
          "show": true
        },
        "legend": {
          "show": true,
          "values": false,
          "min": false,
          "max": false,
          "current": false,
          "total": false,
          "avg": false
        }
      },
      {
        "id": 2,
        "title": "Service Request Duration",
        "type": "graph",
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 12,
          "y": 0
        },
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(unified_service_request_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile",
            "refId": "A"
          },
          {
            "expr": "histogram_quantile(0.50, rate(unified_service_request_duration_seconds_bucket[5m]))",
            "legendFormat": "50th percentile",
            "refId": "B"
          }
        ],
        "yAxes": [
          {
            "label": "Duration (seconds)",
            "min": 0
          }
        ]
      },
      {
        "id": 3,
        "title": "Active Services",
        "type": "singlestat",
        "gridPos": {
          "h": 4,
          "w": 6,
          "x": 0,
          "y": 8
        },
        "targets": [
          {
            "expr": "unified_services_active",
            "refId": "A"
          }
        ],
        "valueName": "current",
        "format": "short",
        "thresholds": "1,5",
        "colorBackground": false,
        "colorValue": true,
        "colors": ["#d44a3a", "rgba(237, 129, 40, 0.89)", "#299c46"]
      },
      {
        "id": 4,
        "title": "DataSource Cache Hit Ratio",
        "type": "singlestat",
        "gridPos": {
          "h": 4,
          "w": 6,
          "x": 6,
          "y": 8
        },
        "targets": [
          {
            "expr": "unified_datasource_cache_hit_ratio",
            "refId": "A"
          }
        ],
        "valueName": "current",
        "format": "percentunit",
        "thresholds": "0.5,0.8",
        "colorBackground": false,
        "colorValue": true
      },
      {
        "id": 5,
        "title": "JVM Memory Usage",
        "type": "graph",
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 12,
          "y": 8
        },
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}",
            "legendFormat": "Heap Used",
            "refId": "A"
          },
          {
            "expr": "jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "Heap Max",
            "refId": "B"
          }
        ],
        "yAxes": [
          {
            "label": "Bytes",
            "min": 0
          }
        ]
      },
      {
        "id": 6,
        "title": "Database Connection Pool",
        "type": "graph",
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 16
        },
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active Connections",
            "refId": "A"
          },
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "Idle Connections",
            "refId": "B"
          },
          {
            "expr": "hikaricp_connections_max",
            "legendFormat": "Max Connections",
            "refId": "C"
          }
        ]
      },
      {
        "id": 7,
        "title": "Error Rate by Service",
        "type": "table",
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 12,
          "y": 16
        },
        "targets": [
          {
            "expr": "rate(unified_service_requests_total{status=\"error\"}[5m]) / rate(unified_service_requests_total[5m]) * 100",
            "format": "table",
            "instant": true,
            "refId": "A"
          }
        ],
        "columns": [
          {
            "text": "Service",
            "value": "service"
          },
          {
            "text": "Error Rate (%)",
            "value": "Value",
            "unit": "percent"
          }
        ]
      }
    ]
  }
}
```

#### 3.2.2 보안 모니터링 대시보드

```json
{
  "dashboard": {
    "title": "Unified CMS Security Monitoring",
    "tags": ["unified-cms", "security"],
    "panels": [
      {
        "id": 1,
        "title": "Login Attempts",
        "type": "graph",
        "targets": [
          {
            "expr": "increase(security_login_attempts_total[5m])",
            "legendFormat": "{{status}} - {{reason}}"
          }
        ]
      },
      {
        "id": 2,
        "title": "Permission Denials",
        "type": "graph",
        "targets": [
          {
            "expr": "increase(security_permission_denied_total[5m])",
            "legendFormat": "{{service}} - {{admin_id}}"
          }
        ]
      },
      {
        "id": 3,
        "title": "Top Failed Login IPs",
        "type": "table",
        "targets": [
          {
            "expr": "topk(10, increase(security_login_attempts_total{status=\"failed\"}[1h]))",
            "format": "table"
          }
        ]
      },
      {
        "id": 4,
        "title": "Suspicious Activities",
        "type": "logs",
        "targets": [
          {
            "expr": "{job=\"unified-cms-api\"} |= \"SUSPICIOUS_ACTIVITY\"",
            "refId": "A"
          }
        ]
      }
    ]
  }
}
```

## 4. 알림 및 경고 시스템

### 4.1 Alertmanager 설정

#### 4.1.1 기본 설정

```yaml
# alertmanager.yml
global:
  smtp_smarthost: "localhost:587"
  smtp_from: "alerts@company.com"
  slack_api_url: "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"

route:
  group_by: ["alertname"]
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: "web.hook"
  routes:
    - match:
        severity: critical
      receiver: "critical-alerts"
    - match:
        severity: warning
      receiver: "warning-alerts"

receivers:
  - name: "web.hook"
    webhook_configs:
      - url: "http://127.0.0.1:5001/"

  - name: "critical-alerts"
    email_configs:
      - to: "admin@company.com"
        subject: "[CRITICAL] Unified CMS Alert"
        body: |
          Alert: {{ .GroupLabels.alertname }}
          Summary: {{ range .Alerts }}{{ .Annotations.summary }}{{ end }}
          Description: {{ range .Alerts }}{{ .Annotations.description }}{{ end }}
    slack_configs:
      - channel: "#alerts-critical"
        title: "Critical Alert: {{ .GroupLabels.alertname }}"
        text: "{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}"

  - name: "warning-alerts"
    slack_configs:
      - channel: "#alerts-warning"
        title: "Warning: {{ .GroupLabels.alertname }}"
        text: "{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}"

inhibit_rules:
  - source_match:
      severity: "critical"
    target_match:
      severity: "warning"
    equal: ["alertname", "instance"]
```

### 4.2 커스텀 알림 서비스

#### 4.2.1 알림 매니저

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final NotificationSender notificationSender;
    private final AlertRepository alertRepository;
    private final AdminUserRepository adminUserRepository;

    public void sendAlert(AlertLevel level, String title, String message, String serviceCode) {
        Alert alert = Alert.builder()
            .level(level)
            .title(title)
            .message(message)
            .serviceCode(serviceCode)
            .status(AlertStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .build();

        alertRepository.save(alert);

        // 알림 발송
        sendNotification(alert);

        log.info("Alert created: {} - {}", title, message);
    }

    private void sendNotification(Alert alert) {
        List<AdminUser> recipients = getRecipientsForAlert(alert);

        for (AdminUser admin : recipients) {
            try {
                notificationSender.sendEmail(admin.getEmail(), alert.getTitle(), alert.getMessage());

                if (alert.getLevel() == AlertLevel.CRITICAL) {
                    notificationSender.sendSlack(admin.getSlackChannel(), alert.getTitle(), alert.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed to send notification to admin: {}", admin.getUsername(), e);
            }
        }
    }

    private List<AdminUser> getRecipientsForAlert(Alert alert) {
        switch (alert.getLevel()) {
            case CRITICAL:
                return adminUserRepository.findByRole(AdminRole.SUPER_ADMIN);
            case WARNING:
                return adminUserRepository.findByRoleIn(Arrays.asList(AdminRole.SUPER_ADMIN, AdminRole.SERVICE_ADMIN));
            default:
                return adminUserRepository.findByRoleIn(Arrays.asList(AdminRole.SUPER_ADMIN, AdminRole.SERVICE_ADMIN, AdminRole.OPERATOR));
        }
    }

    @EventListener
    public void handleServiceDown(ServiceDownEvent event) {
        sendAlert(
            AlertLevel.CRITICAL,
            "Service Down",
            String.format("Service %s is not responding", event.getServiceCode()),
            event.getServiceCode()
        );
    }

    @EventListener
    public void handleHighErrorRate(HighErrorRateEvent event) {
        sendAlert(
            AlertLevel.WARNING,
            "High Error Rate",
            String.format("Service %s has error rate of %.2f%%",
                event.getServiceCode(), event.getErrorRate() * 100),
            event.getServiceCode()
        );
    }

    @EventListener
    public void handleSecurityIncident(SecurityIncidentEvent event) {
        sendAlert(
            AlertLevel.CRITICAL,
            "Security Incident",
            String.format("Suspicious activity detected: %s", event.getDescription()),
            null
        );
    }
}

// 알림 발송자
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSender {

    private final JavaMailSender mailSender;
    private final SlackWebhookService slackWebhookService;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new NotificationException("Email sending failed", e);
        }
    }

    public void sendSlack(String channel, String title, String message) {
        try {
            SlackMessage slackMessage = SlackMessage.builder()
                .channel(channel)
                .text(title)
                .attachments(Arrays.asList(
                    SlackAttachment.builder()
                        .color("danger")
                        .title(title)
                        .text(message)
                        .timestamp(Instant.now().getEpochSecond())
                        .build()
                ))
                .build();

            slackWebhookService.sendMessage(slackMessage);
            log.info("Slack message sent to channel: {}", channel);
        } catch (Exception e) {
            log.error("Failed to send Slack message to channel: {}", channel, e);
            throw new NotificationException("Slack sending failed", e);
        }
    }
}
```

## 5. 로그 분석 및 검색

### 5.1 ELK Stack 설정

#### 5.1.1 Logstash 설정

```ruby
# logstash.conf
input {
  file {
    path => "/var/logs/unified-cms/unified-cms-structured.log"
    start_position => "beginning"
    codec => "json"
  }

  beats {
    port => 5044
  }
}

filter {
  if [eventType] == "SERVICE_REQUEST" {
    mutate {
      add_tag => ["service_request"]
    }

    if [details][duration] {
      mutate {
        convert => { "[details][duration]" => "integer" }
      }
    }
  }

  if [eventType] == "SECURITY_EVENT" {
    mutate {
      add_tag => ["security"]
    }

    if [success] == false {
      mutate {
        add_tag => ["security_failure"]
      }
    }
  }

  if [eventType] == "PERFORMANCE_METRIC" {
    mutate {
      add_tag => ["performance"]
    }
  }

  # GeoIP 추가 (클라이언트 IP 위치 정보)
  if [clientIp] and [clientIp] != "127.0.0.1" {
    geoip {
      source => "clientIp"
      target => "geoip"
    }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "unified-cms-logs-%{+YYYY.MM.dd}"
  }

  stdout {
    codec => rubydebug
  }
}
```

#### 5.1.2 Kibana 대시보드

```json
{
  "version": "7.10.0",
  "objects": [
    {
      "id": "unified-cms-overview",
      "type": "dashboard",
      "attributes": {
        "title": "Unified CMS Log Overview",
        "panelsJSON": "[{\"gridData\":{\"x\":0,\"y\":0,\"w\":24,\"h\":15},\"panelIndex\":\"1\",\"embeddableConfig\":{},\"panelRefName\":\"panel_1\"},{\"gridData\":{\"x\":24,\"y\":0,\"w\":24,\"h\":15},\"panelIndex\":\"2\",\"embeddableConfig\":{},\"panelRefName\":\"panel_2\"}]"
      }
    }
  ]
}
```

### 5.2 로그 검색 API

#### 5.2.1 로그 검색 서비스

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class LogSearchService {

    private final ElasticsearchRestTemplate elasticsearchTemplate;

    public Page<LogEntry> searchLogs(LogSearchRequest request, Pageable pageable) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        // 날짜 범위 필터
        if (request.getStartDate() != null && request.getEndDate() != null) {
            queryBuilder.filter(QueryBuilders.rangeQuery("timestamp")
                .gte(request.getStartDate())
                .lte(request.getEndDate()));
        }

        // 서비스 코드 필터
        if (request.getServiceCode() != null) {
            queryBuilder.filter(QueryBuilders.termQuery("serviceCode", request.getServiceCode()));
        }

        // 이벤트 타입 필터
        if (request.getEventType() != null) {
            queryBuilder.filter(QueryBuilders.termQuery("eventType", request.getEventType()));
        }

        // 관리자 ID 필터
        if (request.getAdminId() != null) {
            queryBuilder.filter(QueryBuilders.termQuery("adminId", request.getAdminId()));
        }

        // 텍스트 검색
        if (request.getSearchText() != null) {
            queryBuilder.must(QueryBuilders.multiMatchQuery(request.getSearchText())
                .field("details")
                .field("operation")
                .field("description"));
        }

        // 보안 이벤트만 필터링
        if (request.isSecurityEventsOnly()) {
            queryBuilder.filter(QueryBuilders.termQuery("eventType", "SECURITY_EVENT"));
        }

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(queryBuilder)
            .withPageable(pageable)
            .withSort(SortBuilders.fieldSort("timestamp").order(SortOrder.DESC))
            .build();

        SearchHits<LogEntry> searchHits = elasticsearchTemplate.search(searchQuery, LogEntry.class);

        List<LogEntry> logEntries = searchHits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());

        return new PageImpl<>(logEntries, pageable, searchHits.getTotalHits());
    }

    public Map<String, Long> getLogStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
            .filter(QueryBuilders.rangeQuery("timestamp")
                .gte(startDate)
                .lte(endDate));

        // 이벤트 타입별 집계
        TermsAggregationBuilder eventTypeAgg = AggregationBuilders
            .terms("eventTypes")
            .field("eventType.keyword");

        // 서비스별 집계
        TermsAggregationBuilder serviceAgg = AggregationBuilders
            .terms("services")
            .field("serviceCode.keyword");

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(queryBuilder)
            .addAggregation(eventTypeAgg)
            .addAggregation(serviceAgg)
            .withMaxResults(0) // 집계만 필요
            .build();

        SearchHits<LogEntry> searchHits = elasticsearchTemplate.search(searchQuery, LogEntry.class);

        Map<String, Long> statistics = new HashMap<>();

        // 결과 파싱
        if (searchHits.getAggregations() != null) {
            Terms eventTypes = searchHits.getAggregations().get("eventTypes");
            eventTypes.getBuckets().forEach(bucket ->
                statistics.put("eventType_" + bucket.getKeyAsString(), bucket.getDocCount()));

            Terms services = searchHits.getAggregations().get("services");
            services.getBuckets().forEach(bucket ->
                statistics.put("service_" + bucket.getKeyAsString(), bucket.getDocCount()));
        }

        return statistics;
    }
}

// 로그 검색 컨트롤러
@RestController
@RequestMapping("/api/unified/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class LogSearchController {

    private final LogSearchService logSearchService;

    @GetMapping("/search")
    @Operation(summary = "로그 검색")
    public ResponseEntity<ApiResponse<Page<LogEntry>>> searchLogs(
            @ModelAttribute LogSearchRequest request,
            Pageable pageable) {

        Page<LogEntry> logs = logSearchService.searchLogs(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/statistics")
    @Operation(summary = "로그 통계")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLogStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        Map<String, Long> statistics = logSearchService.getLogStatistics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
```

---

이렇게 통합 CMS의 모니터링, 로깅, 알림 시스템에 대한 종합적인 가이드를 작성했습니다. 이 가이드는 다음과 같은 핵심 기능들을 포함합니다:

1. **종합적인 모니터링**: 애플리케이션 성능, 서비스 상태, 시스템 리소스 모니터링
2. **구조화된 로깅**: JSON 형태의 구조화된 로그와 보안 감사 로깅
3. **실시간 알림**: 임계 상황 발생 시 즉시 알림 발송
4. **시각화 대시보드**: Grafana를 통한 실시간 메트릭 시각화
5. **로그 분석**: ELK Stack을 활용한 로그 검색 및 분석

이제 배포 및 운영 가이드를 추가로 작성할까요?
