package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.BookingDetail;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, String> {

    List<BookingDetail> findByBooking_Id(String bookingId);

    // 1. Kiểm tra trùng lịch khi TẠO MỚI (không có excludeBookingId)[cite: 3]
    @Query("SELECT COUNT(ct) > 0 FROM BookingDetail ct " +
           "WHERE ct.court.id = :courtId " +
           "AND ct.booking.status <> 'Đã hủy' " +
           "AND ct.startTime < :endTime AND ct.endTime > :startTime")
    boolean existsOverlappingBooking(@Param("courtId") String courtId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    // 2. Kiểm tra trùng lịch khi CẬP NHẬT (loại trừ chính mã phiếu đang chỉnh sửa)[cite: 3]
    @Query("SELECT COUNT(ct) > 0 FROM BookingDetail ct " +
           "WHERE ct.court.id = :courtId " +
           "AND ct.booking.status <> 'Đã hủy' " +
           "AND ct.booking.id <> :excludeBookingId " +
           "AND ct.startTime < :endTime AND ct.endTime > :startTime")
    boolean existsOverlappingBookingExceptSelf(@Param("courtId") String courtId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("excludeBookingId") String excludeBookingId);
}