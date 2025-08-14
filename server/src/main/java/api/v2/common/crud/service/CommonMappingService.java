package api.v2.common.crud.service;

import api.v2.common.crud.dto.CrudContext;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 공통 매핑 서비스 인터페이스
 * 
 * Entity-DTO 변환, 응답 변환 등의 공통 매핑 기능을 정의
 * ModelMapper 기반으로 표준화된 변환 로직 제공
 * 
 * @param <Entity>        엔티티 타입
 * @param <ResponseDto>   응답 DTO 타입
 * @param <CreateRequest> 생성 요청 DTO 타입
 * @param <UpdateRequest> 수정 요청 DTO 타입
 */
public interface CommonMappingService<Entity, ResponseDto, CreateRequest, UpdateRequest> {

    /**
     * 엔티티를 응답 DTO로 변환
     * 
     * @param entity  변환할 엔티티
     * @param context CRUD 컨텍스트
     * @return 응답 DTO
     */
    ResponseDto toResponseDto(Entity entity, CrudContext context);

    /**
     * 엔티티 목록을 응답 DTO 목록으로 변환
     * 
     * @param entities 변환할 엔티티 목록
     * @param context  CRUD 컨텍스트
     * @return 응답 DTO 목록
     */
    List<ResponseDto> toResponseDtoList(List<Entity> entities, CrudContext context);

    /**
     * 페이징된 엔티티를 페이징된 응답 DTO로 변환
     * 
     * @param entityPage 변환할 엔티티 페이지
     * @param context    CRUD 컨텍스트
     * @return 응답 DTO 페이지
     */
    Page<ResponseDto> toResponseDtoPage(Page<Entity> entityPage, CrudContext context);

    /**
     * 생성 요청 DTO를 엔티티로 변환
     * 
     * @param createRequest 생성 요청 DTO
     * @param context       CRUD 컨텍스트
     * @return 엔티티
     */
    Entity toEntity(CreateRequest createRequest, CrudContext context);

    /**
     * 수정 요청 DTO를 기존 엔티티에 적용
     * 
     * @param updateRequest  수정 요청 DTO
     * @param existingEntity 기존 엔티티
     * @param context        CRUD 컨텍스트
     * @return 수정된 엔티티
     */
    Entity updateEntity(UpdateRequest updateRequest, Entity existingEntity, CrudContext context);

    /**
     * 부분 업데이트 (null이 아닌 필드만 적용)
     * 
     * @param updateRequest  수정 요청 DTO
     * @param existingEntity 기존 엔티티
     * @param context        CRUD 컨텍스트
     * @return 수정된 엔티티
     */
    Entity partialUpdateEntity(UpdateRequest updateRequest, Entity existingEntity, CrudContext context);

    /**
     * 감사 필드 설정 (생성자, 생성일시, 수정자, 수정일시 등)
     * 
     * @param entity  대상 엔티티
     * @param context CRUD 컨텍스트
     * @param isNew   새로운 엔티티 여부
     */
    void setAuditFields(Entity entity, CrudContext context, boolean isNew);

    /**
     * 권한 기반 필드 마스킹
     * 
     * @param responseDto 응답 DTO
     * @param context     CRUD 컨텍스트
     * @return 마스킹된 응답 DTO
     */
    ResponseDto maskSensitiveFields(ResponseDto responseDto, CrudContext context);

    /**
     * 유효성 검증과 함께 변환
     * 
     * @param createRequest 생성 요청 DTO
     * @param context       CRUD 컨텍스트
     * @return 검증된 엔티티
     */
    Entity toValidatedEntity(CreateRequest createRequest, CrudContext context);

    /**
     * 커스텀 변환 규칙 적용
     * 
     * @param source      원본 객체
     * @param targetClass 대상 클래스
     * @param context     CRUD 컨텍스트
     * @param <T>         대상 타입
     * @return 변환된 객체
     */
    <T> T mapWithCustomRules(Object source, Class<T> targetClass, CrudContext context);

    /**
     * 연관 엔티티 포함 변환
     * 
     * @param entity      변환할 엔티티
     * @param context     CRUD 컨텍스트
     * @param includeRefs 포함할 연관 엔티티 이름 목록
     * @return 연관 엔티티 포함된 응답 DTO
     */
    ResponseDto toResponseDtoWithReferences(Entity entity, CrudContext context, String... includeRefs);

    /**
     * 변환 메타데이터 생성
     * 
     * @param source  원본 객체
     * @param target  대상 객체
     * @param context CRUD 컨텍스트
     * @return 변환 메타데이터
     */
    MappingMetadata createMappingMetadata(Object source, Object target, CrudContext context);

    /**
     * 변환 메타데이터
     */
    interface MappingMetadata {
        String getSourceType();

        String getTargetType();

        long getMappingTimeMs();

        String getMappedBy();

        String getMappingStrategy();
    }
}
