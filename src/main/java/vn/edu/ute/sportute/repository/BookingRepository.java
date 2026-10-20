package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.Booking;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from Booking b where b.id = :id")
    java.util.Optional<Booking> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer", "customer.account"})
    @org.springframework.data.jpa.repository.Query("select b from Booking b where b.id = :id")
    java.util.Optional<Booking> findForMatch(@org.springframework.data.repository.query.Param("id") String id);
    List<Booking> findByCustomer_Id(String customerId);
    List<Booking> findByStatus(String status);
    List<Booking> findByEmployee_Id(String employeeId);
}