package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "VAITRO")
@Getter
@Setter
@Builder
public class Role {

    @Id
    @Column(name = "MaVaiTro", length = 10, nullable = false)
    private String id;

    @Column(name = "TenVaiTro", length = 50, nullable = false, unique = true)
    private String name;

    @Column(name = "MoTa", length = 200)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "VAITROCHUCNANG",
        joinColumns = @JoinColumn(name = "MaVaiTro"),
        inverseJoinColumns = @JoinColumn(name = "MaCN")
    )
    @Builder.Default
    private Set<Feature> features = new HashSet<>();

    public Role() {
    }

    public Role(String id, String name, String description, Set<Feature> features) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.features = features != null ? features : new HashSet<>();
    }
    public static class RoleBuilder {
        public RoleBuilder() {
        }
    }
}
