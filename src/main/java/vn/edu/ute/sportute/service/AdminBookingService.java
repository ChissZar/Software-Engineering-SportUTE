package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import java.util.List;
import java.util.Map;

public interface AdminBookingService {
    List<AdminBookingDTO> getBookings(String keyword, String status);
    AdminBookingDTO getBookingById(String id);
    void saveBooking(AdminBookingDTO dto, String employeeAccountId);
    void confirmBooking(String id);
    void cancelBooking(String id);
    Map<String, Object> getBookingStatistics();
}