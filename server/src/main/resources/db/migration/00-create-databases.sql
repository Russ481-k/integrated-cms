-- 사용자 계정 생성
CREATE USER IF NOT EXISTS 'integrated_admin'@'%' IDENTIFIED BY 'integrated123!';
CREATE USER IF NOT EXISTS 'admin'@'%' IDENTIFIED BY 'admin123!';

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS integrated_cms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS douzone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 권한 설정
GRANT ALL PRIVILEGES ON integrated_cms.* TO 'integrated_admin'@'%';
GRANT ALL PRIVILEGES ON douzone.* TO 'admin'@'%';
FLUSH PRIVILEGES;
