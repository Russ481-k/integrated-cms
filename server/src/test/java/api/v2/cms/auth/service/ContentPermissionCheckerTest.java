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
 * ContentPermissionChecker 단위 테스트
 * 
 * TDD 커서룰에 따라 작성된 TODO 구현 테스트:
 * - extractUser 메서드 UserDetails → User 변환 로직
 * - 콘텐츠별 세부 권한 검증 로직
 * - 에러 처리 시나리오 테스트
 */
@ExtendWith(MockitoExtension.class)
class ContentPermissionCheckerTest {

    // Java 8 호환 문자열 반복 유틸리티 (필수)
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private ContentPermissionChecker permissionChecker;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // 각 테스트 전 초기화
        permissionChecker = new ContentPermissionChecker();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 정리 작업
    }

    @Test
    @DisplayName("User 객체가 UserDetails로 전달되면 올바르게 추출된다")
    void User_객체가_UserDetails로_전달되면_올바르게_추출된다() {
        System.out.println("\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mUser Object Extraction from UserDetails\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m User 객체를 UserDetails로 전달");
        User testUser = createTestUser("testuser", UserRoleType.ADMIN);
        System.out.println("    \033[90m→\033[0m created test user: \033[36m'" + testUser.getUsername() + "'\033[0m");
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testUser);

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m hasPermission 호출로 extractUser 메서드 간접 테스트");
        boolean result = permissionChecker.hasPermission(authentication, "douzone", "CONTENT", "read");
        System.out.println("    \033[90m→\033[0m hasPermission(authentication, \033[36m\"douzone\"\033[0m, \033[36m\"CONTENT\"\033[0m, \033[36m\"read\"\033[0m)");
        System.out.println("    \033[90m→\033[0m result = \033[32m" + result + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m User 추출이 성공하고 권한 검증이 수행되어야 함");
        // 현재는 extractUser가 TODO이므로 null을 반환하여 실패할 것임
        // 이 테스트가 실패해야 Red 단계가 완성됨
        assertTrue(result, "User extraction should work and permission should be granted for ADMIN role");
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mUser extracted and permission granted\033[0m\n");
    }

    @Test
    @DisplayName("일반 UserDetails 객체에서 User로 변환할 수 있다")
    void 일반_UserDetails_객체에서_User로_변환할_수_있다() {
        System.out.println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mGeneric UserDetails to User Conversion\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 일반 UserDetails 구현체 생성");
        UserDetails genericUserDetails = createGenericUserDetails("genericuser", "ROLE_SITE_ADMIN");
        System.out.println("    \033[90m→\033[0m created generic UserDetails: \033[36m'" + genericUserDetails.getUsername() + "'\033[0m");
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(genericUserDetails);

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m hasPermission 호출로 UserDetails → User 변환 테스트");
        boolean result = permissionChecker.hasPermission(authentication, "douzone", "CONTENT", "write");
        System.out.println("    \033[90m→\033[0m hasPermission(authentication, \033[36m\"douzone\"\033[0m, \033[36m\"CONTENT\"\033[0m, \033[36m\"write\"\033[0m)");
        System.out.println("    \033[90m→\033[0m result = \033[32m" + result + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m UserDetails에서 User 변환이 성공해야 함");
        // 현재는 extractUser TODO 때문에 실패할 것임 (Red 단계)
        assertTrue(result, "Should be able to convert UserDetails to User and grant permission");
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mUserDetails converted successfully\033[0m\n");
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 권한이 거부된다")
    void 인증되지_않은_사용자는_권한이_거부된다() {
        System.out.println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mUnauthenticated User Access Denied\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 인증되지 않은 사용자");
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 권한 확인 시도");
        boolean result = permissionChecker.hasPermission(authentication, "douzone", "CONTENT", "read");
        System.out.println("    \033[90m→\033[0m hasPermission() with unauthenticated user");
        System.out.println("    \033[90m→\033[0m result = \033[33m" + result + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 인증되지 않은 사용자는 권한이 거부되어야 함");
        assertFalse(result, "Unauthenticated users should be denied access");
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mAccess denied for unauthenticated user\033[0m\n");
    }

    @Test
    @DisplayName("null Authentication은 권한이 거부된다")
    void null_Authentication은_권한이_거부된다() {
        System.out.println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mNull Authentication Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m null Authentication");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m null Authentication으로 권한 확인");
        boolean result = permissionChecker.hasPermission(null, "douzone", "CONTENT", "read");
        System.out.println("    \033[90m→\033[0m hasPermission(null, ...)");
        System.out.println("    \033[90m→\033[0m result = \033[33m" + result + "\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m null Authentication은 권한이 거부되어야 함");
        assertFalse(result, "Null authentication should be denied access");
        System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[33mAccess denied for null authentication\033[0m\n");
    }

    @Test
    @DisplayName("SUPER_ADMIN은 모든 콘텐츠 권한을 가진다")
    void SUPER_ADMIN은_모든_콘텐츠_권한을_가진다() {
        System.out.println("\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mSUPER_ADMIN Full Content Permissions\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m SUPER_ADMIN 사용자");
        User superAdmin = createTestUser("superadmin", UserRoleType.SUPER_ADMIN);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(superAdmin);

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m 다양한 권한 시나리오 테스트");
        
        System.out.println("  \033[2m🎯 Scenario 1:\033[0m READ 권한");
        boolean readResult = permissionChecker.hasPermission(authentication, "douzone", "CONTENT", "read");
        System.out.println("    \033[90m→\033[0m READ permission = \033[32m" + readResult + "\033[0m");
        assertTrue(readResult, "SUPER_ADMIN should have READ permission");

        System.out.println();

        System.out.println("  \033[2m🎯 Scenario 2:\033[0m WRITE 권한");
        boolean writeResult = permissionChecker.hasPermission(authentication, "douzone", "CONTENT", "write");
        System.out.println("    \033[90m→\033[0m WRITE permission = \033[32m" + writeResult + "\033[0m");
        assertTrue(writeResult, "SUPER_ADMIN should have WRITE permission");

        System.out.println();

        System.out.println("  \033[2m🎯 Scenario 3:\033[0m DELETE 권한");
        boolean deleteResult = permissionChecker.hasPermission(authentication, "douzone", "CONTENT", "delete");
        System.out.println("    \033[90m→\033[0m DELETE permission = \033[32m" + deleteResult + "\033[0m");
        assertTrue(deleteResult, "SUPER_ADMIN should have DELETE permission");

        System.out.println("    \033[32m✓\033[0m \033[90mAll scenarios passed:\033[0m \033[32mSUPER_ADMIN has full permissions\033[0m\n");
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
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(authority)
                )
        );
    }
}
