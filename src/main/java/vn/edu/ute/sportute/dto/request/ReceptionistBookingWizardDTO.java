package vn.edu.ute.sportute.dto.request;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionistBookingWizardDTO {
    // 1. Khách hàng
    private String customerId; // "NEW" hoặc ID khách đã có
    private String newCustomerName;
    private String newCustomerPhone;
    private String newCustomerEmail;
    private String newCustomerNote;

    // 2. Sân & Khung giờ
    private String courtId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // 3. Chính sách & Khuyến mãi
    private String policyId;
    private String promotionId;

    // 4. Tài chính
    private BigDecimal courtAmount;
    private BigDecimal serviceAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String paymentMethod; // "Tiền mặt", "VietQR", "Chưa thanh toán"

    // 5. Tùy chọn đặc thù Lễ tân
    private boolean autoCheckIn; // Nếu true, khách nhận sân ngay (trạng thái "Đang sử dụng")
    private boolean autoPrint;   // Tự động in biên nhận sau khi tạo

    // 6. Dịch vụ & Nước uống gọi kèm ban đầu
    private List<AddServiceOrderRequestDTO> services;
}
