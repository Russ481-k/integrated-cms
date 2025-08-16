-- Menu 테이블에 service_id 컬럼 추가
-- 통합 CMS v2: 서비스별 메뉴 관리를 위한 스키마 업데이트

-- 1. service_id 컬럼 추가
ALTER TABLE menu
ADD COLUMN service_id VARCHAR(50) NULL COMMENT '서비스 ID (통합 관리용)';

-- 2. service_id 인덱스 추가 (성능 최적화)
CREATE INDEX idx_menu_service_id ON menu (service_id);

-- 3. service_id와 parent_id 복합 인덱스 추가 (메뉴 트리 조회 최적화)
CREATE INDEX idx_menu_service_parent ON menu (service_id, parent_id);

-- 4. service_id와 visible 복합 인덱스 추가 (활성 메뉴 조회 최적화)
CREATE INDEX idx_menu_service_visible ON menu (service_id, visible);

-- 5. 기존 메뉴들을 기본 서비스로 설정 (선택사항)
-- UPDATE menu SET service_id = 'integrated_cms' WHERE service_id IS NULL;

-- 6. 제약조건 추가 (선택사항 - 서비스 테이블과의 외래키)
-- ALTER TABLE menu
-- ADD CONSTRAINT fk_menu_service
-- FOREIGN KEY (service_id) REFERENCES service(SERVICE_ID)
-- ON DELETE SET NULL ON UPDATE CASCADE;

-- 7. 메뉴 이름과 서비스 ID 복합 유니크 제약조건 (선택사항)
-- ALTER TABLE menu
-- ADD CONSTRAINT uk_menu_name_service
-- UNIQUE (name, service_id);