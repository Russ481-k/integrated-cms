package api.v2.common.menu.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 메뉴 엔티티
 * 계층형 메뉴 구조를 표현하는 기본 엔티티
 */
@Entity
@Table(name = "MENU")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MenuType type;

    @Column(name = "DISPLAY_POSITION", nullable = false)
    private String displayPosition;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private boolean visible;

    /**
     * 메뉴가 가리키는 대상의 ID (게시판, 페이지 등)
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * 메뉴가 가리키는 대상의 ID 업데이트
     * 
     * @param targetId 대상 ID
     */
    public void updateTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public boolean isVisible() {
        return visible;
    }

    @Column(name = "SORT_ORDER", nullable = false)
    private int sortOrder;

    @Column(name = "PARENT_ID")
    private Long parentId;

    @Column
    private String url;

    @Column(name = "PAGE_ID")
    private Long pageId;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    @OrderBy("sortOrder ASC")
    private List<Menu> children = new ArrayList<>();

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 메뉴가 자식을 가질 수 있는지 확인
     */
    public boolean canHaveChildren() {
        return type == MenuType.FOLDER;
    }

    /**
     * URL이 필요한 메뉴인지 확인
     */
    public boolean requiresUrl() {
        return type == MenuType.LINK;
    }

    /**
     * 페이지 ID가 필요한 메뉴인지 확인
     */
    public boolean requiresPageId() {
        return type == MenuType.PAGE;
    }

    /**
     * 메뉴의 전체 경로를 반환
     */
    public String getFullPath() {
        if (parentId == null) {
            return name;
        }
        return parentId + " > " + name;
    }
}
