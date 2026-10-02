package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHUCNANG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feature {

    @Id
    @Column(name = "MaCN", length = 10, nullable = false)
    private String id;

    @Column(name = "TenCN", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "MoTa", length = 200)
    private String description;
}