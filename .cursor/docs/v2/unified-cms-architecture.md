# 통합 CMS 고도화 아키텍처 설계서

## 1. 개요

본 문서는 기존 단일 CMS 시스템을 다중 서비스 통합 관리가 가능한 고도화된 CMS 플랫폼으로 전환하기 위한 아키텍처 설계를 다룹니다.

### 1.1 주요 목표

- **통합 관리**: 여러 CMS 서비스를 하나의 관리자 화면에서 통합 관리
- **서비스 분리**: 각 서비스별 독립적인 데이터베이스와 보안 정책
- **유연한 배포**: 온프레미스 단일 서버 또는 클라우드 다중 서버 지원
- **확장성**: 새로운 서비스 추가 시 최소한의 수정으로 확장 가능

### 1.2 배포 시나리오

- **시나리오 A (온프레미스)**: 단일 서버, 동일 클라이언트 IP
- **시나리오 B (클라우드)**: 다중 서버, 서비스별 독립 IP

---

## 2. 시스템 아키텍처

### 2.1 전체 구성도

```mermaid
graph TB
    subgraph "Client Layer"
        UI[통합 관리자 UI<br/>Next.js]
        C1[Service1 Client]
        C2[Service2 Client]
        CN[ServiceN Client]
    end

    subgraph "API Gateway & Proxy"
        NGINX[Nginx Reverse Proxy]
        GATEWAY[API Gateway<br/>Spring Cloud Gateway]
    end

    subgraph "Backend Services"
        UNIFIED[통합 API 서버<br/>Spring Boot]
        SERVICE1[Service1 API]
        SERVICE2[Service2 API]
        SERVICEN[ServiceN API]
    end

    subgraph "Database Layer"
        MASTER_DB[(통합 메타 DB<br/>서비스 접근 정보)]
        DB1[(Service1 DB)]
        DB2[(Service2 DB)]
        DBN[(ServiceN DB)]
    end

    UI --> NGINX
    C1 --> NGINX
    C2 --> NGINX
    CN --> NGINX

    NGINX --> GATEWAY
    GATEWAY --> UNIFIED
    GATEWAY --> SERVICE1
    GATEWAY --> SERVICE2
    GATEWAY --> SERVICEN

    UNIFIED --> MASTER_DB
    UNIFIED --> DB1
    UNIFIED --> DB2
    UNIFIED --> DBN

    SERVICE1 --> DB1
    SERVICE2 --> DB2
    SERVICEN --> DBN
```

### 2.2 핵심 구성 요소

#### 2.2.1 통합 API 서버 (Unified API Server)

- **역할**: 서비스 라우팅, 인증/인가, 통합 관리 기능
- **기술**: Spring Boot + Spring Security + JPA
- **API 패턴**: `/api/unified/` + `/api/service{N}/`

#### 2.2.2 통합 메타 데이터베이스

- **역할**: 서비스 정보, 접근 권한, 관리자 계정 정보 저장
- **암호화**: 개별 DB 접근 정보는 AES-256으로 암호화 저장

#### 2.2.3 서비스별 독립 데이터베이스

- **특징**: 각 서비스만 접근 가능한 독립적인 DB
- **보안**: 서비스별 전용 DB 계정 및 권한

---

## 3. 데이터베이스 설계

### 3.1 통합 메타 데이터베이스 (Master DB)

#### 3.1.1 서비스 관리 테이블

```sql
-- 서비스 정보 테이블
CREATE TABLE services (
    service_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_code VARCHAR(50) UNIQUE NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    service_domain VARCHAR(255),
    api_base_url VARCHAR(255),
    db_connection_info TEXT, -- 암호화된 DB 접속 정보
    status ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 관리자 계정 테이블
CREATE TABLE admin_users (
    admin_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- bcrypt 해시
    email VARCHAR(100),
    role ENUM('SUPER_ADMIN', 'SERVICE_ADMIN', 'OPERATOR'),
    status ENUM('ACTIVE', 'INACTIVE'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 관리자 서비스 권한 테이블
CREATE TABLE admin_service_permissions (
    permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT,
    service_id BIGINT,
    permissions JSON, -- {"board": ["read", "write"], "content": ["read"]}
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,
    FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id),
    FOREIGN KEY (service_id) REFERENCES services(service_id),
    FOREIGN KEY (granted_by) REFERENCES admin_users(admin_id)
);
```

#### 3.1.2 통합 관리 테이블

```sql
-- 통합 게시글 관리 뷰
CREATE TABLE unified_content_management (
    content_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT,
    original_content_id BIGINT,
    content_type ENUM('BOARD', 'POPUP', 'CONTENT', 'MENU'),
    title VARCHAR(255),
    status VARCHAR(50),
    author VARCHAR(100),
    created_at TIMESTAMP,
    last_synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (service_id) REFERENCES services(service_id)
);

-- 통합 활동 로그
CREATE TABLE unified_activity_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT,
    service_id BIGINT,
    action VARCHAR(100),
    target_type VARCHAR(50),
    target_id BIGINT,
    details JSON,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id),
    FOREIGN KEY (service_id) REFERENCES services(service_id)
);
```

### 3.2 개별 서비스 데이터베이스

각 서비스는 기존 CMS 구조를 유지하되, 통합 관리를 위한 메타데이터 추가:

```sql
-- 기존 테이블에 통합 관리용 컬럼 추가
ALTER TABLE board_articles ADD COLUMN unified_sync_flag BOOLEAN DEFAULT FALSE;
ALTER TABLE board_articles ADD COLUMN unified_last_sync TIMESTAMP NULL;

-- 서비스별 관리자 매핑 테이블
CREATE TABLE service_admin_mapping (
    mapping_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    unified_admin_id BIGINT, -- 통합 DB의 admin_id
    local_admin_id BIGINT,   -- 서비스 DB의 admin_id
    permissions JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 4. API 설계

### 4.1 API 구조

#### 4.1.1 통합 관리 API (`/api/unified/`)

```yaml
# 서비스 관리
GET    /api/unified/services                    # 서비스 목록 조회
POST   /api/unified/services                    # 새 서비스 등록
PUT    /api/unified/services/{serviceId}        # 서비스 정보 수정
DELETE /api/unified/services/{serviceId}        # 서비스 삭제

# 통합 컨텐츠 관리
GET    /api/unified/content                     # 모든 서비스 컨텐츠 통합 조회
GET    /api/unified/content/sync                # 서비스별 컨텐츠 동기화
POST   /api/unified/content/bulk-action         # 일괄 작업 (삭제, 상태 변경 등)

# 통합 팝업 관리
GET    /api/unified/popups                      # 모든 서비스 팝업 통합 조회
POST   /api/unified/popups/bulk-action          # 팝업 일괄 관리

# 관리자 권한 관리
GET    /api/unified/admins                      # 관리자 목록
POST   /api/unified/admins                      # 관리자 생성
PUT    /api/unified/admins/{adminId}/permissions # 권한 설정
```

#### 4.1.2 서비스별 API (`/api/service{N}/`)

```yaml
# 기존 CMS API를 서비스별로 분리
GET    /api/service1/board/articles             # Service1의 게시글
POST   /api/service1/board/articles             # Service1에 게시글 작성
GET    /api/service2/board/articles             # Service2의 게시글
POST   /api/service2/board/articles             # Service2에 게시글 작성

# 각 서비스는 독립적인 인증/인가
# 통합 API에서 적절한 서비스로 라우팅
```

### 4.2 인증/인가 플로우

```mermaid
sequenceDiagram
    participant Admin as 관리자
    participant UI as 통합 UI
    participant Gateway as API Gateway
    participant Unified as 통합 API
    participant Service as 서비스 API
    participant MasterDB as 통합 DB
    participant ServiceDB as 서비스 DB

    Admin->>UI: 로그인 요청
    UI->>Gateway: POST /api/unified/auth/login
    Gateway->>Unified: Forward request
    Unified->>MasterDB: 관리자 인증 및 권한 조회
    MasterDB-->>Unified: 인증 정보 + 서비스별 권한
    Unified-->>Gateway: JWT Token (권한 정보 포함)
    Gateway-->>UI: Token + 접근 가능 서비스 목록

    Admin->>UI: Service1 데이터 요청
    UI->>Gateway: GET /api/service1/board with JWT
    Gateway->>Unified: 권한 검증 요청
    Unified->>MasterDB: Service1 접근 권한 확인
    MasterDB-->>Unified: 권한 정보
    Unified->>ServiceDB: Service1 DB 접근
    ServiceDB-->>Unified: 데이터
    Unified-->>Gateway: 필터링된 데이터
    Gateway-->>UI: 응답
```

---

## 5. 통합 프론트엔드 설계

### 5.1 컴포넌트 구조

```typescript
// 통합 관리 페이지 구조
/src/app/unified/
├── dashboard/                    // 통합 대시보드
│   ├── page.tsx                 // 모든 서비스 종합 현황
│   └── components/
│       ├── ServiceSummaryCard.tsx
│       ├── UnifiedMetrics.tsx
│       └── AlertPanel.tsx
├── content/                     // 통합 컨텐츠 관리
│   ├── page.tsx                // 모든 서비스 컨텐츠 일괄 관리
│   └── components/
│       ├── ContentUnifiedTable.tsx
│       ├── BulkActionPanel.tsx
│       └── ServiceFilter.tsx
├── popup/                      // 통합 팝업 관리
├── admin/                      // 관리자 관리
│   ├── page.tsx               // 관리자 계정 및 권한 관리
│   └── components/
│       ├── AdminList.tsx
│       ├── PermissionMatrix.tsx
│       └── ServiceAccessControl.tsx
└── settings/                   // 서비스 설정
    ├── page.tsx               // 서비스 등록/수정/삭제
    └── components/
        ├── ServiceRegistration.tsx
        ├── DatabaseConnectionTest.tsx
        └── ServiceHealthCheck.tsx
```

### 5.2 핵심 기능 컴포넌트

#### 5.2.1 통합 대시보드

```typescript
// UnifiedDashboard.tsx
interface UnifiedMetrics {
  totalServices: number;
  totalContents: number;
  activePopups: number;
  todayActiveUsers: number;
  systemHealth: "HEALTHY" | "WARNING" | "ERROR";
  serviceStatuses: ServiceStatus[];
}

export function UnifiedDashboard() {
  const { data: metrics } = useUnifiedMetrics();
  const { data: services } = useServices();

  return (
    <Grid templateColumns="repeat(4, 1fr)" gap={6}>
      <MetricCard title="총 서비스" value={metrics.totalServices} />
      <MetricCard title="총 컨텐츠" value={metrics.totalContents} />
      <MetricCard title="활성 팝업" value={metrics.activePopups} />
      <MetricCard title="오늘 사용자" value={metrics.todayActiveUsers} />

      <GridItem colSpan={4}>
        <ServiceHealthPanel services={services} />
      </GridItem>

      <GridItem colSpan={2}>
        <ContentDistributionChart />
      </GridItem>

      <GridItem colSpan={2}>
        <RecentActivityFeed />
      </GridItem>
    </Grid>
  );
}
```

#### 5.2.2 권한 관리 매트릭스

```typescript
// PermissionMatrix.tsx
interface PermissionMatrixProps {
  adminId: number;
  services: Service[];
  permissions: AdminPermission[];
  onPermissionChange: (
    serviceId: number,
    module: string,
    actions: string[]
  ) => void;
}

export function PermissionMatrix({
  adminId,
  services,
  permissions,
  onPermissionChange,
}: PermissionMatrixProps) {
  const modules = ["board", "content", "popup", "menu", "user"];
  const actions = ["read", "write", "delete", "publish"];

  return (
    <Table>
      <Thead>
        <Tr>
          <Th>서비스</Th>
          {modules.map((module) => (
            <Th key={module}>{module}</Th>
          ))}
        </Tr>
      </Thead>
      <Tbody>
        {services.map((service) => (
          <Tr key={service.id}>
            <Td>{service.name}</Td>
            {modules.map((module) => (
              <Td key={module}>
                <PermissionCheckboxGroup
                  serviceId={service.id}
                  module={module}
                  availableActions={actions}
                  currentPermissions={getPermissions(
                    permissions,
                    service.id,
                    module
                  )}
                  onChange={(actions) =>
                    onPermissionChange(service.id, module, actions)
                  }
                />
              </Td>
            ))}
          </Tr>
        ))}
      </Tbody>
    </Table>
  );
}
```

---

## 6. 보안 및 접근 제어

### 6.1 다중 레벨 보안

#### 6.1.1 네트워크 레벨

```nginx
# Nginx 설정 - IP 기반 접근 제어
server {
    listen 443 ssl;
    server_name unified-cms.domain.com;

    # 관리자 IP만 허용
    allow 192.168.1.0/24;
    allow 10.0.0.0/8;
    deny all;

    location /api/unified/ {
        proxy_pass http://unified-api:8080;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/service1/ {
        auth_request /auth-service1;
        proxy_pass http://service1-api:8081;
    }
}
```

#### 6.1.2 애플리케이션 레벨

```java
// 통합 보안 설정
@Configuration
@EnableWebSecurity
public class UnifiedSecurityConfig {

    @Bean
    public SecurityFilterChain unifiedFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/api/unified/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/unified/auth/**").permitAll()
                .requestMatchers("/api/unified/admin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/unified/**").hasAnyRole("SUPER_ADMIN", "SERVICE_ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
            .build();
    }
}

// 서비스별 보안 설정
@Component
public class ServiceSecurityManager {

    public boolean hasServicePermission(String adminId, String serviceCode, String module, String action) {
        // 통합 DB에서 권한 조회
        AdminServicePermission permission = permissionRepository
            .findByAdminIdAndServiceCode(adminId, serviceCode);

        if (permission == null) return false;

        Map<String, List<String>> permissions = permission.getPermissions();
        List<String> modulePermissions = permissions.get(module);

        return modulePermissions != null && modulePermissions.contains(action);
    }
}
```

### 6.2 데이터베이스 접근 제어

#### 6.2.1 암호화된 연결 정보 관리

```java
@Service
public class DatabaseConnectionService {

    @Autowired
    private EncryptionService encryptionService;

    public DataSource getServiceDataSource(String serviceCode) {
        Service service = serviceRepository.findByServiceCode(serviceCode);

        // 암호화된 연결 정보 복호화
        String decryptedConnectionInfo = encryptionService.decrypt(service.getDbConnectionInfo());
        DatabaseConnectionInfo dbInfo = JsonUtils.fromJson(decryptedConnectionInfo, DatabaseConnectionInfo.class);

        // 동적 DataSource 생성
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbInfo.getJdbcUrl());
        config.setUsername(dbInfo.getUsername());
        config.setPassword(dbInfo.getPassword());
        config.setMaximumPoolSize(5); // 서비스별 제한

        return new HikariDataSource(config);
    }
}
```

#### 6.2.2 계정별 권한 분리

```sql
-- 서비스별 데이터베이스 계정 생성
CREATE USER 'service1_readonly'@'%' IDENTIFIED BY 'secure_password1';
CREATE USER 'service1_admin'@'%' IDENTIFIED BY 'secure_password2';

-- 읽기 전용 권한
GRANT SELECT ON service1_db.* TO 'service1_readonly'@'%';

-- 관리자 권한 (CRUD)
GRANT SELECT, INSERT, UPDATE, DELETE ON service1_db.* TO 'service1_admin'@'%';

-- 서비스 간 접근 차단
REVOKE ALL ON service2_db.* FROM 'service1_readonly'@'%';
REVOKE ALL ON service2_db.* FROM 'service1_admin'@'%';
```

---

## 7. 구현 로드맵

### 7.1 Phase 1: 기반 구조 구축 (4주)

1. **통합 메타 데이터베이스 설계 및 구축**

   - 서비스 등록 테이블
   - 관리자 권한 관리 테이블
   - 암호화 시스템 구현

2. **API Gateway 구현**

   - Spring Cloud Gateway 설정
   - 라우팅 규칙 정의
   - 인증/인가 필터 구현

3. **기본 통합 API 구현**
   - 서비스 등록/관리 API
   - 관리자 인증/권한 API
   - 헬스체크 API

### 7.2 Phase 2: 통합 관리 기능 구현 (6주)

1. **통합 프론트엔드 구현**

   - 통합 대시보드
   - 서비스 관리 화면
   - 관리자 권한 관리 화면

2. **통합 컨텐츠 관리**

   - 게시글 일괄 관리
   - 팝업 통합 관리
   - 메뉴 통합 관리

3. **권한 관리 시스템**
   - 세분화된 권한 제어
   - 권한 매트릭스 UI
   - 활동 로그 시스템

### 7.3 Phase 3: 고급 기능 및 최적화 (4주)

1. **성능 최적화**

   - 데이터베이스 연결 풀 최적화
   - 캐싱 시스템 도입 (Redis)
   - 비동기 처리 개선

2. **모니터링 및 알림**

   - 서비스 헬스체크 자동화
   - 장애 감지 및 알림
   - 성능 메트릭 수집

3. **보안 강화**
   - 2FA 인증 도입
   - 감사 로그 강화
   - 침입 탐지 시스템

---

## 8. 기존 코드 마이그레이션 가이드

### 8.1 백엔드 마이그레이션

#### 8.1.1 기존 Controller 수정

```java
// 기존 코드
@RestController
@RequestMapping("/cms/content")
public class ContentController {
    // ...
}

// 수정된 코드
@RestController
@RequestMapping("/api/{serviceCode}/content")
public class ServiceContentController {

    @Autowired
    private ServiceSecurityManager securityManager;

    @Autowired
    private DynamicDataSourceService dataSourceService;

    @GetMapping
    public ResponseEntity<Page<ContentDto>> getContents(
            @PathVariable String serviceCode,
            Pageable pageable,
            Authentication auth) {

        // 권한 체크
        if (!securityManager.hasServicePermission(auth.getName(), serviceCode, "content", "read")) {
            throw new AccessDeniedException("No permission for this service");
        }

        // 동적 데이터소스 설정
        try (DataSourceContext context = dataSourceService.setDataSource(serviceCode)) {
            Page<ContentDto> contents = contentService.getContents(pageable);
            return ResponseEntity.ok(contents);
        }
    }
}
```

#### 8.1.2 동적 데이터소스 구현

```java
@Component
public class DynamicDataSourceService {

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    public DataSourceContext setDataSource(String serviceCode) {
        DataSource dataSource = getOrCreateDataSource(serviceCode);
        DataSourceContextHolder.setDataSource(dataSource);
        return new DataSourceContext();
    }

    private DataSource getOrCreateDataSource(String serviceCode) {
        return dataSourceCache.computeIfAbsent(serviceCode, this::createDataSource);
    }

    // DataSourceContext - try-with-resources 지원
    public static class DataSourceContext implements AutoCloseable {
        @Override
        public void close() {
            DataSourceContextHolder.clearDataSource();
        }
    }
}
```

### 8.2 프론트엔드 마이그레이션

#### 8.2.1 API 클라이언트 수정

```typescript
// 기존 API 클라이언트
const contentApi = {
  getContents: () => axios.get("/cms/content"),
  createContent: (data) => axios.post("/cms/content", data),
};

// 수정된 API 클라이언트
const createServiceApi = (serviceCode: string) => ({
  content: {
    getContents: () => axios.get(`/api/${serviceCode}/content`),
    createContent: (data) => axios.post(`/api/${serviceCode}/content`, data),
  },
  board: {
    getArticles: () => axios.get(`/api/${serviceCode}/board/articles`),
    createArticle: (data) =>
      axios.post(`/api/${serviceCode}/board/articles`, data),
  },
});

// 서비스별 API 인스턴스 생성
export const service1Api = createServiceApi("service1");
export const service2Api = createServiceApi("service2");

// 통합 API
export const unifiedApi = {
  services: {
    getAll: () => axios.get("/api/unified/services"),
    create: (data) => axios.post("/api/unified/services", data),
  },
  content: {
    getUnified: () => axios.get("/api/unified/content"),
    bulkAction: (action, ids) =>
      axios.post("/api/unified/content/bulk-action", { action, ids }),
  },
};
```

#### 8.2.2 권한 기반 UI 렌더링

```typescript
// 권한 기반 컴포넌트
interface ServicePermissionProps {
  serviceCode: string;
  module: string;
  action: string;
  children: React.ReactNode;
}

export function ServicePermission({
  serviceCode,
  module,
  action,
  children,
}: ServicePermissionProps) {
  const { permissions } = useAuth();

  const hasPermission = permissions
    ?.find((p) => p.serviceCode === serviceCode)
    ?.modules[module]?.includes(action);

  if (!hasPermission) return null;

  return <>{children}</>;
}

// 사용 예시
<ServicePermission serviceCode="service1" module="content" action="write">
  <Button onClick={handleCreateContent}>새 컨텐츠 작성</Button>
</ServicePermission>;
```

---

## 9. 결론

이 통합 CMS 고도화 아키텍처는 다음과 같은 핵심 가치를 제공합니다:

1. **확장성**: 새로운 서비스 추가 시 최소한의 수정
2. **보안성**: 다중 레벨 보안 및 서비스별 격리
3. **유연성**: 온프레미스/클라우드 다양한 배포 환경 지원
4. **관리효율성**: 통합 관리 화면을 통한 운영 효율 극대화
5. **기존 호환성**: 기존 CMS 기능의 점진적 마이그레이션 지원

이 설계를 통해 기존 CMS의 장점을 유지하면서도, 다중 서비스 환경에서의 통합 관리가 가능한 차세대 CMS 플랫폼을 구축할 수 있습니다.
