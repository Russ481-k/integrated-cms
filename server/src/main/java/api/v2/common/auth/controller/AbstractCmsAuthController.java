package api.v2.common.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import api.v2.common.auth.dto.LoginRequest;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.common.auth.dto.AuthContext;
import api.v2.common.auth.service.CommonAuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

/**
 * CMS 인증 컨트롤러 공통 기본 클래스
 * 
 * 모든 CMS 타입(통합 CMS, 서비스별 CMS)에서 공통으로 사용되는 인증 기능을 제공
 * 각 구현체에서 서비스 타입에 맞는 구체적인 로직을 구현
 */
@Slf4j
public abstract class AbstractCmsAuthController {

    protected final CommonAuthService authService;

    protected AbstractCmsAuthController(CommonAuthService authService) {
        this.authService = authService;
    }

    /**
     * 서비스 타입 반환 (구현체에서 정의)
     * 
     * @return 서비스 타입 ("cms" | "integrated_cms")
     */
    protected abstract String getServiceType();

    /**
     * 서비스 ID 반환 (구현체에서 정의, 통합 CMS는 null)
     * 
     * @param request HTTP 요청
     * @return 서비스 ID (통합 CMS의 경우 null)
     */
    protected abstract String getServiceId(HttpServletRequest request);

    /**
     * 로그 태그 반환 (구현체에서 정의)
     * 
     * @return 로그 태그
     */
    protected abstract String getLogTag();

    /**
     * 공통 로그인 처리
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인을 처리하고 JWT 토큰을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthContext context = createAuthContext(httpRequest);
        log.info("{} 로그인 시도: {}", getLogTag(), request.getUsername());

        return authService.login(request, context);
    }

    /**
     * 공통 로그아웃 처리
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자를 로그아웃 처리합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponseSchema<Void>> logout(HttpServletRequest request) {
        AuthContext context = createAuthContext(request);
        log.info("{} 로그아웃 처리", getLogTag());

        return authService.logout(request, context);
    }

    /**
     * 공통 토큰 검증 처리
     */
    @GetMapping("/verify")
    @Operation(summary = "토큰 검증", description = "JWT 토큰의 유효성을 검증하고 사용자 정보를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유효한 토큰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> verifyToken(HttpServletRequest request) {
        AuthContext context = createAuthContext(request);
        log.debug("{} 토큰 검증 요청", getLogTag());

        return authService.verifyToken(request, context);
    }

    /**
     * 공통 토큰 갱신 처리
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    public ResponseEntity<ApiResponseSchema<Map<String, String>>> refreshToken(HttpServletRequest request) {
        AuthContext context = createAuthContext(request);
        log.info("{} 토큰 갱신 요청", getLogTag());

        return authService.refreshToken(request, context);
    }

    /**
     * 공통 사용자명 중복 확인 처리
     */
    @GetMapping("/check-username/{username}")
    @Operation(summary = "사용자명 중복 확인", description = "사용자명이 이미 사용 중인지 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 성공 (available: true/false)")
    })
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> checkUsernameAvailability(
            @PathVariable String username, HttpServletRequest request) {

        AuthContext context = createAuthContext(request);
        log.debug("{} 사용자명 중복 확인: {}", getLogTag(), username);

        return authService.checkUsernameAvailability(username, context);
    }

    /**
     * AuthContext 생성 헬퍼 메서드
     */
    protected AuthContext createAuthContext(HttpServletRequest request) {
        return authService.createAuthContext(request, getServiceType(), getServiceId(request));
    }
}
