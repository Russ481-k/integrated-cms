package api.v2.cms.user.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import api.v2.common.user.domain.User;
import api.v2.common.user.domain.UserRoleType;
import api.v2.common.user.dto.CustomUserDetails;
import api.v2.common.user.repository.UserRepository;
import api.v2.common.auth.service.CustomUserDetailsService;
import api.v2.common.auth.service.impl.CustomUserDetailsServiceImpl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CustomUserDetailsService 단위 테스트
 * 
 * Spring Security 사용자 정보 로딩 서비스의 핵심 기능을 검증:
 * - 사용자 정보 로딩
 * - 권한 변환
 * - 예외 처리
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

        // Java 8 호환 문자열 반복 유틸리티
        private String repeat(String str, int count) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count; i++) {
                        sb.append(str);
                }
                return sb.toString();
        }

        @Mock
        private UserRepository userRepository;

        private CustomUserDetailsService userDetailsService;
        private User testUser;

        @BeforeEach
        void setUp() {
                userDetailsService = new CustomUserDetailsServiceImpl(userRepository);

                // 테스트용 사용자 생성
                testUser = User.builder()
                                .uuid("user-uuid")
                                .username("user")
                                .password("encoded_password123!")
                                .name("Test User")
                                .email("user@example.com")
                                .role(UserRoleType.USER)
                                .status("ACTIVE")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
        }

        @AfterEach
        void tearDown() {
                userDetailsService = null;
                testUser = null;
        }

        @Test
        @DisplayName("존재하는 사용자명으로 UserDetails를 로드한다")
        void 존재하는_사용자명으로_UserDetails를_로드한다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mLoad UserDetails for Existing User\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 존재하는 사용자 준비");
                when(userRepository.findByUsername(testUser.getUsername()))
                                .thenReturn(Optional.of(testUser));

                System.out.println("    \033[90m→\033[0m Username: \033[36m" + testUser.getUsername() + "\033[0m");
                System.out.println("    \033[90m→\033[0m Role: \033[36m" + testUser.getRole() + "\033[0m");

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m UserDetails 로드");
                UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getUsername());
                System.out.println("    \033[90m→\033[0m loadUserByUsername() 호출");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m UserDetails 검증");
                assertNotNull(userDetails);
                assertTrue(userDetails instanceof CustomUserDetails);
                assertEquals(testUser.getUsername(), userDetails.getUsername());
                assertEquals(testUser.getPassword(), userDetails.getPassword());
                assertTrue(userDetails.isEnabled());

                Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
                assertNotNull(authorities);
                assertEquals(1, authorities.size());
                assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_" + testUser.getRole().name())));

                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                assertEquals(testUser.getUuid(), customUserDetails.getUuid());
                assertEquals(testUser.getName(), customUserDetails.getName());
                assertEquals(testUser.getEmail(), customUserDetails.getEmail());
                assertEquals(testUser.getRole(), customUserDetails.getRole());

                System.out.println(
                                "    \033[32m✓\033[0m \033[90mUsername:\033[0m \033[32m" + userDetails.getUsername()
                                                + "\033[0m");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAuthority:\033[0m \033[32mROLE_"
                                                + testUser.getRole().name() + "\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mEnabled:\033[0m \033[32mtrue\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mCustom fields:\033[0m \033[32mvalidated\033[0m\n");
        }

        @Test
        @DisplayName("존재하지 않는 사용자명으로 조회하면 예외가 발생한다")
        void 존재하지_않는_사용자명으로_조회하면_예외가_발생한다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mThrow Exception for Non-existent User\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 존재하지 않는 사용자명 준비");
                String nonExistentUsername = "nonexistent";
                when(userRepository.findByUsername(nonExistentUsername))
                                .thenReturn(Optional.empty());

                System.out.println("    \033[90m→\033[0m Username: \033[31m" + nonExistentUsername + "\033[0m");

                // When & Then
                System.out.println("  \033[2m⚡ Action:\033[0m 존재하지 않는 사용자 조회");
                UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
                        userDetailsService.loadUserByUsername(nonExistentUsername);
                });
                System.out.println("    \033[90m→\033[0m loadUserByUsername() 호출");

                assertEquals("사용자를 찾을 수 없습니다: " + nonExistentUsername, exception.getMessage());
                verify(userRepository).findByUsername(nonExistentUsername);

                System.out.println("  \033[2m✨ Verify:\033[0m 예외 발생 확인");
                System.out.println("    \033[32m✓\033[0m \033[90mException thrown:\033[0m \033[31m"
                                + exception.getClass().getSimpleName() + "\033[0m");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mError message:\033[0m \033[31m" + exception.getMessage()
                                                + "\033[0m\n");
        }

        @Test
        @DisplayName("비활성화된 사용자는 enabled가 false이다")
        void 비활성화된_사용자는_enabled가_false이다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mInactive User Has Enabled=false\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 비활성화된 사용자 준비");
                testUser.setStatus("INACTIVE");
                when(userRepository.findByUsername(testUser.getUsername()))
                                .thenReturn(Optional.of(testUser));

                System.out.println("    \033[90m→\033[0m Username: \033[36m" + testUser.getUsername() + "\033[0m");
                System.out.println("    \033[90m→\033[0m Status: \033[31m" + testUser.getStatus() + "\033[0m");

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 비활성화된 사용자 로드");
                UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getUsername());
                System.out.println("    \033[90m→\033[0m loadUserByUsername() 호출");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 비활성화 상태 검증");
                assertNotNull(userDetails);
                assertFalse(userDetails.isEnabled());

                Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
                assertNotNull(authorities);
                assertEquals(1, authorities.size());
                assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_" + testUser.getRole().name())));

                System.out.println(
                                "    \033[32m✓\033[0m \033[90mUsername:\033[0m \033[32m" + userDetails.getUsername()
                                                + "\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mEnabled:\033[0m \033[31mfalse\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mAuthority:\033[0m \033[32mROLE_"
                                + testUser.getRole().name()
                                + "\033[0m\n");
        }
}
