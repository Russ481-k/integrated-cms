package api.v2.cms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 동적 DataSource 라우팅 설정
 * 
 * integrated_cms와 각 서비스별 DB를 위한 DataSource 설정
 * ServiceContextHolder 기반으로 동적 라우팅 수행
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Configuration
public class RoutingDataSourceConfig {

    // 통합 CMS DB 설정
    @Value("${INTEGRATED_CMS_DATASOURCE_URL:jdbc:mariadb://db:3306/integrated_cms?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true}")
    private String integratedDbUrl;

    @Value("${INTEGRATED_DB_USERNAME}")
    private String integratedDbUsername;

    @Value("${INTEGRATED_DB_PASSWORD}")
    private String integratedDbPassword;

    // Douzone DB 설정
    @Value("${DOUZONE_DATASOURCE_URL:jdbc:mariadb://db:3306/douzone?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true}")
    private String douzoneDbUrl;

    @Value("${DOUZONE_DB_USERNAME}")
    private String douzoneDbUsername;

    @Value("${DOUZONE_DB_PASSWORD}")
    private String douzoneDbPassword;

    /**
     * 통합 CMS용 DataSource 생성
     */
    private DataSource createIntegratedCmsDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(integratedDbUrl);
        config.setUsername(integratedDbUsername);
        config.setPassword(integratedDbPassword);
        config.setDriverClassName("org.mariadb.jdbc.Driver");

        // Connection Pool 설정
        config.setPoolName("integrated-cms-pool");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        // 성능 최적화 설정
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        log.info("Creating integrated_cms DataSource with URL: {}", integratedDbUrl);
        return new HikariDataSource(config);
    }

    /**
     * Douzone 서비스용 DataSource 생성
     */
    private DataSource createDouzoneDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(douzoneDbUrl);
        config.setUsername(douzoneDbUsername);
        config.setPassword(douzoneDbPassword);
        config.setDriverClassName("org.mariadb.jdbc.Driver");

        // Connection Pool 설정
        config.setPoolName("douzone-pool");
        config.setMaximumPoolSize(15);
        config.setMinimumIdle(3);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        // 성능 최적화 설정
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "200");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        log.info("Creating douzone DataSource with URL: {}", douzoneDbUrl);
        return new HikariDataSource(config);
    }

    /**
     * 동적 라우팅 DataSource 설정
     */
    @Bean
    @Primary
    public DataSource routingDataSource() {
        ContextRoutingDataSource routingDataSource = new ContextRoutingDataSource();

        // Target DataSources 설정
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("integrated_cms", createIntegratedCmsDataSource());
        targetDataSources.put("douzone", createDouzoneDataSource());

        // 추가 서비스 DataSource는 여기에 추가
        // targetDataSources.put("service1", createService1DataSource());
        // targetDataSources.put("service2", createService2DataSource());

        routingDataSource.setTargetDataSources(targetDataSources);

        // 기본 DataSource 설정 (서비스 컨텍스트가 없을 때 사용)
        routingDataSource.setDefaultTargetDataSource(createIntegratedCmsDataSource());

        log.info("RoutingDataSource configured with {} target data sources",
                targetDataSources.size());

        return routingDataSource;
    }
}
