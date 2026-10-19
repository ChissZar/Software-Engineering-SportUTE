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

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/admin/bookings/api/**", "/customer/booking/api/**", "/customer/api/**", "/admin/services/api/**", "/admin/permissions/**", "/v3/api-docs/**", "/swagger-ui/**")) 
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register/**", "/forgot-password/**", "/otp-action", "/auth/**", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**", "/customer/api/avatar/**", "/v3/api-docs/**", "/v3/api-docs", "/swagger-ui/**", "/swagger-ui", "/swagger-ui.html").permitAll()
                .requestMatchers("/customer/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN", "ROLE_MANAGER")
                .requestMatchers("/admin/services/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_RECEPTIONIST", "ROLE_OPERATOR", "ROLE_STAFF")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
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