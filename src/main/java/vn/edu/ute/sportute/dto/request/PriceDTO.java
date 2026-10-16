package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
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

    public PriceDTO() {
        this.details = new ArrayList<>();
    }

    public PriceDTO(String id, String courtId, String courtName, LocalDate fromDate, LocalDate toDate, List<DetailDTO> details) {
        this.id = id;
        this.courtId = courtId;
        this.courtName = courtName;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.details = details != null ? details : new ArrayList<>();
    }

    public static class PriceDTOBuilder {
        public PriceDTOBuilder() {
        }
    }

    @Getter
    @Setter
    @Builder
    public static class DetailDTO {
        private String id;
        private String timeSlot;
        private BigDecimal priceAmount;

        public DetailDTO() {
        }

        public DetailDTO(String id, String timeSlot, BigDecimal priceAmount) {
            this.id = id;
            this.timeSlot = timeSlot;
            this.priceAmount = priceAmount;
        }

        public static class DetailDTOBuilder {
            public DetailDTOBuilder() {
            }
        }
    }
}
