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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * CommonMappingService 단위 테스트
 * 
 * 공통 매핑 서비스의 핵심 기능을 검증:
 * - Entity ↔ DTO 변환 로직
 * - 권한 기반 필드 마스킹
 * - 커스텀 매핑 규칙 적용
 * - 매핑 메타데이터 생성
 * - 비즈니스 규칙 검증
 */
@ExtendWith(MockitoExtension.class)
class CommonMappingServiceTest {

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
    private CrudContext adminContext;
    private CrudContext userContext;

    @BeforeEach
    void setUp() {
        mappingService = new TestMappingService(modelMapper);

        adminContext = CrudContext.builder()
                .serviceType("cms")
                .serviceId("douzone")
                .resourceName("content")
                .userUuid("admin-uuid")
                .username("admin")
                .userRole(UserRoleType.ADMIN)
                .clientIp("127.0.0.1")
                .build();

        userContext = CrudContext.builder()
                .serviceType("cms")
                .serviceId("douzone")
                .resourceName("content")
                .userUuid("user-uuid")
                .username("user")
                .userRole(UserRoleType.USER)
                .clientIp("127.0.0.1")
                .build();
    }

    @AfterEach
    void tearDown() {
        mappingService = null;
        adminContext = null;
        userContext = null;
    }

    @Test
    @DisplayName("엔티티를 응답 DTO로 정상 변환한다")
    void 엔티티를_응답_DTO로_정상_변환한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mEntity to ResponseDto Conversion\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 테스트용 엔티티와 DTO 준비");
        TestEntity entity = new TestEntity("test-id", "Test Content", "sensitive-data");
        TestResponseDto expectedDto = new TestResponseDto("test-id", "Test Content", null);

        when(modelMapper.map(entity, TestResponseDto.class)).thenReturn(expectedDto);
        System.out.println("    \033[90m→\033[0m Entity: \033[36m" + entity.getName() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Expected DTO: \033[36m" + expectedDto.getName() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m toResponseDto() 호출");
        TestResponseDto result = mappingService.toResponseDto(entity, adminContext);
        System.out.println("    \033[90m→\033[0m toResponseDto(\033[36mentity\033[0m, \033[36madminContext\033[0m)");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 변환 결과 검증");
        assertNotNull(result);
        assertEquals("test-id", result.getId());
        assertEquals("Test Content", result.getName());
        System.out.println("    \033[32m✓\033[0m \033[90mConversion successful:\033[0m \033[32m" + result.getId() + "/"
                + result.getName() + "\033[0m\n");
    }

    @Test
    @DisplayName("null 엔티티는 null을 반환한다")
    void null_엔티티는_null을_반환한다() {
        System.out.println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mNull Entity Returns Null\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m null 엔티티 준비");
        TestEntity entity = null;
        System.out.println("    \033[90m→\033[0m Entity: \033[33mnull\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m null 엔티티로 변환 시도");
        TestResponseDto result = mappingService.toResponseDto(entity, adminContext);
        System.out.println("    \033[90m→\033[0m toResponseDto(\033[33mnull\033[0m, \033[36madminContext\033[0m)");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m null 반환 확인");
        assertNull(result);
        System.out.println("    \033[32m✓\033[0m \033[90mNull handling correct:\033[0m \033[33mnull\033[0m\n");
    }

    @Test
    @DisplayName("USER 권한은 민감한 필드가 마스킹된다")
    void USER_권한은_민감한_필드가_마스킹된다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mUSER Role Sensitive Field Masking\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 민감한 데이터가 포함된 엔티티 준비");
        TestEntity entity = new TestEntity("test-id", "Test Content", "sensitive-data");
        TestResponseDto dtoBeforeMasking = new TestResponseDto("test-id", "Test Content", "sensitive-data");

        when(modelMapper.map(entity, TestResponseDto.class)).thenReturn(dtoBeforeMasking);
        System.out.println(
                "    \033[90m→\033[0m Entity with sensitive data: \033[36m" + entity.getSensitiveField() + "\033[0m");
        System.out.println("    \033[90m→\033[0m User role: \033[36m" + userContext.getUserRole() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m USER 권한으로 변환");
        TestResponseDto result = mappingService.toResponseDto(entity, userContext);
        System.out.println("    \033[90m→\033[0m toResponseDto(\033[36mentity\033[0m, \033[36muserContext\033[0m)");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 민감한 필드 마스킹 확인");
        assertNotNull(result);
        assertEquals("test-id", result.getId());
        assertEquals("Test Content", result.getName());
        assertEquals("***MASKED***", result.getSensitiveField()); // USER는 마스킹됨
        System.out.println("    \033[32m✓\033[0m \033[90mSensitive field masked:\033[0m \033[32m"
                + result.getSensitiveField() + "\033[0m\n");
    }

    @Test
    @DisplayName("ADMIN 권한은 민감한 필드를 볼 수 있다")
    void ADMIN_권한은_민감한_필드를_볼_수_있다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mADMIN Role Can View Sensitive Fields\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 민감한 데이터가 포함된 엔티티 준비");
        TestEntity entity = new TestEntity("test-id", "Test Content", "sensitive-data");
        TestResponseDto dtoWithoutMasking = new TestResponseDto("test-id", "Test Content", "sensitive-data");

        when(modelMapper.map(entity, TestResponseDto.class)).thenReturn(dtoWithoutMasking);
        System.out.println(
                "    \033[90m→\033[0m Entity with sensitive data: \033[36m" + entity.getSensitiveField() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Admin role: \033[35m" + adminContext.getUserRole() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m ADMIN 권한으로 변환");
        TestResponseDto result = mappingService.toResponseDto(entity, adminContext);
        System.out.println("    \033[90m→\033[0m toResponseDto(\033[36mentity\033[0m, \033[35madminContext\033[0m)");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 민감한 필드 접근 가능 확인");
        assertNotNull(result);
        assertEquals("test-id", result.getId());
        assertEquals("Test Content", result.getName());
        assertEquals("sensitive-data", result.getSensitiveField()); // ADMIN은 원본 데이터 접근 가능
        System.out.println("    \033[32m✓\033[0m \033[90mSensitive field accessible:\033[0m \033[32m"
                + result.getSensitiveField() + "\033[0m\n");
    }

    @Test
    @DisplayName("엔티티 목록을 DTO 목록으로 변환한다")
    void 엔티티_목록을_DTO_목록으로_변환한다() {
        System.out.println(
                "\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mEntity List to DTO List Conversion\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 여러 엔티티를 포함한 목록 준비");
        List<TestEntity> entities = Arrays.asList(
                new TestEntity("1", "Content 1", "data1"),
                new TestEntity("2", "Content 2", "data2"));

        when(modelMapper.map(any(TestEntity.class), eq(TestResponseDto.class)))
                .thenAnswer(invocation -> {
                    TestEntity entity = invocation.getArgument(0);
                    return new TestResponseDto(entity.getId(), entity.getName(), entity.getSensitiveField());
                });

        System.out.println("    \033[90m→\033[0m Entity count: \033[36m" + entities.size() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 목록 변환 실행");
        List<TestResponseDto> result = mappingService.toResponseDtoList(entities, adminContext);
        System.out.println("    \033[90m→\033[0m toResponseDtoList(\033[36m" + entities.size()
                + " entities\033[0m, \033[35madminContext\033[0m)");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 목록 변환 결과 확인");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("Content 1", result.get(0).getName());
        assertEquals("2", result.get(1).getId());
        assertEquals("Content 2", result.get(1).getName());
        System.out.println("    \033[32m✓\033[0m \033[90mList conversion successful:\033[0m \033[32m" + result.size()
                + " items\033[0m\n");
    }

    @Test
    @DisplayName("매핑 메타데이터를 올바르게 생성한다")
    void 매핑_메타데이터를_올바르게_생성한다() {
        System.out.println("\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mMapping Metadata Creation\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 매핑 소스와 타겟 객체 준비");
        TestEntity source = new TestEntity("test-id", "Test Content", "data");
        TestResponseDto target = new TestResponseDto("test-id", "Test Content", "data");
        System.out
                .println("    \033[90m→\033[0m Source type: \033[36m" + source.getClass().getSimpleName() + "\033[0m");
        System.out
                .println("    \033[90m→\033[0m Target type: \033[36m" + target.getClass().getSimpleName() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Mapped by: \033[35m" + adminContext.getUsername() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 매핑 메타데이터 생성");
        CommonMappingService.MappingMetadata metadata = mappingService.createMappingMetadata(source, target,
                adminContext);
        System.out.println(
                "    \033[90m→\033[0m createMappingMetadata(\033[36msource\033[0m, \033[36mtarget\033[0m, \033[35madminContext\033[0m)");

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 메타데이터 내용 확인");
        assertNotNull(metadata);
        assertEquals("TestEntity", metadata.getSourceType());
        assertEquals("TestResponseDto", metadata.getTargetType());
        assertEquals("admin", metadata.getMappedBy());
        assertEquals("MODEL_MAPPER", metadata.getMappingStrategy());
        assertTrue(metadata.getMappingTimeMs() > 0);
        System.out.println("    \033[32m✓\033[0m \033[90mMetadata created:\033[0m \033[32m" + metadata.getSourceType()
                + " → " + metadata.getTargetType() + "\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mMapping time:\033[0m \033[32m" + metadata.getMappingTimeMs()
                + "ms\033[0m\n");
    }

    /**
     * 테스트용 엔티티
     */
    public static class TestEntity {
        private String id;
        private String name;
        private String sensitiveField;

        public TestEntity(String id, String name, String sensitiveField) {
            this.id = id;
            this.name = name;
            this.sensitiveField = sensitiveField;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSensitiveField() {
            return sensitiveField;
        }
    }

    /**
     * 테스트용 응답 DTO
     */
    public static class TestResponseDto {
        private String id;
        private String name;
        private String sensitiveField;

        public TestResponseDto(String id, String name, String sensitiveField) {
            this.id = id;
            this.name = name;
            this.sensitiveField = sensitiveField;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSensitiveField() {
            return sensitiveField;
        }

        public void setSensitiveField(String sensitiveField) {
            this.sensitiveField = sensitiveField;
        }
    }

    /**
     * 테스트용 생성 요청 DTO
     */
    public static class TestCreateRequest {
        private String name;
        private String sensitiveField;

        public TestCreateRequest(String name, String sensitiveField) {
            this.name = name;
            this.sensitiveField = sensitiveField;
        }

        public String getName() {
            return name;
        }

        public String getSensitiveField() {
            return sensitiveField;
        }
    }

    /**
     * 테스트용 수정 요청 DTO
     */
    public static class TestUpdateRequest {
        private String name;
        private String sensitiveField;

        public TestUpdateRequest(String name, String sensitiveField) {
            this.name = name;
            this.sensitiveField = sensitiveField;
        }

        public String getName() {
            return name;
        }

        public String getSensitiveField() {
            return sensitiveField;
        }
    }

    /**
     * 테스트용 AbstractCommonMappingServiceImpl 구현체
     */
    private static class TestMappingService extends
            AbstractCommonMappingServiceImpl<TestEntity, TestResponseDto, TestCreateRequest, TestUpdateRequest> {

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

        @Override
        public TestResponseDto maskSensitiveFields(TestResponseDto responseDto, CrudContext context) {
            // USER 권한은 민감한 필드 마스킹
            if (context.getUserRole() == UserRoleType.USER) {
                responseDto.setSensitiveField("***MASKED***");
            }
            return responseDto;
        }
    }
}
