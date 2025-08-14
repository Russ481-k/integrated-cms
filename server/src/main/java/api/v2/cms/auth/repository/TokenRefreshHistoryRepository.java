package api.v2.cms.auth.repository;

import api.v2.cms.auth.domain.TokenRefreshHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenRefreshHistoryRepository extends JpaRepository<TokenRefreshHistory, Long> {

    Optional<TokenRefreshHistory> findByRefreshTokenIdAndRevokedFalse(String refreshTokenId);

    List<TokenRefreshHistory> findByUserIdOrderByRefreshedAtDesc(String userId);

    @Query("SELECT h FROM TokenRefreshHistory h WHERE h.userId = :userId AND h.revoked = false AND h.expiresAt > :now")
    List<TokenRefreshHistory> findActiveTokensByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(h) FROM TokenRefreshHistory h WHERE h.userId = :userId AND h.refreshedAt >= :since")
    long countRefreshesInPeriod(@Param("userId") String userId, @Param("since") LocalDateTime since);
}
