package api.v2.cms.auth.service;

import api.v2.cms.auth.dto.LoginRequest;
import api.v2.cms.auth.provider.JwtTokenProvider;
import api.v2.cms.auth.service.impl.AdminAuthServiceImpl;
import api.v2.cms.user.domain.AdminUser;
import api.v2.cms.user.domain.UserRoleType;
import api.v2.cms.user.repository.AdminUserRepository;
import api.v2.common.dto.ApiResponseSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AdminAuthService 단위 테스트
 * 
 * 관리자 인증 서비스의 핵심 기능을 검증:
 * - 로그인 플로우
 * - 비밀번호 검증
 * - 계정 상태 확인
 * - 토큰 발급
 */
@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminAuthService adminAuthService;
    private AdminUser testAdminUser;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        adminAuthService = new AdminAuthServiceImpl(adminUserRepository, jwtTokenProvider, passwordEncoder);

        // 테스트용 관리자 계정 생성
        testAdminUser = AdminUser.builder()
                .uuid("admin-uuid")
                .username("admin")
                .password("encoded_password123!")
                .name("Test Admin")
                .email("admin@example.com")
                .role(UserRoleType.SUPER_ADMIN)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 유효한 로그인 요청 생성
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("admin");
        validLoginRequest.setPassword("password123!");
    }

    @AfterEach
    void tearDown() {
        adminAuthService = null;
        testAdminUser = null;
        validLoginRequest = null;
    }

    @Test
    @DisplayName("유효한 관리자 계정으로 로그인에 성공한다")
    void 유효한_관리자_계정으로_로그인에_성공한다() {
        System.out.println("\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mValid Admin Login Success\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 유효한 관리자 계정과 로그인 요청 준비");
        when(adminUserRepository.findByUsername(validLoginRequest.getUsername()))
                .thenReturn(Optional.of(testAdminUser));
        when(passwordEncoder.matches(validLoginRequest.getPassword(), testAdminUser.getPassword()))
                .thenReturn(true);
        when(jwtTokenProvider.createAccessToken(testAdminUser))
                .thenReturn("test.access.token");
        when(jwtTokenProvider.createRefreshToken(testAdminUser))
                .thenReturn("test.refresh.token");

        System.out.println("    \033[90m→\033[0m Username: \033[36m" + validLoginRequest.getUsername() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Role: \033[36m" + testAdminUser.getRole() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Status: \033[36m" + testAdminUser.getStatus() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 로그인 요청 실행");
        ResponseEntity<ApiResponseSchema<Map<String, Object>>> response = adminAuthService.loginUser(validLoginRequest);
        System.out.println("    \033[90m→\033[0m loginUser() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 로그인 응답 검증");
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ApiResponseSchema<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("로그인이 성공적으로 완료되었습니다.", body.getMessage());

        Map<String, Object> data = body.getData();
        assertNotNull(data);
        assertEquals("test.access.token", data.get("accessToken"));
        assertEquals("test.refresh.token", data.get("refreshToken"));
        assertEquals("Bearer", data.get("tokenType"));

        @SuppressWarnings("unchecked")
        Map<String, Object> userData = (Map<String, Object>) data.get("user");
        assertNotNull(userData);
        assertEquals(testAdminUser.getUuid(), userData.get("uuid"));
        assertEquals(testAdminUser.getUsername(), userData.get("username"));
        assertEquals(testAdminUser.getRole().name(), userData.get("role"));

        verify(adminUserRepository).save(any(AdminUser.class)); // 마지막 로그인 시간 업데이트 확인

        System.out.println(
                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mAccess token:\033[0m \033[32mpresent\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mRefresh token:\033[0m \033[32mpresent\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mUser data:\033[0m \033[32mvalidated\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mLogin time:\033[0m \033[32mupdated\033[0m\n");
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인하면 실패한다")
    void 잘못된_비밀번호로_로그인하면_실패한다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mInvalid Password Login Failure\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 잘못된 비밀번호로 로그인 시도");
        when(adminUserRepository.findByUsername(validLoginRequest.getUsername()))
                .thenReturn(Optional.of(testAdminUser));
        when(passwordEncoder.matches(validLoginRequest.getPassword(), testAdminUser.getPassword()))
                .thenReturn(false); // 비밀번호 불일치

        System.out.println("    \033[90m→\033[0m Username: \033[36m" + validLoginRequest.getUsername() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Password: \033[31minvalid\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 잘못된 비밀번호로 로그인");
        ResponseEntity<ApiResponseSchema<Map<String, Object>>> response = adminAuthService.loginUser(validLoginRequest);
        System.out.println("    \033[90m→\033[0m loginUser() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 로그인 실패 응답 검증");
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        ApiResponseSchema<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("비밀번호가 일치하지 않습니다.", body.getMessage());
        assertEquals("AUTH_INVALID_PASSWORD", body.getErrorCode());

        verify(jwtTokenProvider, never()).createAccessToken(any(AdminUser.class)); // 토큰 생성되지 않음
        verify(adminUserRepository, never()).save(any()); // 로그인 시간 업데이트 안됨

        System.out.println(
                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError message:\033[0m \033[31m" + body.getMessage() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError code:\033[0m \033[31m" + body.getErrorCode() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mNo tokens generated:\033[0m \033[32mconfirmed\033[0m\n");
    }

    @Test
    @DisplayName("비활성화된 관리자 계정으로 로그인하면 실패한다")
    void 비활성화된_관리자_계정으로_로그인하면_실패한다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mInactive Account Login Failure\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 비활성화된 관리자 계정으로 로그인 시도");
        testAdminUser.setStatus("INACTIVE"); // 계정 비활성화

        when(adminUserRepository.findByUsername(validLoginRequest.getUsername()))
                .thenReturn(Optional.of(testAdminUser));
        when(passwordEncoder.matches(validLoginRequest.getPassword(), testAdminUser.getPassword()))
                .thenReturn(true);

        System.out.println("    \033[90m→\033[0m Username: \033[36m" + validLoginRequest.getUsername() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Status: \033[31m" + testAdminUser.getStatus() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 비활성화된 계정으로 로그인");
        ResponseEntity<ApiResponseSchema<Map<String, Object>>> response = adminAuthService.loginUser(validLoginRequest);
        System.out.println("    \033[90m→\033[0m loginUser() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 로그인 실패 응답 검증");
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        ApiResponseSchema<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("비활성화된 관리자 계정입니다.", body.getMessage());
        assertEquals("AUTH_ACCOUNT_DISABLED", body.getErrorCode());

        verify(jwtTokenProvider, never()).createAccessToken(any(AdminUser.class)); // 토큰 생성되지 않음
        verify(adminUserRepository, never()).save(any()); // 로그인 시간 업데이트 안됨

        System.out.println(
                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError message:\033[0m \033[31m" + body.getMessage() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError code:\033[0m \033[31m" + body.getErrorCode() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mNo tokens generated:\033[0m \033[32mconfirmed\033[0m\n");
    }

    @Test
    @DisplayName("존재하지 않는 관리자 계정으로 로그인하면 실패한다")
    void 존재하지_않는_관리자_계정으로_로그인하면_실패한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mNon-existent Account Login Failure\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 존재하지 않는 계정으로 로그인 시도");
        when(adminUserRepository.findByUsername(validLoginRequest.getUsername()))
                .thenReturn(Optional.empty());

        System.out.println("    \033[90m→\033[0m Username: \033[36m" + validLoginRequest.getUsername() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Account: \033[31mnot found\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 존재하지 않는 계정으로 로그인");
        ResponseEntity<ApiResponseSchema<Map<String, Object>>> response = adminAuthService.loginUser(validLoginRequest);
        System.out.println("    \033[90m→\033[0m loginUser() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 로그인 실패 응답 검증");
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        ApiResponseSchema<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("아이디 또는 비밀번호가 일치하지 않습니다.", body.getMessage());
        assertEquals("AUTH_INVALID_CREDENTIALS", body.getErrorCode());

        verify(passwordEncoder, never()).matches(anyString(), anyString()); // 비밀번호 검증 안됨
        verify(jwtTokenProvider, never()).createAccessToken(any(AdminUser.class)); // 토큰 생성되지 않음
        verify(adminUserRepository, never()).save(any()); // 로그인 시간 업데이트 안됨

        System.out.println(
                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError message:\033[0m \033[31m" + body.getMessage() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError code:\033[0m \033[31m" + body.getErrorCode() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mNo password check:\033[0m \033[32mconfirmed\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mNo tokens generated:\033[0m \033[32mconfirmed\033[0m\n");
    }
}
