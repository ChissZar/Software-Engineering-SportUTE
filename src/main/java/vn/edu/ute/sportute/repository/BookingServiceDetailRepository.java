package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.BookingServiceDetail;

import java.util.List;

@Repository
public interface BookingServiceDetailRepository extends JpaRepository<BookingServiceDetail, String> {
    List<BookingServiceDetail> findByBooking_Id(String bookingId);
    boolean existsByServiceItem_Id(String serviceItemId);
}
