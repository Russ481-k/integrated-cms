package api.v2.common.crud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * CRUD 목록 조회 응답 래퍼
 * 
 * 페이징 정보와 메타데이터를 포함한 표준화된 목록 응답 형식
 * 
 * @param <T> 데이터 항목 타입
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CRUD 목록 조회 응답")
public class CrudListResponse<T> {

    @Schema(description = "데이터 목록")
    private List<T> content;

    @Schema(description = "페이징 정보")
    private PageInfo pagination;

    @Schema(description = "메타데이터")
    private ListMetadata metadata;

    /**
     * 페이징 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "페이징 정보")
    public static class PageInfo {
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int pageNumber;

        @Schema(description = "페이지 크기", example = "20")
        private int pageSize;

        @Schema(description = "전체 요소 수", example = "150")
        private long totalElements;

        @Schema(description = "전체 페이지 수", example = "8")
        private int totalPages;

        @Schema(description = "첫 번째 페이지 여부", example = "true")
        private boolean first;

        @Schema(description = "마지막 페이지 여부", example = "false")
        private boolean last;

        @Schema(description = "비어있는 페이지 여부", example = "false")
        private boolean empty;

        @Schema(description = "정렬 정보")
        private SortInfo sort;
    }

    /**
     * 정렬 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "정렬 정보")
    public static class SortInfo {
        @Schema(description = "정렬 여부", example = "true")
        private boolean sorted;

        @Schema(description = "정렬 필드들", example = "[\"createdAt,desc\", \"name,asc\"]")
        private List<String> orders;
    }

    /**
     * 목록 메타데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "목록 메타데이터")
    public static class ListMetadata {
        @Schema(description = "조회 시간 (ms)", example = "45")
        private Long queryTimeMs;

        @Schema(description = "적용된 필터 수", example = "2")
        private Integer appliedFiltersCount;

        @Schema(description = "사용자 권한 레벨", example = "ADMIN")
        private String userPermissionLevel;

        @Schema(description = "서비스 컨텍스트", example = "douzone")
        private String serviceContext;

        @Schema(description = "추가 정보")
        private Object additionalInfo;
    }

    /**
     * Spring Data Page 객체로부터 CrudListResponse 생성
     */
    public static <T> CrudListResponse<T> from(Page<T> page) {
        return CrudListResponse.<T>builder()
                .content(page.getContent())
                .pagination(PageInfo.builder()
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .first(page.isFirst())
                        .last(page.isLast())
                        .empty(page.isEmpty())
                        .sort(SortInfo.builder()
                                .sorted(page.getSort().isSorted())
                                .orders(page.getSort().stream()
                                        .map(order -> order.getProperty() + ","
                                                + order.getDirection().name().toLowerCase())
                                        .collect(java.util.stream.Collectors.toList()))
                                .build())
                        .build())
                .build();
    }

    /**
     * Spring Data Page 객체와 메타데이터로 CrudListResponse 생성
     */
    public static <T> CrudListResponse<T> from(Page<T> page, ListMetadata metadata) {
        CrudListResponse<T> response = from(page);
        response.setMetadata(metadata);
        return response;
    }

    /**
     * 목록과 메타데이터로 CrudListResponse 생성 (페이징 없음)
     */
    public static <T> CrudListResponse<T> from(List<T> content, ListMetadata metadata) {
        return CrudListResponse.<T>builder()
                .content(content)
                .pagination(PageInfo.builder()
                        .pageNumber(0)
                        .pageSize(content.size())
                        .totalElements(content.size())
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .empty(content.isEmpty())
                        .sort(SortInfo.builder()
                                .sorted(false)
                                .orders(java.util.Arrays.asList())
                                .build())
                        .build())
                .metadata(metadata)
                .build();
    }

    /**
     * 간단한 목록 응답 생성 (메타데이터 없음)
     */
    public static <T> CrudListResponse<T> of(List<T> content) {
        return from(content, null);
    }
}
