package api.v2.cms.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.cms.user.domain.User;
import api.v2.cms.user.domain.UserRoleType;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 콘텐츠별 세부 권한을 검증하는 컴포넌트
 * 
 * 하이브리드 보안 시스템의 핵심 비즈니스 로직
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Component("contentPermissionChecker")
@RequiredArgsConstructor
public class ContentPermissionChecker {

    /**
     * 콘텐츠별 세부 권한 확인
     * 
     * @param authentication 현재 인증 정보
     * @param serviceId      서비스 ID
     * @param contentType    콘텐츠 타입 (MENU, CONTENT, BOARD, USER 등)
     * @param action         액션 (READ, write, delete)
     * @return 권한 허용 여부
     */
    public boolean hasPermission(Authentication authentication, String serviceId,
            String contentType, String action) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Authentication is null or not authenticated for content permission check");
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            log.warn("Principal is not UserDetails for content permission check");
            return false;
        }

        User user = extractUser(principal);
        if (user == null) {
            log.warn("Failed to extract user from principal for content permission check");
            return false;
        }

        return checkContentPermission(user, serviceId, contentType, action);
    }

    /**
     * 콘텐츠 삭제 권한 확인 (보통 관리자급만)
     * 
     * @param authentication 현재 인증 정보
     * @param serviceId      서비스 ID
     * @param contentId      콘텐츠 ID
     * @return 삭제 권한 여부
     */
    public boolean canDelete(Authentication authentication, String serviceId, Long contentId) {
        if (!hasPermission(authentication, serviceId, "GENERAL", "delete")) {
            return false;
        }

        User user = extractUser(authentication.getPrincipal());
        if (user == null) {
            return false;
        }

        // SUPER_ADMIN, SERVICE_ADMIN은 모든 콘텐츠 삭제 가능
        if (user.getRole() == UserRoleType.SUPER_ADMIN ||
                user.getRole() == UserRoleType.SERVICE_ADMIN) {
            log.debug("Admin level delete access granted for content: {}", contentId);
            return true;
        }

        // TODO: 작성자 본인인지 확인하는 로직 구현
        // - 콘텐츠의 작성자가 현재 사용자인지 확인
        // - SITE_ADMIN의 경우 본인이 관리하는 사이트의 콘텐츠만 삭제 가능

        log.debug("Content delete permission check for user {} and content {}",
                user.getUsername(), contentId);

        return false; // 임시로 거부
    }

    /**
     * 콘텐츠 수정 권한 확인
     * 
     * @param authentication 현재 인증 정보
     * @param serviceId      서비스 ID
     * @param contentId      콘텐츠 ID
     * @return 수정 권한 여부
     */
    public boolean canEdit(Authentication authentication, String serviceId, Long contentId) {
        if (!hasPermission(authentication, serviceId, "GENERAL", "write")) {
            return false;
        }

        User user = extractUser(authentication.getPrincipal());
        if (user == null) {
            return false;
        }

        // TODO: 작성자 또는 관리자 권한 확인
        log.debug("Content edit permission check for user {} and content {}",
                user.getUsername(), contentId);

        return true; // 임시로 허용
    }

    /**
     * 실제 콘텐츠 권한 확인 로직
     */
    private boolean checkContentPermission(User user, String serviceId,
            String contentType, String action) {
        UserRoleType role = user.getRole();

        // SUPER_ADMIN은 모든 권한
        if (role == UserRoleType.SUPER_ADMIN) {
            log.debug("SUPER_ADMIN content permission granted for {}/{}/{}",
                    serviceId, contentType, action);
            return true;
        }

        // SERVICE_ADMIN은 할당된 서비스의 모든 권한
        if (role == UserRoleType.SERVICE_ADMIN) {
            // TODO: 사용자가 해당 서비스의 관리자인지 확인
            log.debug("SERVICE_ADMIN content permission for {}/{}/{}",
                    serviceId, contentType, action);
            return true; // 임시로 허용
        }

        // 액션별 권한 확인
        switch (action.toLowerCase()) {
            case "read":
                return checkReadPermission(user, serviceId, contentType);
            case "write":
                return checkWritePermission(user, serviceId, contentType);
            case "delete":
                return checkDeletePermission(user, serviceId, contentType);
            default:
                log.warn("Unknown action: {}", action);
                return false;
        }
    }

    private boolean checkReadPermission(User user, String serviceId, String contentType) {
        // 모든 인증된 사용자는 읽기 가능 (기본값)
        UserRoleType role = user.getRole();
        boolean canRead = role == UserRoleType.SITE_ADMIN || role == UserRoleType.ADMIN;

        log.debug("Read permission for user {} on {}/{}: {}",
                user.getUsername(), serviceId, contentType, canRead);

        return canRead;
    }

    private boolean checkWritePermission(User user, String serviceId, String contentType) {
        // SITE_ADMIN, ADMIN 이상만 쓰기 가능
        UserRoleType role = user.getRole();
        boolean canWrite = role == UserRoleType.SITE_ADMIN || role == UserRoleType.ADMIN;

        log.debug("Write permission for user {} on {}/{}: {}",
                user.getUsername(), serviceId, contentType, canWrite);

        return canWrite;
    }

    private boolean checkDeletePermission(User user, String serviceId, String contentType) {
        // 삭제는 더 엄격한 권한 필요
        UserRoleType role = user.getRole();
        boolean canDelete = role == UserRoleType.SITE_ADMIN; // ADMIN은 삭제 불가

        log.debug("Delete permission for user {} on {}/{}: {}",
                user.getUsername(), serviceId, contentType, canDelete);

        return canDelete;
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
    private UserRoleType extractRoleFromAuthorities(java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities) {
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
