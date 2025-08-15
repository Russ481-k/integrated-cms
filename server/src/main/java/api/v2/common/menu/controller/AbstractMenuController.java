package api.v2.common.menu.controller;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.service.AbstractMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 메뉴 관리 컨트롤러의 기본 구현을 제공하는 추상 클래스
 */
@RequiredArgsConstructor
public abstract class AbstractMenuController {

    protected final AbstractMenuService menuService;

    /**
     * 메뉴 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<Menu>> getMenus(Pageable pageable) {
        return ResponseEntity.ok(menuService.findAll(pageable));
    }

    /**
     * 특정 위치의 메뉴 목록 조회
     */
    @GetMapping("/position/{position}")
    public ResponseEntity<List<Menu>> getMenusByPosition(@PathVariable String position) {
        return ResponseEntity.ok(menuService.findByPosition(position));
    }

    /**
     * 특정 위치의 최상위 메뉴 목록 조회
     */
    @GetMapping("/position/{position}/root")
    public ResponseEntity<List<Menu>> getRootMenusByPosition(@PathVariable String position) {
        return ResponseEntity.ok(menuService.findRootMenusByPosition(position));
    }

    /**
     * 하위 메뉴 목록 조회
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<Menu>> getChildren(@PathVariable Long parentId) {
        return ResponseEntity.ok(menuService.findChildren(parentId));
    }

    /**
     * 단일 메뉴 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Menu> getMenu(@PathVariable Long id) {
        return menuService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 메뉴 생성
     */
    @PostMapping
    public ResponseEntity<Menu> createMenu(@RequestBody Menu menu) {
        return ResponseEntity.ok(menuService.create(menu));
    }

    /**
     * 메뉴 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<Menu> updateMenu(@PathVariable Long id, @RequestBody Menu menu) {
        return ResponseEntity.ok(menuService.update(id, menu));
    }

    /**
     * 메뉴 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        menuService.delete(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 메뉴 정렬 순서 변경
     */
    @PutMapping("/{id}/order")
    public ResponseEntity<Void> updateMenuOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        Long targetId = request.get("targetId") != null
                ? Long.valueOf(request.get("targetId").toString())
                : null;
        String position = (String) request.get("position");

        menuService.updateOrder(id, targetId, position);
        return ResponseEntity.ok().build();
    }
}
