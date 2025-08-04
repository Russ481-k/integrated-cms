# 통합 CMS 고도화 구현 가이드

## 1. 구현 준비사항

### 1.1 기술 스택 정의

#### 1.1.1 백엔드 스택

```yaml
Core Framework: Spring Boot 2.7+
Gateway: Spring Cloud Gateway 2023.0.0
Security: Spring Security 5.8+
Database: MySQL 8.0+ / PostgreSQL 14+
ORM: JPA/Hibernate 5.6+
Caching: Redis 7.0+
Message Queue: RabbitMQ 3.11+ (선택사항)
```

#### 1.1.2 프론트엔드 스택

```yaml
Framework: Next.js 14+ (App Router)
UI Library: Chakra UI 2.8+
State Management: Zustand / React Query
Form Management: React Hook Form
Charts: Recharts / Chart.js
Authentication: NextAuth.js
```

#### 1.1.3 인프라 스택

```yaml
Containerization: Docker 20.10+
Orchestration: Docker Compose / Kubernetes
Reverse Proxy: Nginx 1.20+
Monitoring: Prometheus + Grafana
Logging: ELK Stack / Loki
CI/CD: GitHub Actions / Jenkins
```

### 1.2 개발 환경 설정

#### 1.2.1 Docker 개발 환경

```yaml
# docker-compose.dev.yml
version: "3.8"
services:
  unified-api:
    build:
      context: ./server
      dockerfile: Dockerfile.dev
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_HOST=unified-db
    volumes:
      - ./server:/app
      - ~/.m2:/root/.m2
    depends_on:
      - unified-db
      - redis

  service1-api:
    build:
      context: ./server
      dockerfile: Dockerfile.service
    ports:
      - "8081:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=service1
      - DB_HOST=service1-db
    depends_on:
      - service1-db

  unified-db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: unified_cms
    ports:
      - "3306:3306"
    volumes:
      - unified_db_data:/var/lib/mysql

  service1-db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: service1_cms
    ports:
      - "3307:3306"
    volumes:
      - service1_db_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/dev.conf:/etc/nginx/nginx.conf
    depends_on:
      - unified-api
      - service1-api

volumes:
  unified_db_data:
  service1_db_data:
```

---

## 2. 단계별 구현 가이드

### 2.1 Phase 1: 기반 구조 구축

#### 2.1.1 통합 메타 데이터베이스 구축

```sql
-- 1단계: 통합 DB 스키마 생성
CREATE DATABASE unified_cms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2단계: 기본 테이블 생성
USE unified_cms;

-- 서비스 정보 테이블
CREATE TABLE services (
    service_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_code VARCHAR(50) UNIQUE NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    service_domain VARCHAR(255),
    api_base_url VARCHAR(255),
    db_connection_info TEXT, -- AES 암호화된 JSON
    status ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE') DEFAULT 'ACTIVE',
    health_check_url VARCHAR(255),
    last_health_check TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

-- 관리자 테이블
CREATE TABLE admin_users (
    admin_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    full_name VARCHAR(100),
    role ENUM('SUPER_ADMIN', 'SERVICE_ADMIN', 'OPERATOR') DEFAULT 'OPERATOR',
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED') DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP NULL,
    password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 관리자-서비스 권한 매핑
CREATE TABLE admin_service_permissions (
    permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    permissions JSON NOT NULL, -- {"board": ["read", "write"], "content": ["read"]}
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,
    expires_at TIMESTAMP NULL,
    FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES services(service_id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES admin_users(admin_id),
    UNIQUE KEY unique_admin_service (admin_id, service_id)
);

-- 활동 로그 테이블
CREATE TABLE unified_activity_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT,
    service_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    details JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id),
    FOREIGN KEY (service_id) REFERENCES services(service_id),
    INDEX idx_admin_created (admin_id, created_at),
    INDEX idx_service_created (service_id, created_at),
    INDEX idx_action_created (action, created_at)
);

-- 초기 데이터 삽입
INSERT INTO admin_users (username, password, email, full_name, role) VALUES
('superadmin', '$2a$10$encrypted_password', 'admin@company.com', 'Super Administrator', 'SUPER_ADMIN');

-- 기존 서비스 등록 (예시)
INSERT INTO services (service_code, service_name, service_domain, api_base_url, db_connection_info) VALUES
('cms1', 'Main CMS', 'cms1.company.com', 'http://localhost:8081',
 AES_ENCRYPT('{"host":"localhost","port":"3307","database":"service1_cms","username":"cms1_user","password":"secure_pass"}', 'encryption_key'));
```

#### 2.1.2 암호화 서비스 구현

```java
@Service
public class EncryptionService {

    @Value("${app.encryption.key}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    public String encrypt(String plainText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(), KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // IV 생성
            byte[] iv = new byte[16];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호화된 데이터를 Base64로 인코딩
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // IV 추출
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, 16);
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

            SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(), KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

#### 2.1.3 동적 데이터소스 관리자

```java
@Component
@Slf4j
public class DynamicDataSourceManager {

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    private final ServiceRepository serviceRepository;
    private final EncryptionService encryptionService;

    @Autowired
    public DynamicDataSourceManager(ServiceRepository serviceRepository,
                                   EncryptionService encryptionService) {
        this.serviceRepository = serviceRepository;
        this.encryptionService = encryptionService;
    }

    public DataSource getDataSource(String serviceCode) {
        return dataSourceCache.computeIfAbsent(serviceCode, this::createDataSource);
    }

    private DataSource createDataSource(String serviceCode) {
        Service service = serviceRepository.findByServiceCode(serviceCode)
            .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceCode));

        if (!ServiceStatus.ACTIVE.equals(service.getStatus())) {
            throw new ServiceUnavailableException("Service is not active: " + serviceCode);
        }

        try {
            String decryptedConnectionInfo = encryptionService.decrypt(service.getDbConnectionInfo());
            DatabaseConnectionInfo dbInfo = JsonUtils.fromJson(decryptedConnectionInfo, DatabaseConnectionInfo.class);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
                dbInfo.getHost(), dbInfo.getPort(), dbInfo.getDatabase()));
            config.setUsername(dbInfo.getUsername());
            config.setPassword(dbInfo.getPassword());

            // 연결 풀 설정
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);

            // 연결 검증
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);

            HikariDataSource dataSource = new HikariDataSource(config);

            log.info("Created DataSource for service: {}", serviceCode);
            return dataSource;

        } catch (Exception e) {
            log.error("Failed to create DataSource for service: {}", serviceCode, e);
            throw new DataSourceCreationException("Failed to create DataSource", e);
        }
    }

    public void evictDataSource(String serviceCode) {
        DataSource dataSource = dataSourceCache.remove(serviceCode);
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            log.info("Evicted DataSource for service: {}", serviceCode);
        }
    }

    @PreDestroy
    public void cleanup() {
        dataSourceCache.values().forEach(dataSource -> {
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
            }
        });
        dataSourceCache.clear();
        log.info("Cleaned up all DataSources");
    }
}

// 데이터소스 컨텍스트 관리
@Component
public class DataSourceContextHolder {

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    public static void setServiceCode(String serviceCode) {
        contextHolder.set(serviceCode);
    }

    public static String getServiceCode() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }
}

// Try-with-resources 지원
public class DataSourceContext implements AutoCloseable {

    private final String previousServiceCode;

    public DataSourceContext(String serviceCode) {
        this.previousServiceCode = DataSourceContextHolder.getServiceCode();
        DataSourceContextHolder.setServiceCode(serviceCode);
    }

    @Override
    public void close() {
        if (previousServiceCode != null) {
            DataSourceContextHolder.setServiceCode(previousServiceCode);
        } else {
            DataSourceContextHolder.clear();
        }
    }
}
```

### 2.2 Phase 2: API Gateway 구현

#### 2.2.1 Spring Cloud Gateway 설정

```java
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                          ServiceRepository serviceRepository) {
        return builder.routes()
            // 통합 API 라우팅
            .route("unified-api", r -> r
                .path("/api/unified/**")
                .uri("http://localhost:8080"))

            // 동적 서비스 라우팅
            .route("dynamic-services", r -> r
                .path("/api/service*/**")
                .filters(f -> f
                    .filter(new ServiceRoutingFilter(serviceRepository))
                    .filter(new ServiceAuthenticationFilter())
                    .filter(new ServiceLoggingFilter()))
                .uri("no://op")) // 동적으로 결정됨

            .build();
    }

    @Bean
    public GlobalFilter serviceDiscoveryFilter(ServiceRepository serviceRepository) {
        return new ServiceDiscoveryFilter(serviceRepository);
    }
}

// 서비스 라우팅 필터
@Component
@Slf4j
public class ServiceRoutingFilter implements GatewayFilter {

    private final ServiceRepository serviceRepository;
    private final LoadingCache<String, Service> serviceCache;

    public ServiceRoutingFilter(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
        this.serviceCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(this::loadService);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // /api/service1/board -> service1 추출
        String serviceCode = extractServiceCode(path);
        if (serviceCode == null) {
            return handleError(exchange, "Invalid service path", HttpStatus.BAD_REQUEST);
        }

        try {
            Service service = serviceCache.get(serviceCode);
            if (service == null || !ServiceStatus.ACTIVE.equals(service.getStatus())) {
                return handleError(exchange, "Service not available", HttpStatus.SERVICE_UNAVAILABLE);
            }

            // 요청 경로 변환: /api/service1/board -> /cms/board
            String newPath = transformPath(path, serviceCode);
            ServerHttpRequest request = exchange.getRequest().mutate()
                .path(newPath)
                .header("X-Service-Code", serviceCode)
                .build();

            // 서비스 URL로 라우팅
            URI serviceUri = URI.create(service.getApiBaseUrl());
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, serviceUri);

            return chain.filter(exchange.mutate().request(request).build());

        } catch (Exception e) {
            log.error("Service routing failed for: {}", serviceCode, e);
            return handleError(exchange, "Service routing failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractServiceCode(String path) {
        // /api/service1/board -> service1
        Pattern pattern = Pattern.compile("/api/(service\\d+)/.*");
        Matcher matcher = pattern.matcher(path);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private String transformPath(String originalPath, String serviceCode) {
        // /api/service1/board -> /cms/board
        return originalPath.replaceFirst("/api/" + serviceCode, "/cms");
    }

    private Service loadService(String serviceCode) {
        return serviceRepository.findByServiceCode(serviceCode).orElse(null);
    }

    private Mono<Void> handleError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String errorBody = String.format("{\"error\":\"%s\",\"timestamp\":\"%s\"}",
            message, Instant.now().toString());
        DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes());

        return response.writeWith(Mono.just(buffer));
    }
}
```

#### 2.2.2 인증/인가 필터

```java
@Component
@Slf4j
public class ServiceAuthenticationFilter implements GatewayFilter {

    private final JwtUtil jwtUtil;
    private final AdminServicePermissionService permissionService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());

        if (token == null) {
            return handleUnauthorized(exchange, "Missing authentication token");
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            String adminId = claims.getSubject();
            String serviceCode = exchange.getRequest().getHeaders().getFirst("X-Service-Code");
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            // 권한 검증
            if (!hasPermission(adminId, serviceCode, path, method)) {
                return handleForbidden(exchange, "Insufficient permissions");
            }

            // 사용자 정보를 헤더에 추가
            ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Admin-ID", adminId)
                .header("X-Admin-Role", claims.get("role", String.class))
                .build();

            return chain.filter(exchange.mutate().request(request).build());

        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return handleUnauthorized(exchange, "Invalid authentication token");
        }
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean hasPermission(String adminId, String serviceCode, String path, String method) {
        // 권한 체크 로직
        return permissionService.hasPermission(adminId, serviceCode, path, method);
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        return handleError(exchange, message, HttpStatus.UNAUTHORIZED);
    }

    private Mono<Void> handleForbidden(ServerWebExchange exchange, String message) {
        return handleError(exchange, message, HttpStatus.FORBIDDEN);
    }

    private Mono<Void> handleError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String errorBody = String.format("{\"error\":\"%s\",\"timestamp\":\"%s\"}",
            message, Instant.now().toString());
        DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes());

        return response.writeWith(Mono.just(buffer));
    }
}
```

### 2.3 Phase 3: 통합 API 구현

#### 2.3.1 통합 서비스 관리 API

```java
@RestController
@RequestMapping("/api/unified/services")
@RequiredArgsConstructor
@Validated
@Tag(name = "Service Management", description = "서비스 관리 API")
public class ServiceManagementController {

    private final ServiceManagementService serviceManagementService;
    private final ActivityLoggingService activityLoggingService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "서비스 목록 조회", description = "등록된 모든 서비스 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<ServiceDto>>> getAllServices() {
        List<ServiceDto> services = serviceManagementService.getAllServices();
        return ResponseEntity.ok(ApiResponse.success(services));
    }

    @GetMapping("/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @servicePermissionEvaluator.hasAccess(authentication.name, #serviceId)")
    @Operation(summary = "서비스 상세 조회")
    public ResponseEntity<ApiResponse<ServiceDetailDto>> getService(@PathVariable Long serviceId) {
        ServiceDetailDto service = serviceManagementService.getServiceDetail(serviceId);
        return ResponseEntity.ok(ApiResponse.success(service));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "새 서비스 등록")
    public ResponseEntity<ApiResponse<ServiceDto>> createService(
            @Valid @RequestBody CreateServiceRequest request,
            Authentication auth) {

        ServiceDto createdService = serviceManagementService.createService(request, auth.getName());

        // 활동 로그 기록
        activityLoggingService.logActivity(
            auth.getName(),
            null,
            "SERVICE_CREATED",
            "SERVICE",
            createdService.getServiceId(),
            Map.of("serviceName", createdService.getServiceName()),
            getClientIp()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(createdService));
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "서비스 정보 수정")
    public ResponseEntity<ApiResponse<ServiceDto>> updateService(
            @PathVariable Long serviceId,
            @Valid @RequestBody UpdateServiceRequest request,
            Authentication auth) {

        ServiceDto updatedService = serviceManagementService.updateService(serviceId, request, auth.getName());

        activityLoggingService.logActivity(
            auth.getName(),
            serviceId,
            "SERVICE_UPDATED",
            "SERVICE",
            serviceId,
            request.toMap(),
            getClientIp()
        );

        return ResponseEntity.ok(ApiResponse.success(updatedService));
    }

    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "서비스 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable Long serviceId,
            Authentication auth) {

        serviceManagementService.deleteService(serviceId);

        activityLoggingService.logActivity(
            auth.getName(),
            serviceId,
            "SERVICE_DELETED",
            "SERVICE",
            serviceId,
            Map.of(),
            getClientIp()
        );

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{serviceId}/test-connection")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "서비스 연결 테스트")
    public ResponseEntity<ApiResponse<ConnectionTestResult>> testConnection(@PathVariable Long serviceId) {
        ConnectionTestResult result = serviceManagementService.testConnection(serviceId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{serviceId}/health-check")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "서비스 헬스체크")
    public ResponseEntity<ApiResponse<HealthCheckResult>> performHealthCheck(@PathVariable Long serviceId) {
        HealthCheckResult result = serviceManagementService.performHealthCheck(serviceId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private String getClientIp() {
        // 실제 구현에서는 HttpServletRequest에서 IP 추출
        return "127.0.0.1";
    }
}

// 서비스 관리 서비스 구현
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final EncryptionService encryptionService;
    private final DynamicDataSourceManager dataSourceManager;
    private final RestTemplate restTemplate;

    public List<ServiceDto> getAllServices() {
        return serviceRepository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    public ServiceDetailDto getServiceDetail(Long serviceId) {
        Service service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceId));

        ServiceDetailDto dto = convertToDetailDto(service);

        // 연결 상태 확인
        dto.setConnectionStatus(checkConnectionStatus(service));
        dto.setLastHealthCheckResult(getLastHealthCheckResult(service));

        return dto;
    }

    public ServiceDto createService(CreateServiceRequest request, String createdBy) {
        // 서비스 코드 중복 확인
        if (serviceRepository.existsByServiceCode(request.getServiceCode())) {
            throw new DuplicateServiceException("Service code already exists: " + request.getServiceCode());
        }

        // 데이터베이스 연결 정보 암호화
        String encryptedConnectionInfo = encryptionService.encrypt(
            JsonUtils.toJson(request.getDbConnectionInfo()));

        Service service = Service.builder()
            .serviceCode(request.getServiceCode())
            .serviceName(request.getServiceName())
            .serviceDomain(request.getServiceDomain())
            .apiBaseUrl(request.getApiBaseUrl())
            .dbConnectionInfo(encryptedConnectionInfo)
            .healthCheckUrl(request.getHealthCheckUrl())
            .status(ServiceStatus.ACTIVE)
            .createdBy(Long.parseLong(createdBy))
            .build();

        Service savedService = serviceRepository.save(service);

        // 연결 테스트
        try {
            testConnectionInternal(savedService);
            log.info("Service created and connection tested successfully: {}", savedService.getServiceCode());
        } catch (Exception e) {
            log.warn("Service created but connection test failed: {}", savedService.getServiceCode(), e);
            // 서비스는 생성하되 비활성 상태로 변경
            savedService.setStatus(ServiceStatus.INACTIVE);
            serviceRepository.save(savedService);
        }

        return convertToDto(savedService);
    }

    public ConnectionTestResult testConnection(Long serviceId) {
        Service service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceId));

        return testConnectionInternal(service);
    }

    private ConnectionTestResult testConnectionInternal(Service service) {
        ConnectionTestResult result = new ConnectionTestResult();
        result.setServiceId(service.getServiceId());
        result.setTestedAt(LocalDateTime.now());

        try {
            // 데이터베이스 연결 테스트
            DataSource dataSource = dataSourceManager.getDataSource(service.getServiceCode());
            try (Connection connection = dataSource.getConnection()) {
                result.setDatabaseConnected(connection.isValid(5));
            }

            // API 연결 테스트 (헬스체크 엔드포인트 호출)
            if (service.getHealthCheckUrl() != null) {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    service.getHealthCheckUrl(), String.class);
                result.setApiConnected(response.getStatusCode().is2xxSuccessful());
            }

            result.setOverallStatus(result.isDatabaseConnected() && result.isApiConnected()
                ? "HEALTHY" : "UNHEALTHY");

        } catch (Exception e) {
            result.setDatabaseConnected(false);
            result.setApiConnected(false);
            result.setOverallStatus("ERROR");
            result.setErrorMessage(e.getMessage());
            log.error("Connection test failed for service: {}", service.getServiceCode(), e);
        }

        return result;
    }

    private ServiceDto convertToDto(Service service) {
        return ServiceDto.builder()
            .serviceId(service.getServiceId())
            .serviceCode(service.getServiceCode())
            .serviceName(service.getServiceName())
            .serviceDomain(service.getServiceDomain())
            .status(service.getStatus())
            .lastHealthCheck(service.getLastHealthCheck())
            .createdAt(service.getCreatedAt())
            .build();
    }
}
```

---

## 3. 통합 프론트엔드 구현

### 3.1 통합 대시보드 구현

```typescript
// hooks/useUnifiedDashboard.ts
export function useUnifiedDashboard() {
  const { data: services } = useQuery({
    queryKey: ["unified", "services"],
    queryFn: async () => {
      const response = await unifiedApi.services.getAll();
      return response.data.data;
    },
    refetchInterval: 30000, // 30초마다 갱신
  });

  const { data: metrics } = useQuery({
    queryKey: ["unified", "metrics"],
    queryFn: async () => {
      const response = await unifiedApi.dashboard.getMetrics();
      return response.data.data;
    },
    refetchInterval: 10000, // 10초마다 갱신
  });

  const { data: activities } = useQuery({
    queryKey: ["unified", "activities"],
    queryFn: async () => {
      const response = await unifiedApi.dashboard.getRecentActivities();
      return response.data.data;
    },
    refetchInterval: 15000, // 15초마다 갱신
  });

  return {
    services: services || [],
    metrics: metrics || {},
    activities: activities || [],
    isLoading: !services || !metrics || !activities,
  };
}

// components/unified/dashboard/UnifiedDashboard.tsx
export function UnifiedDashboard() {
  const { services, metrics, activities, isLoading } = useUnifiedDashboard();
  const colors = useColors();

  if (isLoading) {
    return <DashboardSkeleton />;
  }

  return (
    <Box bg={colors.bg} minH="100vh" p={6}>
      <VStack spacing={6} align="stretch">
        {/* 헤더 */}
        <Flex justify="space-between" align="center">
          <VStack align="start" spacing={1}>
            <Heading size="lg" color={colors.text.primary}>
              통합 관리 대시보드
            </Heading>
            <Text color={colors.text.secondary} fontSize="sm">
              {services.length}개 서비스 통합 관리 현황
            </Text>
          </VStack>
          <HStack spacing={3}>
            <AutoRefreshToggle />
            <RefreshButton />
            <ExportButton />
          </HStack>
        </Flex>

        {/* 주요 메트릭 카드 */}
        <Grid templateColumns="repeat(auto-fit, minmax(250px, 1fr))" gap={6}>
          <MetricCard
            title="총 서비스"
            value={metrics.totalServices}
            icon={<FiServer />}
            color="blue"
            trend={metrics.servicesTrend}
          />
          <MetricCard
            title="활성 컨텐츠"
            value={metrics.totalActiveContent}
            icon={<FiFileText />}
            color="green"
            trend={metrics.contentTrend}
          />
          <MetricCard
            title="오늘 활동"
            value={metrics.todayActivities}
            icon={<FiActivity />}
            color="purple"
            trend={metrics.activitiesTrend}
          />
          <MetricCard
            title="시스템 상태"
            value={metrics.systemHealth}
            icon={<FiShield />}
            color={getHealthColor(metrics.systemHealth)}
            isStatus={true}
          />
        </Grid>

        {/* 서비스 상태 패널 */}
        <Card>
          <CardHeader>
            <Heading size="md">서비스 상태</Heading>
          </CardHeader>
          <CardBody>
            <ServiceHealthGrid services={services} />
          </CardBody>
        </Card>

        {/* 차트 섹션 */}
        <Grid templateColumns="repeat(auto-fit, minmax(400px, 1fr))" gap={6}>
          <Card>
            <CardHeader>
              <Heading size="sm">서비스별 컨텐츠 분포</Heading>
            </CardHeader>
            <CardBody>
              <ContentDistributionChart data={metrics.contentDistribution} />
            </CardBody>
          </Card>

          <Card>
            <CardHeader>
              <Heading size="sm">최근 7일 활동</Heading>
            </CardHeader>
            <CardBody>
              <ActivityTrendChart data={metrics.activityTrend} />
            </CardBody>
          </Card>
        </Grid>

        {/* 최근 활동 피드 */}
        <Card>
          <CardHeader>
            <Flex justify="space-between" align="center">
              <Heading size="sm">최근 활동</Heading>
              <Button
                variant="ghost"
                size="sm"
                as={Link}
                href="/unified/activities"
              >
                전체 보기
              </Button>
            </Flex>
          </CardHeader>
          <CardBody>
            <RecentActivityFeed activities={activities} />
          </CardBody>
        </Card>
      </VStack>
    </Box>
  );
}

// components/unified/dashboard/ServiceHealthGrid.tsx
interface ServiceHealthGridProps {
  services: Service[];
}

export function ServiceHealthGrid({ services }: ServiceHealthGridProps) {
  return (
    <Grid templateColumns="repeat(auto-fill, minmax(300px, 1fr))" gap={4}>
      {services.map((service) => (
        <ServiceHealthCard key={service.serviceId} service={service} />
      ))}
    </Grid>
  );
}

function ServiceHealthCard({ service }: { service: Service }) {
  const colors = useColors();
  const statusColor = getStatusColor(service.status);
  const healthColor = getHealthColor(
    service.lastHealthCheckResult?.overallStatus
  );

  return (
    <Card variant="outline" position="relative">
      <CardBody>
        <VStack align="start" spacing={3}>
          <Flex justify="space-between" w="full" align="center">
            <VStack align="start" spacing={1}>
              <Heading size="sm">{service.serviceName}</Heading>
              <Text fontSize="xs" color={colors.text.secondary}>
                {service.serviceCode}
              </Text>
            </VStack>
            <Badge colorScheme={statusColor} variant="subtle">
              {service.status}
            </Badge>
          </Flex>

          <Divider />

          <VStack w="full" spacing={2}>
            <Flex justify="space-between" w="full">
              <Text fontSize="sm">데이터베이스</Text>
              <Badge
                colorScheme={
                  service.lastHealthCheckResult?.databaseConnected
                    ? "green"
                    : "red"
                }
                size="sm"
              >
                {service.lastHealthCheckResult?.databaseConnected
                  ? "연결됨"
                  : "연결 실패"}
              </Badge>
            </Flex>

            <Flex justify="space-between" w="full">
              <Text fontSize="sm">API 서버</Text>
              <Badge
                colorScheme={
                  service.lastHealthCheckResult?.apiConnected ? "green" : "red"
                }
                size="sm"
              >
                {service.lastHealthCheckResult?.apiConnected ? "정상" : "오류"}
              </Badge>
            </Flex>

            <Flex justify="space-between" w="full">
              <Text fontSize="sm">마지막 확인</Text>
              <Text fontSize="sm" color={colors.text.secondary}>
                {service.lastHealthCheck
                  ? formatDistanceToNow(new Date(service.lastHealthCheck), {
                      addSuffix: true,
                      locale: ko,
                    })
                  : "미확인"}
              </Text>
            </Flex>
          </VStack>

          <HStack w="full" justify="end" spacing={2}>
            <Button
              size="xs"
              variant="ghost"
              as={Link}
              href={`/unified/services/${service.serviceId}`}
            >
              관리
            </Button>
            <Button
              size="xs"
              variant="outline"
              onClick={() => handleHealthCheck(service.serviceId)}
            >
              상태 확인
            </Button>
          </HStack>
        </VStack>
      </CardBody>
    </Card>
  );
}
```

### 3.2 통합 컨텐츠 관리

```typescript
// pages/unified/content/page.tsx
export default function UnifiedContentPage() {
  const [selectedServices, setSelectedServices] = useState<string[]>([]);
  const [contentType, setContentType] = useState<string>("all");
  const [bulkSelection, setBulkSelection] = useState<number[]>([]);

  const { data: services } = useServices();
  const { data: contentData, isLoading } = useUnifiedContent({
    services: selectedServices,
    contentType,
  });

  const handleBulkAction = async (action: string) => {
    if (bulkSelection.length === 0) {
      toast({
        title: "선택된 항목이 없습니다",
        status: "warning",
      });
      return;
    }

    try {
      await unifiedApi.content.bulkAction(action, bulkSelection);
      toast({
        title: `${bulkSelection.length}개 항목이 ${action} 처리되었습니다`,
        status: "success",
      });
      setBulkSelection([]);
      // 데이터 새로고침
    } catch (error) {
      toast({
        title: "일괄 처리 중 오류가 발생했습니다",
        status: "error",
      });
    }
  };

  return (
    <Box p={6}>
      <VStack spacing={6} align="stretch">
        <Flex justify="space-between" align="center">
          <Heading size="lg">통합 컨텐츠 관리</Heading>
          <HStack>
            <SyncAllButton />
            <ExportButton selection={bulkSelection} />
          </HStack>
        </Flex>

        {/* 필터 섹션 */}
        <Card>
          <CardBody>
            <HStack spacing={4} wrap="wrap">
              <FormControl maxW="300px">
                <FormLabel fontSize="sm">서비스 선택</FormLabel>
                <ServiceMultiSelect
                  services={services}
                  value={selectedServices}
                  onChange={setSelectedServices}
                />
              </FormControl>

              <FormControl maxW="200px">
                <FormLabel fontSize="sm">컨텐츠 유형</FormLabel>
                <Select
                  value={contentType}
                  onChange={(e) => setContentType(e.target.value)}
                >
                  <option value="all">전체</option>
                  <option value="board">게시글</option>
                  <option value="popup">팝업</option>
                  <option value="content">컨텐츠</option>
                  <option value="menu">메뉴</option>
                </Select>
              </FormControl>

              <FormControl maxW="200px">
                <FormLabel fontSize="sm">상태</FormLabel>
                <Select>
                  <option value="all">전체</option>
                  <option value="active">활성</option>
                  <option value="inactive">비활성</option>
                  <option value="draft">임시저장</option>
                </Select>
              </FormControl>
            </HStack>
          </CardBody>
        </Card>

        {/* 일괄 작업 패널 */}
        {bulkSelection.length > 0 && (
          <Card bg="blue.50" borderColor="blue.200">
            <CardBody>
              <Flex justify="space-between" align="center">
                <Text fontWeight="medium">
                  {bulkSelection.length}개 항목이 선택됨
                </Text>
                <HStack spacing={2}>
                  <Button
                    size="sm"
                    onClick={() => handleBulkAction("activate")}
                  >
                    활성화
                  </Button>
                  <Button
                    size="sm"
                    onClick={() => handleBulkAction("deactivate")}
                  >
                    비활성화
                  </Button>
                  <Button
                    size="sm"
                    onClick={() => handleBulkAction("delete")}
                    colorScheme="red"
                  >
                    삭제
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setBulkSelection([])}
                  >
                    선택 해제
                  </Button>
                </HStack>
              </Flex>
            </CardBody>
          </Card>
        )}

        {/* 컨텐츠 테이블 */}
        <Card>
          <CardBody p={0}>
            <UnifiedContentTable
              data={contentData}
              isLoading={isLoading}
              selection={bulkSelection}
              onSelectionChange={setBulkSelection}
            />
          </CardBody>
        </Card>
      </VStack>
    </Box>
  );
}

// components/unified/content/UnifiedContentTable.tsx
interface UnifiedContentTableProps {
  data: UnifiedContent[];
  isLoading: boolean;
  selection: number[];
  onSelectionChange: (selection: number[]) => void;
}

export function UnifiedContentTable({
  data,
  isLoading,
  selection,
  onSelectionChange,
}: UnifiedContentTableProps) {
  const colors = useColors();

  const columns = useMemo(
    () => [
      {
        id: "select",
        header: ({ table }) => (
          <Checkbox
            isChecked={table.getIsAllRowsSelected()}
            isIndeterminate={table.getIsSomeRowsSelected()}
            onChange={table.getToggleAllRowsSelectedHandler()}
          />
        ),
        cell: ({ row }) => (
          <Checkbox
            isChecked={row.getIsSelected()}
            onChange={row.getToggleSelectedHandler()}
          />
        ),
      },
      {
        accessorKey: "serviceName",
        header: "서비스",
        cell: ({ row }) => (
          <Badge colorScheme="blue" variant="subtle">
            {row.original.serviceName}
          </Badge>
        ),
      },
      {
        accessorKey: "contentType",
        header: "유형",
        cell: ({ row }) => (
          <Badge colorScheme={getContentTypeColor(row.original.contentType)}>
            {getContentTypeLabel(row.original.contentType)}
          </Badge>
        ),
      },
      {
        accessorKey: "title",
        header: "제목",
        cell: ({ row }) => (
          <VStack align="start" spacing={1}>
            <Text fontWeight="medium" noOfLines={1}>
              {row.original.title}
            </Text>
            <Text fontSize="sm" color={colors.text.secondary} noOfLines={1}>
              {row.original.summary}
            </Text>
          </VStack>
        ),
      },
      {
        accessorKey: "author",
        header: "작성자",
      },
      {
        accessorKey: "status",
        header: "상태",
        cell: ({ row }) => <StatusBadge status={row.original.status} />,
      },
      {
        accessorKey: "createdAt",
        header: "생성일",
        cell: ({ row }) => (
          <Text fontSize="sm">
            {format(new Date(row.original.createdAt), "yyyy-MM-dd HH:mm")}
          </Text>
        ),
      },
      {
        accessorKey: "lastSyncedAt",
        header: "마지막 동기화",
        cell: ({ row }) => (
          <VStack spacing={1} align="start">
            <Text fontSize="sm">
              {row.original.lastSyncedAt
                ? format(new Date(row.original.lastSyncedAt), "MM-dd HH:mm")
                : "미동기화"}
            </Text>
            <SyncStatus status={row.original.syncStatus} />
          </VStack>
        ),
      },
      {
        id: "actions",
        header: "작업",
        cell: ({ row }) => (
          <HStack spacing={1}>
            <IconButton
              aria-label="편집"
              icon={<FiEdit2 />}
              size="sm"
              variant="ghost"
              onClick={() => handleEdit(row.original)}
            />
            <IconButton
              aria-label="동기화"
              icon={<FiRefreshCw />}
              size="sm"
              variant="ghost"
              onClick={() => handleSync(row.original)}
            />
            <Menu>
              <MenuButton
                as={IconButton}
                aria-label="더보기"
                icon={<FiMoreVertical />}
                size="sm"
                variant="ghost"
              />
              <MenuList>
                <MenuItem
                  icon={<FiEye />}
                  onClick={() => handleView(row.original)}
                >
                  미리보기
                </MenuItem>
                <MenuItem
                  icon={<FiCopy />}
                  onClick={() => handleDuplicate(row.original)}
                >
                  복제
                </MenuItem>
                <MenuDivider />
                <MenuItem
                  icon={<FiTrash2 />}
                  color="red.500"
                  onClick={() => handleDelete(row.original)}
                >
                  삭제
                </MenuItem>
              </MenuList>
            </Menu>
          </HStack>
        ),
      },
    ],
    [colors]
  );

  if (isLoading) {
    return <TableSkeleton columns={columns.length} rows={10} />;
  }

  return (
    <DataTable
      data={data}
      columns={columns}
      selection={selection}
      onSelectionChange={onSelectionChange}
      enableSorting
      enableFiltering
      enablePagination
      pageSize={50}
    />
  );
}
```

이렇게 통합 CMS 고도화를 위한 상세 구현 가이드를 작성했습니다. 다음 단계로는 테스팅 전략, 성능 최적화, 모니터링 및 운영 가이드를 더 추가할 수 있습니다. 어떤 부분을 더 자세히 다루길 원하시나요?
