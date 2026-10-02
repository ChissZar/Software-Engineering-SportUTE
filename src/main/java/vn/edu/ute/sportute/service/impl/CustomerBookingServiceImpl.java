package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.CustomerBookingRequestDTO;
import vn.edu.ute.sportute.dto.request.CustomerCourtFilterDTO;
import vn.edu.ute.sportute.dto.response.CustomerCourtCardDTO;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.CustomerBookingService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class CustomerBookingServiceImpl implements CustomerBookingService {

    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PriceRepository priceRepository;
    private final BookingPolicyRepository bookingPolicyRepository;
    private final PaymentRepository paymentRepository;

    public CustomerBookingServiceImpl(CourtRepository courtRepository,
                                     BookingRepository bookingRepository,
                                     BookingDetailRepository bookingDetailRepository,
                                     MaintenanceRepository maintenanceRepository,
                                     AccountRepository accountRepository,
                                     CustomerRepository customerRepository,
                                     PriceRepository priceRepository,
                                     BookingPolicyRepository bookingPolicyRepository,
                                     PaymentRepository paymentRepository) {
        this.courtRepository = courtRepository;
        this.bookingRepository = bookingRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.priceRepository = priceRepository;
        this.bookingPolicyRepository = bookingPolicyRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerCourtCardDTO> searchAvailableCourts(CustomerCourtFilterDTO filter) {
        List<Court> allCourts = courtRepository.findAll();
        List<CustomerCourtCardDTO> result = new ArrayList<>();

        LocalDateTime startDateTime = LocalDateTime.of(filter.getBookingDate(), filter.getStartTime());
        LocalDateTime endDateTime = startDateTime.plusMinutes(filter.getDurationMinutes());

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

            // 6. Xác định mức giá thuê VND/giờ
            BigDecimal pricePerHour = BigDecimal.valueOf(150000); // Mặc định
            var effectivePrice = priceRepository.findEffectivePrice(court.getId(), filter.getBookingDate());
            if (effectivePrice.isPresent() && !effectivePrice.get().getPriceDetails().isEmpty()) {
                pricePerHour = effectivePrice.get().getPriceDetails().get(0).getPriceAmount();
            } else if (court.getSport() != null) {
                // Giá tham chiếu theo môn nếu chưa cấu hình bảng giá riêng
                if ("M_BD".equalsIgnoreCase(court.getSport().getId())) pricePerHour = BigDecimal.valueOf(350000);
                else if ("M_CL".equalsIgnoreCase(court.getSport().getId())) pricePerHour = BigDecimal.valueOf(120000);
                else if ("M_BR".equalsIgnoreCase(court.getSport().getId())) pricePerHour = BigDecimal.valueOf(200000);
                else if ("M_PB".equalsIgnoreCase(court.getSport().getId())) pricePerHour = BigDecimal.valueOf(120000);
            }

            // 7. Gán thông tin tiện ích và theme màu sắc theo từng môn thể thao[cite: 3]
            List<String> amenities = new ArrayList<>();
            String headerColor = "bg-success text-white";
            if (court.getSport() != null) {
                String mId = court.getSport().getId();
                if ("M_BD".equalsIgnoreCase(mId)) {
                    amenities = Arrays.asList("Cỏ nhân tạo FIFA", "Đèn chiếu LED 400W", "Có mái che");
                    headerColor = "bg-success text-white";
                } else if ("M_CL".equalsIgnoreCase(mId)) {
                    amenities = Arrays.asList("Thảm PVC tiêu chuẩn BWF", "Trong nhà máy lạnh", "Hệ thống chống lóa");
                    headerColor = "bg-primary text-white";
                } else if ("M_BR".equalsIgnoreCase(mId)) {
                    amenities = Arrays.asList("Sàn gỗ giảm chấn", "Bảng rổ chuẩn FIBA", "Khu khán đài");
                    headerColor = "bg-warning text-dark";
                } else if ("M_PB".equalsIgnoreCase(mId)) {
                    amenities = Arrays.asList("Sơn phủ cao cấp", "Lưới đạt chuẩn", "Khu vực nghỉ ngơi");
                    headerColor = "bg-info text-white";
                }
            }

            List<String> slots = Arrays.asList("16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00");

            result.add(CustomerCourtCardDTO.builder()
                    .id(court.getId())
                    .name(court.getName())
                    .sportId(court.getSport() != null ? court.getSport().getId() : "")
                    .sportName(court.getSport() != null ? court.getSport().getName() : "Thể thao")
                    .location(court.getLocation() != null ? court.getLocation() : "Tiêu chuẩn")
                    .status("Sẵn sàng")
                    .pricePerHour(pricePerHour)
                    .amenities(amenities)
                    .timeSlots(slots)
                    .colorHeaderClass(headerColor)
                    .build());
        }

        return result;
    }

    @Override
    public void bookCourtOnline(CustomerBookingRequestDTO request, String accountUsernameOrEmail) {
        LocalDateTime startDateTime = LocalDateTime.of(request.getBookingDate(), request.getStartTime());
        LocalDateTime endDateTime = startDateTime.plusMinutes(request.getDurationMinutes());

        // 1. Kiểm tra lại lịch trống và bảo trì nguyên tử (UC41)[cite: 3]
        if (maintenanceRepository.existsOverlappingMaintenance(request.getCourtId(), startDateTime, endDateTime)) {
            throw new IllegalArgumentException("Rất tiếc! Sân đã được xếp lịch bảo trì trong khung giờ này.");
        }
        if (bookingDetailRepository.existsOverlappingBooking(request.getCourtId(), startDateTime, endDateTime)) {
            throw new IllegalArgumentException("Khung giờ bạn chọn vừa có người khác đặt trước. Vui lòng chọn giờ hoặc sân khác!");
        }

        // 2. Tìm thông tin khách hàng từ tài khoản đang đăng nhập[cite: 3]
        Account account = accountRepository.findByUsernameOrEmail(accountUsernameOrEmail, accountUsernameOrEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không xác định được tài khoản người dùng"));

        Customer customer = customerRepository.findByAccount_Id(account.getId())
                .orElseGet(() -> {
                    // Tạo nhanh hồ sơ khách hàng nếu tài khoản chưa có hồ sơ liên kết
                    Customer newCust = Customer.builder()
                            .id(IdGenerator.generateId("KH"))
                            .fullName(account.getUsername())
                            .phone("0900000000")
                            .email(account.getEmail())
                            .account(account)
                            .status("Hoạt động")
                            .build();
                    return customerRepository.save(newCust);
                });

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại"));

        // Lấy chính sách cọc mặc định[cite: 2, 3]
        BookingPolicy defaultPolicy = bookingPolicyRepository.findAll().stream().findFirst().orElse(null);

        // 3. Tạo phiếu đặt sân trực tuyến[cite: 2, 3]
        Booking booking = Booking.builder()
                .id(IdGenerator.generateId("DS"))
                .customer(customer)
                .employee(null) // Đặt online không qua nhân viên lễ tân lập[cite: 3]
                .bookingPolicy(defaultPolicy)
                .status("Chờ xác nhận") // Lưu ở trạng thái Chờ xác nhận theo quy định cọc[cite: 3]
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

        // 4. Lưu nghĩa vụ thanh toán tạm tính[cite: 2, 3]
        if (request.getTotalAmount() != null && request.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            Payment payment = Payment.builder()
                    .id(IdGenerator.generateId("GD"))
                    .booking(savedBooking)
                    .transactionType("Cọc")
                    .amount(request.getTotalAmount())
                    .paymentMethod("Chuyển khoản")
                    .build();
            paymentRepository.save(payment);
        }
    }
}