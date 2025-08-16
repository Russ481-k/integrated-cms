# 🏊‍♂️ Swimming Lesson CMS

수영강습 관리 시스템 - 고성능 동시성 제어 및 실시간 업데이트 지원

## 📋 **시스템 개요**

이 시스템은 수영강습 신청, 결제, 관리를 위한 종합적인 CMS 솔루션입니다.
동시성 제어, 성능 최적화, 실시간 업데이트 기능을 통해 안정적이고 효율적인 서비스를 제공합니다.

### **주요 기능**

- 🔒 **동시성 제어**: Race Condition 해결 및 정원 관리
- ⚡ **성능 최적화**: HikariCP, Connection Pool 최적화
- 📡 **실시간 업데이트**: WebSocket 기반 정원 현황 실시간 알림
- 📊 **모니터링**: Spring Actuator 기반 성능 메트릭 수집
- 🔐 **보안**: JWT 인증, NICE 본인인증 연동
- 💳 **결제**: KISPG 결제 시스템 연동

## 🛠️ **기술 스택**

### **Backend**

- **Framework**: Spring Boot 2.7.18
- **Database**: MariaDB 10.3+
- **Connection Pool**: HikariCP
- **Authentication**: JWT + NICE CheckPlus
- **Payment**: KISPG
- **Monitoring**: Spring Actuator + Prometheus
- **Real-time**: WebSocket

### **주요 변경사항 (v2.2)**

- ❌ **Redis 제거**: 세션 관리 단순화
- ✅ **기본 세션**: 서블릿 세션 관리 사용
- ✅ **단일 DB**: Read Replica 제거, 단일 DataSource

## 🚀 **Quick Start**

### **필수 요구사항**

- Java 8+
- Maven 3.6+
- MariaDB 10.3+

### **설치 및 실행**

1. **프로젝트 클론**

   ```bash
   git clone [repository-url]
   cd server
   ```

2. **환경 설정**

   ```bash
   # .env 파일 생성
   cp .env.example .env

   # 데이터베이스 및 필수 설정 입력
   nano .env
   ```

3. **데이터베이스 설정**

   ```sql
   CREATE DATABASE cms_new CHARACTER SET utf8mb4;
   CREATE USER 'cms_user'@'%' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON cms_new.* TO 'cms_user'@'%';
   ```

4. **애플리케이션 실행**

   ```bash
   # 개발 환경
   mvn spring-boot:run -Dspring-boot.run.profiles=dev

   # 프로덕션 환경
   mvn clean package -Pprod
   java -jar target/handy-new-cms.jar --spring.profiles.active=prod
   ```

## 📊 **핵심 기능**

### **동시성 제어**

- **비관적 락**: 강습별 배타적 잠금
- **재시도 메커니즘**: 데드락 자동 복구
- **트랜잭션 격리**: SERIALIZABLE 수준

```java
@Retryable(value = {DeadlockLoserDataAccessException.class},
           maxAttempts = 3, backoff = @Backoff(delay = 1000))
@Transactional(isolation = Isolation.SERIALIZABLE)
public EnrollDto completeEnrollment(Long enrollId, PaymentCompleteDto dto)
```

### **실시간 업데이트**

WebSocket을 통한 강습 정원 현황 실시간 브로드캐스트

```javascript
const socket = new WebSocket("ws://localhost:8080/ws/lesson-capacity");
socket.onmessage = function (event) {
  const data = JSON.parse(event.data);
  // 정원 현황 업데이트
};
```

### **성능 모니터링**

- **Actuator 엔드포인트**: `/actuator/health`, `/actuator/metrics`
- **커스텀 메트릭**: 신청 성공률, 평균 처리시간, 데드락 재시도율
- **헬스 체크**: 데이터베이스 응답시간 모니터링

## 📁 **프로젝트 구조**

```
src/main/java/cms/
├── config/                 # 설정 클래스
│   ├── PerformanceMonitoringConfig.java
│   └── EgovConfigAppDataSource.java
├── enroll/                 # 신청 관리
│   ├── service/
│   └── controller/
├── websocket/              # 실시간 통신
│   ├── handler/
│   └── config/
└── ...

Docs/cms/                   # 개발 문서
├── enrollment-system.md    # 시스템 아키텍처
├── initial-server-setup-checklist.md
└── ...
```

## 🔧 **개발 환경 설정**

### **IDE 설정**

- **Lombok**: IDE에 Lombok 플러그인 설치 필요
- **JPA 지원**: Hibernate/JPA 플러그인 권장

### **데이터베이스 설정**

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/cms_new
    username: cms_user
    password: your_password
    hikari:
      maximum-pool-size: 70
      minimum-idle: 10
```

### **세션 관리 (Redis 제거 후)**

```yaml
spring:
  session:
    timeout: 1800 # 30 minutes
    cookie:
      max-age: 1800
```

## 📊 **성능 지표**

| 메트릭          | 목표 값 | 설명                       |
| --------------- | ------- | -------------------------- |
| 신청 성공률     | > 99%   | 전체 신청 대비 성공 비율   |
| 평균 응답시간   | < 2초   | 신청 완료까지 소요 시간    |
| 데드락 재시도율 | < 5%    | 동시성 충돌로 인한 재시도  |
| 동시 사용자     | 50+     | 동시 처리 가능한 사용자 수 |

## 🚀 **배포**

### **프로덕션 빌드**

```bash
mvn clean package -Pprod
```

### **Docker 배포 (선택사항)**

```bash
docker build -t cms-app .
docker run -d -p 8080:8080 --name cms-app cms-app
```

### **시스템 서비스 등록**

```bash
sudo systemctl enable cms.service
sudo systemctl start cms.service
```

## 🚀 **새로운 서비스 추가 방법**

시스템은 중앙 통합 데이터베이스(`integrated_cms`)와 개별 서비스별 데이터베이스(`douzone` 등)로 구성됩니다.
새로운 서비스를 추가할 때는 다음 단계를 따르세요:

### **1. 데이터베이스 생성**

```sql
CREATE DATABASE IF NOT EXISTS service_name;
```

### **2. 권한 부여**

```sql
-- 통합 관리자 권한 부여
GRANT ALL PRIVILEGES ON service_name.* TO 'interated.admin'@'%';

-- 개별 서비스 사용자 권한 부여
GRANT ALL PRIVILEGES ON service_name.* TO 'admin'@'%';

-- 권한 적용
FLUSH PRIVILEGES;
```

### **3. 환경변수 추가** (필요시)

```bash
# .env 파일에 추가
SERVICE_NAME_DATASOURCE_URL=jdbc:mariadb://db:3306/service_name?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
SERVICE_NAME_DB_USERNAME=admin
SERVICE_NAME_DB_PASSWORD=your_password
```

### **4. 데이터소스 설정 업데이트**

새로운 서비스가 별도의 데이터소스가 필요한 경우, `EgovConfigAppDataSource.java`에서 해당 설정을 추가하세요.

## 📚 **문서**

- [🏗️ 시스템 아키텍처](./Docs/cms/enrollment-system.md)
- [⚙️ 서버 설정 가이드](./Docs/cms/initial-server-setup-checklist.md)
- [💳 KISPG 결제 연동](./Docs/cms/kispg-payment-integration.md)
- [🔐 NICE 본인인증](./Docs/cms/NICE_CheckPlus_Integration_Guide.md)

## 🔧 **API 문서**

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Actuator**: `http://localhost:8080/actuator`

## ⚠️ **주의사항**

### **Redis 제거로 인한 제약사항**

- **세션 공유 불가**: 다중 인스턴스 운영 시 세션 분산 처리 제한
- **스케일 아웃 제한**: 단일 인스턴스 운영 권장
- **캐싱**: 애플리케이션 레벨 캐싱으로 대체

### **개발 시 고려사항**

- 트랜잭션 범위 최소화
- WebSocket 연결 관리
- 메모리 사용량 모니터링

## 🐛 **트러블슈팅**

### **일반적인 문제**

**데이터베이스 연결 실패**

```bash
# MariaDB 서비스 확인
sudo systemctl status mariadb
netstat -tlnp | grep 3306
```

**메모리 부족**

```bash
# 메모리 사용량 확인
free -h
ps aux --sort=-%mem | head
```

**데드락 발생**

- 로그 확인: `DeadlockLoserDataAccessException`
- 재시도 메트릭 확인: `/actuator/enrollment-metrics`

## 📞 **지원**

- **기술 문의**: dev@company.com
- **시스템 관리**: sysadmin@company.com

## 🧪 **테스트 실행**

### 전체 테스트 실행

```bash
# Docker 컨테이너에서 전체 테스트 실행 + 상세 결과 리포트
docker exec unitedcms-integrated-backend-1 bash -c "cd /app && mvn test -q && ./test-results.sh"

# 컨테이너가 중지된 경우 재시작
docker-compose up -d integrated-backend

# 개별 테스트 클래스 실행 (단위 테스트)
docker exec unitedcms-integrated-backend-1 mvn test -Dtest="ServiceContextHolderStandardTest" -q

# 여러 테스트 클래스 실행
docker exec unitedcms-integrated-backend-1 mvn test -Dtest="ServiceContextHolderStandardTest,ServiceRepositoryStandardTest" -q

# 특정 패키지 테스트 실행
docker exec unitedcms-integrated-backend-1 mvn test -Dtest="api.v2.integrated_cms.**" -q

# 통합 테스트만 실행
docker exec unitedcms-integrated-backend-1 mvn test -Dtest="*IntegrationTest" -q

# 테스트 커버리지 포함 실행
docker exec unitedcms-integrated-backend-1 mvn clean test jacoco:report -q
```

### 테스트 디버깅 명령어

```bash
# 상세 로그와 함께 테스트 실행
docker exec unitedcms-integrated-backend-1 mvn test -Dtest="ServiceContextHolderStandardTest" -X

# 특정 프로파일로 테스트 실행
docker exec unitedcms-integrated-backend-1 mvn test -Ptest -q

# 실패한 테스트만 재실행
docker exec unitedcms-integrated-backend-1 mvn surefire:test -q

# 테스트 결과 XML 파일 확인
docker exec unitedcms-integrated-backend-1 ls -la target/surefire-reports/
```

### 테스트 결과 해석

- ✅ **성공**: 테스트가 정상적으로 통과됨
- ❌ **실패**: assertion 실패로 인한 테스트 실패
- 🚨 **에러**: 런타임 예외로 인한 테스트 중단
- ⏭️ **스킵**: 조건에 의해 건너뛴 테스트

### 테스트 구조

```
server/src/test/java/
├── testutils/           # 테스트 유틸리티
│   ├── logging/         # 표준화된 로깅 유틸리티 (TestLoggingUtils)
│   ├── base/           # 기본 테스트 클래스들
│   │   ├── BaseTestCase.java      # 공통 테스트 설정
│   │   ├── BaseUnitTest.java      # 단위 테스트 기본 클래스
│   │   ├── BaseRepositoryTest.java # Repository 테스트 기본 클래스
│   │   └── BaseIntegrationTest.java # 통합 테스트 기본 클래스
│   └── config/         # 테스트 설정 (Mock Bean 등)
└── api/v2/             # 실제 테스트 코드
    ├── common/         # 공통 기능 테스트
    ├── integrated_cms/ # 통합 CMS 테스트
    └── cms/           # 서비스별 CMS 테스트
```

### 테스트 리포트 스크립트

`test-results.sh` 스크립트는 Maven Surefire 보고서를 파싱하여 종합적인 테스트 결과를 제공합니다:

```bash
# 스크립트 직접 실행
docker exec unitedcms-integrated-backend-1 ./test-results.sh

# 테스트 후 자동 리포트 생성
docker exec unitedcms-integrated-backend-1 bash -c "mvn test -q && ./test-results.sh"
```

**Modern Test Report Example:**

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                           UNIFIED CMS v2 TEST SUITE                         ║
╚══════════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────────────────┐
│                              TEST RESULTS SUMMARY                          │
└─────────────────────────────────────────────────────────────────────────────┘

  Test Class Name                       │ PASS │ FAIL │ ERR │ SKIP │ STATUS
  ─────────────────────────────────────┼──────┼──────┼─────┼──────┼────────
  ● ServiceContextHolderStandardTest    │   6  │   0  │  0  │   0  │ PASS
  ● ServiceRepositoryStandardTest       │   4  │   0  │  0  │   0  │ PASS
  ● ServiceDescriptionTest              │   4  │   0  │  0  │   0  │ PASS

┌─────────────────────────────────────────────────────────────────────────────┐
│                              OVERALL STATISTICS                            │
└─────────────────────────────────────────────────────────────────────────────┘

  ◆ Total Tests      : 14
  ◆ Passed          : 14 (100.0%)
  ◆ Failed          : 0
  ◆ Errors          : 0
  ◆ Skipped         : 0

╔══════════════════════════════════════════════════════════════════════════════╗
║                          ✓ ALL TESTS PASSED                                ║
╚══════════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────────────────┐
│                         ⬢ BUILD SUCCESSFUL                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 📝 **변경 이력**

| 버전     | 날짜           | 주요 변경사항                    |
| -------- | -------------- | -------------------------------- |
| v1.0     | 2025-01-15     | 기본 CMS 시스템                  |
| v2.0     | 2025-03-15     | 동시성 제어 추가                 |
| v2.1     | 2025-04-01     | 실시간 업데이트                  |
| **v2.2** | **2025-05-23** | **Redis 제거, 세션 관리 단순화** |

---
