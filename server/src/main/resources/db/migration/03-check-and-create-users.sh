#!/bin/bash
set -e

echo "=== 데이터베이스 계정 체크 및 자동 생성 시작 ==="

# 환경변수 기본값 설정
INTEGRATED_USERNAME=${INTEGRATED_DB_USERNAME:-integrated_admin}
INTEGRATED_PASSWORD=${INTEGRATED_DB_PASSWORD:-integrated123!}
CMS_USERNAME=${DOUZONE_DB_USERNAME:-admin}
CMS_PASSWORD=${DOUZONE_DB_PASSWORD:-admin123!}

echo "체크할 계정 정보:"
echo "- 통합 CMS 관리자: $INTEGRATED_USERNAME"
echo "- 개별 서비스 사용자: $CMS_USERNAME"

# 필요한 데이터베이스들 생성
echo "필요한 데이터베이스 생성 중..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
CREATE DATABASE IF NOT EXISTS integrated_cms;
CREATE DATABASE IF NOT EXISTS douzone;
EOSQL

echo "✅ 데이터베이스 생성 완료"

# 통합 CMS 데이터베이스에 admin_user 테이블 초기 데이터 삽입
echo "=== admin_user 테이블 초기 데이터 생성 ==="
mysql -u root -p"$MYSQL_ROOT_PASSWORD" integrated_cms << EOSQL
-- admin_user 테이블에 기본 관리자 계정들 삽입
INSERT IGNORE INTO admin_user (
    UUID, USERNAME, NAME, EMAIL, PASSWORD, ROLE, STATUS, 
    CREATED_AT, UPDATED_AT, MEMO, MEMO_UPDATED_AT, MEMO_UPDATED_BY
) VALUES 
-- 슈퍼 관리자 (비밀번호: super123!)
('super-admin-uuid-0000-0000-000000000001', 'superadmin', 'Super Administrator', 
 'superadmin@integrated-cms.kr', '\$2a\$10\$8K1p/wrkrEo/sX1QJ4qwi.7w5ZwKyZ5/vNqQKXGPKQLEa5jXrVlSa', 
 'SUPER_ADMIN', 'ACTIVE', NOW(), NOW(), '통합 CMS 슈퍼 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001'),

-- 서비스 관리자 (비밀번호: service123!)
('service-admin-uuid-0000-0000-000000000002', 'serviceadmin', 'Service Administrator', 
 'serviceadmin@integrated-cms.kr', '\$2a\$10\$9L2q/xslsEp/tY2RK5rxj.8x6AwLzZ6/wOqRLYHQLRMFb6kYsWmTb', 
 'SERVICE_ADMIN', 'ACTIVE', NOW(), NOW(), '서비스별 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001'),

-- 사이트 관리자 (비밀번호: site123!)
('site-admin-uuid-0000-0000-000000000003', 'siteadmin', 'Site Administrator', 
 'siteadmin@integrated-cms.kr', '\$2a\$10\$0M3r/ytmtFq/uZ3SL6syk.9y7BxMaA7/xPsSOZIRMSNGc7lZtXnUc', 
 'SITE_ADMIN', 'ACTIVE', NOW(), NOW(), '사이트별 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001'),

-- 일반 관리자 (비밀번호: admin123!)
('admin-uuid-0000-0000-000000000004', 'admin', 'Administrator', 
 'admin@integrated-cms.kr', '\$2a\$10\$ifHo7stsn6Bmb4E9XNvNw.DorLb9BoR/wfSspOknFGwmbmqR/94G6', 
 'ADMIN', 'ACTIVE', NOW(), NOW(), '일반 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001');
EOSQL

echo "✅ admin_user 초기 데이터 생성 완료"

# 통합 CMS 관리자 계정 생성
echo "=== 통합 CMS 관리자 계정 생성 ==="
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
DROP USER IF EXISTS '$INTEGRATED_USERNAME'@'%';
CREATE USER '$INTEGRATED_USERNAME'@'%' IDENTIFIED BY '$INTEGRATED_PASSWORD';
GRANT ALL PRIVILEGES ON integrated_cms.* TO '$INTEGRATED_USERNAME'@'%';
GRANT ALL PRIVILEGES ON douzone.* TO '$INTEGRATED_USERNAME'@'%';
FLUSH PRIVILEGES;
EOSQL

echo "✅ 통합 CMS 관리자 계정 생성 완료"

# 개별 서비스 사용자 계정 생성
echo "=== 개별 서비스 사용자 계정 생성 ==="
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
DROP USER IF EXISTS '$CMS_USERNAME'@'%';
DROP USER IF EXISTS '$CMS_USERNAME'@'localhost';
CREATE USER '$CMS_USERNAME'@'%' IDENTIFIED BY '$CMS_PASSWORD';
CREATE USER '$CMS_USERNAME'@'localhost' IDENTIFIED BY '$CMS_PASSWORD';
GRANT ALL PRIVILEGES ON douzone.* TO '$CMS_USERNAME'@'%';
GRANT ALL PRIVILEGES ON douzone.* TO '$CMS_USERNAME'@'localhost';
FLUSH PRIVILEGES;
EOSQL

echo "✅ 개별 서비스 사용자 계정 생성 완료"

echo "🎉 데이터베이스 계정 체크 및 생성 완료!"