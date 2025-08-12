-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: 172.30.1.11    Database: interated_cms
-- ------------------------------------------------------
-- Server version	5.5.5-10.6.18-MariaDB

-- 데이터베이스 생성 및 선택
CREATE DATABASE IF NOT EXISTS integrated_cms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE integrated_cms;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */
;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */
;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */
;
/*!50503 SET NAMES utf8mb4 */
;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */
;
/*!40103 SET TIME_ZONE='+00:00' */
;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */
;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */
;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */
;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */
;

--
-- Table structure for table `admin_user`
--

DROP TABLE IF EXISTS `admin_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `admin_user` (
    `UUID` varchar(36) NOT NULL COMMENT '관리자 고유 ID',
    `USERNAME` varchar(50) NOT NULL COMMENT '관리자 계정명',
    `NAME` varchar(100) NOT NULL COMMENT '관리자 이름',
    `EMAIL` varchar(100) NOT NULL COMMENT '이메일',
    `PASSWORD` varchar(255) NOT NULL COMMENT '암호화된 비밀번호',
    `ROLE` varchar(20) NOT NULL COMMENT '관리자 역할',
    `AVATAR_URL` varchar(255) DEFAULT NULL COMMENT '프로필 이미지 URL',
    `STATUS` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '계정 상태',
    `ORGANIZATION_ID` varchar(36) DEFAULT NULL COMMENT '기관 ID',
    `GROUP_ID` varchar(36) DEFAULT NULL COMMENT '그룹 ID',
    `PHONE` varchar(50) DEFAULT NULL COMMENT '전화번호',
    `TEMP_PW_FLAG` tinyint(1) DEFAULT 0 COMMENT '임시비밀번호여부 (0: 아니오, 1: 예)',
    `PROVIDER` varchar(50) DEFAULT NULL COMMENT '인증 제공자',
    `RESET_TOKEN` varchar(255) DEFAULT NULL COMMENT '비밀번호 재설정 토큰',
    `RESET_TOKEN_EXPIRY` timestamp NULL DEFAULT NULL COMMENT '비밀번호 재설정 토큰 만료 시간',
    `IS_TEMPORARY` tinyint(1) DEFAULT 0 COMMENT '임시 관리자 여부 (0: 아니오, 1: 예)',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `MEMO` text DEFAULT NULL COMMENT '관리자 메모 내용',
    `MEMO_UPDATED_AT` timestamp NULL DEFAULT NULL COMMENT '메모 최종 수정 일시',
    `MEMO_UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '메모 최종 수정자 UUID',
    PRIMARY KEY (`UUID`),
    UNIQUE KEY `username` (`USERNAME`),
    UNIQUE KEY `email` (`EMAIL`),
    KEY `created_by` (`CREATED_BY`),
    KEY `updated_by` (`UPDATED_BY`),
    KEY `fk_admin_memo_updated_by` (`MEMO_UPDATED_BY`),
    CONSTRAINT `admin_user_ibfk_1` FOREIGN KEY (`CREATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `admin_user_ibfk_2` FOREIGN KEY (`UPDATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_admin_memo_updated_by` FOREIGN KEY (`MEMO_UPDATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '통합 관리자 계정 관리';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `admin_user`
--

LOCK TABLES `admin_user` WRITE;
/*!40000 ALTER TABLE `admin_user` DISABLE KEYS */
;
/*!40000 ALTER TABLE `admin_user` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `bbs_article`
--

DROP TABLE IF EXISTS `bbs_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `bbs_article` (
    `NTT_ID` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK: 게시글 ID',
    `BBS_ID` int(10) unsigned NOT NULL COMMENT 'FK: 게시판 ID',
    `MENU_ID` int(11) NOT NULL,
    `PARENT_NTT_ID` int(10) unsigned DEFAULT NULL COMMENT '부모 글 ID(답변형)',
    `THREAD_DEPTH` int(10) unsigned DEFAULT 0 COMMENT '답변 깊이',
    `WRITER` varchar(50) NOT NULL COMMENT '작성자',
    `TITLE` varchar(255) NOT NULL COMMENT '제목',
    `content` text DEFAULT NULL COMMENT '내용',
    `NOTICE_STATE` varchar(1) DEFAULT 'N' COMMENT '공지 여부(Y=공지,N=미공지,P=영구공지)',
    `NOTICE_START_DT` datetime DEFAULT curdate() COMMENT '공지 시작일',
    `NOTICE_END_DT` datetime DEFAULT(curdate() + interval 1 day) COMMENT '공지 종료일',
    `PUBLISH_STATE` varchar(1) DEFAULT 'N' COMMENT '게시 여부(Y=게시,N=미게시,P=영구게시)',
    `PUBLISH_START_DT` datetime DEFAULT curdate() COMMENT '게시 시작일',
    `PUBLISH_END_DT` datetime DEFAULT(curdate() + interval 1 day) COMMENT '게시 종료일',
    `EXTERNAL_LINK` varchar(255) DEFAULT NULL COMMENT '외부 링크 URL',
    `HITS` int(11) DEFAULT 0 COMMENT '조회수',
    `POSTED_AT` datetime NOT NULL DEFAULT current_timestamp() COMMENT '표시용 등록일 (관리자 수정 가능)',
    `DISPLAY_WRITER` varchar(50) DEFAULT NULL COMMENT '표시용 작성자 (관리자 수정 가능)',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    `has_image_in_content` tinyint(1) DEFAULT 0 COMMENT '본문 내 이미지 포함 여부',
    PRIMARY KEY (`NTT_ID`),
    KEY `fk_bbs_article_master` (`BBS_ID`),
    KEY `fk_bbs_article_parent` (`PARENT_NTT_ID`),
    KEY `idx_article_search` (`TITLE`, `content` (255)),
    KEY `fk_bbs_article_menu` (`MENU_ID`),
    CONSTRAINT `fk_bbs_article_master` FOREIGN KEY (`BBS_ID`) REFERENCES `bbs_master` (`BBS_ID`) ON DELETE CASCADE,
    CONSTRAINT `fk_bbs_article_menu` FOREIGN KEY (`MENU_ID`) REFERENCES `menu` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bbs_article_parent` FOREIGN KEY (`PARENT_NTT_ID`) REFERENCES `bbs_article` (`NTT_ID`) ON DELETE SET NULL
) ENGINE = InnoDB AUTO_INCREMENT = 280 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '게시판 게시글';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `bbs_article`
--

LOCK TABLES `bbs_article` WRITE;
/*!40000 ALTER TABLE `bbs_article` DISABLE KEYS */
;
/*!40000 ALTER TABLE `bbs_article` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `bbs_article_category`
--

DROP TABLE IF EXISTS `bbs_article_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `bbs_article_category` (
    `NTT_ID` int(20) unsigned NOT NULL COMMENT 'FK: 게시글 ID',
    `CATEGORY_ID` int(20) unsigned NOT NULL COMMENT 'FK: 카테고리 ID',
    PRIMARY KEY (`NTT_ID`, `CATEGORY_ID`),
    KEY `IDX_ARTICLE_CATEGORY` (`CATEGORY_ID`, `NTT_ID`),
    CONSTRAINT `bbs_article_category_ibfk_1` FOREIGN KEY (`NTT_ID`) REFERENCES `bbs_article` (`NTT_ID`) ON DELETE CASCADE,
    CONSTRAINT `bbs_article_category_ibfk_2` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `bbs_category` (`CATEGORY_ID`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '게시글-카테고리 \r\n매핑';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `bbs_article_category`
--

LOCK TABLES `bbs_article_category` WRITE;
/*!40000 ALTER TABLE `bbs_article_category` DISABLE KEYS */
;
/*!40000 ALTER TABLE `bbs_article_category` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `bbs_category`
--

DROP TABLE IF EXISTS `bbs_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `bbs_category` (
    `CATEGORY_ID` int(20) unsigned NOT NULL AUTO_INCREMENT,
    `BBS_ID` int(20) unsigned NOT NULL COMMENT 'FK: 게시판 ID',
    `CODE` varchar(50) NOT NULL COMMENT '카테고리 코드',
    `NAME` varchar(100) NOT NULL COMMENT '카테고리 이름',
    `SORT_ORDER` int(11) DEFAULT 0 COMMENT '카테고리 정렬 순서',
    `DISPLAY_YN` varchar(1) DEFAULT 'Y' COMMENT '노출 여부(Y,N)',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    PRIMARY KEY (`CATEGORY_ID`),
    UNIQUE KEY `UK_BBS_CATEGORY_CODE` (`BBS_ID`, `CODE`),
    KEY `IDX_CATEGORY_SORT` (
        `BBS_ID`,
        `SORT_ORDER`,
        `DISPLAY_YN`
    ),
    CONSTRAINT `bbs_category_ibfk_1` FOREIGN KEY (`BBS_ID`) REFERENCES `bbs_master` (`BBS_ID`) ON DELETE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 8 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '게시판별 카테고리';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `bbs_category`
--

LOCK TABLES `bbs_category` WRITE;
/*!40000 ALTER TABLE `bbs_category` DISABLE KEYS */
;
INSERT INTO
    `bbs_category`
VALUES (
        4,
        1,
        'NOTICE',
        '공지',
        1,
        'Y',
        NULL,
        NULL,
        '2025-07-16 05:09:54',
        NULL,
        NULL,
        '2025-07-16 05:09:54'
    ),
    (
        5,
        1,
        'PROMOTION',
        '홍보',
        2,
        'Y',
        NULL,
        NULL,
        '2025-07-16 05:09:54',
        NULL,
        NULL,
        '2025-07-16 05:09:54'
    ),
    (
        6,
        1,
        'EXTERNAL_PROMOTION',
        '유관기관 홍보',
        3,
        'Y',
        NULL,
        NULL,
        '2025-07-16 05:09:54',
        NULL,
        NULL,
        '2025-07-16 05:09:54'
    );
/*!40000 ALTER TABLE `bbs_category` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `bbs_comment`
--

DROP TABLE IF EXISTS `bbs_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `bbs_comment` (
    `COMMENT_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'PK: 댓글 ID',
    `NTT_ID` int(10) unsigned NOT NULL COMMENT 'FK: 게시글 ID',
    `CONTENT` text NOT NULL COMMENT '댓글 내용',
    `WRITER` varchar(50) NOT NULL COMMENT '작성자 ID',
    `DISPLAY_WRITER` varchar(50) DEFAULT NULL COMMENT '표시될 작성자명',
    `IS_DELETED` char(1) NOT NULL DEFAULT 'N' COMMENT '삭제 여부 (Y/N)',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    PRIMARY KEY (`COMMENT_ID`),
    KEY `idx_voice_comment_ntt_id` (`NTT_ID`),
    CONSTRAINT `fk_voice_comment_to_bbs_article` FOREIGN KEY (`NTT_ID`) REFERENCES `bbs_article` (`NTT_ID`) ON DELETE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 62 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '고객의 소리 댓글';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `bbs_comment`
--

LOCK TABLES `bbs_comment` WRITE;
/*!40000 ALTER TABLE `bbs_comment` DISABLE KEYS */
;
/*!40000 ALTER TABLE `bbs_comment` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `bbs_master`
--

DROP TABLE IF EXISTS `bbs_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `bbs_master` (
    `BBS_ID` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK: 게시판 ID',
    `BBS_NAME` varchar(100) NOT NULL COMMENT '게시판 이름',
    `SKIN_TYPE` enum(
        'BASIC',
        'FAQ',
        'QNA',
        'PRESS',
        'FORM'
    ) NOT NULL COMMENT '게시판 스킨 유형',
    `READ_AUTH` varchar(50) NOT NULL COMMENT '읽기 권한 코드',
    `WRITE_AUTH` varchar(50) NOT NULL COMMENT '쓰기 권한 코드',
    `ADMIN_AUTH` varchar(50) NOT NULL COMMENT '관리 권한 코드',
    `DISPLAY_YN` varchar(1) DEFAULT 'Y' COMMENT '노출 여부',
    `SORT_ORDER` varchar(1) DEFAULT 'D' COMMENT '게시판 정렬 순서(A=오름차순,D=내림차순)',
    `NOTICE_YN` varchar(1) DEFAULT 'N' COMMENT '공지 여부(Y=공지,N=미공지)',
    `PUBLISH_YN` varchar(1) DEFAULT 'N' COMMENT '게시 여부(Y=게시,N=미게시)',
    `ATTACHMENT_YN` varchar(1) DEFAULT 'N' COMMENT '첨부파일 기능 사용여부',
    `ATTACHMENT_LIMIT` int(11) DEFAULT 0 COMMENT '첨부파일 최대 개수',
    `ATTACHMENT_SIZE` int(11) DEFAULT 0 COMMENT '첨부파일 최대 용량(MB)',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    PRIMARY KEY (`BBS_ID`)
) ENGINE = InnoDB AUTO_INCREMENT = 66 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '게시판 설정';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `bbs_master`
--

LOCK TABLES `bbs_master` WRITE;
/*!40000 ALTER TABLE `bbs_master` DISABLE KEYS */
;
INSERT INTO
    `bbs_master`
VALUES (
        1,
        '공지사항',
        'BASIC',
        'USER',
        'ADMIN',
        'ADMIN',
        'Y',
        'D',
        'Y',
        'Y',
        'Y',
        4,
        10,
        NULL,
        NULL,
        '2025-05-03 06:18:30',
        NULL,
        NULL,
        '2025-07-18 09:27:26'
    ),
    (
        2,
        '자주 묻는 질문',
        'FAQ',
        'USER',
        'ADMIN',
        'ADMIN',
        'Y',
        'D',
        'Y',
        'Y',
        'N',
        0,
        0,
        NULL,
        NULL,
        '2025-05-03 06:18:30',
        NULL,
        NULL,
        '2025-06-13 05:31:21'
    ),
    (
        3,
        '고객의 소리',
        'QNA',
        'USER',
        'USER',
        'ADMIN',
        'N',
        'D',
        'Y',
        'Y',
        'Y',
        3,
        5,
        NULL,
        NULL,
        '2025-05-03 06:18:30',
        NULL,
        NULL,
        '2025-07-21 01:46:13'
    ),
    (
        4,
        '보도자료',
        'PRESS',
        'USER',
        'ADMIN',
        'ADMIN',
        'Y',
        'D',
        'Y',
        'Y',
        'Y',
        5,
        10,
        NULL,
        NULL,
        '2025-05-03 06:18:30',
        NULL,
        NULL,
        '2025-06-13 05:31:21'
    ),
    (
        5,
        '자료실',
        'FORM',
        'USER',
        'ADMIN',
        'ADMIN',
        'Y',
        'D',
        'Y',
        'Y',
        'Y',
        10,
        20,
        NULL,
        NULL,
        '2025-05-03 06:18:30',
        NULL,
        NULL,
        '2025-06-13 05:31:21'
    ),
    (
        6,
        '갤러리',
        'BASIC',
        'USER',
        'ADMIN',
        'ADMIN',
        'Y',
        'D',
        'Y',
        'Y',
        'Y',
        10,
        50,
        NULL,
        NULL,
        '2025-06-12 00:15:29',
        NULL,
        NULL,
        '2025-06-19 07:50:00'
    );
/*!40000 ALTER TABLE `bbs_master` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `content_block_files`
--

DROP TABLE IF EXISTS `content_block_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `content_block_files` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '고유 식별자 (PK)',
    `content_block_id` bigint(20) NOT NULL COMMENT 'content_blocks 테이블 ID (FK)',
    `file_id` bigint(20) NOT NULL COMMENT 'files 테이블 ID (FK)',
    `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '블록 내 파일 표시 순서',
    `created_by` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `created_ip` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `created_date` datetime DEFAULT current_timestamp() COMMENT '생성 일시',
    `updated_by` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `updated_ip` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    PRIMARY KEY (`id`),
    KEY `idx_content_block_id` (`content_block_id`),
    KEY `idx_file_id` (`file_id`),
    CONSTRAINT `fk_content_block_files_to_content_block` FOREIGN KEY (`content_block_id`) REFERENCES `content_blocks` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_block_files_to_file` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 57 DEFAULT CHARSET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '콘텐츠 블록과 파일의 다대다 관계 테이블';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `content_block_files`
--

LOCK TABLES `content_block_files` WRITE;
/*!40000 ALTER TABLE `content_block_files` DISABLE KEYS */
;
/*!40000 ALTER TABLE `content_block_files` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `content_block_history`
--

DROP TABLE IF EXISTS `content_block_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `content_block_history` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '히스토리 레코드의 고유 식별자 (Primary Key)',
    `content_block_id` bigint(20) NOT NULL COMMENT '이력이 속한 원본 콘텐츠 블록의 ID (Foreign Key to content_blocks.id)',
    `version` int(11) NOT NULL COMMENT '변경 이력의 순차적인 버전 번호',
    `type` varchar(20) NOT NULL COMMENT '콘텐츠 블록의 유형 (예: TEXT, IMAGE)',
    `content` text DEFAULT NULL COMMENT '해당 버전의 텍스트 내용 또는 이미지의 캡션',
    `file_ids_json` longtext DEFAULT NULL COMMENT '파일 ID 목록을 저장하는 JSON 필드',
    `created_by` varchar(36) DEFAULT NULL COMMENT '해당 버전을 생성한 사용자의 ID',
    `created_ip` varchar(45) DEFAULT NULL COMMENT '해당 버전을 생성한 사용자의 IP 주소',
    `created_date` datetime DEFAULT current_timestamp() COMMENT '해당 버전 레코드가 생성된 일시',
    `updated_by` varchar(36) DEFAULT NULL COMMENT '해당 버전 레코드를 수정한 사용자의 ID',
    `updated_ip` varchar(45) DEFAULT NULL COMMENT '해당 버전 레코드를 수정한 사용자의 IP 주소',
    `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '해당 버전 레코드가 마지막으로 수정된 일시',
    PRIMARY KEY (`id`),
    KEY `fk_history_content_block` (`content_block_id`) COMMENT '성능 향상을 위해 content_block_id에 인덱스 설정',
    CONSTRAINT `fk_history_content_block` FOREIGN KEY (`content_block_id`) REFERENCES `content_blocks` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 43 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '콘텐츠 블록의 변경 이력을 저장하는 테이블';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `content_block_history`
--

LOCK TABLES `content_block_history` WRITE;
/*!40000 ALTER TABLE `content_block_history` DISABLE KEYS */
;
INSERT INTO
    `content_block_history`
VALUES (
        34,
        29,
        1,
        'TEXT',
        '새 텍스트 블록',
        '[]',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:24',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:24'
    ),
    (
        35,
        30,
        1,
        'IMAGE',
        '새 이미지 캡션',
        '[]',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:46',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:46'
    ),
    (
        36,
        31,
        1,
        'IMAGE',
        '새 이미지 캡션',
        '[]',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:58',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:58'
    ),
    (
        37,
        32,
        1,
        'TEXT',
        '새 텍스트 블록',
        '[]',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:06:27',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:06:27'
    ),
    (
        38,
        33,
        1,
        'IMAGE',
        '새 이미지 캡션',
        '[]',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:06:44',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:06:44'
    ),
    (
        40,
        35,
        1,
        'TEXT',
        '새 텍스트 블록',
        '[]',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:25',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:25'
    ),
    (
        41,
        33,
        2,
        'IMAGE',
        '새 이미지 캡션',
        '[562]',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:42',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:42'
    ),
    (
        42,
        36,
        1,
        'IMAGE',
        '새 이미지 캡션',
        '[]',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:57',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:57'
    );
/*!40000 ALTER TABLE `content_block_history` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `content_blocks`
--

DROP TABLE IF EXISTS `content_blocks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `content_blocks` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `menu_id` int(11) DEFAULT NULL,
    `type` varchar(20) NOT NULL,
    `content` text DEFAULT NULL,
    `file_id` bigint(20) DEFAULT NULL,
    `sort_order` int(11) NOT NULL DEFAULT 0,
    `version` int(11) NOT NULL DEFAULT 1 COMMENT '콘텐츠 버전',
    `created_by` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `created_ip` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `created_date` datetime DEFAULT current_timestamp() COMMENT '생성 일시',
    `updated_by` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `updated_ip` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    PRIMARY KEY (`id`),
    KEY `fk_content_blocks_menu` (`menu_id`),
    KEY `fk_content_blocks_file` (`file_id`),
    CONSTRAINT `fk_content_blocks_file` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_content_blocks_menu` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 37 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `content_blocks`
--

LOCK TABLES `content_blocks` WRITE;
/*!40000 ALTER TABLE `content_blocks` DISABLE KEYS */
;
INSERT INTO
    `content_blocks`
VALUES (
        29,
        NULL,
        'TEXT',
        '광안리 · 해운대 · 센텀시티를 잇는 이상적인 허브',
        NULL,
        0,
        2,
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:15',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:24'
    ),
    (
        30,
        NULL,
        'IMAGE',
        '메인 헤더',
        NULL,
        1,
        2,
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:26',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:46'
    ),
    (
        31,
        NULL,
        'IMAGE',
        '새 이미지 캡션',
        NULL,
        2,
        2,
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:50',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:05:58'
    ),
    (
        32,
        NULL,
        'TEXT',
        '도심 속 합리적인 컨벤션 & 스테이',
        NULL,
        3,
        2,
        'admin',
        '172.30.1.254',
        '2025-07-18 18:06:08',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:15'
    ),
    (
        33,
        NULL,
        'IMAGE',
        '새 이미지 캡션',
        NULL,
        5,
        3,
        'admin',
        '172.30.1.254',
        '2025-07-18 18:06:32',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:42'
    ),
    (
        35,
        NULL,
        'TEXT',
        'Busan Youth Hostel Arpina Busan Youth Hostel Arpina',
        NULL,
        4,
        2,
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:11',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:25'
    ),
    (
        36,
        NULL,
        'IMAGE',
        '새 이미지 캡션',
        NULL,
        6,
        2,
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:50',
        'admin',
        '172.30.1.254',
        '2025-07-18 18:07:57'
    );
/*!40000 ALTER TABLE `content_blocks` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `file`
--

DROP TABLE IF EXISTS `file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `file` (
    `file_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'PK: 파일 ID',
    `menu` varchar(30) NOT NULL COMMENT '메뉴 코드 (BBS, POPUP 등)',
    `menu_id` bigint(20) NOT NULL COMMENT '메뉴별 리소스 ID',
    `origin_name` varchar(255) NOT NULL COMMENT '원본 파일명',
    `saved_name` varchar(255) NOT NULL COMMENT '저장된 파일명',
    `mime_type` varchar(100) NOT NULL COMMENT 'MIME 타입',
    `size` bigint(20) NOT NULL COMMENT '파일 크기(바이트)',
    `ext` varchar(20) NOT NULL COMMENT '파일 확장자',
    `version` int(11) DEFAULT 1 COMMENT '파일 버전',
    `public_yn` varchar(1) DEFAULT 'Y' COMMENT '공개 여부 (Y/N)',
    `file_order` int(11) DEFAULT 0 COMMENT '파일 순서',
    `created_by` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `created_ip` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `created_date` datetime DEFAULT current_timestamp() COMMENT '생성 일시',
    `updated_by` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `updated_ip` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    PRIMARY KEY (`file_id`),
    UNIQUE KEY `uk_saved_name` (`saved_name`),
    KEY `idx_file_public` (`public_yn`),
    KEY `idx_file_menu` (`menu`, `menu_id`),
    KEY `idx_file_order` (
        `menu`,
        `menu_id`,
        `file_order`
    ),
    KEY `idx_file_public_menu` (
        `menu`,
        `menu_id`,
        `public_yn`
    ),
    CONSTRAINT `chk_public_yn` CHECK (`public_yn` in ('Y', 'N'))
) ENGINE = InnoDB AUTO_INCREMENT = 566 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'CMS 파일';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `file`
--

LOCK TABLES `file` WRITE;
/*!40000 ALTER TABLE `file` DISABLE KEYS */
;
/*!40000 ALTER TABLE `file` ENABLE KEYS */
;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */
;
/*!50003 SET @saved_cs_results     = @@character_set_results */
;
/*!50003 SET @saved_col_connection = @@collation_connection */
;
/*!50003 SET character_set_client  = utf8mb4 */
;
/*!50003 SET character_set_results = utf8mb4 */
;
/*!50003 SET collation_connection  = utf8mb4_general_ci */
;
/*!50003 SET @saved_sql_mode       = @@sql_mode */
;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */
;
DELIMITER; ;
/*!50003 CREATE*/
/*!50017 DEFINER=`handy`@`%`*/
/*!50003 TRIGGER trg_file_public_yn_check_insert BEFORE INSERT ON file
FOR EACH ROW
BEGIN
IF NEW.public_yn NOT IN ('Y', 'N') THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'public_yn must be Y or N';
END IF;
END */
; ;
DELIMITER;
/*!50003 SET sql_mode              = @saved_sql_mode */
;
/*!50003 SET character_set_client  = @saved_cs_client */
;
/*!50003 SET character_set_results = @saved_cs_results */
;
/*!50003 SET collation_connection  = @saved_col_connection */
;
/*!50003 SET @saved_cs_client      = @@character_set_client */
;
/*!50003 SET @saved_cs_results     = @@character_set_results */
;
/*!50003 SET @saved_col_connection = @@collation_connection */
;
/*!50003 SET character_set_client  = utf8mb4 */
;
/*!50003 SET character_set_results = utf8mb4 */
;
/*!50003 SET collation_connection  = utf8mb4_general_ci */
;
/*!50003 SET @saved_sql_mode       = @@sql_mode */
;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */
;
DELIMITER; ;
/*!50003 CREATE*/
/*!50017 DEFINER=`handy`@`%`*/
/*!50003 TRIGGER trg_file_order_check_insert BEFORE INSERT ON file
FOR EACH ROW
BEGIN
IF NEW.file_order < 0 THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'file_order must be greater than or equal to 0';
END IF;
END */
; ;
DELIMITER;
/*!50003 SET sql_mode              = @saved_sql_mode */
;
/*!50003 SET character_set_client  = @saved_cs_client */
;
/*!50003 SET character_set_results = @saved_cs_results */
;
/*!50003 SET collation_connection  = @saved_col_connection */
;
/*!50003 SET @saved_cs_client      = @@character_set_client */
;
/*!50003 SET @saved_cs_results     = @@character_set_results */
;
/*!50003 SET @saved_col_connection = @@collation_connection */
;
/*!50003 SET character_set_client  = utf8mb4 */
;
/*!50003 SET character_set_results = utf8mb4 */
;
/*!50003 SET collation_connection  = utf8mb4_general_ci */
;
/*!50003 SET @saved_sql_mode       = @@sql_mode */
;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */
;
DELIMITER; ;
/*!50003 CREATE*/
/*!50017 DEFINER=`handy`@`%`*/
/*!50003 TRIGGER trg_file_public_yn_check_update BEFORE UPDATE ON file
FOR EACH ROW
BEGIN
IF NEW.public_yn NOT IN ('Y', 'N') THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'public_yn must be Y or N';
END IF;
END */
; ;
DELIMITER;
/*!50003 SET sql_mode              = @saved_sql_mode */
;
/*!50003 SET character_set_client  = @saved_cs_client */
;
/*!50003 SET character_set_results = @saved_cs_results */
;
/*!50003 SET collation_connection  = @saved_col_connection */
;
/*!50003 SET @saved_cs_client      = @@character_set_client */
;
/*!50003 SET @saved_cs_results     = @@character_set_results */
;
/*!50003 SET @saved_col_connection = @@collation_connection */
;
/*!50003 SET character_set_client  = utf8mb4 */
;
/*!50003 SET character_set_results = utf8mb4 */
;
/*!50003 SET collation_connection  = utf8mb4_general_ci */
;
/*!50003 SET @saved_sql_mode       = @@sql_mode */
;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */
;
DELIMITER; ;
/*!50003 CREATE*/
/*!50017 DEFINER=`handy`@`%`*/
/*!50003 TRIGGER trg_file_order_check_update BEFORE UPDATE ON file
FOR EACH ROW
BEGIN
IF NEW.file_order < 0 THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'file_order must be greater than or equal to 0';
END IF;
END */
; ;
DELIMITER;
/*!50003 SET sql_mode              = @saved_sql_mode */
;
/*!50003 SET character_set_client  = @saved_cs_client */
;
/*!50003 SET character_set_results = @saved_cs_results */
;
/*!50003 SET collation_connection  = @saved_col_connection */
;

--
-- Table structure for table `groups`
--

DROP TABLE IF EXISTS `groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `groups` (
    `uuid` varchar(36) NOT NULL,
    `name` varchar(100) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `created_by` varchar(36) DEFAULT NULL,
    `created_ip` varchar(45) DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `updated_by` varchar(36) DEFAULT NULL,
    `updated_ip` varchar(45) DEFAULT NULL,
    `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`uuid`),
    KEY `created_by` (`created_by`),
    KEY `updated_by` (`updated_by`),
    CONSTRAINT `groups_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `user` (`uuid`),
    CONSTRAINT `groups_ibfk_2` FOREIGN KEY (`updated_by`) REFERENCES `user` (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `groups`
--

LOCK TABLES `groups` WRITE;
/*!40000 ALTER TABLE `groups` DISABLE KEYS */
;
INSERT INTO
    `groups`
VALUES (
        '00000000-0000-0000-0000-000000000000',
        'Default Group',
        'System default group',
        NULL,
        NULL,
        '2025-06-03 03:02:14',
        NULL,
        NULL,
        '2025-06-03 03:02:14'
    );
/*!40000 ALTER TABLE `groups` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `main_media`
--

DROP TABLE IF EXISTS `main_media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `main_media` (
    `media_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'PK: 메인 미디어 ID',
    `title` varchar(255) NOT NULL COMMENT '미디어 제목',
    `description` text DEFAULT NULL COMMENT '미디어 설명',
    `media_type` enum('IMAGE', 'VIDEO') NOT NULL COMMENT '미디어 타입 (IMAGE/VIDEO)',
    `display_order` int(11) NOT NULL DEFAULT 0 COMMENT '화면 표시 순서',
    `file_id` bigint(20) NOT NULL COMMENT 'FK: 파일 ID',
    `public_yn` varchar(1) DEFAULT 'Y' COMMENT '공개 여부 (Y/N)',
    `created_by` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `created_ip` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `created_date` datetime DEFAULT current_timestamp() COMMENT '생성 일시',
    `updated_by` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `updated_ip` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    PRIMARY KEY (`media_id`),
    KEY `idx_main_media_display_public` (`public_yn`, `display_order`) COMMENT '공개 여부, 표시 순서 정렬용 인덱스',
    KEY `idx_main_media_created` (`created_date`) COMMENT '생성일시 정렬용 인덱스',
    KEY `idx_main_media_type_public` (`media_type`, `public_yn`) COMMENT '미디어 타입별 조회용 인덱스',
    KEY `fk_main_media_file` (`file_id`),
    CONSTRAINT `fk_main_media_file` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`),
    CONSTRAINT `chk_main_media_public_yn` CHECK (`public_yn` in ('Y', 'N'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '메인 미디어 관리';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `main_media`
--

LOCK TABLES `main_media` WRITE;
/*!40000 ALTER TABLE `main_media` DISABLE KEYS */
;
/*!40000 ALTER TABLE `main_media` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `menu`
--

DROP TABLE IF EXISTS `menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `menu` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL,
    `type` enum(
        'LINK',
        'FOLDER',
        'BOARD',
        'CONTENT',
        'PROGRAM'
    ) NOT NULL COMMENT '메뉴 타입',
    `url` varchar(255) DEFAULT NULL,
    `target_id` bigint(20) unsigned DEFAULT NULL COMMENT '연결 대상 ID (BOARD/CONTENT/PROGRAM 타입일 때 필수)',
    `display_position` varchar(50) NOT NULL,
    `visible` tinyint(1) DEFAULT 1,
    `sort_order` int(11) NOT NULL,
    `parent_id` int(11) DEFAULT NULL,
    `created_by` varchar(36) DEFAULT NULL,
    `created_ip` varchar(45) DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `updated_by` varchar(36) DEFAULT NULL,
    `updated_ip` varchar(45) DEFAULT NULL,
    `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`id`),
    KEY `parent_id` (`parent_id`),
    KEY `created_by` (`created_by`),
    KEY `updated_by` (`updated_by`),
    CONSTRAINT `menu_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `menu` (`id`) ON DELETE SET NULL,
    CONSTRAINT `menu_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`uuid`),
    CONSTRAINT `menu_ibfk_3` FOREIGN KEY (`updated_by`) REFERENCES `user` (`uuid`)
) ENGINE = InnoDB AUTO_INCREMENT = 102 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `menu`
--

LOCK TABLES `menu` WRITE;
/*!40000 ALTER TABLE `menu` DISABLE KEYS */
;
INSERT INTO
    `menu`
VALUES (
        51,
        '알림',
        'FOLDER',
        '/bbs/notices',
        NULL,
        'HEADER',
        1,
        7,
        NULL,
        NULL,
        NULL,
        '2025-05-14 21:53:24',
        NULL,
        NULL,
        '2025-07-11 08:38:07'
    ),
    (
        79,
        '공지사항',
        'BOARD',
        '/bbs/notices',
        1,
        'HEADER',
        1,
        0,
        51,
        NULL,
        NULL,
        '2025-05-14 22:15:22',
        NULL,
        NULL,
        '2025-06-12 01:03:48'
    ),
    (
        80,
        '갤러리',
        'BOARD',
        '/bbs/gallery',
        6,
        'HEADER',
        1,
        0,
        51,
        NULL,
        NULL,
        '2025-05-14 22:16:05',
        NULL,
        NULL,
        '2025-07-07 06:12:13'
    ),
    (
        81,
        '자료실',
        'BOARD',
        '/bbs/resources',
        5,
        'HEADER',
        1,
        0,
        51,
        NULL,
        NULL,
        '2025-05-14 22:16:26',
        NULL,
        NULL,
        '2025-06-12 01:04:09'
    ),
    (
        82,
        '자주 묻는 질문',
        'BOARD',
        '/bbs/faq',
        2,
        'HEADER',
        1,
        0,
        51,
        NULL,
        NULL,
        '2025-05-14 22:16:52',
        NULL,
        NULL,
        '2025-06-12 01:04:17'
    ),
    (
        83,
        '고객의 소리',
        'LINK',
        'https://www.bmc.busan.kr/bmc/contents.do?mId=0304000000',
        3,
        'HEADER',
        1,
        0,
        51,
        NULL,
        NULL,
        '2025-05-14 22:17:10',
        NULL,
        NULL,
        '2025-07-21 02:09:58'
    );
/*!40000 ALTER TABLE `menu` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `organizations`
--

DROP TABLE IF EXISTS `organizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `organizations` (
    `uuid` varchar(36) NOT NULL,
    `name` varchar(100) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `created_by` varchar(36) DEFAULT NULL,
    `created_ip` varchar(45) DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `updated_by` varchar(36) DEFAULT NULL,
    `updated_ip` varchar(45) DEFAULT NULL,
    `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`uuid`),
    KEY `created_by` (`created_by`),
    KEY `updated_by` (`updated_by`),
    CONSTRAINT `organizations_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `user` (`uuid`),
    CONSTRAINT `organizations_ibfk_2` FOREIGN KEY (`updated_by`) REFERENCES `user` (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `organizations`
--

LOCK TABLES `organizations` WRITE;
/*!40000 ALTER TABLE `organizations` DISABLE KEYS */
;
/*!40000 ALTER TABLE `organizations` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `payment` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '결제 ID (PK)',
    `enroll_id` bigint(20) NOT NULL COMMENT '수강신청 ID (FK)',
    `status` varchar(50) NOT NULL COMMENT '결제 상태 (PAID, FAILED, CANCELED, PARTIAL_REFUNDED, REFUND_REQUESTED)',
    `paid_at` timestamp NULL DEFAULT NULL COMMENT '결제 일시',
    `moid` varchar(255) DEFAULT NULL COMMENT 'KISPG 주문번호 (temp_*, enroll_* 형식)',
    `tid` varchar(100) DEFAULT NULL COMMENT 'KISPG 거래 ID',
    `paid_amt` int(11) DEFAULT NULL COMMENT '실제 KISPG 확인 금액',
    `lesson_amount` int(11) DEFAULT NULL COMMENT '강습 결제 금액',
    `locker_amount` int(11) DEFAULT NULL COMMENT '사물함 결제 금액',
    `refunded_amt` int(11) DEFAULT 0 COMMENT '환불된 금액',
    `refund_dt` datetime DEFAULT NULL COMMENT '환불 일시',
    `pay_method` varchar(50) DEFAULT NULL COMMENT '결제 수단 (CARD, VBANK 등)',
    `pg_result_code` varchar(20) DEFAULT NULL COMMENT 'PG 결과 코드',
    `pg_result_msg` varchar(255) DEFAULT NULL COMMENT 'PG 결과 메시지',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_tid` (`tid`),
    KEY `idx_payment_moid` (`moid`),
    KEY `idx_payment_status` (`status`),
    KEY `fk_payment_enroll_id` (`enroll_id`),
    CONSTRAINT `fk_payment_enroll_id` FOREIGN KEY (`enroll_id`) REFERENCES `enroll` (`id`) ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 3320 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '결제 정보 (KISPG 전용 최적화)';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */
;
/*!40000 ALTER TABLE `payment` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `popup`
--

DROP TABLE IF EXISTS `popup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `popup` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '팝업 고유 ID',
    `title` varchar(100) NOT NULL COMMENT '팝업 제목',
    `content` text NOT NULL COMMENT '팝업 HTML 콘텐츠 (Lexical Editor)',
    `start_date` datetime NOT NULL COMMENT '노출 시작일시',
    `end_date` datetime NOT NULL COMMENT '노출 종료일시',
    `is_visible` tinyint(1) DEFAULT 1 COMMENT '노출 여부',
    `display_order` int(10) unsigned DEFAULT 1 COMMENT '노출 순서 (낮을수록 먼저)',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 9 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `popup`
--

LOCK TABLES `popup` WRITE;
/*!40000 ALTER TABLE `popup` DISABLE KEYS */
;
/*!40000 ALTER TABLE `popup` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `schedule`
--

DROP TABLE IF EXISTS `schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `schedule` (
    `schedule_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'PK: 일정 ID',
    `title` varchar(255) NOT NULL COMMENT '일정 제목',
    `content` text DEFAULT NULL COMMENT '일정 내용',
    `start_date_time` datetime NOT NULL COMMENT '시작 일시',
    `end_date_time` datetime NOT NULL COMMENT '종료 일시',
    `display_yn` varchar(1) DEFAULT 'Y' COMMENT '노출 여부',
    `created_by` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `created_ip` varchar(45) DEFAULT NULL COMMENT '생성자 IP',
    `created_date` datetime DEFAULT current_timestamp() COMMENT '생성 일시',
    `updated_by` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `updated_ip` varchar(45) DEFAULT NULL COMMENT '수정자 IP',
    `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    PRIMARY KEY (`schedule_id`),
    UNIQUE KEY `uk_schedule_unique` (`title`, `start_date_time`) COMMENT '동일한 제목과 시작 시간의 일정 중복 방지',
    KEY `idx_schedule_display` (`display_yn`) COMMENT '노출 여부 인덱스',
    KEY `idx_schedule_time_range` (
        `start_date_time`,
        `end_date_time`
    ) COMMENT '시간 범위 검색 인덱스'
) ENGINE = InnoDB AUTO_INCREMENT = 5 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '일정';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `schedule`
--

LOCK TABLES `schedule` WRITE;
/*!40000 ALTER TABLE `schedule` DISABLE KEYS */
;
/*!40000 ALTER TABLE `schedule` ENABLE KEYS */
;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */
;
/*!50003 SET @saved_cs_results     = @@character_set_results */
;
/*!50003 SET @saved_col_connection = @@collation_connection */
;
/*!50003 SET character_set_client  = utf8mb4 */
;
/*!50003 SET character_set_results = utf8mb4 */
;
/*!50003 SET collation_connection  = utf8mb4_general_ci */
;
/*!50003 SET @saved_sql_mode       = @@sql_mode */
;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */
;
DELIMITER; ;
/*!50003 CREATE*/
/*!50017 DEFINER=`handy`@`%`*/
/*!50003 TRIGGER trg_schedule_time_check
BEFORE INSERT ON schedule
FOR EACH ROW
BEGIN
IF NEW.start_date_time >= NEW.end_date_time THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = '시작 시간은 종료 시간보다 빨라야 합니다.';
END IF;
END */
; ;
DELIMITER;
/*!50003 SET sql_mode              = @saved_sql_mode */
;
/*!50003 SET character_set_client  = @saved_cs_client */
;
/*!50003 SET character_set_results = @saved_cs_results */
;
/*!50003 SET collation_connection  = @saved_col_connection */
;
/*!50003 SET @saved_cs_client      = @@character_set_client */
;
/*!50003 SET @saved_cs_results     = @@character_set_results */
;
/*!50003 SET @saved_col_connection = @@collation_connection */
;
/*!50003 SET character_set_client  = utf8mb4 */
;
/*!50003 SET character_set_results = utf8mb4 */
;
/*!50003 SET collation_connection  = utf8mb4_general_ci */
;
/*!50003 SET @saved_sql_mode       = @@sql_mode */
;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */
;
DELIMITER; ;
/*!50003 CREATE*/
/*!50017 DEFINER=`handy`@`%`*/
/*!50003 TRIGGER trg_schedule_time_check_update
BEFORE UPDATE ON schedule
FOR EACH ROW
BEGIN
IF NEW.start_date_time >= NEW.end_date_time THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = '시작 시간은 종료 시간보다 빨라야 합니다.';
END IF;
END */
; ;
DELIMITER;
/*!50003 SET sql_mode              = @saved_sql_mode */
;
/*!50003 SET character_set_client  = @saved_cs_client */
;
/*!50003 SET character_set_results = @saved_cs_results */
;
/*!50003 SET collation_connection  = @saved_col_connection */
;

--
-- Table structure for table `service`
--

DROP TABLE IF EXISTS `service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `service` (
    `SERVICE_ID` varchar(36) NOT NULL COMMENT '서비스 고유 ID',
    `SERVICE_CODE` varchar(50) NOT NULL COMMENT '서비스 코드 (중복 불가)',
    `SERVICE_NAME` varchar(100) NOT NULL COMMENT '서비스 이름',
    `SERVICE_DOMAIN` varchar(255) DEFAULT NULL COMMENT '서비스 도메인 (예: https://example.com)',
    `API_BASE_URL` varchar(255) DEFAULT NULL COMMENT 'API 기본 URL',
    `DB_CONNECTION_INFO` text DEFAULT NULL COMMENT '암호화된 DB 접속 정보',
    `STATUS` varchar(20) DEFAULT 'ACTIVE' COMMENT '서비스 상태 (ACTIVE, INACTIVE, MAINTENANCE)',
    `DESCRIPTION` text DEFAULT NULL COMMENT '서비스 설명',
    `CONFIG` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '서비스 설정 정보' CHECK (json_valid(`CONFIG`)),
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    PRIMARY KEY (`SERVICE_ID`),
    UNIQUE KEY `unique_service_code` (`SERVICE_CODE`),
    KEY `CREATED_BY` (`CREATED_BY`),
    KEY `UPDATED_BY` (`UPDATED_BY`),
    KEY `idx_service_code` (`SERVICE_CODE`),
    KEY `idx_status_created` (`STATUS`, `CREATED_AT`),
    KEY `idx_domain` (`SERVICE_DOMAIN`),
    CONSTRAINT `service_ibfk_1` FOREIGN KEY (`CREATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `service_ibfk_2` FOREIGN KEY (`UPDATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '서비스 정보 관리';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `service`
--

LOCK TABLES `service` WRITE;
/*!40000 ALTER TABLE `service` DISABLE KEYS */
;
/*!40000 ALTER TABLE `service` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `service_group`
--

DROP TABLE IF EXISTS `service_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `service_group` (
    `GROUP_ID` varchar(36) NOT NULL COMMENT '그룹 고유 ID',
    `SERVICE_ID` varchar(36) NOT NULL COMMENT '서비스 ID',
    `GROUP_CODE` varchar(50) NOT NULL COMMENT '그룹 코드',
    `GROUP_NAME` varchar(100) NOT NULL COMMENT '그룹명',
    `DESCRIPTION` text DEFAULT NULL COMMENT '그룹 설명',
    `STATUS` varchar(20) DEFAULT 'ACTIVE' COMMENT '그룹 상태',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    PRIMARY KEY (`GROUP_ID`),
    UNIQUE KEY `unique_service_group_code` (`SERVICE_ID`, `GROUP_CODE`),
    KEY `CREATED_BY` (`CREATED_BY`),
    KEY `UPDATED_BY` (`UPDATED_BY`),
    KEY `idx_status` (`STATUS`),
    CONSTRAINT `service_group_ibfk_1` FOREIGN KEY (`SERVICE_ID`) REFERENCES `service` (`SERVICE_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_group_ibfk_2` FOREIGN KEY (`CREATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `service_group_ibfk_3` FOREIGN KEY (`UPDATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '서비스별 권한 그룹 관리';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `service_group`
--

LOCK TABLES `service_group` WRITE;
/*!40000 ALTER TABLE `service_group` DISABLE KEYS */
;
/*!40000 ALTER TABLE `service_group` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `service_member_group`
--

DROP TABLE IF EXISTS `service_member_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `service_member_group` (
    `SERVICE_ID` varchar(36) NOT NULL COMMENT '서비스 ID',
    `USER_UUID` varchar(36) NOT NULL COMMENT '회원 UUID',
    `GROUP_ID` varchar(36) NOT NULL COMMENT '그룹 ID',
    `IS_ADMIN` tinyint(1) DEFAULT 0 COMMENT '서비스 관리자 여부 (0: 아니오, 1: 예)',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    PRIMARY KEY (`SERVICE_ID`, `USER_UUID`),
    KEY `CREATED_BY` (`CREATED_BY`),
    KEY `UPDATED_BY` (`UPDATED_BY`),
    KEY `idx_group_id` (`GROUP_ID`),
    KEY `idx_user_uuid` (`USER_UUID`),
    CONSTRAINT `service_member_group_ibfk_1` FOREIGN KEY (`SERVICE_ID`) REFERENCES `service` (`SERVICE_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_member_group_ibfk_2` FOREIGN KEY (`GROUP_ID`) REFERENCES `service_group` (`GROUP_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_member_group_ibfk_3` FOREIGN KEY (`USER_UUID`) REFERENCES `user` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_member_group_ibfk_4` FOREIGN KEY (`CREATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `service_member_group_ibfk_5` FOREIGN KEY (`UPDATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '서비스별 회원-그룹 매핑';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `service_member_group`
--

LOCK TABLES `service_member_group` WRITE;
/*!40000 ALTER TABLE `service_member_group` DISABLE KEYS */
;
/*!40000 ALTER TABLE `service_member_group` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `service_permission`
--

DROP TABLE IF EXISTS `service_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `service_permission` (
    `PERMISSION_ID` varchar(36) NOT NULL COMMENT '권한 매핑 고유 ID',
    `SERVICE_ID` varchar(36) NOT NULL COMMENT '서비스 ID',
    `GROUP_ID` varchar(36) DEFAULT NULL COMMENT '그룹 ID (개별 권한인 경우 NULL)',
    `USER_UUID` varchar(36) DEFAULT NULL COMMENT '회원 UUID (그룹 권한인 경우 NULL)',
    `PERMISSION_TYPE` varchar(20) NOT NULL COMMENT '권한 유형 (MENU, BOARD, CONTENT)',
    `TARGET_ID` varchar(36) NOT NULL COMMENT '대상 ID',
    `TARGET_NAME` varchar(100) NOT NULL COMMENT '대상 이름',
    `PERMISSION_LEVEL` varchar(20) NOT NULL DEFAULT 'READ' COMMENT '권한 레벨 (READ, WRITE, ADMIN)',
    `IS_ADDITIONAL` tinyint(1) DEFAULT 0 COMMENT '추가 권한 여부 (0: 기본권한, 1: 추가권한)',
    `STATUS` varchar(20) DEFAULT 'ACTIVE' COMMENT '권한 상태',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시각',
    PRIMARY KEY (`PERMISSION_ID`),
    UNIQUE KEY `unique_permission` (
        `SERVICE_ID`,
        `GROUP_ID`,
        `USER_UUID`,
        `PERMISSION_TYPE`,
        `TARGET_ID`,
        `IS_ADDITIONAL`
    ),
    KEY `CREATED_BY` (`CREATED_BY`),
    KEY `UPDATED_BY` (`UPDATED_BY`),
    KEY `idx_group_id` (`GROUP_ID`),
    KEY `idx_user_uuid` (`USER_UUID`),
    KEY `idx_permission_type` (
        `PERMISSION_TYPE`,
        `TARGET_ID`
    ),
    KEY `idx_additional_status` (`IS_ADDITIONAL`, `STATUS`),
    CONSTRAINT `service_permission_ibfk_1` FOREIGN KEY (`SERVICE_ID`) REFERENCES `service` (`SERVICE_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_permission_ibfk_2` FOREIGN KEY (`GROUP_ID`) REFERENCES `service_group` (`GROUP_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_permission_ibfk_3` FOREIGN KEY (`USER_UUID`) REFERENCES `user` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_permission_ibfk_4` FOREIGN KEY (`CREATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `service_permission_ibfk_5` FOREIGN KEY (`UPDATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '통합 권한 관리';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `service_permission`
--

LOCK TABLES `service_permission` WRITE;
/*!40000 ALTER TABLE `service_permission` DISABLE KEYS */
;
/*!40000 ALTER TABLE `service_permission` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `service_permission_log`
--

DROP TABLE IF EXISTS `service_permission_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `service_permission_log` (
    `LOG_ID` varchar(36) NOT NULL COMMENT '로그 고유 ID',
    `SERVICE_ID` varchar(36) NOT NULL COMMENT '서비스 ID',
    `PERMISSION_ID` varchar(36) NOT NULL COMMENT '권한 ID',
    `USER_UUID` varchar(36) NOT NULL COMMENT '회원 UUID',
    `ACTION` varchar(50) NOT NULL COMMENT '수행 작업',
    `BEFORE_LEVEL` varchar(20) DEFAULT NULL COMMENT '변경 전 권한 레벨',
    `AFTER_LEVEL` varchar(20) DEFAULT NULL COMMENT '변경 후 권한 레벨',
    `IS_ADDITIONAL` tinyint(1) DEFAULT NULL COMMENT '추가 권한 여부',
    `STATUS` varchar(20) NOT NULL COMMENT '처리 상태',
    `REQUEST_IP` varchar(45) DEFAULT NULL COMMENT '요청 IP',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 시각',
    PRIMARY KEY (`LOG_ID`),
    KEY `CREATED_BY` (`CREATED_BY`),
    KEY `idx_service_time` (`SERVICE_ID`, `CREATED_AT`),
    KEY `idx_user_time` (`USER_UUID`, `CREATED_AT`),
    KEY `idx_permission` (`PERMISSION_ID`),
    CONSTRAINT `service_permission_log_ibfk_1` FOREIGN KEY (`SERVICE_ID`) REFERENCES `service` (`SERVICE_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_permission_log_ibfk_2` FOREIGN KEY (`PERMISSION_ID`) REFERENCES `service_permission` (`PERMISSION_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_permission_log_ibfk_3` FOREIGN KEY (`USER_UUID`) REFERENCES `user` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `service_permission_log_ibfk_4` FOREIGN KEY (`CREATED_BY`) REFERENCES `admin_user` (`UUID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '권한 변경 이력';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `service_permission_log`
--

LOCK TABLES `service_permission_log` WRITE;
/*!40000 ALTER TABLE `service_permission_log` DISABLE KEYS */
;
/*!40000 ALTER TABLE `service_permission_log` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `template`
--

DROP TABLE IF EXISTS `template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `template` (
    `TEMPLATE_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '템플릿 고유 식별자',
    `TEMPLATE_NM` varchar(100) NOT NULL COMMENT '템플릿 이름',
    `type` enum('MAIN', 'SUB', 'NORMAL') NOT NULL DEFAULT 'NORMAL' COMMENT '템플릿 역할 (MAIN/SUB/NORMAL)',
    `IS_PUBLISHED` tinyint(1) NOT NULL DEFAULT 0 COMMENT '게시 여부',
    `VERSION_NO` int(11) NOT NULL DEFAULT 1 COMMENT '버전 번호',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 일시',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    `DELETED_YN` tinyint(1) NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    `DESCRIPTION` varchar(500) DEFAULT NULL COMMENT '템플릿 설명',
    `LAYOUT_JSON` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '레이아웃 JSON 데이터',
    PRIMARY KEY (`TEMPLATE_ID`),
    KEY `IDX_TEMPLATE_PUBLISHED` (`IS_PUBLISHED`) COMMENT '게시 상태 검색용 인덱스',
    KEY `IDX_TEMPLATE_ROLE` (`type`) COMMENT '템플릿 역할 검색용 인덱스'
) ENGINE = InnoDB AUTO_INCREMENT = 3 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '템플릿 기본 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `template`
--

LOCK TABLES `template` WRITE;
/*!40000 ALTER TABLE `template` DISABLE KEYS */
;
INSERT INTO
    `template`
VALUES (
        1,
        '기본 메인 템플릿',
        'MAIN',
        1,
        1,
        NULL,
        NULL,
        '2025-04-27 23:02:22',
        NULL,
        NULL,
        '2025-04-27 23:02:22',
        0,
        NULL,
        ''
    ),
    (
        2,
        '기본 서브 템플릿',
        'SUB',
        1,
        1,
        NULL,
        NULL,
        '2025-04-27 23:02:24',
        NULL,
        NULL,
        '2025-04-27 23:02:24',
        0,
        NULL,
        ''
    );
/*!40000 ALTER TABLE `template` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `template_cell`
--

DROP TABLE IF EXISTS `template_cell`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `template_cell` (
    `CELL_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '셀 고유 식별자',
    `ROW_ID` bigint(20) NOT NULL COMMENT '참조하는 행 ID',
    `ORDINAL` int(11) NOT NULL COMMENT '셀 순서',
    `SPAN_JSON` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '반응형 너비 설정 (base/md/lg/xl)' CHECK (json_valid(`SPAN_JSON`)),
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 일시',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    PRIMARY KEY (`CELL_ID`),
    UNIQUE KEY `UK_CELL_ORD` (`ROW_ID`, `ORDINAL`),
    KEY `IDX_TEMPLATE_CELL_ROW` (`ROW_ID`) COMMENT '행별 셀 조회용 인덱스',
    CONSTRAINT `template_cell_ibfk_1` FOREIGN KEY (`ROW_ID`) REFERENCES `template_row` (`ROW_ID`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '템플릿 셀 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `template_cell`
--

LOCK TABLES `template_cell` WRITE;
/*!40000 ALTER TABLE `template_cell` DISABLE KEYS */
;
/*!40000 ALTER TABLE `template_cell` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `template_row`
--

DROP TABLE IF EXISTS `template_row`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `template_row` (
    `ROW_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '행 고유 식별자',
    `TEMPLATE_ID` bigint(20) NOT NULL COMMENT '참조하는 템플릿 ID',
    `ORDINAL` int(11) NOT NULL COMMENT '행 순서',
    `HEIGHT_PX` int(11) DEFAULT NULL COMMENT '행 높이(픽셀)',
    `BG_COLOR` varchar(20) DEFAULT NULL COMMENT '배경색',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 일시',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    PRIMARY KEY (`ROW_ID`),
    KEY `IDX_TEMPLATE_ROW_TEMPLATE` (`TEMPLATE_ID`) COMMENT '템플릿별 행 조회용 인덱스',
    CONSTRAINT `template_row_ibfk_1` FOREIGN KEY (`TEMPLATE_ID`) REFERENCES `template` (`TEMPLATE_ID`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '템플릿 행 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `template_row`
--

LOCK TABLES `template_row` WRITE;
/*!40000 ALTER TABLE `template_row` DISABLE KEYS */
;
/*!40000 ALTER TABLE `template_row` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `template_type`
--

DROP TABLE IF EXISTS `template_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `template_type` (
    `CODE` varchar(20) NOT NULL COMMENT '템플릿 타입 코드',
    `NAME` varchar(50) NOT NULL COMMENT '템플릿 타입 이름',
    `DESCRIPTION` varchar(200) DEFAULT NULL COMMENT '템플릿 타입 설명',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 일시',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    PRIMARY KEY (`CODE`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '템플릿 타입 코드 테이블';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `template_type`
--

LOCK TABLES `template_type` WRITE;
/*!40000 ALTER TABLE `template_type` DISABLE KEYS */
;
INSERT INTO
    `template_type`
VALUES (
        'MAIN',
        '메인 템플릿',
        '사이트의 메인 페이지에 사용되는 템플릿',
        NULL,
        NULL,
        '2025-04-27 23:02:20',
        NULL,
        NULL,
        '2025-04-27 23:02:20'
    ),
    (
        'NORMAL',
        '일반 템플릿',
        '일반 페이지에 사용되는 템플릿',
        NULL,
        NULL,
        '2025-04-27 23:02:20',
        NULL,
        NULL,
        '2025-04-27 23:02:20'
    ),
    (
        'SUB',
        '서브 템플릿',
        '서브 페이지에 사용되는 템플릿',
        NULL,
        NULL,
        '2025-04-27 23:02:20',
        NULL,
        NULL,
        '2025-04-27 23:02:20'
    );
/*!40000 ALTER TABLE `template_type` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `template_version`
--

DROP TABLE IF EXISTS `template_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `template_version` (
    `VERSION_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '버전 고유 식별자',
    `TEMPLATE_ID` bigint(20) NOT NULL COMMENT '참조하는 템플릿 ID',
    `VERSION_NO` int(11) NOT NULL COMMENT '버전 번호',
    `LAYOUT_JSON` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '해당 버전의 레이아웃 JSON 데이터' CHECK (json_valid(`LAYOUT_JSON`)),
    `UPDATER` varchar(50) NOT NULL COMMENT '수정자',
    `CREATED_BY` varchar(36) DEFAULT NULL COMMENT '생성자 ID',
    `CREATED_IP` varchar(45) DEFAULT NULL COMMENT '생성 IP',
    `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp() COMMENT '생성 일시',
    `UPDATED_BY` varchar(36) DEFAULT NULL COMMENT '수정자 ID',
    `UPDATED_IP` varchar(45) DEFAULT NULL COMMENT '수정 IP',
    `UPDATED_AT` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 일시',
    PRIMARY KEY (`VERSION_ID`),
    KEY `IDX_TEMPLATE_VERSION_TEMPLATE` (`TEMPLATE_ID`) COMMENT '템플릿별 버전 조회용 인덱스',
    CONSTRAINT `template_version_ibfk_1` FOREIGN KEY (`TEMPLATE_ID`) REFERENCES `template` (`TEMPLATE_ID`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '템플릿 버전 관리 테이블';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `template_version`
--

LOCK TABLES `template_version` WRITE;
/*!40000 ALTER TABLE `template_version` DISABLE KEYS */
;
/*!40000 ALTER TABLE `template_version` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `user` (
    `uuid` varchar(36) NOT NULL,
    `username` varchar(50) NOT NULL,
    `name` varchar(100) NOT NULL,
    `email` varchar(100) NOT NULL,
    `password` varchar(255) NOT NULL,
    `role` varchar(20) NOT NULL,
    `avatar_url` varchar(255) DEFAULT NULL,
    `status` varchar(20) NOT NULL,
    `organization_id` varchar(36) DEFAULT NULL COMMENT '기관 ID (FK)',
    `group_id` varchar(36) DEFAULT NULL,
    `car_no` varchar(50) DEFAULT NULL COMMENT '차량번호',
    `temp_pw_flag` tinyint(1) DEFAULT 0 COMMENT '임시비밀번호여부 (0: 아니오, 1: 예)',
    `birth_date` varchar(8) DEFAULT NULL COMMENT '생년월일 (YYYYMMDD)',
    `di` varchar(255) DEFAULT NULL COMMENT '본인인증 DI (암호화 저장)',
    `provider` varchar(50) DEFAULT NULL,
    `phone` varchar(50) DEFAULT NULL COMMENT '전화번호',
    `address` varchar(255) DEFAULT NULL COMMENT '주소',
    `gender` varchar(10) DEFAULT NULL COMMENT '성별 (예: MALE, FEMALE)',
    `reset_token_expiry` timestamp NULL DEFAULT NULL COMMENT '비밀번호 재설정 토큰 만료 시간',
    `reset_token` varchar(255) DEFAULT NULL COMMENT '비밀번호 재설정 토큰',
    `is_temporary` tinyint(1) DEFAULT 0 COMMENT '임시 사용자 여부 (0: 아니오, 1: 예)',
    `created_by` varchar(36) DEFAULT NULL,
    `created_ip` varchar(45) DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `updated_by` varchar(36) DEFAULT NULL,
    `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `updated_ip` varchar(45) DEFAULT 'NULL',
    `memo` text DEFAULT NULL COMMENT '관리자 메모 내용',
    `memo_updated_at` timestamp NULL DEFAULT NULL COMMENT '메모 최종 수정 일시',
    `memo_updated_by` varchar(36) DEFAULT NULL COMMENT '메모 최종 수정 관리자 UUID',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `username` (`username`),
    UNIQUE KEY `email` (`email`),
    KEY `created_by` (`created_by`),
    KEY `updated_by` (`updated_by`),
    KEY `fk_user_memo_updated_by` (`memo_updated_by`),
    CONSTRAINT `fk_user_memo_updated_by` FOREIGN KEY (`memo_updated_by`) REFERENCES `user` (`uuid`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `user_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `user` (`uuid`),
    CONSTRAINT `user_ibfk_2` FOREIGN KEY (`updated_by`) REFERENCES `user` (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */
;
INSERT INTO
    `user`
VALUES (
        '00008ee4-3c4f-48b3-a30f-f686fdddb51c',
        'admin',
        'admin',
        'admin@ex.co.kr',
        '$2a$10$ifHo7stsn6Bmb4E9XNvNw.DorLb9BoR/wfSspOknFGwmbmqR/94G6',
        'ADMIN',
        '',
        'ACTIVE',
        NULL,
        NULL,
        NULL,
        0,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        0,
        NULL,
        NULL,
        '2025-08-04 02:28:16',
        NULL,
        '2025-08-04 04:22:16',
        'NULL',
        NULL,
        NULL,
        NULL
    );
/*!40000 ALTER TABLE `user` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `user_activity_log`
--

DROP TABLE IF EXISTS `user_activity_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `user_activity_log` (
    `uuid` varchar(36) NOT NULL,
    `user_uuid` varchar(36) NOT NULL,
    `group_id` varchar(36) DEFAULT NULL,
    `organization_id` varchar(36) NOT NULL,
    `activity_type` varchar(50) NOT NULL,
    `description` varchar(255) NOT NULL,
    `user_agent` varchar(255) DEFAULT NULL,
    `created_by` varchar(36) DEFAULT NULL,
    `created_ip` varchar(45) DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `updated_by` varchar(36) DEFAULT NULL,
    `updated_ip` varchar(45) DEFAULT NULL,
    `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`uuid`),
    KEY `fk_user_activity_log_user` (`user_uuid`),
    KEY `fk_user_activity_log_group` (`group_id`),
    KEY `fk_user_activity_log_organization` (`organization_id`),
    KEY `fk_user_activity_log_created_by` (`created_by`),
    KEY `fk_user_activity_log_updated_by` (`updated_by`),
    CONSTRAINT `fk_user_activity_log_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_activity_log_group` FOREIGN KEY (`group_id`) REFERENCES `groups` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_activity_log_organization` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_activity_log_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `user` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_activity_log_user` FOREIGN KEY (`user_uuid`) REFERENCES `user` (`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `user_activity_log`
--

LOCK TABLES `user_activity_log` WRITE;
/*!40000 ALTER TABLE `user_activity_log` DISABLE KEYS */
;
/*!40000 ALTER TABLE `user_activity_log` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Dumping routines for database 'interated_cms'
--
/*!50003 DROP PROCEDURE IF EXISTS `InsertGroupReservationDummyData` */
;
/*!50003 SET @saved_cs_client      = @@character_set_client */
;
/*!50003 SET @saved_cs_results     = @@character_set_results */
;
/*!50003 SET @saved_col_connection = @@collation_connection */
;
/*!50003 SET character_set_client  = utf8mb4 */
;
/*!50003 SET character_set_results = utf8mb4 */
;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */
;
/*!50003 SET @saved_sql_mode       = @@sql_mode */
;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */
;
DELIMITER; ;

CREATE DEFINER=`handy`@`%` PROCEDURE `InsertGroupReservationDummyData`()
BEGIN

    DECLARE i INT DEFAULT 1;

    DECLARE j INT;

    DECLARE rooms_to_add INT;

    DECLARE last_inquiry_id BIGINT;



    
    WHILE i <= 200 DO



        
        INSERT INTO `group_reservation_inquiries`

        (

            `status`, `event_type`, `event_name`, `seating_arrangement`,

            `adult_attendees`, `child_attendees`, `dining_service_usage`, `other_requests`,

            `customer_group_name`, `customer_region`, `contact_person_name`, `contact_person_dpt`,

            `contact_person_phone`, `contact_person_email`, `privacy_agreed`, `marketing_agreed`,

            `admin_memo`, `created_by`, `created_ip`, `created_date`,

            `updated_by`, `updated_ip`, `updated_date`

        )

        VALUES

        (

            ELT(FLOOR(1 + RAND() * 3), 'PENDING', 'CONFIRMED', 'CANCELED'), 
            ELT(FLOOR(1 + RAND() * 4), '세미나', '워크숍', '컨퍼런스', '교육'), 
            CONCAT('2025년 ', ELT(FLOOR(1 + RAND() * 4), '상반기', '하반기', '1분기', '특별'), ' 행사-', i), 
            ELT(FLOOR(1 + RAND() * 4), '강의식', '극장식', '그룹형', 'ㄷ자형'), 
            FLOOR(10 + RAND() * 190), 
            FLOOR(RAND() * 20), 
            RAND() > 0.5, 
            CONCAT('기타 문의사항 내용입니다. 문의 번호: ', i), 
            CONCAT('(주)', ELT(FLOOR(1 + RAND() * 5), '가나다', '미래', '블루밍', '한국', '글로벌'), ' 컴퍼니'), 
            ELT(FLOOR(1 + RAND() * 5), '서울', '경기', '인천', '부산', '대전'), 
            CONCAT(ELT(FLOOR(1 + RAND() * 5), '김', '이', '박', '최', '정'), ELT(FLOOR(1 + RAND() * 5), '민준', '서연', '도윤', '하은', '지후')), 
            CONCAT(ELT(FLOOR(1 + RAND() * 3), '마케팅', '인사', '개발'), '팀'), 
            CONCAT('010-', LPAD(FLOOR(1000 + RAND() * 9000), 4, '0'), '-', LPAD(FLOOR(1000 + RAND() * 9000), 4, '0')), 
            CONCAT('contact', i, '@dummy-email.com'), 
            TRUE, 
            RAND() > 0.5, 
            IF(RAND() > 0.7, CONCAT('관리자 확인 필요: ', i), NULL), 
            'GUEST', 
            CONCAT(FLOOR(1 + RAND() * 254), '.', FLOOR(1 + RAND() * 254), '.', FLOOR(1 + RAND() * 254), '.', FLOOR(1 + RAND() * 254)), 
            NOW() - INTERVAL FLOOR(RAND() * 365) DAY, 
            'admin', 
            '127.0.0.1', 
            NOW() - INTERVAL FLOOR(RAND() * 10) DAY 
        );



        
        SET last_inquiry_id = LAST_INSERT_ID();

        
        SET rooms_to_add = FLOOR(1 + RAND() * 3);

        SET j = 1;



        WHILE j <= rooms_to_add DO

            
            
            SET @room_size = ELT(FLOOR(1 + RAND() * 3), '대회의실', '중회의실', '소회의실');

            SET @room_type =

                CASE @room_size

                    WHEN '대회의실' THEN '대회의실'

                    WHEN '중회의실' THEN ELT(FLOOR(1 + RAND() * 3), '시걸', '클로버', '자스민')

                    WHEN '소회의실' THEN ELT(FLOOR(1 + RAND() * 3), '가람', '누리', '오션')

                END;



            INSERT INTO `inquiry_room_reservations`

            (

                `inquiry_id`, `room_size_desc`, `room_type_desc`, `start_date`,

                `end_date`, `usage_time_desc`, `created_by`, `created_ip`, `created_date`,

                `updated_by`, `updated_ip`, `updated_date`

            )

            VALUES

            (

                last_inquiry_id, 
                @room_size, 
                @room_type, 
                CURDATE() + INTERVAL FLOOR(10 + RAND() * 50) DAY, 
                CURDATE() + INTERVAL FLOOR(60 + RAND() * 60) DAY, 
                CONCAT(LPAD(FLOOR(9 + RAND() * 4), 2, '0'), ':00 - ', LPAD(FLOOR(14 + RAND() * 5), 2, '0'), ':00'), 
                'GUEST', 
                CONCAT(FLOOR(1 + RAND() * 254), '.', FLOOR(1 + RAND() * 254), '.', FLOOR(1 + RAND() * 254), '.', FLOOR(1 + RAND() * 254)), 
                NOW() - INTERVAL FLOOR(RAND() * 365) DAY, 
                'admin', 
                '127.0.0.1', 
                NOW() - INTERVAL FLOOR(RAND() * 10) DAY 
            );

            SET j = j + 1;

        END WHILE;



        SET i = i + 1;

    END WHILE;

    
    SELECT '회의실 단체 예약 문의 더미 데이터 200건 생성이 완료되었습니다.' AS message;

END ;;

DELIMITER;
/*!50003 SET sql_mode              = @saved_sql_mode */
;
/*!50003 SET character_set_client  = @saved_cs_client */
;
/*!50003 SET character_set_results = @saved_cs_results */
;
/*!50003 SET collation_connection  = @saved_col_connection */
;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */
;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */
;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */
;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */
;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */
;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */
;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */
;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */
;

-- Dump completed on 2025-08-11 17:41:05