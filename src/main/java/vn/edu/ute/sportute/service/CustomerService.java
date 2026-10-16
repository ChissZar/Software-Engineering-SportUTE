package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.CustomerDTO;
import vn.edu.ute.sportute.dto.response.CustomerRentHistoryDTO;

import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ quản lý khách hàng (UC30 - UC34).
 */
public interface CustomerService {

    List<CustomerDTO> getAllCustomers(String keyword, String tier, String status);

    CustomerDTO getCustomerById(String id);

    CustomerDTO saveCustomer(CustomerDTO dto);

    void toggleCustomerStatus(String id);

    List<CustomerRentHistoryDTO> getCustomerRentHistory(String customerId);

    Map<String, Object> getCustomerStatistics();
}
