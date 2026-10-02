package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDTO {
    private String id;

    @NotNull(message = "Vui lòng chọn sân áp dụng")
    private String courtId;
    private String courtName;

    @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

    @Builder.Default
    private List<DetailDTO> details = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailDTO {
        private String id;
        private String timeSlot; // Ví dụ: 05:00 - 17:00, 17:00 - 23:00
        private BigDecimal priceAmount;
    }
}