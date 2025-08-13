package api.v2.common.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import api.v2.common.auth.service.CommonAuthService;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 서비스별 CMS 인증 컨트롤러
 * 경로: /api/v2/cms/{serviceId}/auth/**
 * 
 * AbstractCmsAuthController를 상속받아 서비스별 CMS 전용 인증 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/cms/{serviceId}/auth")
@Tag(name = "Service CMS Auth", description = "서비스별 CMS 인증 관련 API")
public class ServiceCmsAuthController extends AbstractCmsAuthController {

    public ServiceCmsAuthController(CommonAuthService authService) {
        super(authService);
    }

    @Override
    protected String getServiceType() {
        return "cms";
    }

    @Override
    protected String getServiceId(HttpServletRequest request) {
        // URL 경로에서 serviceId 추출
        String path = request.getRequestURI();
        String[] pathParts = path.split("/");

        // /api/v2/cms/{serviceId}/auth/... 형태에서 serviceId 추출
        for (int i = 0; i < pathParts.length - 1; i++) {
            if ("cms".equals(pathParts[i]) && i + 1 < pathParts.length) {
                return pathParts[i + 1];
            }
        }

        log.warn("ServiceId not found in request path: {}", path);
        return null;
    }

    @Override
    protected String getLogTag() {
        return "[ServiceCMS]";
    }
}
