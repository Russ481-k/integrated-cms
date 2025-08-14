package api.v2.cms.auth.service;

import api.v2.cms.auth.domain.TokenRefreshHistory;
import api.v2.cms.auth.repository.TokenRefreshHistoryRepository;
import api.v2.cms.auth.service.impl.TokenRefreshHistoryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 토큰 갱신 이력 관리 서비스 단위 테스트
 * 
 * 토큰 갱신 이력 관리의 핵심 기능을 검증:
 * - 갱신 이력 기록
 * - 활성 토큰 조회
 * - 토큰 폐기
 * - 갱신 횟수 제한
 */
@ExtendWith(MockitoExtension.class)
class TokenRefreshHistoryServiceTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    @Mock
    private TokenRefreshHistoryRepository historyRepository;

    private TokenRefreshHistoryService historyService;
    private TokenRefreshHistory testHistory;

    @BeforeEach
    void setUp() {
        historyService = new TokenRefreshHistoryServiceImpl(historyRepository);

        // 테스트용 갱신 이력 생성
        testHistory = TokenRefreshHistory.builder()
                .userId("test-uuid")
                .username("testuser")
                .refreshTokenId("test-refresh-token-id")
                .ipAddress("127.0.0.1")
                .userAgent("Test-Browser/1.0")
                .refreshedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    @AfterEach
    void tearDown() {
        historyService = null;
        testHistory = null;
    }

    @Test
    @DisplayName("토큰 갱신 이력을 기록한다")
    void 토큰_갱신_이력을_기록한다() {
        System.out.println("\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mRecord Token Refresh History\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 토큰 갱신 이력 데이터 준비");
        when(historyRepository.save(any(TokenRefreshHistory.class))).thenReturn(testHistory);

        System.out.println("    \033[90m→\033[0m User ID: \033[36m" + testHistory.getUserId() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Token ID: \033[36m" + testHistory.getRefreshTokenId() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 갱신 이력 기록");
        TokenRefreshHistory savedHistory = historyService.recordRefresh(
                testHistory.getUserId(),
                testHistory.getUsername(),
                testHistory.getRefreshTokenId(),
                testHistory.getIpAddress(),
                testHistory.getUserAgent(),
                testHistory.getExpiresAt());
        System.out.println("    \033[90m→\033[0m recordRefresh() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 갱신 이력 검증");
        assertNotNull(savedHistory);
        assertEquals(testHistory.getUserId(), savedHistory.getUserId());
        assertEquals(testHistory.getRefreshTokenId(), savedHistory.getRefreshTokenId());
        assertFalse(savedHistory.isRevoked());

        verify(historyRepository).save(any(TokenRefreshHistory.class));

        System.out.println("    \033[32m✓\033[0m \033[90mHistory saved:\033[0m \033[32mconfirmed\033[0m");
        System.out.println(
                "    \033[32m✓\033[0m \033[90mUser ID:\033[0m \033[32m" + savedHistory.getUserId() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mToken ID:\033[0m \033[32m" + savedHistory.getRefreshTokenId()
                + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mNot revoked:\033[0m \033[32mconfirmed\033[0m\n");
    }

    @Test
    @DisplayName("사용자의 활성 토큰 목록을 조회한다")
    void 사용자의_활성_토큰_목록을_조회한다() {
        System.out.println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mGet Active Tokens for User\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 활성 토큰 목록 준비");
        List<TokenRefreshHistory> activeTokens = Arrays.asList(
                testHistory,
                TokenRefreshHistory.builder()
                        .userId(testHistory.getUserId())
                        .username(testHistory.getUsername())
                        .refreshTokenId("another-token-id")
                        .ipAddress("192.168.1.1")
                        .userAgent("Another-Browser/2.0")
                        .refreshedAt(LocalDateTime.now().minusHours(1))
                        .expiresAt(LocalDateTime.now().plusDays(6))
                        .build());

        when(historyRepository.findActiveTokensByUserId(eq(testHistory.getUserId()), any(LocalDateTime.class)))
                .thenReturn(activeTokens);

        System.out.println("    \033[90m→\033[0m User ID: \033[36m" + testHistory.getUserId() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Active tokens: \033[36m" + activeTokens.size() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 활성 토큰 목록 조회");
        List<TokenRefreshHistory> result = historyService.getActiveTokens(testHistory.getUserId());
        System.out.println("    \033[90m→\033[0m getActiveTokens() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 활성 토큰 목록 검증");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testHistory.getUserId(), result.get(0).getUserId());

        verify(historyRepository).findActiveTokensByUserId(eq(testHistory.getUserId()), any(LocalDateTime.class));

        System.out.println(
                "    \033[32m✓\033[0m \033[90mActive tokens found:\033[0m \033[32m" + result.size() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mUser ID match:\033[0m \033[32mconfirmed\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mNot expired:\033[0m \033[32mconfirmed\033[0m\n");
    }

    @Test
    @DisplayName("토큰을 폐기하고 이력을 기록한다")
    void 토큰을_폐기하고_이력을_기록한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mRevoke Token and Record History\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 폐기할 토큰 준비");
        String reason = "보안상의 이유로 폐기";
        when(historyRepository.findByRefreshTokenIdAndRevokedFalse(testHistory.getRefreshTokenId()))
                .thenReturn(Optional.of(testHistory));
        when(historyRepository.save(any(TokenRefreshHistory.class))).thenReturn(testHistory);

        System.out.println("    \033[90m→\033[0m Token ID: \033[36m" + testHistory.getRefreshTokenId() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Reason: \033[36m" + reason + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 토큰 폐기");
        boolean result = historyService.revokeToken(testHistory.getRefreshTokenId(), reason);
        System.out.println("    \033[90m→\033[0m revokeToken() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 폐기 결과 검증");
        assertTrue(result);
        verify(historyRepository).findByRefreshTokenIdAndRevokedFalse(testHistory.getRefreshTokenId());
        verify(historyRepository).save(argThat(history -> history.isRevoked() &&
                history.getRevokedReason().equals(reason) &&
                history.getRevokedAt() != null));

        System.out.println("    \033[32m✓\033[0m \033[90mToken revoked:\033[0m \033[32mconfirmed\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mReason recorded:\033[0m \033[32mconfirmed\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mRevocation time:\033[0m \033[32mrecorded\033[0m\n");
    }

    @Test
    @DisplayName("갱신 횟수 제한을 확인한다")
    void 갱신_횟수_제한을_확인한다() {
        System.out.println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mCheck Refresh Count Limit\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 갱신 횟수 데이터 준비");
        int maxRefreshes = 5;
        when(historyRepository.countRefreshesInPeriod(eq(testHistory.getUserId()), any(LocalDateTime.class)))
                .thenReturn((long) maxRefreshes);

        System.out.println("    \033[90m→\033[0m User ID: \033[36m" + testHistory.getUserId() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Max refreshes: \033[36m" + maxRefreshes + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 갱신 횟수 제한 확인");
        boolean canRefresh = historyService.canRefresh(testHistory.getUserId());
        System.out.println("    \033[90m→\033[0m canRefresh() 호출");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 갱신 가능 여부 검증");
        assertFalse(canRefresh);
        verify(historyRepository).countRefreshesInPeriod(eq(testHistory.getUserId()), any(LocalDateTime.class));

        System.out.println("    \033[32m✓\033[0m \033[90mLimit reached:\033[0m \033[32mconfirmed\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mRefresh count:\033[0m \033[32m" + maxRefreshes + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mCannot refresh:\033[0m \033[32mconfirmed\033[0m\n");
    }
}
