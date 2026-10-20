package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.*;
import vn.edu.ute.sportute.dto.response.*;
import vn.edu.ute.sportute.entity.Customer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminBookingService {
    // 1. Quản lý danh sách & xem chi tiết phiếu đặt sân
    List<AdminBookingDTO> getBookings(String keyword, String status);
    AdminBookingDTO getBookingById(String id);
    
    // 2. Lập phiếu đặt sân tại quầy (UC17)
    void saveBooking(AdminBookingDTO dto, String employeeAccountId);
    Customer findOrCreateCustomer(String phone, String fullName, String email, String address);

    // 3. Duyệt & xác nhận đặt sân
    void confirmBooking(String id);

    // 4. Nhận sân (UC21 - Check-in)
    void checkInBooking(String id);

    // 5. Trả sân (UC22 - Check-out)
    void checkOutBooking(String id);
    void checkOutBooking(String id, String paymentMethod, String note);

    // 6. Gia hạn sân (UC23)
    void extendBooking(ExtendBookingDTO dto);

    // 7. Đổi sân hoặc giờ thuê (UC19)
    void changeSchedule(ChangeScheduleDTO dto);

    // 8. Hủy phiếu đặt sân theo chính sách hoàn tiền (UC20)
    void cancelBooking(String id);
    void cancelBookingWithPolicy(CancelBookingDTO dto);

    // 9. Lịch sân thể thao trực quan dạng timeline (UC18)
    List<CourtTimelineDTO> getTimeline(LocalDate date, String sportId);

    // 10. Tự động tính giá và cọc theo bảng giá, khuyến mãi, chính sách
    PriceEstimateResponseDTO calculateEstimate(PriceEstimateRequestDTO req);

    // 11. Báo cáo thống kê
    Map<String, Object> getBookingStatistics();

    // 12. Bán lẻ F&B và dịch vụ tại quầy (Không cần đặt sân)
    AdminBookingDTO createRetailOrder(RetailOrderRequestDTO req, String employeeAccountId);
}