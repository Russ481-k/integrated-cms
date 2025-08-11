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

            // 환경변수가 없으면 기본값 사용
            if (integratedUrl == null || integratedUsername == null || integratedPassword == null) {
                logger.warn("Integrated CMS database configuration not found in .env, using defaults");
                integratedUrl = "jdbc:mariadb://db:3306/integrated_cms?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true";
                integratedUsername = "admin";
                integratedPassword = "admin123";
            }

            integratedDbConfig.put("url", integratedUrl);
            integratedDbConfig.put("username", integratedUsername);
            integratedDbConfig.put("password", integratedPassword);

            // 개별 CMS 데이터베이스 설정 (기본값 설정)
            String cmsUrl = dotenv.get("CMS_DATASOURCE_URL");
            String cmsUsername = dotenv.get("CMS_DB_USERNAME");
            String cmsPassword = dotenv.get("CMS_DB_PASSWORD");

            // 환경변수가 없으면 기본값 사용
            if (cmsUrl == null || cmsUsername == null || cmsPassword == null) {
                logger.warn("CMS database configuration not found in .env, using defaults");
                cmsUrl = "jdbc:mariadb://db:3306/cms?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true";
                cmsUsername = "admin";
                cmsPassword = "admin123";
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
            logger.error("Error loading environment variables", e);
            // 환경변수 로딩 실패 시 기본값으로 설정
            logger.warn("Using fallback database configuration");

            integratedDbConfig.put("url",
                    "jdbc:mariadb://db:3306/integrated_cms?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true");
            integratedDbConfig.put("username", "admin");
            integratedDbConfig.put("password", "admin123");

            cmsDbConfig.put("url",
                    "jdbc:mariadb://db:3306/cms?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true");
            cmsDbConfig.put("username", "admin");
            cmsDbConfig.put("password", "admin123");

            this.dbType = "mariadb";
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
     * Primary DataSource - 기본적으로 통합 CMS 데이터소스 사용
     */
    @Bean(name = { "dataSource", "egov.dataSource", "egovDataSource" })
    @Primary
    public DataSource dataSource() {
        logger.info("Creating Primary DataSource with dbType: {}", dbType);
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