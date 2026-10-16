package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.RegisterRequest;
import vn.edu.ute.sportute.enums.OtpType;

public interface OtpService {

    String generateAndSaveRegisterOtp(RegisterRequest registerRequest);

    RegisterRequest getPendingRegistration(String email);

    boolean verifyRegisterOtp(String email, String otpCode);

    String generateAndSaveForgotPasswordOtp(String email);

    boolean verifyForgotPasswordOtp(String email, String otpCode);

    void clearOtp(String email, OtpType type);

    String getActiveOtpForTesting(String email, OtpType type);
}
