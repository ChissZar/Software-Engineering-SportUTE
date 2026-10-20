package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@lombok.AllArgsConstructor
public class PromotionDTO {

    @Builder.Default
    private String discountType = "AMOUNT";
    private String id;

    @NotBlank(message = "Vui lòng nhập tên chương trình khuyến mãi")
    private String name;

    @NotNull(message = "Vui lòng nhập số tiền giảm")
    private BigDecimal discountAmount;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

    @Builder.Default
    private String status = "Đang áp dụng"; // Đang áp dụng, Sắp diễn ra, Đã kết thúc, Ngừng áp dụng

    public PromotionDTO() {
    }

    public PromotionDTO(String id, String name, BigDecimal discountAmount, LocalDate fromDate, LocalDate toDate, String status) {
        this.id = id;
        this.name = name;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.status = status != null ? status : "Đang áp dụng";
    }
    public static class PromotionDTOBuilder {
        public PromotionDTOBuilder() {
        }
    }
}
