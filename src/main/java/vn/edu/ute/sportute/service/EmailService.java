package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.entity.Booking;
import java.math.BigDecimal;

public interface EmailService {

    void sendRegisterOtpEmail(String toEmail, String fullName, String otpCode);

    void sendForgotPasswordOtpEmail(String toEmail, String otpCode);

    void sendBookingTicketEmail(String toEmail, String customerName, Booking booking, BigDecimal depositPaid, BigDecimal remainingAtCounter);
}
