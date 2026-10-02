package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.Court;
import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, String> {
    List<Court> findBySport_Id(String sportId);
    List<Court> findByStatus(String status);
}