package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

// CHÚ Ý: Phải import Role từ package entity, KHÔNG import vn.edu.ute.sportute.enums.Role
import vn.edu.ute.sportute.entity.Role; 

@Entity
@Table(name = "TAIKHOAN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "MaTK", length = 10, nullable = false)
    private String id;

    @Column(name = "TenDangNhap", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "MatKhau", length = 255, nullable = false)
    private String password;

    @Column(name = "Email", length = 100, unique = true)
    private String email;

    @Column(name = "TrangThai", length = 20, nullable = false)
    @Builder.Default
    private String status = "Hoạt động";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "TAIKHOANVAITRO",
        joinColumns = @JoinColumn(name = "MaTK"),
        inverseJoinColumns = @JoinColumn(name = "MaVaiTro")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}