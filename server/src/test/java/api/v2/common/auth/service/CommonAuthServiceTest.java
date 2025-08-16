package api.v2.common.auth.service;

import api.v2.common.auth.dto.LoginRequest;
import api.v2.common.auth.service.AdminAuthService;
import api.v2.common.auth.service.AuthService;
import api.v2.common.auth.dto.AuthContext;
import api.v2.common.auth.service.impl.CommonAuthServiceImpl;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.common.user.domain.UserRoleType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CommonAuthService 단위 테스트
 * 
 * 통합 인증 서비스의 핵심 기능을 검증:
 * - 통합 인증 로직
 * - 서비스별 인증 분기
 * - 인증 컨텍스트 생성
 */
@ExtendWith(MockitoExtension.class)
class CommonAuthServiceTest {

        @Mock
        private AuthService authService;

        @Mock
        private AdminAuthService adminAuthService;

        private CommonAuthService commonAuthService;
        private LoginRequest validLoginRequest;
        private AuthContext integratedContext;
        private AuthContext serviceContext;

        @BeforeEach
        void setUp() {
                commonAuthService = new CommonAuthServiceImpl(authService, adminAuthService);

                // 유효한 로그인 요청 생성
                validLoginRequest = new LoginRequest();
                validLoginRequest.setUsername("user");
                validLoginRequest.setPassword("password123!");

                // 통합 CMS 컨텍스트 생성
                integratedContext = AuthContext.builder()
                                .serviceType("integrated_cms")
                                .serviceId(null)
                                .userUuid("admin-uuid")
                                .username("admin")
                                .userRole(UserRoleType.SUPER_ADMIN)
                                .clientIp("127.0.0.1")
                                .userAgent("Test-Agent")
                                .build();

                // 서비스별 CMS 컨텍스트 생성
                serviceContext = AuthContext.builder()
                                .serviceType("cms")
                                .serviceId("douzone")
                                .userUuid("user-uuid")
                                .username("user")
                                .userRole(UserRoleType.USER)
                                .clientIp("127.0.0.1")
                                .userAgent("Test-Agent")
                                .build();
        }

        @AfterEach
        void tearDown() {
                commonAuthService = null;
                validLoginRequest = null;
                integratedContext = null;
                serviceContext = null;
        }

        @Test
        @DisplayName("통합 CMS 컨텍스트에서는 AdminAuthService를 사용한다")
        void 통합_CMS_컨텍스트에서는_AdminAuthService를_사용한다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mIntegrated CMS Uses AdminAuthService\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 통합 CMS 컨텍스트와 로그인 요청 준비");
                Map<String, Object> userData = new HashMap<>();
                userData.put("uuid", "admin-uuid");
                userData.put("username", "admin");
                userData.put("role", UserRoleType.SUPER_ADMIN);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("accessToken", "test.access.token");
                responseData.put("refreshToken", "test.refresh.token");
                responseData.put("tokenType", "Bearer");
                responseData.put("user", userData);

                ResponseEntity<ApiResponseSchema<Map<String, Object>>> mockResponse = ResponseEntity
                                .ok(ApiResponseSchema.success(responseData, "로그인이 성공적으로 완료되었습니다."));

                when(adminAuthService.loginUser(validLoginRequest)).thenReturn(mockResponse);

                System.out.println(
                                "    \033[90m→\033[0m Context type: \033[36m" + integratedContext.getServiceType()
                                                + "\033[0m");
                System.out.println("    \033[90m→\033[0m Username: \033[36m" + validLoginRequest.getUsername()
                                + "\033[0m");

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 통합 CMS 로그인 요청");
                ResponseEntity<ApiResponseSchema<Map<String, Object>>> response = commonAuthService.login(
                                validLoginRequest,
                                integratedContext);
                System.out.println("    \033[90m→\033[0m login() 호출");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m AdminAuthService 사용 확인");
                assertNotNull(response);
                assertEquals(HttpStatus.OK, response.getStatusCode());

                verify(adminAuthService).loginUser(validLoginRequest);
                verify(authService, never()).loginUser(any()); // AuthService는 호출되지 않음

                System.out.println(
                                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode()
                                                + "\033[0m");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAdminAuthService used:\033[0m \033[32mconfirmed\033[0m");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAuthService not used:\033[0m \033[32mconfirmed\033[0m\n");
        }

        @Test
        @DisplayName("서비스별 CMS 컨텍스트에서는 AuthService를 사용한다")
        void 서비스별_CMS_컨텍스트에서는_AuthService를_사용한다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mService CMS Uses AuthService\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 서비스별 CMS 컨텍스트와 로그인 요청 준비");
                Map<String, Object> userData = new HashMap<>();
                userData.put("uuid", "user-uuid");
                userData.put("username", "user");
                userData.put("role", UserRoleType.USER);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("accessToken", "test.access.token");
                responseData.put("refreshToken", "test.refresh.token");
                responseData.put("tokenType", "Bearer");
                responseData.put("user", userData);

                ResponseEntity<ApiResponseSchema<Map<String, Object>>> mockResponse = ResponseEntity
                                .ok(ApiResponseSchema.success(responseData, "로그인이 성공적으로 완료되었습니다."));

                when(authService.loginUser(validLoginRequest)).thenReturn(mockResponse);

                System.out.println("    \033[90m→\033[0m Context type: \033[36m" + serviceContext.getServiceType()
                                + "\033[0m");
                System.out.println("    \033[90m→\033[0m Service ID: \033[36m" + serviceContext.getServiceId()
                                + "\033[0m");
                System.out.println("    \033[90m→\033[0m Username: \033[36m" + validLoginRequest.getUsername()
                                + "\033[0m");

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 서비스별 CMS 로그인 요청");
                ResponseEntity<ApiResponseSchema<Map<String, Object>>> response = commonAuthService.login(
                                validLoginRequest,
                                serviceContext);
                System.out.println("    \033[90m→\033[0m login() 호출");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m AuthService 사용 확인");
                assertNotNull(response);
                assertEquals(HttpStatus.OK, response.getStatusCode());

                verify(authService).loginUser(validLoginRequest);
                verify(adminAuthService, never()).loginUser(any()); // AdminAuthService는 호출되지 않음

                System.out.println(
                                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode()
                                                + "\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mAuthService used:\033[0m \033[32mconfirmed\033[0m");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAdminAuthService not used:\033[0m \033[32mconfirmed\033[0m\n");
        }

        @Test
        @DisplayName("로그인 실패 시 적절한 에러 응답을 반환한다")
        void 로그인_실패_시_적절한_에러_응답을_반환한다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mLogin Failure Error Response\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 로그인 실패 응답 준비");
                ResponseEntity<ApiResponseSchema<Map<String, Object>>> mockResponse = ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponseSchema.error("아이디 또는 비밀번호가 일치하지 않습니다.", "401"));

                when(authService.loginUser(validLoginRequest)).thenReturn(mockResponse);

                System.out.println("    \033[90m→\033[0m Context type: \033[36m" + serviceContext.getServiceType()
                                + "\033[0m");
                System.out.println("    \033[90m→\033[0m Username: \033[36m" + validLoginRequest.getUsername()
                                + "\033[0m");
                System.out.println("    \033[90m→\033[0m Expected error: \033[31m401 Unauthorized\033[0m");

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 실패할 로그인 요청");
                ResponseEntity<ApiResponseSchema<Map<String, Object>>> response = commonAuthService.login(
                                validLoginRequest,
                                serviceContext);
                System.out.println("    \033[90m→\033[0m login() 호출");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 에러 응답 검증");
                assertNotNull(response);
                assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

                ApiResponseSchema<Map<String, Object>> body = response.getBody();
                assertNotNull(body);
                assertFalse(body.isSuccess());
                assertEquals("아이디 또는 비밀번호가 일치하지 않습니다.", body.getMessage());
                assertEquals("401", body.getErrorCode());

                System.out.println(
                                "    \033[32m✓\033[0m \033[90mStatus code:\033[0m \033[32m" + response.getStatusCode()
                                                + "\033[0m");
                System.out
                                .println("    \033[32m✓\033[0m \033[90mError message:\033[0m \033[31m"
                                                + body.getMessage() + "\033[0m");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mError code:\033[0m \033[31m" + body.getErrorCode()
                                                + "\033[0m\n");
        }

        @Test
        @DisplayName("인증 컨텍스트를 올바르게 생성한다")
        void 인증_컨텍스트를_올바르게_생성한다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mAuth Context Creation\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m HTTP 요청 정보 준비");
                String serviceType = "cms";
                String serviceId = "douzone";
                String clientIp = "192.168.1.100";
                String userAgent = "Test-Browser/1.0";

                System.out.println("    \033[90m→\033[0m Service type: \033[36m" + serviceType + "\033[0m");
                System.out.println("    \033[90m→\033[0m Service ID: \033[36m" + serviceId + "\033[0m");
                System.out.println("    \033[90m→\033[0m Client IP: \033[36m" + clientIp + "\033[0m");
                System.out.println("    \033[90m→\033[0m User-Agent: \033[36m" + userAgent + "\033[0m");

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 인증 컨텍스트 생성");
                AuthContext context = AuthContext.builder()
                                .serviceType(serviceType)
                                .serviceId(serviceId)
                                .clientIp(clientIp)
                                .userAgent(userAgent)
                                .build();
                System.out.println("    \033[90m→\033[0m AuthContext 생성");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 컨텍스트 필드 검증");
                assertNotNull(context);
                assertEquals(serviceType, context.getServiceType());
                assertEquals(serviceId, context.getServiceId());
                assertEquals(clientIp, context.getClientIp());
                assertEquals(userAgent, context.getUserAgent());
                assertFalse(context.isIntegratedCms()); // cms 타입이므로 false
                assertTrue(context.isServiceCms()); // cms 타입이므로 true

                System.out.println(
                                "    \033[32m✓\033[0m \033[90mService type:\033[0m \033[32m" + context.getServiceType()
                                                + "\033[0m");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mService ID:\033[0m \033[32m" + context.getServiceId()
                                                + "\033[0m");
                System.out
                                .println("    \033[32m✓\033[0m \033[90mClient IP:\033[0m \033[32m"
                                                + context.getClientIp() + "\033[0m");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mUser-Agent:\033[0m \033[32m" + context.getUserAgent()
                                                + "\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mContext type:\033[0m \033[32mservice_cms\033[0m\n");
        }
}
