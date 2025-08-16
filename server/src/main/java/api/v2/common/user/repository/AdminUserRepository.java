package api.v2.common.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import api.v2.common.user.domain.AdminUser;
import api.v2.common.user.domain.UserRoleType;

import java.util.List;
import java.util.Optional;

/**
 * 관리자 사용자 Repository (admin_user 테이블)
 * 통합 CMS 관리자 계정 전용
 *
 * @author CMS Team
 * @since v2.0
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, String> {

    /**
     * 사용자명으로 관리자 조회
     */
    Optional<AdminUser> findByUsername(String username);

    /**
     * 이메일로 관리자 조회
     */
    Optional<AdminUser> findByEmail(String email);

    /**
     * 사용자명 존재 여부 확인
     */
    boolean existsByUsername(String username);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 특정 역할의 관리자 목록 조회
     */
    List<AdminUser> findByRole(UserRoleType role);

    /**
     * 활성 상태의 관리자 목록 조회
     */
    List<AdminUser> findByStatus(String status);

    /**
     * 특정 역할과 상태의 관리자 목록 조회
     */
    List<AdminUser> findByRoleAndStatus(UserRoleType role, String status);

    /**
     * 조직별 관리자 목록 조회
     */
    List<AdminUser> findByOrganizationId(String organizationId);

    /**
     * 그룹별 관리자 목록 조회
     */
    List<AdminUser> findByGroupId(String groupId);

    /**
     * 사용자명으로 활성 관리자 조회
     */
    @Query("SELECT a FROM AdminUser a WHERE a.username = :username AND a.status = 'ACTIVE'")
    Optional<AdminUser> findActiveAdminByUsername(@Param("username") String username);

    /**
     * 임시 비밀번호 사용자 목록 조회
     */
    @Query("SELECT a FROM AdminUser a WHERE a.tempFlag = true AND a.status = 'ACTIVE'")
    List<AdminUser> findTemporaryPasswordUsers();

    /**
     * 만료된 재설정 토큰을 가진 사용자 정리
     */
    @Query("UPDATE AdminUser a SET a.resetToken = null, a.resetTokenExpiry = null WHERE a.resetTokenExpiry < CURRENT_TIMESTAMP")
    void clearExpiredResetTokens();
}
