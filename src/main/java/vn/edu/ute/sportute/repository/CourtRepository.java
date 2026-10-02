package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.Court;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, String> {

    // Tìm danh sách sân theo mã môn thể thao
    List<Court> findBySport_Id(String sportId);

    // Tìm danh sách sân theo trạng thái (Hoạt động, Bảo trì, ...)
    List<Court> findByStatus(String status);

    // Tìm sân theo môn và trạng thái
    List<Court> findBySport_IdAndStatus(String sportId, String status);

    // Kiểm tra trùng tên sân trong cùng một môn thể thao
    boolean existsByNameAndSport_Id(String name, String sportId);
}