package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public AdminBookingServiceImpl(BookingRepository bookingRepository,
                                  BookingDetailRepository bookingDetailRepository,
                                  CustomerRepository customerRepository,
                                  CourtRepository courtRepository,
                                  EmployeeRepository employeeRepository,
                                  MaintenanceRepository maintenanceRepository,
                                  PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.customerRepository = customerRepository;
        this.courtRepository = courtRepository;
        this.employeeRepository = employeeRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminBookingDTO> getBookings(String keyword, String status) {
        List<Booking> all = bookingRepository.findAll();

        return all.stream()
                .filter(b -> {
                    // Lọc theo trạng thái[cite: 3]
                    if (status != null && !status.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(b.getStatus())) return false;
                    }
                    // Lọc theo từ khóa (Mã đặt, Tên khách hàng, Tên sân) (Số 2 Hình 5-8)[cite: 3]
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchId = b.getId() != null && b.getId().toLowerCase().contains(kw);
                        boolean matchCust = b.getCustomer() != null && b.getCustomer().getFullName().toLowerCase().contains(kw);
                        boolean matchCourt = b.getBookingDetails().stream()
                                .anyMatch(d -> d.getCourt().getName().toLowerCase().contains(kw));
                        return matchId || matchCust || matchCourt;
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
    public void cancelBooking(String id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân"));
        // Quy tắc bảo toàn: Không xóa vật lý, chuyển sang trạng thái "Đã hủy"[cite: 3]
        booking.setStatus("Đã hủy");
        bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBookingStatistics() {
        List<Booking> list = bookingRepository.findAll();
        Map<String, Object> stats = new HashMap<>();

        // Thẻ thống kê (Số 4 Hình 5-8): Tổng lượt, Chờ xác nhận, Đã xác nhận, Tạm tính[cite: 3]
        stats.put("total", (long) list.size());
        stats.put("pending", list.stream().filter(b -> "Chờ xác nhận".equalsIgnoreCase(b.getStatus())).count());
        stats.put("confirmed", list.stream().filter(b -> "Đã xác nhận".equalsIgnoreCase(b.getStatus())).count());

        BigDecimal totalAmount = paymentRepository.findAll().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalAmount);

        return stats;
    }
    
    @Override
    public void confirmBooking(String id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + id));

        if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Không thể xác nhận phiếu đã bị hủy!");
        }

        // Kiểm tra xem trong lúc chờ xác nhận có bị vướng bảo trì không[cite: 3]
        if (!booking.getBookingDetails().isEmpty()) {
            BookingDetail detail = booking.getBookingDetails().get(0);
            boolean inMaintenance = maintenanceRepository.existsOverlappingMaintenance(
                    detail.getCourt().getId(), detail.getStartTime(), detail.getEndTime()
            );
            if (inMaintenance) {
                throw new IllegalArgumentException("Không thể xác nhận: Sân đã có lịch bảo trì trong khung giờ này!");
            }
        }

        // Chuyển trạng thái sang "Đã xác nhận"[cite: 3]
        booking.setStatus("Đã xác nhận");
        bookingRepository.save(booking);
    }

    @Override
    public void saveBooking(AdminBookingDTO dto, String employeeAccountId) {
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc!");
        }

        LocalDateTime startDateTime = LocalDateTime.of(dto.getBookingDate(), dto.getStartTime());
        LocalDateTime endDateTime = LocalDateTime.of(dto.getBookingDate(), dto.getEndTime());

        // 1. Kiểm tra xung đột lịch bảo trì[cite: 3]
        boolean inMaintenance = maintenanceRepository.existsOverlappingMaintenance(
                dto.getCourtId(), startDateTime, endDateTime
        );
        if (inMaintenance) {
            throw new IllegalArgumentException("Sân đang trong thời gian bảo trì vào khung giờ này!");
        }

        // 2. Kiểm tra trùng lịch: phân biệt rạch ròi giữa Tạo mới và Cập nhật[cite: 3]
        String currentBookingId = (dto.getId() != null && !dto.getId().trim().isEmpty()) ? dto.getId().trim() : null;
        boolean isOverlapped;
        
        if (currentBookingId != null) {
            // Đang sửa phiếu cũ: bỏ qua chính nó khi xét trùng lịch[cite: 3]
            isOverlapped = bookingDetailRepository.existsOverlappingBookingExceptSelf(
                    dto.getCourtId(), startDateTime, endDateTime, currentBookingId
            );
        } else {
            // Đang tạo mới: kiểm tra toàn bộ[cite: 3]
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

        Booking booking;
        BookingDetail detail;

        if (currentBookingId == null) {
            booking = Booking.builder()
                    .id(IdGenerator.generateId("DS"))
                    .customer(customer)
                    .employee(employee)
                    .status(dto.getStatus() != null ? dto.getStatus() : "Chờ xác nhận")
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

        if (dto.getTotalAmount() != null && dto.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            Payment payment;
            if (savedBooking.getPayments().isEmpty()) {
                payment = Payment.builder()
                        .id(IdGenerator.generateId("GD"))
                        .booking(savedBooking)
                        .transactionType("Thu")
                        .amount(dto.getTotalAmount())
                        .paymentMethod("Tiền mặt")
                        .build();
            } else {
                payment = savedBooking.getPayments().get(0);
                payment.setAmount(dto.getTotalAmount());
            }
            paymentRepository.save(payment);
        }
    }

    private AdminBookingDTO mapToDTO(Booking b) {
        BookingDetail detail = b.getBookingDetails().isEmpty() ? null : b.getBookingDetails().get(0);
        BigDecimal amount = b.getPayments().isEmpty() ? BigDecimal.ZERO : b.getPayments().get(0).getAmount();

        return AdminBookingDTO.builder()
                .id(b.getId())
                .customerId(b.getCustomer() != null ? b.getCustomer().getId() : "")
                .customerName(b.getCustomer() != null ? b.getCustomer().getFullName() : "N/A")
                .customerPhone(b.getCustomer() != null ? b.getCustomer().getPhone() : "")
                .courtId(detail != null ? detail.getCourt().getId() : "")
                .courtName(detail != null ? detail.getCourt().getName() : "N/A")
                .bookingDate(detail != null ? detail.getStartTime().toLocalDate() : LocalDate.now())
                .startTime(detail != null ? detail.getStartTime().toLocalTime() : LocalTime.of(8, 0))
                .endTime(detail != null ? detail.getEndTime().toLocalTime() : LocalTime.of(10, 0))
                .totalAmount(amount)
                .status(b.getStatus() != null ? b.getStatus() : "Chờ xác nhận")
                .build();
    }
}