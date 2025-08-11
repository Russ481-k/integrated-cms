# 통합 CMS v2 데이터베이스 접근 권한 정책

## 1. 개요

본 문서는 통합 CMS v2 시스템의 데이터베이스 접근 권한 체계를 정의합니다. 통합 관리 데이터베이스(마스터 DB)와 서비스별 데이터베이스의 접근 권한을 계층화하여 안전하고 효율적인 데이터 관리를 목표로 합니다.

## 2. 권한 계층 구조

### 2.1 사용자 계층
1. 슈퍼 관리자 (Super Administrator)
   - 시스템 최고 권한자
   - 모든 데이터베이스 접근 가능
   - 권한 관리 시스템 총괄

2. 서비스 관리자 (Service Administrator)
   - 특정 서비스의 최고 권한자
   - 통합 DB 읽기/쓰기 권한
   - 해당 서비스 DB 전체 접근 가능
   - 서비스 내 권한 관리 가능

3. 사이트 관리자 (Site Administrator)
   - 각 사이트의 최고 권한자
   - 통합 DB 접근 불가
   - 해당 서비스 DB 내 사이트 영역 접근 가능
   - 사이트 내 권한 관리 가능

4. 일반 관리자 (Regular Administrator)
   - 통합 DB 접근 불가
   - 서비스 DB 내 그룹 권한에 따른 접근
   - 반드시 하나의 그룹에 소속
   - 추가 권한 보유 가능

### 2.2 데이터베이스 접근 레벨

1. 통합 관리 데이터베이스 (integrated_cms)
   - integrated.admin: 전체 테이블 READ/WRITE (슈퍼관리자, 서비스관리자)
   - admin: 접근 불가 (사이트관리자, 일반관리자)

2. 서비스 데이터베이스 (douzone)
   - integrated.admin: 전체 테이블 READ/WRITE (슈퍼관리자, 서비스관리자)
   - admin: 전체 테이블 READ/WRITE (사이트관리자, 일반관리자)

## 3. 접근 권한 매트릭스

### 3.1 통합 관리 데이터베이스

| 테이블 | 슈퍼 관리자 | 서비스 관리자 | 사이트 관리자 | 일반 관리자 |
|--------|------------|--------------|--------------|------------|
| ADMIN_USER | ADMIN | READ | NONE | NONE |
| SERVICE | ADMIN | READ | NONE | NONE |
| SERVICE_GROUP | ADMIN | WRITE* | NONE | NONE |
| SERVICE_MEMBER_GROUP | ADMIN | WRITE* | NONE | NONE |
| SERVICE_PERMISSION | ADMIN | WRITE* | NONE | NONE |
| SERVICE_PERMISSION_LOG | ADMIN | READ* | NONE | NONE |

\* 해당 서비스 범위 내에서만 가능

### 3.2 서비스 데이터베이스

| 구분 | 슈퍼 관리자 | 서비스 관리자 | 사이트 관리자 | 일반 관리자 |
|------|------------|--------------|--------------|------------|
| 시스템 설정 | SYSTEM_ADMIN | SERVICE_ADMIN | NONE | NONE |
| 사이트 관리 | SYSTEM_ADMIN | SERVICE_ADMIN | SITE_ADMIN | NONE |
| 사용자 관리 | SYSTEM_ADMIN | SERVICE_ADMIN | SITE_ADMIN | GROUP_BASED |
| 컨텐츠 관리 | SYSTEM_ADMIN | SERVICE_ADMIN | SITE_ADMIN | GROUP_BASED |
| 통계/로그 | SYSTEM_ADMIN | SERVICE_ADMIN | SITE_ADMIN | READ |

## 4. 권한 정책

### 4.1 기본 원칙
1. 최소 권한 원칙 (Principle of Least Privilege)
   - 필요한 최소한의 권한만 부여
   - 불필요한 권한은 즉시 회수
   - DB 접근은 역할에 따라 엄격히 제한

2. 직무 분리 (Separation of Duties)
   - 권한 부여자와 실행자의 분리
   - 중요 작업은 복수 승인 필요
   - DB 레벨 접근과 애플리케이션 레벨 접근 분리

3. 접근 통제 (Access Control)
   - RBAC(Role-Based Access Control) 기반
   - 동적 권한 할당 지원
   - DB 접근 감사 로깅

### 4.2 데이터베이스 접근 관리

1. 통합 데이터베이스 접근
   - 슈퍼관리자, 서비스관리자: integrated.admin 계정으로 직접 DB 접근 가능
   - 사이트관리자, 일반관리자: 접근 불가

2. 서비스 데이터베이스 접근
   - 슈퍼관리자, 서비스관리자: integrated.admin 계정으로 직접 DB 접근 가능
   - 사이트관리자, 일반관리자: admin 계정으로 직접 DB 접근 가능

### 4.3 권한 검증 절차

1. DB 접근 시 검증 단계
   - 사용자 인증 확인
   - 역할 레벨 확인
   - DB 접근 권한 확인
   - 작업 로깅

2. API 접근 시 검증 단계
   - 토큰 기반 인증
   - 역할 기반 권한 확인
   - 요청 작업 검증
   - 응답 필터링

## 5. 보안 고려사항

1. 데이터베이스 보안
   - 접근 IP 제한
   - 암호화된 연결 강제
   - 주기적 비밀번호 변경
   - 접근 로그 모니터링

2. 애플리케이션 보안
   - API 엔드포인트 보안
   - 요청/응답 데이터 검증
   - 세션 관리
   - 권한 상승 방지

3. 감사 및 모니터링
   - DB 접근 로그 분석
   - 권한 변경 감사
   - 이상 징후 탐지
   - 주기적 권한 검토

## 6. 운영 및 유지보수

1. 정기 점검
   - DB 접근 권한 검토
   - 사용자 역할 검토
   - 미사용 계정 정리
   - 보안 정책 준수 확인

2. 비상 대응
   - 권한 비상 회수 절차
   - DB 접근 차단 절차
   - 보안 사고 대응 체계
   - 복구 계획

3. 교육 및 훈련
   - DB 접근 보안 교육
   - 권한 관리 교육
   - 보안 의식 강화
   - 절차 숙지 훈련