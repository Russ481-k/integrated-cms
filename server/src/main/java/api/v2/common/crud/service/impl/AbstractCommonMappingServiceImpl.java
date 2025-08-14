package api.v2.common.crud.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.common.crud.dto.CrudContext;
import api.v2.common.crud.service.CommonMappingService;
import api.v2.cms.user.domain.UserRoleType;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 공통 매핑 서비스 기본 구현체
 * 
 * ModelMapper 기반의 표준화된 변환 로직 제공
 * 구체적인 매핑 로직은 상속받는 클래스에서 구현
 * 
 * @param <Entity>        엔티티 타입
 * @param <ResponseDto>   응답 DTO 타입
 * @param <CreateRequest> 생성 요청 DTO 타입
 * @param <UpdateRequest> 수정 요청 DTO 타입
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommonMappingServiceImpl<Entity, ResponseDto, CreateRequest, UpdateRequest>
        implements CommonMappingService<Entity, ResponseDto, CreateRequest, UpdateRequest> {

    protected final ModelMapper modelMapper;

    /**
     * 응답 DTO 클래스 반환 (구현체에서 정의)
     */
    protected abstract Class<ResponseDto> getResponseDtoClass();

    /**
     * 엔티티 클래스 반환 (구현체에서 정의)
     */
    protected abstract Class<Entity> getEntityClass();

    @Override
    public ResponseDto toResponseDto(Entity entity, CrudContext context) {
        if (entity == null) {
            return null;
        }

        long startTime = System.currentTimeMillis();

        try {
            ResponseDto responseDto = modelMapper.map(entity, getResponseDtoClass());

            // 커스텀 매핑 로직 적용
            responseDto = applyCustomMapping(entity, responseDto, context);

            // 권한 기반 필드 마스킹
            responseDto = maskSensitiveFields(responseDto, context);

            // 매핑 메타데이터 로깅
            long mappingTime = System.currentTimeMillis() - startTime;
            log.debug("Entity-to-DTO mapping completed: {} -> {} in {}ms",
                    entity.getClass().getSimpleName(),
                    getResponseDtoClass().getSimpleName(),
                    mappingTime);

            return responseDto;

        } catch (Exception e) {
            log.error("Failed to map entity to response DTO: {}", e.getMessage(), e);
            throw new RuntimeException("Entity mapping failed", e);
        }
    }

    @Override
    public List<ResponseDto> toResponseDtoList(List<Entity> entities, CrudContext context) {
        if (entities == null || entities.isEmpty()) {
            return java.util.Arrays.asList();
        }

        return entities.stream()
                .map(entity -> toResponseDto(entity, context))
                .collect(Collectors.toList());
    }

    @Override
    public Page<ResponseDto> toResponseDtoPage(Page<Entity> entityPage, CrudContext context) {
        if (entityPage == null) {
            return Page.empty();
        }

        List<ResponseDto> dtoList = toResponseDtoList(entityPage.getContent(), context);
        return new PageImpl<>(dtoList, entityPage.getPageable(), entityPage.getTotalElements());
    }

    @Override
    public Entity toEntity(CreateRequest createRequest, CrudContext context) {
        if (createRequest == null) {
            return null;
        }

        long startTime = System.currentTimeMillis();

        try {
            Entity entity = modelMapper.map(createRequest, getEntityClass());

            // 감사 필드 설정 (생성)
            setAuditFields(entity, context, true);

            // 커스텀 생성 로직 적용
            entity = applyCustomCreationLogic(createRequest, entity, context);

            long mappingTime = System.currentTimeMillis() - startTime;
            log.debug("CreateRequest-to-Entity mapping completed: {} -> {} in {}ms",
                    createRequest.getClass().getSimpleName(),
                    getEntityClass().getSimpleName(),
                    mappingTime);

            return entity;

        } catch (Exception e) {
            log.error("Failed to map create request to entity: {}", e.getMessage(), e);
            throw new RuntimeException("Create request mapping failed", e);
        }
    }

    @Override
    public Entity updateEntity(UpdateRequest updateRequest, Entity existingEntity, CrudContext context) {
        if (updateRequest == null || existingEntity == null) {
            return existingEntity;
        }

        long startTime = System.currentTimeMillis();

        try {
            // UpdateRequest의 필드를 기존 엔티티에 매핑
            modelMapper.map(updateRequest, existingEntity);

            // 감사 필드 설정 (수정)
            setAuditFields(existingEntity, context, false);

            // 커스텀 업데이트 로직 적용
            existingEntity = applyCustomUpdateLogic(updateRequest, existingEntity, context);

            long mappingTime = System.currentTimeMillis() - startTime;
            log.debug("UpdateRequest-to-Entity mapping completed: {} -> {} in {}ms",
                    updateRequest.getClass().getSimpleName(),
                    getEntityClass().getSimpleName(),
                    mappingTime);

            return existingEntity;

        } catch (Exception e) {
            log.error("Failed to map update request to entity: {}", e.getMessage(), e);
            throw new RuntimeException("Update request mapping failed", e);
        }
    }

    @Override
    public Entity partialUpdateEntity(UpdateRequest updateRequest, Entity existingEntity, CrudContext context) {
        if (updateRequest == null || existingEntity == null) {
            return existingEntity;
        }

        // 부분 업데이트 로직 (null이 아닌 필드만 적용)
        // 구현체에서 필요에 따라 오버라이드
        return updateEntity(updateRequest, existingEntity, context);
    }

    @Override
    public void setAuditFields(Entity entity, CrudContext context, boolean isNew) {
        // 리플렉션을 사용한 감사 필드 설정
        try {
            Class<?> entityClass = entity.getClass();

            if (isNew) {
                // 생성 필드 설정
                setFieldIfExists(entity, entityClass, "createdBy", context.getUsername());
                setFieldIfExists(entity, entityClass, "createdAt", LocalDateTime.now());
                setFieldIfExists(entity, entityClass, "createdIp", context.getClientIp());
            }

            // 수정 필드 설정 (생성 시에도 설정)
            setFieldIfExists(entity, entityClass, "updatedBy", context.getUsername());
            setFieldIfExists(entity, entityClass, "updatedAt", LocalDateTime.now());
            setFieldIfExists(entity, entityClass, "updatedIp", context.getClientIp());

        } catch (Exception e) {
            log.warn("Failed to set audit fields for entity: {}", e.getMessage());
        }
    }

    @Override
    public ResponseDto maskSensitiveFields(ResponseDto responseDto, CrudContext context) {
        // 기본적으로 관리자가 아니면 민감한 필드 마스킹
        if (context.getUserRole() != null &&
                (context.getUserRole() == UserRoleType.SUPER_ADMIN ||
                        context.getUserRole() == UserRoleType.SERVICE_ADMIN)) {
            return responseDto; // 관리자는 모든 필드 볼 수 있음
        }

        // 구현체에서 필요에 따라 오버라이드하여 민감한 필드 마스킹
        return applyFieldMasking(responseDto, context);
    }

    @Override
    public Entity toValidatedEntity(CreateRequest createRequest, CrudContext context) {
        // 기본 변환 후 검증 로직 적용
        Entity entity = toEntity(createRequest, context);

        // 비즈니스 규칙 검증
        validateBusinessRules(entity, context, true);

        return entity;
    }

    @Override
    public <T> T mapWithCustomRules(Object source, Class<T> targetClass, CrudContext context) {
        if (source == null) {
            return null;
        }

        T target = modelMapper.map(source, targetClass);

        // 커스텀 규칙 적용 (구현체에서 확장)
        return applyCustomMappingRules(source, target, context);
    }

    @Override
    public ResponseDto toResponseDtoWithReferences(Entity entity, CrudContext context, String... includeRefs) {
        ResponseDto responseDto = toResponseDto(entity, context);

        // 연관 엔티티 포함 로직 (구현체에서 확장)
        return includeReferences(entity, responseDto, context, includeRefs);
    }

    @Override
    public MappingMetadata createMappingMetadata(Object source, Object target, CrudContext context) {
        // 기본적으로 0ms로 설정 (실제 매핑 시간 측정이 필요한 경우 오버로드된 메서드 사용)
        return createMappingMetadata(source, target, context, 0L);
    }

    /**
     * 실제 매핑 시간과 함께 메타데이터 생성 (내부 사용)
     */
    protected MappingMetadata createMappingMetadata(Object source, Object target, CrudContext context,
            long actualMappingTimeMs) {
        return new DefaultMappingMetadata(
                source.getClass().getSimpleName(),
                target.getClass().getSimpleName(),
                actualMappingTimeMs,
                context.getUsername(),
                "MODEL_MAPPER");
    }

    // == 확장 가능한 메서드들 (구현체에서 오버라이드) ==

    /**
     * 커스텀 매핑 로직 적용
     */
    protected ResponseDto applyCustomMapping(Entity entity, ResponseDto responseDto, CrudContext context) {
        return responseDto;
    }

    /**
     * 커스텀 생성 로직 적용
     */
    protected Entity applyCustomCreationLogic(CreateRequest createRequest, Entity entity, CrudContext context) {
        return entity;
    }

    /**
     * 커스텀 업데이트 로직 적용
     */
    protected Entity applyCustomUpdateLogic(UpdateRequest updateRequest, Entity entity, CrudContext context) {
        return entity;
    }

    /**
     * 필드 마스킹 적용
     */
    protected ResponseDto applyFieldMasking(ResponseDto responseDto, CrudContext context) {
        return responseDto;
    }

    /**
     * 비즈니스 규칙 검증
     */
    protected void validateBusinessRules(Entity entity, CrudContext context, boolean isNew) {
        // 구현체에서 필요에 따라 구현
    }

    /**
     * 커스텀 매핑 규칙 적용
     */
    protected <T> T applyCustomMappingRules(Object source, T target, CrudContext context) {
        return target;
    }

    /**
     * 연관 엔티티 포함
     */
    protected ResponseDto includeReferences(Entity entity, ResponseDto responseDto, CrudContext context,
            String... includeRefs) {
        return responseDto;
    }

    // == 유틸리티 메서드들 ==

    /**
     * 리플렉션을 사용하여 필드 값 설정
     */
    private void setFieldIfExists(Entity entity, Class<?> entityClass, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = entityClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 필드가 없거나 접근할 수 없으면 무시
        }
    }

    /**
     * 기본 매핑 메타데이터 구현
     */
    private static class DefaultMappingMetadata implements MappingMetadata {
        private final String sourceType;
        private final String targetType;
        private final long mappingTimeMs;
        private final String mappedBy;
        private final String mappingStrategy;

        public DefaultMappingMetadata(String sourceType, String targetType, long mappingTimeMs, String mappedBy,
                String mappingStrategy) {
            this.sourceType = sourceType;
            this.targetType = targetType;
            this.mappingTimeMs = mappingTimeMs;
            this.mappedBy = mappedBy;
            this.mappingStrategy = mappingStrategy;
        }

        @Override
        public String getSourceType() {
            return sourceType;
        }

        @Override
        public String getTargetType() {
            return targetType;
        }

        @Override
        public long getMappingTimeMs() {
            return mappingTimeMs;
        }

        @Override
        public String getMappedBy() {
            return mappedBy;
        }

        @Override
        public String getMappingStrategy() {
            return mappingStrategy;
        }
    }
}
