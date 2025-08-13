package api.v2.cms.user.domain;

import lombok.*;
import org.hibernate.annotations.Comment;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 관리자 사용자 엔티티 (admin_user 테이블)
 * 통합 CMS 관리자 계정 전용
 *
 * @author CMS Team
 * @since v2.0
 */
@Entity
@Table(name = "admin_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password"})
public class AdminUser {

    @Id
    @Column(name = "UUID", length = 36)
    @Comment("관리자 고유 식별자")
    private String uuid;

    @Column(name = "USERNAME", length = 50, nullable = false, unique = true)
    @Comment("관리자 사용자명")
    private String username;

    @Column(name = "NAME", length = 100)
    @Comment("관리자 이름")
    private String name;

    @Column(name = "EMAIL", length = 100, nullable = false)
    @Comment("관리자 이메일")
    private String email;

    @Column(name = "PASSWORD", length = 255, nullable = false)
    @Comment("암호화된 비밀번호")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", length = 20, nullable = false)
    @Comment("관리자 권한 역할")
    private UserRoleType role;

    @Column(name = "AVATAR_URL", length = 500)
    @Comment("프로필 이미지 URL")
    private String avatarUrl;

    @Column(name = "STATUS", length = 20)
    @Builder.Default
    @Comment("계정 상태")
    private String status = "ACTIVE";

    @Column(name = "ORGANIZATION_ID", length = 50)
    @Comment("소속 조직 ID")
    private String organizationId;

    @Column(name = "GROUP_ID", length = 50)
    @Comment("관리자 그룹 ID")
    private String groupId;

    @Column(name = "PHONE", length = 20)
    @Comment("연락처")
    private String phone;

    @Column(name = "TEMP_PW_FLAG")
    @Builder.Default
    @Comment("임시 비밀번호 여부")
    private Boolean tempFlag = false;

    @Column(name = "PROVIDER", length = 50)
    @Comment("인증 제공자")
    private String provider;

    @Column(name = "RESET_TOKEN", length = 255)
    @Comment("비밀번호 재설정 토큰")
    private String resetToken;

    @Column(name = "RESET_TOKEN_EXPIRY")
    @Comment("재설정 토큰 만료일시")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "IS_TEMPORARY")
    @Builder.Default
    @Comment("임시 계정 여부")
    private Boolean isTemporary = false;

    @Column(name = "CREATED_BY", length = 50)
    @Comment("생성자")
    private String createdBy;

    @Column(name = "CREATED_IP", length = 45)
    @Comment("생성 IP")
    private String createdIp;

    @Column(name = "CREATED_AT")
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_BY", length = 50)
    @Comment("수정자")
    private String updatedBy;

    @Column(name = "UPDATED_AT")
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_IP", length = 45)
    @Comment("수정 IP")
    private String updatedIp;

    @Column(name = "MEMO", length = 1000)
    @Comment("관리자 메모")
    private String memo;

    @Column(name = "MEMO_UPDATED_AT")
    @Comment("메모 수정일시")
    private LocalDateTime memoUpdatedAt;

    @Column(name = "MEMO_UPDATED_BY", length = 50)
    @Comment("메모 수정자")
    private String memoUpdatedBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
