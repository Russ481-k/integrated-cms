package api.v2.common.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import api.v2.common.user.domain.User;
import api.v2.common.user.domain.UserRoleType;

/**
 * 통합 CMS 접근 권한을 검증하는 컴포넌트
 * 
 * 하이브리드 보안 시스템의 핵심 비즈니스 로직
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Component("integratedCmsAccessChecker")
@RequiredArgsConstructor
public class IntegratedCmsAccessChecker {

    /**
     * 통합 CMS 접근 권한 확인
     * 
     * @param authentication 현재 인증 정보
     * @return 접근 허용 여부
     */
    public boolean hasAccess(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Authentication is null or not authenticated for integrated CMS");
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            log.warn("Principal is not UserDetails for integrated CMS");
            return false;
        }

        User user = extractUser(principal);
        if (user == null) {
            log.warn("Failed to extract user from principal for integrated CMS");
            return false;
        }

        return checkIntegratedCmsAccess(user);
    }

    /**
     * 시스템 전체 관리 권한 확인 (SUPER_ADMIN만)
     * 
     * @param authentication 현재 인증 정보
     * @return 시스템 관리자 권한 여부
     */
    public boolean isSystemAdmin(Authentication authentication) {
        if (!hasAccess(authentication)) {
            return false;
        }

        User user = extractUser(authentication.getPrincipal());
        if (user == null) {
            return false;
        }

        boolean isSystemAdmin = user.getRole() == UserRoleType.SUPER_ADMIN;
        log.debug("System admin check for user {}: {}", user.getUsername(), isSystemAdmin);

        return isSystemAdmin;
    }

    /**
     * 서비스 생성/삭제 권한 확인 (SUPER_ADMIN만)
     * 
     * @param authentication 현재 인증 정보
     * @return 서비스 관리 권한 여부
     */
    public boolean canManageServices(Authentication authentication) {
        return isSystemAdmin(authentication);
    }

    /**
     * 관리자 사용자 관리 권한 확인 (SUPER_ADMIN만)
     * 
     * @param authentication 현재 인증 정보
     * @return 관리자 관리 권한 여부
     */
    public boolean canManageAdmins(Authentication authentication) {
        return isSystemAdmin(authentication);
    }

    /**
     * 통합 권한 관리 접근 권한 확인 (SUPER_ADMIN, SERVICE_ADMIN)
     * 
     * @param authentication 현재 인증 정보
     * @return 권한 관리 접근 여부
     */
    public boolean canAccessPermissions(Authentication authentication) {
        if (!hasAccess(authentication)) {
            return false;
        }

        User user = extractUser(authentication.getPrincipal());
        if (user == null) {
            return false;
        }

        boolean canAccess = user.getRole() == UserRoleType.SUPER_ADMIN ||
                user.getRole() == UserRoleType.SERVICE_ADMIN;

        log.debug("Permission access check for user {}: {}", user.getUsername(), canAccess);

        return canAccess;
    }

    /**
     * 실제 통합 CMS 접근 권한 확인 로직
     */
    private boolean checkIntegratedCmsAccess(User user) {
        UserRoleType role = user.getRole();

        // integrated_cms는 SUPER_ADMIN, SERVICE_ADMIN만 접근 가능
        boolean hasAccess = role == UserRoleType.SUPER_ADMIN || role == UserRoleType.SERVICE_ADMIN;

        log.debug("Integrated CMS access for user {} with role {}: {}",
                user.getUsername(), role, hasAccess);

        if (!hasAccess) {
            log.warn("Integrated CMS access denied for user {} with role {}",
                    user.getUsername(), role);
        }

        return hasAccess;
    }

    /**
     * UserDetails에서 User 객체 추출
     */
    private User extractUser(Object principal) {
        if (principal instanceof User) {
            return (User) principal;
        }

        // CustomUserDetails 등 다른 타입 처리
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            log.debug("Extracting user from UserDetails: {}", userDetails.getUsername());

            // UserDetails에서 User 객체로 변환
            return convertUserDetailsToUser(userDetails);
        }

        return null;
    }

    /**
     * UserDetails에서 User 객체로 변환
     */
    private User convertUserDetailsToUser(UserDetails userDetails) {
        // UserDetails의 권한에서 역할 추출
        UserRoleType role = extractRoleFromAuthorities(userDetails.getAuthorities());

        // 기본 User 객체 생성 (실제 프로덕션에서는 DB에서 조회해야 함)
        return User.builder()
                .uuid("extracted-" + userDetails.getUsername())
                .username(userDetails.getUsername())
                .password(userDetails.getPassword())
                .name(userDetails.getUsername()) // 기본값으로 username 사용
                .email(userDetails.getUsername() + "@extracted.com") // 기본 이메일
                .role(role)
                .status(userDetails.isEnabled() ? "ACTIVE" : "INACTIVE")
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * Spring Security 권한에서 UserRoleType 추출
     */
    private UserRoleType extractRoleFromAuthorities(
            java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return UserRoleType.GUEST;
        }

        for (org.springframework.security.core.GrantedAuthority authority : authorities) {
            String authorityName = authority.getAuthority();

            // ROLE_ 프리픽스 제거
            if (authorityName.startsWith("ROLE_")) {
                authorityName = authorityName.substring(5);
            }

            // UserRoleType과 매칭
            try {
                return UserRoleType.valueOf(authorityName);
            } catch (IllegalArgumentException e) {
                log.debug("Unknown authority: {}, defaulting to USER", authorityName);
            }
        }

        return UserRoleType.USER; // 기본값
    }
}
