package api.v2.common.menu.service.impl;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.domain.MenuType;
import api.v2.common.menu.repository.MenuRepository;
import api.v2.common.menu.service.AbstractMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 공통 메뉴 관리 서비스 구현체
 */
@Slf4j
@Service
public class CommonMenuServiceImpl extends AbstractMenuService {

    public CommonMenuServiceImpl(MenuRepository menuRepository) {
        super(menuRepository);
    }

    @Override
    protected void validateMenu(Menu menu) {
        // 1. 기본 필드 검증
        if (!StringUtils.hasText(menu.getName())) {
            throw new IllegalArgumentException("Menu name is required");
        }
        if (menu.getType() == null) {
            throw new IllegalArgumentException("Menu type is required");
        }
        if (!StringUtils.hasText(menu.getDisplayPosition())) {
            throw new IllegalArgumentException("Display position is required");
        }

        // 2. 타입별 필수 필드 검증
        if (menu.getType() == MenuType.LINK && !StringUtils.hasText(menu.getUrl())) {
            throw new IllegalArgumentException("URL is required for LINK type menu");
        }
        if (menu.getType() == MenuType.PAGE && menu.getPageId() == null) {
            throw new IllegalArgumentException("Page ID is required for PAGE type menu");
        }

        // 3. 부모 메뉴 검증
        if (menu.getParentId() != null) {
            Menu parentMenu = menuRepository.findById(menu.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent menu not found: " + menu.getParentId()));

            if (!parentMenu.canHaveChildren()) {
                throw new IllegalArgumentException("Parent menu cannot have children: " + menu.getParentId());
            }
        }
    }

    @Override
    protected void validateDeletion(Menu menu) {
        // 1. 하위 메뉴 존재 여부 확인
        List<Menu> children = menuRepository.findByParentIdOrderBySortOrderAsc(menu.getId());
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete menu with children: " + menu.getId());
        }
    }

    @Override
    protected void updateMenu(Menu existingMenu, Menu newMenu) {
        // 1. 기본 정보 업데이트
        existingMenu.setName(newMenu.getName());
        existingMenu.setType(newMenu.getType());
        existingMenu.setDisplayPosition(newMenu.getDisplayPosition());
        existingMenu.setVisible(newMenu.isVisible());
        existingMenu.setUrl(newMenu.getUrl());
        existingMenu.setPageId(newMenu.getPageId());

        // 2. 부모 메뉴 변경 시
        if (!isEqual(existingMenu.getParentId(), newMenu.getParentId())) {
            // 새 부모 메뉴의 마지막 위치로 이동
            existingMenu.setParentId(newMenu.getParentId());
            Integer maxOrder = menuRepository.findMaxSortOrderByParentId(newMenu.getParentId());
            existingMenu.setSortOrder(maxOrder != null ? maxOrder + 1 : 0);
        }
    }

    @Override
    protected void updateMenuOrder(Menu menu, Menu targetMenu, String position) {
        // 1. 같은 부모 내에서의 이동
        if (isEqual(menu.getParentId(), targetMenu != null ? targetMenu.getParentId() : null)) {
            reorderSiblings(menu, targetMenu, position);
            return;
        }

        // 2. 다른 부모로 이동
        Long newParentId = "inside".equals(position) && targetMenu != null
                ? targetMenu.getId()
                : targetMenu != null ? targetMenu.getParentId() : null;

        // 새 부모 메뉴 검증
        if (newParentId != null) {
            Menu newParent = menuRepository.findById(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("New parent menu not found: " + newParentId));

            if (!newParent.canHaveChildren()) {
                throw new IllegalArgumentException("New parent menu cannot have children: " + newParentId);
            }
        }

        // 이동 실행
        menu.setParentId(newParentId);
        reorderSiblings(menu, targetMenu, position);
    }

    /**
     * 형제 메뉴들 사이에서 순서 변경
     */
    private void reorderSiblings(Menu menu, Menu targetMenu, String position) {
        List<Menu> siblings = menuRepository.findByParentIdOrderBySortOrderAsc(
                targetMenu != null ? targetMenu.getParentId() : null);

        int newOrder;
        if (targetMenu == null || "inside".equals(position)) {
            // 최상위로 이동하거나 다른 메뉴의 하위로 이동
            newOrder = siblings.size();
        } else {
            // 특정 메뉴 앞/뒤로 이동
            int targetIndex = siblings.indexOf(targetMenu);
            newOrder = "before".equals(position) ? targetIndex : targetIndex + 1;
        }

        // 순서 업데이트
        menu.setSortOrder(newOrder);
        menuRepository.save(menu); // 이동하는 메뉴 저장

        // 다른 메뉴들의 순서 조정
        for (Menu sibling : siblings) {
            if (sibling.getSortOrder() >= newOrder && !sibling.getId().equals(menu.getId())) {
                sibling.setSortOrder(sibling.getSortOrder() + 1);
                menuRepository.save(sibling);
            }
        }
    }

    private boolean isEqual(Long a, Long b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.equals(b);
    }
}
