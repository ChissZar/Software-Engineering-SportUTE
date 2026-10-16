package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CancelBookingDTO {
    @NotBlank(message = "Mã phiếu không được để trống")
    private String bookingId;

    private String customerName;
    private String courtName;
    private String scheduleTime;

    private String reason;

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal cancellationFeeRate = BigDecimal.valueOf(10.0); // % phí hủy

    @Builder.Default
    private BigDecimal cancellationFee = BigDecimal.ZERO; // Tiền phạt hủy

    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO; // Tiền hoàn lại cho khách

    public CancelBookingDTO() {
        this.totalAmount = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
        this.cancellationFeeRate = BigDecimal.valueOf(10.0);
        this.cancellationFee = BigDecimal.ZERO;
        this.refundAmount = BigDecimal.ZERO;
    }

    public CancelBookingDTO(String bookingId, String customerName, String courtName, String scheduleTime,
                            String reason, BigDecimal totalAmount, BigDecimal paidAmount,
                            BigDecimal cancellationFeeRate, BigDecimal cancellationFee, BigDecimal refundAmount) {
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.courtName = courtName;
        this.scheduleTime = scheduleTime;
        this.reason = reason;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.cancellationFeeRate = cancellationFeeRate != null ? cancellationFeeRate : BigDecimal.valueOf(10.0);
        this.cancellationFee = cancellationFee != null ? cancellationFee : BigDecimal.ZERO;
        this.refundAmount = refundAmount != null ? refundAmount : BigDecimal.ZERO;
    }
    public static class CancelBookingDTOBuilder {
        public CancelBookingDTOBuilder() {
        }
    }
}
