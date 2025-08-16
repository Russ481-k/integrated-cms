#!/bin/bash
set -e

echo "📊 integrated_cms 전용 초기 데이터 설정"

# integrated_cms만의 특화된 초기 데이터나 설정이 필요한 경우 여기에 추가
# 현재는 스키마 생성만으로 충분하므로 플레이스홀더만 제공

echo ""
echo "📋 integrated_cms 특화 설정 확인 중..."

# 예시: 서비스 정보 초기 등록
mysql -u root -p"$MYSQL_ROOT_PASSWORD" integrated_cms << EOSQL
-- douzone 서비스를 서비스 목록에 등록 (중복 시 무시)
INSERT IGNORE INTO service (
    SERVICE_ID, SERVICE_CODE, SERVICE_NAME, SERVICE_DOMAIN, 
    API_BASE_URL, STATUS, DESCRIPTION, CREATED_AT
) VALUES (
    'douzone-service-uuid-001', 'douzone', '더존 CMS', 'http://localhost:3001',
    'http://localhost:8080/api/v2/cms/douzone', 'ACTIVE', 
    '더존 사업 관련 CMS 서비스', NOW()
);
EOSQL

echo "  ✅ douzone 서비스 메타데이터 등록 완료"

echo ""
echo "🎉 integrated_cms 전용 설정 완료!"
