package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.Court;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, String> {
    @org.springframework.data.jpa.repository.Query("select count(d) > 0 from BookingDetail d where d.court.id = :id and d.booking.status = :status and d.startTime <= :now and d.endTime > :now")
    boolean hasCurrentUsage(@org.springframework.data.repository.query.Param("id") String id,
                            @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,
                            @org.springframework.data.repository.query.Param("status") String status);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Court c where c.id = :id")
    java.util.Optional<Court> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);

    // Tìm danh sách sân theo mã môn thể thao
    List<Court> findBySport_Id(String sportId);

    // Tìm danh sách sân theo trạng thái (Hoạt động, Bảo trì, ...)
    List<Court> findByStatus(String status);

    // Tìm sân theo môn và trạng thái
    List<Court> findBySport_IdAndStatus(String sportId, String status);

    // Kiểm tra trùng tên sân trong cùng một môn thể thao
    boolean existsByNameAndSport_Id(String name, String sportId);
}
