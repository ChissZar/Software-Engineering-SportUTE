package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHITIETDICHVU")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingServiceDetail {

    @Id
    @Column(name = "MaCTDV", length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaPhieu", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MaDV", nullable = false)
    private ServiceItem serviceItem;

    @Column(name = "SoLuong", nullable = false)
    private Integer quantity;

    @Column(name = "DonGia", precision = 18, scale = 0, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "GhiChu", length = 200)
    private String note;

    @Column(name = "ThoiGian", nullable = false)
    private LocalDateTime orderTime;

    public BigDecimal getSubtotal() {
        if (unitPrice == null || quantity == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
