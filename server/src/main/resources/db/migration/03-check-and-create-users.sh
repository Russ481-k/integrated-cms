#!/bin/bash
set -e

echo "=== 데이터베이스 계정 체크 및 자동 생성 시작 ==="

# 환경변수에서 사용자 정보 가져오기 (필수 값, 없으면 에러)
if [ -z "$INTEGRATED_DB_USERNAME" ]; then
    echo "❌ 오류: INTEGRATED_DB_USERNAME 환경변수가 설정되지 않았습니다."
    exit 1
fi

if [ -z "$INTEGRATED_DB_PASSWORD" ]; then
    echo "❌ 오류: INTEGRATED_DB_PASSWORD 환경변수가 설정되지 않았습니다."
    exit 1
fi

if [ -z "$DOUZONE_DB_USERNAME" ]; then
    echo "❌ 오류: DOUZONE_DB_USERNAME 환경변수가 설정되지 않았습니다."
    exit 1
fi

if [ -z "$DOUZONE_DB_PASSWORD" ]; then
    echo "❌ 오류: DOUZONE_DB_PASSWORD 환경변수가 설정되지 않았습니다."
    exit 1
fi

INTEGRATED_USERNAME=$INTEGRATED_DB_USERNAME
INTEGRATED_PASSWORD=$INTEGRATED_DB_PASSWORD
CMS_USERNAME=$DOUZONE_DB_USERNAME
CMS_PASSWORD=$DOUZONE_DB_PASSWORD

echo "체크할 계정 정보:"
echo "- 통합 CMS 관리자: $INTEGRATED_USERNAME"
echo "- 개별 서비스 사용자: $CMS_USERNAME"

# 계정 존재 여부 체크 함수
check_user_exists() {
    local username=$1
    local result=$(mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SELECT COUNT(*) as count FROM mysql.user WHERE User='$username';" --batch --skip-column-names 2>/dev/null || echo "0")
    echo $result
}

# 계정 접속 가능 여부 체크 함수
check_user_access() {
    local username=$1
    local password=$2
    local database=$3
    
    if mysql -u "$username" -p"$password" -e "USE $database;" 2>/dev/null; then
        return 0  # 접속 성공
    else
        return 1  # 접속 실패
    fi
}

# 필요한 데이터베이스들 생성
echo "필요한 데이터베이스 생성 중..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOF
-- 중앙 통합 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS integrated_cms;

-- 개별 서비스 데이터베이스 생성 (필요에 따라 추가)
CREATE DATABASE IF NOT EXISTS douzone;

-- 향후 추가될 수 있는 개별 서비스 데이터베이스들
-- CREATE DATABASE IF NOT EXISTS service2;
-- CREATE DATABASE IF NOT EXISTS service3;
EOF

echo "✅ 데이터베이스 생성 완료"

# 통합 CMS 관리자 계정 체크
echo "=== 통합 CMS 관리자 계정 ($INTEGRATED_USERNAME) 체크 ==="
INTEGRATED_EXISTS=$(check_user_exists "$INTEGRATED_USERNAME")

if [ "$INTEGRATED_EXISTS" -eq 0 ]; then
    echo "❌ 통합 CMS 관리자 계정이 존재하지 않음. 생성 중..."
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOF
-- 통합 CMS 관리자 사용자 생성
CREATE USER '$INTEGRATED_USERNAME'@'%' IDENTIFIED BY '$INTEGRATED_PASSWORD';

-- 모든 데이터베이스에 대한 권한 부여
GRANT ALL PRIVILEGES ON integrated_cms.* TO '$INTEGRATED_USERNAME'@'%';
GRANT ALL PRIVILEGES ON douzone.* TO '$INTEGRATED_USERNAME'@'%';

-- 향후 추가될 데이터베이스 권한
-- GRANT ALL PRIVILEGES ON service2.* TO '$INTEGRATED_USERNAME'@'%';
-- GRANT ALL PRIVILEGES ON service3.* TO '$INTEGRATED_USERNAME'@'%';

FLUSH PRIVILEGES;
EOF
    echo "✅ 통합 CMS 관리자 계정 생성 완료"
else
    echo "✅ 통합 CMS 관리자 계정이 이미 존재함"
    
    # 접속 테스트
    if check_user_access "$INTEGRATED_USERNAME" "$INTEGRATED_PASSWORD" "integrated_cms"; then
        echo "✅ 통합 CMS 관리자 계정 접속 성공"
    else
        echo "❌ 통합 CMS 관리자 계정 접속 실패. 권한 재설정 중..."
        mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOF
-- 기존 권한 제거 후 재설정
REVOKE ALL PRIVILEGES ON *.* FROM '$INTEGRATED_USERNAME'@'%';
GRANT ALL PRIVILEGES ON integrated_cms.* TO '$INTEGRATED_USERNAME'@'%';
GRANT ALL PRIVILEGES ON douzone.* TO '$INTEGRATED_USERNAME'@'%';
FLUSH PRIVILEGES;
EOF
        echo "✅ 통합 CMS 관리자 권한 재설정 완료"
    fi
fi

# 개별 서비스 사용자 계정 체크 (강제 재생성)
echo "=== 개별 서비스 사용자 계정 ($CMS_USERNAME) 체크 및 재생성 ==="

# 기존 사용자 완전 삭제 (있는 경우)
echo "🔄 기존 $CMS_USERNAME 사용자 삭제 중..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOF
DROP USER IF EXISTS '$CMS_USERNAME'@'%';
DROP USER IF EXISTS '$CMS_USERNAME'@'localhost';
FLUSH PRIVILEGES;
EOF

# 새로 생성
echo "🆕 $CMS_USERNAME 사용자 새로 생성 중..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOF
-- 개별 서비스 사용자 생성 (% 및 localhost)
CREATE USER '$CMS_USERNAME'@'%' IDENTIFIED BY '$CMS_PASSWORD';
CREATE USER '$CMS_USERNAME'@'localhost' IDENTIFIED BY '$CMS_PASSWORD';

-- 개별 서비스 DB만 권한 부여 (보안상 integrated_cms 접근 금지)
GRANT ALL PRIVILEGES ON douzone.* TO '$CMS_USERNAME'@'%';
GRANT ALL PRIVILEGES ON douzone.* TO '$CMS_USERNAME'@'localhost';

-- 향후 추가될 개별 서비스 DB 권한
-- GRANT ALL PRIVILEGES ON service2.* TO '$CMS_USERNAME'@'%';
-- GRANT ALL PRIVILEGES ON service2.* TO '$CMS_USERNAME'@'localhost';

FLUSH PRIVILEGES;
EOF

# 접속 테스트
if check_user_access "$CMS_USERNAME" "$CMS_PASSWORD" "douzone"; then
    echo "✅ $CMS_USERNAME 사용자 생성 및 접속 테스트 성공"
else
    echo "❌ $CMS_USERNAME 사용자 접속 테스트 실패"
    exit 1
fi

# 최종 검증
echo "=== 최종 계정 검증 ==="
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOF
-- 생성된 사용자 목록 확인
SELECT User as '사용자명', Host as '호스트', 
       CASE 
           WHEN User = '$INTEGRATED_USERNAME' THEN '통합 CMS 관리자'
           WHEN User = '$CMS_USERNAME' THEN '개별 서비스 사용자'
           ELSE '기타'
       END as '역할'
FROM mysql.user 
WHERE User IN ('$INTEGRATED_USERNAME', '$CMS_USERNAME') 
ORDER BY User;

-- 데이터베이스 목록 확인
SHOW DATABASES;
EOF

echo ""
echo "=== 권한 확인 ==="
mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOF
-- 각 사용자별 권한 확인
SHOW GRANTS FOR '$INTEGRATED_USERNAME'@'%';
SHOW GRANTS FOR '$CMS_USERNAME'@'%';
SHOW GRANTS FOR '$CMS_USERNAME'@'localhost';
EOF

echo ""
echo "🎉 데이터베이스 계정 체크 및 생성 완료!"
echo "📊 생성된 구조:"
echo "  🗄️  integrated_cms (중앙 통합 데이터베이스)"
echo "  🗄️  douzone (개별 서비스 데이터베이스)" 
echo ""
echo "👥 계정 정보:"
echo "  👤 $INTEGRATED_USERNAME: 모든 데이터베이스 관리 (통합 관리자)"
echo "  👤 $CMS_USERNAME: 개별 서비스 DB만 접근 (douzone 등)"
echo ""
echo "🔧 새로운 서비스 추가 시:"
echo "  1. CREATE DATABASE IF NOT EXISTS service_name;"
echo "  2. GRANT ALL PRIVILEGES ON service_name.* TO '$INTEGRATED_USERNAME'@'%';"
echo "  3. GRANT ALL PRIVILEGES ON service_name.* TO '$CMS_USERNAME'@'%';"
echo "  4. FLUSH PRIVILEGES;"
