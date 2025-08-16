#!/bin/bash
set -e

echo "📊 douzone 전용 초기 데이터 설정"

# douzone 서비스만의 특화된 초기 데이터 설정
echo ""
echo "📋 douzone 서비스 기본 데이터 생성 중..."

# douzone 데이터베이스에 기본 사용자 및 설정 데이터 추가
mysql -u root -p"$MYSQL_ROOT_PASSWORD" douzone << EOSQL
-- douzone 서비스의 기본 그룹 생성
INSERT IGNORE INTO groups (
    uuid, name, description, created_at
) VALUES 
('douzone-default-group-001', '기본 사용자 그룹', 'douzone 서비스 기본 사용자 그룹', NOW()),
('douzone-admin-group-002', '관리자 그룹', 'douzone 서비스 관리자 그룹', NOW());

-- douzone 서비스의 기본 조직 생성
INSERT IGNORE INTO organizations (
    uuid, name, description, created_at
) VALUES 
('douzone-org-001', '더존비즈온', '더존비즈온 본사', NOW());

-- douzone 서비스의 기본 게시판 생성
INSERT IGNORE INTO bbs_master (
    BBS_NAME, SKIN_TYPE, READ_AUTH, WRITE_AUTH, ADMIN_AUTH,
    DISPLAY_YN, SORT_ORDER, NOTICE_YN, PUBLISH_YN, ATTACHMENT_YN,
    ATTACHMENT_LIMIT, ATTACHMENT_SIZE, CREATED_AT
) VALUES 
('공지사항', 'BASIC', 'USER', 'ADMIN', 'ADMIN', 'Y', 'D', 'Y', 'Y', 'Y', 4, 10, NOW()),
('자주 묻는 질문', 'FAQ', 'USER', 'ADMIN', 'ADMIN', 'Y', 'D', 'Y', 'Y', 'N', 0, 0, NOW()),
('Q&A', 'QNA', 'USER', 'USER', 'ADMIN', 'Y', 'D', 'Y', 'Y', 'Y', 3, 5, NOW());

-- douzone 서비스의 기본 메뉴 구조 생성 (부모 메뉴 먼저)
INSERT IGNORE INTO menu (
    name, type, url, display_position, visible, sort_order, parent_id, created_at
) VALUES 
('홈', 'LINK', '/', 'HEADER', 1, 1, NULL, NOW()),
('서비스 소개', 'LINK', '/about', 'HEADER', 1, 2, NULL, NOW()),
('고객 지원', 'FOLDER', NULL, 'HEADER', 1, 3, NULL, NOW());

-- 고객 지원 하위 메뉴 생성
SET @support_menu_id = LAST_INSERT_ID();

INSERT IGNORE INTO menu (
    name, type, url, display_position, visible, sort_order, parent_id, created_at
) VALUES 
('공지사항', 'BOARD', '/board/notice', 'HEADER', 1, 1, @support_menu_id, NOW()),
('FAQ', 'BOARD', '/board/faq', 'HEADER', 1, 2, @support_menu_id, NOW()),
('Q&A', 'BOARD', '/board/qna', 'HEADER', 1, 3, @support_menu_id, NOW());
EOSQL

echo "  ✅ douzone 기본 데이터 생성 완료"
echo "     - 기본 그룹: 2개"
echo "     - 기본 조직: 1개" 
echo "     - 기본 게시판: 3개 (공지사항, FAQ, Q&A)"
echo "     - 기본 메뉴: 6개"

echo ""
echo "🎉 douzone 전용 설정 완료!"
