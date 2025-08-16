package api.v2.common.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.common.auth.dto.LoginRequest;
import api.v2.common.auth.service.AuthService;
import api.v2.common.auth.service.AdminAuthService;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.common.user.domain.User;
import api.v2.common.auth.dto.AuthContext;
import api.v2.common.auth.service.CommonAuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

/**
 * 공통 인증 서비스 구현체
 * 
 * 기존 AuthService와 AdminAuthService를 활용하여
 * 통합 CMS와 서비스별 CMS의 인증 기능을 통합 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonAuthServiceImpl implements CommonAuthService {

    private final AuthService authService;
    private final AdminAuthService adminAuthService;

    @Override
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> login(LoginRequest request, AuthContext context) {
        log.info("{} 로그인 처리 시작: {}", context.getLogContext(), request.getUsername());

        try {
            ResponseEntity<ApiResponseSchema<Map<String, Object>>> response;

            if (context.isIntegratedCms()) {
                // 통합 CMS: AdminAuthService 사용
                response = adminAuthService.loginUser(request);
            } else {
                // 서비스별 CMS: AuthService 사용
                response = authService.loginUser(request);
            }

            // 실제 응답 상태에 따라 로그 출력
            if (response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null &&
                    response.getBody().isSuccess()) {
                log.info("{} 로그인 성공: {}", context.getLogContext(), request.getUsername());
            } else {
                log.warn("{} 로그인 실패: {} - {}", context.getLogContext(), request.getUsername(),
                        response.getBody() != null ? response.getBody().getMessage() : "Unknown error");
            }

            return response;

        } catch (Exception e) {
            log.error("{} 로그인 실패: {} - {}", context.getLogContext(), request.getUsername(), e.getMessage());
            throw e;
        }
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Void>> logout(HttpServletRequest request, AuthContext context) {
        log.info("{} 로그아웃 처리 시작", context.getLogContext());

        try {
            ResponseEntity<ApiResponseSchema<Void>> response;

            if (context.isIntegratedCms()) {
                // 통합 CMS: AdminAuthService 사용
                response = adminAuthService.logout(request);
            } else {
                // 서비스별 CMS: AuthService 사용 (변환 필요)
                authService.logout(request);
                response = ResponseEntity.ok(ApiResponseSchema.success("로그아웃 성공"));
            }

            log.info("{} 로그아웃 성공", context.getLogContext());
            return response;

        } catch (Exception e) {
            log.error("{} 로그아웃 실패: {}", context.getLogContext(), e.getMessage());
            throw e;
        }
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> verifyToken(HttpServletRequest request,
            AuthContext context) {
        log.debug("{} 토큰 검증 처리", context.getLogContext());

        try {
            // AuthController의 verifyToken 로직과 동일하게 구현
            String authHeader = request.getHeader("Authorization");
            Map<String, Object> response = new HashMap<>();

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("valid", false);
                response.put("error", "인증 토큰이 필요합니다.");
                return ResponseEntity.status(401)
                        .body(ApiResponseSchema.error(response, "인증 토큰이 필요합니다."));
            }

            String token = authHeader.substring(7).trim();
            Authentication auth = authService.verifyToken(token);

            response.put("valid", true);
            response.put("username", auth.getName());
            response.put("authorities", auth.getAuthorities());

            // UserDetails에서 추가 정보 추출
            if (auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                response.put("uuid", user.getUuid());
                response.put("email", user.getEmail() != null ? user.getEmail() : "");
                response.put("role", user.getRole() != null ? user.getRole().name() : "USER");
                response.put("name", user.getName() != null ? user.getName() : user.getUsername());
                response.put("status", user.getStatus() != null ? user.getStatus() : "ACTIVE");
                response.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
                response.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "");
            }

            return ResponseEntity.ok(ApiResponseSchema.success(response, "토큰 검증 성공"));

        } catch (Exception e) {
            log.warn("{} 토큰 검증 실패: {}", context.getLogContext(), e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponseSchema.error(response, e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Map<String, String>>> refreshToken(HttpServletRequest request,
            AuthContext context) {
        log.info("{} 토큰 갱신 처리", context.getLogContext());

        try {
            // AuthController의 refreshToken 로직과 동일하게 구현
            String refreshToken = request.getHeader("Authorization");
            return authService.refreshToken(refreshToken);

        } catch (Exception e) {
            log.error("{} 토큰 갱신 실패: {}", context.getLogContext(), e.getMessage());
            throw e;
        }
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> checkUsernameAvailability(String username,
            AuthContext context) {
        log.debug("{} 사용자명 중복 확인: {}", context.getLogContext(), username);

        try {
            ResponseEntity<ApiResponseSchema<Map<String, Object>>> response;

            if (context.isIntegratedCms()) {
                // 통합 CMS: AdminAuthService 사용
                response = adminAuthService.checkUsernameAvailability(username);
            } else {
                // 서비스별 CMS: AuthService 사용
                response = authService.checkUsernameAvailability(username);
            }

            return response;

        } catch (Exception e) {
            log.error("{} 사용자명 중복 확인 실패: {} - {}", context.getLogContext(), username, e.getMessage());
            throw e;
        }
    }

    @Override
    public AuthContext createAuthContext(HttpServletRequest request, String serviceType, String serviceId) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        return AuthContext.builder()
                .serviceType(serviceType)
                .serviceId(serviceId)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
    }

    @Override
    public AuthContext createAuthContext(Authentication authentication, String serviceType, String serviceId) {
        AuthContext.AuthContextBuilder builder = AuthContext.builder()
                .serviceType(serviceType)
                .serviceId(serviceId)
                .username(authentication.getName());

        // UserDetails에서 추가 정보 추출
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            if (userDetails instanceof User) {
                User user = (User) userDetails;
                builder.userUuid(user.getUuid())
                        .userRole(user.getRole());
            }
        }

        return builder.build();
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
