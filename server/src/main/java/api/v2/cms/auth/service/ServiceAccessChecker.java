package api.v2.cms.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.cms.config.ServiceContextHolder;
import api.v2.cms.user.domain.User;
import api.v2.cms.user.domain.UserRoleType;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 서비스별 접근 권한을 검증하는 컴포넌트
 * 
 * 하이브리드 보안 시스템의 핵심 비즈니스 로직
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Component("serviceAccessChecker")
@RequiredArgsConstructor
public class ServiceAccessChecker {

    /**
     * 서비스별 접근 권한 확인
     * 
     * @param authentication 현재 인증 정보
     * @param serviceId      접근하려는 서비스 ID
     * @return 접근 허용 여부
     */
    public boolean hasAccess(Authentication authentication, String serviceId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Authentication is null or not authenticated for service: {}", serviceId);
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            log.warn("Principal is not UserDetails for service: {}", serviceId);
            return false;
        }

        User user = extractUser(principal);
        if (user == null) {
            log.warn("Failed to extract user from principal for service: {}", serviceId);
            return false;
        }

        // 현재 서비스 컨텍스트 확인
        String currentServiceId = ServiceContextHolder.getCurrentServiceId();
        if (currentServiceId != null && !currentServiceId.equals(serviceId)) {
            log.warn("Service context mismatch. Current: {}, Requested: {}", currentServiceId, serviceId);
        }

        return checkServiceAccess(user, serviceId);
    }

    /**
     * 서비스 관리자 권한 확인
     * 
     * @param authentication 현재 인증 정보
     * @param serviceId      접근하려는 서비스 ID
     * @return 서비스 관리자 권한 여부
     */
    public boolean isServiceAdmin(Authentication authentication, String serviceId) {
        if (!hasAccess(authentication, serviceId)) {
            return false;
        }

        User user = extractUser(authentication.getPrincipal());
        if (user == null) {
            return false;
        }

        // SUPER_ADMIN은 모든 서비스의 관리자
        if (user.getRole() == UserRoleType.SUPER_ADMIN) {
            log.debug("SUPER_ADMIN access granted for service: {}", serviceId);
            return true;
        }

        // SERVICE_ADMIN은 할당된 서비스만 관리 가능
        if (user.getRole() == UserRoleType.SERVICE_ADMIN) {
            // TODO: 실제 서비스-관리자 매핑 테이블에서 확인
            log.debug("SERVICE_ADMIN access check for service: {} (TODO: implement service mapping)", serviceId);
            return true; // 임시로 허용
        }

        return false;
    }

    /**
     * 실제 서비스 접근 권한 확인 로직
     */
    private boolean checkServiceAccess(User user, String serviceId) {
        UserRoleType role = user.getRole();

        // SUPER_ADMIN은 모든 서비스 접근 가능
        if (role == UserRoleType.SUPER_ADMIN) {
            log.debug("SUPER_ADMIN access granted for service: {}", serviceId);
            return true;
        }

        // integrated_cms는 SUPER_ADMIN, SERVICE_ADMIN만 접근 가능
        if ("integrated_cms".equals(serviceId)) {
            boolean hasAccess = role == UserRoleType.SERVICE_ADMIN;
            log.debug("Integrated CMS access for role {}: {}", role, hasAccess);
            return hasAccess;
        }

        // 일반 서비스는 SERVICE_ADMIN, SITE_ADMIN, ADMIN 접근 가능
        boolean hasAccess = role == UserRoleType.SERVICE_ADMIN ||
                role == UserRoleType.SITE_ADMIN ||
                role == UserRoleType.ADMIN;

        log.debug("Service {} access for role {}: {}", serviceId, role, hasAccess);

        // TODO: 실제 사용자-서비스 매핑 테이블에서 세부 권한 확인
        // - 사용자가 해당 서비스에 할당되어 있는지
        // - 사용자의 서비스별 권한 레벨은 무엇인지

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
