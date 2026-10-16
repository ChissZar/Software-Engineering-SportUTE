package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import vn.edu.ute.sportute.dto.request.RegisterRequest;
import vn.edu.ute.sportute.enums.OtpType;
import vn.edu.ute.sportute.service.OtpService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpServiceImpl implements OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private final SecureRandom secureRandom = new SecureRandom();

    private static class OtpEntry {
        final String otpCode;
        final LocalDateTime expiryTime;
        final Object data;

        OtpEntry(String otpCode, LocalDateTime expiryTime, Object data) {
            this.otpCode = otpCode;
            this.expiryTime = expiryTime;
            this.data = data;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }

    // Key format: email.toLowerCase() + ":" + type.name()
    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    private String buildKey(String email, OtpType type) {
        if (email == null) return ":" + type.name();
        return email.trim().toLowerCase() + ":" + type.name();
    }

    private String generateNumericOtp() {
        int code = secureRandom.nextInt(900000) + 100000; // Đảm bảo luôn đủ 6 chữ số từ 100000 đến 999999
        return String.valueOf(code);
    }

    @Override
    public String generateAndSaveRegisterOtp(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail();
        String otpCode = generateNumericOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpStorage.put(buildKey(email, OtpType.REGISTER), new OtpEntry(otpCode, expiry, registerRequest));
        return otpCode;
    }

    @Override
    public RegisterRequest getPendingRegistration(String email) {
        OtpEntry entry = otpStorage.get(buildKey(email, OtpType.REGISTER));
        if (entry != null && !entry.isExpired() && entry.data instanceof RegisterRequest) {
            return (RegisterRequest) entry.data;
        }
        return null;
    }

    @Override
    public boolean verifyRegisterOtp(String email, String otpCode) {
        String key = buildKey(email, OtpType.REGISTER);
        OtpEntry entry = otpStorage.get(key);
        if (entry == null || entry.isExpired()) {
            return false;
        }
        return entry.otpCode.trim().equals(otpCode != null ? otpCode.trim() : "");
    }

    @Override
    public String generateAndSaveForgotPasswordOtp(String email) {
        String otpCode = generateNumericOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpStorage.put(buildKey(email, OtpType.FORGOT_PASSWORD), new OtpEntry(otpCode, expiry, email));
        return otpCode;
    }

    @Override
    public boolean verifyForgotPasswordOtp(String email, String otpCode) {
        String key = buildKey(email, OtpType.FORGOT_PASSWORD);
        OtpEntry entry = otpStorage.get(key);
        if (entry == null || entry.isExpired()) {
            return false;
        }
        return entry.otpCode.trim().equals(otpCode != null ? otpCode.trim() : "");
    }

    @Override
    public void clearOtp(String email, OtpType type) {
        otpStorage.remove(buildKey(email, type));
    }

    @Override
    public String getActiveOtpForTesting(String email, OtpType type) {
        OtpEntry entry = otpStorage.get(buildKey(email, type));
        if (entry != null && !entry.isExpired()) {
            return entry.otpCode;
        }
        return null;
    }
}
