package api.v2.common.auth.service;

import api.v2.common.auth.dto.*;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.common.user.dto.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 통합 인증 서비스 인터페이스
 * 기존 cms/auth/AuthService의 모든 메서드를 포함하고 새로운 기능을 추가
 */
public interface AuthService {

    // =================================
    // 기존 cms/auth/AuthService 메서드들
    // =================================

    ResponseEntity<ApiResponseSchema<CustomUserDetails>> register(CustomUserDetails userDetails);

    ResponseEntity<ApiResponseSchema<Map<String, Object>>> login(CustomUserDetails userDetails);

    ResponseEntity<ApiResponseSchema<Map<String, Object>>> snsLogin(String provider, String token);

    ResponseEntity<ApiResponseSchema<Void>> logout(HttpServletRequest request);

    Authentication verifyToken(String token);

    ResponseEntity<ApiResponseSchema<Map<String, String>>> refreshToken(String refreshToken);

    ResponseEntity<ApiResponseSchema<Void>> requestPasswordReset(String email);

    ResponseEntity<ApiResponseSchema<Void>> resetPassword(ResetPasswordRequest request, UserDetails userDetails);

    ResponseEntity<ApiResponseSchema<Void>> changePassword(Map<String, String> passwordMap, CustomUserDetails user);

    ResponseEntity<ApiResponseSchema<Map<String, Object>>> loginUser(LoginRequest request);

    ResponseEntity<ApiResponseSchema<Void>> registerUser(UserRegistrationRequest request);

    void signup(SignupRequest request);

    ResponseEntity<ApiResponseSchema<Void>> logoutUser(HttpServletRequest request);

    ResponseEntity<ApiResponseSchema<Map<String, Object>>> checkUsernameAvailability(String username);

    // =================================
    // 새로운 통합된 메서드들
    // =================================

    // 로그인 및 인증 (표준화된 버전)
    ResponseEntity<ApiResponseSchema<Map<String, Object>>> login(LoginRequest loginRequest);

    // 이메일 검증
    void sendEmailVerification(SendEmailVerificationRequestDto request);

    void verifyEmail(VerifyEmailRequestDto request);

    // 비밀번호 관리 (표준화된 버전)
    void resetPassword(ResetPasswordRequest request);

    void changePassword(String userId, String newPassword);

    // 토큰 관리
    void revokeToken(String token);

    void revokeAllTokens(String userId);

    boolean isTokenRevoked(String token);

    // 권한 검증
    boolean hasServiceAccess(String serviceId, Authentication authentication);

    boolean hasIntegratedCmsAccess(Authentication authentication);

    boolean hasContentPermission(String serviceId, String contentType, String action, Authentication authentication);
}