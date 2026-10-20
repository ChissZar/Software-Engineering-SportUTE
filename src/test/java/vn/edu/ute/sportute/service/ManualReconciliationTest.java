package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.Test;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.impl.AdminBookingServiceImpl;
import vn.edu.ute.sportute.dto.request.CancelBookingDTO;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManualReconciliationTest {
    private final BookingRepository bookings=mock(BookingRepository.class);
    private final PaymentRepository payments=mock(PaymentRepository.class);
    private final AdminBookingServiceImpl service=new AdminBookingServiceImpl(bookings,mock(BookingDetailRepository.class),
        mock(CustomerRepository.class),mock(CourtRepository.class),mock(EmployeeRepository.class),
        mock(MaintenanceRepository.class),payments,mock(PriceRepository.class),mock(BookingPolicyRepository.class),mock(PromotionRepository.class));
    private Booking booking(String id, String paymentStatus) {
        var b=Booking.builder().id(id).status("Đã xác nhận")
            .bookingPolicy(BookingPolicy.builder().cancellationFeeRate(BigDecimal.TEN).build()).build();
        b.getPayments().add(Payment.builder().id("GD001").booking(b).amount(new BigDecimal("100000"))
            .transactionType("Cọc").paymentStatus(paymentStatus).build());
        when(bookings.findForUpdate(id)).thenReturn(Optional.of(b));TestIds.inject(service);return b;
    }
    @Test void quickCancelAndPolicyCancelCreateSamePendingRefundAndRejectRepeat() {
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        for(boolean quick:new boolean[]{true,false}) {
            var b=booking(quick?"DS001":"DS002","CONFIRMED");
            if(quick)service.cancelBooking(b.getId());else service.cancelBookingWithPolicy(CancelBookingDTO.builder().bookingId(b.getId()).build());
            assertEquals("Đã hủy",b.getStatus());
            var refund=b.getPayments().stream().filter(p->"Hoàn tiền".equals(p.getTransactionType())).findFirst().orElseThrow();
            assertEquals(new BigDecimal("90000"),refund.getAmount());assertEquals("PENDING",refund.getPaymentStatus());
            assertEquals(new BigDecimal("100000"),BookingAmounts.paid(b));
            assertThrows(IllegalArgumentException.class,()->service.cancelBooking(b.getId()));
            assertEquals(2,b.getPayments().size());
        }
    }
    @Test void unverifiedTransferIsVoidedWithoutInventingARefund() {
        var b=booking("DS001","PENDING");service.cancelBooking(b.getId());
        assertEquals("VOID",b.getPayments().get(0).getPaymentStatus());
        assertEquals(1,b.getPayments().size());verify(payments,never()).save(any());
    }
    @Test void existingRefundsAreDeductedAndUsedBookingCannotBeCancelled() {
        var b=booking("DS001","CONFIRMED");
        b.getPayments().add(Payment.builder().id("GD002").transactionType("Hoàn tiền").amount(new BigDecimal("20000")).paymentStatus("CONFIRMED").build());
        b.getPayments().add(Payment.builder().id("GD003").transactionType("Hoàn tiền").amount(new BigDecimal("30000")).paymentStatus("PENDING").build());
        assertEquals(new BigDecimal("40000"),BookingAmounts.cancellationRefund(b));
        b.setStatus("Đang sử dụng");
        assertThrows(IllegalArgumentException.class,()->service.cancelBooking(b.getId()));
        verify(payments,never()).save(any());
    }
}
