package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.response.ChatbotMessageResponse;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.model.MatchmakingPost;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.MatchmakingService;
import vn.edu.ute.sportute.service.SportBotService;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SportBotServiceImpl implements SportBotService {

    private final CourtRepository courtRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final BookingRepository bookingRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final SportRepository sportRepository;
    private final PriceRepository priceRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final MatchmakingService matchmakingService;

    public SportBotServiceImpl(CourtRepository courtRepository,
                               BookingDetailRepository bookingDetailRepository,
                               BookingRepository bookingRepository,
                               MaintenanceRepository maintenanceRepository,
                               SportRepository sportRepository,
                               PriceRepository priceRepository,
                               ServiceItemRepository serviceItemRepository,
                               CustomerRepository customerRepository,
                               AccountRepository accountRepository,
                               MatchmakingService matchmakingService) {
        this.courtRepository = courtRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.bookingRepository = bookingRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.sportRepository = sportRepository;
        this.priceRepository = priceRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.matchmakingService = matchmakingService;
    }

    @Override
    @Transactional(readOnly = true)
    public ChatbotMessageResponse handleMessage(String userMessage, String usernameOrEmail) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ChatbotMessageResponse.builder()
                    .reply("Dạ SportBot xin chào bạn! Bạn đang quan tâm đến sân thể thao nào hoặc cần hỗ trợ thông tin gì ạ?")
                    .status("SUCCESS")
                    .suggestions(getDefaultSuggestions())
                    .build();
        }

        String raw = userMessage.trim();
        String norm = removeAccents(raw.toLowerCase());

        // 1. Cách đặt sân / Hướng dẫn đặt sân / Quy trình đặt sân (Ưu tiên số 1 để giải quyết chính xác câu hỏi cách đặt sân)
        if (norm.contains("cach dat") || norm.contains("huong dan dat") || norm.contains("quy trinh dat")
                || norm.contains("lam sao de dat") || norm.contains("muon dat san") || norm.contains("dat san nhu the nao")
                || norm.contains("cac buoc dat") || norm.contains("thu tuc dat") || norm.contains("lam sao dat")
                || norm.contains("huong dan") || norm.contains("dat nhu the nao") || norm.contains("lam sao")
                || (norm.contains("dat san") && (norm.contains("cach") || norm.contains("the nao")))
                || norm.contains("nguoi moi") || norm.contains("chua biet dat") || norm.contains("lan dau")
                || norm.contains("lam sao de thue") || norm.contains("cach thue") || norm.contains("thue san nhu the nao")) {
            return handleHowToBook();
        }

        // 2. Chào hỏi thuần túy
        if (isGreeting(norm)) {
            return handleGreeting(usernameOrEmail);
        }

        // 3. Phương thức thanh toán / VietQR / Chuyển khoản / Đặt cọc
        if (norm.contains("thanh toan") || norm.contains("chuyen khoan") || norm.contains("vietqr")
                || norm.contains("qr code") || norm.contains("ngan hang") || norm.contains("momo")
                || norm.contains("vnpay") || norm.contains("tien mat") || norm.contains("coc bao nhieu")
                || norm.contains("dat coc nhu the nao") || norm.contains("phuong thuc thanh toan")
                || norm.contains("thanh toan the nao") || norm.contains("cach thanh toan")) {
            return handlePaymentMethods();
        }

        // 4. Thủ tục nhận sân / Check-in tại quầy
        if (norm.contains("nhan san") || norm.contains("checkin") || norm.contains("check-in")
                || norm.contains("den san") || norm.contains("thu tuc nhan") || norm.contains("den noi thi lam gi")
                || norm.contains("lay san") || norm.contains("quet ma qr") || norm.contains("ma qr")) {
            return handleCheckinGuide();
        }

        // 5. Tra cứu lịch sử / đơn đặt sân của khách hàng
        if (norm.contains("don cua toi") || norm.contains("lich cua toi") || norm.contains("phieu cua toi")
                || norm.contains("toi da dat") || norm.contains("lich su dat") || norm.contains("kiem tra don")
                || norm.contains("lich dat") || norm.contains("don dat")) {
            return handleMyBookings(usernameOrEmail);
        }

        // 6. Đổi sân / Đổi giờ / Gia hạn
        if (norm.contains("doi san") || norm.contains("doi gio") || norm.contains("doi lich")
                || norm.contains("gia han") || norm.contains("choi them") || norm.contains("them gio")) {
            return handleChangeOrExtend();
        }

        // 7. Chính sách hủy sân / hoàn tiền cọc
        if (norm.contains("chinh sach") || norm.contains("huy san") || norm.contains("hoan tien")
                || norm.contains("hoan coc") || norm.contains("phi huy") || norm.contains("huy dat")) {
            return handlePolicyInquiry();
        }

        // 8. Địa chỉ / Hotline / Giờ mở cửa / Liên hệ
        if (norm.contains("dia chi") || norm.contains("o dau") || norm.contains("gio mo cua")
                || norm.contains("gio hoat dong") || norm.contains("hotline") || norm.contains("so dien thoai")
                || norm.contains("sdt") || norm.contains("lien he") || norm.contains("email")
                || norm.contains("trung tam o dau") || norm.contains("ute") || norm.contains("thu duc")
                || norm.contains("mo cua") || norm.contains("dong cua")) {
            return handleContactAndHours();
        }

        // 9. Nội quy / Tiện ích / Giày dép / Bãi giữ xe
        if (norm.contains("noi quy") || norm.contains("quy dinh") || norm.contains("giay")
                || norm.contains("mang giay gi") || norm.contains("hut thuoc") || norm.contains("do xe")
                || norm.contains("gui xe") || norm.contains("bai xe") || norm.contains("tien ich")
                || norm.contains("wifi") || norm.contains("phong thay do") || norm.contains("tam")
                || norm.contains("nha ve sinh") || norm.contains("co cho dau xe khong") || norm.contains("giu xe")) {
            return handleRulesAndAmenities();
        }

        // 10. Khuyến mãi / Mã giảm giá / Voucher / Tích điểm
        if (norm.contains("khuyen mai") || norm.contains("giam gia") || norm.contains("voucher")
                || norm.contains("ma giam") || norm.contains("uu dai") || norm.contains("tich diem")
                || norm.contains("hoi vien") || norm.contains("membership") || norm.contains("member")) {
            return handlePromotions();
        }

        // 11. Hướng dẫn tài khoản / Đăng ký / Quên mật khẩu
        if (norm.contains("dang ky") || norm.contains("tao tai khoan") || norm.contains("quen mat khau")
                || norm.contains("doi mat khau") || norm.contains("otp") || norm.contains("khong co tai khoan")
                || norm.contains("chua co tai khoan") || norm.contains("co can tai khoan") || norm.contains("tai khoan")) {
            return handleAccountGuide();
        }

        // 12. Dịch vụ nước uống, đồ ăn, thuê vợt, phụ kiện F&B
        if (norm.contains("nuoc") || norm.contains("uong") || norm.contains("thue vot")
                || norm.contains("thue bong") || norm.contains("dich vu") || norm.contains("f&b")
                || norm.contains("do an") || norm.contains("giai khat") || norm.contains("dung cu")) {
            return handleServiceInquiry();
        }

        // 13. Ghép kèo giao lưu / Tìm đối thủ / Bắt kèo
        if (isMatchmakingQuery(norm)) {
            return handleMatchmakingInquiry(raw, norm);
        }

        // 14. Hỏi bảng giá thuê sân & Dự toán tính tiền theo thời lượng (VD: đặt 2 tiếng rưỡi bao nhiêu tiền,...)
        if (isPriceOrDurationQuery(norm)) {
            return handlePriceInquiry(raw, norm);
        }

        // 15. Kiểm tra xem có phải câu hỏi kiểm tra sân trống / thời gian thực không
        if (isAvailabilityQuery(norm)) {
            return handleRealtimeAvailability(raw, norm);
        }

        // 16. Fallback thông minh: hướng dẫn chi tiết các chức năng mà bot có thể hỗ trợ
        return handleSmartFallback(raw, norm);
    }

    // =========================================================================
    // 2. HƯỚNG DẪN CÁCH ĐẶT SÂN (HOW TO BOOK)
    // =========================================================================
    private ChatbotMessageResponse handleHowToBook() {
        String reply = "📖 **HƯỚNG DẪN 4 BƯỚC ĐẶT SÂN TRỰC TUYẾN TẠI SPORTHUB:**\n\n" +
                "1. 🔍 **Bước 1: Chọn sân & môn yêu thích**\n" +
                "   • Lướt danh sách hoặc lọc theo môn (Bóng đá, Cầu lông, Bóng rổ, Pickleball).\n" +
                "   • Bạn có thể hỏi trực tiếp SportBot để kiểm tra sân trống tức thì (VD: *\"Sân bóng đá 1 lúc 18h hôm nay đã ai đặt chưa?\"*).\n\n" +
                "2. ⏰ **Bước 2: Chọn ngày & Khung giờ chơi**\n" +
                "   • Nhấn vào thẻ sân $\\rightarrow$ Bấm **Đặt sân ngay**.\n" +
                "   • Chọn ngày chơi và khung giờ (30 phút, 1 tiếng, 2 tiếng tùy ý).\n" +
                "   • Chọn thêm nước giải khát hoặc thuê vợt/bóng nếu có nhu cầu.\n\n" +
                "3. 💳 **Bước 3: Xác nhận & Thanh toán cọc qua VietQR**\n" +
                "   • Điền thông tin người đặt (Họ tên, SĐT, Email).\n" +
                "   • Quét mã **VietQR tự động** (hỗ trợ mọi app ngân hàng Napas 24/7 & MoMo, ZaloPay) để đặt cọc (30% - 50%).\n" +
                "   • Hệ thống tự động xác nhận đơn trong 3 giây ngay khi nhận tiền cọc!\n\n" +
                "4. 🎟️ **Bước 4: Nhận mã phiếu & Check-in vào sân**\n" +
                "   • Mã phiếu đặt sân kèm mã QR sẽ gửi ngay về Email/SMS của bạn.\n" +
                "   • Đến giờ chơi, bạn chỉ cần đưa mã QR hoặc đọc SĐT tại bàn lễ tân để nhận sân vào chơi ngay.";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of(
                        "Sân bóng đá 1 lúc 18h hôm nay?",
                        "Hôm nay có sân cầu lông nào trống?",
                        "Bảng giá thuê các loại sân?",
                        "Phương thức thanh toán VietQR"
                ))
                .build();
    }

    // =========================================================================
    // 3. PHƯƠNG THỨC THANH TOÁN & ĐẶT CỌC (PAYMENT METHODS)
    // =========================================================================
    private ChatbotMessageResponse handlePaymentMethods() {
        String reply = "💳 **CÁC PHƯƠNG THỨC THANH TOÁN TẠI SPORTHUB:**\n\n" +
                "1. 📱 **Chuyển khoản VietQR tự động (Khuyên dùng):**\n" +
                "   • Hệ thống tự động tạo mã QR có sẵn số tiền và nội dung chuyển khoản.\n" +
                "   • Hỗ trợ tất cả ứng dụng ngân hàng (Vietcombank, MB Bank, Techcombank, BIDV, ACB...) và các ví MoMo, ZaloPay, VNPay.\n" +
                "   • Hệ thống kích hoạt trạng thái **Đã xác nhận** tức thì sau khi quét mã thành công.\n\n" +
                "2. 💵 **Tiền mặt tại quầy lễ tân:**\n" +
                "   • Áp dụng khi khách hàng đặt sân trực tiếp tại quầy hoặc thanh toán phần tiền còn lại khi trả sân.\n\n" +
                "3. 🔒 **Mức đặt cọc:**\n" +
                "   • Tiêu chuẩn: **30%** tổng tiền thuê sân.\n" +
                "   • Khung giờ cao điểm / Ngày lễ: **50%**.\n" +
                "   • Số tiền còn lại quý khách thanh toán khi trả sân kết thúc buổi chơi.";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of("Cách đặt sân trực tuyến", "Chính sách hủy sân & hoàn tiền cọc", "Bảng giá thuê sân"))
                .build();
    }

    // =========================================================================
    // 4. HƯỚNG DẪN NHẬN SÂN / CHECK-IN (CHECK-IN GUIDE)
    // =========================================================================
    private ChatbotMessageResponse handleCheckinGuide() {
        String reply = "⚡ **QUY TRÌNH TIẾP ĐÓN & NHẬN SÂN SIÊU TỐC:**\n\n" +
                "1. 📍 Khi đến trung tâm SportHub, bạn di chuyển đến **Bàn Tiếp Đón Lễ Tân** tại sảnh chính.\n" +
                "2. 🔍 **Cách nhận sân:**\n" +
                "   • **Cách 1 (Nhanh nhất):** Đưa mã QR trên phiếu đặt sân (trong Email hoặc lịch sử web) cho lễ tân quét.\n" +
                "   • **Cách 2:** Đọc **Số điện thoại** hoặc **Mã phiếu** (VD: `#DS0101`) đã đăng ký.\n" +
                "3. ⏱️ Lễ tân nhấn **Nhận sân ngay** trong vòng 5 giây và hướng dẫn bạn vào đúng vị trí sân thi đấu.\n" +
                "4. 🥤 Bạn có thể gọi thêm nước giải khát, thuê bóng, thuê vợt tại quầy hoặc trên điện thoại bất kỳ lúc nào.";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of("Menu nước uống & thuê vợt", "Nội quy & tiện ích sân", "Cách đổi sân hoặc giờ thuê"))
                .build();
    }

    // =========================================================================
    // 6. ĐỔI SÂN / ĐỔI GIỜ / GIA HẠN THÊM GIỜ (CHANGE OR EXTEND)
    // =========================================================================
    private ChatbotMessageResponse handleChangeOrExtend() {
        String reply = "🔄 **HƯỚNG DẪN ĐỔI SÂN, ĐỔI GIỜ & GIA HẠN:**\n\n" +
                "1. 📅 **Đổi sân hoặc đổi giờ thuê:**\n" +
                "   • Quý khách vào mục **[Lịch sử đặt sân](/customer/history)**.\n" +
                "   • Tìm đơn đặt sân đang ở trạng thái *Chờ xác nhận* hoặc *Đã xác nhận* $\\rightarrow$ Nhấn nút **Đổi**.\n" +
                "   • Chọn ngày mới, khung giờ mới hoặc sân mới còn trống $\\rightarrow$ Xác nhận hoàn toàn **MIỄN PHÍ**!\n\n" +
                "2. ⏱️ **Gia hạn chơi thêm giờ:**\n" +
                "   • Nếu khung giờ tiếp theo của sân chưa có ai đặt, bạn có thể báo lễ tân tại quầy hoặc nhờ SportBot kiểm tra để gia hạn chơi thêm tiếp.\n" +
                "   • Tiền gia hạn được cộng gộp và quyết toán khi trả sân.";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of("Lịch đặt sân của tôi", "Chính sách hủy sân & hoàn tiền cọc", "Kiểm tra sân trống"))
                .build();
    }

    // =========================================================================
    // 8. ĐỊA CHỈ, HOTLINE & GIỜ HOẠT ĐỘNG (CONTACT & HOURS)
    // =========================================================================
    private ChatbotMessageResponse handleContactAndHours() {
        String reply = "📍 **THÔNG TIN LIÊN HỆ & THỜI GIAN HOẠT ĐỘNG SPORTHUB:**\n\n" +
                "• 🏢 **Địa chỉ:** Trung tâm Thể thao SportHub — Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh (Cơ sở Đại học Sư Phạm Kỹ Thuật TP.HCM - UTE).\n" +
                "• ⏰ **Giờ mở cửa:** **05:00 – 23:00** tất cả các ngày trong tuần (kể cả Thứ 7, Chủ nhật và ngày Lễ Tết).\n" +
                "• 📞 **Hotline hỗ trợ & cứu hộ sân:** `0901 234 567` / `0384 662 822`.\n" +
                "• 📧 **Email CSKH:** `support@sporthub.vn` / `sporthub.ute@gmail.com`.\n" +
                "• 🚗 **Bãi đỗ xe:** Có bãi đỗ xe ô tô và xe máy rộng rãi, bảo vệ trông giữ 24/24 an toàn tuyệt đối.";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of("Cách đặt sân trực tuyến", "Bảng giá thuê sân", "Tiện ích & bãi giữ xe"))
                .build();
    }

    // =========================================================================
    // 9. NỘI QUY & TIỆN ÍCH SÂN (RULES & AMENITIES)
    // =========================================================================
    private ChatbotMessageResponse handleRulesAndAmenities() {
        String reply = "🏟️ **TIỆN ÍCH & NỘI QUY TẠI TRUNG TÂM SPORTHUB:**\n\n" +
                "🌟 **Tiện ích miễn phí dành cho khách hàng:**\n" +
                "• Bãi đỗ xe máy và ô tô rộng rãi, có mái che và bảo vệ giữ xe an toàn.\n" +
                "• Phòng thay đồ nam / nữ sạch sẽ, riêng biệt có vòi sen nước nóng thư giãn.\n" +
                "• Hệ thống dàn đèn LED chuyên dụng chống chói, đạt tiêu chuẩn thi đấu ban đêm.\n" +
                "• Wifi tốc độ cao miễn phí toàn bộ khuôn viên trung tâm.\n\n" +
                "📋 **Nội quy thi đấu:**\n" +
                "• Mang giày thể thao phù hợp (đế cao su / giày bóng đá chuyên dụng, không mang giày cao gót hoặc giày đinh sắt gây hư hỏng mặt sân cỏ/sàn gỗ).\n" +
                "• Tuyệt đối không hút thuốc lá trong khuôn viên sân thi đấu.\n" +
                "• Giữ gìn vệ sinh chung, bỏ rác đúng nơi quy định.\n" +
                "• Thi đấu đúng giờ đã đăng ký để đảm bảo quyền lợi cho các nhóm khách tiếp theo.";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of("Bảng giá thuê sân", "Menu nước giải khát F&B", "Địa chỉ trung tâm"))
                .build();
    }

    // =========================================================================
    // 10. CHƯƠNG TRÌNH KHUYẾN MÃI & TÍCH ĐIỂM (PROMOTIONS)
    // =========================================================================
    private ChatbotMessageResponse handlePromotions() {
        String reply = "🎁 **CÁC CHƯƠNG TRÌNH ƯU ĐÃI & KHUYẾN MÃI HIỆN CÓ:**\n\n" +
                "1. 🏷️ **Mã `KM_FRIENDS`:** Giảm ngay **20,000 đ** cho đơn đặt sân nhóm bạn thể thao.\n" +
                "2. 🌅 **Ưu đãi Khung Giờ Vàng Sáng Sớm (05:00 – 07:00):** Giá thuê cực kỳ ưu đãi chỉ từ 50,000 đ/giờ.\n" +
                "3. ⭐ **Tích lũy Điểm Hội Viên (Member Points):**\n" +
                "   • Mỗi lần đặt sân tích ngay **5%** giá trị hóa đơn.\n" +
                "   • Điểm tích lũy dùng để trừ tiền thuê sân cho lần sau hoặc đổi nước uống F&B miễn phí!\n\n" +
                "👉 Bạn hãy nhập mã khuyến mãi tại bước xác nhận đặt sân để được giảm giá ngay nhé!";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of("Cách đặt sân để nhận ưu đãi", "Bảng giá thuê sân", "Đăng ký thành viên tích điểm"))
                .build();
    }

    // =========================================================================
    // 11. HƯỚNG DẪN TÀI KHOẢN (ACCOUNT GUIDE)
    // =========================================================================
    private ChatbotMessageResponse handleAccountGuide() {
        String reply = "👤 **HƯỚNG DẪN TÀI KHOẢN & ĐĂNG KÝ:**\n\n" +
                "• **Không có tài khoản có đặt sân được không?**\n" +
                "  $\\rightarrow$ Có! Bạn hoàn toàn có thể đặt sân nhanh bằng cách điền SĐT & Email mà không cần tạo tài khoản.\n\n" +
                "• **Lợi ích khi đăng ký tài khoản SportHub:**\n" +
                "  1. Quản lý toàn bộ lịch sử đặt sân, xem và tải hóa đơn PDF mọi lúc.\n" +
                "  2. Tự thao tác đổi sân, đổi giờ hoặc hủy đơn trực tuyến mà không cần gọi tổng đài.\n" +
                "  3. Tích điểm hội viên (Member Points) để đổi giờ chơi miễn phí.\n\n" +
                "• **Đăng ký ngay tại:** [Trang Đăng Ký](/register) (Chỉ mất 30 giây, xác thực mã OTP an toàn qua Email).\n" +
                "• **Quên mật khẩu?** Bạn chọn [Quên Mật Khẩu](/forgot-password) trên màn hình đăng nhập để đặt lại mật khẩu mới.";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of("Cách đặt sân trực tuyến", "Đăng nhập tài khoản", "Kiểm tra sân trống"))
                .build();
    }

    // =========================================================================
    // 13. GHÉP KÈO GIAO LƯU & TÌM ĐỐI THỦ (MATCHMAKING)
    // =========================================================================
    private ChatbotMessageResponse handleMatchmakingInquiry(String raw, String norm) {
        String sport = null;
        if (norm.contains("bong da") || norm.contains("da banh") || norm.contains("football")) {
            sport = "Bóng đá";
        } else if (norm.contains("cau long") || norm.contains("badminton")) {
            sport = "Cầu lông";
        } else if (norm.contains("bong ro") || norm.contains("basketball")) {
            sport = "Bóng rổ";
        } else if (norm.contains("pickleball")) {
            sport = "Pickleball";
        }

        List<MatchmakingPost> activePosts = matchmakingService.getActivePosts(sport, null);

        StringBuilder sb = new StringBuilder();
        sb.append("🤝 **KÈO GHÉP GIAO LƯU & TÌM ĐỐI THỦ TẠI SPORTHUB:**\n\n");
        if (sport != null) {
            sb.append("🔍 Đang lọc theo môn: **").append(sport).append("**\n\n");
        }

        if (activePosts.isEmpty()) {
            sb.append("Hiện tại chưa có kèo giao lưu nào đang chờ đối thủ");
            if (sport != null) {
                sb.append(" cho môn **").append(sport).append("**");
            }
            sb.append(".\n\n");
            sb.append("💡 **Bạn muốn tạo kèo mới?**\n");
            sb.append("• Khi đặt sân trực tuyến tại SportHub, ở bước thanh toán bạn chỉ cần gạt bật **\"Tìm đối thủ / Ghép kèo giao lưu\"**.\n");
            sb.append("• Hệ thống sẽ tự động hiển thị kèo của bạn lên **Bảng Ghép Kèo** để các đội khác vào bắt đối!\n\n");
        } else {
            sb.append("🔥 Hiện có **").append(activePosts.size()).append(" kèo đang chờ đối thủ** tham gia:\n\n");
            int count = 0;
            for (MatchmakingPost post : activePosts) {
                if (count++ >= 3) break;
                sb.append("• ").append(getSportIcon(post.getSportName())).append(" **").append(post.getSportName()).append("** - ").append(post.getCourtName()).append("\n");
                sb.append("  ⏰ Thời gian: `").append(post.getStartTime()).append(" - ").append(post.getEndTime()).append("` (Ngày: ").append(post.getMatchDate()).append(")\n");
                sb.append("  👤 Người tạo: **").append(post.getCreatorName()).append("** | Trình độ: *").append(post.getSkillLevel() != null ? post.getSkillLevel() : "Giao lưu vui vẻ").append("*\n");
                if (post.getMessage() != null && !post.getMessage().isBlank()) {
                    sb.append("  💬 Lời nhắn: \"").append(post.getMessage()).append("\"\n");
                }
                sb.append("\n");
            }

            if (activePosts.size() > 3) {
                sb.append("*(Và còn ").append(activePosts.size() - 3).append(" kèo khác trên hệ thống...)*\n\n");
            }

            sb.append("👉 Bạn hãy vào mục **[Ghép Kèo Giao Lưu](/customer/community)** trên thanh điều hướng để xem chi tiết danh sách và bấm **'Bắt Đối'** ngay nhé!\n\n");
        }

        sb.append("⚠️ **Lưu ý & Quy định quan trọng từ SportHub:**\n");
        sb.append("1. 💵 **Chi phí & Thanh toán:** Hệ thống không chịu trách nhiệm về chi phí đối thủ. Mọi thanh toán tiền sân & cọc do chính người đặt sân ban đầu chịu; chi phí share/tiền sân do 2 bên tự thỏa thuận ngoài đời.\n");
        sb.append("2. 👥 **Dự phòng người chơi:** Người đặt sân nên chủ động rủ đủ số lượng người chơi đề phòng trường hợp không có ai nhận kèo hoặc đối thủ hủy đột xuất.\n");
        sb.append("3. 🚫 **Nghiêm cấm bùng kèo:** Người nhận kèo phải xác nhận cam kết có mặt đúng giờ. Nếu cố ý nhận rồi không đến, hệ thống sẽ ghi nhận vi phạm, cảnh cáo và khóa vĩnh viễn quyền tham gia ghép đối!\n");

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Xem Bảng Ghép Kèo");
        suggestions.add("Kèo Bóng đá hôm nay");
        suggestions.add("Kèo Cầu lông hôm nay");
        suggestions.add("Cách đặt sân mới");

        return ChatbotMessageResponse.builder()
                .reply(sb.toString())
                .status("SUCCESS")
                .suggestions(suggestions)
                .build();
    }

    // =========================================================================
    // 16. FALLBACK THÔNG MINH
    // =========================================================================
    private ChatbotMessageResponse handleSmartFallback(String raw, String norm) {
        String reply = "🤖 **Dạ SportBot có thể hỗ trợ bạn các thông tin sau:**\n\n" +
                "• 🤝 **Ghép kèo giao lưu / Tìm đối thủ:** Gõ *\"có kèo nào không\"* hoặc *\"tìm đối bóng đá\"*.\n" +
                "• 📖 **Cách đặt sân & hướng dẫn từng bước:** Gõ *\"cách đặt sân\"* hoặc *\"hướng dẫn đặt sân\"*.\n" +
                "• 🔍 **Kiểm tra tình trạng sân theo thời gian thực:** Gõ ví dụ: *\"Sân bóng đá 1 lúc 18h hôm nay đã ai đặt chưa?\"* hoặc *\"Hôm nay có sân nào trống?\"*.\n" +
                "• 💰 **Bảng giá thuê sân:** Gõ *\"bảng giá thuê sân\"*.\n" +
                "• 💳 **Phương thức thanh toán & đặt cọc:** Gõ *\"thanh toán\"* hoặc *\"đặt cọc\"*.\n" +
                "• 🥤 **Menu nước uống & dụng cụ F&B:** Gõ *\"menu nước uống\"* hoặc *\"thuê vợt\"*.\n" +
                "• 📜 **Chính sách hủy sân & hoàn tiền cọc:** Gõ *\"chính sách hủy sân\"*.\n" +
                "• 📍 **Địa chỉ & giờ mở cửa:** Gõ *\"địa chỉ trung tâm\"* hoặc *\"giờ mở cửa\"*.\n\n" +
                "Bạn vui lòng chọn một trong các gợi ý bên dưới hoặc gửi câu hỏi cụ thể hơn giúp SportBot nhé!";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of(
                        "Ghép kèo giao lưu thể thao",
                        "Cách đặt sân trực tuyến",
                        "Sân bóng đá 1 lúc 18h đã ai đặt chưa?",
                        "Hôm nay có sân cầu lông nào trống?",
                        "Bảng giá thuê các loại sân?",
                        "Địa chỉ & giờ mở cửa trung tâm"
                ))
                .build();
    }

    // =========================================================================
    // XỬ LÝ TRA CỨU TÌNH TRẠNG SÂN THEO THỜI GIAN THỰC (REAL-TIME AVAILABILITY)
    // =========================================================================
    private ChatbotMessageResponse handleRealtimeAvailability(String raw, String norm) {
        List<Court> allCourts = courtRepository.findAll();
        LocalDate targetDate = parseDate(norm);
        LocalTime targetTime = parseTime(norm);

        // Xác định sân cụ thể hoặc môn thể thao
        Court matchedCourt = findMatchedCourt(norm, allCourts);
        Sport matchedSport = (matchedCourt != null) ? matchedCourt.getSport() : findMatchedSport(norm);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        String dateStr = targetDate.format(dateFmt);
        String dayDesc = targetDate.equals(LocalDate.now()) ? "Hôm nay (" + dateStr + ")"
                : targetDate.equals(LocalDate.now().plusDays(1)) ? "Ngày mai (" + dateStr + ")"
                : "Ngày " + dateStr;

        // Trường hợp 1: Có sân cụ thể VÀ có khung giờ cụ thể
        if (matchedCourt != null && targetTime != null) {
            LocalDateTime startDT = LocalDateTime.of(targetDate, targetTime);
            Double dur = parseDurationHours(norm);
            int minutes = (dur != null && dur > 0) ? (int) Math.round(dur * 60) : 60;
            LocalDateTime endDT = startDT.plusMinutes(minutes);

            // 1.1 Kiểm tra bảo trì sân
            Optional<Maintenance> maintOpt = maintenanceRepository.findAll().stream()
                    .filter(m -> m.getCourt().getId().equals(matchedCourt.getId()))
                    .filter(m -> m.getStartDate().isBefore(endDT) && m.getEndDate().isAfter(startDT))
                    .findFirst();

            if (maintOpt.isPresent()) {
                Maintenance m = maintOpt.get();
                String reply = String.format(
                        "⚠️ **%s** vào lúc **%s** (%s) hiện đang trong kế hoạch **BẢO TRÌ ĐỊNH KỲ**.\n\n" +
                        "🔧 **Lý do:** %s\n" +
                        "⏰ **Thời gian bảo trì:** Từ %s đến %s\n\n" +
                        "💡 Bạn vui lòng tham khảo các sân khác cùng môn hoặc khung giờ khác nhé!",
                        matchedCourt.getName(), targetTime.format(timeFmt), dayDesc,
                        m.getReason() != null ? m.getReason() : "Bảo trì nâng cấp mặt sân",
                        m.getStartDate().format(timeFmt), m.getEndDate().format(timeFmt)
                );
                return ChatbotMessageResponse.builder()
                        .reply(reply)
                        .status("SUCCESS")
                        .suggestions(getAlternativeCourtSuggestions(matchedCourt, allCourts))
                        .build();
            }

            // 1.2 Kiểm tra đặt sân thực tế từ cơ sở dữ liệu thời gian thực
            List<BookingDetail> overlapping = bookingDetailRepository.findOverlappingBookings(matchedCourt.getId(), startDT, endDT);

            if (!overlapping.isEmpty()) {
                BookingDetail bd = overlapping.get(0);
                String occupiedRange = bd.getStartTime().format(timeFmt) + " – " + bd.getEndTime().format(timeFmt);
                String bStatus = bd.getBooking().getStatus();

                // Tìm sân cùng môn khác đang còn trống trong cùng khung giờ
                List<Court> otherFreeCourts = allCourts.stream()
                        .filter(c -> !c.getId().equals(matchedCourt.getId()))
                        .filter(c -> c.getSport().getId().equals(matchedCourt.getSport().getId()))
                        .filter(c -> !bookingDetailRepository.existsOverlappingBooking(c.getId(), startDT, endDT))
                        .collect(Collectors.toList());

                StringBuilder reply = new StringBuilder();
                reply.append(String.format("❌ Dạ **%s** vào lúc **%s** %s hiện **ĐÃ CÓ NGƯỜI ĐẶT RỒI** ạ!\n\n",
                        matchedCourt.getName(), targetTime.format(timeFmt), dayDesc));
                if (dur != null && dur > 0) {
                    reply.append(String.format("⏰ **Khung giờ bạn hỏi:** `%s – %s` (%s)\n",
                            targetTime.format(timeFmt), endDT.format(timeFmt), formatDurationDesc(dur)));
                }
                reply.append(String.format("⏰ **Khung giờ đã có người đặt:** `%s`\n", occupiedRange));
                reply.append(String.format("📌 **Trạng thái:** %s\n\n", bStatus));

                if (!otherFreeCourts.isEmpty()) {
                    reply.append("✨ **Gợi ý sân trống cùng thời điểm:**\n");
                    for (Court ofc : otherFreeCourts) {
                        reply.append(String.format("• **%s** hiện **ĐANG TRỐNG** lúc %s! 👉 Bạn có thể đặt ngay sân này.\n",
                                ofc.getName(), targetTime.format(timeFmt)));
                    }
                } else {
                    reply.append(String.format("💡 Các sân cùng môn hiện đều kín lúc %s. Bạn có thể tham khảo sau `%s` hoặc chọn khung giờ khác nhé!\n",
                            targetTime.format(timeFmt), bd.getEndTime().format(timeFmt)));
                }

                List<String> suggestions = new ArrayList<>();
                for (Court ofc : otherFreeCourts) {
                    suggestions.add("Kiểm tra " + ofc.getName() + " lúc " + targetTime.format(timeFmt));
                }
                suggestions.add("Xem bảng giá " + matchedCourt.getSport().getName());
                suggestions.add("Xem các sân còn trống hôm nay");

                return ChatbotMessageResponse.builder()
                        .reply(reply.toString())
                        .status("SUCCESS")
                        .suggestions(suggestions)
                        .actionData(Map.of("courtId", matchedCourt.getId(), "available", false))
                        .build();

            } else {
                // Sân hoàn toàn trống!
                BigDecimal price = getCourtPrice(matchedCourt, targetDate);
                long hourlyVal = (price != null) ? price.longValue() : 100000L;
                String priceStr;
                String durNotice = "";
                if (dur != null && dur > 0) {
                    long totalVal = Math.round(hourlyVal * dur);
                    priceStr = String.format("`%,d đ` (cho %s, đơn giá: %,d đ/giờ)", totalVal, formatDurationDesc(dur), hourlyVal);
                    durNotice = String.format("⏰ **Khung giờ giữ chỗ:** `%s – %s` (%s)\n", targetTime.format(timeFmt), endDT.format(timeFmt), formatDurationDesc(dur));
                } else {
                    priceStr = String.format("`%,d đ/giờ`", hourlyVal);
                }

                StringBuilder reply = new StringBuilder();
                reply.append(String.format("✅ **Tuyệt vời! %s** vào lúc **%s** %s hiện **CHƯA CÓ AI ĐẶT (ĐANG TRỐNG)** bạn nhé! 🎉\n\n",
                        matchedCourt.getName(), targetTime.format(timeFmt), dayDesc));
                if (!durNotice.isEmpty()) {
                    reply.append(durNotice);
                }
                reply.append(String.format("🏟️ **Môn thể thao:** %s\n", matchedCourt.getSport().getName()));
                reply.append(String.format("💵 **Giá thuê dự kiến:** %s\n", priceStr));
                reply.append("🌟 **Tiện ích:** Mặt sân cao cấp, đèn LED chiếu sáng ban đêm, có mái che che mưa nắng\n\n");
                reply.append("👉 Bạn hãy nhấp vào nút bên dưới hoặc chọn sân này trên danh sách để đặt giữ chỗ ngay nhé!");

                Map<String, Object> actionData = new HashMap<>();
                actionData.put("courtId", matchedCourt.getId());
                actionData.put("courtName", matchedCourt.getName());
                actionData.put("time", targetTime.format(timeFmt));
                actionData.put("available", true);

                List<String> suggestions = List.of(
                        "👉 Đặt " + matchedCourt.getName() + " ngay",
                        "Xem dịch vụ nước uống & thuê vợt",
                        "Chính sách hủy & hoàn tiền"
                );

                return ChatbotMessageResponse.builder()
                        .reply(reply.toString())
                        .status("SUCCESS")
                        .suggestions(suggestions)
                        .actionData(actionData)
                        .build();
            }
        }

        // Trường hợp 2: Có sân cụ thể nhưng KHÔNG có giờ cụ thể -> Báo cáo tổng thể cả ngày
        if (matchedCourt != null && targetTime == null) {
            LocalDateTime startOfDay = targetDate.atStartOfDay();
            LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);
            List<BookingDetail> dayBookings = bookingDetailRepository.findActiveBookingsByCourtAndDate(matchedCourt.getId(), startOfDay, endOfDay);

            StringBuilder reply = new StringBuilder();
            reply.append(String.format("📅 **Tình trạng %s trong %s:**\n\n", matchedCourt.getName(), dayDesc));

            if (dayBookings.isEmpty()) {
                reply.append("🌟 **Sân hiện đang TRỐNG TOÀN BỘ CÁC KHUNG GIỜ** từ 05:00 đến 23:00 hôm nay! Bạn có thể thoải mái chọn bất kỳ khung giờ nào để đặt.\n");
            } else {
                reply.append("🔴 **Các khung giờ đã có người đặt:**\n");
                for (BookingDetail bd : dayBookings) {
                    reply.append(String.format("• `%s – %s`: Đã đặt (%s)\n",
                            bd.getStartTime().format(timeFmt), bd.getEndTime().format(timeFmt), bd.getBooking().getStatus()));
                }
                reply.append("\n🟢 **Các khung giờ còn lại đều đang mở trống** cho bạn đặt sân!");
            }

            List<String> suggestions = List.of(
                    "Kiểm tra " + matchedCourt.getName() + " lúc 18:00",
                    "Kiểm tra " + matchedCourt.getName() + " lúc 19:30",
                    "Xem giá thuê " + matchedCourt.getSport().getName(),
                    "Xem các sân khác còn trống"
            );

            return ChatbotMessageResponse.builder()
                    .reply(reply.toString())
                    .status("SUCCESS")
                    .suggestions(suggestions)
                    .build();
        }

        // Trường hợp 3: Hỏi chung về môn (hoặc toàn trung tâm) xem hôm nay còn sân nào trống
        return handleGeneralAvailability(matchedSport, allCourts, targetDate, targetTime, dayDesc);
    }

    private ChatbotMessageResponse handleGeneralAvailability(Sport sport, List<Court> allCourts,
                                                             LocalDate targetDate, LocalTime targetTime, String dayDesc) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        List<Court> filterCourts = (sport != null)
                ? allCourts.stream().filter(c -> c.getSport().getId().equals(sport.getId())).collect(Collectors.toList())
                : allCourts;

        LocalTime checkTime = (targetTime != null) ? targetTime : LocalTime.of(18, 0);
        LocalDateTime startDT = LocalDateTime.of(targetDate, checkTime);
        LocalDateTime endDT = startDT.plusMinutes(60);

        List<Court> freeCourts = new ArrayList<>();
        List<Court> busyCourts = new ArrayList<>();

        for (Court c : filterCourts) {
            boolean isBusy = bookingDetailRepository.existsOverlappingBooking(c.getId(), startDT, endDT);
            if (isBusy) {
                busyCourts.add(c);
            } else {
                freeCourts.add(c);
            }
        }

        StringBuilder reply = new StringBuilder();
        reply.append(String.format("📊 **Báo cáo tình trạng sân %s lúc %s:**\n\n", dayDesc, checkTime.format(timeFmt)));

        if (!freeCourts.isEmpty()) {
            reply.append(String.format("🟢 **Đang còn trống (%d sân):**\n", freeCourts.size()));
            for (Court fc : freeCourts) {
                BigDecimal price = getCourtPrice(fc, targetDate);
                String priceStr = price != null ? String.format("%,d đ/h", price.longValue()) : "";
                reply.append(String.format("• **%s** (%s) %s — *Sẵn sàng đặt*\n",
                        fc.getName(), fc.getSport().getName(), priceStr.isEmpty() ? "" : "- " + priceStr));
            }
        } else {
            reply.append(String.format("🔴 Rất tiếc, vào lúc %s thì tất cả các sân trong danh mục này đều đã kín lịch.\n", checkTime.format(timeFmt)));
        }

        if (!busyCourts.isEmpty()) {
            reply.append(String.format("\n🔴 **Đã có người đặt (%d sân):** %s\n",
                    busyCourts.size(), busyCourts.stream().map(Court::getName).collect(Collectors.joining(", "))));
        }

        reply.append("\n💡 Bạn muốn kiểm tra chi tiết sân nào hoặc khung giờ nào khác không ạ?");

        List<String> suggestions = new ArrayList<>();
        for (Court fc : freeCourts.stream().limit(3).collect(Collectors.toList())) {
            suggestions.add("Đặt " + fc.getName() + " lúc " + checkTime.format(timeFmt));
        }
        suggestions.add("Xem bảng giá tất cả các sân");
        suggestions.add("Dịch vụ F&B nước uống");

        return ChatbotMessageResponse.builder()
                .reply(reply.toString())
                .status("SUCCESS")
                .suggestions(suggestions)
                .build();
    }

    // =========================================================================
    // XỬ LÝ TRA CỨU ĐƠN CỦA TÔI (MY BOOKINGS)
    // =========================================================================
    private ChatbotMessageResponse handleMyBookings(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || "khachle".equalsIgnoreCase(usernameOrEmail)) {
            return ChatbotMessageResponse.builder()
                    .reply("🔒 Bạn hiện đang là khách vãng lai. Vui lòng **[Đăng nhập tại đây](/login)** để SportBot có thể tra cứu chính xác lịch sử đặt sân và phiếu của bạn nhé!")
                    .status("SUCCESS")
                    .suggestions(List.of("Đăng nhập tài khoản", "Xem bảng giá", "Kiểm tra sân trống"))
                    .build();
        }

        Optional<Account> accOpt = accountRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        Optional<Customer> custOpt = Optional.empty();
        if (accOpt.isPresent()) {
            custOpt = customerRepository.findByAccount_Id(accOpt.get().getId());
        }
        if (custOpt.isEmpty()) {
            custOpt = customerRepository.findByEmail(usernameOrEmail);
        }
        if (custOpt.isEmpty()) {
            custOpt = customerRepository.findByPhone(usernameOrEmail);
        }

        if (custOpt.isEmpty()) {
            return ChatbotMessageResponse.builder()
                    .reply("Chưa tìm thấy thông tin khách hàng liên kết với tài khoản này. Bạn hãy thử kiểm tra lại số điện thoại hoặc email nhé!")
                    .status("SUCCESS")
                    .suggestions(getDefaultSuggestions())
                    .build();
        }

        Customer cust = custOpt.get();
        List<Booking> bookings = bookingRepository.findByCustomer_Id(cust.getId());

        if (bookings.isEmpty()) {
            return ChatbotMessageResponse.builder()
                    .reply(String.format("👋 Xin chào **%s**, hiện tại bạn chưa có phiếu đặt sân nào trên hệ thống SportHub. Bạn có muốn đặt sân thể thao hôm nay không?", cust.getFullName()))
                    .status("SUCCESS")
                    .suggestions(List.of("Xem sân bóng đá trống", "Xem sân cầu lông trống", "Bảng giá thuê sân"))
                    .build();
        }

        // Lấy 3 đơn mới nhất theo thời gian bắt đầu
        List<Booking> recent = bookings.stream()
                .sorted((a, b) -> {
                    LocalDateTime ta = (a.getBookingDetails() != null && !a.getBookingDetails().isEmpty() && a.getBookingDetails().get(0).getStartTime() != null)
                            ? a.getBookingDetails().get(0).getStartTime() : LocalDateTime.MIN;
                    LocalDateTime tb = (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty() && b.getBookingDetails().get(0).getStartTime() != null)
                            ? b.getBookingDetails().get(0).getStartTime() : LocalDateTime.MIN;
                    return tb.compareTo(ta);
                })
                .limit(3)
                .collect(Collectors.toList());

        StringBuilder reply = new StringBuilder();
        reply.append(String.format("📋 **Lịch sử đặt sân của bạn (%s):**\n\n", cust.getFullName()));
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Booking b : recent) {
            String courtName = (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty())
                    ? b.getBookingDetails().get(0).getCourt().getName() : "Sân thể thao";
            String timeRange = "";
            LocalDate bDate = LocalDate.now();
            if (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty()) {
                BookingDetail bd = b.getBookingDetails().get(0);
                if (bd.getStartTime() != null) {
                    bDate = bd.getStartTime().toLocalDate();
                    timeRange = bd.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                }
                if (bd.getEndTime() != null) {
                    timeRange += " - " + bd.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                }
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            if (b.getPayments() != null) {
                totalAmount = b.getPayments().stream()
                        .filter(p -> !"Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                        .map(Payment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            reply.append(String.format("• **Mã phiếu:** `#%s`\n", b.getId()));
            reply.append(String.format("  🏟️ **Sân:** %s | ⏰ **Khung giờ:** %s (%s)\n",
                    courtName, timeRange, bDate.format(dFmt)));
            reply.append(String.format("  💵 **Tổng tiền:** %,d đ | 📌 **Trạng thái:** `%s`\n\n",
                    totalAmount.longValue(), b.getStatus()));
        }

        reply.append("👉 Bạn có thể vào mục **[Lịch sử đặt sân](/customer/history)** để xem đầy đủ chi tiết hoặc tải hóa đơn PDF nhé!");

        return ChatbotMessageResponse.builder()
                .reply(reply.toString())
                .status("SUCCESS")
                .suggestions(List.of("Đặt thêm sân mới", "Chính sách hủy sân", "Dịch vụ giải khát F&B"))
                .build();
    }

    // =========================================================================
    // XỬ LÝ CHÍNH SÁCH HỦY & ĐỔI SÂN (POLICIES)
    // =========================================================================
    private ChatbotMessageResponse handlePolicyInquiry() {
        String reply = "📜 **CHÍNH SÁCH ĐẶT, ĐỔI & HỦY SÂN TẠI SPORTHUB:**\n\n" +
                "1. 🟢 **Hủy trước 24 giờ:**\n" +
                "   • Được **hoàn lại 100%** số tiền cọc đã thanh toán.\n\n" +
                "2. 🟡 **Hủy từ 12 đến 24 giờ trước giờ chơi:**\n" +
                "   • Áp dụng phí hủy **30%** giá trị đặt cọc, hoàn lại 70% còn lại.\n\n" +
                "3. 🔴 **Hủy dưới 12 giờ:**\n" +
                "   • Không được hoàn lại cọc do sân đã được giữ chỗ và từ chối các khách khác.\n\n" +
                "4. 🔄 **Đổi sân / Đổi giờ:**\n" +
                "   • Quý khách có thể tự thao tác đổi giờ hoặc đổi sang sân khác hoàn toàn miễn phí trên website nếu khung giờ mới còn trống!\n\n" +
                "5. ⚡ **Quy trình nhận sân:**\n" +
                "   • Đến quầy lễ tân đọc SĐT hoặc đưa mã QR phiếu đặt để nhận sân trong 5 giây.";

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(List.of("Kiểm tra sân trống hôm nay", "Bảng giá thuê sân", "Đơn đặt của tôi"))
                .build();
    }

    // =========================================================================
    // XỬ LÝ DỊCH VỤ NƯỚC UỐNG & DỤNG CỤ THỂ THAO (SERVICES)
    // =========================================================================
    private ChatbotMessageResponse handleServiceInquiry() {
        List<ServiceItem> items = serviceItemRepository.findAll();
        StringBuilder reply = new StringBuilder();
        reply.append("🥤 **MENU DỊCH VỤ NƯỚC UỐNG & CHO THUÊ DỤNG CỤ THỂ THAO:**\n\n");

        if (items.isEmpty()) {
            reply.append("• Nước khoáng Aquafina: 10,000 đ/chai\n");
            reply.append("• Nước tăng lực Revive chanh muối: 15,000 đ/chai\n");
            reply.append("• Bò Húc RedBull: 20,000 đ/lon\n");
            reply.append("• Thuê vợt cầu lông Yonex: 30,000 đ/buổi\n");
            reply.append("• Thuê bóng đá Molten tiêu chuẩn: 20,000 đ/buổi\n");
            reply.append("• Thuê vợt Pickleball carbon: 25,000 đ/buổi\n\n");
        } else {
            Map<String, List<ServiceItem>> grouped = items.stream()
                    .collect(Collectors.groupingBy(s -> s.getCategory() != null ? s.getCategory() : "Dịch vụ khác"));

            for (Map.Entry<String, List<ServiceItem>> entry : grouped.entrySet()) {
                reply.append(String.format("🔸 **%s:**\n", entry.getKey()));
                for (ServiceItem s : entry.getValue()) {
                    reply.append(String.format("  • %s: `%,d đ/%s` (%s)\n",
                            s.getName(), s.getPrice().longValue(), s.getUnit() != null ? s.getUnit() : "lần",
                            "Còn hàng".equalsIgnoreCase(s.getStatus()) ? "Sẵn sàng" : "Hết"));
                }
                reply.append("\n");
            }
        }

        reply.append("💡 Bạn có thể gọi thêm dịch vụ ngay tại quầy lễ tân hoặc thêm vào phiếu đặt sân trực tuyến bất kỳ lúc nào!");

        return ChatbotMessageResponse.builder()
                .reply(reply.toString())
                .status("SUCCESS")
                .suggestions(List.of("Sân bóng đá trống", "Sân cầu lông trống", "Bảng giá thuê sân"))
                .build();
    }

    // =========================================================================
    // 13. XỬ LÝ BẢNG GIÁ & DỰ TOÁN TÍNH TIỀN THEO THỜI LƯỢNG (PRICE & DURATION CALCULATION)
    // =========================================================================
    private ChatbotMessageResponse handlePriceInquiry(String raw, String norm) {
        List<Court> allCourts = courtRepository.findAll();
        LocalDate today = LocalDate.now();
        Double duration = parseDurationHours(norm);

        // Xác định sân cụ thể hoặc môn thể thao nếu có
        Court matchedCourt = findMatchedCourt(norm, allCourts);
        Sport matchedSport = (matchedCourt != null) ? matchedCourt.getSport() : findMatchedSport(norm);

        // Trường hợp 1: Có thời lượng cụ thể (VD: "2 tiếng rưỡi", "1 tiếng rưỡi", "30 phút", "2h",...)
        if (duration != null && duration > 0) {
            return calculatePriceForDuration(duration, matchedCourt, matchedSport, allCourts, today);
        }

        // Trường hợp 2: Hỏi giá một môn cụ thể (VD: "giá thuê sân bóng đá là bao nhiêu", "cầu lông giá sao")
        if (matchedSport != null) {
            return handleSportPriceInquiry(matchedSport, allCourts, today);
        }

        // Trường hợp 3: Hỏi giá một sân cụ thể (VD: "giá Sân bóng đá 1", "Sân cầu lông 2 bao nhiêu tiền")
        if (matchedCourt != null) {
            return handleCourtPriceInquiry(matchedCourt, today);
        }

        // Trường hợp 4: Bảng giá tổng quát tất cả các môn
        return handleGeneralPriceTable(allCourts, today);
    }

    private ChatbotMessageResponse calculatePriceForDuration(double duration, Court court, Sport sport, List<Court> allCourts, LocalDate date) {
        String durDesc = formatDurationDesc(duration);
        StringBuilder reply = new StringBuilder();

        if (court != null) {
            // 1. Tính giá cho 1 sân cụ thể
            BigDecimal hourly = getCourtPrice(court, date);
            long hourlyVal = (hourly != null) ? hourly.longValue() : 100000L;
            long total = Math.round(hourlyVal * duration);
            long deposit = Math.round(total * 0.3); // Cọc 30%
            long remaining = total - deposit;

            reply.append(String.format("💵 **DỰ TOÁN CHI PHÍ THUÊ %s (%s):**\n\n", court.getName().toUpperCase(), durDesc));
            reply.append(String.format("🏟️ **Sân:** %s (Môn: %s)\n", court.getName(), court.getSport().getName()));
            reply.append(String.format("⏰ **Thời lượng thuê:** %s\n", durDesc));
            reply.append(String.format("💲 **Đơn giá niêm yết:** `%,d đ/giờ` (tương đương `%,d đ / 30 phút`)\n\n", hourlyVal, hourlyVal / 2));
            reply.append(String.format("💰 **TỔNG TIỀN THUÊ:** `%,d đ` (%,d đ × %s giờ)\n", total, hourlyVal, formatDecimal(duration)));
            reply.append(String.format("💳 **Số tiền cọc VietQR (30%%):** `%,d đ` (để hoàn tất giữ chỗ)\n", deposit));
            reply.append(String.format("💵 **Thanh toán tại quầy khi nhận sân:** `%,d đ`\n\n", remaining));
            reply.append("🎁 **Ưu đãi áp dụng:**\n");
            reply.append("• Nhập mã `KM_FRIENDS`: Giảm ngay **20,000 đ**!\n");
            reply.append(String.format("• Tích lũy ngay **5%%** điểm hội viên (~%,d điểm) sau trận đấu.\n\n", Math.round(total * 0.05)));
            reply.append(String.format("👉 Bạn có muốn SportBot kiểm tra sân **%s** còn trống hôm nay để đặt ngay không?", court.getName()));

            return ChatbotMessageResponse.builder()
                    .reply(reply.toString())
                    .status("SUCCESS")
                    .suggestions(List.of(
                            "Kiểm tra " + court.getName() + " lúc 18h",
                            "Kiểm tra " + court.getName() + " hôm nay",
                            "Cách đặt sân trực tuyến",
                            "Menu nước uống F&B"
                    ))
                    .actionData(Map.of("courtId", court.getId(), "available", true, "courtName", court.getName()))
                    .build();

        } else if (sport != null) {
            // 2. Tính giá theo môn cụ thể (liệt kê các sân của môn đó)
            List<Court> sportCourts = allCourts.stream()
                    .filter(c -> c.getSport().getId().equals(sport.getId()))
                    .collect(Collectors.toList());

            reply.append(String.format("💵 **DỰ TOÁN CHI PHÍ THUÊ SÂN %s (%s):**\n\n", sport.getName().toUpperCase(), durDesc));
            reply.append(String.format("Hệ thống SportHub hỗ trợ thuê linh hoạt theo khung giờ (slot 30 phút). Dưới đây là chi phí dự kiến cho **%s**:\n\n", durDesc));

            for (Court sc : sportCourts) {
                BigDecimal hourly = getCourtPrice(sc, date);
                long hourlyVal = (hourly != null) ? hourly.longValue() : 100000L;
                long total = Math.round(hourlyVal * duration);
                long deposit = Math.round(total * 0.3);

                reply.append(String.format("🏟️ **%s:**\n", sc.getName()));
                reply.append(String.format("   • Đơn giá niêm yết: %,d đ/giờ\n", hourlyVal));
                reply.append(String.format("   • **Tổng tiền thuê (%s):** `%,d đ`\n", durDesc, total));
                reply.append(String.format("   • Tiền đặt cọc VietQR (30%%): `%,d đ`\n\n", deposit));
            }

            reply.append("🎁 **Ưu đãi kèm theo:** Nhập mã `KM_FRIENDS` giảm ngay 20,000 đ và tích lũy 5% điểm hội viên.\n\n");
            reply.append("👉 Bạn muốn kiểm tra sân nào đang trống để đặt giữ chỗ ngay?");

            List<String> suggestions = new ArrayList<>();
            for (Court sc : sportCourts) {
                suggestions.add("Kiểm tra " + sc.getName() + " trống không");
            }
            suggestions.add("Cách đặt sân trực tuyến");

            return ChatbotMessageResponse.builder()
                    .reply(reply.toString())
                    .status("SUCCESS")
                    .suggestions(suggestions)
                    .build();

        } else {
            // 3. Không chỉ định môn cụ thể -> Bảng tính tiền so sánh cho TẤT CẢ CÁC MÔN!
            reply.append(String.format("📊 **DỰ TOÁN CHI PHÍ THUÊ SÂN CHO %s:**\n\n", durDesc.toUpperCase()));
            reply.append(String.format("Hệ thống SportHub chia slot 30 phút linh hoạt. Chi phí thuê trọn gói cho **%s** tương ứng theo từng môn như sau:\n\n", durDesc));

            Map<String, List<Court>> grouped = allCourts.stream()
                    .collect(Collectors.groupingBy(c -> c.getSport().getName()));

            for (Map.Entry<String, List<Court>> entry : grouped.entrySet()) {
                String sportName = entry.getKey();
                List<Court> cList = entry.getValue();
                String icon = getSportIcon(sportName);

                reply.append(String.format("%s **%s:**\n", icon, sportName.toUpperCase()));
                for (Court c : cList) {
                    BigDecimal hourly = getCourtPrice(c, date);
                    long hourlyVal = (hourly != null) ? hourly.longValue() : 100000L;
                    long total = Math.round(hourlyVal * duration);
                    long deposit = Math.round(total * 0.3);

                    reply.append(String.format("   • **%s:** `%,d đ` *(Đơn giá: %,d đ/h | Cọc 30%%: %,d đ)*\n",
                            c.getName(), total, hourlyVal, deposit));
                }
                reply.append("\n");
            }

            reply.append("💡 **Chính sách đặt cọc & thanh toán:**\n");
            reply.append("• Bạn chỉ cần đặt cọc **30%** qua chuyển khoản VietQR tự động để giữ chỗ ngay lập tức.\n");
            reply.append("• Số tiền còn lại sẽ thanh toán tại quầy lễ tân khi bạn đến nhận sân.\n");
            reply.append("• Nhập mã `KM_FRIENDS` để được giảm ngay **20,000 đ** vào đơn đặt sân!\n\n");
            reply.append("👉 Bạn muốn kiểm tra sân trống môn nào để SportBot hỗ trợ đặt ngay?");

            return ChatbotMessageResponse.builder()
                    .reply(reply.toString())
                    .status("SUCCESS")
                    .suggestions(List.of(
                            "Kiểm tra sân bóng đá hôm nay",
                            "Kiểm tra sân cầu lông hôm nay",
                            "Kiểm tra sân pickleball hôm nay",
                            "Hướng dẫn cách đặt sân"
                    ))
                    .build();
        }
    }

    private ChatbotMessageResponse handleSportPriceInquiry(Sport sport, List<Court> allCourts, LocalDate date) {
        List<Court> sportCourts = allCourts.stream()
                .filter(c -> c.getSport().getId().equals(sport.getId()))
                .collect(Collectors.toList());

        String icon = getSportIcon(sport.getName());
        StringBuilder reply = new StringBuilder();
        reply.append(String.format("%s **BẢNG GIÁ THUÊ SÂN %s HIỆN HÀNH:**\n\n", icon, sport.getName().toUpperCase()));

        for (Court c : sportCourts) {
            BigDecimal hourly = getCourtPrice(c, date);
            long hourlyVal = (hourly != null) ? hourly.longValue() : 100000L;
            reply.append(String.format("🏟️ **%s:** `%,d đ/giờ` (tương đương `%,d đ / 30 phút`)\n",
                    c.getName(), hourlyVal, hourlyVal / 2));
        }

        // Bảng tính giá mẫu cho các thời lượng phổ biến
        if (!sportCourts.isEmpty()) {
            Court sampleCourt = sportCourts.get(0);
            BigDecimal hourly = getCourtPrice(sampleCourt, date);
            long hourlyVal = (hourly != null) ? hourly.longValue() : 100000L;

            reply.append(String.format("\n⏰ **Dự toán giá thuê mẫu (%s):**\n", sampleCourt.getName()));
            reply.append(String.format("• 30 phút: `%,d đ` (Cọc: %,d đ)\n", hourlyVal / 2, Math.round(hourlyVal * 0.5 * 0.3)));
            reply.append(String.format("• 1 tiếng: `%,d đ` (Cọc: %,d đ)\n", hourlyVal, Math.round(hourlyVal * 0.3)));
            reply.append(String.format("• 1 tiếng rưỡi (90p): `%,d đ` (Cọc: %,d đ)\n", Math.round(hourlyVal * 1.5), Math.round(hourlyVal * 1.5 * 0.3)));
            reply.append(String.format("• 2 tiếng: `%,d đ` (Cọc: %,d đ)\n", hourlyVal * 2, Math.round(hourlyVal * 2 * 0.3)));
            reply.append(String.format("• 2 tiếng rưỡi (150p): `%,d đ` (Cọc: %,d đ)\n", Math.round(hourlyVal * 2.5), Math.round(hourlyVal * 2.5 * 0.3)));
            reply.append(String.format("• 3 tiếng: `%,d đ` (Cọc: %,d đ)\n", hourlyVal * 3, Math.round(hourlyVal * 3 * 0.3)));
        }

        reply.append("\n💡 *Giá trên đã bao gồm hệ thống chiếu sáng đèn LED, lưới, vạch tiêu chuẩn và nước rửa tay tại sân.*");

        List<String> suggestions = new ArrayList<>();
        for (Court c : sportCourts) {
            suggestions.add("Kiểm tra " + c.getName() + " hôm nay");
        }
        suggestions.add("Cách đặt sân trực tuyến");

        return ChatbotMessageResponse.builder()
                .reply(reply.toString())
                .status("SUCCESS")
                .suggestions(suggestions)
                .build();
    }

    private ChatbotMessageResponse handleCourtPriceInquiry(Court court, LocalDate date) {
        BigDecimal hourly = getCourtPrice(court, date);
        long hourlyVal = (hourly != null) ? hourly.longValue() : 100000L;
        String icon = getSportIcon(court.getSport().getName());

        StringBuilder reply = new StringBuilder();
        reply.append(String.format("%s **BẢNG GIÁ THUÊ %s:**\n\n", icon, court.getName().toUpperCase()));
        reply.append(String.format("🏟️ **Môn thể thao:** %s\n", court.getSport().getName()));
        reply.append(String.format("💲 **Đơn giá niêm yết:** `%,d đ/giờ` (tương đương `%,d đ / 30 phút`)\n\n", hourlyVal, hourlyVal / 2));

        reply.append("⏰ **Dự toán chi phí theo thời lượng:**\n");
        reply.append(String.format("• 30 phút: `%,d đ` (Cọc: %,d đ)\n", hourlyVal / 2, Math.round(hourlyVal * 0.5 * 0.3)));
        reply.append(String.format("• 1 tiếng: `%,d đ` (Cọc: %,d đ)\n", hourlyVal, Math.round(hourlyVal * 0.3)));
        reply.append(String.format("• 1 tiếng rưỡi (90p): `%,d đ` (Cọc: %,d đ)\n", Math.round(hourlyVal * 1.5), Math.round(hourlyVal * 1.5 * 0.3)));
        reply.append(String.format("• 2 tiếng: `%,d đ` (Cọc: %,d đ)\n", hourlyVal * 2, Math.round(hourlyVal * 2 * 0.3)));
        reply.append(String.format("• 2 tiếng rưỡi (150p): `%,d đ` (Cọc: %,d đ)\n", Math.round(hourlyVal * 2.5), Math.round(hourlyVal * 2.5 * 0.3)));
        reply.append(String.format("• 3 tiếng: `%,d đ` (Cọc: %,d đ)\n\n", hourlyVal * 3, Math.round(hourlyVal * 3 * 0.3)));

        reply.append("🎁 Áp dụng mã `KM_FRIENDS` để giảm ngay 20,000 đ vào đơn đặt sân!");

        return ChatbotMessageResponse.builder()
                .reply(reply.toString())
                .status("SUCCESS")
                .suggestions(List.of(
                        "Kiểm tra " + court.getName() + " lúc 18h",
                        "Kiểm tra " + court.getName() + " hôm nay",
                        "Cách đặt sân trực tuyến",
                        "Bảng giá các môn khác"
                ))
                .actionData(Map.of("courtId", court.getId(), "available", true, "courtName", court.getName()))
                .build();
    }

    private ChatbotMessageResponse handleGeneralPriceTable(List<Court> courts, LocalDate today) {
        StringBuilder reply = new StringBuilder();
        reply.append("💰 **BẢNG GIÁ THUÊ SÂN HIỆN HÀNH TẠI SPORTHUB:**\n\n");

        Map<String, List<Court>> sportMap = courts.stream()
                .collect(Collectors.groupingBy(c -> c.getSport().getName()));

        for (Map.Entry<String, List<Court>> entry : sportMap.entrySet()) {
            String sportName = entry.getKey();
            String icon = getSportIcon(sportName);
            reply.append(String.format("%s **%s:**\n", icon, sportName.toUpperCase()));
            for (Court c : entry.getValue()) {
                BigDecimal p = getCourtPrice(c, today);
                String pStr = (p != null) ? String.format("%,d đ/giờ", p.longValue()) : "100,000 đ/giờ";
                reply.append(String.format("  • %s: `%s`\n", c.getName(), pStr));
            }
            reply.append("\n");
        }

        reply.append("⏰ *Lưu ý: Giá trên đã bao gồm hệ thống chiếu sáng đèn LED và nước rửa tay tại sân. Giờ hoạt động từ 05:00 đến 23:00 hàng ngày.*\n\n");
        reply.append("💡 **Mẹo tính tiền:** Bạn có thể hỏi ví dụ: *\"nếu tôi đặt sân 2 tiếng rưỡi sẽ là bao nhiêu\"* hoặc *\"thuê sân bóng đá 2h giá sao\"* để SportBot tính tiền tự động tức thì nhé!");

        return ChatbotMessageResponse.builder()
                .reply(reply.toString())
                .status("SUCCESS")
                .suggestions(List.of(
                        "Đặt sân 2 tiếng rưỡi bao nhiêu tiền?",
                        "Kiểm tra Sân bóng đá 1 lúc 18h",
                        "Kiểm tra Sân cầu lông 1 hôm nay",
                        "Menu nước uống F&B"
                ))
                .build();
    }

    // =========================================================================
    // XỬ LÝ LỜI CHÀO & GIỚI THIỆU (GREETING)
    // =========================================================================
    private ChatbotMessageResponse handleGreeting(String usernameOrEmail) {
        String name = "bạn";
        if (usernameOrEmail != null && !usernameOrEmail.isBlank() && !"khachle".equalsIgnoreCase(usernameOrEmail)) {
            Optional<Customer> cust = customerRepository.findByEmail(usernameOrEmail);
            if (cust.isPresent() && cust.get().getFullName() != null) {
                name = cust.get().getFullName();
            }
        }

        String reply = String.format(
                "👋 **Dạ SportBot xin chào %s!** Rất vui được đồng hành cùng bạn tại **Trung tâm Thể thao SportHub** 🏸⚽🏀🎾\n\n" +
                "SportBot có thể hỗ trợ bạn tức thì các thông tin:\n" +
                "• 📖 **Hướng dẫn cách đặt sân & thanh toán VietQR**\n" +
                "• 🔍 **Kiểm tra tình trạng sân trống thời gian thực** (VD: *\"Sân bóng đá 1 lúc 18h hôm nay đã ai đặt chưa?\"*)\n" +
                "• 💰 **Bảng giá thuê sân & khung giờ vàng**\n" +
                "• 🥤 **Dịch vụ F&B, nước giải khát & thuê vợt, bóng**\n" +
                "• 📜 **Chính sách hủy sân & hoàn tiền cọc**\n" +
                "• 📍 **Địa chỉ & giờ mở cửa trung tâm**\n\n" +
                "Bạn cần SportBot hỗ trợ thông tin gì ngay bây giờ ạ?", name);

        return ChatbotMessageResponse.builder()
                .reply(reply)
                .status("SUCCESS")
                .suggestions(getDefaultSuggestions())
                .build();
    }

    // =========================================================================
    // CÁC HÀM TIỆN ÍCH TRÍCH XUẤT THÔNG TIN TỰ NHIÊN (NLP UTILS)
    // =========================================================================
    private boolean isAvailabilityQuery(String norm) {
        return norm.contains("con trong") || norm.contains("ai dat chua") || norm.contains("co ai dat")
                || norm.contains("trong khong") || norm.contains("trong gio nao") || norm.contains("san nao trong")
                || norm.contains("gio nao con") || norm.contains("kiem tra san") || norm.contains("san trong")
                || norm.contains("co san nao") || norm.contains("dat duoc khong")
                || norm.contains("con san nao") || norm.contains("tinh trang san")
                || (findMatchedCourt(norm, courtRepository.findAll()) != null && parseTime(norm) != null);
    }

    private Court findMatchedCourt(String norm, List<Court> allCourts) {
        // 1. Khớp chính xác tên sân
        for (Court c : allCourts) {
            String cNorm = removeAccents(c.getName().toLowerCase());
            if (isCourtMatched(norm, cNorm)) return c;
        }

        // 2. Khớp các từ viết tắt / từ lóng phổ biến
        if (isCourtMatched(norm, "bong da 1", "sbd 1", "sbd1", "da 1", "bong da so 1", "san so 1 bong da", "san 1 bong da")) {
            return findCourtById(allCourts, "S01");
        }
        if (isCourtMatched(norm, "bong da 2", "sbd 2", "sbd2", "da 2", "bong da so 2", "san so 2 bong da", "san 2 bong da")) {
            return findCourtById(allCourts, "S02");
        }
        if (isCourtMatched(norm, "cau long 1", "scl 1", "scl1", "long 1", "cau long so 1", "san so 1 cau long", "san 1 cau long")) {
            return findCourtById(allCourts, "S03");
        }
        if (isCourtMatched(norm, "cau long 2", "scl 2", "scl2", "long 2", "cau long so 2", "san so 2 cau long", "san 2 cau long")) {
            return findCourtById(allCourts, "S04");
        }
        if (isCourtMatched(norm, "bong ro 1", "sbr 1", "sbr1", "ro 1", "bong ro so 1", "san so 1 bong ro", "san 1 bong ro")) {
            return findCourtById(allCourts, "S05");
        }
        if (isCourtMatched(norm, "bong ro 2", "sbr 2", "sbr2", "ro 2", "bong ro so 2", "san so 2 bong ro", "san 2 bong ro")) {
            return findCourtById(allCourts, "S06");
        }
        if (isCourtMatched(norm, "pickleball 1", "spk 1", "spk1", "pickle 1", "pickleball so 1", "san so 1 pickleball", "san 1 pickleball")) {
            return findCourtById(allCourts, "S07");
        }
        if (isCourtMatched(norm, "pickleball 2", "spk 2", "spk2", "pickle 2", "pickleball so 2", "san so 2 pickleball", "san 2 pickleball")) {
            return findCourtById(allCourts, "S08");
        }

        return null;
    }

    private boolean isCourtMatched(String norm, String... keywords) {
        Pattern durUnit = Pattern.compile("^\\s*(?:tieng|gio|h\\b|phut|p\\b)");
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = norm.indexOf(kw, idx)) != -1) {
                if (Character.isDigit(kw.charAt(kw.length() - 1))) {
                    String after = norm.substring(idx + kw.length());
                    if (durUnit.matcher(after).find()) {
                        idx += kw.length();
                        continue;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private Sport findMatchedSport(String norm) {
        List<Sport> sports = sportRepository.findAll();
        for (Sport s : sports) {
            String sNorm = removeAccents(s.getName().toLowerCase());
            if (norm.contains(sNorm)) return s;
        }
        if (norm.contains("bong da") || norm.contains("da banh")) return findSportByName(sports, "Bóng đá");
        if (norm.contains("cau long")) return findSportByName(sports, "Cầu lông");
        if (norm.contains("bong ro")) return findSportByName(sports, "Bóng rổ");
        if (norm.contains("pickleball") || norm.contains("pickle")) return findSportByName(sports, "Pickleball");
        return null;
    }

    private Court findCourtById(List<Court> list, String id) {
        return list.stream().filter(c -> c.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    private Sport findSportByName(List<Sport> list, String name) {
        return list.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    private LocalDate parseDate(String norm) {
        LocalDate today = LocalDate.now();
        if (norm.contains("ngay mai") || norm.contains("mai") || norm.contains("tomorrow")) {
            return today.plusDays(1);
        }
        if (norm.contains("ngay mot") || norm.contains("mot")) {
            return today.plusDays(2);
        }

        // Khớp ngày định dạng dd/MM hoặc dd/MM/yyyy
        Pattern p = Pattern.compile("(\\d{1,2})[\\/\\-](\\d{1,2})([\\/\\-](\\d{2,4}))?");
        Matcher m = p.matcher(norm);
        if (m.find()) {
            try {
                int day = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int year = today.getYear();
                if (m.group(4) != null) {
                    year = Integer.parseInt(m.group(4));
                    if (year < 100) year += 2000;
                }
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {
            }
        }

        return today; // Mặc định là hôm nay
    }

    private LocalTime parseTime(String norm) {
        // Tránh nhầm lẫn thời lượng (VD: "2 gio ruoi", "thue 2 gio", "2 tieng") thành mốc giờ bắt đầu
        if (norm.contains("gio ruoi") || norm.contains("tieng ruoi") || norm.contains("nua tieng") || norm.contains("nua gio")) {
            // Chỉ bắt mốc giờ bắt đầu nếu có tiền tố "luc", "tu", "vao" (VD: "luc 18h trong 2 tieng ruoi")
            Pattern pAt = Pattern.compile("(?:luc|tu|vao)\\s*(\\d{1,2})[h:](\\d{2})?");
            Matcher mAt = pAt.matcher(norm);
            if (mAt.find()) {
                int h = Integer.parseInt(mAt.group(1));
                int min = (mAt.group(2) != null) ? Integer.parseInt(mAt.group(2)) : 0;
                if (h <= 11 && (norm.contains("toi") || norm.contains("chieu") || norm.contains("pm"))) h += 12;
                return LocalTime.of(h, min);
            }
            return null;
        }

        // 1. Dạng HH:mm (VD: 18:00, 18:30)
        Pattern pCol = Pattern.compile("(\\d{1,2}):(\\d{2})");
        Matcher mCol = pCol.matcher(norm);
        if (mCol.find()) {
            int h = Integer.parseInt(mCol.group(1));
            int min = Integer.parseInt(mCol.group(2));
            if (h >= 0 && h <= 23 && min >= 0 && min <= 59) {
                return LocalTime.of(h, min);
            }
        }

        // 2. Dạng Xh hoặc XhY (VD: 18h, 18h30, 6h, 6h30)
        Pattern pH = Pattern.compile("(\\d{1,2})h(\\d{2})?");
        Matcher mH = pH.matcher(norm);
        if (mH.find()) {
            int h = Integer.parseInt(mH.group(1));
            int min = (mH.group(2) != null) ? Integer.parseInt(mH.group(2)) : 0;
            // Nếu người dùng nói "6h tối" hoặc "7h tối"
            if (h <= 11 && (norm.contains("toi") || norm.contains("chieu") || norm.contains("pm"))) {
                h += 12;
            }
            // Nếu h < 5 mà không có chỉ định tối/chiều/sáng, thường là số lượng giờ thuê (VD: "thue 2h") chứ không phải 2h sáng
            if (h >= 5 && h <= 23 && min >= 0 && min <= 59) {
                return LocalTime.of(h, min);
            }
        }

        // 3. Dạng "X gio" (VD: "18 gio", "6 gio toi")
        Pattern pG = Pattern.compile("(?:luc|tu|vao)?\\s*(\\d{1,2})\\s*gio");
        Matcher mG = pG.matcher(norm);
        if (mG.find()) {
            int h = Integer.parseInt(mG.group(1));
            if (h <= 11 && (norm.contains("toi") || norm.contains("chieu") || norm.contains("pm"))) {
                h += 12;
            }
            if (h >= 5 && h <= 23) {
                return LocalTime.of(h, 0);
            }
        }

        return null; // Không đề cập giờ cụ thể
    }

    private Double parseDurationHours(String norm) {
        // 1. "X tieng ruoi" / "X gio ruoi" / "mot tieng ruoi", "hai tieng ruoi", ...
        Pattern pRuoi = Pattern.compile("(\\d+|mot|hai|ba|bon|nam|sau)\\s*(?:tieng|gio|h)\\s*ruoi");
        Matcher mRuoi = pRuoi.matcher(norm);
        if (mRuoi.find()) {
            double base = parseWordOrNumber(mRuoi.group(1));
            return base + 0.5;
        }

        // 2. "nua tieng" / "nua gio"
        if (norm.contains("nua tieng") || norm.contains("nua gio")) {
            return 0.5;
        }

        // 3. "X tieng Y phut" / "X gio Y phut" / "XhY" (như 2h30, 1h30)
        Pattern pHourMin = Pattern.compile("(\\d+)\\s*(?:tieng|gio|h)\\s*(\\d+)\\s*(?:phut|p)?");
        Matcher mHourMin = pHourMin.matcher(norm);
        if (mHourMin.find()) {
            double h = Double.parseDouble(mHourMin.group(1));
            double m = Double.parseDouble(mHourMin.group(2));
            return h + (m / 60.0);
        }

        // 4. Decimal: "2.5 tieng", "2,5 gio", "1.5h"
        Pattern pDec = Pattern.compile("(\\d+[.,]\\d+)\\s*(?:tieng|gio|h)");
        Matcher mDec = pDec.matcher(norm);
        if (mDec.find()) {
            return Double.parseDouble(mDec.group(1).replace(',', '.'));
        }

        // 5. "X phut" (VD: 30 phut, 45 phut, 90 phut, 150 phut)
        Pattern pMin = Pattern.compile("(\\d+)\\s*(?:phut|p\\b)");
        Matcher mMin = pMin.matcher(norm);
        if (mMin.find()) {
            int mins = Integer.parseInt(mMin.group(1));
            if (!norm.contains("h" + mins) && !norm.contains("gio " + mins)) {
                return mins / 60.0;
            }
        }

        // 6. "X tieng" / "X gio"
        Pattern pExact = Pattern.compile("(?:trong|thue|dat|choi|tap)?\\s*(\\d+|mot|hai|ba|bon|nam|sau)\\s*(?:tieng|gio)(?!\\s*toi)(?!\\s*sang)(?!\\s*chieu)");
        Matcher mExact = pExact.matcher(norm);
        if (mExact.find()) {
            double val = parseWordOrNumber(mExact.group(1));
            if (val > 10 && (norm.contains("luc") || norm.contains("tu"))) {
                return null;
            }
            return val;
        }

        // 7. "Xh" đơn thuần khi đi kèm từ thue, dat, trong, choi
        Pattern pH = Pattern.compile("(?:thue|dat|choi|tap|trong)\\s*(\\d+)\\s*h\\b");
        Matcher mH = pH.matcher(norm);
        if (mH.find()) {
            return Double.parseDouble(mH.group(1));
        }

        return null;
    }

    private double parseWordOrNumber(String s) {
        if (s == null) return 1.0;
        switch (s.trim().toLowerCase()) {
            case "mot": return 1.0;
            case "hai": return 2.0;
            case "ba": return 3.0;
            case "bon": return 4.0;
            case "nam": return 5.0;
            case "sau": return 6.0;
            default:
                try {
                    return Double.parseDouble(s);
                } catch (Exception e) {
                    return 1.0;
                }
        }
    }

    private String formatDurationDesc(double duration) {
        if (Math.abs(duration - 0.5) < 0.01) return "30 phút (nửa tiếng)";
        if (Math.abs(duration - 1.0) < 0.01) return "1 tiếng (60 phút)";
        if (Math.abs(duration - 1.5) < 0.01) return "1 tiếng rưỡi (90 phút / 1.5 giờ)";
        if (Math.abs(duration - 2.0) < 0.01) return "2 tiếng (120 phút)";
        if (Math.abs(duration - 2.5) < 0.01) return "2 tiếng rưỡi (150 phút / 2.5 giờ)";
        if (Math.abs(duration - 3.0) < 0.01) return "3 tiếng (180 phút)";
        if (Math.abs(duration - 3.5) < 0.01) return "3 tiếng rưỡi (210 phút / 3.5 giờ)";
        if (Math.abs(duration - 4.0) < 0.01) return "4 tiếng (240 phút)";
        return String.format("%s tiếng (%d phút)", formatDecimal(duration), Math.round(duration * 60));
    }

    private String formatDecimal(double d) {
        if (d == Math.floor(d)) {
            return String.format("%.0f", d);
        }
        return String.format(Locale.US, "%.1f", d);
    }

    private String getSportIcon(String sportName) {
        if (sportName == null) return "🏟️";
        String s = removeAccents(sportName.toLowerCase());
        if (s.contains("bong da") || s.contains("da banh")) return "⚽";
        if (s.contains("cau long")) return "🏸";
        if (s.contains("bong ro")) return "🏀";
        if (s.contains("pickleball")) return "🎾";
        return "🏟️";
    }

    private boolean isPriceOrDurationQuery(String norm) {
        if (norm.contains("con trong") || norm.contains("ai dat chua") || norm.contains("co ai dat")
                || norm.contains("trong khong") || norm.contains("san nao trong") || norm.contains("gio nao con")) {
            return false;
        }

        if (norm.contains("gia") || norm.contains("bang gia") || norm.contains("bao nhieu")
                || norm.contains("chi phi") || norm.contains("tinh tien") || norm.contains("tien thue")
                || norm.contains("phi thue") || norm.contains("gia sao") || norm.contains("het may")
                || norm.contains("ton may") || norm.contains("bao tien") || norm.contains("tinh gia")
                || norm.contains("se la bao nhieu")) {
            return true;
        }

        // Nếu có thời lượng cụ thể và đi kèm các từ đặt, thuê, sân, tiền, tính
        Double dur = parseDurationHours(norm);
        if (dur != null && dur > 0 && (norm.contains("dat") || norm.contains("thue") || norm.contains("san")
                || norm.contains("tien") || norm.contains("se la") || norm.contains("ton") || norm.contains("het"))) {
            return true;
        }

        return false;
    }

    private BigDecimal getCourtPrice(Court court, LocalDate date) {
        Optional<Price> pOpt = priceRepository.findEffectivePrice(court.getId(), date);
        if (pOpt.isPresent() && pOpt.get().getPriceDetails() != null && !pOpt.get().getPriceDetails().isEmpty()) {
            return pOpt.get().getPriceDetails().get(0).getPriceAmount();
        }
        // Giá mặc định theo môn
        String s = court.getSport().getName();
        if ("Bóng đá".equalsIgnoreCase(s)) return new BigDecimal("120000");
        if ("Cầu lông".equalsIgnoreCase(s)) return new BigDecimal("60000");
        if ("Bóng rổ".equalsIgnoreCase(s)) return new BigDecimal("100000");
        if ("Pickleball".equalsIgnoreCase(s)) return new BigDecimal("50000");
        return new BigDecimal("80000");
    }

    private boolean isMatchmakingQuery(String norm) {
        return norm.contains("ghep keo") || norm.contains("tim doi") || norm.contains("tim ban")
                || norm.contains("bat keo") || norm.contains("bat doi") || norm.contains("giao luu")
                || norm.contains("co keo nao") || norm.contains("keo nao") || norm.contains("tim tran")
                || norm.contains("ghep doi") || norm.contains("giao huu") || norm.contains("thieu nguoi")
                || norm.contains("bang ghep") || norm.contains("doi thu") || norm.contains("keo bong")
                || norm.contains("keo cau long") || norm.contains("keo pickleball") || norm.contains("keo bong ro");
    }

    private boolean isGreeting(String norm) {
        return norm.matches(".*\\b(chao|xin chao|hello|hi|bot|alo|start)\\b.*")
                && !norm.contains("con trong") && !norm.contains("ai dat chua") && !norm.contains("gia")
                && !norm.contains("cach dat") && !norm.contains("huong dan");
    }

    private String removeAccents(String text) {
        if (text == null) return "";
        String n = Normalizer.normalize(text, Normalizer.Form.NFD);
        String res = n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return res.replace('đ', 'd').replace('Đ', 'D');
    }

    private List<String> getDefaultSuggestions() {
        return List.of(
                "Ghép kèo giao lưu / Tìm đối thủ",
                "Cách đặt sân trực tuyến như thế nào?",
                "Sân bóng đá 1 lúc 18h đã ai đặt chưa?",
                "Hôm nay có sân cầu lông nào trống?",
                "Phương thức thanh toán VietQR",
                "Bảng giá thuê các loại sân?",
                "Địa chỉ & giờ mở cửa trung tâm"
        );
    }

    private List<String> getAlternativeCourtSuggestions(Court court, List<Court> allCourts) {
        List<String> list = new ArrayList<>();
        allCourts.stream()
                .filter(c -> !c.getId().equals(court.getId()) && c.getSport().getId().equals(court.getSport().getId()))
                .limit(2)
                .forEach(c -> list.add("Kiểm tra " + c.getName()));
        list.add("Bảng giá " + court.getSport().getName());
        list.add("Sân nào đang trống hôm nay?");
        return list;
    }
}
