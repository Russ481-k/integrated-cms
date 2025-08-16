package api.v2.common.config;

import api.v2.common.config.ServiceContextHolder;
import api.v2.common.config.DynamicServiceDataSourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

/**
 * ServiceContext 기반 동적 DataSource 라우팅
 * 
 * ServiceContextHolder에 설정된 serviceId를 기반으로
 * 적절한 데이터소스로 라우팅하는 클래스
 * 
 * 동적 데이터소스 생성 지원:
 * - 환경변수 기반 새로운 서비스 데이터소스 자동 생성
 * - 런타임 서비스 추가/제거 지원
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
public class EnhancedContextRoutingDataSource extends AbstractRoutingDataSource {

    private DynamicServiceDataSourceManager dataSourceManager;

    public void setDataSourceManager(DynamicServiceDataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String serviceId = ServiceContextHolder.getCurrentServiceId();

        log.debug("Determining DataSource lookup key: {}", serviceId);

        // 기본값 처리: serviceId가 없으면 integrated_cms 사용
        if (serviceId == null || serviceId.trim().isEmpty()) {
            log.debug("No service context found, defaulting to 'integrated_cms'");
            return "integrated_cms";
        }

        return serviceId;
    }

    @Override
    protected DataSource determineTargetDataSource() {
        String serviceId = (String) determineCurrentLookupKey();
        
        if (dataSourceManager != null) {
            // 동적 데이터소스 관리자에서 데이터소스 조회 (없으면 환경변수에서 생성 시도)
            DataSource dataSource = dataSourceManager.getServiceDataSource(serviceId);
            if (dataSource != null) {
                log.debug("✅ Using dynamic data source for service: {}", serviceId);
                return dataSource;
            }
        }

        // 기본 라우팅 로직으로 폴백
        log.debug("🔄 Falling back to default routing for service: {}", serviceId);
        return super.determineTargetDataSource();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        log.info("🚀 EnhancedContextRoutingDataSource initialized successfully with dynamic data source support");
    }
}
