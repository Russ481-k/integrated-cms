package api.v2.common.menu.service;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.domain.MenuType;

import java.util.Optional;

/**
 * 메뉴 관리 서비스 인터페이스
 */
public interface MenuService {
    /**
     * 메뉴 ID로 메뉴 조회
     */
    Optional<Menu> findById(Long menuId);

    /**
     * 메뉴 타입과 대상 ID로 메뉴 조회
     */
    Optional<Menu> findByTypeAndTargetId(MenuType type, Long targetId);

    /**
     * 메뉴의 대상 ID 업데이트
     */
    void updateTargetId(Long menuId, Long targetId);
}
