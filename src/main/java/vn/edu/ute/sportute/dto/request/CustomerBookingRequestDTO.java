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
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBookingRequestDTO {
    @NotBlank(message = "Vui lòng chọn sân")
    private String courtId;

    @NotNull(message = "Vui lòng chọn ngày thuê")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu")
    @DateTimeFormat(pattern = "HH:mm")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @Builder.Default
    private int durationMinutes = 120;

    private BigDecimal totalAmount;

    private String note;

    @Builder.Default
    private String paymentMethod = "Chuyển khoản VietQR";

    @Builder.Default
    private String depositChoice = "DEPOSIT_30";

    private String voucherCode;

    private BigDecimal discountAmount;

    // Tính năng ghép đối thủ giao lưu
    @Builder.Default
    private boolean enableMatchmaking = false;

    private String matchmakingSkillLevel;

    private String matchmakingTeamSize;

    private String matchmakingMessage;
}
