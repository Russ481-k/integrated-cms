package api.v2.common.auth.service;

import api.v2.common.dto.ApiResponseSchema;
import org.springframework.http.ResponseEntity;
import java.util.Map;

/**
 * 토큰 갱신 서비스
 * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급
 */
public interface TokenRefreshService {
    /**
     * 리프레시 토큰을 검증하고 새로운 액세스 토큰을 발급
     *
     * @param refreshToken 리프레시 토큰
     * @return 새로운 액세스 토큰이 포함된 응답
     */
    ResponseEntity<ApiResponseSchema<Map<String, String>>> refreshToken(String refreshToken);
}
