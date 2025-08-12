package api.v2.cms.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.cms.user.domain.User;
import api.v2.cms.user.domain.UserRoleType;
import api.v2.cms.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 하이브리드 인증 시스템 테스트를 위한 초기 사용자 생성 서비스
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridAuthTestService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 애플리케이션 시작 시 테스트용 사용자들 생성
     */
    @PostConstruct
    @Transactional
    public void createTestUsers() {
        log.info("Creating test users for hybrid authentication system");

        try {
            createUserIfNotExists("superadmin", "super123!", "슈퍼관리자", "superadmin@test.com", UserRoleType.SUPER_ADMIN);
            createUserIfNotExists("serviceadmin", "service123!", "서비스관리자", "serviceadmin@test.com",
                    UserRoleType.SERVICE_ADMIN);
            createUserIfNotExists("siteadmin", "site123!", "사이트관리자", "siteadmin@test.com", UserRoleType.SITE_ADMIN);
            createUserIfNotExists("admin", "admin123!", "일반관리자", "admin@test.com", UserRoleType.ADMIN);
            createUserIfNotExists("user", "user123!", "일반사용자", "user@test.com", UserRoleType.USER);

            log.info("Test users created successfully for hybrid authentication testing");
        } catch (Exception e) {
            log.error("Failed to create test users", e);
        }
    }

    private void createUserIfNotExists(String username, String password, String name, String email, UserRoleType role) {
        if (userRepository.existsByUsername(username)) {
            log.debug("User {} already exists, skipping creation", username);
            return;
        }

        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username(username)
                .password(passwordEncoder.encode(password))
                .name(name)
                .email(email)
                .role(role)
                .status("ACTIVE")
                .isTemporary(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("Created test user: {} with role: {}", username, role);
    }

    /**
     * 테스트용 사용자 계정 정보 반환
     */
    public String getTestUserInfo() {
        return "하이브리드 인증 시스템 테스트 계정:\n\n" +
                "1. SUPER_ADMIN: superadmin / super123!\n" +
                "   - 모든 서비스 접근 가능\n" +
                "   - 통합 CMS 모든 기능 사용 가능\n" +
                "   - 서비스 생성/삭제, 관리자 관리 가능\n\n" +
                "2. SERVICE_ADMIN: serviceadmin / service123!\n" +
                "   - 할당된 서비스 관리 가능\n" +
                "   - 통합 CMS 일부 기능 사용 가능\n" +
                "   - 권한 조회 가능\n\n" +
                "3. SITE_ADMIN: siteadmin / site123!\n" +
                "   - 사이트별 관리 권한\n" +
                "   - 콘텐츠 생성/수정/삭제 가능\n" +
                "   - 서비스별 CMS 접근 가능\n\n" +
                "4. ADMIN: admin / admin123!\n" +
                "   - 일반 관리자 권한\n" +
                "   - 콘텐츠 조회/생성/수정 가능 (삭제 불가)\n" +
                "   - 서비스별 CMS 접근 가능\n\n" +
                "5. USER: user / user123!\n" +
                "   - 일반 사용자 권한\n" +
                "   - 제한적 접근";
    }
}
