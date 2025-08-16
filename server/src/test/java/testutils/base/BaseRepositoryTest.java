package testutils.base;

import testutils.config.TestMailConfiguration;
import testutils.logging.TestLoggingUtils;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Repository 계층 테스트 기본 클래스
 * 
 * JPA Repository 테스트를 위한 표준 설정을 제공합니다.
 * - 실제 DB 연동 테스트
 * - 트랜잭션 자동 롤백
 * - 메일 의존성 Mock 처리
 * - 표준 로깅 컨벤션 적용
 */
@DataJpaTest(showSql = false)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
        "logging.level.org.springframework=WARN",
        "logging.level.org.hibernate=WARN", 
        "logging.level.com.zaxxer.hikari=WARN"
})
@Import(TestMailConfiguration.class)
public abstract class BaseRepositoryTest extends BaseTestCase {

    /**
     * Repository 테스트에서 사용할 테스트 번호 카운터
     */
    protected int testNumber = 1;

    /**
     * 다음 테스트 번호를 반환하고 증가
     */
    protected int nextTestNumber() {
        return testNumber++;
    }

    /**
     * Repository 테스트용 표준 테스트 헤더 출력
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
