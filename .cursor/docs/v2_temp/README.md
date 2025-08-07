# 통합 CMS 문서 세트 v2.0

## 📚 문서 개요

본 문서 세트는 기존 단일 CMS를 **다중 서비스 통합 관리**가 가능한 차세대 CMS 플랫폼으로 고도화하기 위한 완전한 설계 및 구현 가이드입니다.

### 🎯 주요 특징

- **RBAC + ABAC 하이브리드 권한 시스템**
- **마이크로서비스 아키텍처**
- **통합 메타 DB + 개별 서비스 DB 구조**
- **API Gateway 기반 라우팅**
- **Next.js + Spring Boot 기술 스택**

---

## 📖 문서 구조

### 🏗️ **핵심 아키텍처** (01-03)

#### [01. 시스템 개요](./01-unified-cms-overview.md)

- 프로젝트 목적 및 전체 시스템 개요
- 핵심 기능 및 기대 효과
- 기술 스택 및 배포 시나리오

#### [02. 상세 아키텍처](./02-unified-cms-architecture.md)

- 시스템 구성 요소 및 상호 작용
- 보안 모델 및 데이터 흐름
- 확장성 및 성능 고려사항

#### [03. 데이터베이스 설계](./03-unified-cms-database-design.md)

- 통합 메타 DB + 개별 서비스 DB 스키마
- 외래키 제약조건 및 테이블 생성 순서
- 고급 권한 시스템 DB 설계

### 🔧 **설계 명세서** (04-06)

#### [04. API 명세서](./04-unified-cms-api-specification.md)

- REST API 엔드포인트 설계
- 인증/인가 및 보안 정책
- API 테스팅 및 성능 최적화

#### [05. 프론트엔드 명세서](./05-unified-cms-frontend-specification.md)

- Next.js 기반 통합 관리 UI
- 컴포넌트 설계 및 상태 관리
- 성능 최적화 및 접근성

#### [06. 고급 권한 시스템](./06-unified-cms-permission-system.md)

- RBAC + ABAC 하이브리드 모델
- 권한 검증 흐름 및 API 설계
- 그룹 관리 및 성능 최적화

### 🚀 **구현 및 운영** (07-09)

#### [07. 구현 가이드](./07-unified-cms-implementation-guide.md)

- 단계별 구현 방법론
- 기술 스택 설정 및 개발 환경
- 코드 구조 및 베스트 프랙티스

#### [08. 배포 가이드](./08-unified-cms-deployment-guide.md)

- CI/CD 파이프라인 구성
- 환경별 배포 전략 (Dev/Staging/Prod)
- Docker 컨테이너 및 Kubernetes 설정

#### [09. 마이그레이션 가이드](./09-unified-cms-migration-guide.md)

- 기존 CMS 전환 전략
- Strangler Fig 패턴 적용
- 데이터 마이그레이션 및 롤백 계획

### 📊 **품질 보증** (10-12)

#### [10. 테스팅 및 성능](./10-unified-cms-testing-performance.md)

- 단위/통합/E2E 테스트 전략
- 성능 테스트 및 최적화
- 부하 테스트 및 용량 계획

#### [11. 모니터링 및 로깅](./11-unified-cms-monitoring-logging.md)

- Prometheus + Grafana 모니터링
- ELK Stack 로그 분석
- 알림 시스템 및 대시보드

#### [12. 운영 가이드](./12-unified-cms-operations-guide.md)

- 장애 대응 및 복구 절차
- 백업 및 재해 복구
- 보안 감사 및 유지보수

---

## 🎯 활용 가이드

### 📋 **문서 읽기 순서**

#### 🔰 **기획자/관리자**

1. [01. 시스템 개요](./01-unified-cms-overview.md) - 전체 시스템 이해
2. [06. 고급 권한 시스템](./06-unified-cms-permission-system.md) - 권한 관리 기능
3. [09. 마이그레이션 가이드](./09-unified-cms-migration-guide.md) - 전환 계획

#### 👨‍💻 **아키텍트/시니어 개발자**

1. [02. 상세 아키텍처](./02-unified-cms-architecture.md) - 시스템 설계
2. [03. 데이터베이스 설계](./03-unified-cms-database-design.md) - DB 구조
3. [04. API 명세서](./04-unified-cms-api-specification.md) - API 설계
4. [07. 구현 가이드](./07-unified-cms-implementation-guide.md) - 구현 방법

#### 🛠️ **개발자**

1. [01. 시스템 개요](./01-unified-cms-overview.md) - 기본 이해
2. [07. 구현 가이드](./07-unified-cms-implementation-guide.md) - 개발 환경
3. [04. API 명세서](./04-unified-cms-api-specification.md) - API 사용법
4. [05. 프론트엔드 명세서](./05-unified-cms-frontend-specification.md) - UI 개발

#### 🔧 **DevOps/운영자**

1. [08. 배포 가이드](./08-unified-cms-deployment-guide.md) - 인프라 설정
2. [11. 모니터링 및 로깅](./11-unified-cms-monitoring-logging.md) - 모니터링 설정
3. [12. 운영 가이드](./12-unified-cms-operations-guide.md) - 운영 절차
4. [10. 테스팅 및 성능](./10-unified-cms-testing-performance.md) - 성능 관리

#### 🧪 **QA/테스터**

1. [01. 시스템 개요](./01-unified-cms-overview.md) - 기능 이해
2. [10. 테스팅 및 성능](./10-unified-cms-testing-performance.md) - 테스트 전략
3. [06. 고급 권한 시스템](./06-unified-cms-permission-system.md) - 권한 테스트

---

## 📈 버전 정보

### 📅 **v2.0.0 (2024-03-25) - 최신**

- ✅ 구버전 문서 정리 및 중복 제거
- ✅ 일관된 명명 규칙 적용 (`NN-unified-cms-xxxxx.md`)
- ✅ 고급 권한 시스템 3개 문서 → 1개 통합 문서
- ✅ 모든 문서에 헤더 정보 및 상호 참조 추가
- ✅ 학습/구현 순서에 따른 번호 체계 도입

### 📅 **v1.x.x (2024-03-01~20)**

- 초기 아키텍처 설계
- 개별 모듈별 문서 작성
- 분산된 권한 시스템 설계

---

## 🔗 관련 자료

### 📚 **외부 참조**

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Next.js Documentation](https://nextjs.org/docs)
- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [OWASP 보안 가이드라인](https://owasp.org/www-project-web-security-testing-guide/)

### 🏛️ **표준 프레임워크**

- [전자정부 표준프레임워크](https://www.egovframe.go.kr/guide/guide.jsp)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

---

## 📞 문의 및 지원

### 📋 **문서 관련 문의**

- 문서 개선 제안
- 기술적 질문
- 구현 관련 지원

### 🔄 **문서 업데이트**

- 새로운 요구사항 반영
- 기술 변경 사항 업데이트
- 사용자 피드백 반영

---

## ⚡ 빠른 시작

### 🏃‍♂️ **5분 만에 이해하기**

1. **[시스템 개요](./01-unified-cms-overview.md)** 읽기 (5분)
2. **아키텍처 다이어그램** 확인
3. **핵심 기능** 파악

### 🚀 **30분 만에 설계 파악하기**

1. [상세 아키텍처](./02-unified-cms-architecture.md) (15분)
2. [데이터베이스 설계](./03-unified-cms-database-design.md) (10분)
3. [권한 시스템](./06-unified-cms-permission-system.md) (5분)

### 💻 **1시간 만에 개발 시작하기**

1. [구현 가이드](./07-unified-cms-implementation-guide.md) (30분)
2. [API 명세서](./04-unified-cms-api-specification.md) (20분)
3. [프론트엔드 명세서](./05-unified-cms-frontend-specification.md) (10분)

---

**🎉 통합 CMS v2.0으로 차세대 콘텐츠 관리 플랫폼을 구축해보세요!**
