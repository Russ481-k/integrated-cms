package api.v2.common.menu.controller;

import api.v2.common.menu.service.AbstractMenuService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메뉴 관리 컨트롤러 구현체
 */
@RestController
@RequestMapping("/api/v2/common/menus")
public class CommonMenuController extends AbstractMenuController {

    public CommonMenuController(AbstractMenuService menuService) {
        super(menuService);
    }
}