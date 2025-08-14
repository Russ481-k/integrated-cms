package api.v2.cms.auth.service;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import api.v2.cms.auth.dto.LoginRequest;
import api.v2.cms.auth.dto.ResetPasswordRequest;
import api.v2.cms.auth.dto.SignupRequest;
import api.v2.cms.auth.dto.UserRegistrationRequest;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.cms.user.dto.CustomUserDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface AuthService {
    ResponseEntity<ApiResponseSchema<CustomUserDetails>> register(CustomUserDetails userDetails);

    ResponseEntity<ApiResponseSchema<Map<String, Object>>> login(CustomUserDetails userDetails);

    ResponseEntity<ApiResponseSchema<Map<String, Object>>> snsLogin(String provider, String token);

    ResponseEntity<ApiResponseSchema<Void>> logout(HttpServletRequest request);

    Authentication verifyToken(String token);

    ResponseEntity<ApiResponseSchema<Map<String, String>>> refreshToken(String refreshToken);

    ResponseEntity<ApiResponseSchema<Void>> requestPasswordReset(String email);

    ResponseEntity<ApiResponseSchema<Void>> changePassword(Map<String, String> passwordMap, CustomUserDetails user);

    ResponseEntity<ApiResponseSchema<Void>> registerUser(UserRegistrationRequest request);

    ResponseEntity<ApiResponseSchema<Map<String, Object>>> loginUser(LoginRequest request);

    ResponseEntity<ApiResponseSchema<Void>> logoutUser(HttpServletRequest request);

    ResponseEntity<ApiResponseSchema<Void>> resetPassword(ResetPasswordRequest request, UserDetails userDetails);

    void signup(SignupRequest request);

    // Method for checking username availability
    ResponseEntity<ApiResponseSchema<Map<String, Object>>> checkUsernameAvailability(String username);
}
