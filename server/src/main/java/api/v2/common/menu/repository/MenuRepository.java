package api.v2.common.menu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.v2.common.menu.domain.Menu;
import api.v2.common.menu.domain.MenuType;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByVisibleTrue();

    /**
     * 특정 타입의 활성화된 메뉴를 조회합니다.
     * 
     * @param type 메뉴 타입
     * @return 메뉴 목록
     */
    List<Menu> findByTypeAndVisibleTrue(String type);

    Page<Menu> findByType(MenuType type, Pageable pageable);

    // Find the first menu linked to a specific target type (enum) and ID
    Optional<Menu> findFirstByTypeAndTargetId(MenuType type, Long targetId);

    /**
     * 특정 서비스의 메뉴 목록을 조회합니다.
     * 
     * @param serviceId 서비스 ID
     * @return 메뉴 목록
     */
    List<Menu> findByServiceId(String serviceId);

    /**
     * 특정 서비스의 활성화된 메뉴 목록을 조회합니다.
     * 
     * @param serviceId 서비스 ID
     * @return 활성화된 메뉴 목록
     */
    List<Menu> findByServiceIdAndVisibleTrue(String serviceId);

    /**
     * 특정 서비스의 특정 타입 메뉴를 조회합니다.
     * 
     * @param serviceId 서비스 ID
     * @param type      메뉴 타입
     * @return 메뉴 목록
     */
    List<Menu> findByServiceIdAndType(String serviceId, MenuType type);

    /**
     * 특정 서비스의 특정 타입 활성화 메뉴를 조회합니다.
     * 
     * @param serviceId 서비스 ID
     * @param type      메뉴 타입
     * @return 활성화된 메뉴 목록
     */
    List<Menu> findByServiceIdAndTypeAndVisibleTrue(String serviceId, MenuType type);
}