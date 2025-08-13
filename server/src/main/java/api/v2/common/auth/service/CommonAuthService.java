package api.v2.common.auth.service;

import api.v2.cms.auth.dto.LoginRequest;
import api.v2.cms.common.dto.ApiResponseSchema;
import api.v2.common.auth.dto.AuthContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * 공통 인증 서비스 인터페이스
 * CMS와 integrated_cms에서 공통으로 사용되는 인증 기능을 정의
 */
public interface CommonAuthService {

    /**
     * 사용자 로그인
     * 
     * @param request 로그인 요청 정보
     * @param context 인증 컨텍스트
     * @return 로그인 응답 (토큰 포함)
     */
    ResponseEntity<ApiResponseSchema<Map<String, Object>>> login(LoginRequest request, AuthContext context);

    /**
     * 사용자 로그아웃
     * 
     * @param request HTTP 요청
     * @param context 인증 컨텍스트
     * @return 로그아웃 응답
     */
    ResponseEntity<ApiResponseSchema<Void>> logout(HttpServletRequest request, AuthContext context);

    /**
     * 토큰 검증
     * 
     * @param request HTTP 요청
     * @param context 인증 컨텍스트
     * @return 토큰 검증 응답 (사용자 정보 포함)
     */
    ResponseEntity<ApiResponseSchema<Map<String, Object>>> verifyToken(HttpServletRequest request, AuthContext context);

    /**
     * 토큰 갱신
     * 
     * @param request HTTP 요청
     * @param context 인증 컨텍스트
     * @return 새로운 토큰 응답
     */
    ResponseEntity<ApiResponseSchema<Map<String, String>>> refreshToken(HttpServletRequest request,
            AuthContext context);

    /**
     * 사용자명 중복 확인
     * 
     * @param username 확인할 사용자명
     * @param context  인증 컨텍스트
     * @return 중복 확인 응답
     */
    ResponseEntity<ApiResponseSchema<Map<String, Object>>> checkUsernameAvailability(String username,
            AuthContext context);

    /**
     * 인증 컨텍스트 생성
     * HTTP 요청에서 인증 컨텍스트 정보를 추출하여 생성
     * 
     * @param request     HTTP 요청
     * @param serviceType 서비스 타입 (cms | integrated_cms)
     * @param serviceId   서비스 ID (cms 타입에서만 사용)
     * @return 인증 컨텍스트
     */
    AuthContext createAuthContext(HttpServletRequest request, String serviceType, String serviceId);

    /**
     * 현재 인증 정보에서 AuthContext 생성
     * 
     * @param authentication Spring Security 인증 정보
     * @param serviceType    서비스 타입
     * @param serviceId      서비스 ID
     * @return 인증 컨텍스트
     */
    AuthContext createAuthContext(Authentication authentication, String serviceType, String serviceId);
}
