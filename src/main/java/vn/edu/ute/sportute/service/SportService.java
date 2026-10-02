package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.SportDTO;
import java.util.List;

public interface SportService {
    List<SportDTO> getAllSports();
    SportDTO getSportById(String id);
    void saveSport(SportDTO dto);
    void toggleStatus(String id);
}