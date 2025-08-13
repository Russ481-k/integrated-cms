package api.v2.cms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 동적 멀티 테넌트 데이터소스 설정
 * - 런타임에 데이터소스 추가/제거 가능
 * - 테넌트별 독립적인 연결 풀 관리
 * - 장애 격리 및 성능 모니터링
 */
// @Configuration  // 🚨 일시적으로 비활성화 - RoutingDataSourceConfig와 충돌 방지
public class DynamicDataSourceConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourceConfiguration.class);

    @Bean
    @Primary
    public DataSource routingDataSource(DynamicDataSourceManager dataSourceManager) {
        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource();
        routingDataSource.setDataSourceManager(dataSourceManager);

        // 기본 데이터소스 설정 (통합 CMS)
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("integrated", dataSourceManager.createDefaultDataSource());

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(targetDataSources.get("integrated"));

        logger.info("Dynamic routing DataSource configured with default tenant: integrated");
        return routingDataSource;
    }

    /**
     * 동적 라우팅 데이터소스
     */
    public static class DynamicRoutingDataSource extends AbstractRoutingDataSource {
        private static final Logger logger = LoggerFactory.getLogger(DynamicRoutingDataSource.class);
        private DynamicDataSourceManager dataSourceManager;

        public void setDataSourceManager(DynamicDataSourceManager dataSourceManager) {
            this.dataSourceManager = dataSourceManager;
        }

        @Override
        protected Object determineCurrentLookupKey() {
            String tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null) {
                tenantId = "integrated"; // 기본 테넌트
            }

            // 동적으로 데이터소스가 추가된 경우 업데이트
            if (dataSourceManager != null && dataSourceManager.hasDataSource(tenantId)) {
                dataSourceManager.ensureDataSourceExists(tenantId);
            }

            logger.debug("Routing to tenant: {}", tenantId);
            return tenantId;
        }
    }

    /**
     * 동적 데이터소스 관리자
     */
    @Component
    public static class DynamicDataSourceManager {
        private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourceManager.class);
        private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

        @Value("${spring.datasource.url}")
        private String defaultUrl;

        @Value("${spring.datasource.username}")
        private String defaultUsername;

        @Value("${spring.datasource.password}")
        private String defaultPassword;

        /**
         * 기본 데이터소스 생성
         */
        public DataSource createDefaultDataSource() {
            return createDataSource("integrated", defaultUrl, defaultUsername, defaultPassword);
        }

        /**
         * 새 테넌트 데이터소스 생성
         */
        public DataSource createTenantDataSource(String tenantId, String url, String username, String password) {
            if (dataSources.containsKey(tenantId)) {
                logger.warn("DataSource for tenant {} already exists", tenantId);
                return dataSources.get(tenantId);
            }

            DataSource dataSource = createDataSource(tenantId, url, username, password);
            dataSources.put(tenantId, dataSource);

            logger.info("Created new DataSource for tenant: {}", tenantId);
            return dataSource;
        }

        /**
         * 데이터소스 생성 공통 메서드
         */
        private DataSource createDataSource(String tenantId, String url, String username, String password) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setPoolName(tenantId + "-pool");

            // 테넌트별 풀 설정
            if ("integrated".equals(tenantId)) {
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

            return new HikariDataSource(config);
        }

        /**
         * 데이터소스 존재 확인
         */
        public boolean hasDataSource(String tenantId) {
            return dataSources.containsKey(tenantId);
        }

        /**
         * 데이터소스 존재 보장
         */
        public void ensureDataSourceExists(String tenantId) {
            if (!hasDataSource(tenantId)) {
                // 환경변수에서 해당 테넌트의 설정을 찾아서 생성
                String url = System.getenv(tenantId.toUpperCase() + "_DATASOURCE_URL");
                String username = System.getenv(tenantId.toUpperCase() + "_DB_USERNAME");
                String password = System.getenv(tenantId.toUpperCase() + "_DB_PASSWORD");

                if (url != null && username != null && password != null) {
                    createTenantDataSource(tenantId, url, username, password);
                    logger.info("Dynamically created DataSource for tenant: {}", tenantId);
                } else {
                    logger.warn("Configuration not found for tenant: {}. Using default datasource.", tenantId);
                }
            }
        }

        /**
         * 시작 시 기존 테넌트 초기화
         */
        @PostConstruct
        public void initializeKnownTenants() {
            logger.info("Initializing known tenants from environment variables...");

            // douzone 테넌트 자동 초기화
            ensureDataSourceExists("douzone");

            // 추가 테넌트들도 환경변수가 있으면 자동 초기화
            String[] knownTenants = { "arpina", "test", "demo" };
            for (String tenant : knownTenants) {
                String url = System.getenv(tenant.toUpperCase() + "_DATASOURCE_URL");
                if (url != null) {
                    ensureDataSourceExists(tenant);
                }
            }

            logger.info("Tenant initialization completed. Active tenants: {}", dataSources.keySet());
        }

        /**
         * 데이터소스 제거
         */
        public void removeDataSource(String tenantId) {
            DataSource dataSource = dataSources.remove(tenantId);
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
                logger.info("Removed DataSource for tenant: {}", tenantId);
            }
        }

        /**
         * 모든 데이터소스 정보 조회
         */
        public Map<String, String> getDataSourceInfo() {
            Map<String, String> info = new HashMap<>();
            dataSources.forEach((tenantId, dataSource) -> {
                if (dataSource instanceof HikariDataSource) {
                    HikariDataSource hikariDS = (HikariDataSource) dataSource;
                    info.put(tenantId, String.format("Pool: %s, Active: %d, Idle: %d",
                            hikariDS.getPoolName(),
                            hikariDS.getHikariPoolMXBean().getActiveConnections(),
                            hikariDS.getHikariPoolMXBean().getIdleConnections()));
                }
            });
            return info;
        }
    }

    /**
     * 테넌트 컨텍스트 관리
     */
    public static class TenantContext {
        private static final ThreadLocal<String> TENANT_CONTEXT = new ThreadLocal<>();

        public static void setCurrentTenant(String tenantId) {
            TENANT_CONTEXT.set(tenantId);
        }

        public static String getCurrentTenant() {
            return TENANT_CONTEXT.get();
        }

        public static void clear() {
            TENANT_CONTEXT.remove();
        }
    }
}
