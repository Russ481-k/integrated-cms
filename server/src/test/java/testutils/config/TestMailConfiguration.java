package testutils.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.mockito.Mockito;

/**
 * 테스트 환경용 메일 설정
 * JavaMailSender와 SpringTemplateEngine을 Mock으로 제공하여 테스트 환경에서 메일 의존성 문제를 해결합니다.
 */
@TestConfiguration
@Profile("test")
public class TestMailConfiguration {

    /**
     * 테스트용 Mock JavaMailSender Bean
     * 실제 메일 발송 없이 테스트가 진행됩니다.
     */
    @Bean
    @Primary
    public JavaMailSender mockJavaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }

    /**
     * 테스트용 Mock SpringTemplateEngine Bean
     * 실제 템플릿 엔진 없이 테스트가 진행됩니다.
     */
    @Bean
    @Primary
    public SpringTemplateEngine mockSpringTemplateEngine() {
        return Mockito.mock(SpringTemplateEngine.class);
    }
}
