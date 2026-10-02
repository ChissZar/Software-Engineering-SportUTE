package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "NHANVIEN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @Column(name = "MaNV", length = 10, nullable = false)
    private String id;

    @Column(name = "HoTen", length = 100, nullable = false)
    private String fullName;

    @Column(name = "SDT", length = 15, nullable = false, unique = true)
    private String phone;

    @Column(name = "Email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "ChucVu", length = 50)
    private String position;

    @Column(name = "TrangThai", length = 20, nullable = false)
    @Builder.Default
    private String status = "Đang làm";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTK")
    private Account account;
}