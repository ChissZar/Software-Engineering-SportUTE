package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Vui lòng nhập Email hoặc Tên đăng nhập")
    private String usernameOrEmail;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;

    private boolean rememberMe;

    public LoginRequest() {
    }

    public LoginRequest(String usernameOrEmail, String password, boolean rememberMe) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
        this.rememberMe = rememberMe;
    }
}
