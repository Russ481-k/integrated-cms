# 통합 CMS 테스팅 및 성능 최적화 가이드

## 1. 테스팅 전략

### 1.1 단위 테스트

#### 1.1.1 백엔드 단위 테스트

```java
// ServiceManagementServiceTest.java
@ExtendWith(MockitoExtension.class)
@DisplayName("서비스 관리 서비스 테스트")
class ServiceManagementServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private DynamicDataSourceManager dataSourceManager;

    @InjectMocks
    private ServiceManagementService serviceManagementService;

    @Test
    @DisplayName("새 서비스 생성 성공")
    void createService_Success() {
        // Given
        CreateServiceRequest request = CreateServiceRequest.builder()
            .serviceCode("test-service")
            .serviceName("Test Service")
            .apiBaseUrl("http://localhost:8081")
            .dbConnectionInfo(DatabaseConnectionInfo.builder()
                .host("localhost")
                .port(3307)
                .database("test_db")
                .username("test_user")
                .password("test_pass")
                .build())
            .build();

        when(serviceRepository.existsByServiceCode("test-service")).thenReturn(false);
        when(encryptionService.encrypt(any())).thenReturn("encrypted_connection_info");
        when(serviceRepository.save(any(Service.class))).thenAnswer(invocation -> {
            Service service = invocation.getArgument(0);
            service.setServiceId(1L);
            return service;
        });

        // When
        ServiceDto result = serviceManagementService.createService(request, "admin");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getServiceCode()).isEqualTo("test-service");
        assertThat(result.getServiceName()).isEqualTo("Test Service");
        verify(serviceRepository).save(any(Service.class));
        verify(encryptionService).encrypt(any());
    }

    @Test
    @DisplayName("중복 서비스 코드로 생성 시 예외 발생")
    void createService_DuplicateServiceCode_ThrowsException() {
        // Given
        CreateServiceRequest request = CreateServiceRequest.builder()
            .serviceCode("existing-service")
            .build();

        when(serviceRepository.existsByServiceCode("existing-service")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> serviceManagementService.createService(request, "admin"))
            .isInstanceOf(DuplicateServiceException.class)
            .hasMessageContaining("Service code already exists");
    }

    @Test
    @DisplayName("서비스 연결 테스트 성공")
    void testConnection_Success() throws SQLException {
        // Given
        Service service = Service.builder()
            .serviceId(1L)
            .serviceCode("test-service")
            .dbConnectionInfo("encrypted_info")
            .healthCheckUrl("http://localhost:8081/health")
            .build();

        DataSource mockDataSource = mock(DataSource.class);
        Connection mockConnection = mock(Connection.class);

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(dataSourceManager.getDataSource("test-service")).thenReturn(mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(5)).thenReturn(true);

        // When
        ConnectionTestResult result = serviceManagementService.testConnection(1L);

        // Then
        assertThat(result.isDatabaseConnected()).isTrue();
        assertThat(result.getOverallStatus()).isEqualTo("HEALTHY");
    }
}

// DynamicDataSourceManagerTest.java
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class DynamicDataSourceManagerTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private DynamicDataSourceManager dataSourceManager;

    @Test
    @Order(1)
    @DisplayName("데이터소스 생성 및 캐싱 테스트")
    void getDataSource_CreatesAndCaches() {
        // Given
        Service service = Service.builder()
            .serviceCode("test-service")
            .dbConnectionInfo("encrypted_info")
            .status(ServiceStatus.ACTIVE)
            .build();

        DatabaseConnectionInfo dbInfo = DatabaseConnectionInfo.builder()
            .host("localhost")
            .port(3306)
            .database("test_db")
            .username("test_user")
            .password("test_pass")
            .build();

        when(serviceRepository.findByServiceCode("test-service"))
            .thenReturn(Optional.of(service));
        when(encryptionService.decrypt("encrypted_info"))
            .thenReturn(JsonUtils.toJson(dbInfo));

        // When
        DataSource dataSource1 = dataSourceManager.getDataSource("test-service");
        DataSource dataSource2 = dataSourceManager.getDataSource("test-service");

        // Then
        assertThat(dataSource1).isNotNull();
        assertThat(dataSource1).isSameAs(dataSource2); // 캐싱 확인
        verify(serviceRepository, times(1)).findByServiceCode("test-service");
    }

    @Test
    @Order(2)
    @DisplayName("비활성 서비스 접근 시 예외 발생")
    void getDataSource_InactiveService_ThrowsException() {
        // Given
        Service service = Service.builder()
            .serviceCode("inactive-service")
            .status(ServiceStatus.INACTIVE)
            .build();

        when(serviceRepository.findByServiceCode("inactive-service"))
            .thenReturn(Optional.of(service));

        // When & Then
        assertThatThrownBy(() -> dataSourceManager.getDataSource("inactive-service"))
            .isInstanceOf(ServiceUnavailableException.class);
    }
}
```

#### 1.1.2 프론트엔드 단위 테스트

```typescript
// __tests__/hooks/useUnifiedDashboard.test.ts
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useUnifiedDashboard } from "@/hooks/useUnifiedDashboard";
import { unifiedApi } from "@/lib/api/unifiedApi";

// Mock API
jest.mock("@/lib/api/unifiedApi", () => ({
  unifiedApi: {
    services: {
      getAll: jest.fn(),
    },
    dashboard: {
      getMetrics: jest.fn(),
      getRecentActivities: jest.fn(),
    },
  },
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe("useUnifiedDashboard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should fetch and return dashboard data", async () => {
    // Given
    const mockServices = [
      { serviceId: 1, serviceName: "Service 1", status: "ACTIVE" },
      { serviceId: 2, serviceName: "Service 2", status: "ACTIVE" },
    ];

    const mockMetrics = {
      totalServices: 2,
      totalActiveContent: 150,
      todayActivities: 45,
      systemHealth: "HEALTHY",
    };

    const mockActivities = [
      { id: 1, action: "CREATE", description: "Created new content" },
      { id: 2, action: "UPDATE", description: "Updated service settings" },
    ];

    (unifiedApi.services.getAll as jest.Mock).mockResolvedValue({
      data: { data: mockServices },
    });
    (unifiedApi.dashboard.getMetrics as jest.Mock).mockResolvedValue({
      data: { data: mockMetrics },
    });
    (unifiedApi.dashboard.getRecentActivities as jest.Mock).mockResolvedValue({
      data: { data: mockActivities },
    });

    // When
    const { result } = renderHook(() => useUnifiedDashboard(), {
      wrapper: createWrapper(),
    });

    // Then
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.services).toEqual(mockServices);
    expect(result.current.metrics).toEqual(mockMetrics);
    expect(result.current.activities).toEqual(mockActivities);
  });

  it("should handle API errors gracefully", async () => {
    // Given
    (unifiedApi.services.getAll as jest.Mock).mockRejectedValue(
      new Error("API Error")
    );
    (unifiedApi.dashboard.getMetrics as jest.Mock).mockRejectedValue(
      new Error("API Error")
    );
    (unifiedApi.dashboard.getRecentActivities as jest.Mock).mockRejectedValue(
      new Error("API Error")
    );

    // When
    const { result } = renderHook(() => useUnifiedDashboard(), {
      wrapper: createWrapper(),
    });

    // Then
    await waitFor(() => {
      expect(result.current.services).toEqual([]);
      expect(result.current.metrics).toEqual({});
      expect(result.current.activities).toEqual([]);
    });
  });
});

// __tests__/components/ServiceHealthCard.test.tsx
import { render, screen, fireEvent } from "@testing-library/react";
import { ServiceHealthCard } from "@/components/unified/dashboard/ServiceHealthCard";
import { ChakraProvider } from "@chakra-ui/react";

const mockService = {
  serviceId: 1,
  serviceCode: "test-service",
  serviceName: "Test Service",
  status: "ACTIVE",
  lastHealthCheck: "2024-01-15T10:30:00Z",
  lastHealthCheckResult: {
    databaseConnected: true,
    apiConnected: true,
    overallStatus: "HEALTHY",
  },
};

const renderWithChakra = (component: React.ReactElement) => {
  return render(<ChakraProvider>{component}</ChakraProvider>);
};

describe("ServiceHealthCard", () => {
  it("should render service information correctly", () => {
    // When
    renderWithChakra(<ServiceHealthCard service={mockService} />);

    // Then
    expect(screen.getByText("Test Service")).toBeInTheDocument();
    expect(screen.getByText("test-service")).toBeInTheDocument();
    expect(screen.getByText("ACTIVE")).toBeInTheDocument();
    expect(screen.getByText("연결됨")).toBeInTheDocument();
    expect(screen.getByText("정상")).toBeInTheDocument();
  });

  it("should show error status for unhealthy service", () => {
    // Given
    const unhealthyService = {
      ...mockService,
      lastHealthCheckResult: {
        databaseConnected: false,
        apiConnected: false,
        overallStatus: "ERROR",
      },
    };

    // When
    renderWithChakra(<ServiceHealthCard service={unhealthyService} />);

    // Then
    expect(screen.getByText("연결 실패")).toBeInTheDocument();
    expect(screen.getByText("오류")).toBeInTheDocument();
  });

  it("should handle health check button click", () => {
    // Given
    const mockHandleHealthCheck = jest.fn();
    // Mock the handleHealthCheck function
    jest
      .spyOn(
        require("@/components/unified/dashboard/ServiceHealthCard"),
        "handleHealthCheck"
      )
      .mockImplementation(mockHandleHealthCheck);

    // When
    renderWithChakra(<ServiceHealthCard service={mockService} />);
    fireEvent.click(screen.getByText("상태 확인"));

    // Then
    expect(mockHandleHealthCheck).toHaveBeenCalledWith(1);
  });
});
```

### 1.2 통합 테스트

#### 1.2.1 API 통합 테스트

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("서비스 관리 API 통합 테스트")
class ServiceManagementIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @MockBean
    private EncryptionService encryptionService;

    private String authToken;

    @BeforeEach
    void setUp() {
        // 테스트용 관리자 생성
        AdminUser admin = AdminUser.builder()
            .username("testadmin")
            .password("$2a$10$encoded_password")
            .role(AdminRole.SUPER_ADMIN)
            .status(AdminStatus.ACTIVE)
            .build();
        adminUserRepository.save(admin);

        // JWT 토큰 생성
        authToken = generateTestToken(admin);

        // 암호화 서비스 모킹
        when(encryptionService.encrypt(any())).thenReturn("encrypted_data");
        when(encryptionService.decrypt(any())).thenReturn("{\"host\":\"localhost\",\"port\":3306,\"database\":\"test\",\"username\":\"user\",\"password\":\"pass\"}");
    }

    @Test
    @DisplayName("서비스 생성 API 성공")
    void createService_Success() {
        // Given
        CreateServiceRequest request = CreateServiceRequest.builder()
            .serviceCode("integration-test")
            .serviceName("Integration Test Service")
            .serviceDomain("integration.test.com")
            .apiBaseUrl("http://localhost:8081")
            .healthCheckUrl("http://localhost:8081/health")
            .dbConnectionInfo(DatabaseConnectionInfo.builder()
                .host("localhost")
                .port(3306)
                .database("integration_test")
                .username("test_user")
                .password("test_pass")
                .build())
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<CreateServiceRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/unified/services", entity, ApiResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(200);

        // DB 검증
        Optional<Service> savedService = serviceRepository.findByServiceCode("integration-test");
        assertThat(savedService).isPresent();
        assertThat(savedService.get().getServiceName()).isEqualTo("Integration Test Service");
    }

    @Test
    @DisplayName("권한 없는 사용자의 서비스 생성 시 403 반환")
    void createService_Unauthorized_Returns403() {
        // Given
        CreateServiceRequest request = CreateServiceRequest.builder()
            .serviceCode("unauthorized-test")
            .serviceName("Unauthorized Test")
            .build();

        // 권한이 없는 토큰 생성 (OPERATOR 역할)
        AdminUser operator = AdminUser.builder()
            .username("operator")
            .role(AdminRole.OPERATOR)
            .build();
        adminUserRepository.save(operator);
        String operatorToken = generateTestToken(operator);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(operatorToken);
        HttpEntity<CreateServiceRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/unified/services", entity, ApiResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("서비스 목록 조회 API 성공")
    void getAllServices_Success() {
        // Given
        Service service1 = Service.builder()
            .serviceCode("service1")
            .serviceName("Service 1")
            .status(ServiceStatus.ACTIVE)
            .build();
        Service service2 = Service.builder()
            .serviceCode("service2")
            .serviceName("Service 2")
            .status(ServiceStatus.INACTIVE)
            .build();
        serviceRepository.saveAll(Arrays.asList(service1, service2));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            "/api/unified/services", HttpMethod.GET, entity, ApiResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(200);

        List<Map<String, Object>> services = (List<Map<String, Object>>) response.getBody().getData();
        assertThat(services).hasSize(2);
    }
}
```

#### 1.2.2 E2E 테스트

```typescript
// e2e/unified-dashboard.spec.ts
import { test, expect } from "@playwright/test";

test.describe("Unified Dashboard", () => {
  test.beforeEach(async ({ page }) => {
    // 로그인
    await page.goto("/login");
    await page.fill("[data-testid=username]", "testadmin");
    await page.fill("[data-testid=password]", "password123");
    await page.click("[data-testid=login-button]");

    // 대시보드로 이동
    await page.goto("/unified/dashboard");
  });

  test("should display dashboard metrics", async ({ page }) => {
    // 메트릭 카드들이 표시되는지 확인
    await expect(
      page.locator("[data-testid=metric-total-services]")
    ).toBeVisible();
    await expect(
      page.locator("[data-testid=metric-active-content]")
    ).toBeVisible();
    await expect(
      page.locator("[data-testid=metric-today-activities]")
    ).toBeVisible();
    await expect(
      page.locator("[data-testid=metric-system-health]")
    ).toBeVisible();

    // 메트릭 값이 숫자인지 확인
    const totalServices = await page
      .locator("[data-testid=metric-total-services] .metric-value")
      .textContent();
    expect(parseInt(totalServices || "0")).toBeGreaterThanOrEqual(0);
  });

  test("should display service health grid", async ({ page }) => {
    // 서비스 헬스 그리드가 표시되는지 확인
    await expect(
      page.locator("[data-testid=service-health-grid]")
    ).toBeVisible();

    // 최소 하나의 서비스 카드가 있는지 확인
    const serviceCards = page.locator("[data-testid^=service-card-]");
    await expect(serviceCards.first()).toBeVisible();

    // 첫 번째 서비스 카드의 정보가 표시되는지 확인
    const firstCard = serviceCards.first();
    await expect(firstCard.locator(".service-name")).toBeVisible();
    await expect(firstCard.locator(".service-status")).toBeVisible();
  });

  test("should perform health check when button clicked", async ({ page }) => {
    // 첫 번째 서비스의 상태 확인 버튼 클릭
    await page.click(
      '[data-testid^=service-card-] button:has-text("상태 확인")'
    );

    // 로딩 상태 확인
    await expect(page.locator(".loading-spinner")).toBeVisible();

    // 완료 후 결과가 업데이트되는지 확인
    await page.waitForSelector(".loading-spinner", { state: "detached" });

    // 성공 토스트 메시지 확인
    await expect(page.locator(".toast-success")).toBeVisible();
  });

  test("should refresh data automatically", async ({ page }) => {
    // 초기 마지막 업데이트 시간 기록
    const initialTimestamp = await page
      .locator("[data-testid=last-updated]")
      .textContent();

    // 30초 대기 (자동 새로고침 주기)
    await page.waitForTimeout(30000);

    // 업데이트 시간이 변경되었는지 확인
    const updatedTimestamp = await page
      .locator("[data-testid=last-updated]")
      .textContent();
    expect(updatedTimestamp).not.toBe(initialTimestamp);
  });
});

// e2e/unified-content.spec.ts
test.describe("Unified Content Management", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/login");
    await page.fill("[data-testid=username]", "testadmin");
    await page.fill("[data-testid=password]", "password123");
    await page.click("[data-testid=login-button]");
    await page.goto("/unified/content");
  });

  test("should filter content by service", async ({ page }) => {
    // 서비스 필터 선택
    await page.click("[data-testid=service-filter]");
    await page.click("[data-testid=service-option-service1]");

    // 필터링된 결과 확인
    await page.waitForSelector("[data-testid=content-table]");
    const rows = page.locator("[data-testid=content-table] tbody tr");

    // 모든 행이 선택된 서비스의 컨텐츠인지 확인
    const count = await rows.count();
    for (let i = 0; i < count; i++) {
      const serviceCell = rows.nth(i).locator("td:first-child");
      await expect(serviceCell).toContainText("Service1");
    }
  });

  test("should perform bulk actions", async ({ page }) => {
    // 여러 아이템 선택
    await page.check("[data-testid=select-all-checkbox]");

    // 일괄 작업 패널이 나타나는지 확인
    await expect(page.locator("[data-testid=bulk-action-panel]")).toBeVisible();

    // 활성화 버튼 클릭
    await page.click("[data-testid=bulk-activate-button]");

    // 확인 다이얼로그
    await page.click("[data-testid=confirm-bulk-action]");

    // 성공 메시지 확인
    await expect(page.locator(".toast-success")).toBeVisible();
    await expect(page.locator(".toast-success")).toContainText(
      "활성화 처리되었습니다"
    );
  });

  test("should export selected content", async ({ page }) => {
    // 몇 개 아이템 선택
    await page.check("[data-testid=content-row-1] input[type=checkbox]");
    await page.check("[data-testid=content-row-2] input[type=checkbox]");

    // 내보내기 버튼 클릭
    const downloadPromise = page.waitForEvent("download");
    await page.click("[data-testid=export-button]");

    // 다운로드 완료 확인
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toMatch(/content-export-.*\.xlsx/);
  });
});
```

## 2. 성능 최적화

### 2.1 데이터베이스 최적화

#### 2.1.1 인덱스 최적화

```sql
-- 통합 DB 인덱스 최적화
USE unified_cms;

-- 서비스 조회 최적화
CREATE INDEX idx_services_code_status ON services(service_code, status);
CREATE INDEX idx_services_status_updated ON services(status, updated_at);

-- 권한 조회 최적화
CREATE INDEX idx_permissions_admin_service ON admin_service_permissions(admin_id, service_id);
CREATE INDEX idx_permissions_service_active ON admin_service_permissions(service_id, expires_at);

-- 활동 로그 조회 최적화
CREATE INDEX idx_activity_logs_admin_created ON unified_activity_logs(admin_id, created_at DESC);
CREATE INDEX idx_activity_logs_service_created ON unified_activity_logs(service_id, created_at DESC);
CREATE INDEX idx_activity_logs_action_created ON unified_activity_logs(action, created_at DESC);

-- 통합 컨텐츠 관리 최적화
CREATE INDEX idx_unified_content_service_type ON unified_content_management(service_id, content_type);
CREATE INDEX idx_unified_content_status_sync ON unified_content_management(status, last_synced_at);

-- 파티셔닝 (활동 로그 테이블)
ALTER TABLE unified_activity_logs
PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

#### 2.1.2 연결 풀 최적화

```java
@Configuration
@ConfigurationProperties(prefix = "app.datasource")
public class DataSourceConfig {

    private int maxPoolSize = 20;
    private int minIdle = 5;
    private long connectionTimeout = 30000;
    private long idleTimeout = 600000;
    private long maxLifetime = 1800000;
    private long leakDetectionThreshold = 60000;

    @Bean
    @Primary
    public DataSource unifiedDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/unified_cms");
        config.setUsername("${spring.datasource.username}");
        config.setPassword("${spring.datasource.password}");

        // 성능 최적화 설정
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakDetectionThreshold);

        // MySQL 최적화
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        return new HikariDataSource(config);
    }

    @Bean
    public DataSourceHealthIndicator dataSourceHealthIndicator() {
        return new DataSourceHealthIndicator(unifiedDataSource(), "SELECT 1");
    }
}

// 동적 데이터소스 최적화
@Component
@Slf4j
public class OptimizedDynamicDataSourceManager {

    private final LoadingCache<String, DataSource> dataSourceCache;
    private final ServiceRepository serviceRepository;
    private final EncryptionService encryptionService;

    public OptimizedDynamicDataSourceManager(ServiceRepository serviceRepository,
                                           EncryptionService encryptionService) {
        this.serviceRepository = serviceRepository;
        this.encryptionService = encryptionService;

        this.dataSourceCache = Caffeine.newBuilder()
            .maximumSize(50) // 최대 50개 서비스
            .expireAfterWrite(1, TimeUnit.HOURS) // 1시간 후 만료
            .expireAfterAccess(30, TimeUnit.MINUTES) // 30분 비활성 시 만료
            .removalListener(this::onDataSourceRemoval)
            .recordStats() // 통계 기록
            .build(this::createDataSource);
    }

    public DataSource getDataSource(String serviceCode) {
        try {
            return dataSourceCache.get(serviceCode);
        } catch (Exception e) {
            log.error("Failed to get DataSource for service: {}", serviceCode, e);
            throw new DataSourceCreationException("DataSource creation failed", e);
        }
    }

    private DataSource createDataSource(String serviceCode) {
        Service service = serviceRepository.findByServiceCodeAndStatus(serviceCode, ServiceStatus.ACTIVE)
            .orElseThrow(() -> new ServiceNotFoundException("Active service not found: " + serviceCode));

        try {
            String decryptedInfo = encryptionService.decrypt(service.getDbConnectionInfo());
            DatabaseConnectionInfo dbInfo = JsonUtils.fromJson(decryptedInfo, DatabaseConnectionInfo.class);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
                dbInfo.getHost(), dbInfo.getPort(), dbInfo.getDatabase()));
            config.setUsername(dbInfo.getUsername());
            config.setPassword(dbInfo.getPassword());

            // 서비스별 연결 풀 크기 조정 (서비스 규모에 따라)
            int poolSize = calculatePoolSize(service);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(Math.max(1, poolSize / 4));

            // 연결 타임아웃 설정
            config.setConnectionTimeout(20000); // 20초
            config.setValidationTimeout(5000);  // 5초
            config.setIdleTimeout(300000);      // 5분
            config.setMaxLifetime(900000);      // 15분

            // 연결 검증
            config.setConnectionTestQuery("SELECT 1");

            // MySQL 최적화 (서비스별 DB용)
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "100");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");

            return new HikariDataSource(config);

        } catch (Exception e) {
            log.error("Failed to create DataSource for service: {}", serviceCode, e);
            throw new DataSourceCreationException("DataSource creation failed", e);
        }
    }

    private int calculatePoolSize(Service service) {
        // 서비스별 예상 동시 접속자 수에 따라 풀 크기 결정
        // 실제로는 서비스별 통계 데이터를 기반으로 계산
        return 10; // 기본값
    }

    private void onDataSourceRemoval(String serviceCode, DataSource dataSource, RemovalCause cause) {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            log.info("DataSource removed for service: {} (cause: {})", serviceCode, cause);
        }
    }

    @EventListener
    public void handleServiceStatusChange(ServiceStatusChangeEvent event) {
        if (ServiceStatus.INACTIVE.equals(event.getNewStatus()) ||
            ServiceStatus.MAINTENANCE.equals(event.getNewStatus())) {
            evictDataSource(event.getServiceCode());
        }
    }

    public void evictDataSource(String serviceCode) {
        dataSourceCache.invalidate(serviceCode);
    }

    public CacheStats getCacheStats() {
        return dataSourceCache.stats();
    }
}
```

### 2.2 캐싱 전략

#### 2.2.1 Redis 캐시 구성

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("localhost", 6379));
        factory.setValidateConnection(true);
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());

        // 캐시별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 서비스 정보 캐시 (5분)
        cacheConfigurations.put("services",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("unified:services:"));

        // 권한 정보 캐시 (10분)
        cacheConfigurations.put("permissions",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .prefixCacheNameWith("unified:permissions:"));

        // 대시보드 메트릭 캐시 (1분)
        cacheConfigurations.put("dashboard-metrics",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))
                .prefixCacheNameWith("unified:metrics:"));

        // 컨텐츠 데이터 캐시 (30분)
        cacheConfigurations.put("content-data",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("unified:content:"));

        return builder.withInitialCacheConfigurations(cacheConfigurations).build();
    }

    private RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

### 2.3 프론트엔드 최적화

#### 2.3.1 데이터 Fetching 최적화

```typescript
// hooks/useOptimizedQuery.ts
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";

interface OptimizedQueryOptions<T> {
  queryKey: string[];
  queryFn: () => Promise<T>;
  staleTime?: number;
  cacheTime?: number;
  refetchOnWindowFocus?: boolean;
  refetchInterval?: number;
  prefetch?: boolean;
}

export function useOptimizedQuery<T>({
  queryKey,
  queryFn,
  staleTime = 5 * 60 * 1000, // 5분
  cacheTime = 10 * 60 * 1000, // 10분
  refetchOnWindowFocus = false,
  refetchInterval,
  prefetch = false,
}: OptimizedQueryOptions<T>) {
  const queryClient = useQueryClient();

  // 데이터 미리 가져오기
  const prefetchData = useCallback(() => {
    queryClient.prefetchQuery({
      queryKey,
      queryFn,
      staleTime,
      cacheTime,
    });
  }, [queryClient, queryKey, queryFn, staleTime, cacheTime]);

  const query = useQuery({
    queryKey,
    queryFn,
    staleTime,
    cacheTime,
    refetchOnWindowFocus,
    refetchInterval,
    retry: (failureCount, error) => {
      // 네트워크 오류는 3번까지 재시도
      if (error?.message?.includes("Network")) {
        return failureCount < 3;
      }
      // 권한 오류는 재시도 안함
      if (error?.status === 403) {
        return false;
      }
      return failureCount < 2;
    },
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });

  return {
    ...query,
    prefetchData,
  };
}

// 서비스별 최적화된 훅
export function useServices() {
  return useOptimizedQuery({
    queryKey: ["unified", "services"],
    queryFn: async () => {
      const response = await unifiedApi.services.getAll();
      return response.data.data;
    },
    staleTime: 5 * 60 * 1000, // 서비스 정보는 자주 변경되지 않음
    cacheTime: 30 * 60 * 1000, // 30분 캐시
    prefetch: true,
  });
}

export function useUnifiedContent(filters: ContentFilters) {
  const queryKey = ["unified", "content", JSON.stringify(filters)];

  return useOptimizedQuery({
    queryKey,
    queryFn: async () => {
      const response = await unifiedApi.content.getUnified(filters);
      return response.data.data;
    },
    staleTime: 2 * 60 * 1000, // 컨텐츠는 자주 변경될 수 있음
    cacheTime: 10 * 60 * 1000,
  });
}

// hooks/useInfiniteContent.ts - 대용량 데이터 처리
export function useInfiniteContent(filters: ContentFilters) {
  return useInfiniteQuery({
    queryKey: ["unified", "content", "infinite", JSON.stringify(filters)],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await unifiedApi.content.getUnified({
        ...filters,
        page: pageParam,
        size: 50,
      });
      return response.data.data;
    },
    getNextPageParam: (lastPage, pages) => {
      if (lastPage.length < 50) return undefined;
      return pages.length;
    },
    staleTime: 2 * 60 * 1000,
    cacheTime: 10 * 60 * 1000,
  });
}
```

#### 2.3.2 컴포넌트 최적화

```typescript
// components/optimized/VirtualizedTable.tsx
import { FixedSizeList as List } from "react-window";
import { memo, useMemo } from "react";

interface VirtualizedTableProps<T> {
  data: T[];
  columns: Column<T>[];
  height: number;
  itemHeight: number;
  onRowClick?: (item: T) => void;
}

export const VirtualizedTable = memo(
  <T>({
    data,
    columns,
    height,
    itemHeight,
    onRowClick,
  }: VirtualizedTableProps<T>) => {
    const Row = useMemo(
      () =>
        ({ index, style }: { index: number; style: React.CSSProperties }) => {
          const item = data[index];

          return (
            <div
              style={style}
              className="table-row"
              onClick={() => onRowClick?.(item)}
            >
              {columns.map((column, colIndex) => (
                <div key={colIndex} className="table-cell">
                  {column.render ? column.render(item) : item[column.key]}
                </div>
              ))}
            </div>
          );
        },
      [data, columns, onRowClick]
    );

    return (
      <div className="virtualized-table">
        <div className="table-header">
          {columns.map((column, index) => (
            <div key={index} className="table-header-cell">
              {column.title}
            </div>
          ))}
        </div>
        <List
          height={height}
          itemCount={data.length}
          itemSize={itemHeight}
          overscanCount={5}
        >
          {Row}
        </List>
      </div>
    );
  }
);

// components/optimized/LazyLoadingCard.tsx
interface LazyLoadingCardProps {
  data: any;
  onVisible?: () => void;
}

export const LazyLoadingCard = memo(
  ({ data, onVisible }: LazyLoadingCardProps) => {
    const [isVisible, setIsVisible] = useState(false);
    const cardRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
      const observer = new IntersectionObserver(
        ([entry]) => {
          if (entry.isIntersecting && !isVisible) {
            setIsVisible(true);
            onVisible?.();
          }
        },
        { threshold: 0.1 }
      );

      if (cardRef.current) {
        observer.observe(cardRef.current);
      }

      return () => observer.disconnect();
    }, [isVisible, onVisible]);

    return (
      <div ref={cardRef} className="lazy-card">
        {isVisible ? (
          <div className="card-content">
            {/* 실제 컨텐츠 렌더링 */}
            <h3>{data.title}</h3>
            <p>{data.description}</p>
          </div>
        ) : (
          <div className="card-skeleton">
            <Skeleton height="20px" mb="2" />
            <Skeleton height="60px" />
          </div>
        )}
      </div>
    );
  }
);

// hooks/useDebounced.ts - 검색 최적화
export function useDebounced<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return debouncedValue;
}

// components/optimized/SearchableSelect.tsx
interface SearchableSelectProps {
  options: Option[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

export const SearchableSelect = memo(
  ({ options, value, onChange, placeholder }: SearchableSelectProps) => {
    const [searchTerm, setSearchTerm] = useState("");
    const debouncedSearchTerm = useDebounced(searchTerm, 300);

    const filteredOptions = useMemo(() => {
      if (!debouncedSearchTerm) return options;

      return options.filter((option) =>
        option.label.toLowerCase().includes(debouncedSearchTerm.toLowerCase())
      );
    }, [options, debouncedSearchTerm]);

    return (
      <Select value={value} onChange={onChange} placeholder={placeholder}>
        <option value="">
          <Input
            placeholder="검색..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onClick={(e) => e.stopPropagation()}
          />
        </option>
        {filteredOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </Select>
    );
  }
);
```

---

이제 모니터링 및 로깅 가이드를 별도 파일로 작성하겠습니다.
