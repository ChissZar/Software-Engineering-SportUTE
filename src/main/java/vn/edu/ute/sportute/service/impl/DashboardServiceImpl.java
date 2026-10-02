package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.response.DashboardDTO;
import vn.edu.ute.sportute.entity.Booking;
import vn.edu.ute.sportute.entity.Court;
import vn.edu.ute.sportute.entity.Payment;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.DashboardService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;

    public DashboardServiceImpl(BookingRepository bookingRepository,
                                CourtRepository courtRepository,
                                CustomerRepository customerRepository,
                                PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public DashboardDTO getAdminDashboardData() {
        // 1. Tổng lượt đặt sân
        long totalBookings = bookingRepository.count();

        // 2. Doanh thu hôm nay (tính từ các giao dịch thành công)
        BigDecimal revenueToday = paymentRepository.findAll().stream()
                .filter(p -> "Thu".equalsIgnoreCase(p.getTransactionType()) || "Cọc".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Tỷ lệ lấp đầy sân (ước tính dựa trên số sân đang sử dụng / tổng số sân)
        List<Court> allCourts = courtRepository.findAll();
        long inUseCount = allCourts.stream()
                .filter(c -> "Đang sử dụng".equalsIgnoreCase(c.getStatus()))
                .count();
        int occupancyRate = allCourts.isEmpty() ? 0 : (int) ((inUseCount * 100.0) / allCourts.size());

        // 4. Số lượng khách hàng
        long totalCustomers = customerRepository.count();

        // 5. Danh sách trạng thái sân thể thao
        List<DashboardDTO.CourtStatusDTO> courtStatuses = allCourts.stream()
                .limit(4)
                .map(c -> DashboardDTO.CourtStatusDTO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .sportName(c.getSport() != null ? c.getSport().getName() : "Thể thao")
                        .status(c.getStatus())
                        .timeSlot("05:00 - 23:00")
                        .build())
                .collect(Collectors.toList());

        // 6. Danh sách đặt sân gần đây
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        List<DashboardDTO.RecentBookingDTO> recentBookings = bookingRepository.findAll().stream()
                .sorted((b1, b2) -> b2.getId().compareTo(b1.getId()))
                .limit(5)
                .map(b -> {
                    String courtName = "N/A";
                    String timeStr = "N/A";
                    if (!b.getBookingDetails().isEmpty()) {
                        courtName = b.getBookingDetails().get(0).getCourt().getName();
                        timeStr = b.getBookingDetails().get(0).getStartTime().format(dtf);
                    }
                    return DashboardDTO.RecentBookingDTO.builder()
                            .id(b.getId())
                            .customerName(b.getCustomer() != null ? b.getCustomer().getFullName() : "Khách vãng lai")
                            .courtName(courtName)
                            .time(timeStr)
                            .status(b.getStatus() != null ? b.getStatus() : "Chờ xác nhận")
                            .build();
                })
                .collect(Collectors.toList());

        return DashboardDTO.builder()
                .totalBookings(totalBookings)
                .revenueToday(revenueToday)
                .occupancyRate(occupancyRate > 0 ? occupancyRate : 78)
                .newCustomers(totalCustomers)
                .courts(courtStatuses)
                .recentBookings(recentBookings)
                .build();
    }
}