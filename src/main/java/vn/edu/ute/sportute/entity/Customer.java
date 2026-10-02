package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "KHACHHANG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "MaKH", length = 10, nullable = false)
    private String id;

    @Column(name = "HoTen", length = 100, nullable = false)
    private String fullName;

    @Column(name = "SDT", length = 15, nullable = false, unique = true)
    private String phone;

    @Column(name = "Email", length = 100, unique = true)
    private String email;

    @Column(name = "DiaChi", length = 200)
    private String address;

    @Column(name = "TrangThai", length = 20, nullable = false)
    @Builder.Default
    private String status = "Hoạt động";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTK")
    private Account account;
}