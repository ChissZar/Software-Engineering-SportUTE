package vn.edu.ute.sportute.service.impl;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.ute.sportute.entity.Booking;
import vn.edu.ute.sportute.entity.BookingDetail;
import vn.edu.ute.sportute.service.EmailService;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Override
    public void sendRegisterOtpEmail(String toEmail, String fullName, String otpCode) {
        String recipientName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "Quý khách";
        String encodedEmail = URLEncoder.encode(toEmail, StandardCharsets.UTF_8);
        String actionUrl = baseUrl + "/otp-action?email=" + encodedEmail + "&otp=" + otpCode + "&type=register";

        String htmlContent = buildRegisterOtpHtml(recipientName, otpCode, actionUrl);
        String subject = "[SportHub] Mã xác thực đăng ký tài khoản: " + otpCode;

        sendHtmlEmail(toEmail, subject, htmlContent, otpCode);
    }

    @Override
    public void sendForgotPasswordOtpEmail(String toEmail, String otpCode) {
        String encodedEmail = URLEncoder.encode(toEmail, StandardCharsets.UTF_8);
        String actionUrl = baseUrl + "/otp-action?email=" + encodedEmail + "&otp=" + otpCode + "&type=forgot";

        String htmlContent = buildForgotPasswordOtpHtml(toEmail, otpCode, actionUrl);
        String subject = "[SportHub] Mã xác thực đặt lại mật khẩu: " + otpCode;

        sendHtmlEmail(toEmail, subject, htmlContent, otpCode);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent, String otpCode) {
        boolean sentSuccessfully = false;

        log.info("[SPORTHUB EMAIL SERVICE] Chuẩn bị gửi email: mailSender={}, mailFrom={}, mã OTP: [{}] tới: {}",
                (mailSender != null ? "Sẵn sàng" : "NULL"), mailFrom, otpCode, toEmail);

        if (mailSender != null && mailFrom != null && !mailFrom.isBlank()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(new InternetAddress(mailFrom.trim(), "SportHub", "UTF-8"));
                helper.setTo(toEmail.trim());
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                sentSuccessfully = true;
                log.info("[SPORTHUB EMAIL SERVICE] Gửi email thành công qua SMTP ({}) tới: {}", mailFrom, toEmail);
            } catch (Exception e) {
                log.error("[SPORTHUB EMAIL SERVICE] Gửi email qua SMTP ({}) thất bại: {}", mailFrom, e.getMessage(), e);
            }
        } else {
            log.warn(
                    "[SPORTHUB EMAIL SERVICE] Chưa cấu hình MAIL_USERNAME/MAIL_PASSWORD hoặc mailSender là NULL. Đang chạy chế độ ghi nhận nội bộ.");
        }

        if (!sentSuccessfully) {
            log.info("================================================================================");
            log.info("[SPORTHUB EMAIL SERVICE] TIÊU ĐỀ EMAIL: {}", subject);
            log.info("[SPORTHUB EMAIL SERVICE] GỬI TỚI: {}", toEmail);
            log.info("[SPORTHUB EMAIL SERVICE] MÃ XÁC THỰC OTP: >>> {} <<< (Hiệu lực: 5 phút)", otpCode);
            log.info("================================================================================");
        }
    }

    private String buildRegisterOtpHtml(String recipientName, String otpCode, String verifyUrl) {
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head><meta charset=\"UTF-8\"><title>Mã OTP SportHub</title></head>"
                + "<body style=\"margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#1e293b;\">"
                + "  <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color:#f1f5f9;padding:40px 15px;\">"
                + "    <tr><td align=\"center\">"
                + "      <table width=\"100%\" max-width=\"560px\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width:560px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.06);border:1px solid #e2e8f0;\">"
                + "        <tr>"
                + "          <td style=\"background:linear-gradient(135deg,#064e3b 0%,#059669 100%);padding:30px 24px;text-align:center;\">"
                + "            <div style=\"font-size:26px;font-weight:900;color:#ffffff;letter-spacing:1px;\">SPORT<span style=\"color:#6ee7b7;\">HUB</span></div>"
                + "            <div style=\"font-size:10.5px;font-weight:800;color:#d1fae5;letter-spacing:2.5px;text-transform:uppercase;margin-top:4px;\">PLAY • BOOK • TOGETHER</div>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style=\"padding:34px 32px 28px;\">"
                + "            <h2 style=\"margin:0 0 12px;font-size:21px;font-weight:800;color:#0f172a;\">Kích hoạt tài khoản SportHub</h2>"
                + "            <p style=\"margin:0 0 18px;font-size:14px;line-height:1.6;color:#475569;\">"
                + "              Xin chào <strong>" + recipientName + "</strong>,<br>"
                + "              Cảm ơn bạn đã đăng ký tài khoản thành viên tại SportHub. Dưới đây là mã xác thực 6 chữ số để hoàn tất việc đăng ký:"
                + "            </p>"
                + "            <div style=\"text-align:center;margin:26px 0 22px;\">"
                + "              <div style=\"display:inline-block;background-color:#ecfdf5;border:2px dashed #059669;border-radius:14px;padding:16px 36px;user-select:all;-webkit-user-select:all;\">"
                + "                <span style=\"font-family:'Courier New',Consolas,monospace;font-size:36px;font-weight:900;letter-spacing:10px;color:#064e3b;\">"
                + otpCode + "</span>"
                + "              </div>"
                + "              <div style=\"margin-top:10px;font-size:12.5px;color:#059669;font-weight:600;\">"
                + "                💡 Nhấp đúp chuột để chọn mã nhanh (Ctrl + C)"
                + "              </div>"
                + "            </div>"
                + "            <div style=\"background-color:#f8fafc;border-left:4px solid #059669;border-radius:6px;padding:12px 16px;margin-top:20px;\">"
                + "              <p style=\"margin:0;font-size:13px;line-height:1.5;color:#475569;\">"
                + "                ⏱️ <strong>Thời hạn:</strong> Mã xác thực có hiệu lực trong vòng <strong>5 phút</strong>.<br>"
                + "                🔒 <strong>Bảo mật:</strong> Tuyệt đối không chia sẻ mã này cho bất kỳ ai để đảm bảo an toàn cho tài khoản."
                + "              </p>"
                + "            </div>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style=\"background-color:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 24px;text-align:center;font-size:12px;color:#94a3b8;\">"
                + "            © 2026 SportHub Việt Nam. Email này được tạo tự động, vui lòng không trả lời thư này."
                + "          </td>"
                + "        </tr>"
                + "      </table>"
                + "    </td></tr>"
                + "  </table>"
                + "</body></html>";
    }

    private String buildForgotPasswordOtpHtml(String toEmail, String otpCode, String verifyUrl) {
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head><meta charset=\"UTF-8\"><title>Mã OTP Quên Mật Khẩu SportHub</title></head>"
                + "<body style=\"margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#1e293b;\">"
                + "  <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color:#f1f5f9;padding:40px 15px;\">"
                + "    <tr><td align=\"center\">"
                + "      <table width=\"100%\" max-width=\"560px\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width:560px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.06);border:1px solid #e2e8f0;\">"
                + "        <tr>"
                + "          <td style=\"background:linear-gradient(135deg,#064e3b 0%,#059669 100%);padding:30px 24px;text-align:center;\">"
                + "            <div style=\"font-size:26px;font-weight:900;color:#ffffff;letter-spacing:1px;\">SPORT<span style=\"color:#6ee7b7;\">HUB</span></div>"
                + "            <div style=\"font-size:10.5px;font-weight:800;color:#d1fae5;letter-spacing:2.5px;text-transform:uppercase;margin-top:4px;\">PLAY • BOOK • TOGETHER</div>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style=\"padding:34px 32px 28px;\">"
                + "            <h2 style=\"margin:0 0 12px;font-size:21px;font-weight:800;color:#0f172a;\">Đặt lại mật khẩu tài khoản</h2>"
                + "            <p style=\"margin:0 0 18px;font-size:14px;line-height:1.6;color:#475569;\">"
                + "              Xin chào bạn,<br>"
                + "              SportHub vừa nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email <strong>"
                + toEmail + "</strong>. Dưới đây là mã xác thực OTP:"
                + "            </p>"
                + "            <div style=\"text-align:center;margin:26px 0 22px;\">"
                + "              <div style=\"display:inline-block;background-color:#ecfdf5;border:2px dashed #059669;border-radius:14px;padding:16px 36px;user-select:all;-webkit-user-select:all;\">"
                + "                <span style=\"font-family:'Courier New',Consolas,monospace;font-size:36px;font-weight:900;letter-spacing:10px;color:#064e3b;\">"
                + otpCode + "</span>"
                + "              </div>"
                + "              <div style=\"margin-top:10px;font-size:12.5px;color:#059669;font-weight:600;\">"
                + "                💡 Nhấp đúp chuột để chọn mã nhanh (Ctrl + C)"
                + "              </div>"
                + "            </div>"
                + "            <div style=\"background-color:#f8fafc;border-left:4px solid #059669;border-radius:6px;padding:12px 16px;margin-top:20px;\">"
                + "              <p style=\"margin:0;font-size:13px;line-height:1.5;color:#475569;\">"
                + "                ⏱️ <strong>Thời hạn:</strong> Mã OTP có hiệu lực trong vòng <strong>5 phút</strong>.<br>"
                + "                🔒 <strong>Bảo mật:</strong> Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua thư này. Mật khẩu của bạn sẽ không bị thay đổi."
                + "              </p>"
                + "            </div>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style=\"background-color:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 24px;text-align:center;font-size:12px;color:#94a3b8;\">"
                + "            © 2026 SportHub Việt Nam. Email này được tạo tự động, vui lòng không trả lời thư này."
                + "          </td>"
                + "        </tr>"
                + "      </table>"
                + "    </td></tr>"
                + "  </table>"
                + "</body></html>";
    }

    @Override
    public void sendBookingTicketEmail(String toEmail, String customerName, Booking booking, BigDecimal depositPaid,
            BigDecimal remainingAtCounter) {
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            log.warn("[SPORTHUB EMAIL SERVICE] Bỏ qua gửi vé điện tử do email không hợp lệ: {}", toEmail);
            return;
        }

        String recipientName = (customerName != null && !customerName.isBlank()) ? customerName.trim() : "Quý khách";
        String courtName = "Sân thể thao SportHub";
        String sportName = "Thể thao";
        String location = "SportUTE - Cơ sở Võ Văn Ngân";
        String timeDisplay = "--:--";
        String dateDisplay = "--/--/----";
        long durationMin = 30;

        if (booking != null && booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty()) {
            BookingDetail detail = booking.getBookingDetails().get(0);
            if (detail.getCourt() != null) {
                courtName = detail.getCourt().getName();
                if (detail.getCourt().getSport() != null) {
                    sportName = detail.getCourt().getSport().getName();
                }
                if (detail.getCourt().getLocation() != null && !detail.getCourt().getLocation().isBlank()) {
                    location = detail.getCourt().getLocation();
                }
            }
            if (detail.getStartTime() != null) {
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                dateDisplay = detail.getStartTime().format(dateFmt);
                String startStr = detail.getStartTime().format(timeFmt);
                String endStr = (detail.getEndTime() != null) ? detail.getEndTime().format(timeFmt) : "";
                timeDisplay = startStr + (endStr.isEmpty() ? "" : " - " + endStr);

                if (detail.getEndTime() != null) {
                    durationMin = Duration.between(detail.getStartTime(), detail.getEndTime()).toMinutes();
                }
            }
        }

        NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String depositStr = (depositPaid != null) ? vnFormat.format(depositPaid) + " đ" : "0 đ";
        String remainingStr = (remainingAtCounter != null && remainingAtCounter.compareTo(BigDecimal.ZERO) > 0)
                ? vnFormat.format(remainingAtCounter) + " đ"
                : "0 đ (Đã thanh toán đủ 100%)";

        String bookingId = (booking != null && booking.getId() != null) ? booking.getId()
                : "DS" + System.currentTimeMillis();
        String subject = "[SportHub] Vé điện tử xác nhận đặt sân #" + bookingId + " - " + courtName;
        String htmlContent = buildBookingTicketHtml(recipientName, bookingId, courtName, sportName, location,
                dateDisplay, timeDisplay, durationMin, depositStr, remainingStr);

        sendHtmlEmail(toEmail, subject, htmlContent, bookingId);
    }

    private String buildBookingTicketHtml(String recipientName, String bookingId, String courtName, String sportName,
            String location, String dateDisplay, String timeDisplay, long durationMin,
            String depositStr, String remainingStr) {
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head><meta charset=\"UTF-8\"><title>Vé điện tử đặt sân SportHub</title></head>"
                + "<body style=\"margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#1e293b;\">"
                + "  <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color:#f1f5f9;padding:40px 15px;\">"
                + "    <tr><td align=\"center\">"
                + "      <table width=\"100%\" max-width=\"600px\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width:600px;background-color:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 12px 30px rgba(0,0,0,0.08);border:1px solid #e2e8f0;\">"
                + "        <tr>"
                + "          <td style=\"background:linear-gradient(135deg,#064e3b 0%,#059669 100%);padding:28px 24px;text-align:center;\">"
                + "            <div style=\"font-size:26px;font-weight:900;color:#ffffff;letter-spacing:1px;\">SPORT<span style=\"color:#6ee7b7;\">HUB</span></div>"
                + "            <div style=\"font-size:11px;font-weight:800;color:#d1fae5;letter-spacing:2px;text-transform:uppercase;margin-top:4px;\">VÉ ĐIỆN TỬ XÁC NHẬN ĐẶT SÂN</div>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style=\"padding:32px 28px;\">"
                + "            <h2 style=\"margin:0 0 10px;font-size:20px;font-weight:800;color:#0f172a;\">Đặt sân thành công!</h2>"
                + "            <p style=\"margin:0 0 20px;font-size:14px;line-height:1.6;color:#475569;\">"
                + "              Kính gửi <strong>" + recipientName + "</strong>,<br>"
                + "              SportHub xin thông báo phiếu đặt sân của bạn đã được ghi nhận và xác nhận giữ chỗ thành công. Dưới đây là thông tin vé điện tử chính thức:"
                + "            </p>"
                + "            <div style=\"background-color:#ecfdf5;border:2px dashed #059669;border-radius:14px;padding:20px;margin-bottom:24px;text-align:center;\">"
                + "              <div style=\"font-size:12px;font-weight:700;color:#047857;text-transform:uppercase;letter-spacing:1px;\">MÃ PHIẾU ĐẶT SÂN</div>"
                + "              <div style=\"font-family:'Courier New',Consolas,monospace;font-size:32px;font-weight:900;letter-spacing:4px;color:#064e3b;margin:6px 0;\">"
                + bookingId + "</div>"
                + "              <div style=\"display:inline-block;background-color:#059669;color:#ffffff;font-size:12px;font-weight:800;padding:4px 14px;border-radius:20px;\">✓ ĐÃ XÁC NHẬN GIỮ SÂN</div>"
                + "            </div>"
                + "            <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"font-size:13.5px;color:#1e293b;border-collapse:collapse;margin-bottom:24px;\">"
                + "              <tr style=\"border-bottom:1px solid #f1f5f9;\">"
                + "                <td style=\"padding:10px 0;color:#64748b;\">🏟️ Sân thi đấu:</td>"
                + "                <td style=\"padding:10px 0;text-align:right;font-weight:700;color:#0f172a;\">"
                + courtName + " (" + sportName + ")</td>"
                + "              </tr>"
                + "              <tr style=\"border-bottom:1px solid #f1f5f9;\">"
                + "                <td style=\"padding:10px 0;color:#64748b;\">📍 Địa điểm:</td>"
                + "                <td style=\"padding:10px 0;text-align:right;font-weight:700;color:#0f172a;\">"
                + location + "</td>"
                + "              </tr>"
                + "              <tr style=\"border-bottom:1px solid #f1f5f9;\">"
                + "                <td style=\"padding:10px 0;color:#64748b;\">📅 Ngày thi đấu:</td>"
                + "                <td style=\"padding:10px 0;text-align:right;font-weight:700;color:#0f172a;\">"
                + dateDisplay + "</td>"
                + "              </tr>"
                + "              <tr style=\"border-bottom:1px solid #f1f5f9;\">"
                + "                <td style=\"padding:10px 0;color:#64748b;\">⏰ Khung giờ:</td>"
                + "                <td style=\"padding:10px 0;text-align:right;font-weight:700;color:#059669;\">"
                + timeDisplay + " (" + durationMin + " phút)</td>"
                + "              </tr>"
                + "              <tr style=\"border-bottom:1px solid #f1f5f9;\">"
                + "                <td style=\"padding:10px 0;color:#64748b;\">💳 Tiền đã thanh toán:</td>"
                + "                <td style=\"padding:10px 0;text-align:right;font-weight:700;color:#059669;\">"
                + depositStr + "</td>"
                + "              </tr>"
                + "              <tr>"
                + "                <td style=\"padding:10px 0;color:#64748b;\">💵 Còn lại tại quầy:</td>"
                + "                <td style=\"padding:10px 0;text-align:right;font-weight:700;color:#0f172a;\">"
                + remainingStr + "</td>"
                + "              </tr>"
                + "            </table>"
                + "            <div style=\"background-color:#f8fafc;border-left:4px solid #059669;border-radius:6px;padding:14px 16px;\">"
                + "              <div style=\"font-weight:800;color:#0f172a;margin-bottom:6px;font-size:13.5px;\">📌 Hướng dẫn nhận sân:</div>"
                + "              <div style=\"font-size:13px;line-height:1.6;color:#475569;\">"
                + "                • Xuất trình mã vé <strong>" + bookingId
                + "</strong> tại quầy lễ tân khi đến sân.<br>"
                + "                • Vui lòng có mặt trước giờ chơi <strong>5 - 10 phút</strong> để nhận sân và chuẩn bị thi đấu.<br>"
                + "                • Tổng đài hỗ trợ & CSKH SportHub: <strong>0384662822</strong>."
                + "              </div>"
                + "            </div>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style=\"background-color:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 24px;text-align:center;font-size:12px;color:#94a3b8;\">"
                + "            © 2026 SportHub Việt Nam • Trường ĐH Sư Phạm Kỹ Thuật TP.HCM (HCMUTE). Email này được gửi tự động, vui lòng không trả lời thư này."
                + "          </td>"
                + "        </tr>"
                + "      </table>"
                + "    </td></tr>"
                + "  </table>"
                + "</body></html>";
    }
}
