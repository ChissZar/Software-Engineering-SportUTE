package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.EmployeeDTO;

import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ quản lý nhân sự (UC05 - UC08).
 */
public interface EmployeeService {

    List<EmployeeDTO> getAllEmployees(String keyword, String position, String status);

    EmployeeDTO getEmployeeById(String id);

    EmployeeDTO saveEmployee(EmployeeDTO dto);

    void toggleEmployeeStatus(String id);

    Map<String, Object> getEmployeeStatistics();
}
