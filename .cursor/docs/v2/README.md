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

## API 구조 예시

### 통합 관리 API
- `GET /api/v1/cms/integrated/services` - 전체 서비스 목록 조회
- `GET /api/v1/cms/integrated/users` - 통합 관리자 목록 조회
- `POST /api/v1/cms/integrated/permissions` - 통합 권한 설정
- `GET /api/v1/cms/integrated/dashboard` - 전체 시스템 대시보드

### 서비스별 API  
- `GET /api/v1/cms/douzone/board/articles` - douzone 서비스 게시글 목록
- `GET /api/v1/cms/service1/users` - service1 사용자 목록
- `POST /api/v1/cms/service2/content` - service2 컨텐츠 생성
- `PUT /api/v1/cms/douzone/menu/{id}` - douzone 메뉴 수정

### API 라우팅 플로우
1. **통합 관리**: `/cms/integrated/**` → integrated_cms DB 직접 접근
2. **서비스별**: `/cms/{serviceId}/**` → 메타데이터 조회 → 서비스 DB 동적 접근