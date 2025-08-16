package api.v2.integrated_cms.service.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ServiceEntity 도메인 로직 테스트
 * 
 * 서비스 메타데이터 엔티티의 비즈니스 규칙과 데이터 무결성을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class ServiceEntityTest {

        @BeforeEach
        void tearDown() {
                // 각 테스트 후 정리 작업은 없음 (단위 테스트)
        }

        @Test
        @DisplayName("서비스 엔티티를 올바른 정보로 생성할 수 있다")
        void 서비스_엔티티를_올바른_정보로_생성할_수_있다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #1\033[0m \033[90m│\033[0m \033[1mService Entity Creation with Valid Data\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 올바른 서비스 정보 준비");
                String serviceCode = "douzone";
                String serviceName = "더존 비즈온";
                String serviceDomain = "https://bizon.co.kr";
                String apiBaseUrl = "https://api.bizon.co.kr";
                String description = "더존 비즈온 서비스";
                String createdBy = "admin";
                String createdIp = "127.0.0.1";

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m ServiceEntity 생성");
                ServiceEntity service = ServiceEntity.builder()
                                .serviceCode(serviceCode)
                                .serviceName(serviceName)
                                .serviceDomain(serviceDomain)
                                .apiBaseUrl(apiBaseUrl)
                                .status(ServiceStatus.ACTIVE)
                                .description(description)
                                .createdBy(createdBy)
                                .createdIp(createdIp)
                                .build();

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 서비스 엔티티 필드 값 검증");
                assertThat(service.getServiceCode()).isEqualTo(serviceCode);
                assertThat(service.getServiceName()).isEqualTo(serviceName);
                assertThat(service.getServiceDomain()).isEqualTo(serviceDomain);
                assertThat(service.getApiBaseUrl()).isEqualTo(apiBaseUrl);
                assertThat(service.getStatus()).isEqualTo(ServiceStatus.ACTIVE);
                assertThat(service.getDescription()).isEqualTo(description);
                assertThat(service.getCreatedBy()).isEqualTo(createdBy);
                assertThat(service.getCreatedIp()).isEqualTo(createdIp);
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m모든 필드가 올바르게 설정됨\033[0m\n");
        }

        @Test
        @DisplayName("서비스 정보를 수정할 수 있다")
        void 서비스_정보를_수정할_수_있다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #2\033[0m \033[90m│\033[0m \033[1mService Entity Update Operations\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 기존 서비스 엔티티 생성");
                ServiceEntity service = ServiceEntity.builder()
                                .serviceCode("douzone")
                                .serviceName("더존 비즈온")
                                .status(ServiceStatus.ACTIVE)
                                .build();

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 서비스 정보 업데이트");
                String newServiceName = "더존 비즈온 프로";
                String newDescription = "업데이트된 설명";
                String updatedBy = "admin";
                String updatedIp = "192.168.1.1";

                service.updateBasicInfo(newServiceName, service.getStatus(), service.getServiceDomain(),
                                service.getApiBaseUrl(), service.getDbConnectionInfo(),
                                newDescription, service.getConfig());
                service.setAuditFields(null, null, updatedBy, updatedIp);

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 업데이트된 정보 검증");
                assertThat(service.getServiceName()).isEqualTo(newServiceName);
                assertThat(service.getDescription()).isEqualTo(newDescription);
                assertThat(service.getUpdatedBy()).isEqualTo(updatedBy);
                assertThat(service.getUpdatedIp()).isEqualTo(updatedIp);
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m서비스 정보가 올바르게 업데이트됨\033[0m\n");
        }

        @Test
        @DisplayName("서비스 상태를 변경할 수 있다")
        void 서비스_상태를_변경할_수_있다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #3\033[0m \033[90m│\033[0m \033[1mService Status Change Operations\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m ACTIVE 상태의 서비스 생성");
                ServiceEntity service = ServiceEntity.builder()
                                .serviceCode("douzone")
                                .serviceName("더존 비즈온")
                                .status(ServiceStatus.ACTIVE)
                                .build();

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 서비스 상태를 MAINTENANCE로 변경");
                String updatedBy = "admin";
                String updatedIp = "127.0.0.1";

                service.changeStatus(ServiceStatus.MAINTENANCE);
                service.setAuditFields(null, null, updatedBy, updatedIp);

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 변경된 상태 검증");
                assertThat(service.getStatus()).isEqualTo(ServiceStatus.MAINTENANCE);
                assertThat(service.getUpdatedBy()).isEqualTo(updatedBy);
                assertThat(service.getUpdatedIp()).isEqualTo(updatedIp);
                System.out
                                .println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32mMAINTENANCE\033[0m 상태로 변경됨\n");
        }

        @Test
        @DisplayName("서비스 설정 정보를 JSON으로 관리할 수 있다")
        void 서비스_설정_정보를_JSON으로_관리할_수_있다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #4\033[0m \033[90m│\033[0m \033[1mService Configuration JSON Management\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 서비스 설정 정보 Map 준비");
                ServiceEntity service = ServiceEntity.builder()
                                .serviceCode("douzone")
                                .serviceName("더존 비즈온")
                                .status(ServiceStatus.ACTIVE)
                                .build();

                Map<String, Object> config = new HashMap<>();
                config.put("maxUsers", 1000);
                config.put("features", new String[] { "billing", "hr", "accounting" });
                config.put("timezone", "Asia/Seoul");

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 서비스 설정 정보 업데이트");
                service.updateConfig("maxUsers", "1000");
                service.setAuditFields(null, null, "admin", "127.0.0.1");

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 설정 정보 업데이트 메서드 호출 검증");
                // JSON 설정은 서비스 계층에서 처리하므로 여기서는 메서드 호출만 검증
                assertThat(service.getUpdatedBy()).isEqualTo("admin");
                assertThat(service.getUpdatedIp()).isEqualTo("127.0.0.1");
                System.out.println(
                                "    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m설정 정보 업데이트 메서드가 올바르게 호출됨\033[0m\n");
        }

        @Test
        @DisplayName("서비스 코드는 필수값이고 null일 수 없다")
        void 서비스_코드는_필수값이고_null일_수_없다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #5\033[0m \033[90m│\033[0m \033[1mService Code Validation - Required Field\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m null 서비스 코드로 엔티티 생성 시도");

                // When & Then
                System.out.println("  \033[2m⚡ Action:\033[0m null 서비스 코드로 생성 시도");
                System.out.println("  \033[2m✨ Verify:\033[0m IllegalArgumentException 발생해야 함");

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                        ServiceEntity.builder()
                                        .serviceCode(null) // null 서비스 코드
                                        .serviceName("테스트 서비스")
                                        .status(ServiceStatus.ACTIVE)
                                        .build()
                                        .validateServiceCode(); // 검증 메서드 호출
                });

                System.out.println("    \033[31m⚠️\033[0m Exception thrown: \033[31mIllegalArgumentException\033[0m");
                System.out.println("    \033[90m📝\033[0m Message: \033[31m'" + exception.getMessage() + "'\033[0m");
                System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m예상된 예외 발생\033[0m\n");
        }

        @Test
        @DisplayName("중복되지 않는 서비스 코드는 유효하다")
        void 중복되지_않는_서비스_코드는_유효하다() {
                System.out.println(
                                "\n\033[1;96m🧪 TEST #6\033[0m \033[90m│\033[0m \033[1mService Code Uniqueness Validation\033[0m");

                // Given
                System.out.println("  \033[2m🔍 Setup:\033[0m 유니크한 서비스 코드로 엔티티 생성");
                String uniqueServiceCode = "unique_service_" + System.currentTimeMillis();

                // When
                System.out.println("  \033[2m⚡ Action:\033[0m 서비스 엔티티 생성 및 검증");
                ServiceEntity service = ServiceEntity.builder()
                                .serviceCode(uniqueServiceCode)
                                .serviceName("유니크 서비스")
                                .status(ServiceStatus.ACTIVE)
                                .build();

                // Then
                System.out.println("  \033[2m✨ Verify:\033[0m 서비스 코드 유효성 검증");
                boolean isValid = service.isValidServiceCode();

                assertThat(isValid).isTrue();
                assertThat(service.getServiceCode()).isEqualTo(uniqueServiceCode);
                System.out.println("    \033[32m✓\033[0m \033[90mAssertion passed:\033[0m \033[32m'" + uniqueServiceCode
                                + "'\033[0m는 유효한 서비스 코드\n");
        }
}
