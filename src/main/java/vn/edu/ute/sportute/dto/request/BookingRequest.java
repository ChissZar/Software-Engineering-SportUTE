package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class BookingRequest {
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

    // 12. Giờ kết thúc
    @NotNull(message = "Vui lòng chọn giờ kết thúc")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    // 13. Tổng tiền (đ)
    @NotNull(message = "Vui lòng nhập tổng tiền")
    private BigDecimal totalAmount;

    // 14. Trạng thái
    @NotBlank(message = "Vui lòng chọn trạng thái")
    private String status;

    public BookingRequest() {
    }

    public BookingRequest(String id, String customerId, String customerName, String customerPhone,
                          String courtId, String courtName, LocalDate bookingDate,
                          LocalTime startTime, LocalTime endTime, BigDecimal totalAmount, String status) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.courtId = courtId;
        this.courtName = courtName;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalAmount = totalAmount;
        this.status = status;
    }
    public static class BookingRequestBuilder {
        public BookingRequestBuilder() {
        }
    }
}
