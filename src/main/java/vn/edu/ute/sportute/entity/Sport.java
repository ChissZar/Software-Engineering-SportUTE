package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MONTHETHAO")
@Getter
@Setter
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

    public Sport() {
    }

    public Sport(String id, String name, String description, String status, List<Court> courts) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status != null ? status : "Hoạt động";
        this.courts = courts != null ? courts : new ArrayList<>();
    }
    public static class SportBuilder {
        public SportBuilder() {
        }
    }
}
