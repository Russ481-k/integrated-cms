#!/bin/bash
set -e

echo "🚀 통합 CMS v2 동적 데이터베이스 초기화 시작"
echo "=================================================="

# 서비스 발견 및 초기화 순서 정의
INIT_DIR="/docker-entrypoint-initdb.d"
SERVICES_DIR="$INIT_DIR"

# 1. 서비스 자동 발견
echo ""
echo "🔍 1단계: 서비스 자동 발견"
echo "----------------------------------------"

# integrated_cms는 항상 첫 번째로 초기화 (메타 데이터베이스)
DISCOVERED_SERVICES=("integrated_cms")

# 다른 서비스들 자동 발견 (integrated_cms 제외)
for service_dir in "$SERVICES_DIR"/*; do
    if [ -d "$service_dir" ]; then
        service_name=$(basename "$service_dir")
        if [ "$service_name" != "integrated_cms" ] && [ -f "$service_dir/01-init-schema.sql" ]; then
            DISCOVERED_SERVICES+=("$service_name")
            echo "  🎯 발견된 서비스: $service_name"
        fi
    fi
done

echo "  📋 초기화 대상 서비스: ${DISCOVERED_SERVICES[*]}"
echo "  📊 총 ${#DISCOVERED_SERVICES[@]}개 서비스 발견"

# 2. 서비스별 스키마 초기화
echo ""
echo "📁 2단계: 데이터베이스 스키마 초기화"
echo "----------------------------------------"

for service in "${DISCOVERED_SERVICES[@]}"; do
    schema_file="$SERVICES_DIR/$service/01-init-schema.sql"
    if [ -f "$schema_file" ]; then
        echo "  📊 $service 스키마 생성 중..."
        mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "$schema_file"
        echo "  ✅ $service 스키마 완료"
    else
        echo "  ⚠️  $service 스키마 파일 없음: $schema_file"
    fi
done

# 3. 동적 사용자 및 권한 생성
echo ""
echo "🔐 3단계: 동적 사용자 계정 및 권한 설정"
echo "----------------------------------------"

# 환경변수에서 서비스별 DB 설정 자동 수집
declare -A SERVICE_DB_CONFIG

# integrated_cms (기본값)
SERVICE_DB_CONFIG["integrated_cms"]="${INTEGRATED_DB_USERNAME:-integrated_admin}:${INTEGRATED_DB_PASSWORD:-integrated123!}"

# 환경변수 패턴으로 서비스 설정 자동 수집
for service in "${DISCOVERED_SERVICES[@]}"; do
    if [ "$service" != "integrated_cms" ]; then
        # 서비스명을 대문자로 변환 (douzone -> DOUZONE)
        service_upper=$(echo "$service" | tr '[:lower:]' '[:upper:]')
        
        # 환경변수명 생성 (DOUZONE_DB_USERNAME, DOUZONE_DB_PASSWORD)
        username_var="${service_upper}_DB_USERNAME"
        password_var="${service_upper}_DB_PASSWORD"
        
        # 환경변수 값 추출 (기본값 포함)
        username=$(eval "echo \${$username_var:-admin}")
        password=$(eval "echo \${$password_var:-admin123!}")
        
        SERVICE_DB_CONFIG["$service"]="$username:$password"
        echo "  🔧 $service 서비스 설정: 사용자=$username"
    fi
done

# integrated_admin 계정 생성 (모든 DB 접근)
echo "  👑 통합 관리자 계정 생성 중..."
integrated_config="${SERVICE_DB_CONFIG["integrated_cms"]}"
integrated_user=$(echo "$integrated_config" | cut -d':' -f1)
integrated_pass=$(echo "$integrated_config" | cut -d':' -f2)

mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
DROP USER IF EXISTS '$integrated_user'@'%';
DROP USER IF EXISTS '$integrated_user'@'localhost';
CREATE USER '$integrated_user'@'%' IDENTIFIED BY '$integrated_pass';
CREATE USER '$integrated_user'@'localhost' IDENTIFIED BY '$integrated_pass';
EOSQL

# 모든 발견된 데이터베이스에 권한 부여
for service in "${DISCOVERED_SERVICES[@]}"; do
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
GRANT ALL PRIVILEGES ON ${service}.* TO '$integrated_user'@'%';
GRANT ALL PRIVILEGES ON ${service}.* TO '$integrated_user'@'localhost';
EOSQL
    echo "    ✅ $integrated_user → $service DB 권한 부여"
done

# 서비스별 전용 계정 생성
for service in "${DISCOVERED_SERVICES[@]}"; do
    if [ "$service" != "integrated_cms" ]; then
        service_config="${SERVICE_DB_CONFIG["$service"]}"
        service_user=$(echo "$service_config" | cut -d':' -f1)
        service_pass=$(echo "$service_config" | cut -d':' -f2)
        
        echo "  🏢 $service 전용 계정 생성 중..."
        mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
DROP USER IF EXISTS '$service_user'@'%';
DROP USER IF EXISTS '$service_user'@'localhost';
CREATE USER '$service_user'@'%' IDENTIFIED BY '$service_pass';
CREATE USER '$service_user'@'localhost' IDENTIFIED BY '$service_pass';
GRANT ALL PRIVILEGES ON ${service}.* TO '$service_user'@'%';
GRANT ALL PRIVILEGES ON ${service}.* TO '$service_user'@'localhost';
EOSQL
        echo "    ✅ $service_user → $service DB 권한 부여"
    fi
done

mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOSQL
FLUSH PRIVILEGES;
EOSQL

# 4. integrated_cms admin_user 테이블 초기 데이터
echo ""
echo "👤 4단계: integrated_cms 관리자 계정 생성"
echo "----------------------------------------"

mysql -u root -p"$MYSQL_ROOT_PASSWORD" integrated_cms << EOSQL
INSERT IGNORE INTO admin_user (
    UUID, USERNAME, NAME, EMAIL, PASSWORD, ROLE, STATUS, 
    CREATED_AT, UPDATED_AT, MEMO, MEMO_UPDATED_AT, MEMO_UPDATED_BY
) VALUES 
('super-admin-uuid-0000-0000-000000000001', 'superadmin', 'Super Administrator', 
 'superadmin@integrated-cms.kr', '\$2a\$10\$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'SUPER_ADMIN', 'ACTIVE', NOW(), NOW(), '통합 CMS 슈퍼 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001'),
('service-admin-uuid-0000-0000-000000000002', 'serviceadmin', 'Service Administrator', 
 'serviceadmin@integrated-cms.kr', '\$2a\$10\$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'SERVICE_ADMIN', 'ACTIVE', NOW(), NOW(), '서비스별 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001'),
('site-admin-uuid-0000-0000-000000000003', 'siteadmin', 'Site Administrator', 
 'siteadmin@integrated-cms.kr', '\$2a\$10\$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'SITE_ADMIN', 'ACTIVE', NOW(), NOW(), '사이트별 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001'),
('admin-uuid-0000-0000-000000000004', 'admin', 'Administrator', 
 'admin@integrated-cms.kr', '\$2a\$10\$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'ADMIN', 'ACTIVE', NOW(), NOW(), '일반 관리자 계정', NOW(), 'super-admin-uuid-0000-0000-000000000001');
EOSQL

echo "  ✅ 웹 로그인용 관리자 계정 생성 완료 (비밀번호: password)"

# 5. 서비스별 초기 데이터 설정
echo ""
echo "📊 5단계: 서비스별 초기 데이터 설정"
echo "----------------------------------------"

for service in "${DISCOVERED_SERVICES[@]}"; do
    setup_script="$SERVICES_DIR/$service/02-setup-${service}-data.sh"
    if [ -f "$setup_script" ]; then
        echo "  🏢 $service 초기 데이터 설정 중..."
        bash "$setup_script"
        echo "  ✅ $service 초기 데이터 완료"
    else
        echo "  ⚠️  $service 초기 데이터 스크립트 없음"
    fi
done

# 6. 최종 상태 확인 및 요약
echo ""
echo "✅ 6단계: 초기화 완료 및 상태 확인"
echo "=================================================="

echo "📊 생성된 데이터베이스:"
for service in "${DISCOVERED_SERVICES[@]}"; do
    echo "  ✅ $service"
done

echo ""
echo "👤 생성된 사용자 계정:"
mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SELECT User, Host FROM mysql.user WHERE User NOT IN ('root', 'mysql.sys', 'mysql.session', 'mysql.infoschema');"

echo ""
echo "🎉 동적 데이터베이스 초기화 완료!"
echo ""
echo "📋 요약:"
echo "  🔢 총 ${#DISCOVERED_SERVICES[@]}개 서비스 초기화 완료"
echo "  🔐 통합 관리자: $integrated_user (모든 DB 접근)"
for service in "${DISCOVERED_SERVICES[@]}"; do
    if [ "$service" != "integrated_cms" ]; then
        service_config="${SERVICE_DB_CONFIG["$service"]}"
        service_user=$(echo "$service_config" | cut -d':' -f1)
        echo "  🏢 $service 관리자: $service_user ($service DB만 접근)"
    fi
done
echo "  👤 웹 로그인: superadmin, serviceadmin, siteadmin, admin (비밀번호: password)"
echo ""
echo "🌐 접속 정보:"
echo "  - 통합 CMS: http://localhost:3000"
echo "  - 더존 CMS: http://localhost:3001"
echo "  - API 서버: http://localhost:8080"
