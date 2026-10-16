package vn.edu.ute.sportute.dto.request;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class PriceEstimateRequestDTO {
    public PriceEstimateRequestDTO() {}

    public PriceEstimateRequestDTO(String courtId, LocalDate date, LocalTime startTime, LocalTime endTime, String promotionCode, String policyId) {
        this.courtId = courtId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.promotionCode = promotionCode;
        this.policyId = policyId;
    }
    private String courtId;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String promotionCode;
    private String policyId;
    public static class PriceEstimateRequestDTOBuilder {
        public PriceEstimateRequestDTOBuilder() {
        }
    }
}
