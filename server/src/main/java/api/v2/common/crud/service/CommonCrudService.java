package api.v2.common.crud.service;

import api.v2.common.crud.dto.CrudContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 공통 CRUD 서비스 인터페이스
 * CMS와 integrated_cms에서 공통으로 사용되는 CRUD 기능을 정의
 * 
 * @param <T>             엔티티 타입
 * @param <ID>            엔티티 ID 타입
 * @param <CreateRequest> 생성 요청 DTO 타입
 * @param <UpdateRequest> 수정 요청 DTO 타입
 * @param <ResponseDto>   응답 DTO 타입
 */
public interface CommonCrudService<T, ID, CreateRequest, UpdateRequest, ResponseDto> {

    /**
     * 페이징된 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param context  CRUD 컨텍스트
     * @return 페이징된 응답 DTO 목록
     */
    Page<ResponseDto> getList(Pageable pageable, CrudContext context);

    /**
     * 전체 목록 조회 (페이징 없음)
     * 
     * @param context CRUD 컨텍스트
     * @return 전체 응답 DTO 목록
     */
    List<ResponseDto> getAllList(CrudContext context);

    /**
     * ID로 단일 조회
     * 
     * @param id      조회할 엔티티 ID
     * @param context CRUD 컨텍스트
     * @return 응답 DTO
     */
    ResponseDto getById(ID id, CrudContext context);

    /**
     * 새로운 엔티티 생성
     * 
     * @param request 생성 요청 DTO
     * @param context CRUD 컨텍스트
     * @return 생성된 엔티티의 응답 DTO
     */
    ResponseDto create(CreateRequest request, CrudContext context);

    /**
     * 기존 엔티티 수정
     * 
     * @param id      수정할 엔티티 ID
     * @param request 수정 요청 DTO
     * @param context CRUD 컨텍스트
     * @return 수정된 엔티티의 응답 DTO
     */
    ResponseDto update(ID id, UpdateRequest request, CrudContext context);

    /**
     * 엔티티 삭제
     * 
     * @param id      삭제할 엔티티 ID
     * @param context CRUD 컨텍스트
     */
    void delete(ID id, CrudContext context);

    /**
     * CRUD 컨텍스트 생성
     * HTTP 요청에서 CRUD 컨텍스트 정보를 추출하여 생성
     * 
     * @param request      HTTP 요청
     * @param serviceType  서비스 타입 (cms | integrated_cms)
     * @param serviceId    서비스 ID (cms 타입에서만 사용)
     * @param resourceName 리소스 이름
     * @return CRUD 컨텍스트
     */
    CrudContext createCrudContext(HttpServletRequest request, String serviceType, String serviceId,
            String resourceName);

    /**
     * 권한 확인 - 목록 조회 권한
     * 
     * @param context CRUD 컨텍스트
     * @return 권한 여부
     */
    boolean hasListPermission(CrudContext context);

    /**
     * 권한 확인 - 읽기 권한
     * 
     * @param id      엔티티 ID
     * @param context CRUD 컨텍스트
     * @return 권한 여부
     */
    boolean hasReadPermission(ID id, CrudContext context);

    /**
     * 권한 확인 - 생성 권한
     * 
     * @param context CRUD 컨텍스트
     * @return 권한 여부
     */
    boolean hasCreatePermission(CrudContext context);

    /**
     * 권한 확인 - 수정 권한
     * 
     * @param id      엔티티 ID
     * @param context CRUD 컨텍스트
     * @return 권한 여부
     */
    boolean hasUpdatePermission(ID id, CrudContext context);

    /**
     * 권한 확인 - 삭제 권한
     * 
     * @param id      엔티티 ID
     * @param context CRUD 컨텍스트
     * @return 권한 여부
     */
    boolean hasDeletePermission(ID id, CrudContext context);
}
