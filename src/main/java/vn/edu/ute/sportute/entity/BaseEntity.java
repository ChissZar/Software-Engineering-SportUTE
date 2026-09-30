package vn.edu.ute.sportute.entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
/** Mau ID ky thuat; ma nghiep vu, quan he va audit do nhom bo sung. */
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() { return id; }
    protected void setId(Long id) { this.id = id; }
}
