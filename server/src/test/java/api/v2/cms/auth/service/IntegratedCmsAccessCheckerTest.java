package api.v2.cms.auth.service;

import api.v2.cms.user.domain.User;
import api.v2.cms.user.domain.UserRoleType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * IntegratedCmsAccessChecker 단위 테스트
 * 
 * TDD 커서룰에 따라 작성된 TODO 구현 테스트:
 * - extractUser 메서드 UserDetails → User 변환 로직
 * - 통합 CMS 접근 권한 검증 로직
 * - 시스템 관리자 권한 검증 로직
 */
@ExtendWith(MockitoExtension.class)
class IntegratedCmsAccessCheckerTest {

    // Java 8 호환 문자열 반복 유틸리티 (필수)
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private IntegratedCmsAccessChecker integratedCmsAccessChecker;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // 각 테스트 전 초기화
        integratedCmsAccessChecker = new IntegratedCmsAccessChecker();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 정리 작업
    }

    @Test
    @DisplayName("User 객체가 UserDetails로 전달되면 올바르게 추출된다")
    void User_객체가_UserDetails로_전달되면_올바르게_추출된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mUser Object Extraction from UserDetails\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m User 객체를 UserDetails로 전달");
        User testUser = createTestUser("testuser", UserRoleType.SUPER_ADMIN);
        System.out.println("    \033[90m→\033[0m created test user: \033[36m'" + testUser.getUsername() + "'\033[0m");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testUser);

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m hasAccess 호출로 extractUser 메서드 간접 테스트");
        boolean result = integratedCmsAccessChecker.hasAccess(authentication);
        System.out.println("    \033[90m→\033[0m hasAccess(authentication)");
        System.out.println("    \033[90m→\033[0m result = \033[32m" + result + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m User 추출이 성공하고 권한 검증이 수행되어야 함");
        assertTrue(result, "User extraction should work and access should be granted for SUPER_ADMIN role");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mUser extracted and access granted\033[0m\n");
    }

    @Test
    @DisplayName("일반 UserDetails 객체에서 User로 변환할 수 있다")
    void 일반_UserDetails_객체에서_User로_변환할_수_있다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mGeneric UserDetails to User Conversion\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 일반 UserDetails 구현체 생성");
        UserDetails genericUserDetails = createGenericUserDetails("genericuser", "ROLE_SERVICE_ADMIN");
        System.out.println("    \033[90m→\033[0m created generic UserDetails: \033[36m'"
                + genericUserDetails.getUsername() + "'\033[0m");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(genericUserDetails);

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m hasAccess 호출로 UserDetails → User 변환 테스트");
        boolean result = integratedCmsAccessChecker.hasAccess(authentication);
        System.out.println("    \033[90m→\033[0m hasAccess(authentication)");
        System.out.println("    \033[90m→\033[0m result = \033[32m" + result + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m UserDetails에서 User 변환이 성공해야 함");
        // 현재는 extractUser TODO 때문에 실패할 것임 (Red 단계)
        assertTrue(result, "Should be able to convert UserDetails to User and grant access");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mUserDetails converted successfully\033[0m\n");
    }

    @Test
    @DisplayName("SUPER_ADMIN은 시스템 관리자 권한을 가진다")
    void SUPER_ADMIN은_시스템_관리자_권한을_가진다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mSUPER_ADMIN System Admin Rights\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m SUPER_ADMIN 사용자");
        User superAdmin = createTestUser("superadmin", UserRoleType.SUPER_ADMIN);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(superAdmin);

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 시스템 관리자 권한 확인");
        boolean isSystemAdmin = integratedCmsAccessChecker.isSystemAdmin(authentication);
        System.out.println("    \033[90m→\033[0m isSystemAdmin(authentication)");
        System.out.println("    \033[90m→\033[0m result = \033[32m" + isSystemAdmin + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m SUPER_ADMIN은 시스템 관리자 권한을 가져야 함");
        assertTrue(isSystemAdmin, "SUPER_ADMIN should have system admin rights");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mSUPER_ADMIN has system admin rights\033[0m\n");
    }

    @Test
    @DisplayName("SERVICE_ADMIN은 통합 CMS에 접근할 수 있다")
    void SERVICE_ADMIN은_통합_CMS에_접근할_수_있다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mSERVICE_ADMIN Integrated CMS Access\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m SERVICE_ADMIN 사용자");
        User serviceAdmin = createTestUser("serviceadmin", UserRoleType.SERVICE_ADMIN);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(serviceAdmin);

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 통합 CMS 접근 권한 확인");
        boolean hasAccess = integratedCmsAccessChecker.hasAccess(authentication);
        System.out.println("    \033[90m→\033[0m hasAccess(authentication)");
        System.out.println("    \033[90m→\033[0m result = \033[32m" + hasAccess + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m SERVICE_ADMIN은 통합 CMS에 접근할 수 있어야 함");
        assertTrue(hasAccess, "SERVICE_ADMIN should have access to integrated CMS");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mSERVICE_ADMIN has integrated CMS access\033[0m\n");
    }

    @Test
    @DisplayName("SITE_ADMIN은 통합 CMS 접근이 거부된다")
    void SITE_ADMIN은_통합_CMS_접근이_거부된다() {
        System.out.println("\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mSITE_ADMIN Access Denied\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m SITE_ADMIN 사용자");
        User siteAdmin = createTestUser("siteadmin", UserRoleType.SITE_ADMIN);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(siteAdmin);

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 통합 CMS 접근 시도");
        boolean hasAccess = integratedCmsAccessChecker.hasAccess(authentication);
        System.out.println("    \033[90m→\033[0m hasAccess(authentication)");
        System.out.println("    \033[90m→\033[0m result = \033[33m" + hasAccess + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m SITE_ADMIN은 통합 CMS 접근이 거부되어야 함");
        assertFalse(hasAccess, "SITE_ADMIN should be denied access to integrated CMS");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mAccess denied for SITE_ADMIN\033[0m\n");
    }

    @Test
    @DisplayName("서비스 관리 권한은 SUPER_ADMIN만 가진다")
    void 서비스_관리_권한은_SUPER_ADMIN만_가진다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mService Management Rights Restriction\033[0m");

        // When & Then
        System.out.println("  \033[2m🎯 Scenario 1:\033[0m SUPER_ADMIN 사용자");
        User superAdmin = createTestUser("superadmin", UserRoleType.SUPER_ADMIN);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(superAdmin);

        boolean superCanManage = integratedCmsAccessChecker.canManageServices(authentication);
        System.out
                .println("    \033[90m→\033[0m SUPER_ADMIN canManageServices = \033[32m" + superCanManage + "\033[0m");
        assertTrue(superCanManage, "SUPER_ADMIN should be able to manage services");

        System.out.println();

        System.out.println("  \033[2m🎯 Scenario 2:\033[0m SERVICE_ADMIN 사용자");
        User serviceAdmin = createTestUser("serviceadmin", UserRoleType.SERVICE_ADMIN);
        when(authentication.getPrincipal()).thenReturn(serviceAdmin);

        boolean serviceCanManage = integratedCmsAccessChecker.canManageServices(authentication);
        System.out.println(
                "    \033[90m→\033[0m SERVICE_ADMIN canManageServices = \033[33m" + serviceCanManage + "\033[0m");
        assertFalse(serviceCanManage, "SERVICE_ADMIN should not be able to manage services");

        System.out.println(
                "    \033[32m✓\033[0m \033[90mAll scenarios passed:\033[0m \033[32mService management restricted to SUPER_ADMIN\033[0m\n");
    }

    // Helper methods for test data creation
    private User createTestUser(String username, UserRoleType role) {
        return User.builder()
                .uuid("test-uuid-" + username)
                .username(username)
                .password("password123")
                .name("Test User")
                .email(username + "@test.com")
                .role(role)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserDetails createGenericUserDetails(String username, String authority) {
        return new org.springframework.security.core.userdetails.User(
                username,
                "password123",
                java.util.Collections.singletonList(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(authority)));
    }
}
