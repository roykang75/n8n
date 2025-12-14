package xyz.oiio.n8n.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import xyz.oiio.n8n.security.CustomUserDetailsService;
import xyz.oiio.n8n.security.JwtAuthenticationEntryPoint;
import xyz.oiio.n8n.security.JwtAuthenticationFilter;

import org.springframework.security.web.header.HeaderWriterFilter;

import java.util.Arrays;
import java.util.List;

@Profile({ "default", "dev" })
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 공개 엔드포인트
                        .requestMatchers("/rest/login", "/rest/register", "/rest/owner/setup", "/rest/refresh",
                                "/rest/logout", "/rest/forgot-password",
                                "/rest/resolve-signup-token", "/rest/settings",
                                "/rest/projects/**", "/rest/roles", "/rest/module-settings",
                                "/rest/workflows", "/rest/active-workflows", "/rest/license")
                        .permitAll()
                        .requestMatchers("/static/**").permitAll()
                        .requestMatchers("/rest/health").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/rest/public/**").permitAll()

                        // Actuator 엔드포인트 (프로덕션에서는 제한 가능)
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()

                        // 정적 리소스
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/webjars/**").permitAll()

                        // WebSocket 엔드포인트 (워크플로우 실행용)
                        .requestMatchers("/ws/**").permitAll()

                        // Swagger/Swagger UI (개발 환경)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 관리자 엔드포인트
                        .requestMatchers("/rest/admin/**").hasRole("ADMIN")

                        // 기타 모든 요청은 인증 필요
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 특정 오리진 허용 (프로덕션에서는 제한 필요)
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 특정 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 특정 헤더 허용
        configuration.setAllowedHeaders(List.of("*"));

        // 자격 증명 허용
        configuration.setAllowCredentials(true);

        // 특정 헤더 노출
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // 프리플라이트 요청 최대 시간
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

@Profile({ "no-auth", "test" })
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
class NoAuthSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}