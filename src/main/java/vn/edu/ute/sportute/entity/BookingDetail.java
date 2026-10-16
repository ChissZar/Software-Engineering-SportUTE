package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CHITIETDATSAN")
@Getter
@Setter
@Builder
public class BookingDetail {

    @Id
    @Column(name = "MaCT", length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaPhieu", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSan", nullable = false)
    private Court court;

    @Column(name = "GioBD", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "GioKT", nullable = false)
    private LocalDateTime endTime;

    public BookingDetail() {
    }

    public BookingDetail(String id, Booking booking, Court court, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.booking = booking;
        this.court = court;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public static class BookingDetailBuilder {
        public BookingDetailBuilder() {
        }
    }
}
