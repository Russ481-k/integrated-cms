package api.v2.cms.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 서비스별 접근 권한을 확인하는 어노테이션
 * 
 * 하이브리드 보안 시스템의 2계층: 커스텀 비즈니스 권한 로직
 * 
 * @author CMS Team
 * @since v2.0
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@serviceAccessChecker.hasAccess(authentication, #serviceId)")
public @interface RequireServiceAccess {
    /**
     * 서비스 접근 권한 확인 메시지
     */
    String message() default "서비스 접근 권한이 필요합니다.";
}
