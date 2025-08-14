package api.v2.cms.auth.service;

import api.v2.cms.auth.dto.LoginRequest;
import api.v2.common.dto.ApiResponseSchema;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 관리자 인증 서비스 인터페이스
 * admin_user 테이블 기반 인증 전용
 *
 * @author CMS Team
 * @since v2.0
 */
public interface AdminAuthService {

    /**
     * 관리자 로그인
     * @param request 로그인 요청 정보
     * @return JWT 토큰 및 사용자 정보
     */
    ResponseEntity<ApiResponseSchema<Map<String, Object>>> loginUser(LoginRequest request);

    /**
     * 관리자 로그아웃
     * @param request HTTP 요청
     * @return 로그아웃 결과
     */
    ResponseEntity<ApiResponseSchema<Void>> logout(HttpServletRequest request);

    /**
     * 관리자 사용자명 중복 확인
     * @param username 확인할 사용자명
     * @return 사용 가능 여부
     */
    ResponseEntity<ApiResponseSchema<Map<String, Object>>> checkUsernameAvailability(String username);
}
