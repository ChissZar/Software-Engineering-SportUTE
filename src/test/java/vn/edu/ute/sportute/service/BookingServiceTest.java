package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.sportute.dto.request.CancelBookingDTO;
import vn.edu.ute.sportute.dto.request.ChangeScheduleDTO;
import vn.edu.ute.sportute.dto.request.ExtendBookingDTO;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.impl.AdminBookingServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingDetailRepository bookingDetailRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private MaintenanceRepository maintenanceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PriceRepository priceRepository;
    @Mock private BookingPolicyRepository bookingPolicyRepository;
    @Mock private PromotionRepository promotionRepository;

    private AdminBookingServiceImpl adminBookingService;

    @BeforeEach
    void setUp() {
        adminBookingService = new AdminBookingServiceImpl(
                bookingRepository,
                bookingDetailRepository,
                customerRepository,
                courtRepository,
                employeeRepository,
                maintenanceRepository,
                paymentRepository,
                priceRepository,
                bookingPolicyRepository,
                promotionRepository
        );
        TestIds.inject(adminBookingService);
        var pricing = new BookingPricingService(priceRepository,courtRepository,promotionRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(adminBookingService, "pricing", pricing);
        org.springframework.test.util.ReflectionTestUtils.setField(adminBookingService, "amounts", new BookingAmounts(pricing));
    }

    @Test
    void testCheckInSuccess() {
        Court court = Court.builder().id("S01").name("Sân 1").status("Hoạt động").build();
        BookingDetail detail = BookingDetail.builder().id("CT01").court(court)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2))
                .build();
        Booking booking = Booking.builder().id("DS01").status("Đã xác nhận")
                .bookingDetails(new ArrayList<>())
                .build();
        booking.getBookingDetails().add(detail);

        when(bookingRepository.findById("DS01")).thenReturn(Optional.of(booking));

        adminBookingService.checkInBooking("DS01");

        assertEquals("Đang sử dụng", booking.getStatus());
        assertEquals("Đang sử dụng", court.getStatus());
        verify(bookingRepository, times(1)).save(booking);
        verify(courtRepository, times(1)).save(court);
    }

    @Test
    void testCheckOutSuccess() {
        Court court = Court.builder().id("S01").name("Sân 1").status("Đang sử dụng").build();
        BookingDetail detail = BookingDetail.builder().id("CT01").court(court)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now())
                .build();
        Booking booking = Booking.builder().id("DS01").status("Đang sử dụng")
                .quotedAmount(BigDecimal.valueOf(300000))
                .bookingDetails(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
        booking.getBookingDetails().add(detail);

        when(bookingRepository.findById("DS01")).thenReturn(Optional.of(booking));

        adminBookingService.checkOutBooking("DS01");

        assertEquals("Hoàn tất", booking.getStatus());
        assertEquals("Hoạt động", court.getStatus());
        verify(bookingRepository, times(1)).save(booking);
        verify(courtRepository, times(1)).save(court);
    }

    @Test
    void testExtendBookingConflictWithMaintenance() {
        Court court = Court.builder().id("S01").name("Sân 1").status("Đang sử dụng").build();
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 10, 0);
        BookingDetail detail = BookingDetail.builder().id("CT01").court(court)
                .startTime(end.minusHours(2))
                .endTime(end)
                .build();
        Booking booking = Booking.builder().id("DS01").status("Đang sử dụng")
                .bookingDetails(new ArrayList<>())
                .build();
        booking.getBookingDetails().add(detail);

        when(bookingRepository.findById("DS01")).thenReturn(Optional.of(booking));
        when(maintenanceRepository.existsOverlappingMaintenance(eq("S01"), eq(end), any())).thenReturn(true);

        ExtendBookingDTO dto = ExtendBookingDTO.builder()
                .bookingId("DS01")
                .newEndTime(LocalTime.of(11, 0))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> adminBookingService.extendBooking(dto));
        assertTrue(ex.getMessage().contains("bảo trì"));
    }

    @Test
    void testCancelBookingWithPolicyRefund() {
        BookingPolicy policy = BookingPolicy.builder().id("CS01").name("Tiêu chuẩn")
                .cancellationFeeRate(BigDecimal.valueOf(10.0))
                .build();
        Court court = Court.builder().id("S01").name("Sân 1").status("Hoạt động").build();
        BookingDetail detail = BookingDetail.builder().id("CT01").court(court).build();

        Booking booking = Booking.builder().id("DS01").status("Đã xác nhận")
                .bookingPolicy(policy)
                .bookingDetails(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
        booking.getBookingDetails().add(detail);
        booking.getPayments().add(Payment.builder().id("GD01").amount(BigDecimal.valueOf(200000)).transactionType("Thu").build());

        when(bookingRepository.findById("DS01")).thenReturn(Optional.of(booking));

        CancelBookingDTO dto = CancelBookingDTO.builder()
                .bookingId("DS01")
                .reason("Khách hủy lịch")
                .build();

        adminBookingService.cancelBookingWithPolicy(dto);

        assertEquals("Đã hủy", booking.getStatus());
        verify(paymentRepository, times(1)).save(argThat(p -> 
                "Hoàn tiền".equals(p.getTransactionType()) && p.getAmount().compareTo(BigDecimal.valueOf(180000)) == 0
        ));
    }
}
