package vn.edu.ute.sportute.service;
import java.math.*;
import java.time.Duration;
import vn.edu.ute.sportute.entity.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class BookingAmounts {
    private final BookingPricingService pricing;
    public static boolean counted(Payment p) { return p.getPaymentStatus() == null || "LEGACY".equals(p.getPaymentStatus()) || "CONFIRMED".equals(p.getPaymentStatus()); }
    public static BigDecimal paid(Booking b) {
        return b.getPayments().stream().filter(BookingAmounts::counted)
            .map(p -> "Hoàn tiền".equals(p.getTransactionType()) ? p.getAmount().negate() : p.getAmount())
            .reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    public static BigDecimal cancellationRefund(Booking b) {
        BigDecimal received = b.getPayments().stream().filter(BookingAmounts::counted)
            .filter(p -> !"Hoàn tiền".equals(p.getTransactionType())).map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rate = b.getBookingPolicy() != null && b.getBookingPolicy().getCancellationFeeRate() != null
            ? b.getBookingPolicy().getCancellationFeeRate() : BigDecimal.TEN;
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("Tỷ lệ phí hủy phải từ 0 đến 100%");
        BigDecimal fee = received.multiply(rate).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP);
        BigDecimal alreadyRefundedOrPending = b.getPayments().stream()
            .filter(p -> "Hoàn tiền".equals(p.getTransactionType()) && (counted(p) || "PENDING".equals(p.getPaymentStatus())))
            .map(Payment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        return received.subtract(fee).subtract(alreadyRefundedOrPending).max(BigDecimal.ZERO);
    }
    public BigDecimal total(Booking b) {
        BigDecimal court = b.getQuotedAmount();
        if (court == null) {
            court = BigDecimal.ZERO;
            for (BookingDetail d : b.getBookingDetails()) court = court.add(pricing.hourlyRate(d.getCourt().getId(),d.getStartTime().toLocalDate())
                .multiply(BigDecimal.valueOf(Duration.between(d.getStartTime(),d.getEndTime()).toMinutes())).divide(BigDecimal.valueOf(60),0,RoundingMode.HALF_UP));
            court = court.subtract(b.getDiscountApplied() == null ? pricing.discount(b.getPromotion(), court) : b.getDiscountApplied()).max(BigDecimal.ZERO);
        }
        return court.add(b.getServiceDetails().stream().map(BookingServiceDetail::getSubtotal).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
}
