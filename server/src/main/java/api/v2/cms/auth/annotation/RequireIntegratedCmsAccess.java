package api.v2.cms.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 통합 CMS 관리 권한을 확인하는 어노테이션
 * 
 * 하이브리드 보안 시스템의 2계층: 커스텀 비즈니스 권한 로직
 * 
 * @author CMS Team
 * @since v2.0
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@integratedCmsAccessChecker.hasAccess(authentication)")
public @interface RequireIntegratedCmsAccess {
    /**
     * 통합 관리 권한 확인 메시지
     */
    String message() default "통합 CMS 관리 권한이 필요합니다.";
}
