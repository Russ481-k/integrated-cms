package api.v2.common.menu.service.impl;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.domain.MenuType;
import api.v2.common.menu.repository.MenuRepository;
import api.v2.common.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 메뉴 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Menu> findById(Long menuId) {
        return menuRepository.findById(menuId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Menu> findByTypeAndTargetId(MenuType type, Long targetId) {
        return menuRepository.findFirstByTypeAndTargetId(type, targetId);
    }

    @Override
    @Transactional
    public void updateTargetId(Long menuId, Long targetId) {
        menuRepository.findById(menuId).ifPresent(menu -> {
            menu.updateTargetId(targetId);
            menuRepository.save(menu);
        });
    }
}
