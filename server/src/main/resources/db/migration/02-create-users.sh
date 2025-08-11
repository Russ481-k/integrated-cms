#!/bin/bash
set -e

# 환경변수에서 사용자 정보 가져오기 (기본값 설정)
INTEGRATED_USERNAME=${INTEGRATED_DB_USERNAME:-"integrated_admin"}
INTEGRATED_PASSWORD=${INTEGRATED_DB_PASSWORD:-"integrated_password"}
CMS_USERNAME=${CMS_DB_USERNAME:-"cms_admin"}
CMS_PASSWORD=${CMS_DB_PASSWORD:-"cms_password"}

echo "데이터베이스 사용자 생성 시작..."
echo "통합 CMS 사용자: $INTEGRATED_USERNAME"
echo "개별 CMS 사용자: $CMS_USERNAME"

# MySQL 접속하여 사용자 생성
mysql -u root -p$MYSQL_ROOT_PASSWORD << EOF

-- 통합 CMS 사용자 생성 (모든 데이터베이스에 접근 가능)
CREATE USER IF NOT EXISTS '$INTEGRATED_USERNAME'@'%' IDENTIFIED BY '$INTEGRATED_PASSWORD';
GRANT ALL PRIVILEGES ON *.* TO '$INTEGRATED_USERNAME'@'%' WITH GRANT OPTION;

-- 개별 CMS 사용자 생성 (특정 CMS 데이터베이스만 접근 가능)
CREATE USER IF NOT EXISTS '$CMS_USERNAME'@'%' IDENTIFIED BY '$CMS_PASSWORD';

-- 개별 CMS 사용자에게 integrated_cms 데이터베이스 권한 부여
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON integrated_cms.* TO '$CMS_USERNAME'@'%';

-- CMS 관련 데이터베이스 생성 및 권한 부여
CREATE DATABASE IF NOT EXISTS cms;
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON cms.* TO '$CMS_USERNAME'@'%';

CREATE DATABASE IF NOT EXISTS douzone;
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON douzone.* TO '$CMS_USERNAME'@'%';

-- 권한 새로고침
FLUSH PRIVILEGES;

-- 생성된 사용자 확인
SELECT User as '생성된_사용자', Host as '호스트' FROM mysql.user WHERE User IN ('$INTEGRATED_USERNAME', '$CMS_USERNAME') ORDER BY User;

EOF

echo "데이터베이스 사용자 생성 완료!"
echo "- 통합 CMS 사용자 '$INTEGRATED_USERNAME': 모든 데이터베이스 접근 가능"
echo "- 개별 CMS 사용자 '$CMS_USERNAME': integrated_cms, cms, douzone 데이터베이스 접근 가능"
