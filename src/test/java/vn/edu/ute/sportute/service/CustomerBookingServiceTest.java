package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.sportute.dto.request.CustomerBookingRequestDTO;
import vn.edu.ute.sportute.dto.request.CustomerCourtFilterDTO;
import vn.edu.ute.sportute.dto.response.CustomerCourtCardDTO;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.impl.CustomerBookingServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerBookingServiceTest {

    @Mock private CourtRepository courtRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingDetailRepository bookingDetailRepository;
    @Mock private MaintenanceRepository maintenanceRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PriceRepository priceRepository;
    @Mock private BookingPolicyRepository bookingPolicyRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PromotionRepository promotionRepository;

    private CustomerBookingServiceImpl customerBookingService;

    @BeforeEach
    void setUp() {
        customerBookingService = new CustomerBookingServiceImpl(
                courtRepository,
                bookingRepository,
                bookingDetailRepository,
                maintenanceRepository,
                accountRepository,
                customerRepository,
                priceRepository,
                bookingPolicyRepository,
                paymentRepository, promotionRepository
        );
        TestIds.inject(customerBookingService);
        org.springframework.test.util.ReflectionTestUtils.setField(customerBookingService,"amounts",new BookingAmounts(new BookingPricingService(priceRepository,courtRepository,promotionRepository)));
        org.springframework.test.util.ReflectionTestUtils.setField(customerBookingService, "pricing", new BookingPricingService(priceRepository,courtRepository,promotionRepository));
    }

    @Test
    void testSearchAvailableCourts_FilterMaintenanceAndBooked() {
        Sport badminton = Sport.builder().id("M_CL").name("Cầu lông").build();
        Court court1 = Court.builder().id("S01").name("Sân 1").sport(badminton).status("Hoạt động").build();
        Court court2 = Court.builder().id("S02").name("Sân 2").sport(badminton).status("Hoạt động").build();

        when(courtRepository.findAll()).thenReturn(List.of(court1, court2));
        // S01 is in maintenance, S02 is available
        when(maintenanceRepository.existsOverlappingMaintenance(eq("S01"), any(), any())).thenReturn(true);
        when(maintenanceRepository.existsOverlappingMaintenance(eq("S02"), any(), any())).thenReturn(false);
        when(bookingDetailRepository.existsOverlappingBooking(eq("S02"), any(), any())).thenReturn(false);

        CustomerCourtFilterDTO filter = CustomerCourtFilterDTO.builder()
                .sportId("M_CL")
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .durationMinutes(60)
                .build();

        List<CustomerCourtCardDTO> results = customerBookingService.searchAvailableCourts(filter);

        assertEquals(1, results.size());
        assertEquals("S02", results.get(0).getId());
    }

    @Test
    void testBookCourtOnline_Success() {
        Account account = Account.builder().id("TK01").username("minhanh").email("minhanh@gmail.com").build();
        Customer customer = Customer.builder().id("KH01").fullName("Minh Anh").account(account).build();
        Court court = Court.builder().id("S01").name("Sân 1").build();

        when(accountRepository.findByUsernameOrEmail("minhanh", "minhanh")).thenReturn(Optional.of(account));
        when(customerRepository.findByAccount_Id("TK01")).thenReturn(Optional.of(customer));
        when(courtRepository.findForUpdate("S01")).thenReturn(Optional.of(court));
        when(courtRepository.findById("S01")).thenReturn(Optional.of(court));
        when(maintenanceRepository.existsOverlappingMaintenance(eq("S01"), any(), any())).thenReturn(false);
        when(bookingDetailRepository.existsOverlappingBooking(eq("S01"), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerBookingRequestDTO request = CustomerBookingRequestDTO.builder()
                .courtId("S01")
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(17, 0))
                .durationMinutes(60)
                .totalAmount(BigDecimal.valueOf(120000))
                .build();

        customerBookingService.bookCourtOnline(request, "minhanh");

        verify(bookingRepository, times(1)).save(argThat(b -> "Chờ xác nhận".equals(b.getStatus())));
        verify(paymentRepository, times(1)).save(argThat(p -> "Cọc".equals(p.getTransactionType())));
    }

    @Test
    void testCancelCustomerBooking_RefundCalculated() {
        Account account = Account.builder().id("TK01").username("minhanh").build();
        Customer customer = Customer.builder().id("KH01").fullName("Minh Anh").account(account).build();
        Court court = Court.builder().id("S01").name("Sân 1").status("Đang sử dụng").build();
        BookingPolicy policy = BookingPolicy.builder().id("CS01").cancellationFeeRate(BigDecimal.valueOf(10.0)).build();

        Booking booking = Booking.builder().id("DS01").status("Đã xác nhận")
                .customer(customer)
                .bookingPolicy(policy)
                .bookingDetails(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
        booking.getBookingDetails().add(BookingDetail.builder().id("CT01").court(court).build());
        booking.getPayments().add(Payment.builder().id("GD01").amount(BigDecimal.valueOf(100000)).transactionType("Cọc").build());

        when(accountRepository.findByUsernameOrEmail("minhanh", "minhanh")).thenReturn(Optional.of(account));
        when(customerRepository.findByAccount_Id("TK01")).thenReturn(Optional.of(customer));
        when(bookingRepository.findById("DS01")).thenReturn(Optional.of(booking));

        customerBookingService.cancelBookingByCustomer("DS01", "minhanh", "Bận việc");

        assertEquals("Đã hủy", booking.getStatus());
        assertEquals("Hoạt động", court.getStatus());
        verify(paymentRepository, times(1)).save(argThat(p -> 
                "Hoàn tiền".equals(p.getTransactionType()) && p.getAmount().compareTo(BigDecimal.valueOf(90000)) == 0
        ));
    }
}
