package api.v2.common.menu.repository;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.domain.MenuType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 메뉴 리포지토리 인터페이스
 */
public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 상위 메뉴 ID로 하위 메뉴 목록 조회
     */
    List<Menu> findByParentIdOrderBySortOrderAsc(Long parentId);

    /**
     * 특정 위치의 메뉴 목록 조회
     */
    List<Menu> findByDisplayPositionOrderBySortOrderAsc(String displayPosition);

    /**
     * 특정 위치의 최상위 메뉴 목록 조회
     */
    List<Menu> findByDisplayPositionAndParentIdIsNullOrderBySortOrderAsc(String displayPosition);

    /**
     * 메뉴의 최대 정렬 순서 조회
     */
    @Query("SELECT MAX(m.sortOrder) FROM Menu m WHERE m.parentId = :parentId")
    Integer findMaxSortOrderByParentId(@Param("parentId") Long parentId);

    /**
     * 특정 위치의 최상위 메뉴의 최대 정렬 순서 조회
     */
    @Query("SELECT MAX(m.sortOrder) FROM Menu m WHERE m.parentId IS NULL AND m.displayPosition = :position")
    Integer findMaxSortOrderByPosition(@Param("position") String position);

    /**
     * 특정 타입과 대상 ID로 첫 번째 메뉴 조회
     */
    Optional<Menu> findFirstByTypeAndTargetId(MenuType type, Long targetId);
}
