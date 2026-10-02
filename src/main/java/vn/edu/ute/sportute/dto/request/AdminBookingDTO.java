package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingDTO {
    // 7. Mã đặt sân (Chỉ đọc)
    private String id;

    // 8. Khách hàng
    @NotBlank(message = "Vui lòng chọn khách hàng")
    private String customerId;
    private String customerName;
    private String customerPhone;

    // 9. Sân
    @NotBlank(message = "Vui lòng chọn sân")
    private String courtId;
    private String courtName;

    // 10. Ngày đặt
    @NotNull(message = "Vui lòng chọn ngày đặt")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    // 11. Giờ bắt đầu
    @NotNull(message = "Vui lòng chọn giờ bắt đầu")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    // 12. Giờ kết thúc[cite: 3]
    @NotNull(message = "Vui lòng chọn giờ kết thúc")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    // 13. Tổng tiền (đ)[cite: 3]
    @NotNull(message = "Vui lòng nhập tổng tiền")
    private BigDecimal totalAmount;

    // 14. Trạng thái[cite: 3]
    @NotBlank(message = "Vui lòng chọn trạng thái")
    private String status; // Chờ xác nhận, Đã xác nhận, Đang sử dụng, Hoàn thành, Đã hủy[cite: 3]
}