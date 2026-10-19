package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.Price;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceRepository extends JpaRepository<Price, String> {
    List<Price> findByCourt_Id(String courtId);

    // Lấy danh sách bảng giá đang có hiệu lực tại một ngày cụ thể (ưu tiên bảng giá mới nhất theo ngày bắt đầu)
    @Query("SELECT p FROM Price p WHERE p.court.id = :courtId " +
           "AND p.fromDate <= :targetDate " +
           "AND (p.toDate IS NULL OR p.toDate >= :targetDate) " +
           "ORDER BY p.fromDate DESC")
    List<Price> findEffectivePricesList(@Param("courtId") String courtId,
                                        @Param("targetDate") LocalDate targetDate);

    default Optional<Price> findEffectivePrice(String courtId, LocalDate targetDate) {
        List<Price> prices = findEffectivePricesList(courtId, targetDate);
        return prices.isEmpty() ? Optional.empty() : Optional.of(prices.get(0));
    }
}