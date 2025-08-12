package api.v2.integrated_cms;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.cms.auth.dto.LoginRequest;
import api.v2.cms.auth.service.AuthService;
import api.v2.cms.common.dto.ApiResponseSchema;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/integrated-cms/auth")
@RequiredArgsConstructor
@Tag(name = "Integrated CMS Authentication", description = "통합 CMS 인증 API")
public class IntegratedCmsAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "통합 CMS 로그인", description = "통합 CMS 관리자 로그인을 처리하고 JWT 토큰을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Integrated CMS login attempt for user: {}", request.getUsername());
        return authService.loginUser(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "통합 CMS 로그아웃", description = "사용자를 로그아웃 처리합니다.")
    public ResponseEntity<ApiResponseSchema<Void>> logout(HttpServletRequest request) {
        return authService.logout(request);
    }

    @GetMapping("/check-username/{username}")
    @Operation(summary = "사용자명 중복 확인", description = "사용자명이 이미 사용 중인지 확인합니다.")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> checkUsername(@PathVariable String username) {
        return authService.checkUsernameAvailability(username);
    }
}
