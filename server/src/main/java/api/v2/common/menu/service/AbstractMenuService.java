package api.v2.common.menu.service;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.repository.MenuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 메뉴 관리 서비스의 기본 구현을 제공하는 추상 클래스
 */
@Transactional(readOnly = true)
public abstract class AbstractMenuService {

    protected final MenuRepository menuRepository;

    protected AbstractMenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    /**
     * 메뉴 목록 조회 (페이징)
     */
    public Page<Menu> findAll(Pageable pageable) {
        return menuRepository.findAll(pageable);
    }

    /**
     * 특정 위치의 메뉴 목록 조회
     */
    public List<Menu> findByPosition(String position) {
        return menuRepository.findByDisplayPositionOrderBySortOrderAsc(position);
    }

    /**
     * 특정 위치의 최상위 메뉴 목록 조회
     */
    public List<Menu> findRootMenusByPosition(String position) {
        return menuRepository.findByDisplayPositionAndParentIdIsNullOrderBySortOrderAsc(position);
    }

    /**
     * 하위 메뉴 목록 조회
     */
    public List<Menu> findChildren(Long parentId) {
        return menuRepository.findByParentIdOrderBySortOrderAsc(parentId);
    }

    /**
     * 단일 메뉴 조회
     */
    public Optional<Menu> findById(Long id) {
        return menuRepository.findById(id);
    }

    /**
     * 메뉴 생성
     */
    @Transactional
    public Menu create(Menu menu) {
        validateMenu(menu);
        setDefaultSortOrder(menu);
        return menuRepository.save(menu);
    }

    /**
     * 메뉴 수정
     */
    @Transactional
    public Menu update(Long id, Menu menu) {
        Menu existingMenu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + id));

        validateMenu(menu);
        updateMenu(existingMenu, menu);
        return menuRepository.save(existingMenu);
    }

    /**
     * 메뉴 삭제
     */
    @Transactional
    public void delete(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + id));

        validateDeletion(menu);
        menuRepository.delete(menu);
    }

    /**
     * 메뉴 정렬 순서 변경
     */
    @Transactional
    public void updateOrder(Long id, Long targetId, String position) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + id));

        Menu targetMenu = targetId != null
                ? menuRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("Target menu not found: " + targetId))
                : null;

        updateMenuOrder(menu, targetMenu, position);
    }

    /**
     * 메뉴 유효성 검증
     */
    protected abstract void validateMenu(Menu menu);

    /**
     * 메뉴 삭제 가능 여부 검증
     */
    protected abstract void validateDeletion(Menu menu);

    /**
     * 메뉴 정보 업데이트
     */
    protected abstract void updateMenu(Menu existingMenu, Menu newMenu);

    /**
     * 메뉴 정렬 순서 업데이트
     */
    protected abstract void updateMenuOrder(Menu menu, Menu targetMenu, String position);

    /**
     * 기본 정렬 순서 설정
     */
    private void setDefaultSortOrder(Menu menu) {
        if (menu.getSortOrder() == 0) {
            Integer maxOrder = menu.getParentId() != null
                    ? menuRepository.findMaxSortOrderByParentId(menu.getParentId())
                    : menuRepository.findMaxSortOrderByPosition(menu.getDisplayPosition());

            menu.setSortOrder(maxOrder != null ? maxOrder + 1 : 0);
        }
    }
}
