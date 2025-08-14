package api.v2.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL 프리픽스를 기반으로 서비스 컨텍스트를 설정하는 인터셉터
 * 
 * v2 API 라우팅 패턴:
 * - /api/v2/integrated-cms/** → integrated_cms
 * - /api/v2/cms/{serviceId}/** → 동적 serviceId 추출
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Component
public class ServiceContextInterceptor implements HandlerInterceptor {

    // 서비스별 CMS API 패턴: /api/v2/cms/{serviceId}/...
    private static final Pattern SERVICE_CMS_PATTERN = Pattern.compile("/api/v2/cms/([^/]+)/.*");

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler)
            throws Exception {

        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug("Processing request: {} {}", method, path);

        try {
            // 1. 통합 관리 API 패턴 확인
            if (path.startsWith("/api/v2/integrated-cms/")) {
                ServiceContextHolder.setCurrentServiceId("integrated_cms");
                log.info("Service context set to 'integrated_cms' for path: {}", path);
                return true;
            }

            // 2. 서비스별 CMS API 패턴 확인
            Matcher matcher = SERVICE_CMS_PATTERN.matcher(path);
            if (matcher.matches()) {
                String serviceId = matcher.group(1);
                ServiceContextHolder.setCurrentServiceId(serviceId);
                log.info("Service context set to '{}' for path: {}", serviceId, path);
                return true;
            }

            // 3. v2 API 경로이지만 패턴이 맞지 않는 경우
            if (path.startsWith("/api/v2/")) {
                log.warn("v2 API path does not match expected patterns: {}", path);
                // 서비스 컨텍스트를 설정하지 않고 계속 진행
                // 다른 인터셉터나 컨트롤러에서 처리할 수 있도록 함
            }

            return true;

        } catch (Exception e) {
            log.error("Error setting service context for path: {}", path, e);
            // 에러가 발생해도 요청은 계속 진행
            return true;
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler, @Nullable Exception ex) throws Exception {

        try {
            // 요청 완료 후 서비스 컨텍스트 정리 (메모리 누수 방지)
            ServiceContextHolder.clear();
            log.debug("Service context cleared for request: {} {}",
                    request.getMethod(), request.getRequestURI());
        } catch (Exception e) {
            log.error("Error clearing service context", e);
        }
    }

}
