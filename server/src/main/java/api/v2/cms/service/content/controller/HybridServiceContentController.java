package api.v2.cms.service.content.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.cms.common.dto.ApiResponseSchema;
import api.v2.cms.config.ServiceContextHolder;
import api.v2.cms.auth.annotation.RequireServiceAccess;
import api.v2.cms.auth.annotation.RequireContentPermission;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 서비스별 콘텐츠 관리 컨트롤러 (하이브리드 인증 적용)
 * 
 * 하이브리드 보안 시스템 3계층 적용:
 * 1. SecurityConfig: /api/v2/cms/** → SUPER_ADMIN, SERVICE_ADMIN, SITE_ADMIN,
 * ADMIN
 * 2. 클래스 레벨: @RequireServiceAccess (서비스별 접근 권한)
 * 3. 메서드 레벨: @RequireContentPermission (콘텐츠별 세부 권한)
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@RestController
@RequestMapping("/cms/{serviceId}/content")
@RequireServiceAccess // 2계층: 서비스별 접근 권한 확인
@RequiredArgsConstructor
public class HybridServiceContentController {

    /**
     * 콘텐츠 목록 조회
     * 3계층: 읽기 권한 확인
     */
    @GetMapping
    @RequireContentPermission(action = "read", contentType = "CONTENT")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> getContents(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Content list requested for service: {} with hybrid authentication", serviceId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "콘텐츠 목록 조회 (하이브리드 인증 적용)");
        response.put("serviceId", serviceId);
        response.put("serviceContext", ServiceContextHolder.getCurrentServiceId());
        Map<String, Integer> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        response.put("pagination", pagination);
        response.put("authenticationLayers", "SecurityConfig + @RequireServiceAccess + @RequireContentPermission");

        return ResponseEntity.ok(ApiResponseSchema.success(response, "하이브리드 인증으로 보호됨"));
    }

    /**
     * 특정 콘텐츠 조회
     * 3계층: 읽기 권한 확인
     */
    @GetMapping("/{contentId}")
    @RequireContentPermission(action = "read", contentType = "CONTENT")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> getContent(
            @PathVariable String serviceId,
            @PathVariable Long contentId) {

        log.info("Content {} requested for service: {} with hybrid authentication", contentId, serviceId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "콘텐츠 상세 조회 (하이브리드 인증 적용)");
        response.put("serviceId", serviceId);
        response.put("contentId", contentId);
        response.put("serviceContext", ServiceContextHolder.getCurrentServiceId());
        response.put("authenticationLayers", "SecurityConfig + @RequireServiceAccess + @RequireContentPermission");

        return ResponseEntity.ok(ApiResponseSchema.success(response, "하이브리드 인증으로 보호됨"));
    }

    /**
     * 콘텐츠 생성
     * 3계층: 쓰기 권한 확인
     */
    @PostMapping
    @RequireContentPermission(action = "write", contentType = "CONTENT")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> createContent(
            @PathVariable String serviceId,
            @RequestBody Map<String, Object> contentData) {

        log.info("Content creation requested for service: {} with hybrid authentication", serviceId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "콘텐츠 생성 (하이브리드 인증 적용)");
        response.put("serviceId", serviceId);
        response.put("serviceContext", ServiceContextHolder.getCurrentServiceId());
        response.put("contentData", contentData);
        response.put("authenticationLayers", "SecurityConfig + @RequireServiceAccess + @RequireContentPermission");

        return ResponseEntity.ok(ApiResponseSchema.success(response, "콘텐츠 생성 권한 확인됨"));
    }

    /**
     * 콘텐츠 수정
     * 3계층: 쓰기 권한 + 작성자/관리자 확인
     */
    @PutMapping("/{contentId}")
    @PreAuthorize("@contentPermissionChecker.canEdit(authentication, #serviceId, #contentId)")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> updateContent(
            @PathVariable String serviceId,
            @PathVariable Long contentId,
            @RequestBody Map<String, Object> contentData) {

        log.info("Content {} update requested for service: {} with hybrid authentication", contentId, serviceId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "콘텐츠 수정 (하이브리드 인증 적용)");
        response.put("serviceId", serviceId);
        response.put("contentId", contentId);
        response.put("serviceContext", ServiceContextHolder.getCurrentServiceId());
        response.put("contentData", contentData);
        response.put("authenticationLayers", "SecurityConfig + @RequireServiceAccess + @PreAuthorize(canEdit)");

        return ResponseEntity.ok(ApiResponseSchema.success(response, "콘텐츠 수정 권한 확인됨"));
    }

    /**
     * 콘텐츠 삭제
     * 3계층: 삭제 권한 + 작성자/관리자 확인
     */
    @DeleteMapping("/{contentId}")
    @PreAuthorize("@contentPermissionChecker.canDelete(authentication, #serviceId, #contentId)")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> deleteContent(
            @PathVariable String serviceId,
            @PathVariable Long contentId) {

        log.info("Content {} deletion requested for service: {} with hybrid authentication", contentId, serviceId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "콘텐츠 삭제 (하이브리드 인증 적용)");
        response.put("serviceId", serviceId);
        response.put("contentId", contentId);
        response.put("serviceContext", ServiceContextHolder.getCurrentServiceId());
        response.put("authenticationLayers", "SecurityConfig + @RequireServiceAccess + @PreAuthorize(canDelete)");

        return ResponseEntity.ok(ApiResponseSchema.success(response, "콘텐츠 삭제 권한 확인됨"));
    }

    /**
     * 콘텐츠 일괄 작업 (관리자 전용)
     * 3계층: 고급 권한 확인
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SERVICE_ADMIN', 'SITE_ADMIN')")
    @RequireContentPermission(action = "write", contentType = "CONTENT")
    public ResponseEntity<ApiResponseSchema<Map<String, Object>>> batchOperation(
            @PathVariable String serviceId,
            @RequestBody Map<String, Object> batchData) {

        log.info("Batch operation requested for service: {} with hybrid authentication", serviceId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "콘텐츠 일괄 작업 (하이브리드 인증 적용)");
        response.put("serviceId", serviceId);
        response.put("serviceContext", ServiceContextHolder.getCurrentServiceId());
        response.put("batchData", batchData);
        response.put("authenticationLayers",
                "SecurityConfig + @RequireServiceAccess + @PreAuthorize + @RequireContentPermission");

        return ResponseEntity.ok(ApiResponseSchema.success(response, "관리자 전용 기능 접근 허용"));
    }
}
