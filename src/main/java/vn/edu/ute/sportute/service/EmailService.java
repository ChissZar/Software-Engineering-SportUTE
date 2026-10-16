package vn.edu.ute.sportute.service;

public interface EmailService {

    void sendRegisterOtpEmail(String toEmail, String fullName, String otpCode);

    void sendForgotPasswordOtpEmail(String toEmail, String otpCode);
}
