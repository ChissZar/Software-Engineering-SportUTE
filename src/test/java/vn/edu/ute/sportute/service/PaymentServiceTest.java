package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.sportute.dto.response.PaymentDTO;
import vn.edu.ute.sportute.entity.Booking;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.entity.Payment;
import vn.edu.ute.sportute.repository.BookingRepository;
import vn.edu.ute.sportute.repository.PaymentRepository;
import vn.edu.ute.sportute.service.impl.PaymentServiceImpl;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Customer sampleCustomer;
    private Booking sampleBooking;
    private Payment paymentThu;
    private Payment paymentCoc;
    private Payment paymentHoan;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .id("KH001")
                .fullName("Nguyễn Văn An")
                .phone("0901234567")
                .build();

        sampleBooking = Booking.builder()
                .id("DS001")
                .customer(sampleCustomer)
                .status("Đã xác nhận")
                .build();

        paymentThu = Payment.builder()
                .id("GD001")
                .booking(sampleBooking)
                .amount(new BigDecimal("300000"))
                .transactionType("Thu")
                .paymentMethod("Tiền mặt")
                .build();

        paymentCoc = Payment.builder()
                .id("GD002")
                .booking(sampleBooking)
                .amount(new BigDecimal("200000"))
                .transactionType("Cọc")
                .paymentMethod("Chuyển khoản")
                .build();

        paymentHoan = Payment.builder()
                .id("GD003")
                .booking(sampleBooking)
                .amount(new BigDecimal("50000"))
                .transactionType("Hoàn tiền")
                .paymentMethod("Chuyển khoản")
                .build();
    }

    @Test
    @DisplayName("RB4: Tính toán chính xác doanh thu ròng (Thu + Cọc - Hoàn)")
    void testGetPaymentStatisticsNetRevenue() {
        when(paymentRepository.findAll()).thenReturn(List.of(paymentThu, paymentCoc, paymentHoan));

        Map<String, Object> stats = paymentService.getPaymentStatistics();

        // Thu: 300.000, Cọc: 200.000, Hoàn: 50.000 => Thu ròng = 450.000
        BigDecimal totalThu = (BigDecimal) stats.get("totalThu");
        BigDecimal totalDeposit = (BigDecimal) stats.get("totalDeposit");
        BigDecimal totalRefund = (BigDecimal) stats.get("totalRefund");
        BigDecimal netRevenue = (BigDecimal) stats.get("netRevenue");

        assertEquals(new BigDecimal("300000"), totalThu);
        assertEquals(new BigDecimal("200000"), totalDeposit);
        assertEquals(new BigDecimal("50000"), totalRefund);
        assertEquals(new BigDecimal("450000"), netRevenue);
        assertEquals(3L, stats.get("totalTransactions"));
    }

    @Test
    @DisplayName("Ghi nhận giao dịch thanh toán thành công và lưu vào cơ sở dữ liệu")
    void testRecordPaymentSuccess() {
        when(bookingRepository.findById("DS001")).thenReturn(Optional.of(sampleBooking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.recordPayment("DS001", new BigDecimal("100000"), "Thu", "Tiền mặt");

        verify(bookingRepository, times(1)).findById("DS001");
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Lọc danh sách thanh toán theo từ khóa, loại giao dịch và phương thức thanh toán")
    void testGetAllPaymentsFiltering() {
        when(paymentRepository.findAll()).thenReturn(List.of(paymentThu, paymentCoc, paymentHoan));

        // Lọc theo loại "Cọc"
        List<PaymentDTO> cocList = paymentService.getAllPayments(null, "Cọc", "Tất cả");
        assertEquals(1, cocList.size());
        assertEquals("GD002", cocList.get(0).getId());

        // Lọc theo phương thức "Tiền mặt"
        List<PaymentDTO> cashList = paymentService.getAllPayments(null, "Tất cả", "Tiền mặt");
        assertEquals(1, cashList.size());
        assertEquals("GD001", cashList.get(0).getId());

        // Lọc theo từ khóa mã phiếu "DS001"
        List<PaymentDTO> searchList = paymentService.getAllPayments("DS001", "Tất cả", "Tất cả");
        assertEquals(3, searchList.size());
    }
}
