package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SAN")
@Getter
@Setter
@Builder
public class Court {

    @Id
    @Column(name = "MaSan", length = 10, nullable = false)
    private String id;

    @Column(name = "TenSan", length = 50, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaMon", nullable = false)
    private Sport sport;

    @Column(name = "ViTri", length = 100)
    private String location;

    @Column(name = "TrangThai", length = 20, nullable = false)
    @Builder.Default
    private String status = "Hoạt động";

    public Court() {
    }

    public Court(String id, String name, Sport sport, String location, String status) {
        this.id = id;
        this.name = name;
        this.sport = sport;
        this.location = location;
        this.status = status != null ? status : "Hoạt động";
    }
    public static class CourtBuilder {
        public CourtBuilder() {
        }
    }
}
