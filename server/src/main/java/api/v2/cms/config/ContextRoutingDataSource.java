package api.v2.cms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * ServiceContext 기반 동적 DataSource 라우팅
 * 
 * ServiceContextHolder에 설정된 serviceId를 기반으로
 * 적절한 데이터소스로 라우팅하는 클래스
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
public class ContextRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String serviceId = ServiceContextHolder.getCurrentServiceId();

        log.debug("Determining DataSource lookup key: {}", serviceId);

        // 기본값 처리: serviceId가 없으면 integrated_cms 사용
        if (serviceId == null || serviceId.trim().isEmpty()) {
            log.warn("No service context found, defaulting to 'integrated_cms'");
            return "integrated_cms";
        }

        return serviceId;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        log.info("ContextRoutingDataSource initialized successfully");
    }
}
