package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.BookingPolicy;

@Repository
public interface BookingPolicyRepository extends JpaRepository<BookingPolicy, String> {
}