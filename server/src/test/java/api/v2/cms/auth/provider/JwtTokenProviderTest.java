package api.v2.cms.auth.provider;

import api.v2.cms.user.domain.User;
import api.v2.cms.user.domain.UserRoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Collection;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;
import static api.v2.cms.auth.provider.JwtTokenProvider.TOKEN_TYPE_CLAIM;
import static api.v2.cms.auth.provider.JwtTokenProvider.TOKEN_TYPE_REFRESH;

/**
 * JwtTokenProvider 단위 테스트
 * 
 * JWT 토큰 생성, 검증, 예외 처리 로직을 검증:
 * - 토큰 생성과 파싱
 * - 만료 토큰 처리
 * - 잘못된 형식/서명 처리
 * - 필수 클레임 검증
 * - 토큰 타입 검증
 */
class JwtTokenProviderTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();

        // 테스트용 시크릿 키와 만료 시간 설정
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", "testSecretKeyWithLength32BytesLong!");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityInMilliseconds", 3600000L); // 1시간
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidityInMilliseconds", 2592000000L); // 30일

        // 테스트용 사용자 생성
        testUser = User.builder()
                .uuid("test-uuid")
                .username("testuser")
                .password("password123!")
                .name("Test User")
                .email("test@example.com")
                .role(UserRoleType.ADMIN)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        jwtTokenProvider = null;
        testUser = null;
    }

    @Test
    @DisplayName("액세스 토큰이 올바르게 생성되고 검증된다")
    void 액세스_토큰이_올바르게_생성되고_검증된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mAccess Token Creation and Validation\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 테스트 사용자로 액세스 토큰 생성");
        System.out.println("    \033[90m→\033[0m User: \033[36m" + testUser.getUsername() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Role: \033[36m" + testUser.getRole() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 액세스 토큰 생성 및 검증");
        String accessToken = jwtTokenProvider.createAccessToken(testUser);
        System.out
                .println("    \033[90m→\033[0m Token created: \033[36m" + accessToken.substring(0, 20) + "...\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 토큰 검증 및 인증 정보 확인");
        assertTrue(jwtTokenProvider.validateToken(accessToken));
        assertTrue(jwtTokenProvider.isAccessToken(accessToken));

        Authentication auth = jwtTokenProvider.getAuthentication(accessToken);
        assertNotNull(auth);
        assertEquals(testUser.getUsername(), auth.getName());

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + testUser.getRole().name())));

        System.out.println("    \033[32m✓\033[0m \033[90mToken validated:\033[0m \033[32mvalid\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mToken type:\033[0m \033[32mACCESS\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mAuthentication:\033[0m \033[32m" + auth.getName() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mAuthorities:\033[0m \033[32m"
                + authorities.iterator().next().getAuthority() + "\033[0m\n");
    }

    @Test
    @DisplayName("리프레시 토큰이 올바르게 생성되고 검증된다")
    void 리프레시_토큰이_올바르게_생성되고_검증된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mRefresh Token Creation and Validation\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 테스트 사용자로 리프레시 토큰 생성");
        System.out.println("    \033[90m→\033[0m User: \033[36m" + testUser.getUsername() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 리프레시 토큰 생성 및 검증");
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        System.out
                .println("    \033[90m→\033[0m Token created: \033[36m" + refreshToken.substring(0, 20) + "...\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 토큰 검증 및 타입 확인");
        assertTrue(jwtTokenProvider.validateRefreshToken(refreshToken));
        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
        assertFalse(jwtTokenProvider.isAccessToken(refreshToken));

        // 리프레시 토큰의 클레임 검증
        Key key = new SecretKeySpec(jwtTokenProvider.getSecretKey().getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS256.getJcaName());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        assertEquals(testUser.getUsername(), claims.getSubject());
        assertEquals(testUser.getUuid(), claims.get("userId"));
        assertEquals(TOKEN_TYPE_REFRESH, claims.get(TOKEN_TYPE_CLAIM));
        assertNull(claims.get("role"), "리프레시 토큰은 role 클레임을 포함하지 않아야 함");

        System.out.println("    \033[32m✓\033[0m \033[90mRefresh token validated:\033[0m \033[32mvalid\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mToken type:\033[0m \033[32mREFRESH\033[0m");
        System.out
                .println("    \033[32m✓\033[0m \033[90mClaims verified:\033[0m \033[32msubject, userId, type\033[0m\n");
    }

    @Test
    @DisplayName("만료된 토큰은 JwtException을 발생시킨다")
    void 만료된_토큰은_JwtException을_발생시킨다() {
        System.out.println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mExpired Token Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 즉시 만료되는 토큰 설정");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityInMilliseconds", -3600000L); // 1시간 전 만료
        System.out.println("    \033[90m→\033[0m Validity: \033[33m-1 hour\033[0m (already expired)");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 만료된 토큰 생성 및 검증");
        String expiredToken = jwtTokenProvider.createAccessToken(testUser);
        System.out
                .println("    \033[90m→\033[0m Token created: \033[36m" + expiredToken.substring(0, 20) + "...\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 만료 예외 발생 확인");
        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtTokenProvider.validateToken(expiredToken);
        });

        assertEquals("토큰이 만료되었습니다.", exception.getMessage());
        System.out.println("    \033[32m✓\033[0m \033[90mException thrown:\033[0m \033[31m" + exception.getMessage()
                + "\033[0m\n");
    }

    @Test
    @DisplayName("잘못된 형식의 토큰은 JwtException을 발생시킨다")
    void 잘못된_형식의_토큰은_JwtException을_발생시킨다() {
        System.out.println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mMalformed Token Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 잘못된 형식의 토큰 준비");
        String malformedToken = "invalid.jwt.token";
        System.out.println("    \033[90m→\033[0m Token: \033[33m" + malformedToken + "\033[0m");

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m 잘못된 토큰 검증");
        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtTokenProvider.validateToken(malformedToken);
        });

        assertEquals("잘못된 형식의 토큰입니다.", exception.getMessage());
        System.out.println("    \033[32m✓\033[0m \033[90mException thrown:\033[0m \033[31m" + exception.getMessage()
                + "\033[0m\n");
    }

    @Test
    @DisplayName("다른 시크릿 키로 서명된 토큰은 JwtException을 발생시킨다")
    void 다른_시크릿_키로_서명된_토큰은_JwtException을_발생시킨다() {
        System.out.println("\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mInvalid Signature Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 다른 시크릿 키로 토큰 생성");
        String originalToken = jwtTokenProvider.createAccessToken(testUser);

        // 시크릿 키 변경
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", "differentSecretKeyWithLength32Bytes!");
        System.out.println(
                "    \033[90m→\033[0m Original token: \033[36m" + originalToken.substring(0, 20) + "...\033[0m");
        System.out.println("    \033[90m→\033[0m Secret key changed");

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m 변경된 키로 토큰 검증");
        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtTokenProvider.validateToken(originalToken);
        });

        assertEquals("토큰 서명이 유효하지 않습니다.", exception.getMessage());
        System.out.println("    \033[32m✓\033[0m \033[90mException thrown:\033[0m \033[31m" + exception.getMessage()
                + "\033[0m\n");
    }

    @Test
    @DisplayName("필수 클레임이 없는 토큰은 JwtException을 발생시킨다")
    void 필수_클레임이_없는_토큰은_JwtException을_발생시킨다() {
        System.out.println("\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mMissing Claims Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m userId가 없는 불완전한 사용자로 토큰 생성");
        testUser.setUuid(null); // userId 클레임이 누락되도록 설정
        System.out.println("    \033[90m→\033[0m User UUID: \033[33mnull\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 불완전한 토큰 생성 및 검증");
        String tokenWithMissingClaims = jwtTokenProvider.createAccessToken(testUser);
        System.out.println("    \033[90m→\033[0m Token created: \033[36m" + tokenWithMissingClaims.substring(0, 20)
                + "...\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 필수 클레임 누락 예외 확인");
        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtTokenProvider.validateToken(tokenWithMissingClaims);
        });

        assertEquals("토큰에 필수 정보가 없습니다.", exception.getMessage());
        System.out.println("    \033[32m✓\033[0m \033[90mException thrown:\033[0m \033[31m" + exception.getMessage()
                + "\033[0m\n");
    }

    @Test
    @DisplayName("토큰 타입이 일치하지 않으면 JwtException을 발생시킨다")
    void 토큰_타입이_일치하지_않으면_JwtException을_발생시킨다() {
        System.out.println("\n\033[1;96m🧪 TEST #7\033[0m \033[90m│\033[0m \033[1mInvalid Token Type Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 리프레시 토큰 생성");
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        System.out
                .println("    \033[90m→\033[0m Token created: \033[36m" + refreshToken.substring(0, 20) + "...\033[0m");
        System.out.println("    \033[90m→\033[0m Token type: \033[36mREFRESH\033[0m");

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m 리프레시 토큰을 액세스 토큰으로 검증");
        assertFalse(jwtTokenProvider.isAccessToken(refreshToken));

        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtTokenProvider.validateToken(refreshToken);
        });

        assertEquals("잘못된 토큰 타입입니다.", exception.getMessage());
        System.out.println("    \033[32m✓\033[0m \033[90mNot an access token:\033[0m \033[32mconfirmed\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mException thrown:\033[0m \033[31m" + exception.getMessage()
                + "\033[0m\n");
    }
}
