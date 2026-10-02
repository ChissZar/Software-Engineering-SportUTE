package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CustomerBookingRequestDTO {
    @NotBlank(message = "Vui lòng chọn sân")
    private String courtId;

    @NotNull(message = "Vui lòng chọn ngày thuê")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    private int durationMinutes = 120;

    private BigDecimal totalAmount;

    private String note;
}