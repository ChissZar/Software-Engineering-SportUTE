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
public class CustomerBookingRequestDTO {
    @NotBlank(message = "Vui lòng chọn sân")
    private String courtId;

    @NotNull(message = "Vui lòng chọn ngày thuê")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @Builder.Default
    private int durationMinutes = 120;

    private BigDecimal totalAmount;

    private String note;

    public CustomerBookingRequestDTO() {
        this.durationMinutes = 120;
    }

    public CustomerBookingRequestDTO(String courtId, LocalDate bookingDate, LocalTime startTime,
                                     int durationMinutes, BigDecimal totalAmount, String note) {
        this.courtId = courtId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.totalAmount = totalAmount;
        this.note = note;
    }
    public static class CustomerBookingRequestDTOBuilder {
        public CustomerBookingRequestDTOBuilder() {
        }
    }
}
