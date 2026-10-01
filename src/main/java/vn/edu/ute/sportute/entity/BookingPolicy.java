package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "CHINHSACH")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPolicy {

    @Id
    @Column(name = "MaCS", length = 10, nullable = false)
    private String id;

    @Column(name = "TenCS", length = 100, nullable = false)
    private String name;

    @Column(name = "TyLeCoc", precision = 5, scale = 2)
    private BigDecimal depositRate;

    @Column(name = "TyLeHuy", precision = 5, scale = 2)
    private BigDecimal cancellationFeeRate;
}