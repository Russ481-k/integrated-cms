package api.v2.cms.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import api.v2.cms.auth.service.AdminAuthService;
import api.v2.cms.user.domain.AdminUser;
import api.v2.cms.user.repository.AdminUserRepository;
import api.v2.cms.auth.provider.JwtTokenProvider;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.cms.auth.dto.LoginRequest;

/**
 * 관리자 인증 서비스 구현
 * admin_user 테이블 기반 인증 전용
 *
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Service("adminAuthService")
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> loginUser(LoginRequest request) {
        try {
            log.info("Admin login attempt for user: {}", request.getUsername());

            // 1. 사용자명으로 관리자 계정 조회
            AdminUser adminUser = adminUserRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("관리자 계정을 찾을 수 없습니다."));

            log.debug("Found admin user: {} with role: {}", adminUser.getUsername(), adminUser.getRole());

            // 2. 비밀번호 일치 여부 확인
            if (!passwordEncoder.matches(request.getPassword(), adminUser.getPassword())) {
                log.warn("Password mismatch for admin user: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseSchema.error("비밀번호가 일치하지 않습니다.", "AUTH_INVALID_PASSWORD"));
            }

            // 3. 계정 상태 확인 (예: 활성화 여부)
            if (!"ACTIVE".equals(adminUser.getStatus())) {
                log.warn("Inactive admin account: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseSchema.error("비활성화된 관리자 계정입니다.", "AUTH_ACCOUNT_DISABLED"));
            }

            // 4. 인증 성공 로깅
            log.info("Authentication successful for admin user: {}", adminUser.getUsername());

            // 5. JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(adminUser);
            String refreshToken = jwtTokenProvider.createRefreshToken(adminUser);

            // 6. 마지막 로그인 시간 업데이트
            adminUser.setUpdatedAt(LocalDateTime.now());
            adminUserRepository.save(adminUser);

            // 7. 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", accessToken);
            responseData.put("refreshToken", refreshToken);
            responseData.put("tokenType", "Bearer");
            responseData.put("user", createUserResponseData(adminUser));

            return ResponseEntity.ok(ApiResponseSchema.success(responseData, "로그인이 성공적으로 완료되었습니다."));

        } catch (BadCredentialsException e) {
            log.warn("Admin login failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseSchema.error("아이디 또는 비밀번호가 일치하지 않습니다.", "AUTH_INVALID_CREDENTIALS"));
        } catch (Exception e) {
            log.error("Admin login error for user {}: {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseSchema.error("로그인 처리 중 오류가 발생했습니다.", "AUTH_LOGIN_ERROR"));
        }
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Void>> logout(HttpServletRequest request) {
        try {
            // Stateless JWT 기반이므로 서버에서 별도 처리할 내용 없음
            // 클라이언트에서 토큰을 삭제하면 인증이 해제됨
            
            // 필요시 로그아웃 감사 로그 기록
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String username = jwtTokenProvider.getUsernameFromToken(token);
                    log.info("Admin logout successful for user: {}", username);
                } catch (Exception e) {
                    log.debug("Could not extract username from token during logout", e);
                }
            }

            return ResponseEntity.ok(ApiResponseSchema.success(null, "로그아웃이 성공적으로 처리되었습니다."));

        } catch (Exception e) {
            log.error("Admin logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseSchema.error("로그아웃 처리 중 오류가 발생했습니다.", "AUTH_LOGOUT_ERROR"));
        }
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> checkUsernameAvailability(String username) {
        try {
            boolean isAvailable = !adminUserRepository.existsByUsername(username);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("isAvailable", isAvailable);
            
            return ResponseEntity.ok(ApiResponseSchema.success(responseData, 
                isAvailable ? "사용 가능한 사용자명입니다." : "이미 사용 중인 사용자명입니다."));
        } catch (Exception e) {
            log.error("Error checking username availability for admin: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseSchema.error("사용자명 중복 확인 중 오류가 발생했습니다.", "AUTH_USERNAME_CHECK_ERROR"));
        }
    }

    /**
     * 사용자 응답 데이터 생성 (민감한 정보 제외)
     */
    private Map<String, Object> createUserResponseData(AdminUser adminUser) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uuid", adminUser.getUuid());
        userData.put("username", adminUser.getUsername());
        userData.put("name", adminUser.getName());
        userData.put("email", adminUser.getEmail());
        userData.put("role", adminUser.getRole());
        userData.put("status", adminUser.getStatus());
        userData.put("organizationId", adminUser.getOrganizationId());
        userData.put("groupId", adminUser.getGroupId());
        userData.put("avatarUrl", adminUser.getAvatarUrl());
        return userData;
    }
}
