package testutils.base;

import testutils.config.TestMailConfiguration;
import testutils.logging.TestLoggingUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트 기본 클래스
 * 
 * 전체 Spring 애플리케이션 컨텍스트를 로드하는 통합 테스트를 위한 표준 설정을 제공합니다.
 * - 실제 integrated_cms DB 연동
 * - 서비스 컨텍스트 라우팅 테스트
 * - 트랜잭션 자동 롤백
 * - 불필요한 로그 최소화
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
        "logging.level.org.springframework=WARN",
        "logging.level.org.hibernate=WARN",
        "logging.level.com.zaxxer.hikari=WARN",
        "logging.level.org.apache.tomcat=WARN",
        "logging.level.org.springframework.web=WARN",
        "logging.level.org.springframework.security=WARN",
        "spring.jpa.show-sql=false"
})
@Import(TestMailConfiguration.class)
public abstract class BaseIntegrationTest extends BaseTestCase {

    /**
     * 통합 테스트에서 사용할 테스트 번호 카운터
     */
    protected int testNumber = 1;

    /**
     * 다음 테스트 번호를 반환하고 증가
     */
    protected int nextTestNumber() {
        return testNumber++;
    }

    /**
     * 통합 테스트용 표준 테스트 헤더 출력
     * 
     * @param description 영어 설명
     */
    protected void printTestHeader(String description) {
        TestLoggingUtils.printTestHeader(nextTestNumber(), description);
    }

    /**
     * Given 단계 출력
     * 
     * @param koreanDescription 한국어 설명
     */
    protected void printGiven(String koreanDescription) {
        TestLoggingUtils.printGiven(koreanDescription);
    }

    /**
     * When 단계 출력
     * 
     * @param koreanDescription 한국어 설명
     */
    protected void printWhen(String koreanDescription) {
        TestLoggingUtils.printWhen(koreanDescription);
    }

    /**
     * Then 단계 출력
     * 
     * @param koreanDescription 한국어 설명
     */
    protected void printThen(String koreanDescription) {
        TestLoggingUtils.printThen(koreanDescription);
    }

    /**
     * 성공한 assertion 출력
     * 
     * @param message 성공 메시지
     */
    protected void printSuccess(String message) {
        TestLoggingUtils.printAssertionSuccess(message);
    }
}
