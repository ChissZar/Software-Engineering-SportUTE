package vn.edu.ute.sportute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import vn.edu.ute.sportute.dto.request.CustomerBookingRequestDTO;
import vn.edu.ute.sportute.dto.request.CustomerCourtFilterDTO;
import vn.edu.ute.sportute.dto.response.CustomerCourtCardDTO;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.CustomerBookingService;
import vn.edu.ute.sportute.service.EmailService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerBookingServiceImpl implements CustomerBookingService {

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private vn.edu.ute.sportute.service.MatchmakingService matchmakingService;

    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PriceRepository priceRepository;
    private final BookingPolicyRepository bookingPolicyRepository;
    private final PaymentRepository paymentRepository;
    private final vn.edu.ute.sportute.repository.PromotionRepository promotionRepository;

    public CustomerBookingServiceImpl(CourtRepository courtRepository,
                                      BookingRepository bookingRepository,
                                      BookingDetailRepository bookingDetailRepository,
                                      MaintenanceRepository maintenanceRepository,
                                      AccountRepository accountRepository,
                                      CustomerRepository customerRepository,
                                      PriceRepository priceRepository,
                                      BookingPolicyRepository bookingPolicyRepository,
                                      PaymentRepository paymentRepository,
                                      vn.edu.ute.sportute.repository.PromotionRepository promotionRepository) {
        this.courtRepository = courtRepository;
        this.bookingRepository = bookingRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.priceRepository = priceRepository;
        this.bookingPolicyRepository = bookingPolicyRepository;
        this.paymentRepository = paymentRepository;
        this.promotionRepository = promotionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerCourtCardDTO> searchAvailableCourts(CustomerCourtFilterDTO filter) {
        List<Court> allCourts = courtRepository.findAll();
        List<CustomerCourtCardDTO> result = new ArrayList<>();

        LocalDate bookingDate = filter.getBookingDate() != null ? filter.getBookingDate() : LocalDate.now();
        LocalTime startTime = filter.getStartTime() != null ? filter.getStartTime() : LocalTime.of(17, 0);
        int durationMinutes = filter.getDurationMinutes() > 0 ? filter.getDurationMinutes() : 60;

        LocalDateTime startDateTime = LocalDateTime.of(bookingDate, startTime);
        LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);

        for (Court court : allCourts) {
            // 1. Lọc theo trạng thái hoạt động
            if (!"Hoạt động".equalsIgnoreCase(court.getStatus()) && !"Đang sử dụng".equalsIgnoreCase(court.getStatus())) {
                continue;
            }

            // 2. Lọc theo môn thể thao nếu được chọn
            if (filter.getSportId() != null && !filter.getSportId().trim().isEmpty() && !"ALL".equalsIgnoreCase(filter.getSportId())) {
                if (court.getSport() == null || !court.getSport().getId().equalsIgnoreCase(filter.getSportId().trim())) {
                    continue;
                }
            }

            // 3. Lọc theo từ khóa tìm kiếm (Tên sân hoặc mã sân)
            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String kw = filter.getKeyword().trim().toLowerCase();
                boolean matchName = court.getName().toLowerCase().contains(kw);
                boolean matchId = court.getId().toLowerCase().contains(kw);
                if (!matchName && !matchId) continue;
            }

            // 4. Kiểm tra xung đột với lịch bảo trì (RB2)
            boolean inMaintenance = maintenanceRepository.existsOverlappingMaintenance(court.getId(), startDateTime, endDateTime);
            if (inMaintenance) continue;

            // 5. Kiểm tra trùng lịch đặt sân đang hiệu lực (RB1)
            boolean isBooked = bookingDetailRepository.existsOverlappingBooking(court.getId(), startDateTime, endDateTime);
            if (isBooked) continue;

            // 6. Xác định mức giá thuê VND/giờ theo bảng giá hoặc môn
            BigDecimal pricePerHour = getCourtHourlyRate(court.getId(), bookingDate, court.getSport());

            // 7. Gán thông tin tiện ích và theme màu sắc theo từng môn thể thao
            List<String> amenities = new ArrayList<>();
            String headerColor = "bg-success text-white";
            String imageUrl = "/images/courts/badminton.jpg";
            String spec = "2 sân tiêu chuẩn";
            String extraAmenity = "Có WiFi";
            String operatingHours = "05:00 - 23:00";
            String loc = (court.getLocation() != null && !court.getLocation().isBlank()) 
                    ? court.getLocation() : "Nhà thi đấu SportHub";

            if (court.getSport() != null) {
                String mId = court.getSport().getId();
                if ("M_BD".equalsIgnoreCase(mId)) {
                    amenities = Arrays.asList("Cỏ nhân tạo FIFA", "Đèn chiếu LED 400W", "Có mái che");
                    headerColor = "bg-success text-white";
                    imageUrl = "/images/courts/soccer.jpg";
                    spec = "Sân 5 người";
                    extraAmenity = "Có bãi giữ xe";
                    operatingHours = "06:00 - 24:00";
                    loc = "Sân bóng đá SportHub";
                } else if ("M_CL".equalsIgnoreCase(mId)) {
                    amenities = Arrays.asList("Thảm PVC tiêu chuẩn BWF", "Trong nhà máy lạnh", "Hệ thống chống lóa");
                    headerColor = "bg-primary text-white";
                    imageUrl = "/images/courts/badminton.jpg";
                    spec = "2 sân tiêu chuẩn";
                    extraAmenity = "Có WiFi";
                    operatingHours = "05:00 - 23:00";
                    loc = "Nhà thi đấu SportHub";
                } else if ("M_BR".equalsIgnoreCase(mId)) {
                    amenities = Arrays.asList("Sàn gỗ giảm chấn", "Bảng rổ chuẩn FIBA", "Khu khán đài");
                    headerColor = "bg-warning text-dark";
                    imageUrl = "/images/courts/basketball.jpg";
                    spec = "Sân tiêu chuẩn";
                    extraAmenity = "Có WiFi";
                    operatingHours = "05:00 - 23:00";
                    loc = "Nhà thi đấu SportHub";
                } else if ("M_PB".equalsIgnoreCase(mId)) {
                    amenities = Arrays.asList("Sơn phủ cao cấp", "Lưới đạt chuẩn", "Khu vực nghỉ ngơi");
                    headerColor = "bg-info text-white";
                    imageUrl = "/images/courts/pickleball.jpg";
                    spec = "2 sân tiêu chuẩn";
                    extraAmenity = "Có bãi giữ xe";
                    operatingHours = "05:00 - 23:00";
                    loc = "Khu thể thao ngoài trời";
                }
            }

            List<String> slots = Arrays.asList("16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00");

            result.add(CustomerCourtCardDTO.builder()
                    .id(court.getId())
                    .name(court.getName())
                    .sportId(court.getSport() != null ? court.getSport().getId() : "")
                    .sportName(court.getSport() != null ? court.getSport().getName() : "Thể thao")
                    .location(loc)
                    .status("Còn trống")
                    .pricePerHour(pricePerHour)
                    .amenities(amenities)
                    .timeSlots(slots)
                    .colorHeaderClass(headerColor)
                    .imageUrl(imageUrl)
                    .spec(spec)
                    .extraAmenity(extraAmenity)
                    .operatingHours(operatingHours)
                    .build());
        }

        return result;
    }

    @Override
    public Booking bookCourtOnline(CustomerBookingRequestDTO request, String accountUsernameOrEmail) {
        LocalDateTime startDateTime = LocalDateTime.of(request.getBookingDate(), request.getStartTime());
        LocalDateTime endDateTime = startDateTime.plusMinutes(request.getDurationMinutes());

        // 1. Kiểm tra lại lịch trống và bảo trì nguyên tử (UC41)
        if (maintenanceRepository.existsOverlappingMaintenance(request.getCourtId(), startDateTime, endDateTime)) {
            throw new IllegalArgumentException("Rất tiếc! Sân đã được xếp lịch bảo trì trong khung giờ này.");
        }
        if (bookingDetailRepository.existsOverlappingBooking(request.getCourtId(), startDateTime, endDateTime)) {
            throw new IllegalArgumentException("Khung giờ bạn chọn vừa có người khác đặt trước. Vui lòng chọn giờ hoặc sân khác!");
        }

        // 2. Tìm thông tin khách hàng từ tài khoản đang đăng nhập
        Customer customer = resolveCustomer(accountUsernameOrEmail);

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại"));

        // Lấy chính sách cọc mặc định
        BookingPolicy defaultPolicy = bookingPolicyRepository.findAll().stream().findFirst().orElse(null);

        // Tìm khuyến mãi / voucher nếu có
        vn.edu.ute.sportute.entity.Promotion promotion = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            String vCode = request.getVoucherCode().trim();
            promotion = promotionRepository.findById(vCode).orElse(null);
            if (promotion == null) {
                promotion = promotionRepository.findAll().stream()
                        .filter(p -> p.getId().equalsIgnoreCase(vCode) || p.getName().equalsIgnoreCase(vCode))
                        .findFirst().orElse(null);
            }
        }

        // 3. Tạo phiếu đặt sân trực tuyến
        Booking booking = Booking.builder()
                .id(IdGenerator.generateId("DS"))
                .customer(customer)
                .employee(null) // Đặt online không qua nhân viên lễ tân lập
                .bookingPolicy(defaultPolicy)
                .promotion(promotion)
                .status("Đã xác nhận") // Thanh toán thành công -> chuyển thẳng sang Đã xác nhận giữ sân
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(IdGenerator.generateId("CT"))
                .booking(booking)
                .court(court)
                .startTime(startDateTime)
                .endTime(endDateTime)
                .build();

        booking.getBookingDetails().add(detail);
        Booking savedBooking = bookingRepository.save(booking);

        // 4. Lưu nghĩa vụ thanh toán (Cọc hoặc Toàn phần)
        BigDecimal total = request.getTotalAmount();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal hourlyRate = getCourtHourlyRate(court.getId(), request.getBookingDate(), court.getSport());
            total = hourlyRate.multiply(BigDecimal.valueOf(request.getDurationMinutes()))
                    .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
        }

        if (request.getDiscountAmount() != null && request.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(request.getDiscountAmount());
            if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;
        }

        BigDecimal payAmount = total;
        String transType = "Thanh toán";
        if ("DEPOSIT_30".equalsIgnoreCase(request.getDepositChoice())) {
            payAmount = total.multiply(BigDecimal.valueOf(30)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            transType = "Cọc";
        }

        String pMethod = (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod() : "Chuyển khoản VietQR";
        if (pMethod.length() > 30) pMethod = pMethod.substring(0, 30);

        Payment payment = Payment.builder()
                .id(IdGenerator.generateId("GD"))
                .booking(savedBooking)
                .transactionType(transType)
                .amount(payAmount)
                .paymentMethod(pMethod)
                .build();
        paymentRepository.save(payment);

        // 5. Tự động gửi Vé điện tử chính thức qua Email cho khách hàng (chạy bất đồng bộ để phản hồi web tức thì)
        if (emailService != null) {
            String custEmail = (customer != null && customer.getEmail() != null) ? customer.getEmail().trim() : null;
            if (custEmail == null || custEmail.isBlank()) {
                if (customer != null && customer.getAccount() != null && customer.getAccount().getEmail() != null) {
                    custEmail = customer.getAccount().getEmail().trim();
                }
            }
            if (custEmail != null && custEmail.contains("@")) {
                final String targetEmail = custEmail;
                final String custName = (customer != null && customer.getFullName() != null) ? customer.getFullName() : "Quý khách";
                final BigDecimal depPaid = payAmount;
                final BigDecimal remAtCounter = total.subtract(payAmount);
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        emailService.sendBookingTicketEmail(targetEmail, custName, savedBooking, depPaid, remAtCounter);
                    } catch (Exception ex) {
                        System.err.println("[CustomerBookingService] Lỗi gửi email vé điện tử: " + ex.getMessage());
                    }
                });
            }
        }

        // 6. Tự động đăng tin Ghép đối thủ nếu khách hàng có chọn
        if (request.isEnableMatchmaking() && matchmakingService != null) {
            try {
                String sportName = court.getSport() != null ? court.getSport().getName() : "Thể thao";
                String skill = (request.getMatchmakingSkillLevel() != null && !request.getMatchmakingSkillLevel().isBlank())
                        ? request.getMatchmakingSkillLevel() : "Trung bình / Phong trào";
                String tSize = (request.getMatchmakingTeamSize() != null && !request.getMatchmakingTeamSize().isBlank())
                        ? request.getMatchmakingTeamSize() : "Giao lưu";
                String msg = (request.getMatchmakingMessage() != null && !request.getMatchmakingMessage().isBlank())
                        ? request.getMatchmakingMessage() : "Giao lưu vui vẻ, share 50% tiền sân!";

                java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

                vn.edu.ute.sportute.model.MatchmakingPost post = vn.edu.ute.sportute.model.MatchmakingPost.builder()
                        .bookingId(savedBooking.getId())
                        .sportName(sportName)
                        .courtName(court.getName())
                        .courtId(court.getId())
                        .matchDate(request.getBookingDate().format(dateFmt))
                        .startTime(request.getStartTime().format(timeFmt))
                        .endTime(request.getStartTime().plusMinutes(request.getDurationMinutes()).format(timeFmt))
                        .durationMinutes(request.getDurationMinutes())
                        .creatorName(customer != null ? customer.getFullName() : "Khách thể thao")
                        .creatorPhone(customer != null ? customer.getPhone() : "")
                        .creatorEmail(customer != null ? customer.getEmail() : "")
                        .skillLevel(skill)
                        .teamSize(tSize)
                        .message(msg)
                        .status("WAITING")
                        .build();

                matchmakingService.createPost(post);
            } catch (Exception ex) {
                System.err.println("[CustomerBookingService] Lỗi tự động tạo tin ghép đối: " + ex.getMessage());
            }
        }

        return savedBooking;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminBookingDTO> getCustomerBookings(String accountUsernameOrEmail, String status, String keyword) {
        Customer customer = resolveCustomer(accountUsernameOrEmail);
        List<Booking> all = bookingRepository.findAll();

        return all.stream()
                .filter(b -> b.getCustomer() != null && b.getCustomer().getId().equals(customer.getId()))
                .filter(b -> {
                    if (status != null && !status.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(b.getStatus())) return false;
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchId = b.getId() != null && b.getId().toLowerCase().contains(kw);
                        boolean matchCourt = b.getBookingDetails().stream()
                                .anyMatch(d -> d.getCourt() != null && d.getCourt().getName().toLowerCase().contains(kw));
                        return matchId || matchCourt;
                    }
                    return true;
                })
                .sorted((b1, b2) -> b2.getId().compareTo(b1.getId()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBookingDTO getCustomerBookingDetail(String bookingId, String accountUsernameOrEmail) {
        Customer customer = resolveCustomer(accountUsernameOrEmail);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + bookingId));

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập phiếu đặt sân này!");
        }

        return mapToDTO(booking);
    }

    @Override
    public void cancelBookingByCustomer(String bookingId, String accountUsernameOrEmail, String reason) {
        Customer customer = resolveCustomer(accountUsernameOrEmail);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + bookingId));

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền hủy phiếu đặt sân của người khác!");
        }

        if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Phiếu đặt sân này đã bị hủy trước đó!");
        }

        if ("Hoàn tất".equalsIgnoreCase(booking.getStatus()) || "Đang sử dụng".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Không thể hủy phiếu khi đang sử dụng sân hoặc đã hoàn tất!");
        }

        // Tính phí hủy theo chính sách
        BookingPolicy policy = booking.getBookingPolicy();
        BigDecimal cancelRate = BigDecimal.valueOf(10.0);
        if (policy != null && policy.getCancellationFeeRate() != null) {
            cancelRate = policy.getCancellationFeeRate();
        }

        BigDecimal totalPaid = booking.getPayments().stream()
                .filter(p -> !"Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cancellationFee = totalPaid.multiply(cancelRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal refundAmount = totalPaid.subtract(cancellationFee);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) refundAmount = BigDecimal.ZERO;

        booking.setStatus("Đã hủy");
        bookingRepository.save(booking);

        // Giải phóng sân
        if (!booking.getBookingDetails().isEmpty()) {
            Court court = booking.getBookingDetails().get(0).getCourt();
            court.setStatus("Hoạt động");
            courtRepository.save(court);
        }

        // Ghi nhận hoàn tiền
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            Payment refund = Payment.builder()
                    .id(IdGenerator.generateId("GD"))
                    .booking(booking)
                    .transactionType("Hoàn tiền")
                    .amount(refundAmount)
                    .paymentMethod("Chuyển khoản")
                    .build();
            paymentRepository.save(refund);
        }

        // Tự động hủy/gỡ bài ghép đối thủ trên sàn giao lưu (nếu có)
        try {
            matchmakingService.cancelPostByBookingId(bookingId, customer.getFullName());
        } catch (Exception ex) {
            System.err.println("[CustomerBookingService] Lỗi gỡ bài ghép kèo: " + ex.getMessage());
        }
    }

    private Customer resolveCustomer(String accountUsernameOrEmail) {
        if (accountUsernameOrEmail == null || accountUsernameOrEmail.isBlank()) {
            return customerRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Chưa có hồ sơ khách hàng trong hệ thống"));
        }

        Optional<Account> accountOpt = accountRepository.findByUsernameOrEmail(accountUsernameOrEmail, accountUsernameOrEmail);
        if (accountOpt.isPresent()) {
            Optional<Customer> custOpt = customerRepository.findByAccount_Id(accountOpt.get().getId());
            if (custOpt.isPresent()) return custOpt.get();
        }

        Optional<Customer> byEmail = customerRepository.findByEmail(accountUsernameOrEmail);
        if (byEmail.isPresent()) return byEmail.get();

        Optional<Customer> byPhone = customerRepository.findByPhone(accountUsernameOrEmail);
        if (byPhone.isPresent()) return byPhone.get();

        // Tự động tạo hồ sơ khách hàng nếu tài khoản chưa gắn hồ sơ
        Customer newCust = Customer.builder()
                .id(IdGenerator.generateId("KH"))
                .fullName(accountUsernameOrEmail)
                .phone("09" + String.format("%08d", new Random().nextInt(100000000)))
                .email(accountUsernameOrEmail.contains("@") ? accountUsernameOrEmail : accountUsernameOrEmail + "@sporthub.vn")
                .account(accountOpt.orElse(null))
                .status("Hoạt động")
                .build();
        return customerRepository.save(newCust);
    }

    private BigDecimal getCourtHourlyRate(String courtId, LocalDate date, Sport sport) {
        var effectivePrice = priceRepository.findEffectivePrice(courtId, date);
        if (effectivePrice.isPresent() && !effectivePrice.get().getPriceDetails().isEmpty()) {
            return effectivePrice.get().getPriceDetails().get(0).getPriceAmount();
        }

        if (sport != null) {
            String mId = sport.getId();
            if ("M_BD".equalsIgnoreCase(mId)) return BigDecimal.valueOf(350000);
            if ("M_CL".equalsIgnoreCase(mId)) return BigDecimal.valueOf(120000);
            if ("M_BR".equalsIgnoreCase(mId)) return BigDecimal.valueOf(200000);
            if ("M_PB".equalsIgnoreCase(mId)) return BigDecimal.valueOf(120000);
        }
        return BigDecimal.valueOf(150000);
    }

    private AdminBookingDTO mapToDTO(Booking b) {
        BookingDetail detail = (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty()) 
                ? b.getBookingDetails().get(0) : null;

        BigDecimal totalAmount = BigDecimal.ZERO;
        if (b.getPayments() != null && !b.getPayments().isEmpty()) {
            totalAmount = b.getPayments().stream()
                    .filter(p -> !"Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        LocalDate bDate = LocalDate.now();
        LocalTime sTime = LocalTime.of(8, 0);
        LocalTime eTime = LocalTime.of(10, 0);
        String courtId = "";
        String courtName = "N/A";
        String sportName = "N/A";

        if (detail != null) {
            if (detail.getStartTime() != null) {
                bDate = detail.getStartTime().toLocalDate();
                sTime = detail.getStartTime().toLocalTime();
            }
            if (detail.getEndTime() != null) {
                eTime = detail.getEndTime().toLocalTime();
            }
            if (detail.getCourt() != null) {
                courtId = detail.getCourt().getId();
                courtName = detail.getCourt().getName();
                if (detail.getCourt().getSport() != null) {
                    sportName = detail.getCourt().getSport().getName();
                }
            }
        }

        return AdminBookingDTO.builder()
                .id(b.getId())
                .customerId(b.getCustomer() != null ? b.getCustomer().getId() : "")
                .customerName(b.getCustomer() != null ? b.getCustomer().getFullName() : "N/A")
                .customerPhone(b.getCustomer() != null ? b.getCustomer().getPhone() : "")
                .courtId(courtId)
                .courtName(courtName)
                .sportName(sportName)
                .bookingDate(bDate)
                .startTime(sTime)
                .endTime(eTime)
                .totalAmount(totalAmount)
                .status(b.getStatus() != null ? b.getStatus() : "Chờ xác nhận")
                .build();
    }
}