package api.v2.common.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import api.v2.common.dto.ApiResponseSchema;
import api.v2.common.crud.dto.CrudContext;
import api.v2.common.crud.service.CommonCrudService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * CMS CRUD 컨트롤러 공통 기본 클래스
 * 
 * 모든 CMS 타입(통합 CMS, 서비스별 CMS)에서 공통으로 사용되는 CRUD 기능을 제공
 * 각 구현체에서 서비스 타입에 맞는 구체적인 로직을 구현
 * 
 * @param <T>             엔티티 타입
 * @param <ID>            엔티티 ID 타입
 * @param <CreateRequest> 생성 요청 DTO 타입
 * @param <UpdateRequest> 수정 요청 DTO 타입
 * @param <ResponseDto>   응답 DTO 타입
 */
@Slf4j
public abstract class AbstractCmsCrudController<T, ID, CreateRequest, UpdateRequest, ResponseDto> {

    protected final CommonCrudService<T, ID, CreateRequest, UpdateRequest, ResponseDto> crudService;

    protected AbstractCmsCrudController(
            CommonCrudService<T, ID, CreateRequest, UpdateRequest, ResponseDto> crudService) {
        this.crudService = crudService;
    }

    /**
     * 서비스 타입 반환 (구현체에서 정의)
     * 
     * @return 서비스 타입 ("cms" | "integrated_cms")
     */
    protected abstract String getServiceType();

    /**
     * 서비스 ID 반환 (구현체에서 정의, 통합 CMS는 null)
     * 
     * @param request HTTP 요청
     * @return 서비스 ID (통합 CMS의 경우 null)
     */
    protected abstract String getServiceId(HttpServletRequest request);

    /**
     * 로그 태그 반환 (구현체에서 정의)
     * 
     * @return 로그 태그
     */
    protected abstract String getLogTag();

    /**
     * 리소스 이름 반환 (구현체에서 정의)
     * 
     * @return 리소스 이름 (예: "menu", "content", "popup")
     */
    protected abstract String getResourceName();

    /**
     * 공통 목록 조회
     */
    @GetMapping
    @Operation(summary = "목록 조회", description = "리소스 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponseSchema<Page<ResponseDto>>> getList(
            Pageable pageable,
            HttpServletRequest httpRequest) {

        CrudContext context = createCrudContext(httpRequest);
        log.info("{} {} 목록 조회 요청", getLogTag(), getResourceName());

        Page<ResponseDto> result = crudService.getList(pageable, context);
        return ResponseEntity.ok(ApiResponseSchema.success(result));
    }

    /**
     * 공통 전체 목록 조회 (페이징 없음)
     */
    @GetMapping("/all")
    @Operation(summary = "전체 목록 조회", description = "모든 리소스를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponseSchema<List<ResponseDto>>> getAllList(HttpServletRequest httpRequest) {
        CrudContext context = createCrudContext(httpRequest);
        log.info("{} {} 전체 목록 조회 요청", getLogTag(), getResourceName());

        List<ResponseDto> result = crudService.getAllList(context);
        return ResponseEntity.ok(ApiResponseSchema.success(result));
    }

    /**
     * 공통 단일 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "단일 조회", description = "특정 리소스를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponseSchema<ResponseDto>> getById(
            @PathVariable ID id,
            HttpServletRequest httpRequest) {

        CrudContext context = createCrudContext(httpRequest);
        log.info("{} {} 단일 조회 요청: {}", getLogTag(), getResourceName(), id);

        ResponseDto result = crudService.getById(id, context);
        return ResponseEntity.ok(ApiResponseSchema.success(result));
    }

    /**
     * 공통 생성
     */
    @PostMapping
    @Operation(summary = "생성", description = "새로운 리소스를 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponseSchema<ResponseDto>> create(
            @Valid @RequestBody CreateRequest request,
            HttpServletRequest httpRequest) {

        CrudContext context = createCrudContext(httpRequest);
        log.info("{} {} 생성 요청", getLogTag(), getResourceName());

        ResponseDto result = crudService.create(request, context);
        return ResponseEntity.status(201)
                .body(ApiResponseSchema.success(result, getResourceName() + "이(가) 성공적으로 생성되었습니다."));
    }

    /**
     * 공통 수정
     */
    @PutMapping("/{id}")
    @Operation(summary = "수정", description = "기존 리소스를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponseSchema<ResponseDto>> update(
            @PathVariable ID id,
            @Valid @RequestBody UpdateRequest request,
            HttpServletRequest httpRequest) {

        CrudContext context = createCrudContext(httpRequest);
        log.info("{} {} 수정 요청: {}", getLogTag(), getResourceName(), id);

        ResponseDto result = crudService.update(id, request, context);
        return ResponseEntity.ok(ApiResponseSchema.success(result, getResourceName() + "이(가) 성공적으로 수정되었습니다."));
    }

    /**
     * 공통 삭제
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "삭제", description = "기존 리소스를 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponseSchema<Void>> delete(
            @PathVariable ID id,
            HttpServletRequest httpRequest) {

        CrudContext context = createCrudContext(httpRequest);
        log.info("{} {} 삭제 요청: {}", getLogTag(), getResourceName(), id);

        crudService.delete(id, context);
        return ResponseEntity.ok(ApiResponseSchema.success(getResourceName() + "이(가) 성공적으로 삭제되었습니다."));
    }

    /**
     * CrudContext 생성 헬퍼 메서드
     */
    protected CrudContext createCrudContext(HttpServletRequest request) {
        return crudService.createCrudContext(request, getServiceType(), getServiceId(request), getResourceName());
    }
}
