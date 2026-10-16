package vn.edu.ute.sportute.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class PriceEstimateResponseDTO {
    private boolean valid;
    private String message;

    private BigDecimal pricePerHour;
    private long durationMinutes;
    private BigDecimal rawAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    private String policyId;
    private String policyName;
    private BigDecimal depositRate;
    private BigDecimal depositAmount;

    public PriceEstimateResponseDTO() {
    }

    public PriceEstimateResponseDTO(boolean valid, String message, BigDecimal pricePerHour, long durationMinutes,
                                    BigDecimal rawAmount, BigDecimal discountAmount, BigDecimal totalAmount,
                                    String policyId, String policyName, BigDecimal depositRate, BigDecimal depositAmount) {
        this.valid = valid;
        this.message = message;
        this.pricePerHour = pricePerHour;
        this.durationMinutes = durationMinutes;
        this.rawAmount = rawAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.policyId = policyId;
        this.policyName = policyName;
        this.depositRate = depositRate;
        this.depositAmount = depositAmount;
    }
    public static class PriceEstimateResponseDTOBuilder {
        public PriceEstimateResponseDTOBuilder() {
        }
    }
}
