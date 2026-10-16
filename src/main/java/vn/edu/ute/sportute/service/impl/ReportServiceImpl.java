package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.response.ReportDataDTO;
import vn.edu.ute.sportute.dto.response.ReportDataDTO.*;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.ReportService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final SportRepository sportRepository;
    private final PaymentRepository paymentRepository;

    public ReportServiceImpl(BookingRepository bookingRepository,
                             CourtRepository courtRepository,
                             SportRepository sportRepository,
                             PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.sportRepository = sportRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public ReportDataDTO generateReport(LocalDate fromDate, LocalDate toDate, String sportId, String courtId, String bookingType) {
        // 1. Chuẩn hóa khoảng thời gian
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate == null) {
            fromDate = toDate.minusDays(30);
        }
        if (fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        List<Booking> allBookings = bookingRepository.findAll();
        List<Court> allCourts = courtRepository.findAll();
        List<Sport> allSports = sportRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();

        final LocalDate finalFrom = fromDate;
        final LocalDate finalTo = toDate;

        // 2. Lọc danh sách đặt sân trong kỳ và theo tiêu chí
        List<Booking> filteredBookings = allBookings.stream().filter(b -> {
            if (b.getBookingDetails() == null || b.getBookingDetails().isEmpty()) {
                return false;
            }
            LocalDate bDate = b.getBookingDetails().get(0).getStartTime().toLocalDate();
            if (bDate.isBefore(finalFrom) || bDate.isAfter(finalTo)) {
                return false;
            }
            // Lọc hình thức đặt
            if (bookingType != null && !bookingType.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(bookingType)) {
                if ("Online".equalsIgnoreCase(bookingType) && b.getEmployee() != null) return false;
                if ("Tại quầy".equalsIgnoreCase(bookingType) && b.getEmployee() == null) return false;
            }
            // Lọc theo môn
            if (sportId != null && !sportId.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(sportId)) {
                boolean matchSport = b.getBookingDetails().stream().anyMatch(d ->
                        d.getCourt() != null && d.getCourt().getSport() != null &&
                        (sportId.equalsIgnoreCase(d.getCourt().getSport().getId()) ||
                         sportId.equalsIgnoreCase(d.getCourt().getSport().getName())));
                if (!matchSport) return false;
            }
            // Lọc theo sân
            if (courtId != null && !courtId.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(courtId)) {
                boolean matchCourt = b.getBookingDetails().stream().anyMatch(d ->
                        d.getCourt() != null &&
                        (courtId.equalsIgnoreCase(d.getCourt().getId()) ||
                         courtId.equalsIgnoreCase(d.getCourt().getName())));
                if (!matchCourt) return false;
            }
            return true;
        }).collect(Collectors.toList());

        // Lọc các lượt hoàn tất (UC36)
        List<Booking> completedBookings = filteredBookings.stream()
                .filter(b -> "Hoàn tất".equalsIgnoreCase(b.getStatus()) || "Đã hoàn tất".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());

        // 3. Tính toán dòng tiền thu ròng (UC35)
        Set<String> filteredBookingIds = filteredBookings.stream().map(Booking::getId).collect(Collectors.toSet());
        List<Payment> matchingPayments = allPayments.stream()
                .filter(p -> p.getBooking() != null && filteredBookingIds.contains(p.getBooking().getId()))
                .collect(Collectors.toList());

        BigDecimal totalThu = matchingPayments.stream()
                .filter(p -> "Thu".equalsIgnoreCase(p.getTransactionType()) || "Thanh toán".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDeposit = matchingPayments.stream()
                .filter(p -> "Cọc".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefund = matchingPayments.stream()
                .filter(p -> "Hoàn tiền".equalsIgnoreCase(p.getTransactionType()) || "Hoàn cọc".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netRevenue = totalThu.add(totalDeposit).subtract(totalRefund);

        // 4. Tính công suất sử dụng (UC36)
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        double totalRentedHours = completedBookings.stream()
                .flatMap(b -> b.getBookingDetails().stream())
                .mapToDouble(d -> {
                    if (d.getStartTime() != null && d.getEndTime() != null) {
                        return Duration.between(d.getStartTime(), d.getEndTime()).toMinutes() / 60.0;
                    }
                    return 0.0;
                }).sum();

        List<Court> scopedCourts = allCourts.stream().filter(c -> {
            if (courtId != null && !courtId.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(courtId)) {
                return courtId.equalsIgnoreCase(c.getId()) || courtId.equalsIgnoreCase(c.getName());
            }
            if (sportId != null && !sportId.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(sportId)) {
                return c.getSport() != null && (sportId.equalsIgnoreCase(c.getSport().getId()) || sportId.equalsIgnoreCase(c.getSport().getName()));
            }
            return true;
        }).collect(Collectors.toList());

        double totalAvailableHours = Math.max(1, scopedCourts.size()) * 16.0 * Math.max(1, days);
        double avgCapacity = totalAvailableHours > 0 ? (totalRentedHours / totalAvailableHours) * 100.0 : 0.0;
        avgCapacity = Math.min(100.0, Math.round(avgCapacity * 10.0) / 10.0);

        // 5. Báo cáo khách hàng thường xuyên (UC38)
        Map<Customer, List<Booking>> custBookings = completedBookings.stream()
                .filter(b -> b.getCustomer() != null)
                .collect(Collectors.groupingBy(Booking::getCustomer));

        List<FrequentCustomerReportDTO> frequentCusts = custBookings.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().size(), e1.getValue().size()))
                .map(entry -> {
                    Customer cust = entry.getKey();
                    List<Booking> bList = entry.getValue();
                    long cCount = bList.size();
                    double totalHrs = bList.stream()
                            .flatMap(b -> b.getBookingDetails().stream())
                            .mapToDouble(d -> Duration.between(d.getStartTime(), d.getEndTime()).toMinutes() / 60.0)
                            .sum();

                    long onlineCount = bList.stream().filter(b -> b.getEmployee() == null).count();
                    String primType = onlineCount >= (cCount - onlineCount) ? "Online" : "Tại quầy";

                    String favSport = bList.stream()
                            .flatMap(b -> b.getBookingDetails().stream())
                            .filter(d -> d.getCourt() != null && d.getCourt().getSport() != null)
                            .map(d -> d.getCourt().getSport().getName())
                            .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("Chưa xác định");

                    String st = cCount >= 3 ? "Thường xuyên" : "Tiềm năng";
                    if (cCount >= 30) st = "VIP";
                    else if (cCount >= 10) st = "Thân thiết";

                    return FrequentCustomerReportDTO.builder()
                            .customerId(cust.getId())
                            .fullName(cust.getFullName())
                            .phone(cust.getPhone())
                            .completedCount(cCount)
                            .totalHours(Math.round(totalHrs * 10.0) / 10.0)
                            .primaryType(primType)
                            .favoriteSport(favSport)
                            .status(st)
                            .build();
                })
                .collect(Collectors.toList());

        long frequentCount = frequentCusts.stream().filter(c -> c.getCompletedCount() >= 3).count();

        // 6. Xếp hạng sân (UC37)
        Map<Court, List<BookingDetail>> courtDetails = completedBookings.stream()
                .flatMap(b -> b.getBookingDetails().stream())
                .filter(d -> d.getCourt() != null)
                .collect(Collectors.groupingBy(BookingDetail::getCourt));

        List<TopCourtDTO> topCourts = new ArrayList<>();
        int rank = 1;
        List<Map.Entry<Court, List<BookingDetail>>> sortedCourtEntries = courtDetails.entrySet().stream()
                .sorted((e1, e2) -> {
                    int cmp = Integer.compare(e2.getValue().size(), e1.getValue().size());
                    if (cmp != 0) return cmp;
                    return e1.getKey().getId().compareTo(e2.getKey().getId());
                })
                .collect(Collectors.toList());

        for (Map.Entry<Court, List<BookingDetail>> entry : sortedCourtEntries) {
            Court c = entry.getKey();
            List<BookingDetail> details = entry.getValue();
            long count = details.size();
            double hours = details.stream().mapToDouble(d -> Duration.between(d.getStartTime(), d.getEndTime()).toMinutes() / 60.0).sum();
            double courtAvail = 16.0 * Math.max(1, days);
            double cap = courtAvail > 0 ? (hours / courtAvail) * 100.0 : 0.0;

            BigDecimal rev = matchingPayments.stream()
                    .filter(p -> p.getBooking() != null && p.getBooking().getBookingDetails().stream().anyMatch(d -> d.getCourt().getId().equals(c.getId())))
                    .filter(p -> "Thu".equalsIgnoreCase(p.getTransactionType()) || "Thanh toán".equalsIgnoreCase(p.getTransactionType()) || "Cọc".equalsIgnoreCase(p.getTransactionType()))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            topCourts.add(TopCourtDTO.builder()
                    .rank(rank++)
                    .courtId(c.getId())
                    .courtName(c.getName())
                    .sportName(c.getSport() != null ? c.getSport().getName() : "N/A")
                    .completedCount(count)
                    .capacity(Math.min(100.0, Math.round(cap * 10.0) / 10.0))
                    .totalRevenue(rev)
                    .build());
        }

        // 7. Cơ cấu môn thể thao (Donut)
        Map<Sport, List<Booking>> sportBookings = completedBookings.stream()
                .filter(b -> !b.getBookingDetails().isEmpty() && b.getBookingDetails().get(0).getCourt() != null && b.getBookingDetails().get(0).getCourt().getSport() != null)
                .collect(Collectors.groupingBy(b -> b.getBookingDetails().get(0).getCourt().getSport()));

        List<SportBreakdownDTO> sportBreakdowns = new ArrayList<>();
        long totalCompl = Math.max(1, completedBookings.size());
        String[] colors = {"#1aaa62", "#1684ef", "#ef9a28", "#7958e9", "#e45555"};
        int colorIdx = 0;

        for (Sport s : allSports) {
            List<Booking> sBookings = sportBookings.getOrDefault(s, Collections.emptyList());
            long count = sBookings.size();
            double pct = Math.round(((double) count / totalCompl * 100.0) * 10.0) / 10.0;

            double sHours = sBookings.stream()
                    .flatMap(b -> b.getBookingDetails().stream())
                    .mapToDouble(d -> Duration.between(d.getStartTime(), d.getEndTime()).toMinutes() / 60.0)
                    .sum();
            int courtCount = (int) allCourts.stream().filter(c -> c.getSport() != null && c.getSport().getId().equals(s.getId())).count();
            double sAvail = Math.max(1, courtCount) * 16.0 * Math.max(1, days);
            double sCap = sAvail > 0 ? (sHours / sAvail * 100.0) : 0.0;
            sCap = Math.min(100.0, Math.round(sCap * 10.0) / 10.0);

            sportBreakdowns.add(SportBreakdownDTO.builder()
                    .sportId(s.getId())
                    .sportName(s.getName())
                    .color(colors[colorIdx++ % colors.length])
                    .completedCount(count)
                    .percentage(pct)
                    .capacity(sCap)
                    .build());
        }

        // 8. Dòng tiền theo ngày
        List<DailyRevenueDTO> dailyRevenues = new ArrayList<>();
        LocalDate cur = fromDate;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        while (!cur.isAfter(toDate)) {
            final LocalDate d = cur;
            List<Payment> dPayments = matchingPayments.stream()
                    .filter(p -> p.getBooking() != null && !p.getBooking().getBookingDetails().isEmpty() &&
                            p.getBooking().getBookingDetails().get(0).getStartTime().toLocalDate().equals(d))
                    .collect(Collectors.toList());

            BigDecimal dThu = dPayments.stream()
                    .filter(p -> "Thu".equalsIgnoreCase(p.getTransactionType()) || "Thanh toán".equalsIgnoreCase(p.getTransactionType()) || "Cọc".equalsIgnoreCase(p.getTransactionType()))
                    .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal dHoan = dPayments.stream()
                    .filter(p -> "Hoàn tiền".equalsIgnoreCase(p.getTransactionType()) || "Hoàn cọc".equalsIgnoreCase(p.getTransactionType()))
                    .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            dailyRevenues.add(DailyRevenueDTO.builder()
                    .date(d)
                    .dateStr(d.format(dtf))
                    .thu(dThu)
                    .hoan(dHoan)
                    .netRevenue(dThu.subtract(dHoan))
                    .build());

            cur = cur.plusDays(1);
        }

        ReportSummaryDTO summary = ReportSummaryDTO.builder()
                .totalThu(totalThu)
                .totalDeposit(totalDeposit)
                .totalRefund(totalRefund)
                .netRevenue(netRevenue)
                .completedBookings(completedBookings.size())
                .averageCapacity(avgCapacity)
                .frequentCustomersCount(frequentCount)
                .build();

        return ReportDataDTO.builder()
                .summary(summary)
                .sportBreakdowns(sportBreakdowns)
                .topCourts(topCourts)
                .frequentCustomers(frequentCusts)
                .dailyRevenues(dailyRevenues)
                .build();
    }

    @Override
    public byte[] exportReportToCsv(LocalDate fromDate, LocalDate toDate, String sportId, String courtId, String bookingType) {
        ReportDataDTO report = generateReport(fromDate, toDate, sportId, courtId, bookingType);

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM
        sb.append('\uFEFF');

        sb.append("BÁO CÁO THỐNG KÊ HOẠT ĐỘNG & TÀI CHÍNH SPORTHUB\n");
        sb.append("Kỳ báo cáo: Từ ").append(fromDate).append(" Đến ").append(toDate).append("\n");
        sb.append("Môn: ").append(sportId != null ? sportId : "Tất cả").append(", Sân: ").append(courtId != null ? courtId : "Tất cả").append(", Hình thức: ").append(bookingType != null ? bookingType : "Tất cả").append("\n\n");

        sb.append("--- CHỈ SỐ KPI TỔNG HỢP ---\n");
        sb.append("Thu tiền ròng (VNĐ):,").append(report.getSummary().getNetRevenue()).append("\n");
        sb.append("Tổng thu thành công (VNĐ):,").append(report.getSummary().getTotalThu()).append("\n");
        sb.append("Tổng tiền cọc (VNĐ):,").append(report.getSummary().getTotalDeposit()).append("\n");
        sb.append("Tổng tiền hoàn trả (VNĐ):,").append(report.getSummary().getTotalRefund()).append("\n");
        sb.append("Số lượt thuê hoàn tất:,").append(report.getSummary().getCompletedBookings()).append("\n");
        sb.append("Công suất sử dụng trung bình (%):,").append(report.getSummary().getAverageCapacity()).append("%\n");
        sb.append("Số khách hàng thường xuyên (>=3 lượt):,").append(report.getSummary().getFrequentCustomersCount()).append("\n\n");

        sb.append("--- XẾP HẠNG SÂN THEO LƯỢT THUÊ (UC37) ---\n");
        sb.append("Hạng,Mã sân,Tên sân,Môn thể thao,Số lượt hoàn tất,Công suất (%),Doanh thu (VNĐ)\n");
        for (TopCourtDTO c : report.getTopCourts()) {
            sb.append(c.getRank()).append(",")
              .append(c.getCourtId()).append(",")
              .append("\"").append(c.getCourtName()).append("\",")
              .append("\"").append(c.getSportName()).append("\",")
              .append(c.getCompletedCount()).append(",")
              .append(c.getCapacity()).append("%,")
              .append(c.getTotalRevenue()).append("\n");
        }
        sb.append("\n");

        sb.append("--- DANH SÁCH KHÁCH HÀNG THƯỜNG XUYÊN (UC38) ---\n");
        sb.append("Mã khách,Họ và tên,Số điện thoại,Số lượt hoàn tất,Tổng giờ thuê,Hình thức chủ yếu,Môn thường đặt,Phân hạng\n");
        for (FrequentCustomerReportDTO cust : report.getFrequentCustomers()) {
            sb.append(cust.getCustomerId()).append(",")
              .append("\"").append(cust.getFullName()).append("\",")
              .append(cust.getPhone()).append(",")
              .append(cust.getCompletedCount()).append(",")
              .append(cust.getTotalHours()).append(",")
              .append(cust.getPrimaryType()).append(",")
              .append("\"").append(cust.getFavoriteSport()).append("\",")
              .append(cust.getStatus()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
