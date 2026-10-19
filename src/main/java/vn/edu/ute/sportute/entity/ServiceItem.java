package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "DICHVU")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItem {

    @Id
    @Column(name = "MaDV", length = 10, nullable = false)
    private String id;

    @Column(name = "TenDV", length = 100, nullable = false)
    private String name;

    @Column(name = "LoaiDV", length = 50, nullable = false)
    private String category; // 'Thức uống', 'Đồ ăn nhẹ', 'Thuê dụng cụ', 'Phụ kiện'

    @Column(name = "DonViTinh", length = 20, nullable = false)
    private String unit; // 'Chai', 'Lon', 'Cái', 'Đôi', 'Giờ'

    @Column(name = "DonGia", precision = 18, scale = 0, nullable = false)
    private BigDecimal price;

    @Column(name = "TonKho", nullable = false)
    private Integer stock;

    @Column(name = "HinhAnh", length = 255)
    private String imageUrl;

    @Column(name = "TrangThai", length = 20, nullable = false)
    private String status; // 'Đang bán', 'Tạm ngưng'
}
