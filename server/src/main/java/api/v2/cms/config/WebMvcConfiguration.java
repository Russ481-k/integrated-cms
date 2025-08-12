package api.v2.cms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 - 테넌트 인터셉터 등록
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**", // 헬스체크 등 모니터링
                        "/swagger-ui/**", // API 문서
                        "/v3/api-docs/**", // API 문서
                        "/h2-console/**", // H2 콘솔 (개발용)
                        "/public/**", // 정적 리소스
                        "/health", // 헬스체크
                        "/error" // 에러 페이지
                );
    }
}
