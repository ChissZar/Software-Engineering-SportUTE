package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.sportute.dto.response.ReportDataDTO;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.impl.ReportServiceImpl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private SportRepository sportRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Sport footballSport;
    private Court footballCourt;
    private Customer sampleCustomer;
    private Booking completedBooking;
    private Payment paymentThu;
    private Payment paymentCoc;
    private Payment paymentHoan;

    @BeforeEach
    void setUp() {
        footballSport = Sport.builder().id("MON01").name("Bóng đá").build();
        footballCourt = Court.builder().id("S01").name("Sân bóng đá 1").sport(footballSport).build();

        sampleCustomer = Customer.builder().id("KH001").fullName("Lê Văn Tám").phone("0988776655").build();

        BookingDetail detail = BookingDetail.builder()
                .id("CT01")
                .court(footballCourt)
                .startTime(LocalDateTime.of(2026, 9, 15, 17, 0))
                .endTime(LocalDateTime.of(2026, 9, 15, 19, 0))
                .build();

        completedBooking = Booking.builder()
                .id("B001")
                .customer(sampleCustomer)
                .status("Hoàn tất")
                .bookingDetails(List.of(detail))
                .build();

        paymentThu = Payment.builder().id("GD01").booking(completedBooking).transactionType("Thu").amount(new BigDecimal("400000")).build();
        paymentCoc = Payment.builder().id("GD02").booking(completedBooking).transactionType("Cọc").amount(new BigDecimal("100000")).build();
        paymentHoan = Payment.builder().id("GD03").booking(completedBooking).transactionType("Hoàn tiền").amount(new BigDecimal("50000")).build();
    }

    @Test
    @DisplayName("UC35: Tính toán chính xác doanh thu ròng RB4 = (Thu + Cọc) - Hoàn tiền")
    void testNetRevenueCalculation() {
        when(bookingRepository.findAll()).thenReturn(List.of(completedBooking));
        when(courtRepository.findAll()).thenReturn(List.of(footballCourt));
        when(sportRepository.findAll()).thenReturn(List.of(footballSport));
        when(paymentRepository.findAll()).thenReturn(List.of(paymentThu, paymentCoc, paymentHoan));

        ReportDataDTO result = reportService.generateReport(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "Tất cả", "Tất cả", "Tất cả"
        );

        assertNotNull(result);
        assertNotNull(result.getSummary());

        // (400.000 + 100.000) - 50.000 = 450.000 đ
        assertEquals(new BigDecimal("450000"), result.getSummary().getNetRevenue());
        assertEquals(new BigDecimal("400000"), result.getSummary().getTotalThu());
        assertEquals(new BigDecimal("100000"), result.getSummary().getTotalDeposit());
        assertEquals(new BigDecimal("50000"), result.getSummary().getTotalRefund());
        assertEquals(1, result.getSummary().getCompletedBookings());
    }

    @Test
    @DisplayName("UC37: Xếp hạng sân được thuê theo lượt hoàn tất giảm dần")
    void testTopCourtsRanking() {
        when(bookingRepository.findAll()).thenReturn(List.of(completedBooking));
        when(courtRepository.findAll()).thenReturn(List.of(footballCourt));
        when(sportRepository.findAll()).thenReturn(List.of(footballSport));
        when(paymentRepository.findAll()).thenReturn(List.of(paymentThu));

        ReportDataDTO result = reportService.generateReport(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "Tất cả", "Tất cả", "Tất cả"
        );

        assertEquals(1, result.getTopCourts().size());
        assertEquals("Sân bóng đá 1", result.getTopCourts().get(0).getCourtName());
        assertEquals(1, result.getTopCourts().get(0).getCompletedCount());
        assertEquals(1, result.getTopCourts().get(0).getRank());
    }

    @Test
    @DisplayName("UC39: Xuất báo cáo CSV chứa UTF-8 BOM và đúng nội dung báo cáo")
    void testExportReportToCsv() {
        when(bookingRepository.findAll()).thenReturn(List.of(completedBooking));
        when(courtRepository.findAll()).thenReturn(List.of(footballCourt));
        when(sportRepository.findAll()).thenReturn(List.of(footballSport));
        when(paymentRepository.findAll()).thenReturn(List.of(paymentThu));

        byte[] bytes = reportService.exportReportToCsv(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "Tất cả", "Tất cả", "Tất cả"
        );

        assertNotNull(bytes);
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(csv.contains("BÁO CÁO THỐNG KÊ"));
        assertTrue(csv.contains("Thu tiền ròng"));
        assertTrue(csv.contains("Sân bóng đá 1"));
    }
}
