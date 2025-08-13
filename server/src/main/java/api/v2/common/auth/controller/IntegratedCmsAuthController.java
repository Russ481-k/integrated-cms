package api.v2.common.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import api.v2.common.auth.service.CommonAuthService;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 통합 CMS 인증 컨트롤러
 * 경로: /api/v2/integrated-cms/auth/**
 * 
 * AbstractCmsAuthController를 상속받아 통합 CMS 전용 인증 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/integrated-cms/auth")
@Tag(name = "Integrated CMS Auth", description = "통합 CMS 인증 관련 API")
public class IntegratedCmsAuthController extends AbstractCmsAuthController {

    public IntegratedCmsAuthController(CommonAuthService authService) {
        super(authService);
    }

    @Override
    protected String getServiceType() {
        return "integrated_cms";
    }

    @Override
    protected String getServiceId(HttpServletRequest request) {
        // 통합 CMS는 serviceId가 없음
        return null;
    }

    @Override
    protected String getLogTag() {
        return "[IntegratedCMS]";
    }
}
