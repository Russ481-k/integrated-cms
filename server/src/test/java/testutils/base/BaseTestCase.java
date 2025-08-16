package testutils.base;

import api.v2.common.config.ServiceContextHolder;
import org.junit.jupiter.api.AfterEach;

/**
 * 모든 테스트의 기본 클래스
 * 
 * 공통 설정과 정리 작업을 제공하여 테스트 코드의 일관성을 보장합니다.
 * TDD 커서룰에 따른 표준 테스트 구조를 제공합니다.
 */
public abstract class BaseTestCase {

    /**
     * 각 테스트 후 공통 정리 작업
     * - ServiceContext 정리 (메모리 누수 방지)
     * - 기타 ThreadLocal 정리
     */
    @AfterEach
    void tearDown() {
        // ServiceContext 정리 (모든 테스트에서 필수)
        ServiceContextHolder.clear();

        // 추가 정리 작업이 필요한 경우 하위 클래스에서 구현
        additionalTearDown();
    }

    /**
     * 하위 클래스에서 추가 정리 작업을 구현할 수 있는 훅 메서드
     */
    protected void additionalTearDown() {
        // 기본 구현은 비어있음 - 필요시 하위 클래스에서 오버라이드
    }

    /**
     * Java 8 호환 문자열 반복 유틸리티 (표준)
     * 모든 테스트 클래스에서 일관되게 사용
     */
    protected String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
