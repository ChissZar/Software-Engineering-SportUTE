package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;

public class VerifyOtpRequest {

    private String email;

    @NotBlank(message = "Vui lòng nhập mã OTP gồm 6 chữ số")
    private String otp;

    public VerifyOtpRequest() {
    }

    public VerifyOtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
