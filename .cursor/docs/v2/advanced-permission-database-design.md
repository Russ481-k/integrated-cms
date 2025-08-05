# 고도화된 권한 시스템 데이터베이스 설계

## 1. 데이터베이스 설계 원칙

### 1.1 설계 목표

통합 CMS의 권한 시스템은 다음 원칙을 기반으로 설계됩니다:

- **확장성**: 새로운 서비스와 권한 추가가 용이해야 함
- **성능**: 권한 검증이 빠르게 수행되어야 함
- **보안**: 최소 권한 원칙과 명시적 거부 우선 적용
- **감사**: 모든 권한 변경사항이 추적 가능해야 함
- **재사용성**: 개별 사이트에서도 동일한 구조 적용 가능

### 1.2 권한 모델 특징

- **계층적 구조**: 그룹 → 사용자, 역할 → 권한의 상속
- **명시적 제어**: ALLOW/DENY 명시적 권한 설정
- **시간 제한**: 권한 만료 시간 설정 가능
- **조건부 권한**: 특정 조건에서만 유효한 권한
- **메뉴 단위 세분화**: 메뉴별 기능별 세부 권한 제어

## 2. 핵심 테이블 설계

### 2.1 관리자 사용자 테이블 (admin_users)

관리자 계정의 기본 정보를 저장하며, 개별 사이트와 통합 사이트 모두에서 사용됩니다.

```sql
CREATE TABLE admin_users (
    admin_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '관리자 고유 ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '로그인용 사용자 이름',
    password VARCHAR(255) NOT NULL COMMENT 'bcrypt로 해시된 비밀번호',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '이메일 주소',
    full_name VARCHAR(100) NOT NULL COMMENT '전체 이름',
    phone VARCHAR(20) COMMENT '전화번호',
    department VARCHAR(100) COMMENT '부서',
    position VARCHAR(100) COMMENT '직책',
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_APPROVAL') DEFAULT 'PENDING_APPROVAL' COMMENT '계정 상태',
    last_login_at TIMESTAMP NULL COMMENT '마지막 로그인 시간',
    password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '비밀번호 변경 시간',
    failed_login_attempts INT DEFAULT 0 COMMENT '연속 로그인 실패 횟수',
    locked_until TIMESTAMP NULL COMMENT '계정 잠금 해제 시간',
    profile_image_url VARCHAR(255) COMMENT '프로필 이미지 URL',
    timezone VARCHAR(50) DEFAULT 'Asia/Seoul' COMMENT '시간대',
    language VARCHAR(10) DEFAULT 'ko' COMMENT '언어 설정',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID',
    deleted_at TIMESTAMP NULL COMMENT '삭제 시각 (소프트 삭제)',

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_department (department),
    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(admin_id)
) COMMENT '관리자 사용자 기본 정보';
```

### 2.2 관리자 그룹 테이블 (admin_groups)

부서별, 프로젝트별, 또는 커스텀 그룹을 통한 권한 관리를 지원합니다.

```sql
CREATE TABLE admin_groups (
    group_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '그룹 고유 ID',
    group_name VARCHAR(100) NOT NULL COMMENT '그룹 이름',
    group_code VARCHAR(50) UNIQUE NOT NULL COMMENT '그룹 코드',
    description TEXT COMMENT '그룹 설명',
    group_type ENUM('SYSTEM', 'DEPARTMENT', 'PROJECT', 'CUSTOM') DEFAULT 'CUSTOM' COMMENT '그룹 유형',
    parent_group_id BIGINT NULL COMMENT '상위 그룹 ID (계층 구조 지원)',
    group_level INT DEFAULT 1 COMMENT '그룹 계층 레벨',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 여부',
    max_members INT DEFAULT 0 COMMENT '최대 멤버 수 (0=무제한)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID',

    FOREIGN KEY (parent_group_id) REFERENCES admin_groups(group_id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(admin_id),
    INDEX idx_group_code (group_code),
    INDEX idx_group_type (group_type),
    INDEX idx_parent_group (parent_group_id),
    INDEX idx_active (is_active)
) COMMENT '관리자 그룹 정보';
```

### 2.3 그룹 멤버십 테이블 (admin_group_members)

관리자와 그룹 간의 소속 관계를 관리합니다.

```sql
CREATE TABLE admin_group_members (
    member_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '멤버십 고유 ID',
    group_id BIGINT NOT NULL COMMENT '그룹 ID',
    admin_id BIGINT NOT NULL COMMENT '관리자 ID',
    member_type ENUM('OWNER', 'ADMIN', 'MEMBER') DEFAULT 'MEMBER' COMMENT '멤버 유형',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '가입 시간',
    expires_at TIMESTAMP NULL COMMENT '멤버십 만료 시간',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 여부',
    created_by BIGINT COMMENT '등록자 ID',

    FOREIGN KEY (group_id) REFERENCES admin_groups(group_id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    UNIQUE KEY unique_group_admin (group_id, admin_id),
    INDEX idx_group_id (group_id),
    INDEX idx_admin_id (admin_id),
    INDEX idx_member_type (member_type),
    INDEX idx_active (is_active)
) COMMENT '그룹 멤버십 관계';
```

### 2.4 역할 정의 테이블 (roles)

시스템 내에서 사용되는 모든 역할을 정의합니다.

```sql
CREATE TABLE roles (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '역할 고유 ID',
    role_name VARCHAR(100) NOT NULL COMMENT '역할 이름',
    role_code VARCHAR(50) UNIQUE NOT NULL COMMENT '역할 코드',
    description TEXT COMMENT '역할 설명',
    role_type ENUM('SYSTEM', 'SERVICE', 'CUSTOM') DEFAULT 'CUSTOM' COMMENT '역할 유형',
    role_level INT DEFAULT 1 COMMENT '역할 레벨 (높을수록 상위 역할)',
    is_system_role BOOLEAN DEFAULT FALSE COMMENT '시스템 역할 여부 (삭제 불가)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 여부',
    max_duration_days INT DEFAULT 0 COMMENT '최대 할당 기간 (일, 0=무제한)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID',

    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(admin_id),
    INDEX idx_role_code (role_code),
    INDEX idx_role_type (role_type),
    INDEX idx_role_level (role_level),
    INDEX idx_active (is_active)
) COMMENT '역할 정의';
```

### 2.5 권한 정의 테이블 (permissions)

시스템의 모든 세부 권한을 정의합니다.

```sql
CREATE TABLE permissions (
    permission_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '권한 고유 ID',
    permission_name VARCHAR(100) NOT NULL COMMENT '권한 이름',
    permission_code VARCHAR(100) UNIQUE NOT NULL COMMENT '권한 코드',
    description TEXT COMMENT '권한 설명',
    permission_category VARCHAR(50) NOT NULL COMMENT '권한 카테고리 (MENU, FUNCTION, DATA, SYSTEM)',
    resource_type VARCHAR(50) NOT NULL COMMENT '리소스 타입 (board, content, menu, user 등)',
    action_type VARCHAR(50) NOT NULL COMMENT '액션 타입 (create, read, update, delete, publish 등)',
    resource_pattern VARCHAR(255) COMMENT '리소스 패턴 (정규식 지원)',
    condition_expression TEXT COMMENT '권한 조건 식 (SpEL 지원)',
    is_system_permission BOOLEAN DEFAULT FALSE COMMENT '시스템 권한 여부 (삭제 불가)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 여부',
    priority_order INT DEFAULT 100 COMMENT '우선순위 (낮을수록 우선)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID',

    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(admin_id),
    INDEX idx_permission_code (permission_code),
    INDEX idx_category_resource (permission_category, resource_type),
    INDEX idx_resource_action (resource_type, action_type),
    INDEX idx_active (is_active),
    INDEX idx_priority (priority_order)
) COMMENT '권한 정의';
```

### 2.6 역할-권한 매핑 테이블 (role_permissions)

역할에 포함되는 권한들을 매핑합니다.

```sql
CREATE TABLE role_permissions (
    role_permission_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '역할-권한 매핑 고유 ID',
    role_id BIGINT NOT NULL COMMENT '역할 ID',
    permission_id BIGINT NOT NULL COMMENT '권한 ID',
    is_granted BOOLEAN DEFAULT TRUE COMMENT '권한 부여 여부 (DENY 권한 지원)',
    condition_expression TEXT COMMENT '조건부 권한 식',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '권한 부여 시간',
    granted_by BIGINT COMMENT '권한 부여자 ID',

    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES admin_users(admin_id),
    UNIQUE KEY unique_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id),
    INDEX idx_granted (is_granted)
) COMMENT '역할-권한 매핑';
```

### 2.7 메뉴 정의 테이블 (menus)

서비스별 메뉴 구조를 정의하고 접근 권한을 관리합니다.

```sql
CREATE TABLE menus (
    menu_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '메뉴 고유 ID',
    service_id BIGINT NULL COMMENT '서비스 ID (NULL이면 통합 관리용 메뉴)',
    menu_code VARCHAR(50) NOT NULL COMMENT '메뉴 코드',
    menu_name VARCHAR(100) NOT NULL COMMENT '메뉴 이름',
    menu_path VARCHAR(255) COMMENT '메뉴 경로',
    parent_menu_id BIGINT NULL COMMENT '상위 메뉴 ID',
    menu_level INT DEFAULT 1 COMMENT '메뉴 레벨',
    menu_order INT DEFAULT 0 COMMENT '메뉴 순서',
    menu_icon VARCHAR(100) COMMENT '메뉴 아이콘',
    menu_type ENUM('MENU', 'PAGE', 'FUNCTION', 'BUTTON') DEFAULT 'MENU' COMMENT '메뉴 타입',
    required_permissions JSON COMMENT '메뉴 접근에 필요한 권한 목록',
    available_functions JSON COMMENT '메뉴에서 사용 가능한 기능 목록',
    menu_metadata JSON COMMENT '추가 메뉴 속성 (설정값, 옵션 등)',
    is_visible BOOLEAN DEFAULT TRUE COMMENT '화면 표시 여부',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID',

    FOREIGN KEY (service_id) REFERENCES services(service_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_menu_id) REFERENCES menus(menu_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(admin_id),
    UNIQUE KEY unique_service_menu_code (service_id, menu_code),
    INDEX idx_service_id (service_id),
    INDEX idx_parent_menu (parent_menu_id),
    INDEX idx_menu_level_order (menu_level, menu_order),
    INDEX idx_active (is_active)
) COMMENT '메뉴 정의';
```

### 2.8 사용자-서비스-역할 매핑 테이블 (admin_service_roles)

관리자에게 서비스별로 역할을 할당합니다.

```sql
CREATE TABLE admin_service_roles (
    assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '할당 고유 ID',
    admin_id BIGINT NULL COMMENT '관리자 ID',
    group_id BIGINT NULL COMMENT '그룹 ID',
    service_id BIGINT NULL COMMENT '서비스 ID (NULL이면 통합 시스템)',
    role_id BIGINT NOT NULL COMMENT '역할 ID',
    assignment_type ENUM('USER', 'GROUP') NOT NULL COMMENT '할당 타입',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 여부',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '권한 부여 시간',
    expires_at TIMESTAMP NULL COMMENT '권한 만료 시간',
    granted_by BIGINT COMMENT '권한 부여자 ID',
    revoked_at TIMESTAMP NULL COMMENT '권한 철회 시간',
    revoked_by BIGINT COMMENT '권한 철회자 ID',
    revoke_reason TEXT COMMENT '권한 철회 사유',
    assignment_note TEXT COMMENT '할당 참고사항',

    FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES admin_groups(group_id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES services(service_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (revoked_by) REFERENCES admin_users(admin_id),
    CHECK (
        (assignment_type = 'USER' AND admin_id IS NOT NULL AND group_id IS NULL) OR
        (assignment_type = 'GROUP' AND admin_id IS NULL AND group_id IS NOT NULL)
    ),
    UNIQUE KEY unique_user_service_role (admin_id, service_id, role_id),
    UNIQUE KEY unique_group_service_role (group_id, service_id, role_id),
    INDEX idx_admin_service (admin_id, service_id),
    INDEX idx_group_service (group_id, service_id),
    INDEX idx_service_role (service_id, role_id),
    INDEX idx_active (is_active),
    INDEX idx_expires (expires_at)
) COMMENT '사용자-서비스-역할 매핑';
```

### 2.9 메뉴별 세부 권한 테이블 (admin_menu_permissions)

메뉴 단위의 세분화된 권한을 관리합니다.

```sql
CREATE TABLE admin_menu_permissions (
    menu_permission_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '메뉴 권한 고유 ID',
    admin_id BIGINT NULL COMMENT '관리자 ID',
    group_id BIGINT NULL COMMENT '그룹 ID',
    menu_id BIGINT NOT NULL COMMENT '메뉴 ID',
    permission_type ENUM('ALLOW', 'DENY') NOT NULL COMMENT '권한 타입 (허용/거부)',
    assignment_type ENUM('USER', 'GROUP') NOT NULL COMMENT '할당 타입',
    specific_permissions JSON COMMENT '메뉴 내 세부 기능별 권한 ["create", "update", "delete"]',
    permission_scope ENUM('FULL', 'PARTIAL', 'READ_ONLY') DEFAULT 'FULL' COMMENT '권한 범위',
    condition_expression TEXT COMMENT '조건부 권한 식',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 여부',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '권한 부여 시간',
    expires_at TIMESTAMP NULL COMMENT '권한 만료 시간',
    granted_by BIGINT COMMENT '권한 부여자 ID',
    permission_note TEXT COMMENT '권한 부여 참고사항',

    FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES admin_groups(group_id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES menus(menu_id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES admin_users(admin_id),
    CHECK (
        (assignment_type = 'USER' AND admin_id IS NOT NULL AND group_id IS NULL) OR
        (assignment_type = 'GROUP' AND admin_id IS NULL AND group_id IS NOT NULL)
    ),
    INDEX idx_admin_menu (admin_id, menu_id),
    INDEX idx_group_menu (group_id, menu_id),
    INDEX idx_menu_permission (menu_id, permission_type),
    INDEX idx_active (is_active),
    INDEX idx_expires (expires_at)
) COMMENT '메뉴별 세부 권한';
```

### 2.10 권한 감사 로그 테이블 (permission_audit_logs)

모든 권한 변경사항을 추적합니다.

```sql
CREATE TABLE permission_audit_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '로그 고유 ID',
    audit_type ENUM('ROLE_ASSIGN', 'ROLE_REVOKE', 'PERMISSION_GRANT', 'PERMISSION_DENY', 'LOGIN', 'LOGOUT', 'ACCESS_DENIED') NOT NULL COMMENT '감사 유형',
    target_admin_id BIGINT NULL COMMENT '대상 관리자 ID',
    target_group_id BIGINT NULL COMMENT '대상 그룹 ID',
    service_id BIGINT NULL COMMENT '서비스 ID',
    role_id BIGINT NULL COMMENT '역할 ID',
    permission_id BIGINT NULL COMMENT '권한 ID',
    menu_id BIGINT NULL COMMENT '메뉴 ID',
    actor_admin_id BIGINT NOT NULL COMMENT '작업 수행자 ID',
    action_description TEXT NOT NULL COMMENT '작업 설명',
    before_value JSON COMMENT '변경 전 값',
    after_value JSON COMMENT '변경 후 값',
    client_ip VARCHAR(45) COMMENT '클라이언트 IP',
    user_agent TEXT COMMENT 'User Agent',
    session_id VARCHAR(255) COMMENT '세션 ID',
    request_id VARCHAR(255) COMMENT '요청 ID (트레이싱용)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',

    FOREIGN KEY (target_admin_id) REFERENCES admin_users(admin_id),
    FOREIGN KEY (target_group_id) REFERENCES admin_groups(group_id),
    FOREIGN KEY (service_id) REFERENCES services(service_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id),
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id),
    FOREIGN KEY (menu_id) REFERENCES menus(menu_id),
    FOREIGN KEY (actor_admin_id) REFERENCES admin_users(admin_id),
    INDEX idx_audit_type (audit_type),
    INDEX idx_target_admin (target_admin_id),
    INDEX idx_actor_admin (actor_admin_id),
    INDEX idx_service (service_id),
    INDEX idx_created_at (created_at),
    INDEX idx_client_ip (client_ip)
) COMMENT '권한 변경 감사 로그';
```

## 3. 초기 데이터 설정

### 3.1 기본 시스템 역할

```sql
-- 시스템 기본 역할 생성
INSERT INTO roles (role_name, role_code, description, role_type, is_system_role, role_level) VALUES
('슈퍼 관리자', 'SUPER_ADMIN', '시스템 전체에 대한 모든 권한', 'SYSTEM', TRUE, 100),
('통합 관리자', 'UNIFIED_ADMIN', '통합 관리 시스템의 모든 권한', 'SYSTEM', TRUE, 90),
('서비스 관리자', 'SERVICE_ADMIN', '개별 서비스의 관리 권한', 'SERVICE', TRUE, 80),
('컨텐츠 관리자', 'CONTENT_ADMIN', '컨텐츠 관리 권한', 'SERVICE', TRUE, 70),
('게시판 관리자', 'BOARD_ADMIN', '게시판 관리 권한', 'SERVICE', TRUE, 60),
('메뉴 관리자', 'MENU_ADMIN', '메뉴 관리 권한', 'SERVICE', TRUE, 50),
('사용자 관리자', 'USER_ADMIN', '사용자 관리 권한', 'SERVICE', TRUE, 40),
('운영자', 'OPERATOR', '기본 운영 권한', 'SERVICE', TRUE, 30),
('뷰어', 'VIEWER', '조회 전용 권한', 'SERVICE', TRUE, 10);
```

### 3.2 기본 시스템 그룹

```sql
-- 시스템 기본 그룹 생성
INSERT INTO admin_groups (group_name, group_code, description, group_type) VALUES
('시스템 관리자', 'SYSTEM_ADMIN', '시스템 전체 관리자 그룹', 'SYSTEM'),
('서비스 관리자', 'SERVICE_ADMIN', '개별 서비스 관리자 그룹', 'SYSTEM'),
('운영자', 'OPERATOR', '일반 운영자 그룹', 'SYSTEM'),
('개발팀', 'DEVELOPMENT', '개발팀 그룹', 'DEPARTMENT'),
('운영팀', 'OPERATION', '운영팀 그룹', 'DEPARTMENT'),
('고객지원팀', 'SUPPORT', '고객지원팀 그룹', 'DEPARTMENT');
```

### 3.3 기본 권한 정의

```sql
-- 시스템 권한
INSERT INTO permissions (permission_name, permission_code, description, permission_category, resource_type, action_type, is_system_permission) VALUES
('시스템 관리', 'SYSTEM_MANAGE', '시스템 전체 관리 권한', 'SYSTEM', 'system', 'manage', TRUE),
('서비스 관리', 'SERVICE_MANAGE', '서비스 등록/수정/삭제 권한', 'SYSTEM', 'service', 'manage', TRUE),
('관리자 관리', 'ADMIN_MANAGE', '관리자 계정 관리 권한', 'SYSTEM', 'admin', 'manage', TRUE),
('그룹 관리', 'GROUP_MANAGE', '그룹 관리 권한', 'SYSTEM', 'group', 'manage', TRUE),
('역할 관리', 'ROLE_MANAGE', '역할 관리 권한', 'SYSTEM', 'role', 'manage', TRUE),
('권한 관리', 'PERMISSION_MANAGE', '권한 관리 권한', 'SYSTEM', 'permission', 'manage', TRUE),

-- 메뉴 접근 권한
('통합 대시보드 접근', 'MENU_UNIFIED_DASHBOARD', '통합 대시보드 메뉴 접근', 'MENU', 'dashboard', 'access', FALSE),
('서비스 관리 메뉴', 'MENU_SERVICE_MANAGE', '서비스 관리 메뉴 접근', 'MENU', 'service', 'access', FALSE),
('관리자 관리 메뉴', 'MENU_ADMIN_MANAGE', '관리자 관리 메뉴 접근', 'MENU', 'admin', 'access', FALSE),
('컨텐츠 관리 메뉴', 'MENU_CONTENT_MANAGE', '컨텐츠 관리 메뉴 접근', 'MENU', 'content', 'access', FALSE),
('게시판 관리 메뉴', 'MENU_BOARD_MANAGE', '게시판 관리 메뉴 접근', 'MENU', 'board', 'access', FALSE),

-- 기능 권한
('컨텐츠 생성', 'CONTENT_CREATE', '컨텐츠 생성 권한', 'FUNCTION', 'content', 'create', FALSE),
('컨텐츠 조회', 'CONTENT_READ', '컨텐츠 조회 권한', 'FUNCTION', 'content', 'read', FALSE),
('컨텐츠 수정', 'CONTENT_UPDATE', '컨텐츠 수정 권한', 'FUNCTION', 'content', 'update', FALSE),
('컨텐츠 삭제', 'CONTENT_DELETE', '컨텐츠 삭제 권한', 'FUNCTION', 'content', 'delete', FALSE),
('컨텐츠 발행', 'CONTENT_PUBLISH', '컨텐츠 발행 권한', 'FUNCTION', 'content', 'publish', FALSE);
```

## 4. 권한 검증 플로우

### 4.1 권한 검증 순서

1. **캐시 확인**: Redis에서 권한 결과 조회
2. **명시적 거부 검사**: DENY 권한 존재 시 즉시 거부
3. **직접 권한 검사**: 사용자에게 직접 할당된 권한 확인
4. **그룹 권한 검사**: 사용자 그룹의 권한 확인
5. **역할 권한 검사**: 할당된 역할의 기본 권한 확인
6. **기본 거부**: 모든 조건 실패 시 거부

### 4.2 메뉴 접근 권한 검증

1. **메뉴 존재 및 활성화 확인**
2. **메뉴별 명시적 DENY 권한 검사**
3. **메뉴별 명시적 ALLOW 권한 검사**
4. **메뉴 필수 권한 검사**
5. **역할 기반 권한 검사**

### 4.3 세부 기능 권한 검증

1. **기본 메뉴 접근 권한 확인**
2. **기능별 명시적 DENY 권한 검사**
3. **기능별 명시적 ALLOW 권한 검사**
4. **역할 기반 기능 권한 검사**

## 5. 성능 최적화 전략

### 5.1 인덱스 최적화

- 복합 인덱스를 통한 권한 검증 쿼리 최적화
- 권한 검증에 자주 사용되는 컬럼에 단일 인덱스 생성
- 시간 기반 조회를 위한 created_at, expires_at 인덱스

### 5.2 캐싱 전략

- **권한 결과 캐싱**: 15분 TTL로 권한 검증 결과 캐싱
- **메뉴 트리 캐싱**: 서비스별 메뉴 구조 캐싱
- **역할 권한 캐싱**: 역할별 권한 목록 캐싱

### 5.3 쿼리 최적화

- 권한 검증을 위한 최소한의 JOIN 사용
- 서브쿼리 대신 EXISTS 절 활용
- 배치 권한 검증을 위한 IN 절 활용

## 6. 확장성 고려사항

### 6.1 개별 사이트 적용

- 동일한 테이블 구조를 개별 사이트에 적용
- service_id를 통한 권한 범위 제한
- 통합 DB와의 동기화 메커니즘

### 6.2 새로운 권한 타입 추가

- permission_category를 통한 새로운 권한 유형 추가
- JSON 필드를 활용한 유연한 권한 속성 정의
- 조건부 권한을 위한 SpEL 표현식 지원

### 6.3 대용량 처리

- 파티셔닝을 통한 대용량 로그 테이블 관리
- 읽기 전용 복제본을 통한 권한 검증 성능 향상
- 배치 처리를 통한 권한 일괄 업데이트

이 설계를 통해 통합 CMS에서 요구되는 모든 권한 관리 기능을 구조적으로 완전하게 구현할 수 있습니다.
