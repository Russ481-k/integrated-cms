package api.v2.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 동적 서비스 데이터소스 관리자
 * 
 * ServiceContext 시스템에서 사용하는 동적 데이터소스 생성 및 관리
 * 환경변수 기반으로 새로운 서비스 데이터소스를 런타임에 생성
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Component
public class DynamicServiceDataSourceManager {

    private final Map<String, DataSource> serviceDataSources = new ConcurrentHashMap<>();

    // 기본 integrated_cms 설정
    @Value("${INTEGRATED_CMS_DATASOURCE_URL:jdbc:mariadb://db:3306/integrated_cms?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true}")
    private String integratedDbUrl;

    @Value("${INTEGRATED_DB_USERNAME}")
    private String integratedDbUsername;

    @Value("${INTEGRATED_DB_PASSWORD}")
    private String integratedDbPassword;

    /**
     * 초기화 시 기본 데이터소스들 생성
     */
    @PostConstruct
    public void initializeDefaultDataSources() {
        log.info("Initializing default service data sources...");

        // 1. integrated_cms (기본)
        createServiceDataSource("integrated_cms", integratedDbUrl, integratedDbUsername, integratedDbPassword);

        // 2. 환경변수에서 추가 서비스들 자동 감지
        String[] knownServices = {"douzone", "service1", "service2", "arpina"};
        for (String serviceId : knownServices) {
            tryCreateServiceFromEnvironment(serviceId);
        }

        log.info("Service data source initialization completed. Active services: {}", serviceDataSources.keySet());
    }

    /**
     * 환경변수에서 서비스 데이터소스 생성 시도
     */
    public boolean tryCreateServiceFromEnvironment(String serviceId) {
        String envPrefix = serviceId.toUpperCase();
        String url = System.getenv(envPrefix + "_DATASOURCE_URL");
        String username = System.getenv(envPrefix + "_DB_USERNAME");
        String password = System.getenv(envPrefix + "_DB_PASSWORD");

        if (url != null && username != null && password != null) {
            createServiceDataSource(serviceId, url, username, password);
            log.info("✅ Service '{}' data source created from environment variables", serviceId);
            return true;
        } else {
            log.debug("❌ Environment variables not found for service: {}", serviceId);
            return false;
        }
    }

    /**
     * 서비스 데이터소스 생성
     */
    public DataSource createServiceDataSource(String serviceId, String url, String username, String password) {
        if (serviceDataSources.containsKey(serviceId)) {
            log.warn("Service data source already exists: {}", serviceId);
            return serviceDataSources.get(serviceId);
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setPoolName(serviceId + "-pool");

            // 서비스별 풀 설정
            if ("integrated_cms".equals(serviceId)) {
                config.setMaximumPoolSize(20);
                config.setMinimumIdle(5);
            } else {
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
            }

            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);

            // 성능 최적화 설정
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            DataSource dataSource = new HikariDataSource(config);
            serviceDataSources.put(serviceId, dataSource);

            log.info("🚀 Created data source for service '{}' with URL: {}", serviceId, url);
            return dataSource;

        } catch (Exception e) {
            log.error("❌ Failed to create data source for service '{}': {}", serviceId, e.getMessage(), e);
            throw new RuntimeException("Failed to create data source for service: " + serviceId, e);
        }
    }

    /**
     * 서비스 데이터소스 존재 확인
     */
    public boolean hasServiceDataSource(String serviceId) {
        return serviceDataSources.containsKey(serviceId);
    }

    /**
     * 서비스 데이터소스 조회 (없으면 환경변수에서 생성 시도)
     */
    public DataSource getServiceDataSource(String serviceId) {
        DataSource dataSource = serviceDataSources.get(serviceId);
        
        if (dataSource == null) {
            log.info("Service data source not found for '{}', trying to create from environment...", serviceId);
            if (tryCreateServiceFromEnvironment(serviceId)) {
                dataSource = serviceDataSources.get(serviceId);
            }
        }

        if (dataSource == null) {
            log.warn("No data source available for service '{}', using integrated_cms as fallback", serviceId);
            return serviceDataSources.get("integrated_cms");
        }

        return dataSource;
    }

    /**
     * 모든 서비스 데이터소스 조회
     */
    public Map<String, DataSource> getAllServiceDataSources() {
        return new ConcurrentHashMap<>(serviceDataSources);
    }

    /**
     * 서비스 데이터소스 제거
     */
    public void removeServiceDataSource(String serviceId) {
        if ("integrated_cms".equals(serviceId)) {
            log.warn("Cannot remove integrated_cms data source");
            return;
        }

        DataSource dataSource = serviceDataSources.remove(serviceId);
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            log.info("🗑️ Removed data source for service: {}", serviceId);
        }
    }

    /**
     * 서비스 데이터소스 상태 정보
     */
    public Map<String, String> getServiceDataSourceInfo() {
        Map<String, String> info = new ConcurrentHashMap<>();
        serviceDataSources.forEach((serviceId, dataSource) -> {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDS = (HikariDataSource) dataSource;
                try {
                    info.put(serviceId, String.format("Pool: %s, Active: %d, Idle: %d, Total: %d",
                            hikariDS.getPoolName(),
                            hikariDS.getHikariPoolMXBean().getActiveConnections(),
                            hikariDS.getHikariPoolMXBean().getIdleConnections(),
                            hikariDS.getHikariPoolMXBean().getTotalConnections()));
                } catch (Exception e) {
                    info.put(serviceId, "Status unavailable: " + e.getMessage());
                }
            } else {
                info.put(serviceId, "Non-Hikari DataSource");
            }
        });
        return info;
    }
}
