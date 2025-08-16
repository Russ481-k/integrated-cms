package testutils.base;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import testutils.logging.TestLoggingUtils;

/**
 * 단위 테스트 기본 클래스
 * 
 * Mockito 기반 격리된 단위 테스트를 위한 표준 설정을 제공합니다.
 * - 외부 의존성 Mock 처리
 * - 빠른 실행 속도
 * - 표준 로깅 컨벤션 적용
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest extends BaseTestCase {

    /**
     * 단위 테스트에서 사용할 테스트 번호 카운터
     * 각 테스트 클래스에서 독립적으로 관리
     */
    protected int testNumber = 1;

    /**
     * 다음 테스트 번호를 반환하고 증가
     */
    protected int nextTestNumber() {
        return testNumber++;
    }

    /**
     * 단위 테스트용 표준 테스트 헤더 출력
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
