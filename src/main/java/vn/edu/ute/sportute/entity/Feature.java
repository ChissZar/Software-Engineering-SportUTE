package vn.edu.ute.sportute.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHUCNANG")
@Getter
@Setter
@Builder
public class Feature {

    @Id
    @Column(name = "MaCN", length = 10, nullable = false)
    private String id;

    @Column(name = "TenCN", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "MoTa", length = 200)
    private String description;

    public Feature() {
    }

    public Feature(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
    public static class FeatureBuilder {
        public FeatureBuilder() {
        }
    }
}
