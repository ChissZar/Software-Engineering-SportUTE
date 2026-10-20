package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "GIAODICH")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Payment {

    @Column(name = "CreatedAt", updatable = false)
    private java.time.LocalDateTime createdAt;
    @Column(name = "PaymentStatus", length = 30)
    @Builder.Default
    private String paymentStatus = "CONFIRMED";
    @PrePersist
    void timestamp() { if (createdAt == null) createdAt = java.time.LocalDateTime.now(); }

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

    public Payment() {
    }

    public Payment(String id, Booking booking, String transactionType, BigDecimal amount, String paymentMethod) {
        this.id = id;
        this.booking = booking;
        this.transactionType = transactionType;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.paymentMethod = paymentMethod;
    }
    public static class PaymentBuilder {
        public PaymentBuilder() {
        }
    }
}
