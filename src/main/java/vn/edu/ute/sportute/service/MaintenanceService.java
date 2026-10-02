package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.MaintenanceDTO;
import java.util.List;
import java.util.Map;

public interface MaintenanceService {
    List<MaintenanceDTO> getAllMaintenances();
    void createMaintenance(MaintenanceDTO dto, String employeeId);
    void deleteMaintenance(String id);
    Map<String, Long> getMaintenanceStatistics();
}