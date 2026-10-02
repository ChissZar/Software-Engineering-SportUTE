package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MONTHETHAO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sport {

    @Id
    @Column(name = "MaMon", length = 10, nullable = false)
    private String id;

    @Column(name = "TenMon", length = 50, nullable = false, unique = true)
    private String name;

    @Column(name = "MoTa", length = 200)
    private String description;

    @Column(name = "TrangThai", length = 20, nullable = false)
    @Builder.Default
    private String status = "Hoạt động";

    @OneToMany(mappedBy = "sport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Court> courts = new ArrayList<>();
}