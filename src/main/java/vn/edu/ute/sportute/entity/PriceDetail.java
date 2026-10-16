package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "CHITIETBANGGIA")
@Getter
@Setter
@Builder
public class PriceDetail {

    @Id
    @Column(name = "MaCTGia", length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaBangGia", nullable = false)
    private Price price;

    @Column(name = "KhungGio", length = 50)
    private String timeSlot;

    @Column(name = "DonGia", precision = 18, scale = 0, nullable = false)
    @Builder.Default
    private BigDecimal priceAmount = BigDecimal.ZERO;

    public PriceDetail() {
    }

    public PriceDetail(String id, Price price, String timeSlot, BigDecimal priceAmount) {
        this.id = id;
        this.price = price;
        this.timeSlot = timeSlot;
        this.priceAmount = priceAmount != null ? priceAmount : BigDecimal.ZERO;
    }
    public static class PriceDetailBuilder {
        public PriceDetailBuilder() {
        }
    }
}
