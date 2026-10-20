package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "KHUYENMAI")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Promotion {

    @Column(name = "DiscountType", length = 10)
    @Builder.Default
    private String discountType = "AMOUNT";

    @Id
    @Column(name = "MaKM", length = 10, nullable = false)
    private String id;

    @Column(name = "TenKM", length = 100, nullable = false)
    private String name;

    @Column(name = "GiaTriGiam", precision = 18, scale = 0)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "TuNgay")
    private LocalDate fromDate;

    @Column(name = "DenNgay")
    private LocalDate toDate;

    public Promotion() {
    }

    public Promotion(String id, String name, BigDecimal discountAmount, LocalDate fromDate, LocalDate toDate) {
        this.id = id;
        this.name = name;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }
    public static class PromotionBuilder {
        public PromotionBuilder() {
        }
    }
}
