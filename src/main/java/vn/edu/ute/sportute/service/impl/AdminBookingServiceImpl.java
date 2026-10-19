package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.*;
import vn.edu.ute.sportute.dto.response.*;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminBookingServiceImpl implements AdminBookingService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final CustomerRepository customerRepository;
    private final CourtRepository courtRepository;
    private final EmployeeRepository employeeRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PaymentRepository paymentRepository;
    private final PriceRepository priceRepository;
    private final BookingPolicyRepository bookingPolicyRepository;
    private final PromotionRepository promotionRepository;

    public AdminBookingServiceImpl(BookingRepository bookingRepository,
                                  BookingDetailRepository bookingDetailRepository,
                                  CustomerRepository customerRepository,
                                  CourtRepository courtRepository,
                                  EmployeeRepository employeeRepository,
                                  MaintenanceRepository maintenanceRepository,
                                  PaymentRepository paymentRepository,
                                  PriceRepository priceRepository,
                                  BookingPolicyRepository bookingPolicyRepository,
                                  PromotionRepository promotionRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.customerRepository = customerRepository;
        this.courtRepository = courtRepository;
        this.employeeRepository = employeeRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.paymentRepository = paymentRepository;
        this.priceRepository = priceRepository;
        this.bookingPolicyRepository = bookingPolicyRepository;
        this.promotionRepository = promotionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminBookingDTO> getBookings(String keyword, String status) {
        List<Booking> all = bookingRepository.findAll();

        return all.stream()
                .filter(b -> {
                    // Lọc theo trạng thái
                    if (status != null && !status.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(b.getStatus())) return false;
                    }
                    // Lọc theo từ khóa (Mã đặt, Tên khách hàng, Số điện thoại, Tên sân)
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchId = b.getId() != null && b.getId().toLowerCase().contains(kw);
                        boolean matchCust = b.getCustomer() != null && b.getCustomer().getFullName().toLowerCase().contains(kw);
                        boolean matchPhone = b.getCustomer() != null && b.getCustomer().getPhone() != null && b.getCustomer().getPhone().contains(kw);
                        boolean matchCourt = b.getBookingDetails().stream()
                                .anyMatch(d -> d.getCourt() != null && d.getCourt().getName().toLowerCase().contains(kw));
                        return matchId || matchCust || matchPhone || matchCourt;
                    }
                    return true;
                })
                .sorted((b1, b2) -> b2.getId().compareTo(b1.getId()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBookingDTO getBookingById(String id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + id));
        return mapToDTO(b);
    }

    @Override
    public Customer findOrCreateCustomer(String phone, String fullName, String email, String address) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại khách hàng không được để trống");
        }
        String cleanPhone = phone.trim();
        Optional<Customer> existing = customerRepository.findByPhone(cleanPhone);
        if (existing.isPresent()) {
            Customer cust = existing.get();
            if (fullName != null && !fullName.trim().isEmpty() && !fullName.equals(cust.getFullName())) {
                cust.setFullName(fullName.trim());
                customerRepository.save(cust);
            }
            return cust;
        }

        String defaultEmail = (email != null && !email.trim().isEmpty())
                ? email.trim()
                : ("guest." + cleanPhone + "@sporthub.local");

        Customer newCust = Customer.builder()
                .id(IdGenerator.generateId("KH"))
                .fullName((fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : "Khách lẻ " + cleanPhone)
                .phone(cleanPhone)
                .email(defaultEmail)
                .address(address != null && !address.trim().isEmpty() ? address.trim() : "Khách tại quầy")
                .status("Hoạt động")
                .build();
        return customerRepository.save(newCust);
    }

    @Override
    public void saveBooking(AdminBookingDTO dto, String employeeAccountId) {
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc!");
        }

        LocalDateTime startDateTime = LocalDateTime.of(dto.getBookingDate(), dto.getStartTime());
        LocalDateTime endDateTime = LocalDateTime.of(dto.getBookingDate(), dto.getEndTime());

        // 1. Kiểm tra xung đột lịch bảo trì (RB2)
        boolean inMaintenance = maintenanceRepository.existsOverlappingMaintenance(
                dto.getCourtId(), startDateTime, endDateTime
        );
        if (inMaintenance) {
            throw new IllegalArgumentException("Sân đang trong thời gian bảo trì vào khung giờ này!");
        }

        // 2. Kiểm tra trùng lịch: phân biệt rạch ròi giữa Tạo mới và Cập nhật (RB1)
        String currentBookingId = (dto.getId() != null && !dto.getId().trim().isEmpty()) ? dto.getId().trim() : null;
        boolean isOverlapped;
        
        if (currentBookingId != null) {
            isOverlapped = bookingDetailRepository.existsOverlappingBookingExceptSelf(
                    dto.getCourtId(), startDateTime, endDateTime, currentBookingId
            );
        } else {
            isOverlapped = bookingDetailRepository.existsOverlappingBooking(
                    dto.getCourtId(), startDateTime, endDateTime
            );
        }

        if (isOverlapped && !"Đã hủy".equalsIgnoreCase(dto.getStatus())) {
            throw new IllegalArgumentException("Khung giờ này đã có người đặt trước trên sân này!");
        }

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
        Court court = courtRepository.findById(dto.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại"));
        Employee employee = null;
        if (employeeAccountId != null) {
            employee = employeeRepository.findByAccount_Id(employeeAccountId).orElse(null);
        }

        // Tìm chính sách đặt sân
        BookingPolicy policy = null;
        if (dto.getPolicyId() != null && !dto.getPolicyId().trim().isEmpty()) {
            policy = bookingPolicyRepository.findById(dto.getPolicyId()).orElse(null);
        }
        if (policy == null) {
            policy = bookingPolicyRepository.findAll().stream().findFirst().orElse(null);
        }

        // Tìm khuyến mãi nếu có
        Promotion promotion = null;
        if (dto.getPromotionId() != null && !dto.getPromotionId().trim().isEmpty()) {
            promotion = promotionRepository.findById(dto.getPromotionId()).orElse(null);
        }

        // Tự động tính giá nếu chưa nhập
        BigDecimal totalAmount = dto.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            PriceEstimateResponseDTO estimate = calculateEstimate(PriceEstimateRequestDTO.builder()
                    .courtId(dto.getCourtId())
                    .date(dto.getBookingDate())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .policyId(policy != null ? policy.getId() : null)
                    .promotionCode(promotion != null ? promotion.getId() : null)
                    .build());
            totalAmount = estimate.getTotalAmount();
        }

        Booking booking;
        BookingDetail detail;

        if (currentBookingId == null) {
            String initialStatus = (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) 
                    ? dto.getStatus() : "Chờ xác nhận";

            booking = Booking.builder()
                    .id(IdGenerator.generateId("DS"))
                    .customer(customer)
                    .employee(employee)
                    .bookingPolicy(policy)
                    .promotion(promotion)
                    .status(initialStatus)
                    .build();

            detail = BookingDetail.builder()
                    .id(IdGenerator.generateId("CT"))
                    .booking(booking)
                    .court(court)
                    .startTime(startDateTime)
                    .endTime(endDateTime)
                    .build();
            booking.getBookingDetails().add(detail);
        } else {
            booking = bookingRepository.findById(currentBookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt"));
            booking.setCustomer(customer);
            booking.setBookingPolicy(policy);
            booking.setPromotion(promotion);
            booking.setStatus(dto.getStatus());

            if (!booking.getBookingDetails().isEmpty()) {
                detail = booking.getBookingDetails().get(0);
                detail.setCourt(court);
                detail.setStartTime(startDateTime);
                detail.setEndTime(endDateTime);
            } else {
                detail = BookingDetail.builder()
                        .id(IdGenerator.generateId("CT"))
                        .booking(booking)
                        .court(court)
                        .startTime(startDateTime)
                        .endTime(endDateTime)
                        .build();
                booking.getBookingDetails().add(detail);
            }
        }

        Booking savedBooking = bookingRepository.save(booking);
        dto.setId(savedBooking.getId());

        // Lưu / cập nhật giao dịch thanh toán
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            Payment payment;
            if (savedBooking.getPayments().isEmpty()) {
                String txType = "Đã xác nhận".equalsIgnoreCase(savedBooking.getStatus()) ? "Thu" : "Cọc";
                payment = Payment.builder()
                        .id(IdGenerator.generateId("GD"))
                        .booking(savedBooking)
                        .transactionType(txType)
                        .amount(totalAmount)
                        .paymentMethod("Tiền mặt")
                        .build();
            } else {
                payment = savedBooking.getPayments().get(0);
                payment.setAmount(totalAmount);
            }
            paymentRepository.save(payment);
        }
    }

    @Override
    public void confirmBooking(String id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + id));

        if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Không thể xác nhận phiếu đã bị hủy!");
        }

        if (!booking.getBookingDetails().isEmpty()) {
            BookingDetail detail = booking.getBookingDetails().get(0);
            boolean inMaintenance = maintenanceRepository.existsOverlappingMaintenance(
                    detail.getCourt().getId(), detail.getStartTime(), detail.getEndTime()
            );
            if (inMaintenance) {
                throw new IllegalArgumentException("Không thể xác nhận: Sân đã có lịch bảo trì trong khung giờ này!");
            }
        }

        booking.setStatus("Đã xác nhận");
        bookingRepository.save(booking);
    }

    @Override
    public void checkInBooking(String id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + id));

        if (!"Đã xác nhận".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể nhận sân cho các phiếu ở trạng thái 'Đã xác nhận'!");
        }

        booking.setStatus("Đang sử dụng");
        bookingRepository.save(booking);

        // Cập nhật trạng thái sân sang "Đang sử dụng"
        if (!booking.getBookingDetails().isEmpty()) {
            Court court = booking.getBookingDetails().get(0).getCourt();
            court.setStatus("Đang sử dụng");
            courtRepository.save(court);
        }
    }

    @Override
    public void checkOutBooking(String id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + id));

        if (!"Đang sử dụng".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể trả sân cho các phiếu ở trạng thái 'Đang sử dụng'!");
        }

        // Tính tổng tiền cần thanh toán và tiền đã thanh toán
        BigDecimal totalPaid = booking.getPayments().stream()
                .filter(p -> !"Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Chuyển trạng thái sang Hoàn tất (RB6)
        booking.setStatus("Hoàn tất");
        bookingRepository.save(booking);

        // Trả lại trạng thái sân thành "Hoạt động"
        if (!booking.getBookingDetails().isEmpty()) {
            Court court = booking.getBookingDetails().get(0).getCourt();
            court.setStatus("Hoạt động");
            courtRepository.save(court);
        }
    }

    @Override
    public void extendBooking(ExtendBookingDTO dto) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + dto.getBookingId()));

        if (!"Đang sử dụng".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể gia hạn khi khách đang sử dụng sân!");
        }

        if (booking.getBookingDetails().isEmpty()) {
            throw new IllegalArgumentException("Phiếu đặt sân không có chi tiết sân để gia hạn!");
        }

        BookingDetail detail = booking.getBookingDetails().get(0);
        LocalDateTime currentEnd = detail.getEndTime();
        LocalDateTime newEnd = LocalDateTime.of(currentEnd.toLocalDate(), dto.getNewEndTime());

        if (!newEnd.isAfter(currentEnd)) {
            throw new IllegalArgumentException("Giờ kết thúc gia hạn (" + dto.getNewEndTime() + ") phải sau giờ kết thúc hiện tại (" + currentEnd.toLocalTime() + ")!");
        }

        // Kiểm tra lịch bảo trì trong khoảng thời gian gia hạn [currentEnd, newEnd]
        if (maintenanceRepository.existsOverlappingMaintenance(detail.getCourt().getId(), currentEnd, newEnd)) {
            throw new IllegalArgumentException("Không thể gia hạn: Sân vướng lịch bảo trì trong khung giờ gia hạn!");
        }

        // Kiểm tra trùng lịch với phiếu đặt tiếp theo của cùng sân (ngoại trừ chính nó)
        if (bookingDetailRepository.existsOverlappingBookingExceptSelf(detail.getCourt().getId(), currentEnd, newEnd, booking.getId())) {
            throw new IllegalArgumentException("Không thể gia hạn: Khung giờ tiếp theo đã có người đặt trước!");
        }

        // Tính tiền phát sinh
        BigDecimal hourlyRate = getCourtHourlyRate(detail.getCourt().getId(), currentEnd.toLocalDate());
        long extraMinutes = Duration.between(currentEnd, newEnd).toMinutes();
        BigDecimal extraFee = hourlyRate.multiply(BigDecimal.valueOf(extraMinutes))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);

        if (dto.getAdditionalFee() != null && dto.getAdditionalFee().compareTo(BigDecimal.ZERO) > 0) {
            extraFee = dto.getAdditionalFee();
        }

        // Cập nhật giờ kết thúc mới
        detail.setEndTime(newEnd);
        bookingDetailRepository.save(detail);

        // Ghi nhận giao dịch phát sinh
        if (extraFee.compareTo(BigDecimal.ZERO) > 0) {
            Payment extraPayment = Payment.builder()
                    .id(IdGenerator.generateId("GD"))
                    .booking(booking)
                    .transactionType("Thu")
                    .amount(extraFee)
                    .paymentMethod(dto.getPaymentMethod() != null ? dto.getPaymentMethod() : "Tiền mặt")
                    .build();
            paymentRepository.save(extraPayment);
        }
    }

    @Override
    public void changeSchedule(ChangeScheduleDTO dto) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + dto.getBookingId()));

        if (!"Đã xác nhận".equalsIgnoreCase(booking.getStatus()) && !"Chờ xác nhận".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể đổi sân hoặc giờ thuê khi phiếu ở trạng thái 'Chờ xác nhận' hoặc 'Đã xác nhận'!");
        }

        if (!dto.getNewStartTime().isBefore(dto.getNewEndTime())) {
            throw new IllegalArgumentException("Giờ bắt đầu mới phải trước giờ kết thúc mới!");
        }

        LocalDateTime newStart = LocalDateTime.of(dto.getNewBookingDate(), dto.getNewStartTime());
        LocalDateTime newEnd = LocalDateTime.of(dto.getNewBookingDate(), dto.getNewEndTime());

        // Kiểm tra bảo trì trên sân mới
        if (maintenanceRepository.existsOverlappingMaintenance(dto.getNewCourtId(), newStart, newEnd)) {
            throw new IllegalArgumentException("Sân mới được chọn đang có lịch bảo trì vào khung giờ này!");
        }

        // Kiểm tra trùng lịch (loại trừ chính phiếu đang đổi - UC19 / RB1)
        if (bookingDetailRepository.existsOverlappingBookingExceptSelf(dto.getNewCourtId(), newStart, newEnd, booking.getId())) {
            throw new IllegalArgumentException("Khung giờ mới trên sân được chọn đã có người khác đặt!");
        }

        Court newCourt = courtRepository.findById(dto.getNewCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Sân mới không tồn tại"));

        // Tính tiền mới
        PriceEstimateResponseDTO estimate = calculateEstimate(PriceEstimateRequestDTO.builder()
                .courtId(dto.getNewCourtId())
                .date(dto.getNewBookingDate())
                .startTime(dto.getNewStartTime())
                .endTime(dto.getNewEndTime())
                .policyId(booking.getBookingPolicy() != null ? booking.getBookingPolicy().getId() : null)
                .promotionCode(booking.getPromotion() != null ? booking.getPromotion().getId() : null)
                .build());

        // Lấy tổng tiền đã trả cũ
        BigDecimal oldPaid = booking.getPayments().stream()
                .filter(p -> !"Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal priceDiff = estimate.getTotalAmount().subtract(oldPaid);

        // Cập nhật chi tiết đặt sân
        if (!booking.getBookingDetails().isEmpty()) {
            BookingDetail detail = booking.getBookingDetails().get(0);
            detail.setCourt(newCourt);
            detail.setStartTime(newStart);
            detail.setEndTime(newEnd);
            bookingDetailRepository.save(detail);
        }

        // Nếu phát sinh chênh lệch
        if (priceDiff.compareTo(BigDecimal.ZERO) > 0) {
            // Cần thu thêm
            Payment extraPayment = Payment.builder()
                    .id(IdGenerator.generateId("GD"))
                    .booking(booking)
                    .transactionType("Thu")
                    .amount(priceDiff)
                    .paymentMethod("Tiền mặt")
                    .build();
            paymentRepository.save(extraPayment);
        } else if (priceDiff.compareTo(BigDecimal.ZERO) < 0) {
            // Cần hoàn tiền chênh lệch
            Payment refundPayment = Payment.builder()
                    .id(IdGenerator.generateId("GD"))
                    .booking(booking)
                    .transactionType("Hoàn tiền")
                    .amount(priceDiff.abs())
                    .paymentMethod("Chuyển khoản")
                    .build();
            paymentRepository.save(refundPayment);
        }
    }

    @Override
    public void cancelBooking(String id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + id));

        booking.setStatus("Đã hủy");
        bookingRepository.save(booking);

        if (!booking.getBookingDetails().isEmpty()) {
            Court court = booking.getBookingDetails().get(0).getCourt();
            court.setStatus("Hoạt động");
            courtRepository.save(court);
        }
    }

    @Override
    public void cancelBookingWithPolicy(CancelBookingDTO dto) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + dto.getBookingId()));

        if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Phiếu đặt này đã bị hủy trước đó!");
        }
        if ("Hoàn tất".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Không thể hủy phiếu đã hoàn tất!");
        }

        // Lấy chính sách cọc & hủy
        BookingPolicy policy = booking.getBookingPolicy();
        BigDecimal cancelRate = BigDecimal.valueOf(10.0); // Mặc định 10%
        if (policy != null && policy.getCancellationFeeRate() != null) {
            cancelRate = policy.getCancellationFeeRate();
        }

        // Tổng tiền đã thu/cọc
        BigDecimal totalPaid = booking.getPayments().stream()
                .filter(p -> !"Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính phí hủy và tiền hoàn
        BigDecimal cancellationFee = totalPaid.multiply(cancelRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal refundAmount = totalPaid.subtract(cancellationFee);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) refundAmount = BigDecimal.ZERO;

        // Cập nhật trạng thái "Đã hủy" (RB1: không xóa vật lý)
        booking.setStatus("Đã hủy");
        bookingRepository.save(booking);

        // Giải phóng sân
        if (!booking.getBookingDetails().isEmpty()) {
            Court court = booking.getBookingDetails().get(0).getCourt();
            court.setStatus("Hoạt động");
            courtRepository.save(court);
        }

        // Ghi nhận hoàn tiền nếu có
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
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtTimelineDTO> getTimeline(LocalDate date, String sportId) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        List<Court> courts = courtRepository.findAll();
        if (sportId != null && !sportId.trim().isEmpty() && !"ALL".equalsIgnoreCase(sportId)) {
            courts = courts.stream()
                    .filter(c -> c.getSport() != null && c.getSport().getId().equalsIgnoreCase(sportId.trim()))
                    .collect(Collectors.toList());
        }

        List<CourtTimelineDTO> result = new ArrayList<>();

        // Lấy tất cả chi tiết đặt sân trong ngày đó (loại trừ phiếu đã hủy)
        List<BookingDetail> allDetails = bookingDetailRepository.findAll().stream()
                .filter(d -> d.getStartTime() != null && d.getStartTime().toLocalDate().equals(targetDate))
                .filter(d -> d.getBooking() != null && !"Đã hủy".equalsIgnoreCase(d.getBooking().getStatus()))
                .collect(Collectors.toList());

        // Lấy tất cả lịch bảo trì trong ngày đó
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);
        List<Maintenance> allMaintenance = maintenanceRepository.findAll().stream()
                .filter(m -> m.getStartDate().isBefore(endOfDay) && m.getEndDate().isAfter(startOfDay))
                .collect(Collectors.toList());

        // Các mốc giờ từ 05:00 đến 23:00 (18 khung giờ)
        int[] hours = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};

        for (Court court : courts) {
            List<TimelineSlotDTO> slots = new ArrayList<>();

            for (int h : hours) {
                LocalTime slotStart = LocalTime.of(h, 0);
                LocalTime slotEnd = slotStart.plusHours(1);
                LocalDateTime slotStartDateTime = LocalDateTime.of(targetDate, slotStart);
                LocalDateTime slotEndDateTime = LocalDateTime.of(targetDate, slotEnd);
                String timeStr = String.format("%02d:00", h);

                // 1. Kiểm tra bảo trì
                Optional<Maintenance> maint = allMaintenance.stream()
                        .filter(m -> m.getCourt().getId().equals(court.getId()))
                        .filter(m -> m.getStartDate().isBefore(slotEndDateTime) && m.getEndDate().isAfter(slotStartDateTime))
                        .findFirst();

                if (maint.isPresent()) {
                    slots.add(TimelineSlotDTO.builder()
                            .time(timeStr)
                            .status("MAINT")
                            .statusLabel("Bảo trì")
                            .cssClass("slot-maint")
                            .note(maint.get().getReason() != null ? maint.get().getReason() : "Bảo trì sân")
                            .build());
                    continue;
                }

                // 2. Kiểm tra đặt sân
                Optional<BookingDetail> bookingDetail = allDetails.stream()
                        .filter(d -> d.getCourt().getId().equals(court.getId()))
                        .filter(d -> d.getStartTime().isBefore(slotEndDateTime) && d.getEndTime().isAfter(slotStartDateTime))
                        .findFirst();

                if (bookingDetail.isPresent()) {
                    Booking b = bookingDetail.get().getBooking();
                    String bStatus = b.getStatus();
                    String custName = b.getCustomer() != null ? b.getCustomer().getFullName() : "Khách";

                    if ("Đang sử dụng".equalsIgnoreCase(bStatus)) {
                        slots.add(TimelineSlotDTO.builder()
                                .time(timeStr)
                                .status("IN_USE")
                                .statusLabel("Đang dùng")
                                .cssClass("slot-inuse")
                                .bookingId(b.getId())
                                .customerName(custName)
                                .build());
                    } else {
                        slots.add(TimelineSlotDTO.builder()
                                .time(timeStr)
                                .status("BUSY")
                                .statusLabel("Đã đặt")
                                .cssClass("slot-busy")
                                .bookingId(b.getId())
                                .customerName(custName)
                                .build());
                    }
                    continue;
                }

                // 3. Trống
                slots.add(TimelineSlotDTO.builder()
                        .time(timeStr)
                        .status("FREE")
                        .statusLabel("Trống")
                        .cssClass("slot-free")
                        .build());
            }

            result.add(CourtTimelineDTO.builder()
                    .courtId(court.getId())
                    .courtName(court.getName())
                    .sportId(court.getSport() != null ? court.getSport().getId() : "")
                    .sportName(court.getSport() != null ? court.getSport().getName() : "")
                    .courtStatus(court.getStatus() != null ? court.getStatus() : "Hoạt động")
                    .slots(slots)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PriceEstimateResponseDTO calculateEstimate(PriceEstimateRequestDTO req) {
        if (req.getCourtId() == null || req.getDate() == null || req.getStartTime() == null || req.getEndTime() == null) {
            return PriceEstimateResponseDTO.builder()
                    .valid(false)
                    .message("Thiếu thông tin sân, ngày hoặc khung giờ cần tính giá")
                    .build();
        }

        if (!req.getStartTime().isBefore(req.getEndTime())) {
            return PriceEstimateResponseDTO.builder()
                    .valid(false)
                    .message("Giờ bắt đầu phải trước giờ kết thúc")
                    .build();
        }

        long minutes = Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
        BigDecimal hourlyRate = getCourtHourlyRate(req.getCourtId(), req.getDate());
        BigDecimal rawAmount = hourlyRate.multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);

        BigDecimal discount = BigDecimal.ZERO;
        if (req.getPromotionCode() != null && !req.getPromotionCode().trim().isEmpty()) {
            Optional<Promotion> promoOpt = promotionRepository.findById(req.getPromotionCode().trim());
            if (promoOpt.isPresent()) {
                Promotion p = promoOpt.get();
                if (p.getDiscountAmount() != null) {
                    discount = p.getDiscountAmount();
                }
            }
        }

        BigDecimal totalAmount = rawAmount.subtract(discount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        // Chính sách cọc
        BookingPolicy policy = null;
        if (req.getPolicyId() != null && !req.getPolicyId().trim().isEmpty()) {
            policy = bookingPolicyRepository.findById(req.getPolicyId()).orElse(null);
        }
        if (policy == null) {
            policy = bookingPolicyRepository.findAll().stream().findFirst().orElse(null);
        }

        BigDecimal depositRate = BigDecimal.valueOf(30.0);
        String policyName = "Chính sách tiêu chuẩn (Cọc 30%)";
        if (policy != null) {
            if (policy.getDepositRate() != null) depositRate = policy.getDepositRate();
            policyName = policy.getName();
        }

        BigDecimal depositAmount = totalAmount.multiply(depositRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        return PriceEstimateResponseDTO.builder()
                .valid(true)
                .message("Tính giá thành công")
                .pricePerHour(hourlyRate)
                .durationMinutes(minutes)
                .rawAmount(rawAmount)
                .discountAmount(discount)
                .totalAmount(totalAmount)
                .policyId(policy != null ? policy.getId() : "CS01")
                .policyName(policyName)
                .depositRate(depositRate)
                .depositAmount(depositAmount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBookingStatistics() {
        List<Booking> list = bookingRepository.findAll();
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", (long) list.size());
        stats.put("pending", list.stream().filter(b -> "Chờ xác nhận".equalsIgnoreCase(b.getStatus())).count());
        stats.put("confirmed", list.stream().filter(b -> "Đã xác nhận".equalsIgnoreCase(b.getStatus())).count());
        stats.put("inUse", list.stream().filter(b -> "Đang sử dụng".equalsIgnoreCase(b.getStatus())).count());
        stats.put("completed", list.stream().filter(b -> "Hoàn tất".equalsIgnoreCase(b.getStatus())).count());
        stats.put("cancelled", list.stream().filter(b -> "Đã hủy".equalsIgnoreCase(b.getStatus())).count());

        BigDecimal totalRevenue = paymentRepository.findAll().stream()
                .filter(p -> !"Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalRevenue", totalRevenue);
        return stats;
    }

    private BigDecimal getCourtHourlyRate(String courtId, LocalDate date) {
        var effectivePrice = priceRepository.findEffectivePrice(courtId, date);
        if (effectivePrice.isPresent() && !effectivePrice.get().getPriceDetails().isEmpty()) {
            return effectivePrice.get().getPriceDetails().get(0).getPriceAmount();
        }

        Optional<Court> courtOpt = courtRepository.findById(courtId);
        if (courtOpt.isPresent() && courtOpt.get().getSport() != null) {
            String sportId = courtOpt.get().getSport().getId();
            if ("M_BD".equalsIgnoreCase(sportId)) return BigDecimal.valueOf(350000);
            if ("M_CL".equalsIgnoreCase(sportId)) return BigDecimal.valueOf(120000);
            if ("M_BR".equalsIgnoreCase(sportId)) return BigDecimal.valueOf(200000);
            if ("M_PB".equalsIgnoreCase(sportId)) return BigDecimal.valueOf(120000);
        }
        return BigDecimal.valueOf(150000);
    }

    private AdminBookingDTO mapToDTO(Booking b) {
        BookingDetail detail = (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty()) 
                ? b.getBookingDetails().get(0) : null;

        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (b.getPayments() != null && !b.getPayments().isEmpty()) {
            paidAmount = b.getPayments().stream()
                    .filter(p -> !"Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalAmount = paidAmount;
        }

        LocalDate bDate = LocalDate.now();
        LocalTime sTime = LocalTime.of(8, 0);
        LocalTime eTime = LocalTime.of(10, 0);
        String courtId = "";
        String courtName = "N/A";
        String sportId = "";
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
                    sportId = detail.getCourt().getSport().getId();
                    sportName = detail.getCourt().getSport().getName();
                }
            }
        }

        // Chính sách
        String policyId = b.getBookingPolicy() != null ? b.getBookingPolicy().getId() : "CS01";
        String policyName = b.getBookingPolicy() != null ? b.getBookingPolicy().getName() : "Chính sách tiêu chuẩn";
        BigDecimal depRate = b.getBookingPolicy() != null && b.getBookingPolicy().getDepositRate() != null 
                ? b.getBookingPolicy().getDepositRate() : BigDecimal.valueOf(30.0);
        BigDecimal cancelRate = b.getBookingPolicy() != null && b.getBookingPolicy().getCancellationFeeRate() != null 
                ? b.getBookingPolicy().getCancellationFeeRate() : BigDecimal.valueOf(10.0);

        // Dịch vụ F&B và thuê dụng cụ phát sinh
        BigDecimal serviceTotal = BigDecimal.ZERO;
        String serviceSummary = "";
        if (b.getServiceDetails() != null && !b.getServiceDetails().isEmpty()) {
            serviceTotal = b.getServiceDetails().stream()
                    .map(vn.edu.ute.sportute.entity.BookingServiceDetail::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            serviceSummary = b.getServiceDetails().stream()
                    .map(d -> d.getQuantity() + "x " + (d.getServiceItem() != null ? d.getServiceItem().getName() : "Món"))
                    .collect(java.util.stream.Collectors.joining(", "));
        }

        totalAmount = totalAmount.add(serviceTotal);
        BigDecimal depositAmount = totalAmount.multiply(depRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) remainingAmount = BigDecimal.ZERO;

        // Khuyến mãi
        String promoId = b.getPromotion() != null ? b.getPromotion().getId() : "";
        String promoName = b.getPromotion() != null ? b.getPromotion().getName() : "";
        BigDecimal discountAmount = b.getPromotion() != null && b.getPromotion().getDiscountAmount() != null 
                ? b.getPromotion().getDiscountAmount() : BigDecimal.ZERO;

        return AdminBookingDTO.builder()
                .id(b.getId())
                .customerId(b.getCustomer() != null ? b.getCustomer().getId() : "")
                .customerName(b.getCustomer() != null ? b.getCustomer().getFullName() : "N/A")
                .customerPhone(b.getCustomer() != null ? b.getCustomer().getPhone() : "")
                .customerEmail(b.getCustomer() != null ? b.getCustomer().getEmail() : "")
                .courtId(courtId)
                .courtName(courtName)
                .sportId(sportId)
                .sportName(sportName)
                .bookingDate(bDate)
                .startTime(sTime)
                .endTime(eTime)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .depositAmount(depositAmount)
                .remainingAmount(remainingAmount)
                .serviceAmount(serviceTotal)
                .serviceSummary(serviceSummary)
                .policyId(policyId)
                .policyName(policyName)
                .depositRate(depRate)
                .cancellationFeeRate(cancelRate)
                .promotionId(promoId)
                .promotionName(promoName)
                .discountAmount(discountAmount)
                .status(b.getStatus() != null ? b.getStatus() : "Chờ xác nhận")
                .build();
    }
}