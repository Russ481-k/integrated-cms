package api.v2.cms.auth.service;

import api.v2.cms.auth.domain.TokenRefreshHistory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 토큰 갱신 이력 관리 서비스
 * 리프레시 토큰의 갱신 이력을 관리하고 제한을 적용
 */
public interface TokenRefreshHistoryService {
    /**
     * 토큰 갱신 이력을 기록
     */
    TokenRefreshHistory recordRefresh(String userId, String username, String refreshTokenId,
            String ipAddress, String userAgent, LocalDateTime expiresAt);

    /**
     * 사용자의 활성 토큰 목록 조회
     */
    List<TokenRefreshHistory> getActiveTokens(String userId);

    /**
     * 토큰 폐기 및 이력 기록
     */
    boolean revokeToken(String refreshTokenId, String reason);

    /**
     * 갱신 횟수 제한 확인
     */
    boolean canRefresh(String userId);
}
