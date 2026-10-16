package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.sportute.dto.request.CustomerDTO;
import vn.edu.ute.sportute.dto.response.CustomerRentHistoryDTO;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.impl.CustomerServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .id("KH001")
                .fullName("Nguyễn Minh Anh")
                .phone("0908111222")
                .email("minhanh@gmail.com")
                .address("1 Võ Văn Ngân, Thủ Đức")
                .status("Hoạt động")
                .build();
    }

    @Test
    @DisplayName("UC30: Thêm mới khách hàng thành công và sinh mã KH tự động")
    void testSaveNewCustomer() {
        CustomerDTO dto = CustomerDTO.builder()
                .fullName("Trần Quốc Huy")
                .phone("0912345678")
                .email("huy.tran@gmail.com")
                .address("Hồ Chí Minh")
                .status("Hoạt động")
                .build();

        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerDTO saved = customerService.saveCustomer(dto);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("KH"));
        assertEquals("Trần Quốc Huy", saved.getFullName());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("UC31: Tính phân hạng VIP khi khách hàng có từ 30 lượt thuê")
    void testCustomerTierCalculationVIP() {
        when(customerRepository.findAll()).thenReturn(List.of(sampleCustomer));

        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            bookings.add(Booking.builder().id("B" + i).customer(sampleCustomer).status("Hoạt động").build());
        }
        when(bookingRepository.findAll()).thenReturn(bookings);

        List<CustomerDTO> list = customerService.getAllCustomers(null, "Tất cả", "Tất cả");

        assertEquals(1, list.size());
        assertEquals("VIP", list.get(0).getTier());
    }

    @Test
    @DisplayName("UC32: Tra cứu lịch sử thuê sân của khách hàng")
    void testGetCustomerRentHistory() {
        Court court = Court.builder().id("S01").name("Sân bóng đá 1").build();
        BookingDetail detail = BookingDetail.builder()
                .id("CT01")
                .court(court)
                .startTime(LocalDateTime.of(2026, 9, 10, 18, 0))
                .endTime(LocalDateTime.of(2026, 9, 10, 20, 0))
                .build();

        Payment payment = Payment.builder()
                .id("GD01")
                .amount(new BigDecimal("350000"))
                .transactionType("Thu")
                .build();

        Booking booking = Booking.builder()
                .id("B001")
                .customer(sampleCustomer)
                .status("Hoàn tất")
                .bookingDetails(List.of(detail))
                .payments(List.of(payment))
                .build();

        when(bookingRepository.findByCustomer_Id("KH001")).thenReturn(List.of(booking));

        List<CustomerRentHistoryDTO> history = customerService.getCustomerRentHistory("KH001");

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("B001", history.get(0).getBookingId());
        assertEquals("Sân bóng đá 1", history.get(0).getCourtName());
        assertEquals(new BigDecimal("350000"), history.get(0).getTotalAmount());
    }

    @Test
    @DisplayName("UC33: Khóa / Mở khóa tài khoản khách hàng thành công")
    void testToggleCustomerStatus() {
        Account account = Account.builder().id("TK001").status("Hoạt động").build();
        sampleCustomer.setAccount(account);

        when(customerRepository.findById("KH001")).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        customerService.toggleCustomerStatus("KH001");

        assertEquals("Ngừng hoạt động", sampleCustomer.getStatus());
        assertEquals("Bị khóa", account.getStatus());
    }
}
