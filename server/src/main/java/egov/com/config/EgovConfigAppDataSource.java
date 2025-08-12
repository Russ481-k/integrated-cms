package egov.com.config;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * @ClassName : EgovConfigAppDataSource.java
 * @Description : DataSource 설정 (통합)
 *
 * @author : 윤주호
 * @since : 2021. 7. 20
 * @version : 1.0
 *
 *          <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일              수정자               수정내용
 *  -------------  ------------   ---------------------
 *   2021. 7. 20    윤주호               최초 생성
 *   2025. 5. 29    통합                 두 설정 파일 통합
 *          </pre>
 *
 */
@Configuration
public class EgovConfigAppDataSource {
    private static final Logger logger = LoggerFactory.getLogger(EgovConfigAppDataSource.class);

    private String dbType;
    private Map<String, String> integratedDbConfig = new HashMap<>();
    private Map<String, String> cmsDbConfig = new HashMap<>();

    @PostConstruct
    void init() {
        try {
            // Load .env file first
            logger.info("Loading .env file from: {}", System.getProperty("user.dir"));
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();

            // 통합 CMS 데이터베이스 설정 (기본값 설정)
            String integratedUrl = dotenv.get("INTEGRATED_CMS_DATASOURCE_URL");
            String integratedUsername = dotenv.get("INTEGRATED_DB_USERNAME");
            String integratedPassword = dotenv.get("INTEGRATED_DB_PASSWORD");

            // 환경변수 필수 체크 - 보안을 위해 기본값 제공하지 않음
            if (integratedUrl == null || integratedUsername == null || integratedPassword == null) {
                logger.error("Integrated CMS database configuration is missing in .env file");
                logger.error(
                        "Required environment variables: INTEGRATED_CMS_DATASOURCE_URL, INTEGRATED_DB_USERNAME, INTEGRATED_DB_PASSWORD");
                throw new RuntimeException("Integrated CMS database configuration is missing. Please check .env file.");
            }

            integratedDbConfig.put("url", integratedUrl);
            integratedDbConfig.put("username", integratedUsername);
            integratedDbConfig.put("password", integratedPassword);

            // 개별 CMS 데이터베이스 설정 (DOUZONE)
            String cmsUrl = dotenv.get("DOUZONE_DATASOURCE_URL");
            String cmsUsername = dotenv.get("DOUZONE_DB_USERNAME");
            String cmsPassword = dotenv.get("DOUZONE_DB_PASSWORD");

            // 환경변수 필수 체크 - 보안을 위해 기본값 제공하지 않음
            if (cmsUrl == null || cmsUsername == null || cmsPassword == null) {
                logger.error("CMS database configuration is missing in .env file");
                logger.error(
                        "Required environment variables: DOUZONE_DATASOURCE_URL, DOUZONE_DB_USERNAME, DOUZONE_DB_PASSWORD");
                throw new RuntimeException("CMS database configuration is missing. Please check .env file.");
            }

            cmsDbConfig.put("url", cmsUrl);
            cmsDbConfig.put("username", cmsUsername);
            cmsDbConfig.put("password", cmsPassword);

            this.dbType = "mariadb";

            logger.info("Integrated CMS DB configuration loaded - URL: {}, Username: {}",
                    integratedDbConfig.get("url"), integratedDbConfig.get("username"));
            logger.info("CMS DB configuration loaded - URL: {}, Username: {}",
                    cmsDbConfig.get("url"), cmsDbConfig.get("username"));
        } catch (Exception e) {
            logger.error("Error loading environment variables from .env file", e);

            // .env 파일 로딩 실패 시 시스템 환경변수에서 직접 읽기 시도
            logger.info("Attempting to load configuration from system environment variables");

            String fallbackIntegratedUrl = System.getenv("INTEGRATED_CMS_DATASOURCE_URL");
            String fallbackIntegratedUsername = System.getenv("INTEGRATED_DB_USERNAME");
            String fallbackIntegratedPassword = System.getenv("INTEGRATED_DB_PASSWORD");

            String fallbackCmsUrl = System.getenv("DOUZONE_DATASOURCE_URL");
            String fallbackCmsUsername = System.getenv("DOUZONE_DB_USERNAME");
            String fallbackCmsPassword = System.getenv("DOUZONE_DB_PASSWORD");

            // 통합 CMS 환경변수 필수 체크
            if (fallbackIntegratedUrl == null || fallbackIntegratedUsername == null
                    || fallbackIntegratedPassword == null) {
                logger.error("Integrated CMS database configuration is missing in system environment variables");
                logger.error(
                        "Required environment variables: INTEGRATED_CMS_DATASOURCE_URL, INTEGRATED_DB_USERNAME, INTEGRATED_DB_PASSWORD");
                throw new RuntimeException("Database configuration is missing. Please check environment variables.");
            }

            // 개별 CMS 환경변수 필수 체크
            if (fallbackCmsUrl == null || fallbackCmsUsername == null || fallbackCmsPassword == null) {
                logger.error("CMS database configuration is missing in system environment variables");
                logger.error(
                        "Required environment variables: DOUZONE_DATASOURCE_URL, DOUZONE_DB_USERNAME, DOUZONE_DB_PASSWORD");
                throw new RuntimeException("Database configuration is missing. Please check environment variables.");
            }

            // 환경변수가 모두 존재할 때만 설정
            integratedDbConfig.put("url", fallbackIntegratedUrl);
            integratedDbConfig.put("username", fallbackIntegratedUsername);
            integratedDbConfig.put("password", fallbackIntegratedPassword);

            cmsDbConfig.put("url", fallbackCmsUrl);
            cmsDbConfig.put("username", fallbackCmsUsername);
            cmsDbConfig.put("password", fallbackCmsPassword);

            this.dbType = "mariadb";

            logger.info("Successfully loaded database configuration from system environment variables");
        }
    }

    /**
     * @return [dataSource 설정] HSQL 설정
     */
    private DataSource dataSourceHSQL() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.HSQL)
                .setScriptEncoding("UTF8")
                .addScript("classpath:/db/shtdb.sql")
                .build();
    }

    /**
     * 통합 CMS 데이터소스 (모든 데이터베이스 접근 가능)
     */
    @Bean(name = "integratedDataSource")
    public DataSource integratedDataSource() {
        logger.info("Creating Integrated CMS DataSource");
        return createDataSource(integratedDbConfig, "integrated-pool", 20);
    }

    /**
     * 개별 CMS 데이터소스 (해당 CMS 데이터베이스만 접근 가능)
     */
    @Bean(name = "cmsDataSource")
    public DataSource cmsDataSource() {
        logger.info("Creating CMS DataSource");
        return createDataSource(cmsDbConfig, "cms-pool", 50);
    }

    /**
     * Primary DataSource - 동적 라우팅 데이터소스 사용
     * 주의: DynamicDataSourceConfiguration이 우선 적용되므로
     * 이 빈은 fallback으로만 사용됩니다.
     */
    @Bean(name = { "egov.dataSource", "egovDataSource", "dataSource" })
    public DataSource egovDataSource() {
        logger.info("Creating Egov DataSource with dbType: {}", dbType);
        if ("hsql".equals(dbType)) {
            return dataSourceHSQL();
        } else {
            // 기본적으로 통합 CMS 데이터소스를 사용
            return integratedDataSource();
        }
    }

    /**
     * 공통 데이터소스 생성 메서드
     */
    private DataSource createDataSource(Map<String, String> config, String poolName, int maxPoolSize) {
        String url = config.get("url");
        String username = config.get("username");
        String password = config.get("password");

        if (url == null || url.trim().isEmpty() || username == null || username.trim().isEmpty()) {
            logger.error("Database configuration incomplete for pool: {} - URL: {}, Username: {}", poolName, url,
                    username);
            throw new RuntimeException("Database configuration incomplete for pool: " + poolName);
        }

        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password != null ? password : "");
            hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
            hikariConfig.setPoolName(poolName);

            // HikariCP 추가 설정
            hikariConfig.setMaximumPoolSize(maxPoolSize);
            hikariConfig.setMinimumIdle(5);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setLeakDetectionThreshold(60000);

            // 성능 최적화 설정
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

            logger.info("Creating HikariDataSource for pool: {} with URL: {}", poolName, url);
            return new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            logger.error("Failed to create DataSource for pool: {}", poolName, e);
            throw new RuntimeException("Failed to create DataSource for pool: " + poolName, e);
        }
    }
}