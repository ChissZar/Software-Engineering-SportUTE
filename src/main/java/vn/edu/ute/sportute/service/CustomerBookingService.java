package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import vn.edu.ute.sportute.dto.request.CustomerBookingRequestDTO;
import vn.edu.ute.sportute.dto.request.CustomerCourtFilterDTO;
import vn.edu.ute.sportute.dto.response.CustomerCourtCardDTO;
import vn.edu.ute.sportute.entity.Booking;

import java.util.List;

public interface CustomerBookingService {
    // 1. Xem và tìm sân trống trực tuyến (UC40)
    List<CustomerCourtCardDTO> searchAvailableCourts(CustomerCourtFilterDTO filter);

    // 2. Đặt sân trực tuyến (UC41)
    Booking bookCourtOnline(CustomerBookingRequestDTO request, String accountUsernameOrEmail);

    // 3. Xem lịch sử đặt sân của mình (UC42)
    List<AdminBookingDTO> getCustomerBookings(String accountUsernameOrEmail, String status, String keyword);
    AdminBookingDTO getCustomerBookingDetail(String bookingId, String accountUsernameOrEmail);

    // 4. Khách hàng tự hủy phiếu đặt sân của mình theo chính sách (UC43)
    void cancelBookingByCustomer(String bookingId, String accountUsernameOrEmail, String reason);
}