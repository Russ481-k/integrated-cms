package testutils.logging;

/**
 * 통합 CMS v2 테스트 로깅 유틸리티
 * 
 * 모든 테스트에서 일관된 로깅 스타일을 제공하는 중앙화된 유틸리티입니다.
 * TDD 커서룰에 따른 표준 로깅 컨벤션을 준수합니다.
 */
public final class TestLoggingUtils {

    // 색상 정의 (ANSI 컬러 코드)
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";

    // 테스트 관련 색상
    private static final String TEST_HEADER = "\033[1;96m"; // Bright Cyan, Bold
    private static final String SUCCESS = "\033[32m"; // Green
    private static final String ERROR = "\033[31m"; // Red
    private static final String WARNING = "\033[33m"; // Yellow
    private static final String INFO = "\033[36m"; // Cyan
    private static final String INTEGRATED = "\033[35m"; // Magenta
    private static final String GRAY = "\033[90m"; // Gray

    // 이모지
    private static final String TEST_EMOJI = "🧪";
    private static final String SETUP_EMOJI = "🔍";
    private static final String ACTION_EMOJI = "⚡";
    private static final String VERIFY_EMOJI = "✨";
    private static final String SUCCESS_EMOJI = "✓";
    private static final String ERROR_EMOJI = "✗";

    // Java 8 호환 문자열 반복 유틸리티 (표준)
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 테스트 헤더 출력 (표준 포맷)
     * 
     * @param testNumber  테스트 번호
     * @param description 영어 설명
     */
    public static void printTestHeader(int testNumber, String description) {
        System.out.println("\n" + TEST_HEADER + TEST_EMOJI + " TEST #" + testNumber + RESET +
                " " + GRAY + "│" + RESET + " " + BOLD + description + RESET);
    }

    /**
     * Given 단계 출력
     * 
     * @param koreanDescription 한국어 설명
     */
    public static void printGiven(String koreanDescription) {
        System.out.println("  " + DIM + SETUP_EMOJI + " Setup:" + RESET + " " + koreanDescription);
    }

    /**
     * Given 단계 상세 정보 출력
     * 
     * @param detail 상세 정보
     */
    public static void printGivenDetail(String detail) {
        System.out.println("    " + GRAY + "→" + RESET + " " + detail);
    }

    /**
     * When 단계 출력
     * 
     * @param koreanDescription 한국어 설명
     */
    public static void printWhen(String koreanDescription) {
        System.out.println("  " + DIM + ACTION_EMOJI + " Action:" + RESET + " " + koreanDescription);
    }

    /**
     * When 단계 메서드 호출 출력
     * 
     * @param methodCall 메서드 호출 정보
     */
    public static void printWhenMethodCall(String methodCall) {
        System.out.println("    " + GRAY + "→" + RESET + " " + methodCall);
    }

    /**
     * Then 단계 출력
     * 
     * @param koreanDescription 한국어 설명
     */
    public static void printThen(String koreanDescription) {
        System.out.println("  " + DIM + VERIFY_EMOJI + " Verify:" + RESET + " " + koreanDescription);
    }

    /**
     * 성공한 assertion 출력
     * 
     * @param message 성공 메시지
     */
    public static void printAssertionSuccess(String message) {
        System.out.println("    " + SUCCESS + SUCCESS_EMOJI + RESET + " " + GRAY + "Assertion passed:" + RESET +
                " " + SUCCESS + message + RESET + "\n");
    }

    /**
     * 실패한 assertion 출력
     * 
     * @param message 실패 메시지
     */
    public static void printAssertionFailure(String message) {
        System.out.println("    " + ERROR + ERROR_EMOJI + RESET + " " + GRAY + "Assertion failed:" + RESET +
                " " + ERROR + message + RESET + "\n");
    }

    /**
     * 예외 정보 출력
     * 
     * @param exceptionClass 예외 클래스
     * @param message        예외 메시지
     */
    public static void printExceptionInfo(String exceptionClass, String message) {
        System.out.println("    " + WARNING + "⚠️" + RESET + " Exception thrown: " + ERROR + exceptionClass + RESET);
        System.out.println("    " + GRAY + "📝" + RESET + " Message: " + ERROR + "'" + message + "'" + RESET);
    }

    /**
     * 동시성 테스트용 헤더 (특별 포맷)
     * 
     * @param testNumber  테스트 번호
     * @param description 한국어 설명
     */
    public static void printConcurrencyTestHeader(int testNumber, String description) {
        System.out.println("\n" + repeat("=", 80));
        System.out.println(TEST_EMOJI + "  TEST #" + testNumber + " │ " + TEST_HEADER + description + RESET);
        System.out.println(repeat("=", 80));
    }

    /**
     * 동시성 테스트 결과 출력
     * 
     * @param message 결과 메시지
     */
    public static void printConcurrencyResult(String message) {
        System.out.println("🎯 RESULT   │ " + BOLD + SUCCESS + message + RESET);
        System.out.println(repeat("=", 80) + "\n");
    }

    /**
     * 컬러가 적용된 값 출력 유틸리티
     * 
     * @param value 값
     * @return 컬러가 적용된 문자열
     */
    public static String colorValue(String value) {
        return INFO + "'" + value + "'" + RESET;
    }

    /**
     * integrated_cms 서비스 전용 컬러
     * 
     * @param value 값
     * @return 컬러가 적용된 문자열
     */
    public static String colorIntegratedService(String value) {
        return INTEGRATED + value + RESET;
    }

    /**
     * 일반 서비스 전용 컬러
     * 
     * @param value 값
     * @return 컬러가 적용된 문자열
     */
    public static String colorService(String value) {
        return INFO + value + RESET;
    }
}
