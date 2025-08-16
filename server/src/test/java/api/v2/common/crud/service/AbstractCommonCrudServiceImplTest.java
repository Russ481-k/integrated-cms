package api.v2.common.crud.service;

import api.v2.common.crud.dto.CrudContext;
import api.v2.common.crud.service.impl.AbstractCommonCrudServiceImpl;
import api.v2.common.user.domain.UserRoleType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractCommonCrudServiceImpl 단위 테스트
 * 
 * 공통 CRUD 서비스의 핵심 기능을 검증:
 * - CrudContext 생성 로직
 * - 권한별 접근 제어 로직
 * - 클라이언트 IP 추출 로직
 * - 감사 로깅 기능
 */
@ExtendWith(MockitoExtension.class)
class AbstractCommonCrudServiceImplTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private TestCrudService crudService;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        crudService = new TestCrudService();
        mockRequest = new MockHttpServletRequest();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 정리
        crudService = null;
        mockRequest = null;
    }

    @Test
    @DisplayName("HTTP 요청에서 CRUD 컨텍스트를 올바르게 생성한다")
    void HTTP_요청에서_CRUD_컨텍스트를_올바르게_생성한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mCrudContext Creation From HTTP Request\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m HTTP 요청에 클라이언트 정보 설정");
        mockRequest.setRemoteAddr("127.0.0.1");
        mockRequest.addHeader("User-Agent", "Test-Browser/1.0");
        mockRequest.addHeader("X-Forwarded-For", "192.168.1.100");
        System.out.println("    \033[90m→\033[0m Remote IP: \033[36m127.0.0.1\033[0m");
        System.out.println("    \033[90m→\033[0m X-Forwarded-For: \033[36m192.168.1.100\033[0m");
        System.out.println("    \033[90m→\033[0m User-Agent: \033[36mTest-Browser/1.0\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m createCrudContext() 호출");
        CrudContext context = crudService.createCrudContext(
                mockRequest, "cms", "douzone", "content");
        System.out.println(
                "    \033[90m→\033[0m createCrudContext(\033[36m\"cms\"\033[0m, \033[36m\"douzone\"\033[0m, \033[36m\"content\"\033[0m)");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 컨텍스트 정보가 올바르게 설정되었는지 확인");
        assertNotNull(context);
        assertEquals("cms", context.getServiceType());
        assertEquals("douzone", context.getServiceId());
        assertEquals("content", context.getResourceName());
        assertEquals("192.168.1.100", context.getClientIp()); // X-Forwarded-For 우선
        assertEquals("Test-Browser/1.0", context.getUserAgent());
        assertNotNull(context.getRequestTime());
        System.out.println("    \033[32m✓\033[0m \033[90mContext created:\033[0m \033[32m" + context.getServiceType()
                + "/" + context.getServiceId() + "/" + context.getResourceName() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mClient IP extracted:\033[0m \033[32m" + context.getClientIp()
                + "\033[0m\n");
    }

    @Test
    @DisplayName("SUPER_ADMIN은 모든 CRUD 작업 권한을 가진다")
    void SUPER_ADMIN_은_모든_CRUD_작업_권한을_가진다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mSUPER_ADMIN Full CRUD Permissions\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m SUPER_ADMIN 권한의 CRUD 컨텍스트 생성");
        CrudContext context = createTestContext(UserRoleType.SUPER_ADMIN);
        System.out.println("    \033[90m→\033[0m UserRole: \033[35m" + context.getUserRole() + "\033[0m");

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m 모든 권한 검증");

        // 목록 조회 권한
        assertTrue(crudService.hasListPermission(context));
        System.out.println("    \033[32m✓\033[0m \033[90mList permission:\033[0m \033[32mgranted\033[0m");

        // 읽기 권한
        assertTrue(crudService.hasReadPermission(1L, context));
        System.out.println("    \033[32m✓\033[0m \033[90mRead permission:\033[0m \033[32mgranted\033[0m");

        // 생성 권한
        assertTrue(crudService.hasCreatePermission(context));
        System.out.println("    \033[32m✓\033[0m \033[90mCreate permission:\033[0m \033[32mgranted\033[0m");

        // 수정 권한
        assertTrue(crudService.hasUpdatePermission(1L, context));
        System.out.println("    \033[32m✓\033[0m \033[90mUpdate permission:\033[0m \033[32mgranted\033[0m");

        // 삭제 권한
        assertTrue(crudService.hasDeletePermission(1L, context));
        System.out.println("    \033[32m✓\033[0m \033[90mDelete permission:\033[0m \033[32mgranted\033[0m");

        System.out.println("  \033[2m✨ Verify:\033[0m SUPER_ADMIN의 전체 권한 확인 완료\n");
    }

    @Test
    @DisplayName("ADMIN은 삭제 권한이 없고 나머지 권한만 가진다")
    void ADMIN_은_삭제_권한이_없고_나머지_권한만_가진다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mADMIN Limited CRUD Permissions\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m ADMIN 권한의 CRUD 컨텍스트 생성");
        CrudContext context = createTestContext(UserRoleType.ADMIN);
        System.out.println("    \033[90m→\033[0m UserRole: \033[36m" + context.getUserRole() + "\033[0m");

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m ADMIN 권한 검증 (삭제 제외)");

        // 허용되는 권한들
        assertTrue(crudService.hasListPermission(context));
        System.out.println("    \033[32m✓\033[0m \033[90mList permission:\033[0m \033[32mgranted\033[0m");

        assertTrue(crudService.hasReadPermission(1L, context));
        System.out.println("    \033[32m✓\033[0m \033[90mRead permission:\033[0m \033[32mgranted\033[0m");

        assertTrue(crudService.hasCreatePermission(context));
        System.out.println("    \033[32m✓\033[0m \033[90mCreate permission:\033[0m \033[32mgranted\033[0m");

        assertTrue(crudService.hasUpdatePermission(1L, context));
        System.out.println("    \033[32m✓\033[0m \033[90mUpdate permission:\033[0m \033[32mgranted\033[0m");

        // 거부되는 권한
        assertFalse(crudService.hasDeletePermission(1L, context));
        System.out.println("    \033[31m✗\033[0m \033[90mDelete permission:\033[0m \033[31mdenied\033[0m");

        System.out.println("  \033[2m✨ Verify:\033[0m ADMIN 권한 제한 확인 완료\n");
    }

    @Test
    @DisplayName("USER는 읽기 권한만 가지고 변경 권한은 없다")
    void USER_는_읽기_권한만_가지고_변경_권한은_없다() {
        System.out.println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mUSER Read-Only Permissions\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m USER 권한의 CRUD 컨텍스트 생성");
        CrudContext context = createTestContext(UserRoleType.USER);
        System.out.println("    \033[90m→\033[0m UserRole: \033[36m" + context.getUserRole() + "\033[0m");

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m USER 읽기 전용 권한 검증");

        // 읽기 권한만 허용
        assertTrue(crudService.hasListPermission(context));
        System.out.println("    \033[32m✓\033[0m \033[90mList permission:\033[0m \033[32mgranted\033[0m");

        assertTrue(crudService.hasReadPermission(1L, context));
        System.out.println("    \033[32m✓\033[0m \033[90mRead permission:\033[0m \033[32mgranted\033[0m");

        // 변경 권한은 모두 거부
        assertFalse(crudService.hasCreatePermission(context));
        System.out.println("    \033[31m✗\033[0m \033[90mCreate permission:\033[0m \033[31mdenied\033[0m");

        assertFalse(crudService.hasUpdatePermission(1L, context));
        System.out.println("    \033[31m✗\033[0m \033[90mUpdate permission:\033[0m \033[31mdenied\033[0m");

        assertFalse(crudService.hasDeletePermission(1L, context));
        System.out.println("    \033[31m✗\033[0m \033[90mDelete permission:\033[0m \033[31mdenied\033[0m");

        System.out.println("  \033[2m✨ Verify:\033[0m USER 읽기 전용 권한 확인 완료\n");
    }

    @Test
    @DisplayName("권한이 없는 사용자는 모든 작업이 거부된다")
    void 권한이_없는_사용자는_모든_작업이_거부된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mUnauthorized User All Permissions Denied\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 권한이 없는 CRUD 컨텍스트 생성");
        CrudContext context = createTestContext(null); // null 역할
        System.out.println("    \033[90m→\033[0m UserRole: \033[33mnull\033[0m (권한 없음)");

        // When & Then
        System.out.println("  \033[2m⚡ Action:\033[0m 모든 권한 거부 검증");

        assertFalse(crudService.hasListPermission(context));
        System.out.println("    \033[31m✗\033[0m \033[90mList permission:\033[0m \033[31mdenied\033[0m");

        assertFalse(crudService.hasReadPermission(1L, context));
        System.out.println("    \033[31m✗\033[0m \033[90mRead permission:\033[0m \033[31mdenied\033[0m");

        assertFalse(crudService.hasCreatePermission(context));
        System.out.println("    \033[31m✗\033[0m \033[90mCreate permission:\033[0m \033[31mdenied\033[0m");

        assertFalse(crudService.hasUpdatePermission(1L, context));
        System.out.println("    \033[31m✗\033[0m \033[90mUpdate permission:\033[0m \033[31mdenied\033[0m");

        assertFalse(crudService.hasDeletePermission(1L, context));
        System.out.println("    \033[31m✗\033[0m \033[90mDelete permission:\033[0m \033[31mdenied\033[0m");

        System.out.println("  \033[2m✨ Verify:\033[0m 미인증 사용자 전체 권한 거부 확인 완료\n");
    }

    @Test
    @DisplayName("클라이언트 IP 주소를 올바른 우선순위로 추출한다")
    void 클라이언트_IP_주소를_올바른_우선순위로_추출한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mClient IP Address Extraction Priority\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 다양한 IP 헤더 설정");

        System.out.println("  \033[2m🎯 Scenario 1:\033[0m X-Forwarded-For 우선순위 검증");
        mockRequest.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        mockRequest.addHeader("X-Real-IP", "192.168.1.50");
        mockRequest.setRemoteAddr("127.0.0.1");

        // When
        CrudContext context1 = crudService.createCrudContext(mockRequest, "cms", "test", "content");

        // Then
        System.out.println(
                "    \033[90m→\033[0m X-Forwarded-For: \033[36m203.0.113.195, 70.41.3.18, 150.172.238.178\033[0m");
        System.out.println("    \033[90m→\033[0m X-Real-IP: \033[36m192.168.1.50\033[0m");
        System.out.println("    \033[90m→\033[0m RemoteAddr: \033[36m127.0.0.1\033[0m");
        assertEquals("203.0.113.195", context1.getClientIp());
        System.out.println(
                "    \033[32m✓\033[0m \033[90mSelected IP:\033[0m \033[32m203.0.113.195\033[0m (X-Forwarded-For 첫 번째)\n");

        System.out.println("  \033[2m🎯 Scenario 2:\033[0m X-Real-IP 폴백 검증");
        MockHttpServletRequest mockRequest2 = new MockHttpServletRequest();
        mockRequest2.addHeader("X-Real-IP", "192.168.1.50");
        mockRequest2.setRemoteAddr("127.0.0.1");

        CrudContext context2 = crudService.createCrudContext(mockRequest2, "cms", "test", "content");
        assertEquals("192.168.1.50", context2.getClientIp());
        System.out.println("    \033[32m✓\033[0m \033[90mFallback to X-Real-IP:\033[0m \033[32m192.168.1.50\033[0m\n");

        System.out.println("  \033[2m🎯 Scenario 3:\033[0m RemoteAddr 최종 폴백 검증");
        MockHttpServletRequest mockRequest3 = new MockHttpServletRequest();
        mockRequest3.setRemoteAddr("127.0.0.1");

        CrudContext context3 = crudService.createCrudContext(mockRequest3, "cms", "test", "content");
        assertEquals("127.0.0.1", context3.getClientIp());
        System.out.println(
                "    \033[32m✓\033[0m \033[90mFinal fallback to RemoteAddr:\033[0m \033[32m127.0.0.1\033[0m\n");
    }

    /**
     * 테스트용 CRUD 컨텍스트 생성 헬퍼
     */
    private CrudContext createTestContext(UserRoleType role) {
        return CrudContext.builder()
                .serviceType("cms")
                .serviceId("douzone")
                .resourceName("content")
                .userUuid("test-uuid")
                .username("testuser")
                .userRole(role)
                .clientIp("127.0.0.1")
                .userAgent("Test-Agent")
                .requestTime("2024-01-01T12:00:00")
                .build();
    }

    /**
     * 테스트용 AbstractCommonCrudServiceImpl 구현체
     */
    private static class TestCrudService extends AbstractCommonCrudServiceImpl<Object, Long, Object, Object, Object> {

        @Override
        public Page<Object> getList(Pageable pageable, CrudContext context) {
            return null; // 테스트에서는 사용하지 않음
        }

        @Override
        public List<Object> getAllList(CrudContext context) {
            return null; // 테스트에서는 사용하지 않음
        }

        @Override
        public Object getById(Long id, CrudContext context) {
            return null; // 테스트에서는 사용하지 않음
        }

        @Override
        public Object create(Object request, CrudContext context) {
            return null; // 테스트에서는 사용하지 않음
        }

        @Override
        public Object update(Long id, Object request, CrudContext context) {
            return null; // 테스트에서는 사용하지 않음
        }

        @Override
        public void delete(Long id, CrudContext context) {
            // 테스트에서는 사용하지 않음
        }
    }
}
