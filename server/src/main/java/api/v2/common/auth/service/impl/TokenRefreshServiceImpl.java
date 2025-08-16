package api.v2.common.auth.service.impl;

import api.v2.common.auth.provider.JwtTokenProvider;
import api.v2.common.auth.service.TokenRefreshService;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.common.user.domain.User;
import api.v2.common.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenRefreshServiceImpl implements TokenRefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<ApiResponseSchema<Map<String, String>>> refreshToken(String refreshToken) {
        try {
            // 리프레시 토큰 검증
            if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseSchema.error("유효하지 않은 토큰입니다. 다시 로그인해주세요.", "TOKEN_INVALID"));
            }

            // 토큰에서 사용자 정보 추출
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new JwtException("유효하지 않은 토큰입니다."));

            // 새로운 액세스 토큰 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(user);

            // 응답 데이터 구성
            Map<String, String> tokenInfo = new HashMap<>();
            tokenInfo.put("accessToken", newAccessToken);
            tokenInfo.put("tokenType", "Bearer");

            return ResponseEntity.ok(ApiResponseSchema.success(tokenInfo, "토큰이 성공적으로 갱신되었습니다."));

        } catch (JwtException e) {
            String message = e.getMessage().contains("만료") ? "토큰이 만료되었습니다. 다시 로그인해주세요." : "유효하지 않은 토큰입니다. 다시 로그인해주세요.";
            String errorCode = e.getMessage().contains("만료") ? "TOKEN_EXPIRED" : "TOKEN_INVALID";

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseSchema.error(message, errorCode));
        }
    }
}
