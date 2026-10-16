package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "LICHBAOTRI")
@Getter
@Setter
@Builder
public class Maintenance {

    @Id
    @Column(name = "MaBaoTri", length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSan", nullable = false)
    private Court court;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNV", nullable = false)
    private Employee employee;

    @Column(name = "NgayBD", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "NgayKT", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "LyDo", length = 200)
    private String reason;

    public Maintenance() {
    }

    public Maintenance(String id, Court court, Employee employee, LocalDateTime startDate, LocalDateTime endDate, String reason) {
        this.id = id;
        this.court = court;
        this.employee = employee;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }
    public static class MaintenanceBuilder {
        public MaintenanceBuilder() {
        }
    }
}
