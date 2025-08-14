package api.v2.cms.auth.service.impl;

import api.v2.cms.auth.domain.TokenRefreshHistory;
import api.v2.cms.auth.repository.TokenRefreshHistoryRepository;
import api.v2.cms.auth.service.TokenRefreshHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenRefreshHistoryServiceImpl implements TokenRefreshHistoryService {

    private static final int MAX_REFRESHES_PER_DAY = 5;

    private final TokenRefreshHistoryRepository historyRepository;

    @Override
    @Transactional
    public TokenRefreshHistory recordRefresh(String userId, String username, String refreshTokenId,
            String ipAddress, String userAgent, LocalDateTime expiresAt) {
        TokenRefreshHistory history = TokenRefreshHistory.builder()
                .userId(userId)
                .username(username)
                .refreshTokenId(refreshTokenId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .refreshedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        return historyRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenRefreshHistory> getActiveTokens(String userId) {
        return historyRepository.findActiveTokensByUserId(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public boolean revokeToken(String refreshTokenId, String reason) {
        return historyRepository.findByRefreshTokenIdAndRevokedFalse(refreshTokenId)
                .map(history -> {
                    history.setRevoked(true);
                    history.setRevokedAt(LocalDateTime.now());
                    history.setRevokedReason(reason);
                    historyRepository.save(history);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRefresh(String userId) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long refreshCount = historyRepository.countRefreshesInPeriod(userId, oneDayAgo);
        return refreshCount < MAX_REFRESHES_PER_DAY;
    }
}
