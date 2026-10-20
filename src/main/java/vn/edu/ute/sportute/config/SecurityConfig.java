package vn.edu.ute.sportute.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import vn.edu.ute.sportute.security.CustomAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;
    private final vn.edu.ute.sportute.security.CustomAuthenticationFailureHandler failureHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler,
                          vn.edu.ute.sportute.security.CustomAuthenticationFailureHandler failureHandler) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/admin/bookings/api/**", "/customer/booking/api/**", "/customer/community/api/**", "/customer/api/**", "/admin/services/api/**", "/admin/permissions/**", "/v3/api-docs/**", "/swagger-ui/**")) 
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/customer/password/change").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/customer/community/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN", "ROLE_MANAGER")
                .requestMatchers("/customer/community/mine").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN", "ROLE_MANAGER")
                .requestMatchers("/favicon.ico", "/favicon.png", "/login", "/register/**", "/forgot-password/**", "/otp-action", "/auth/**", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**", "/customer/api/avatar/**", "/api/chat", "/customer/api/chat", "/customer/community/**", "/v3/api-docs/**", "/v3/api-docs", "/swagger-ui/**", "/swagger-ui", "/swagger-ui.html").permitAll()
                .requestMatchers("/customer/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN", "ROLE_MANAGER")
                // Chỉ Quản trị viên và Quản lý mới có quyền vào quản lý môn thể thao, nhân sự, báo cáo, giá và dịch vụ
                .requestMatchers("/admin/sports/**", "/admin/employees/**", "/admin/reports/**", "/admin/prices/**", "/admin/policies/**", "/admin/services/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                .requestMatchers("/admin/permissions/**", "/admin/accounts/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/admin/payments/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_RECEPTIONIST")
                .requestMatchers("/admin/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_OPERATOR")
                .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_RECEPTIONIST", "ROLE_OPERATOR", "ROLE_STAFF")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
