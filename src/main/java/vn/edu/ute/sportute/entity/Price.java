package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "BANGGIA", schema = "dbo") // Chỉ định rõ schema dbo
@Getter
@Setter
@Builder
public class Price {

    @Id
    @Column(name = "MaBangGia", length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSan", nullable = false)
    private Court court;

    @Column(name = "TuNgay", nullable = false)
    private LocalDate fromDate;

    @Column(name = "DenNgay")
    private LocalDate toDate;

    @OneToMany(mappedBy = "price", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PriceDetail> priceDetails = new ArrayList<>();

    public Price() {
    }

    public Price(String id, Court court, LocalDate fromDate, LocalDate toDate, List<PriceDetail> priceDetails) {
        this.id = id;
        this.court = court;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.priceDetails = priceDetails != null ? priceDetails : new ArrayList<>();
    }
    public static class PriceBuilder {
        public PriceBuilder() {
        }
    }
}
