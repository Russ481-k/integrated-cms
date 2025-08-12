# 통합 CMS v2.0 시스템

## 개요

하나의 백엔드에서 여러 서비스를 통합 관리하는 플랫폼입니다. 각 서비스는 독립적인 프론트엔드와 데이터베이스를 가지며, 통합 CMS 백엔드를 통해 관리됩니다.

## 시스템 구조

- **통합 CMS 백엔드 (Java)**: 모든 서비스의 요청을 처리하는 중앙 백엔드
- **통합 CMS DB (integrated_cms)**: 통합 관리용 메타데이터 저장
- **서비스별 DB**: 각 서비스의 독립적인 데이터 저장 (douzone 등)
- **다중 프론트엔드**: 통합 관리자 포털 + 각 서비스별 프론트엔드

## 권한 체계

1. **슈퍼 관리자**: 모든 서비스와 DB에 대한 최고 권한 (integrated.admin 계정)
2. **서비스 관리자**: 특정 서비스 전체 관리 권한 (integrated.admin 계정)
3. **사이트 관리자**: 특정 사이트 관리 권한 (admin 계정)
4. **일반 관리자**: 그룹 기반 제한된 권한 (admin 계정)

## 주요 기능

### 통합 관리자 포털

- 전체 서비스 대시보드 및 모니터링
- 서비스별 사이트 목록 관리
- 통합 사용자 및 권한 관리
- 서비스별 설정 관리

### 권한 관리 시스템

- 계층적 권한 구조 (슈퍼 → 서비스 → 사이트 → 일반)
- 그룹 기반 기본 권한 + 사용자별 추가 권한
- 메뉴, 게시판, 컨텐츠, 사이트 접근 권한 제어
- 실시간 권한 검증 및 감사 로깅

## API 구조

### 라우팅 패턴

- **통합 패턴**: `/api/v2/cms/{serviceId}/**`
- **통합 관리**: `/api/v2/cms/integrated_cms/**` → 통합 CMS DB 접근
- **개별 서비스**: `/api/v2/cms/{serviceId}/**` → 서비스별 DB 접근

### 동적 라우팅 플로우

1. **serviceId 추출 및 검증**: URL에서 serviceId 파라미터 추출
2. **라우팅 분기**:
   - `integrated_cms`: 통합 DB 직접 접근
   - 기타 서비스: 서비스 메타데이터 조회 후 해당 DB 접근
3. **서비스 메타데이터 조회**: integrated_cms.SERVICE 테이블에서 DB 연결정보 획득
4. **적절한 DB 연결**: 서비스별 DB 연결 및 데이터 처리

### API 예시

```
# 통합 관리 기능
GET /api/v2/cms/integrated_cms/service/list
GET /api/v2/cms/integrated_cms/user/permissions

# 개별 서비스 기능
GET /api/v2/cms/douzone/bbs/article/list
POST /api/v2/cms/service1/content/create
PUT /api/v2/cms/service2/menu/update
```
