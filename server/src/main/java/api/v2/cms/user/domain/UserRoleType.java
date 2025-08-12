package api.v2.cms.user.domain;

public enum UserRoleType {
    SUPER_ADMIN, // 시스템 최고 관리자
    SERVICE_ADMIN, // 서비스 관리자
    SITE_ADMIN, // 사이트 관리자
    ADMIN, // 일반 관리자
    USER, // 일반 사용자
    GUEST // 게스트
}