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
   - 해당 서비스 DB 전체 접근 가능
   - 서비스 내 권한 관리 가능

3. 일반 관리자 (Regular Administrator)
   - 반드시 하나의 그룹에 소속
   - 소속 그룹의 기본 권한 보유
   - 추가 권한 보유 가능

### 2.2 데이터베이스 접근 레벨

1. 통합 관리 데이터베이스 (마스터 DB)
   - ADMIN: 전체 테이블 READ/WRITE
   - WRITE: 지정된 테이블 READ/WRITE
   - READ: 지정된 테이블 READ ONLY
   - NONE: 접근 불가

2. 서비스 데이터베이스
   - ADMIN: 서비스 DB 전체 READ/WRITE
   - WRITE: 지정된 테이블/기능 READ/WRITE
   - READ: 지정된 테이블/기능 READ ONLY
   - NONE: 접근 불가

## 3. 접근 권한 매트릭스

### 3.1 통합 관리 데이터베이스

| 테이블 | 슈퍼 관리자 | 서비스 관리자 | 일반 관리자 |
|--------|------------|--------------|------------|
| ADMIN_USER | ADMIN | READ | NONE |
| SERVICE | ADMIN | READ | NONE |
| SERVICE_GROUP | ADMIN | WRITE* | READ |
| SERVICE_MEMBER_GROUP | ADMIN | WRITE* | READ |
| SERVICE_PERMISSION | ADMIN | WRITE* | READ |
| SERVICE_PERMISSION_LOG | ADMIN | READ* | NONE |

\* 해당 서비스 범위 내에서만 가능

### 3.2 서비스 데이터베이스

| 구분 | 슈퍼 관리자 | 서비스 관리자 | 일반 관리자 |
|------|------------|--------------|------------|
| 시스템 설정 | ADMIN | ADMIN | NONE |
| 사용자 관리 | ADMIN | ADMIN | 그룹 권한에 따름 |
| 컨텐츠 관리 | ADMIN | ADMIN | 그룹 권한 + 추가 권한 |
| 통계/로그 | ADMIN | ADMIN | READ |

## 4. 권한 정책

### 4.1 기본 원칙
1. 최소 권한 원칙 (Principle of Least Privilege)
   - 필요한 최소한의 권한만 부여
   - 불필요한 권한은 즉시 회수

2. 직무 분리 (Separation of Duties)
   - 권한 부여자와 실행자의 분리
   - 중요 작업은 복수 승인 필요

3. 접근 통제 (Access Control)
   - RBAC(Role-Based Access Control) 기반
   - 동적 권한 할당 지원

### 4.2 권한 관리 정책

1. 권한 부여
   - 그룹 기반 기본 권한 우선
   - 사용자별 추가 권한 보완
   - 상위 관리자 승인 필수

2. 권한 변경
   - 변경 사유 필수 기록
   - 권한 변경 이력 보관
   - 주기적 권한 검토

3. 권한 회수
   - 퇴직/이동 시 즉시 회수
   - 미사용 권한 정기 검토
   - 회수 이력 관리

### 4.3 모니터링 및 감사

1. 접근 로깅
   - 중요 데이터 접근 기록
   - 권한 변경 이력 추적
   - 비정상 접근 탐지

2. 정기 감사
   - 월간 권한 현황 검토
   - 분기별 권한 적정성 검토
   - 연간 보안 감사 수행

## 5. 구현 가이드라인

### 5.1 데이터베이스 접근 제어

```sql
-- 예시: 서비스별 권한 체크 프로시저
DELIMITER //
CREATE PROCEDURE check_service_permission(
    IN p_user_uuid VARCHAR(36),
    IN p_service_id VARCHAR(36),
    IN p_permission_type VARCHAR(20),
    IN p_target_id VARCHAR(36)
)
BEGIN
    DECLARE v_has_permission BOOLEAN;
    
    -- 1. 통합 관리자 권한 확인
    -- 2. 그룹 기본 권한 확인
    -- 3. 추가 권한 확인
    -- 4. 통합 결과 반환
    
    SELECT EXISTS (
        SELECT 1 FROM SERVICE_PERMISSION
        WHERE USER_UUID = p_user_uuid
        AND SERVICE_ID = p_service_id
        AND PERMISSION_TYPE = p_permission_type
        AND TARGET_ID = p_target_id
        AND STATUS = 'ACTIVE'
    ) INTO v_has_permission;
    
    RETURN v_has_permission;
END //
DELIMITER ;
```

### 5.2 권한 캐싱 전략

1. 캐시 계층
   - Redis 기반 권한 캐싱
   - 사용자별 권한 세트 관리
   - 실시간 권한 변경 반영

2. 캐시 무효화
   - 권한 변경 시 즉시 갱신
   - 정기적 캐시 재구성
   - 장애 대비 백업 전략

## 6. 보안 고려사항

1. 데이터 암호화
   - 중요 정보 암호화 저장
   - 통신 구간 암호화
   - 키 관리 체계 수립

2. 감사 추적
   - 권한 변경 이력 보관
   - 접근 로그 분석
   - 이상 징후 탐지

3. 비상 대응
   - 권한 비상 회수 절차
   - 백업 및 복구 계획
   - 보안 사고 대응 체계

## 7. 운영 및 유지보수

1. 정기 점검
   - 월간 권한 현황 검토
   - 분기별 감사 로그 분석
   - 연간 정책 개선 검토

2. 교육 및 훈련
   - 관리자 권한 교육
   - 보안 의식 강화
   - 절차 숙지 훈련

3. 정책 개선
   - 피드백 수렴 및 반영
   - 보안 요구사항 갱신
   - 정책 최신화 관리