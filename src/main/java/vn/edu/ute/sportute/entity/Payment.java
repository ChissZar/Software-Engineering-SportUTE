package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "GIAODICH")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @Column(name = "MaGD", length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaPhieu", nullable = false)
    private Booking booking;

    @Column(name = "LoaiGD", length = 20)
    private String transactionType;

    @Column(name = "SoTien", precision = 18, scale = 0, nullable = false)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "PhuongThuc", length = 30)
    private String paymentMethod;
}