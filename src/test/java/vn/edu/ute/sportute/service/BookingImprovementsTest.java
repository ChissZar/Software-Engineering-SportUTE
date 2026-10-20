package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.model.MatchmakingPost;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.impl.*;
import vn.edu.ute.sportute.dto.request.*;
import java.math.BigDecimal;
import java.time.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BookingImprovementsTest {
    @TempDir Path temp;
    private final PriceRepository prices = mock(PriceRepository.class);
    private final CourtRepository courts = mock(CourtRepository.class);
    private final PromotionRepository promotions = mock(PromotionRepository.class);
    private final BookingPricingService pricing = new BookingPricingService(prices,courts,promotions);
    private final Court court = Court.builder().id("S1").status("Hoạt động").sport(Sport.builder().id("M_CL").build()).build();

    @BeforeEach void setup() { when(courts.findById("S1")).thenReturn(Optional.of(court)); }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }

    @Test void missingCustomerProfileNeverCreatesFakeCustomer() {
        var accounts=mock(AccountRepository.class);var customers=mock(CustomerRepository.class);
        var service=new CustomerBookingServiceImpl(courts,mock(BookingRepository.class),mock(BookingDetailRepository.class),
            mock(MaintenanceRepository.class),accounts,customers,prices,mock(BookingPolicyRepository.class),mock(PaymentRepository.class),promotions);
        when(accounts.findByUsernameOrEmail("admin","admin")).thenReturn(Optional.of(Account.builder().id("TK001").build()));
        assertThrows(IllegalArgumentException.class,()->service.getCustomerBookings("admin",null,null));
        assertThrows(IllegalArgumentException.class,()->service.getCustomerBookings(null,null,null));
        verify(customers,never()).save(any());
        verify(customers,never()).findAll();
    }

    @Test void thirtyMinuteGapHasCorrectPriceAndExplicitPercent() {
        Promotion p=Promotion.builder().id("KM1").discountType("PERCENT").discountAmount(new BigDecimal("10")).fromDate(LocalDate.now()).toDate(LocalDate.now()).build();
        when(promotions.findAll()).thenReturn(List.of(p));
        var q=pricing.quote("S1",LocalDate.now().plusDays(1),LocalTime.of(14,30),LocalTime.of(15,0),"km1");
        assertEquals(new BigDecimal("60000"),q.raw());
        assertEquals(new BigDecimal("6000"),q.discount());
        assertEquals(new BigDecimal("54000"),q.total());
        p.setDiscountType("AMOUNT");
        assertEquals(new BigDecimal("10"),pricing.discount(p,q.raw()));
    }
    @Test void invalidExpiredAndUnknownPromotionsCannotApply() {
        var p=Promotion.builder().id("OLD").toDate(LocalDate.now().minusDays(1)).build();
        when(promotions.findAll()).thenReturn(List.of(p));
        assertThrows(IllegalArgumentException.class,()->pricing.promotion("OLD"));
        assertThrows(IllegalArgumentException.class,()->pricing.promotion("SUMMER2026"));
        assertThrows(IllegalArgumentException.class,()->pricing.quote("S1",LocalDate.now(),LocalTime.of(14,30),LocalTime.of(14,45),null));
    }
    @Test void serverIgnoresForgedClientPriceAndDiscount() {
        var bookings=mock(BookingRepository.class);var details=mock(BookingDetailRepository.class);var maintenance=mock(MaintenanceRepository.class);
        var accounts=mock(AccountRepository.class);var customers=mock(CustomerRepository.class);var policies=mock(BookingPolicyRepository.class);var payments=mock(PaymentRepository.class);
        var service=new CustomerBookingServiceImpl(courts,bookings,details,maintenance,accounts,customers,prices,policies,payments,promotions); TestIds.inject(service);
        ReflectionTestUtils.setField(service,"pricing",pricing);
        var account=Account.builder().id("A1").username("alice").build();
        var customer=Customer.builder().id("C1").account(account).build();
        when(accounts.findByUsernameOrEmail("alice","alice")).thenReturn(Optional.of(account));
        when(customers.findByAccount_Id("A1")).thenReturn(Optional.of(customer));
        when(courts.findForUpdate("S1")).thenReturn(Optional.of(court));
        when(bookings.save(any())).thenAnswer(i->i.getArgument(0));
        var request=CustomerBookingRequestDTO.builder().courtId("S1").bookingDate(LocalDate.now().plusDays(1)).startTime(LocalTime.of(14,30)).durationMinutes(30)
            .totalAmount(BigDecimal.ONE).discountAmount(new BigDecimal("999999")).depositChoice("DEPOSIT_30").build();
        var b=service.bookCourtOnline(request,"alice");
        assertEquals(new BigDecimal("60000"),b.getQuotedAmount());
        assertEquals("Chờ xác nhận",b.getStatus());
        verify(payments).save(argThat(p->p.getAmount().equals(new BigDecimal("18000")) && "PENDING".equals(p.getPaymentStatus())));
        verify(details).existsOverlappingBooking("S1",request.getBookingDate().atTime(14,30),request.getBookingDate().atTime(15,0));
    }
    @Test void pendingTransfersAreNotRevenueAndCannotBeCountedTwice() {
        var repo=mock(PaymentRepository.class);var bookings=mock(BookingRepository.class);
        var service=new PaymentServiceImpl(repo,bookings); TestIds.inject(service);
        ReflectionTestUtils.setField(service,"amounts",new BookingAmounts(pricing));
        var b=Booking.builder().id("B").status("Chờ xác nhận").quotedAmount(new BigDecimal("100000")).build();
        var p=Payment.builder().id("P").booking(b).amount(new BigDecimal("30000")).transactionType("Cọc").paymentStatus("PENDING").build();b.getPayments().add(p);
        when(repo.findAll()).thenReturn(List.of(p)); when(repo.findById("P")).thenReturn(Optional.of(p));when(bookings.findForUpdate("B")).thenReturn(Optional.of(b));
        assertEquals(BigDecimal.ZERO,service.getPaymentStatistics().get("netRevenue"));
        service.confirmPayment("P",true);
        assertEquals("Đã xác nhận",b.getStatus());
        assertEquals(new BigDecimal("30000"),BookingAmounts.paid(b));
        assertThrows(IllegalArgumentException.class,()->service.confirmPayment("P",true));
        assertThrows(IllegalArgumentException.class,()->service.recordPayment("B",new BigDecimal("70001"),"Thu","Tiền mặt"));
    }
    @Test void paymentDatesRemainUnknownForLegacyRows() {
        var repo=mock(PaymentRepository.class);var service=new PaymentServiceImpl(repo,mock(BookingRepository.class)); TestIds.inject(service);
        when(repo.findAll()).thenReturn(List.of(Payment.builder().id("P").paymentStatus("LEGACY").build()));
        var dto=service.getAllPayments(null,null,null).get(0);
        assertNull(dto.getTransactionDate());assertTrue(dto.getStatus().contains("chưa đối soát"));
    }
    @Test void historyOrdersByCreationTimeInsteadOfRandomIdentifier() {
        var repo=mock(BookingRepository.class);
        var service=new AdminBookingServiceImpl(repo,mock(BookingDetailRepository.class),mock(CustomerRepository.class),courts,
            mock(EmployeeRepository.class),mock(MaintenanceRepository.class),mock(PaymentRepository.class),prices,mock(BookingPolicyRepository.class),promotions);
        ReflectionTestUtils.setField(service,"amounts",new BookingAmounts(pricing));
        var old=Booking.builder().id("DSZZZZ").createdAt(LocalDateTime.now().minusDays(1)).quotedAmount(BigDecimal.ZERO).build();
        var latest=Booking.builder().id("DSAAAA").createdAt(LocalDateTime.now()).quotedAmount(BigDecimal.ZERO).build();
        var legacy=Booking.builder().id("DS9999").quotedAmount(BigDecimal.ZERO).build();
        when(repo.findAll()).thenReturn(List.of(old,legacy,latest));
        assertEquals(List.of("DSAAAA","DSZZZZ","DS9999"),service.getBookings(null,null).stream().map(AdminBookingDTO::getId).toList());
    }
    @Test void privateContactsAndMessagesSurviveRestart() {
        var service=new MatchmakingServiceImpl(Optional.empty()); TestIds.inject(service);
        var store=mock(MatchmakingStore.class);
        var records=new java.util.HashMap<String,MatchmakingPost>();
        doAnswer(i -> { MatchmakingPost post=i.getArgument(0);records.put(post.getId(),post);return null; }).when(store).save(any());
        when(store.findById(anyString())).thenAnswer(i -> records.get(i.getArgument(0)));
        ReflectionTestUtils.setField(service,"store",store);
        var p=MatchmakingPost.builder().creatorAccountId("A1").creatorPhone("0901111111").creatorEmail("a@example.test").creatorName("Alice")
            .matchDate(LocalDate.now().plusDays(1).toString()).startTime("14:30").endTime("15:00").build();
        service.createPost(p);
        assertThrows(IllegalArgumentException.class,()->service.acceptMatch(p.getId(),"Alice","0901","","hi","A1"));
        service.acceptMatch(p.getId(),"Bob","0902222222","b@example.test","Hẹn ở cổng sân","A2");
        assertNull(service.visibleTo(p,"OUTSIDER").getOpponentPhone());
        assertNull(service.visibleTo(p,null).getOpponentMessage());
        assertEquals("0902222222",service.visibleTo(p,"A1").getOpponentPhone());
        assertEquals("Hẹn ở cổng sân",service.visibleTo(p,"A2").getOpponentMessage());
        assertThrows(IllegalStateException.class,()->service.acceptMatch(p.getId(),"Third","0903333333","","","A3"));
        service.markNotificationRead(p.getId(),"A1");
        var restarted=new MatchmakingServiceImpl(Optional.empty());ReflectionTestUtils.setField(restarted,"store",store);
        assertTrue(restarted.getPostById(p.getId()).isCreatorNotificationRead());
        assertFalse(restarted.getPostById(p.getId()).isOpponentNotificationRead());
    }
    @Test void maintenanceResolvesUsernameAndKeepsHistoryWhenCancelled() {
        var repo=mock(MaintenanceRepository.class);var employees=mock(EmployeeRepository.class);var bookings=mock(BookingDetailRepository.class);
        var service=new MaintenanceServiceImpl(repo,courts,employees,bookings); TestIds.inject(service);
        var employee=Employee.builder().id("NV1").build();when(employees.findByAccount_Username("operator")).thenReturn(Optional.of(employee));when(courts.findForUpdate("S1")).thenReturn(Optional.of(court));
        var dto=MaintenanceDTO.builder().courtId("S1").startDate(LocalDateTime.now().plusDays(1)).endDate(LocalDateTime.now().plusDays(1).plusHours(1)).reason("Thay lưới").build();
        service.createMaintenance(dto,"operator");verify(repo).save(argThat(m->m.getEmployee()==employee));
        var m=Maintenance.builder().id("M1").court(court).startDate(dto.getStartDate()).endDate(dto.getEndDate()).build();when(repo.findById("M1")).thenReturn(Optional.of(m));
        service.deleteMaintenance("M1");assertEquals("CANCELLED",m.getLifecycleStatus());verify(repo,never()).delete(any());
        when(repo.existsOverlappingMaintenance(anyString(),any(),any())).thenReturn(true);
        assertThrows(IllegalArgumentException.class,()->service.createMaintenance(dto,"operator"));
    }
    @Test void managerCannotPromoteOrDisableAdministrator() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("manager","",List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
        var employees=mock(EmployeeRepository.class);var service=new EmployeeServiceImpl(employees,mock(AccountRepository.class),mock(RoleRepository.class),mock(org.springframework.security.crypto.password.PasswordEncoder.class)); TestIds.inject(service);
        assertThrows(IllegalArgumentException.class,()->service.saveEmployee(EmployeeDTO.builder().position("Quản lý").build()));
        var admin=Employee.builder().id("NV_ADMIN").position("Quản trị viên").build();when(employees.findById("NV_ADMIN")).thenReturn(Optional.of(admin));
        assertThrows(IllegalArgumentException.class,()->service.toggleEmployeeStatus("NV_ADMIN"));verify(employees,never()).save(any());
    }
}
