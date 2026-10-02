package vn.edu.ute.sportute.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            // Nếu là quản trị hoặc nhân viên, chuyển về admin dashboard
            if (role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER") || role.equals("ROLE_STAFF")) {
                response.sendRedirect("/admin/dashboard");
                return;
            }
        }

        // Khách hàng mặc định (ROLE_CUSTOMER) chuyển thẳng đến trang đặt sân trực tuyến
        response.sendRedirect("/customer/booking");
    }
}