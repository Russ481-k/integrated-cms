package api.v2.common.menu.domain;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.domain.MenuType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.domain.MenuType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Menu 엔티티 단위 테스트
 * 
 * 메뉴 엔티티의 핵심 기능을 검증:
 * - 기본 정보 관리
 * - 계층 구조 관리
 * - 메뉴 타입별 동작
 * - 정렬 순서 관리
 */
class MenuTest {

    // Java 8 호환 문자열 반복 유틸리티
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    @Test
    @DisplayName("메뉴 엔티티가 올바르게 생성된다")
    void 메뉴_엔티티가_올바르게_생성된다() {
        System.out.println("\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mMenu Entity Creation\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 메뉴 엔티티 생성 정보 준비");
        String name = "메인 메뉴";
        MenuType type = MenuType.FOLDER;
        String displayPosition = "HEADER";

        System.out.println("    \033[90m→\033[0m Name: \033[36m" + name + "\033[0m");
        System.out.println("    \033[90m→\033[0m Type: \033[36m" + type + "\033[0m");
        System.out.println("    \033[90m→\033[0m Position: \033[36m" + displayPosition + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 메뉴 엔티티 생성");
        Menu menu = Menu.builder()
                .name(name)
                .type(type)
                .displayPosition(displayPosition)
                .visible(true)
                .sortOrder(0)
                .children(new ArrayList<>())
                .build();

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 엔티티 속성 검증");
        assertNotNull(menu);
        assertEquals(name, menu.getName());
        assertEquals(type, menu.getType());
        assertEquals(displayPosition, menu.getDisplayPosition());
        assertTrue(menu.isVisible());
        assertEquals(0, menu.getSortOrder());
        assertNotNull(menu.getChildren());
        assertTrue(menu.getChildren().isEmpty());

        System.out.println("    \033[32m✓\033[0m \033[90mEntity created:\033[0m \033[32mSuccess\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mProperties:\033[0m \033[32mValidated\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mChildren list:\033[0m \033[32mInitialized\033[0m\n");
    }

    @Test
    @DisplayName("메뉴 계층 구조가 올바르게 동작한다")
    void 메뉴_계층_구조가_올바르게_동작한다() {
        System.out.println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mMenu Hierarchy Management\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 부모-자식 메뉴 구조 준비");
        Menu parentMenu = Menu.builder()
                .name("상위 메뉴")
                .type(MenuType.FOLDER)
                .displayPosition("HEADER")
                .visible(true)
                .sortOrder(0)
                .children(new ArrayList<>())
                .build();

        Menu childMenu = Menu.builder()
                .name("하위 메뉴")
                .type(MenuType.LINK)
                .displayPosition("HEADER")
                .visible(true)
                .sortOrder(0)
                .parentId(1L)
                .children(new ArrayList<>())
                .build();

        System.out.println("    \033[90m→\033[0m Parent: \033[36m" + parentMenu.getName() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Child: \033[36m" + childMenu.getName() + "\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 부모 메뉴에 자식 메뉴 추가");
        parentMenu.getChildren().add(childMenu);

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 계층 구조 검증");
        assertEquals(1, parentMenu.getChildren().size());
        assertEquals(childMenu, parentMenu.getChildren().get(0));
        assertEquals(MenuType.FOLDER, parentMenu.getType());
        assertEquals(MenuType.LINK, childMenu.getType());
        assertEquals(1L, childMenu.getParentId());

        System.out.println("    \033[32m✓\033[0m \033[90mHierarchy:\033[0m \033[32mEstablished\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mParent type:\033[0m \033[32mFOLDER\033[0m");
        System.out.println("    \033[32m✓\033[0m \033[90mChild type:\033[0m \033[32mLINK\033[0m\n");
    }

    @Test
    @DisplayName("메뉴 타입별 제약조건이 올바르게 동작한다")
    void 메뉴_타입별_제약조건이_올바르게_동작한다() {
        System.out.println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mMenu Type Constraints\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 다양한 타입의 메뉴 준비");
        Menu folderMenu = Menu.builder()
                .name("폴더 메뉴")
                .type(MenuType.FOLDER)
                .displayPosition("HEADER")
                .visible(true)
                .sortOrder(0)
                .children(new ArrayList<>())
                .build();

        Menu linkMenu = Menu.builder()
                .name("링크 메뉴")
                .type(MenuType.LINK)
                .displayPosition("HEADER")
                .visible(true)
                .sortOrder(1)
                .url("https://example.com")
                .children(new ArrayList<>())
                .build();

        System.out.println("    \033[90m→\033[0m Folder menu: \033[36m" + folderMenu.getName() + "\033[0m");
        System.out.println("    \033[90m→\033[0m Link menu: \033[36m" + linkMenu.getName() + "\033[0m");

        // When & Then
        System.out.println("  \033[2m⚡ Action & ✨ Verify:\033[0m 타입별 제약조건 검증");

        // 1. FOLDER 타입은 자식을 가질 수 있음
        folderMenu.getChildren().add(linkMenu);
        assertEquals(1, folderMenu.getChildren().size());
        System.out.println("    \033[32m✓\033[0m \033[90mFolder can have children:\033[0m \033[32mTrue\033[0m");

        // 2. LINK 타입은 URL을 가짐
        assertNotNull(linkMenu.getUrl());
        assertEquals("https://example.com", linkMenu.getUrl());
        System.out
                .println("    \033[32m✓\033[0m \033[90mLink has URL:\033[0m \033[32m" + linkMenu.getUrl() + "\033[0m");

        // 3. 정렬 순서가 올바르게 설정됨
        assertEquals(0, folderMenu.getSortOrder());
        assertEquals(1, linkMenu.getSortOrder());
        System.out.println("    \033[32m✓\033[0m \033[90mSort order:\033[0m \033[32mCorrect\033[0m\n");
    }

    @Test
    @DisplayName("메뉴 정렬 순서가 올바르게 관리된다")
    void 메뉴_정렬_순서가_올바르게_관리된다() {
        System.out.println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mMenu Sort Order Management\033[0m");

        // Given
        System.out.println("  \033[2m🔍 Setup:\033[0m 여러 메뉴 항목 준비");
        Menu menu1 = Menu.builder()
                .name("메뉴 1")
                .type(MenuType.LINK)
                .displayPosition("HEADER")
                .visible(true)
                .sortOrder(0)
                .build();

        Menu menu2 = Menu.builder()
                .name("메뉴 2")
                .type(MenuType.LINK)
                .displayPosition("HEADER")
                .visible(true)
                .sortOrder(1)
                .build();

        Menu menu3 = Menu.builder()
                .name("메뉴 3")
                .type(MenuType.LINK)
                .displayPosition("HEADER")
                .visible(true)
                .sortOrder(2)
                .build();

        System.out.println("    \033[90m→\033[0m Initial order: \033[36m" +
                menu1.getName() + " (" + menu1.getSortOrder() + ") → " +
                menu2.getName() + " (" + menu2.getSortOrder() + ") → " +
                menu3.getName() + " (" + menu3.getSortOrder() + ")\033[0m");

        // When
        System.out.println("  \033[2m⚡ Action:\033[0m 정렬 순서 변경");
        menu2.setSortOrder(0);
        menu1.setSortOrder(1);

        // Then
        System.out.println("  \033[2m✨ Verify:\033[0m 변경된 정렬 순서 검증");
        assertEquals(1, menu1.getSortOrder());
        assertEquals(0, menu2.getSortOrder());
        assertEquals(2, menu3.getSortOrder());

        System.out.println("    \033[32m✓\033[0m \033[90mNew order:\033[0m \033[32m" +
                menu2.getName() + " (" + menu2.getSortOrder() + ") → " +
                menu1.getName() + " (" + menu1.getSortOrder() + ") → " +
                menu3.getName() + " (" + menu3.getSortOrder() + ")\033[0m\n");
    }
}
