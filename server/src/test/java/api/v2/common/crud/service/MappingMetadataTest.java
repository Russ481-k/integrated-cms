package api.v2.common.crud.service;

import api.v2.common.crud.dto.CrudContext;
import api.v2.common.crud.service.impl.AbstractCommonMappingServiceImpl;
import api.v2.cms.user.domain.UserRoleType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MappingMetadata 단위 테스트
 * 
 * 매핑 메타데이터 생성 로직의 버그를 TDD로 수정:
 * - 현재 시간이 아닌 실제 매핑 시간 기록
 * - 매핑 시간이 합리적인 범위인지 검증
 * - 메타데이터 필드들의 정확성 검증
 */
@ExtendWith(MockitoExtension.class)
class MappingMetadataTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    @Mock
    private ModelMapper modelMapper;

    private TestMappingService mappingService;
    private CrudContext context;

    @BeforeEach
    void setUp() {
        mappingService = new TestMappingService(modelMapper);

        context = CrudContext.builder()
                .serviceType("cms")
                .serviceId("douzone")
                .resourceName("content")
                .userUuid("admin-uuid")
                .username("admin")
                .userRole(UserRoleType.ADMIN)
                .clientIp("127.0.0.1")
                .build();
    }

    @AfterEach
    void tearDown() {
        mappingService = null;
        context = null;
    }

    @Test
    @DisplayName("매핑 시간이 합리적인 범위 내에 있다 (1초 미만)")
    void 매핑_시간이_합리적인_범위_내에_있다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mMapping Time Should Be Reasonable Range\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 매핑할 소스와 타겟 객체 준비");
        TestEntity source = new TestEntity("test-id", "Test Content");
        TestResponseDto target = new TestResponseDto("test-id", "Test Content");
        System.out.println("    \033[90m→\033[0m Source: \033[36m" + source.getClass().getSimpleName() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Target: \033[36m" + target.getClass().getSimpleName() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 매핑 메타데이터 생성 및 시간 측정");
        long startTime = System.currentTimeMillis();
        CommonMappingService.MappingMetadata metadata = mappingService.createMappingMetadata(source, target, context);
        long endTime = System.currentTimeMillis();
        long actualElapsed = endTime - startTime;

        System.out.println("    \033[90m→\033[0m createMappingMetadata() 실행");
        System.out.println("    \033[90m→\033[0m 실제 경과 시간: \033[36m" + actualElapsed + "ms\033[0m");
        System.out.println("    \033[90m→\033[0m 메타데이터 기록 시간: \033[36m" + metadata.getMappingTimeMs() + "ms\033[0m");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 매핑 시간이 합리적인 범위인지 확인");
        assertNotNull(metadata);

        // 매핑 시간이 1초(1000ms) 미만이어야 함
        assertTrue(metadata.getMappingTimeMs() < 1000,
                "매핑 시간이 1초를 초과합니다: " + metadata.getMappingTimeMs() + "ms");

        // 매핑 시간이 0보다 커야 함 (최소한의 처리 시간)
        assertTrue(metadata.getMappingTimeMs() >= 0,
                "매핑 시간이 음수입니다: " + metadata.getMappingTimeMs() + "ms");

        // 실제 경과 시간과 비슷해야 함 (±10ms 오차 허용)
        long timeDifference = Math.abs(metadata.getMappingTimeMs() - actualElapsed);
        assertTrue(timeDifference <= 10,
                "기록된 시간과 실제 시간의 차이가 너무 큽니다: " + timeDifference + "ms");

        System.out.println("    \033[32m✓\033[0m \033[90mMapping time is reasonable:\033[0m \033[32m"
                + metadata.getMappingTimeMs() + "ms\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mTime difference acceptable:\033[0m \033[32m" + timeDifference
                + "ms\033[0m\n");
    }

    @Test
    @DisplayName("매핑 메타데이터의 모든 필드가 올바르게 설정된다")
    void 매핑_메타데이터의_모든_필드가_올바르게_설정된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mMapping Metadata All Fields Correct\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 매핑할 소스와 타겟 객체 준비");
        TestEntity source = new TestEntity("test-id", "Test Content");
        TestResponseDto target = new TestResponseDto("test-id", "Test Content");
        String expectedUsername = context.getUsername();
        System.out.println("    \033[90m→\033[0m Expected username: \033[35m" + expectedUsername + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 매핑 메타데이터 생성");
        CommonMappingService.MappingMetadata metadata = mappingService.createMappingMetadata(source, target, context);
        System.out.println("    \033[90m→\033[0m createMappingMetadata() 실행 완료");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 모든 메타데이터 필드 검증");
        assertNotNull(metadata);

        assertEquals("TestEntity", metadata.getSourceType());
        System.out.println(
                "    \033[32m✓\033[0m \033[90mSource type:\033[0m \033[32m" + metadata.getSourceType() + "\033[0m");

        assertEquals("TestResponseDto", metadata.getTargetType());
        System.out.println(
                "    \033[32m✓\033[0m \033[90mTarget type:\033[0m \033[32m" + metadata.getTargetType() + "\033[0m");

        assertEquals(expectedUsername, metadata.getMappedBy());
        System.out.println(
                "    \033[32m✓\033[0m \033[90mMapped by:\033[0m \033[32m" + metadata.getMappedBy() + "\033[0m");

        assertEquals("MODEL_MAPPER", metadata.getMappingStrategy());
        System.out.println("    \033[32m✓\033[0m \033[90mMapping strategy:\033[0m \033[32m"
                + metadata.getMappingStrategy() + "\033[0m");

        assertTrue(metadata.getMappingTimeMs() >= 0);
        System.out.println("    \033[32m✓\033[0m \033[90mMapping time valid:\033[0m \033[32m"
                + metadata.getMappingTimeMs() + "ms\033[0m\n");
    }

    @Test
    @DisplayName("빈 문자열 username이 있는 컨텍스트도 처리된다")
    void 빈_문자열_username이_있는_컨텍스트도_처리된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mEmpty Username Context Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 빈 username이 있는 컨텍스트 준비");
        CrudContext emptyUsernameContext = CrudContext.builder()
                .serviceType("cms")
                .serviceId("douzone")
                .resourceName("content")
                .userUuid("user-uuid")
                .username("") // 빈 문자열
                .userRole(UserRoleType.USER)
                .clientIp("127.0.0.1")
                .build();

        TestEntity source = new TestEntity("test-id", "Test Content");
        TestResponseDto target = new TestResponseDto("test-id", "Test Content");
        System.out.println("    \033[90m→\033[0m Username: \033[33m''\033[0m (빈 문자열)");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 빈 username으로 메타데이터 생성");
        CommonMappingService.MappingMetadata metadata = mappingService.createMappingMetadata(source, target,
                emptyUsernameContext);
        System.out.println("    \033[90m→\033[0m createMappingMetadata() 실행");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 빈 username이 올바르게 처리되는지 확인");
        assertNotNull(metadata);
        assertEquals("", metadata.getMappedBy());
        System.out.println("    \033[32m✓\033[0m \033[90mEmpty username handled:\033[0m \033[33m''\033[0m\n");
    }

    @Test
    @DisplayName("null username이 있는 컨텍스트도 처리된다")
    void null_username이_있는_컨텍스트도_처리된다() {
        System.out
                .println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mNull Username Context Handling\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m null username이 있는 컨텍스트 준비");
        CrudContext nullUsernameContext = CrudContext.builder()
                .serviceType("cms")
                .serviceId("douzone")
                .resourceName("content")
                .userUuid("user-uuid")
                .username(null) // null
                .userRole(UserRoleType.USER)
                .clientIp("127.0.0.1")
                .build();

        TestEntity source = new TestEntity("test-id", "Test Content");
        TestResponseDto target = new TestResponseDto("test-id", "Test Content");
        System.out.println("    \033[90m→\033[0m Username: \033[33mnull\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m null username으로 메타데이터 생성");
        CommonMappingService.MappingMetadata metadata = mappingService.createMappingMetadata(source, target,
                nullUsernameContext);
        System.out.println("    \033[90m→\033[0m createMappingMetadata() 실행");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m null username이 올바르게 처리되는지 확인");
        assertNotNull(metadata);
        assertNull(metadata.getMappedBy());
        System.out.println("    \033[32m✓\033[0m \033[90mNull username handled:\033[0m \033[33mnull\033[0m\n");
    }

    /**
     * 테스트용 엔티티
     */
    public static class TestEntity {
        private String id;
        private String name;

        public TestEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 테스트용 응답 DTO
     */
    public static class TestResponseDto {
        private String id;
        private String name;

        public TestResponseDto(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 테스트용 AbstractCommonMappingServiceImpl 구현체
     */
    private static class TestMappingService
            extends AbstractCommonMappingServiceImpl<TestEntity, TestResponseDto, Object, Object> {

        public TestMappingService(ModelMapper modelMapper) {
            super(modelMapper);
        }

        @Override
        protected Class<TestResponseDto> getResponseDtoClass() {
            return TestResponseDto.class;
        }

        @Override
        protected Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }
    }
}
