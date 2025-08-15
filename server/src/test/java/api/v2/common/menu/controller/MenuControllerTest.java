package api.v2.common.menu.controller;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.domain.MenuType;
import api.v2.common.menu.service.AbstractMenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 메뉴 컨트롤러 단위 테스트
 */
@WebMvcTest(CommonMenuController.class)
@Import(TestSecurityConfig.class)
class MenuControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AbstractMenuService menuService;

        @Autowired
        private ObjectMapper objectMapper;

        // Java 8 호환 문자열 반복 유틸리티
        private String repeat(String str, int count) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count; i++) {
                        sb.append(str);
                }
                return sb.toString();
        }

        @Test
        @DisplayName("메뉴 목록이 올바르게 조회된다")
        void 메뉴_목록이_올바르게_조회된다() throws Exception {
                System.out.println("\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mMenu List Retrieval\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 페이징된 메뉴 목록 준비");
                Menu menu1 = Menu.builder()
                                .id(1L)
                                .name("메뉴 1")
                                .type(MenuType.FOLDER)
                                .displayPosition("HEADER")
                                .visible(true)
                                .build();

                Menu menu2 = Menu.builder()
                                .id(2L)
                                .name("메뉴 2")
                                .type(MenuType.LINK)
                                .displayPosition("HEADER")
                                .visible(true)
                                .url("https://example.com")
                                .build();

                Page<Menu> menuPage = new PageImpl<>(
                                Arrays.asList(menu1, menu2),
                                PageRequest.of(0, 10),
                                2);

                when(menuService.findAll(any())).thenReturn(menuPage);

                System.out.println("    \033[90m→\033[0m Total menus: \033[36m2\033[0m");
                System.out.println("    \033[90m→\033[0m Page size: \033[36m10\033[0m");

                // When & Then
                System.out.println("  \033[2m⚡ Action & ✨ Verify:\033[0m API 호출 및 응답 검증");
                mockMvc.perform(get("/api/v2/common/menus"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.content[0].name").value("메뉴 1"))
                                .andExpect(jsonPath("$.content[1].name").value("메뉴 2"));

                verify(menuService).findAll(any());

                System.out.println("    \033[32m✓\033[0m \033[90mAPI response:\033[0m \033[32m200 OK\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mContent validation:\033[0m \033[32mPassed\033[0m\n");
        }

        @Test
        @DisplayName("메뉴가 올바르게 생성된다")
        void 메뉴가_올바르게_생성된다() throws Exception {
                System.out.println("\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mMenu Creation\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 메뉴 생성 요청 준비");
                Menu newMenu = Menu.builder()
                                .name("새 메뉴")
                                .type(MenuType.FOLDER)
                                .displayPosition("HEADER")
                                .visible(true)
                                .build();

                Menu savedMenu = Menu.builder()
                                .id(1L)
                                .name("새 메뉴")
                                .type(MenuType.FOLDER)
                                .displayPosition("HEADER")
                                .visible(true)
                                .build();

                when(menuService.create(any(Menu.class))).thenReturn(savedMenu);

                System.out.println("    \033[90m→\033[0m Menu name: \033[36m" + newMenu.getName() + "\033[0m");
                System.out.println("    \033[90m→\033[0m Type: \033[36m" + newMenu.getType() + "\033[0m");

                // When & Then
                System.out.println("  \033[2m⚡ Action & ✨ Verify:\033[0m API 호출 및 응답 검증");
                mockMvc.perform(post("/api/v2/common/menus")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newMenu)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1L))
                                .andExpect(jsonPath("$.name").value("새 메뉴"));

                verify(menuService).create(any(Menu.class));

                System.out.println("    \033[32m✓\033[0m \033[90mAPI response:\033[0m \033[32m200 OK\033[0m");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mCreated menu:\033[0m \033[32mID=" + savedMenu.getId()
                                                + "\033[0m\n");
        }

        @Test
        @DisplayName("메뉴 정렬 순서가 올바르게 변경된다")
        void 메뉴_정렬_순서가_올바르게_변경된다() throws Exception {
                System.out.println("\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mMenu Order Update\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 메뉴 순서 변경 요청 준비");
                Map<String, Object> request = new HashMap<>();
                request.put("targetId", 2L);
                request.put("position", "after");

                System.out.println("    \033[90m→\033[0m Menu ID: \033[36m1\033[0m");
                System.out.println("    \033[90m→\033[0m Target ID: \033[36m2\033[0m");
                System.out.println("    \033[90m→\033[0m Position: \033[36mafter\033[0m");

                // When & Then
                System.out.println("  \033[2m⚡ Action & ✨ Verify:\033[0m API 호출 및 응답 검증");
                mockMvc.perform(put("/api/v2/common/menus/1/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(menuService).updateOrder(eq(1L), eq(2L), eq("after"));

                System.out.println("    \033[32m✓\033[0m \033[90mAPI response:\033[0m \033[32m200 OK\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mOrder update:\033[0m \033[32mSuccess\033[0m\n");
        }

        @Test
        @DisplayName("메뉴가 올바르게 삭제된다")
        void 메뉴가_올바르게_삭제된다() throws Exception {
                System.out.println("\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mMenu Deletion\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 메뉴 삭제 요청 준비");
                Long menuId = 1L;
                Menu menu = Menu.builder()
                                .id(menuId)
                                .name("삭제할 메뉴")
                                .type(MenuType.FOLDER)
                                .displayPosition("HEADER")
                                .visible(true)
                                .build();

                when(menuService.findById(menuId)).thenReturn(Optional.of(menu));

                System.out.println("    \033[90m→\033[0m Menu ID: \033[36m" + menuId + "\033[0m");
                System.out.println("    \033[90m→\033[0m Menu name: \033[36m" + menu.getName() + "\033[0m");

                // When & Then
                System.out.println("  \033[2m⚡ Action & ✨ Verify:\033[0m API 호출 및 응답 검증");
                mockMvc.perform(delete("/api/v2/common/menus/" + menuId))
                                .andExpect(status().isOk());

                verify(menuService).delete(menuId);

                System.out.println("    \033[32m✓\033[0m \033[90mAPI response:\033[0m \033[32m200 OK\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mDeletion:\033[0m \033[32mSuccess\033[0m\n");
        }
}
