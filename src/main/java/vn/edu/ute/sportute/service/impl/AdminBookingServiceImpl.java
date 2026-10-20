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
    @org.springframework.beans.factory.annotation.Autowired
    private IdGenerator idGenerator;

    @org.springframework.beans.factory.annotation.Autowired
    private vn.edu.ute.sportute.service.BookingPricingService pricing;
    @org.springframework.beans.factory.annotation.Autowired
    private vn.edu.ute.sportute.service.BookingAmounts amounts;


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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.sportute.service.MatchmakingService matchmakingService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.sportute.service.ServiceItemService serviceItemService;

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
                .sorted(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Booking::getId))
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
                .id(idGenerator.generateId("KH"))
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

        courtRepository.findForUpdate(dto.getCourtId()).orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại"));
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
            employee = employeeRepository.findByAccount_Username(employeeAccountId)
                    .or(() -> employeeRepository.findByAccount_Id(employeeAccountId)).orElse(null);
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

        var quote = pricing.quote(dto.getCourtId(), dto.getBookingDate(), dto.getStartTime(), dto.getEndTime(), dto.getPromotionId());
        BigDecimal totalAmount = quote.total();

        Booking booking;
        BookingDetail detail;

        if (currentBookingId == null) {
            String initialStatus = (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) 
                    ? dto.getStatus() : "Chờ xác nhận";

            booking = Booking.builder()
                    .id(idGenerator.generateId("DS"))
                    .customer(customer)
                    .employee(employee)
                    .bookingPolicy(policy)
                    .promotion(promotion)
                    .status(initialStatus)
                    .build();

            detail = BookingDetail.builder()
                    .id(idGenerator.generateId("CT"))
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
                        .id(idGenerator.generateId("CT"))
                        .booking(booking)
                        .court(court)
                        .startTime(startDateTime)
                        .endTime(endDateTime)
                        .build();
                booking.getBookingDetails().add(detail);
            }
        }

        booking.setQuotedAmount(totalAmount);
        booking.setDiscountApplied(quote.discount());
        Booking savedBooking = bookingRepository.save(booking);
        dto.setId(savedBooking.getId());

        // Khoản thu được ghi nhận riêng tại Thanh toán & thu cọc; sửa phiếu không sửa lịch sử giao dịch.
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
        checkOutBooking(id, "Tiền mặt tại quầy", null);
    }

    @Override
    public void checkOutBooking(String id, String paymentMethod, String note) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + id));

        if (!"Đang sử dụng".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể trả sân cho các phiếu ở trạng thái 'Đang sử dụng'!");
        }

        // Quyết toán tiền còn lại nếu có
        if (booking.getPayments().stream().anyMatch(p -> "PENDING".equals(p.getPaymentStatus())))
            throw new IllegalArgumentException("Vui lòng đối soát các khoản chờ xác nhận trước khi trả sân");
        AdminBookingDTO dto = mapToDTO(booking);
        BigDecimal remaining = dto.getRemainingAmount();
        if (remaining != null && remaining.compareTo(BigDecimal.ZERO) > 0) {
            String pMethod = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod : "Tiền mặt tại quầy";
            if (pMethod.length() > 30) pMethod = pMethod.substring(0, 30);
            Payment settlePayment = Payment.builder()
                    .id(idGenerator.generateId("GD"))
                    .booking(booking)
                    .transactionType("Thanh toán")
                    .amount(remaining)
                    .paymentMethod(pMethod)
                    .build();
            paymentRepository.save(settlePayment);
            booking.getPayments().add(settlePayment);
        }

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

        BigDecimal oldCourtTotal = amounts.total(booking).subtract(booking.getServiceDetails().stream().map(BookingServiceDetail::getSubtotal).reduce(BigDecimal.ZERO,BigDecimal::add));
        booking.setQuotedAmount(oldCourtTotal.add(extraFee));
        bookingRepository.save(booking);
        // Cập nhật giờ kết thúc mới
        detail.setEndTime(newEnd);
        bookingDetailRepository.save(detail);

        // Ghi nhận giao dịch phát sinh
        if (extraFee.compareTo(BigDecimal.ZERO) > 0) {
            Payment extraPayment = Payment.builder()
                    .id(idGenerator.generateId("GD"))
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
                .filter(vn.edu.ute.sportute.service.BookingAmounts::counted)
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

        booking.setQuotedAmount(estimate.getTotalAmount());
        booking.setDiscountApplied(estimate.getDiscountAmount());
        bookingRepository.save(booking);
        // Nếu phát sinh chênh lệch
        if (priceDiff.compareTo(BigDecimal.ZERO) > 0) {
            // Cần thu thêm
            Payment extraPayment = Payment.builder()
                    .id(idGenerator.generateId("GD"))
                    .booking(booking)
                    .transactionType("Thu")
                    .amount(priceDiff)
                    .paymentMethod("Tiền mặt")
                    .build();
            paymentRepository.save(extraPayment);
        } else if (priceDiff.compareTo(BigDecimal.ZERO) < 0) {
            // Cần hoàn tiền chênh lệch
            Payment refundPayment = Payment.builder()
                    .id(idGenerator.generateId("GD"))
                    .booking(booking)
                    .transactionType("Hoàn tiền")
                    .paymentStatus("PENDING")
                    .amount(priceDiff.abs())
                    .paymentMethod("Chuyển khoản")
                    .build();
            paymentRepository.save(refundPayment);
        }
    }

    @Override
    public void cancelBooking(String id) {
        cancelBookingWithPolicy(CancelBookingDTO.builder().bookingId(id).build());
    }

    @Override
    public void cancelBookingWithPolicy(CancelBookingDTO dto) {
        Booking booking = bookingRepository.findForUpdate(dto.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + dto.getBookingId()));

        if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Phiếu đặt này đã bị hủy trước đó!");
        }
        if ("Hoàn tất".equalsIgnoreCase(booking.getStatus()) || "Đang sử dụng".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Không thể hủy phiếu đang sử dụng hoặc đã hoàn tất!");
        }

        BigDecimal refundAmount = vn.edu.ute.sportute.service.BookingAmounts.cancellationRefund(booking);

        // Cập nhật trạng thái "Đã hủy" (RB1: không xóa vật lý)
        for (Payment pending : booking.getPayments()) if ("PENDING".equals(pending.getPaymentStatus()) && !"Hoàn tiền".equals(pending.getTransactionType())) pending.setPaymentStatus("VOID");
        booking.setStatus("Đã hủy");
        bookingRepository.save(booking);

        // Ghi nhận hoàn tiền nếu có
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            Payment refund = Payment.builder()
                    .id(idGenerator.generateId("GD"))
                    .booking(booking)
                    .transactionType("Hoàn tiền")
                    .paymentStatus("PENDING")
                    .amount(refundAmount)
                    .paymentMethod("Chuyển khoản")
                    .build();
            booking.getPayments().add(paymentRepository.save(refund));
        }

        if (matchmakingService != null) {
            try {
                matchmakingService.cancelPostByBookingId(dto.getBookingId(), "Quản trị viên");
            } catch (Exception ignored) {}
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
                .filter(m -> !"CANCELLED".equals(m.getLifecycleStatus()) && !"COMPLETED".equals(m.getLifecycleStatus()))
                .filter(m -> m.getStartDate().isBefore(endOfDay) && m.getEndDate().isAfter(startOfDay))
                .collect(Collectors.toList());

        // Các mốc giờ từ 05:00 đến 23:00 mỗi 30 phút (36 khung giờ: 05:00, 05:30, ..., 22:30)
        List<LocalTime> timePoints = new ArrayList<>();
        LocalTime tp = LocalTime.of(5, 0);
        while (tp.isBefore(LocalTime.of(23, 0))) {
            timePoints.add(tp);
            tp = tp.plusMinutes(30);
        }
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (Court court : courts) {
            List<TimelineSlotDTO> slots = new ArrayList<>();
            int i = 0;
            while (i < timePoints.size()) {
                LocalTime slotStart = timePoints.get(i);
                LocalTime slotEnd = slotStart.plusMinutes(30);
                LocalDateTime slotStartDateTime = LocalDateTime.of(targetDate, slotStart);
                LocalDateTime slotEndDateTime = LocalDateTime.of(targetDate, slotEnd);
                String timeStr = slotStart.format(timeFmt);

                // 1. Kiểm tra bảo trì
                Optional<Maintenance> maint = allMaintenance.stream()
                        .filter(m -> m.getCourt().getId().equals(court.getId()))
                        .filter(m -> m.getStartDate().isBefore(slotEndDateTime) && m.getEndDate().isAfter(slotStartDateTime))
                        .findFirst();

                if (maint.isPresent()) {
                    Maintenance m = maint.get();
                    int span = 0;
                    while (i + span < timePoints.size()) {
                        LocalTime checkStart = timePoints.get(i + span);
                        LocalTime checkEnd = checkStart.plusMinutes(30);
                        LocalDateTime checkStartDT = LocalDateTime.of(targetDate, checkStart);
                        LocalDateTime checkEndDT = LocalDateTime.of(targetDate, checkEnd);
                        if (m.getStartDate().isBefore(checkEndDT) && m.getEndDate().isAfter(checkStartDT)) {
                            span++;
                        } else {
                            break;
                        }
                    }
                    if (span < 1) span = 1;

                    slots.add(TimelineSlotDTO.builder()
                            .time(timeStr)
                            .status("MAINT")
                            .statusLabel("Bảo trì")
                            .cssClass("slot-maint")
                            .note(m.getReason() != null ? m.getReason() : "Bảo trì sân")
                            .span(span)
                            .isSpanContinuation(false)
                            .build());

                    for (int k = 1; k < span; k++) {
                        slots.add(TimelineSlotDTO.builder()
                                .time(timePoints.get(i + k).format(timeFmt))
                                .span(1)
                                .isSpanContinuation(true)
                                .build());
                    }
                    i += span;
                    continue;
                }

                // 2. Kiểm tra đặt sân
                Optional<BookingDetail> bookingDetail = allDetails.stream()
                        .filter(d -> d.getCourt().getId().equals(court.getId()))
                        .filter(d -> d.getStartTime().isBefore(slotEndDateTime) && d.getEndTime().isAfter(slotStartDateTime))
                        .findFirst();

                if (bookingDetail.isPresent()) {
                    BookingDetail bd = bookingDetail.get();
                    Booking b = bd.getBooking();
                    String bStatus = b.getStatus();

                    // Thông tin khách hàng
                    String customerId = (b.getCustomer() != null && b.getCustomer().getId() != null) ? b.getCustomer().getId() : "KH";
                    String fullName = (b.getCustomer() != null && b.getCustomer().getFullName() != null) ? b.getCustomer().getFullName().trim() : "Khách";
                    String customerPhone = (b.getCustomer() != null && b.getCustomer().getPhone() != null) ? b.getCustomer().getPhone() : "";

                    // Tách chỉ lấy tên (không đệm và họ)
                    String shortName = fullName;
                    if (fullName.contains(" ")) {
                        String[] parts = fullName.split("\\s+");
                        shortName = parts[parts.length - 1];
                    }
                    String displayName = customerId + " - " + shortName;

                    // Khung giờ đầy đủ
                    String timeRange = bd.getStartTime().format(timeFmt) + " - " + bd.getEndTime().format(timeFmt);

                    // Tính span số ô 30 phút mà đơn đặt sân này bao phủ
                    int span = 0;
                    while (i + span < timePoints.size()) {
                        LocalTime checkStart = timePoints.get(i + span);
                        LocalTime checkEnd = checkStart.plusMinutes(30);
                        LocalDateTime checkStartDT = LocalDateTime.of(targetDate, checkStart);
                        LocalDateTime checkEndDT = LocalDateTime.of(targetDate, checkEnd);
                        if (bd.getStartTime().isBefore(checkEndDT) && bd.getEndTime().isAfter(checkStartDT)) {
                            span++;
                        } else {
                            break;
                        }
                    }
                    if (span < 1) span = 1;

                    boolean isInUse = "Đang sử dụng".equalsIgnoreCase(bStatus);
                    slots.add(TimelineSlotDTO.builder()
                            .time(timeStr)
                            .status(isInUse ? "IN_USE" : "BUSY")
                            .statusLabel(isInUse ? "Đang dùng" : "Đã đặt")
                            .cssClass(isInUse ? "slot-inuse" : "slot-busy")
                            .bookingId(b.getId())
                            .customerId(customerId)
                            .customerName(fullName)
                            .customerShortName(shortName)
                            .customerDisplayName(displayName)
                            .customerPhone(customerPhone)
                            .timeRange(timeRange)
                            .span(span)
                            .isSpanContinuation(false)
                            .build());

                    for (int k = 1; k < span; k++) {
                        slots.add(TimelineSlotDTO.builder()
                                .time(timePoints.get(i + k).format(timeFmt))
                                .span(1)
                                .isSpanContinuation(true)
                                .build());
                    }
                    i += span;
                    continue;
                }

                // 3. Trống (Free 30m slot)
                slots.add(TimelineSlotDTO.builder()
                        .time(timeStr)
                        .status("FREE")
                        .statusLabel("Trống")
                        .cssClass("slot-free")
                        .span(1)
                        .isSpanContinuation(false)
                        .build());
                i++;
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

        var quote = pricing.quote(req.getCourtId(), req.getDate(), req.getStartTime(), req.getEndTime(), req.getPromotionCode());
        hourlyRate = quote.rate();
        rawAmount = quote.raw();
        BigDecimal discount = quote.discount();
        BigDecimal totalAmount = quote.total();

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
                .filter(vn.edu.ute.sportute.service.BookingAmounts::counted)
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
                    .filter(vn.edu.ute.sportute.service.BookingAmounts::counted)
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
        } else if (b.getServiceDetails() != null && !b.getServiceDetails().isEmpty()) {
            bDate = b.getServiceDetails().get(0).getOrderTime().toLocalDate();
            sTime = b.getServiceDetails().get(0).getOrderTime().toLocalTime();
            eTime = sTime.plusMinutes(15);
            courtName = "Bán lẻ F&B tại quầy";
            sportName = "F&B Dịch vụ";
        }

        if (b.getQuotedAmount() != null) totalAmount = b.getQuotedAmount();

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

        totalAmount = amounts.total(b);
        paidAmount = vn.edu.ute.sportute.service.BookingAmounts.paid(b);

        BigDecimal depositAmount = totalAmount.multiply(depRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) remainingAmount = BigDecimal.ZERO;

        // Khuyến mãi
        String promoId = b.getPromotion() != null ? b.getPromotion().getId() : "";
        String promoName = b.getPromotion() != null ? b.getPromotion().getName() : "";
        BigDecimal discountAmount = b.getDiscountApplied() != null ? b.getDiscountApplied() : BigDecimal.ZERO;

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

    @Override
    public AdminBookingDTO createRetailOrder(RetailOrderRequestDTO req, String employeeAccountId) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng bán lẻ chưa có món nào!");
        }

        // 1. Nhận diện hoặc tạo khách hàng (Mặc định: Khách vãng lai tại quầy)
        String phone = (req.getCustomerPhone() != null && !req.getCustomerPhone().trim().isEmpty())
                ? req.getCustomerPhone().trim()
                : "0900000000";
        String name = (req.getCustomerName() != null && !req.getCustomerName().trim().isEmpty())
                ? req.getCustomerName().trim()
                : "Khách vãng lai tại quầy";

        Customer customer = findOrCreateCustomer(phone, name, null, "Khách mua lẻ F&B tại quầy");

        // 2. Nhân viên lập đơn
        Employee employee = null;
        if (employeeAccountId != null) {
            employee = employeeRepository.findByAccount_Id(employeeAccountId).orElse(null);
        }

        // 3. Chính sách tiêu chuẩn
        BookingPolicy policy = bookingPolicyRepository.findAll().stream().findFirst().orElse(null);

        // 4. Tạo phiếu đặt kiểu Bán Lẻ (không gắn sân)
        String bookingId = idGenerator.generateId("BL");
        Booking booking = Booking.builder()
                .id(bookingId)
                .customer(customer)
                .employee(employee)
                .bookingPolicy(policy)
                .status("Hoàn tất")
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // 5. Thêm các món vào chi tiết dịch vụ & trừ kho
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (serviceItemService != null) {
            for (var itemReq : req.getItems()) {
                if (itemReq.getQuantity() != null && itemReq.getQuantity() > 0) {
                    itemReq.setBookingId(bookingId);
                    BookingServiceDetail detail = serviceItemService.addServiceToBooking(itemReq);
                    totalAmount = totalAmount.add(detail.getSubtotal());
                }
            }
        }

        // 6. Ghi nhận giao dịch thanh toán ngay tại quầy
        String paymentMethod = (req.getPaymentMethod() != null && !req.getPaymentMethod().trim().isEmpty())
                ? req.getPaymentMethod().trim()
                : "Tiền mặt";

        Payment payment = Payment.builder()
                .id(idGenerator.generateId("GD"))
                .booking(savedBooking)
                .transactionType("Thu")
                .amount(totalAmount)
                .paymentMethod(paymentMethod)
                .build();
        paymentRepository.save(payment);

        AdminBookingDTO dto = mapToDTO(savedBooking);
        dto.setCourtName("Bán lẻ F&B tại quầy");
        dto.setSportName("F&B Dịch vụ");
        dto.setTotalAmount(totalAmount);
        dto.setPaidAmount(totalAmount);
        dto.setRemainingAmount(BigDecimal.ZERO);
        dto.setServiceAmount(totalAmount);
        return dto;
    }
}
