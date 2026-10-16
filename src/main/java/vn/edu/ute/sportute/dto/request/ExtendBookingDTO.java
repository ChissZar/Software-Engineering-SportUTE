package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class ExtendBookingDTO {
    @NotBlank(message = "Mã phiếu không được để trống")
    private String bookingId;

    private String courtName;
    private LocalTime currentEndTime;

    @NotNull(message = "Vui lòng chọn giờ kết thúc gia hạn mới")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime newEndTime;

    @Builder.Default
    private BigDecimal additionalFee = BigDecimal.ZERO;

    @Builder.Default
    private String paymentMethod = "Tiền mặt"; // Tiền mặt, Chuyển khoản

    public ExtendBookingDTO() {
        this.additionalFee = BigDecimal.ZERO;
        this.paymentMethod = "Tiền mặt";
    }

    public ExtendBookingDTO(String bookingId, String courtName, LocalTime currentEndTime,
                            LocalTime newEndTime, BigDecimal additionalFee, String paymentMethod) {
        this.bookingId = bookingId;
        this.courtName = courtName;
        this.currentEndTime = currentEndTime;
        this.newEndTime = newEndTime;
        this.additionalFee = additionalFee != null ? additionalFee : BigDecimal.ZERO;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "Tiền mặt";
    }
    public static class ExtendBookingDTOBuilder {
        public ExtendBookingDTOBuilder() {
        }
    }
}
