package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class ChangeScheduleDTO {
    @NotBlank(message = "Mã phiếu đặt sân không được để trống")
    private String bookingId;

    private String currentCourtId;
    private String currentCourtName;
    private LocalDate currentDate;
    private LocalTime currentStartTime;
    private LocalTime currentEndTime;
    private BigDecimal currentTotalAmount;

    @NotBlank(message = "Vui lòng chọn sân mới")
    private String newCourtId;
    private String newCourtName;

    @NotNull(message = "Vui lòng chọn ngày mới")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate newBookingDate;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu mới")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime newStartTime;

    @NotNull(message = "Vui lòng chọn giờ kết thúc mới")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime newEndTime;

    // Chênh lệch tiền (Dương: thu thêm, Âm: hoàn trả, 0: không đổi)
    @Builder.Default
    private BigDecimal priceDifference = BigDecimal.ZERO;

    private String reason;

    public ChangeScheduleDTO() {
        this.priceDifference = BigDecimal.ZERO;
    }

    public ChangeScheduleDTO(String bookingId, String currentCourtId, String currentCourtName,
                             LocalDate currentDate, LocalTime currentStartTime, LocalTime currentEndTime,
                             BigDecimal currentTotalAmount, String newCourtId, String newCourtName,
                             LocalDate newBookingDate, LocalTime newStartTime, LocalTime newEndTime,
                             BigDecimal priceDifference, String reason) {
        this.bookingId = bookingId;
        this.currentCourtId = currentCourtId;
        this.currentCourtName = currentCourtName;
        this.currentDate = currentDate;
        this.currentStartTime = currentStartTime;
        this.currentEndTime = currentEndTime;
        this.currentTotalAmount = currentTotalAmount;
        this.newCourtId = newCourtId;
        this.newCourtName = newCourtName;
        this.newBookingDate = newBookingDate;
        this.newStartTime = newStartTime;
        this.newEndTime = newEndTime;
        this.priceDifference = priceDifference != null ? priceDifference : BigDecimal.ZERO;
        this.reason = reason;
    }
    public static class ChangeScheduleDTOBuilder {
        public ChangeScheduleDTOBuilder() {
        }
    }
}
