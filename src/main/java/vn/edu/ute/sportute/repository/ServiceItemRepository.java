package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.ServiceItem;

import java.util.List;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, String> {
    List<ServiceItem> findByCategory(String category);
    List<ServiceItem> findByStatus(String status);
    List<ServiceItem> findByNameContainingIgnoreCase(String name);
}
