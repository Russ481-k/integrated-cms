#!/bin/bash
set -e

echo "🔐 데이터베이스 사용자 계정 및 권한 통합 생성 시작"

# 환경변수 기본값 설정
INTEGRATED_USERNAME=${INTEGRATED_DB_USERNAME:-integrated_admin}
INTEGRATED_PASSWORD=${INTEGRATED_DB_PASSWORD:-integrated123!}
DOUZONE_USERNAME=${DOUZONE_DB_USERNAME:-admin}
DOUZONE_PASSWORD=${DOUZONE_DB_PASSWORD:-admin123!}

echo "생성할 계정 정보:"
echo "  - 통합 관리자: $INTEGRATED_USERNAME (integrated_cms + douzone 접근)"
echo "  - douzone 관리자: $DOUZONE_USERNAME (douzone만 접근)"

# =================================================================
# 1. 통합 CMS 관리자 계정 생성 (모든 DB 접근 가능)
# =================================================================
echo ""
echo "📋 1. 통합 CMS 관리자 계정 생성 중..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
-- 기존 계정 삭제 (있을 경우)
DROP USER IF EXISTS '$INTEGRATED_USERNAME'@'%';
DROP USER IF EXISTS '$INTEGRATED_USERNAME'@'localhost';

-- 통합 관리자 계정 생성
CREATE USER '$INTEGRATED_USERNAME'@'%' IDENTIFIED BY '$INTEGRATED_PASSWORD';
CREATE USER '$INTEGRATED_USERNAME'@'localhost' IDENTIFIED BY '$INTEGRATED_PASSWORD';

-- 모든 데이터베이스에 대한 권한 부여
GRANT ALL PRIVILEGES ON integrated_cms.* TO '$INTEGRATED_USERNAME'@'%';
GRANT ALL PRIVILEGES ON integrated_cms.* TO '$INTEGRATED_USERNAME'@'localhost';
GRANT ALL PRIVILEGES ON douzone.* TO '$INTEGRATED_USERNAME'@'%';
GRANT ALL PRIVILEGES ON douzone.* TO '$INTEGRATED_USERNAME'@'localhost';

-- 권한 즉시 적용
FLUSH PRIVILEGES;
EOSQL

echo "  ✅ 통합 CMS 관리자 계정 생성 완료"
echo "     - 계정명: $INTEGRATED_USERNAME"
echo "     - 접근 가능 DB: integrated_cms, douzone"

# =================================================================
# 2. douzone 전용 관리자 계정 생성 (douzone DB만 접근)
# =================================================================
echo ""
echo "📋 2. douzone 전용 관리자 계정 생성 중..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
-- 기존 계정 삭제 (있을 경우)
DROP USER IF EXISTS '$DOUZONE_USERNAME'@'%';
DROP USER IF EXISTS '$DOUZONE_USERNAME'@'localhost';

-- douzone 전용 계정 생성
CREATE USER '$DOUZONE_USERNAME'@'%' IDENTIFIED BY '$DOUZONE_PASSWORD';
CREATE USER '$DOUZONE_USERNAME'@'localhost' IDENTIFIED BY '$DOUZONE_PASSWORD';

-- douzone 데이터베이스에만 권한 부여
GRANT ALL PRIVILEGES ON douzone.* TO '$DOUZONE_USERNAME'@'%';
GRANT ALL PRIVILEGES ON douzone.* TO '$DOUZONE_USERNAME'@'localhost';

-- 권한 즉시 적용
FLUSH PRIVILEGES;
EOSQL

echo "  ✅ douzone 전용 관리자 계정 생성 완료"
echo "     - 계정명: $DOUZONE_USERNAME"
echo "     - 접근 가능 DB: douzone만"

# =================================================================
# 3. integrated_cms의 admin_user 테이블에 초기 관리자 데이터 삽입
# =================================================================
echo ""
echo "📋 3. integrated_cms admin_user 테이블 초기 데이터 생성 중..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" integrated_cms << EOSQL
-- admin_user 테이블에 기본 관리자 계정들 삽입 (중복 시 무시)
INSERT IGNORE INTO admin_user (
    UUID, USERNAME, NAME, EMAIL, PASSWORD, ROLE, STATUS, 
    CREATED_AT, UPDATED_AT, MEMO, MEMO_UPDATED_AT, MEMO_UPDATED_BY
) VALUES 
-- 슈퍼 관리자 (비밀번호: password)
('super-admin-uuid-0000-0000-000000000001', 'superadmin', 'Super Administrator', 
 'superadmin@integrated-cms.kr', '\$2a\$10\$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'SUPER_ADMIN', 'ACTIVE', NOW(), NOW(), '통합 CMS 슈퍼 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001'),

-- 서비스 관리자 (비밀번호: password)
('service-admin-uuid-0000-0000-000000000002', 'serviceadmin', 'Service Administrator', 
 'serviceadmin@integrated-cms.kr', '\$2a\$10\$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'SERVICE_ADMIN', 'ACTIVE', NOW(), NOW(), '서비스별 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001'),

-- 사이트 관리자 (비밀번호: password)
('site-admin-uuid-0000-0000-000000000003', 'siteadmin', 'Site Administrator', 
 'siteadmin@integrated-cms.kr', '\$2a\$10\$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'SITE_ADMIN', 'ACTIVE', NOW(), NOW(), '사이트별 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001'),

-- 일반 관리자 (비밀번호: password)
('admin-uuid-0000-0000-000000000004', 'admin', 'Administrator', 
 'admin@integrated-cms.kr', '\$2a\$10\$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'ADMIN', 'ACTIVE', NOW(), NOW(), '일반 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001');
EOSQL

echo "  ✅ integrated_cms admin_user 초기 데이터 생성 완료"
echo "     - 생성된 관리자: superadmin, serviceadmin, siteadmin, admin"

# =================================================================
# 4. 권한 확인 및 요약
# =================================================================
echo ""
echo "📋 4. 생성된 계정 권한 확인..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
-- 생성된 사용자 계정 확인
SELECT 
    User as '계정명',
    Host as '접속 허용 호스트',
    plugin as '인증 플러그인'
FROM mysql.user 
WHERE User IN ('$INTEGRATED_USERNAME', '$DOUZONE_USERNAME')
ORDER BY User, Host;

-- 데이터베이스별 권한 확인
SELECT 
    User as '사용자',
    Host as '호스트',
    Db as '데이터베이스',
    Select_priv as '조회권한',
    Insert_priv as '삽입권한',
    Update_priv as '수정권한',
    Delete_priv as '삭제권한'
FROM mysql.db 
WHERE User IN ('$INTEGRATED_USERNAME', '$DOUZONE_USERNAME')
ORDER BY User, Db;
EOSQL

echo ""
echo "🎉 데이터베이스 사용자 계정 및 권한 설정 완료!"
echo ""
echo "📊 생성된 계정 요약:"
echo "  1. $INTEGRATED_USERNAME"
echo "     - 통합 관리자 계정 (SUPER_ADMIN, SERVICE_ADMIN 용)"
echo "     - 접근 가능 DB: integrated_cms, douzone"
echo "     - 비밀번호: $INTEGRATED_PASSWORD"
echo ""
echo "  2. $DOUZONE_USERNAME"
echo "     - douzone 서비스 전용 계정 (SITE_ADMIN, ADMIN 용)"
echo "     - 접근 가능 DB: douzone만"
echo "     - 비밀번호: $DOUZONE_PASSWORD"
echo ""
echo "📋 integrated_cms의 웹 로그인용 계정:"
echo "  - superadmin / password (슈퍼 관리자)"
echo "  - serviceadmin / password (서비스 관리자)"
echo "  - siteadmin / password (사이트 관리자)"
echo "  - admin / password (일반 관리자)"
