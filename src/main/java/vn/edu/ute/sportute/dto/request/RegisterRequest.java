package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Pattern(
        regexp = "^[\\p{L}\\s]{2,50}$",
        message = "Họ và tên chỉ được chứa chữ cái và tối thiểu 2 ký tự"
    )
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không hợp lệ (Ví dụ: 0912345678)")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có tối thiểu 8 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu")
    private String confirmPassword;

    public RegisterRequest() {
    }

    public RegisterRequest(String fullName, String phone, String email, String password) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.password = password;
    }

    public RegisterRequest(String fullName, String phone, String email, String password, String confirmPassword) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }
}
