package api.v2.cms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import api.v2.cms.auth.service.CustomUserDetailsService;
import egov.com.jwt.JwtAuthenticationEntryPoint;
import egov.com.jwt.JwtRequestFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Arrays;

/**
 * 통합 CMS v2 하이브리드 보안 설정
 * 
 * 3계층 하이브리드 보안 전략:
 * 1. SecurityConfig: 기본 보안 정책 + 큰 도메인 분류
 * 2. 커스텀 어노테이션: 공통 비즈니스 권한 로직
 * 3. 컨트롤러: 세밀한 메서드별 제어
 * 
 * @author CMS Team
 * @since v2.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class UnifiedCmsSecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * v2 API 전용 보안 필터 체인
     * 하이브리드 계층 구조의 1계층: 큰 도메인 분류별 기본 권한
     */
    @Bean
    @Order(1)
    public SecurityFilterChain v2ApiSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring v2 API security filter chain with hybrid authentication");

        return http
                .requestMatcher(request -> request.getRequestURI().startsWith("/api/v2/"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 공개 API (인증 불필요)
                        .requestMatchers(new AntPathRequestMatcher("/api/v2/**", HttpMethod.OPTIONS.name())).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v2/auth/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v2/integrated-cms/auth/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v2/cms/**/auth/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v2/public/**")).permitAll()

                        // 통합 관리 영역 - SUPER_ADMIN, SERVICE_ADMIN만 접근
                        .requestMatchers(new AntPathRequestMatcher("/api/v2/integrated-cms/**"))
                        .hasAnyRole("SUPER_ADMIN", "SERVICE_ADMIN")

                        // 서비스별 CMS API - 계층적 권한 (세부 권한은 컨트롤러에서 제어)
                        .requestMatchers(new AntPathRequestMatcher("/api/v2/cms/**"))
                        .hasAnyRole("SUPER_ADMIN", "SERVICE_ADMIN", "SITE_ADMIN", "ADMIN")

                        // 나머지는 인증 필요 (세부 권한은 @PreAuthorize에서 제어)
                        .anyRequest().authenticated())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .build();
    }

    /**
     * 공통/관리 도구용 보안 필터 체인 (Actuator 등)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring management security filter chain");

        return http
                .requestMatcher(request -> request.getRequestURI().startsWith("/actuator/") ||
                        request.getRequestURI().startsWith("/swagger-ui/") ||
                        request.getRequestURI().startsWith("/v3/api-docs/"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    /**
     * 기존 v1 API 호환성을 위한 보안 필터 체인
     */
    @Bean
    @Order(3)
    public SecurityFilterChain legacyApiSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring legacy v1 API security filter chain");

        return http
                .requestMatcher(request -> request.getRequestURI().startsWith("/api/v1/"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // v1 공개 API
                        .requestMatchers(new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name())).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cms/menu/public/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cms/template/public")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cms/bbs/master")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cms/schedule/public/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cms/file/public/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cms/popups/active")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cms/bbs/**", HttpMethod.GET.name()))
                        .permitAll()

                        // v1 인증 필요 API
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cms/**")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/mypage/**")).hasRole("USER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}