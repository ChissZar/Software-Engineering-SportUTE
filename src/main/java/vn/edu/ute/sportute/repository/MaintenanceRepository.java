package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.Maintenance;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, String> {
    List<Maintenance> findByCourt_Id(String courtId);
    List<Maintenance> findByEmployee_Id(String employeeId);

    // Kiểm tra xem sân có đang trong thời gian bảo trì hay không
    @Query("SELECT COUNT(m) > 0 FROM Maintenance m " +
           "WHERE m.court.id = :courtId " +
           "AND m.startDate < :endTime AND m.endDate > :startTime")
    boolean existsOverlappingMaintenance(@Param("courtId") String courtId,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
}