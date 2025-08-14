package api.v2.cms.auth.service;

import api.v2.cms.auth.provider.JwtTokenProvider;
import api.v2.cms.auth.service.impl.TokenRefreshServiceImpl;
import api.v2.cms.user.domain.User;
import io.jsonwebtoken.JwtException;
import api.v2.cms.user.domain.UserRoleType;
import api.v2.cms.user.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 토큰 갱신 서비스 단위 테스트
 * 
 * 리프레시 토큰 갱신 로직의 핵심 기능을 검증:
 * - 유효한 리프레시 토큰으로 액세스 토큰 갱신
 * - 만료된 리프레시 토큰 처리
 * - 유효하지 않은 리프레시 토큰 처리
 * - 갱신 이력 관리
 */
@ExtendWith(MockitoExtension.class)
class TokenRefreshServiceTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    private TokenRefreshService tokenRefreshService;
    private User testUser;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        tokenRefreshService = new TokenRefreshServiceImpl(jwtTokenProvider, userRepository);

        // 테스트용 사용자 생성
        testUser = User.builder()
                .uuid("test-uuid")
                .username("testuser")
                .password("password123!")
                .name("Test User")
                .email("test@example.com")
                .role(UserRoleType.USER)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validRefreshToken = "valid.refresh.token";
    }

    @AfterEach
    void tearDown() {
        tokenRefreshService = null;
        testUser = null;
        validRefreshToken = null;
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 액세스 토큰을 갱신한다")
    void 유효한_리프레시_토큰으로_액세스_토큰을_갱신한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mRefresh Access Token with Valid Refresh Token\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 유효한 리프레시 토큰과 사용자 정보 준비");
        when(jwtTokenProvider.validateRefreshToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(validRefreshToken)).thenReturn(testUser.getUsername());
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.createAccessToken(testUser)).thenReturn("new.access.token");

        System.out.println("    \033[90m→\033[0m Username: \033[36m" + testUser.getUsername() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Refresh token: \033[36m" + validRefreshToken + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 토큰 갱신 요청");
        ResponseEntity<ApiResponseSchema<Map<String, String>>> response = tokenRefreshService
                .refreshToken(validRefreshToken);
        System.out.println("    \033[90m→\033[0m refreshToken() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 갱신 응답 검증");
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ApiResponseSchema<Map<String, String>> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("토큰이 성공적으로 갱신되었습니다.", body.getMessage());

        Map<String, String> data = body.getData();
        assertNotNull(data);
        assertEquals("new.access.token", data.get("accessToken"));
        assertEquals("Bearer", data.get("tokenType"));

        verify(jwtTokenProvider).validateRefreshToken(validRefreshToken);
        verify(jwtTokenProvider).createAccessToken(testUser);

        System.out.println(
                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mNew access token:\033[0m \033[32mpresent\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mToken type:\033[0m \033[32mBearer\033[0m\n");
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 갱신 시 실패한다")
    void 만료된_리프레시_토큰으로_갱신_시_실패한다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mRefresh Token Expired Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 만료된 리프레시 토큰 준비");
        String expiredToken = "expired.refresh.token";
        when(jwtTokenProvider.validateRefreshToken(expiredToken))
                .thenThrow(new JwtException("토큰이 만료되었습니다."));

        System.out.println("    \033[90m→\033[0m Expired token: \033[31m" + expiredToken + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 만료된 토큰으로 갱신 시도");
        ResponseEntity<ApiResponseSchema<Map<String, String>>> response = tokenRefreshService
                .refreshToken(expiredToken);
        System.out.println("    \033[90m→\033[0m refreshToken() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 실패 응답 검증");
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        ApiResponseSchema<Map<String, String>> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("토큰이 만료되었습니다. 다시 로그인해주세요.", body.getMessage());
        assertEquals("TOKEN_EXPIRED", body.getErrorCode());

        verify(jwtTokenProvider, never()).createAccessToken(any(User.class));

        System.out.println(
                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError message:\033[0m \033[31m" + body.getMessage() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError code:\033[0m \033[31m" + body.getErrorCode() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mNo new token:\033[0m \033[32mconfirmed\033[0m\n");
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 토큰으로 갱신 시 실패한다")
    void 존재하지_않는_사용자의_토큰으로_갱신_시_실패한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mNon-existent User Token Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 존재하지 않는 사용자의 토큰 준비");
        when(jwtTokenProvider.validateRefreshToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(validRefreshToken)).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        System.out.println("    \033[90m→\033[0m Username: \033[31mnonexistent\033[0m");
        System.out.println("    \033[90m→\033[0m Token: \033[36m" + validRefreshToken + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 존재하지 않는 사용자의 토큰으로 갱신 시도");
        ResponseEntity<ApiResponseSchema<Map<String, String>>> response = tokenRefreshService
                .refreshToken(validRefreshToken);
        System.out.println("    \033[90m→\033[0m refreshToken() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 실패 응답 검증");
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        ApiResponseSchema<Map<String, String>> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("유효하지 않은 토큰입니다. 다시 로그인해주세요.", body.getMessage());
        assertEquals("TOKEN_INVALID", body.getErrorCode());

        verify(jwtTokenProvider, never()).createAccessToken(any(User.class));

        System.out.println(
                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError message:\033[0m \033[31m" + body.getMessage() + "\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mError code:\033[0m \033[31m" + body.getErrorCode() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mNo new token:\033[0m \033[32mconfirmed\033[0m\n");
    }
}
