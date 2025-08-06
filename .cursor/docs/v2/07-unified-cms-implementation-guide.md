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

-- 관리자 사용자 테이블
CREATE TABLE admin_users (
    admin_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    full_name VARCHAR(100),
    phone VARCHAR(20),
    department VARCHAR(100),
    position VARCHAR(100),
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_APPROVAL') DEFAULT 'PENDING_APPROVAL',
    last_login_at TIMESTAMP NULL,
    password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    profile_image_url VARCHAR(255),
    timezone VARCHAR(50) DEFAULT 'Asia/Seoul',
    language VARCHAR(10) DEFAULT 'ko',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL,

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- 관리자 그룹 테이블
CREATE TABLE admin_groups (
    group_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL,
    group_code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    group_type ENUM('SYSTEM', 'DEPARTMENT', 'PROJECT', 'CUSTOM') DEFAULT 'CUSTOM',
    parent_group_id BIGINT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    FOREIGN KEY (parent_group_id) REFERENCES admin_groups(group_id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(admin_id),
    INDEX idx_group_code (group_code),
    INDEX idx_parent_group (parent_group_id)
);

-- 관리자 그룹 멤버십 테이블
CREATE TABLE admin_group_members (
    member_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    member_type ENUM('OWNER', 'ADMIN', 'MEMBER') DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,

    FOREIGN KEY (group_id) REFERENCES admin_groups(group_id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    UNIQUE KEY unique_group_admin (group_id, admin_id),
    INDEX idx_group_id (group_id),
    INDEX idx_admin_id (admin_id)
);

-- 역할 정의 테이블
CREATE TABLE roles (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    role_type ENUM('SYSTEM', 'SERVICE', 'CUSTOM') DEFAULT 'CUSTOM',
    is_system_role BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(admin_id),
    INDEX idx_role_code (role_code),
    INDEX idx_role_type (role_type)
);

-- 권한 정의 테이블
CREATE TABLE permissions (
    permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    permission_category VARCHAR(50), -- 'MENU', 'FUNCTION', 'DATA', 'SYSTEM'
    resource_type VARCHAR(50), -- 'board', 'content', 'menu', 'user' 등
    action_type VARCHAR(50), -- 'create', 'read', 'update', 'delete', 'publish' 등
    is_system_permission BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(admin_id),
    INDEX idx_permission_code (permission_code),
    INDEX idx_category_resource (permission_category, resource_type),
    INDEX idx_resource_action (resource_type, action_type)
);

-- 역할-권한 매핑 테이블
CREATE TABLE role_permissions (
    role_permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    is_granted BOOLEAN DEFAULT TRUE,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,

    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES admin_users(admin_id),
    UNIQUE KEY unique_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
);

-- 메뉴 정의 테이블 (서비스별 메뉴 구조 정의)
CREATE TABLE menus (
    menu_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NULL, -- NULL이면 통합 관리용 메뉴
    menu_code VARCHAR(50) NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    menu_path VARCHAR(255),
    parent_menu_id BIGINT NULL,
    menu_level INT DEFAULT 1,
    menu_order INT DEFAULT 0,
    menu_icon VARCHAR(100),
    menu_type ENUM('MENU', 'PAGE', 'FUNCTION', 'BUTTON') DEFAULT 'MENU',
    is_visible BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    required_permissions JSON, -- ["permission_code1", "permission_code2"]
    menu_metadata JSON, -- 추가 메뉴 속성
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    FOREIGN KEY (service_id) REFERENCES services(service_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_menu_id) REFERENCES menus(menu_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES admin_users(admin_id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(admin_id),
    UNIQUE KEY unique_service_menu_code (service_id, menu_code),
    INDEX idx_service_id (service_id),
    INDEX idx_parent_menu (parent_menu_id),
    INDEX idx_menu_level_order (menu_level, menu_order)
);

-- 사용자-서비스-역할 매핑 테이블
CREATE TABLE admin_service_roles (
    assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NULL,
    group_id BIGINT NULL,
    service_id BIGINT NULL, -- NULL이면 통합 관리 시스템
    role_id BIGINT NOT NULL,
    assignment_type ENUM('USER', 'GROUP') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    granted_by BIGINT,
    revoked_at TIMESTAMP NULL,
    revoked_by BIGINT,
    revoke_reason TEXT,

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
    INDEX idx_service_role (service_id, role_id)
);

-- 사용자별 메뉴 권한 오버라이드 테이블
CREATE TABLE admin_menu_permissions (
    menu_permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NULL,
    group_id BIGINT NULL,
    menu_id BIGINT NOT NULL,
    permission_type ENUM('ALLOW', 'DENY') NOT NULL,
    assignment_type ENUM('USER', 'GROUP') NOT NULL,
    specific_permissions JSON, -- 메뉴 내 세부 기능별 권한 ["create", "update", "delete"]
    is_active BOOLEAN DEFAULT TRUE,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    granted_by BIGINT,

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
    INDEX idx_menu_permission (menu_id, permission_type)
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

-- 1. 시스템 관리자 생성
INSERT INTO admin_users (username, password, email, full_name, department, position, status) VALUES
('superadmin', '$2a$10$encrypted_password', 'admin@company.com', 'Super Administrator', 'IT', 'System Administrator', 'ACTIVE');

-- 2. 기본 그룹 생성
INSERT INTO admin_groups (group_name, group_code, description, group_type) VALUES
('시스템 관리자', 'SYSTEM_ADMIN', '시스템 전체 관리자 그룹', 'SYSTEM'),
('서비스 관리자', 'SERVICE_ADMIN', '개별 서비스 관리자 그룹', 'SYSTEM'),
('운영자', 'OPERATOR', '일반 운영자 그룹', 'SYSTEM'),
('개발팀', 'DEVELOPMENT', '개발팀 그룹', 'DEPARTMENT'),
('운영팀', 'OPERATION', '운영팀 그룹', 'DEPARTMENT');

-- 3. 기본 역할 생성
INSERT INTO roles (role_name, role_code, description, role_type, is_system_role) VALUES
('슈퍼 관리자', 'SUPER_ADMIN', '시스템 전체에 대한 모든 권한', 'SYSTEM', TRUE),
('통합 관리자', 'UNIFIED_ADMIN', '통합 관리 시스템의 모든 권한', 'SYSTEM', TRUE),
('서비스 관리자', 'SERVICE_ADMIN', '개별 서비스의 관리 권한', 'SERVICE', TRUE),
('컨텐츠 관리자', 'CONTENT_ADMIN', '컨텐츠 관리 권한', 'SERVICE', TRUE),
('게시판 관리자', 'BOARD_ADMIN', '게시판 관리 권한', 'SERVICE', TRUE),
('메뉴 관리자', 'MENU_ADMIN', '메뉴 관리 권한', 'SERVICE', TRUE),
('사용자 관리자', 'USER_ADMIN', '사용자 관리 권한', 'SERVICE', TRUE),
('운영자', 'OPERATOR', '기본 운영 권한', 'SERVICE', TRUE),
('뷰어', 'VIEWER', '조회 전용 권한', 'SERVICE', TRUE);

-- 4. 기본 권한 생성
INSERT INTO permissions (permission_name, permission_code, description, permission_category, resource_type, action_type, is_system_permission) VALUES
-- 시스템 권한
('시스템 관리', 'SYSTEM_MANAGE', '시스템 전체 관리 권한', 'SYSTEM', 'system', 'manage', TRUE),
('서비스 관리', 'SERVICE_MANAGE', '서비스 등록/수정/삭제 권한', 'SYSTEM', 'service', 'manage', TRUE),
('관리자 관리', 'ADMIN_MANAGE', '관리자 계정 관리 권한', 'SYSTEM', 'admin', 'manage', TRUE),
('그룹 관리', 'GROUP_MANAGE', '그룹 관리 권한', 'SYSTEM', 'group', 'manage', TRUE),
('역할 관리', 'ROLE_MANAGE', '역할 관리 권한', 'SYSTEM', 'role', 'manage', TRUE),
('권한 관리', 'PERMISSION_MANAGE', '권한 관리 권한', 'SYSTEM', 'permission', 'manage', TRUE),
('시스템 설정', 'SYSTEM_CONFIG', '시스템 설정 관리 권한', 'SYSTEM', 'config', 'manage', TRUE),

-- 메뉴 접근 권한
('통합 대시보드 접근', 'MENU_UNIFIED_DASHBOARD', '통합 대시보드 메뉴 접근', 'MENU', 'dashboard', 'access', FALSE),
('서비스 관리 메뉴', 'MENU_SERVICE_MANAGE', '서비스 관리 메뉴 접근', 'MENU', 'service', 'access', FALSE),
('관리자 관리 메뉴', 'MENU_ADMIN_MANAGE', '관리자 관리 메뉴 접근', 'MENU', 'admin', 'access', FALSE),
('컨텐츠 관리 메뉴', 'MENU_CONTENT_MANAGE', '컨텐츠 관리 메뉴 접근', 'MENU', 'content', 'access', FALSE),
('게시판 관리 메뉴', 'MENU_BOARD_MANAGE', '게시판 관리 메뉴 접근', 'MENU', 'board', 'access', FALSE),
('메뉴 관리 메뉴', 'MENU_MENU_MANAGE', '메뉴 관리 메뉴 접근', 'MENU', 'menu', 'access', FALSE),
('사용자 관리 메뉴', 'MENU_USER_MANAGE', '사용자 관리 메뉴 접근', 'MENU', 'user', 'access', FALSE),

-- 기능 권한
('컨텐츠 생성', 'CONTENT_CREATE', '컨텐츠 생성 권한', 'FUNCTION', 'content', 'create', FALSE),
('컨텐츠 조회', 'CONTENT_READ', '컨텐츠 조회 권한', 'FUNCTION', 'content', 'read', FALSE),
('컨텐츠 수정', 'CONTENT_UPDATE', '컨텐츠 수정 권한', 'FUNCTION', 'content', 'update', FALSE),
('컨텐츠 삭제', 'CONTENT_DELETE', '컨텐츠 삭제 권한', 'FUNCTION', 'content', 'delete', FALSE),
('컨텐츠 발행', 'CONTENT_PUBLISH', '컨텐츠 발행 권한', 'FUNCTION', 'content', 'publish', FALSE),

('게시글 생성', 'BOARD_CREATE', '게시글 생성 권한', 'FUNCTION', 'board', 'create', FALSE),
('게시글 조회', 'BOARD_READ', '게시글 조회 권한', 'FUNCTION', 'board', 'read', FALSE),
('게시글 수정', 'BOARD_UPDATE', '게시글 수정 권한', 'FUNCTION', 'board', 'update', FALSE),
('게시글 삭제', 'BOARD_DELETE', '게시글 삭제 권한', 'FUNCTION', 'board', 'delete', FALSE),
('게시글 승인', 'BOARD_APPROVE', '게시글 승인 권한', 'FUNCTION', 'board', 'approve', FALSE),

('메뉴 생성', 'MENU_CREATE', '메뉴 생성 권한', 'FUNCTION', 'menu', 'create', FALSE),
('메뉴 조회', 'MENU_READ', '메뉴 조회 권한', 'FUNCTION', 'menu', 'read', FALSE),
('메뉴 수정', 'MENU_UPDATE', '메뉴 수정 권한', 'FUNCTION', 'menu', 'update', FALSE),
('메뉴 삭제', 'MENU_DELETE', '메뉴 삭제 권한', 'FUNCTION', 'menu', 'delete', FALSE),

('사용자 생성', 'USER_CREATE', '사용자 생성 권한', 'FUNCTION', 'user', 'create', FALSE),
('사용자 조회', 'USER_READ', '사용자 조회 권한', 'FUNCTION', 'user', 'read', FALSE),
('사용자 수정', 'USER_UPDATE', '사용자 수정 권한', 'FUNCTION', 'user', 'update', FALSE),
('사용자 삭제', 'USER_DELETE', '사용자 삭제 권한', 'FUNCTION', 'user', 'delete', FALSE),

('파일 업로드', 'FILE_UPLOAD', '파일 업로드 권한', 'FUNCTION', 'file', 'upload', FALSE),
('파일 삭제', 'FILE_DELETE', '파일 삭제 권한', 'FUNCTION', 'file', 'delete', FALSE);

-- 5. 역할-권한 매핑 (슈퍼 관리자)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_code = 'SUPER_ADMIN';

-- 6. 역할-권한 매핑 (통합 관리자)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_code = 'UNIFIED_ADMIN'
AND p.permission_code NOT IN ('SYSTEM_MANAGE', 'SERVICE_MANAGE', 'ADMIN_MANAGE');

-- 7. 역할-권한 매핑 (서비스 관리자)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_code = 'SERVICE_ADMIN'
AND p.permission_category IN ('MENU', 'FUNCTION')
AND p.resource_type != 'system';

-- 8. 기본 메뉴 구조 생성 (통합 관리용 - service_id = NULL)
INSERT INTO menus (service_id, menu_code, menu_name, menu_path, parent_menu_id, menu_level, menu_order, menu_icon, menu_type, required_permissions) VALUES
(NULL, 'UNIFIED_DASHBOARD', '통합 대시보드', '/unified/dashboard', NULL, 1, 1, 'dashboard', 'MENU', '["MENU_UNIFIED_DASHBOARD"]'),
(NULL, 'SYSTEM_MANAGE', '시스템 관리', '/unified/system', NULL, 1, 2, 'settings', 'MENU', '["SYSTEM_MANAGE"]'),
(NULL, 'SERVICE_MANAGE', '서비스 관리', '/unified/services', NULL, 1, 3, 'server', 'MENU', '["MENU_SERVICE_MANAGE"]'),
(NULL, 'ADMIN_MANAGE', '관리자 관리', '/unified/admin', NULL, 1, 4, 'users', 'MENU', '["MENU_ADMIN_MANAGE"]'),
(NULL, 'CONTENT_MANAGE', '통합 컨텐츠', '/unified/content', NULL, 1, 5, 'file-text', 'MENU', '["MENU_CONTENT_MANAGE"]'),
(NULL, 'MONITORING', '모니터링', '/unified/monitoring', NULL, 1, 6, 'activity', 'MENU', '["SYSTEM_MANAGE"]');

-- 서브메뉴 추가
INSERT INTO menus (service_id, menu_code, menu_name, menu_path, parent_menu_id, menu_level, menu_order, menu_icon, menu_type, required_permissions) VALUES
(NULL, 'ADMIN_USER_MANAGE', '사용자 관리', '/unified/admin/users', (SELECT menu_id FROM menus WHERE menu_code = 'ADMIN_MANAGE' AND service_id IS NULL), 2, 1, 'user', 'PAGE', '["USER_READ"]'),
(NULL, 'ADMIN_GROUP_MANAGE', '그룹 관리', '/unified/admin/groups', (SELECT menu_id FROM menus WHERE menu_code = 'ADMIN_MANAGE' AND service_id IS NULL), 2, 2, 'users', 'PAGE', '["GROUP_MANAGE"]'),
(NULL, 'ADMIN_ROLE_MANAGE', '역할 관리', '/unified/admin/roles', (SELECT menu_id FROM menus WHERE menu_code = 'ADMIN_MANAGE' AND service_id IS NULL), 2, 3, 'shield', 'PAGE', '["ROLE_MANAGE"]'),
(NULL, 'ADMIN_PERMISSION_MANAGE', '권한 관리', '/unified/admin/permissions', (SELECT menu_id FROM menus WHERE menu_code = 'ADMIN_MANAGE' AND service_id IS NULL), 2, 4, 'lock', 'PAGE', '["PERMISSION_MANAGE"]');

-- 9. 시스템 관리자 그룹에 슈퍼 관리자 추가
INSERT INTO admin_group_members (group_id, admin_id, member_type, created_by) VALUES
((SELECT group_id FROM admin_groups WHERE group_code = 'SYSTEM_ADMIN'),
 (SELECT admin_id FROM admin_users WHERE username = 'superadmin'),
 'OWNER',
 (SELECT admin_id FROM admin_users WHERE username = 'superadmin'));

-- 10. 슈퍼 관리자에게 SUPER_ADMIN 역할 부여 (통합 시스템)
INSERT INTO admin_service_roles (admin_id, service_id, role_id, assignment_type, granted_by) VALUES
((SELECT admin_id FROM admin_users WHERE username = 'superadmin'),
 NULL, -- 통합 시스템
 (SELECT role_id FROM roles WHERE role_code = 'SUPER_ADMIN'),
 'USER',
 (SELECT admin_id FROM admin_users WHERE username = 'superadmin'));

-- 11. 기존 서비스 등록 (예시)
INSERT INTO services (service_code, service_name, service_domain, api_base_url, db_connection_info) VALUES
('cms1', 'Main CMS', 'cms1.company.com', 'http://localhost:8081',
 AES_ENCRYPT('{"host":"localhost","port":"3307","database":"service1_cms","username":"cms1_user","password":"secure_pass"}', 'encryption_key')),
('cms2', 'Secondary CMS', 'cms2.company.com', 'http://localhost:8082',
 AES_ENCRYPT('{"host":"localhost","port":"3308","database":"service2_cms","username":"cms2_user","password":"secure_pass"}', 'encryption_key'));

-- 12. 개별 서비스 메뉴 구조 생성 (예: cms1)
INSERT INTO menus (service_id, menu_code, menu_name, menu_path, parent_menu_id, menu_level, menu_order, menu_icon, menu_type, required_permissions) VALUES
((SELECT service_id FROM services WHERE service_code = 'cms1'), 'DASHBOARD', '대시보드', '/dashboard', NULL, 1, 1, 'home', 'MENU', '["MENU_UNIFIED_DASHBOARD"]'),
((SELECT service_id FROM services WHERE service_code = 'cms1'), 'BOARD', '게시판 관리', '/board', NULL, 1, 2, 'message-square', 'MENU', '["MENU_BOARD_MANAGE"]'),
((SELECT service_id FROM services WHERE service_code = 'cms1'), 'CONTENT', '컨텐츠 관리', '/content', NULL, 1, 3, 'file-text', 'MENU', '["MENU_CONTENT_MANAGE"]'),
((SELECT service_id FROM services WHERE service_code = 'cms1'), 'MENU', '메뉴 관리', '/menu', NULL, 1, 4, 'menu', 'MENU', '["MENU_MENU_MANAGE"]'),
((SELECT service_id FROM services WHERE service_code = 'cms1'), 'USER', '사용자 관리', '/user', NULL, 1, 5, 'users', 'MENU', '["MENU_USER_MANAGE"]');
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
