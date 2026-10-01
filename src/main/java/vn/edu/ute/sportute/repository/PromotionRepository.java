package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.Promotion;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, String> {
    
    // Tìm các chương trình khuyến mãi còn hiệu lực
    @Query("SELECT p FROM Promotion p WHERE " +
           "(p.fromDate IS NULL OR p.fromDate <= :today) AND " +
           "(p.toDate IS NULL OR p.toDate >= :today)")
    List<Promotion> findActivePromotions(@Param("today") LocalDate today);
}