package api.v2.common.auth.service.impl;

import api.v2.common.auth.dto.*;
import api.v2.common.auth.dto.TokenDto;
import api.v2.common.auth.provider.JwtTokenProvider;
import api.v2.common.auth.service.*;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.common.user.domain.User;
import api.v2.common.user.domain.UserRoleType;
import api.v2.common.user.dto.CustomUserDetails;
import api.v2.common.user.repository.UserRepository;
import api.v2.common.exception.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 통합 인증 서비스 구현
 * 기존 cms/auth/AuthServiceImpl의 모든 기능을 보존하고 새로운 기능을 추가
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final TokenRefreshService tokenRefreshService;
    private final VerificationCodeService verificationCodeService;
    private final ServiceAccessChecker serviceAccessChecker;
    private final IntegratedCmsAccessChecker integratedCmsAccessChecker;
    private final ContentPermissionChecker contentPermissionChecker;

    @Autowired
    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            UserRepository userRepository,
            TokenRefreshService tokenRefreshService,
            VerificationCodeService verificationCodeService,
            ServiceAccessChecker serviceAccessChecker,
            IntegratedCmsAccessChecker integratedCmsAccessChecker,
            ContentPermissionChecker contentPermissionChecker) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.tokenRefreshService = tokenRefreshService;
        this.verificationCodeService = verificationCodeService;
        this.serviceAccessChecker = serviceAccessChecker;
        this.integratedCmsAccessChecker = integratedCmsAccessChecker;
        this.contentPermissionChecker = contentPermissionChecker;
    }

    // =================================
    // 기존 기능들 (원본 유지)
    // =================================

    @Override
    @Transactional
    public ResponseEntity<ApiResponseSchema<CustomUserDetails>> register(CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponseSchema.success(new CustomUserDetails(user), "사용자가 성공적으로 등록되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> login(CustomUserDetails userDetails) {
        Map<String, Object> result = new HashMap<>();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDetails.getUsername(), userDetails.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.createAccessToken(customUserDetails.getUser());
            String refreshToken = jwtTokenProvider.createRefreshToken(customUserDetails.getUser());

            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("user", customUserDetails);
            result.put("status", "success");

            return ResponseEntity.ok(ApiResponseSchema.success(result, "로그인이 성공적으로 완료되었습니다."));
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return ResponseEntity.ok(ApiResponseSchema.error(result, "로그인에 실패했습니다.", "AUTH_001"));
        }
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> snsLogin(String provider, String token) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        return ResponseEntity.ok(ApiResponseSchema.success(result, "SNS 로그인이 성공적으로 완료되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Void>> logout(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null) {
            // TODO: 토큰 무효화 로직 구현
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponseSchema.success("로그아웃이 완료되었습니다."));
    }

    @Override
    public Authentication verifyToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("토큰이 없습니다.");
        }

        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        return jwtTokenProvider.getAuthentication(token);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponseSchema<Map<String, String>>> refreshToken(String refreshToken) {
        return tokenRefreshService.refreshToken(refreshToken);
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Void>> requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 등록된 사용자가 없습니다."));

        if (!user.getStatus().equals("ACTIVE")) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        try {
            sendResetEmail(user.getEmail(), resetToken);
        } catch (MailException e) {
            throw new IllegalArgumentException("이메일 전송에 실패했습니다.", e);
        }

        return ResponseEntity.ok(ApiResponseSchema.success("비밀번호 재설정 이메일이 전송되었습니다."));
    }

    private void sendResetEmail(String email, String resetToken) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("비밀번호 재설정");
            helper.setText("비밀번호 재설정을 위해 다음 링크를 클릭하세요: " + resetToken);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalArgumentException("이메일 전송에 실패했습니다.", e);
        }
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Void>> resetPassword(ResetPasswordRequest request,
            UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponseSchema.success("비밀번호가 성공적으로 변경되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Void>> changePassword(Map<String, String> passwordMap,
            CustomUserDetails user) {
        User existingUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(passwordMap.get("currentPassword"), existingUser.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        existingUser.setPassword(passwordEncoder.encode(passwordMap.get("newPassword")));
        userRepository.save(existingUser);

        return ResponseEntity.ok(ApiResponseSchema.success("비밀번호가 성공적으로 변경되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> loginUser(LoginRequest request) {
        try {
            // 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 사용자 정보 조회
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(user);
            String refreshToken = jwtTokenProvider.createRefreshToken(user);

            // 마지막 로그인 시간 업데이트
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // 응답 데이터 구성
            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("tokenType", "Bearer");
            result.put("user", new HashMap<String, Object>() {
                {
                    put("uuid", user.getUuid());
                    put("username", user.getUsername());
                    put("role", user.getRole().name());
                }
            });

            return ResponseEntity.ok(ApiResponseSchema.success(result, "로그인이 성공적으로 완료되었습니다."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseSchema.error("아이디 또는 비밀번호가 일치하지 않습니다.", "401"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseSchema.error("로그인 처리 중 오류가 발생했습니다.", "500"));
        }
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Void>> registerUser(UserRegistrationRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .name(request.getName())
                .role(request.getRole())
                .status("ACTIVE")
                .organizationId(request.getOrganizationId())
                .groupId(request.getGroupId())
                .build();

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponseSchema.success("사용자가 성공적으로 등록되었습니다."));
    }

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        log.info("[AuthService] signup attempt for username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("이미 사용 중인 사용자 ID입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .carNo(request.getCarNo())
                .gender(request.getGender())
                .role(UserRoleType.USER)
                .status("ACTIVE")
                .provider("LOCAL")
                .build();

        userRepository.save(user);
        log.info("[AuthService] User {} signed up successfully.", request.getUsername());
    }

    @Override
    public ResponseEntity<ApiResponseSchema<Void>> logoutUser(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponseSchema.success("로그아웃이 완료되었습니다."));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> checkUsernameAvailability(String username) {
        boolean available = !userRepository.existsByUsername(username);
        Map<String, Object> result = new HashMap<>();
        result.put("available", available);
        if (!available) {
            result.put("message", "이미 사용 중인 사용자 ID입니다.");
        }
        return ResponseEntity.ok(ApiResponseSchema.success(result,
                available ? "사용 가능한 사용자 ID입니다." : "이미 사용 중인 사용자 ID입니다."));
    }

    // =================================
    // 통합된 새로운 기능들
    // =================================

    @Override
    @Transactional
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> login(LoginRequest loginRequest) {
        try {
            // 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 사용자 정보 조회
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(user);
            String refreshToken = jwtTokenProvider.createRefreshToken(user);

            // 응답 데이터 구성
            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("tokenType", "Bearer");
            result.put("user", new HashMap<String, Object>() {
                {
                    put("uuid", user.getUuid());
                    put("username", user.getUsername());
                    put("role", user.getRole().name());
                }
            });

            return ResponseEntity.ok(ApiResponseSchema.success(result, "로그인이 성공적으로 완료되었습니다."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.ok(ApiResponseSchema.error("아이디 또는 비밀번호가 일치하지 않습니다.", "AUTH_001"));
        }
    }

    @Override
    @Transactional
    public void sendEmailVerification(SendEmailVerificationRequestDto request) {
        verificationCodeService.generateAndStoreCode(request.getEmail());
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequestDto request) {
        verificationCodeService.verifyCode(request.getEmail(), request.getCode());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // This method signature seems incomplete - it should take UserDetails or
        // username parameter
        // For now, implementing basic logic without user identification
        throw new UnsupportedOperationException("resetPassword method needs proper user identification parameter");
    }

    @Override
    @Transactional
    public void changePassword(String userId, String newPassword) {
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        // TODO: Implement token blacklist mechanism for JWT token revocation
        // Since JWT tokens are stateless, server-side revocation requires a blacklist
    }

    @Override
    @Transactional
    public void revokeAllTokens(String userId) {
        // TODO: Implement token blacklist for all user's tokens
    }

    @Override
    public boolean isTokenRevoked(String token) {
        // TODO: 토큰 무효화 상태 확인 로직 구현
        return false;
    }

    @Override
    public boolean hasServiceAccess(String serviceId, Authentication authentication) {
        return serviceAccessChecker.hasAccess(authentication, serviceId);
    }

    @Override
    public boolean hasIntegratedCmsAccess(Authentication authentication) {
        return integratedCmsAccessChecker.hasAccess(authentication);
    }

    @Override
    public boolean hasContentPermission(String serviceId, String contentType, String action,
            Authentication authentication) {
        return contentPermissionChecker.hasPermission(authentication, serviceId, contentType, action);
    }
}