package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.CourtDTO;
import java.util.List;
import java.util.Map;

public interface CourtService {
    List<CourtDTO> getAllCourts();
    CourtDTO getCourtById(String id);
    void saveCourt(CourtDTO dto);
    void toggleCourtStatus(String id);
    Map<String, Long> getCourtStatistics();
}