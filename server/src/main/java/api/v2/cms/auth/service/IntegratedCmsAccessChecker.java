package api.v2.cms.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.cms.user.domain.User;
import api.v2.cms.user.domain.UserRoleType;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

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
            // TODO: UserDetails에서 User 객체로 변환하는 로직 구현
            log.debug("Extracting user from UserDetails: {}", userDetails.getUsername());
        }

        return null;
    }
}
