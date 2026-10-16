package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import java.util.List;

/**
 * Hợp đồng nghiệp vụ quản lý phiếu đặt sân toàn hệ thống.
 */
public interface BookingService {
    List<AdminBookingDTO> getAllBookings();
    AdminBookingDTO getBookingById(String id);
}
