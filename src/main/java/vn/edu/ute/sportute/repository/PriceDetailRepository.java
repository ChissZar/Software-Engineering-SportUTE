package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.PriceDetail;

import java.util.List;

@Repository
public interface PriceDetailRepository extends JpaRepository<PriceDetail, String> {
    List<PriceDetail> findByPrice_Id(String priceId);
}