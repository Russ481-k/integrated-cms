package api.v2.common.crud.service;

import api.v2.common.crud.dto.CrudContext;
import api.v2.common.crud.dto.CrudPermissionResponse;
import api.v2.cms.user.domain.UserRoleType;

/**
 * 공통 권한 검증 서비스 인터페이스
 * 
 * CMS와 integrated_cms에서 공통으로 사용되는 권한 검증 기능을 정의
 * 기존 개별 권한 체커들을 통합하여 일관된 권한 검증 제공
 */
public interface CommonPermissionService {

    /**
     * CRUD 작업에 대한 권한 확인
     * 
     * @param context    CRUD 컨텍스트
     * @param operation  작업 타입 (READ, CREATE, UPDATE, DELETE)
     * @param resourceId 리소스 ID (선택사항)
     * @return 권한 확인 결과
     */
    CrudPermissionResponse checkCrudPermission(CrudContext context, String operation, String resourceId);

    /**
     * 리소스 목록 조회 권한 확인
     * 
     * @param context CRUD 컨텍스트
     * @return 권한 여부
     */
    boolean hasListPermission(CrudContext context);

    /**
     * 리소스 읽기 권한 확인
     * 
     * @param context    CRUD 컨텍스트
     * @param resourceId 리소스 ID
     * @return 권한 여부
     */
    boolean hasReadPermission(CrudContext context, String resourceId);

    /**
     * 리소스 생성 권한 확인
     * 
     * @param context CRUD 컨텍스트
     * @return 권한 여부
     */
    boolean hasCreatePermission(CrudContext context);

    /**
     * 리소스 수정 권한 확인
     * 
     * @param context    CRUD 컨텍스트
     * @param resourceId 리소스 ID
     * @return 권한 여부
     */
    boolean hasUpdatePermission(CrudContext context, String resourceId);

    /**
     * 리소스 삭제 권한 확인
     * 
     * @param context    CRUD 컨텍스트
     * @param resourceId 리소스 ID
     * @return 권한 여부
     */
    boolean hasDeletePermission(CrudContext context, String resourceId);

    /**
     * 통합 CMS 접근 권한 확인
     * 
     * @param context CRUD 컨텍스트
     * @return 권한 여부
     */
    boolean hasIntegratedCmsAccess(CrudContext context);

    /**
     * 서비스별 CMS 접근 권한 확인
     * 
     * @param context   CRUD 컨텍스트
     * @param serviceId 서비스 ID
     * @return 권한 여부
     */
    boolean hasServiceCmsAccess(CrudContext context, String serviceId);

    /**
     * 역할 기반 권한 확인
     * 
     * @param userRole     사용자 역할
     * @param requiredRole 필요한 최소 역할
     * @return 권한 여부
     */
    boolean hasRolePermission(UserRoleType userRole, UserRoleType requiredRole);

    /**
     * 리소스 소유자 권한 확인
     * 
     * @param context    CRUD 컨텍스트
     * @param resourceId 리소스 ID
     * @param ownerId    리소스 소유자 ID
     * @return 권한 여부
     */
    boolean hasOwnerPermission(CrudContext context, String resourceId, String ownerId);

    /**
     * 조건부 권한 확인
     * 
     * @param context    CRUD 컨텍스트
     * @param operation  작업 타입
     * @param resourceId 리소스 ID
     * @param conditions 추가 조건들
     * @return 권한 여부
     */
    boolean hasConditionalPermission(CrudContext context, String operation, String resourceId, Object conditions);

    /**
     * 권한 캐시 갱신
     * 
     * @param userUuid 사용자 UUID
     */
    void refreshPermissionCache(String userUuid);

    /**
     * 권한 감사 로그 기록
     * 
     * @param context    CRUD 컨텍스트
     * @param operation  작업 타입
     * @param resourceId 리소스 ID
     * @param granted    권한 부여 여부
     * @param reason     권한 결정 이유
     */
    void logPermissionCheck(CrudContext context, String operation, String resourceId, boolean granted, String reason);

    /**
     * 사용자별 권한 정보 조회
     * 
     * @param context CRUD 컨텍스트
     * @return 사용자 권한 정보
     */
    CrudPermissionResponse getUserPermissionInfo(CrudContext context);

    /**
     * 리소스별 권한 매트릭스 조회
     * 
     * @param context      CRUD 컨텍스트
     * @param resourceType 리소스 타입
     * @return 권한 매트릭스
     */
    CrudPermissionResponse getResourcePermissionMatrix(CrudContext context, String resourceType);
}
