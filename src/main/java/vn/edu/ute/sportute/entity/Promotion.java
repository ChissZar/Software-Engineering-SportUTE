package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "KHUYENMAI")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

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
}