package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PHIEUDATSAN")
@Getter
@Setter
@Builder
public class Booking {

    @Id
    @Column(name = "MaPhieu", length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKH", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNV")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKM")
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaCS")
    private BookingPolicy bookingPolicy;

    @Column(name = "TrangThai", length = 30)
    private String status;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingDetail> bookingDetails = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingServiceDetail> serviceDetails = new ArrayList<>();

    public Booking() {
    }

    public Booking(String id, Customer customer, Employee employee, Promotion promotion, BookingPolicy bookingPolicy, String status, List<BookingDetail> bookingDetails, List<Payment> payments, List<BookingServiceDetail> serviceDetails) {
        this.id = id;
        this.customer = customer;
        this.employee = employee;
        this.promotion = promotion;
        this.bookingPolicy = bookingPolicy;
        this.status = status;
        this.bookingDetails = bookingDetails != null ? bookingDetails : new ArrayList<>();
        this.payments = payments != null ? payments : new ArrayList<>();
        this.serviceDetails = serviceDetails != null ? serviceDetails : new ArrayList<>();
    }

    public Booking(String id, Customer customer, Employee employee, Promotion promotion, BookingPolicy bookingPolicy, String status, List<BookingDetail> bookingDetails, List<Payment> payments) {
        this(id, customer, employee, promotion, bookingPolicy, status, bookingDetails, payments, new ArrayList<>());
    }
    public static class BookingBuilder {
        public BookingBuilder() {
        }
    }
}
