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
public class AdminBookingDTO {
    // Mã đặt sân (Chỉ đọc)
    private String id;

    // Khách hàng
    @NotBlank(message = "Vui lòng chọn khách hàng")
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // Sân và môn
    @NotBlank(message = "Vui lòng chọn sân")
    private String courtId;
    private String courtName;
    private String sportId;
    private String sportName;

    // Thời gian
    @NotNull(message = "Vui lòng chọn ngày đặt")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Vui lòng chọn giờ kết thúc")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    // Tài chính và tiền tệ
    @NotNull(message = "Vui lòng nhập tổng tiền")
    private BigDecimal totalAmount;

    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal serviceAmount = BigDecimal.ZERO;
    private String serviceSummary;

    // Chính sách & Khuyến mãi
    private String policyId;
    private String policyName;
    private BigDecimal depositRate;
    private BigDecimal cancellationFeeRate;

    private String promotionId;
    private String promotionName;
    private BigDecimal discountAmount;

    // Trạng thái: Chờ xác nhận, Đã xác nhận, Đang sử dụng, Hoàn tất, Đã hủy
    @NotBlank(message = "Vui lòng chọn trạng thái")
    private String status;

    private String note;

    public AdminBookingDTO() {
        this.depositAmount = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
        this.remainingAmount = BigDecimal.ZERO;
        this.serviceAmount = BigDecimal.ZERO;
    }

    public AdminBookingDTO(String id, String customerId, String customerName, String customerPhone, String customerEmail,
                           String courtId, String courtName, String sportId, String sportName,
                           LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                           BigDecimal totalAmount, BigDecimal depositAmount, BigDecimal paidAmount, BigDecimal remainingAmount,
                           BigDecimal serviceAmount, String serviceSummary,
                           String policyId, String policyName, BigDecimal depositRate, BigDecimal cancellationFeeRate,
                           String promotionId, String promotionName, BigDecimal discountAmount,
                           String status, String note) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail;
        this.courtId = courtId;
        this.courtName = courtName;
        this.sportId = sportId;
        this.sportName = sportName;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalAmount = totalAmount;
        this.depositAmount = depositAmount != null ? depositAmount : BigDecimal.ZERO;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.remainingAmount = remainingAmount != null ? remainingAmount : BigDecimal.ZERO;
        this.serviceAmount = serviceAmount != null ? serviceAmount : BigDecimal.ZERO;
        this.serviceSummary = serviceSummary;
        this.policyId = policyId;
        this.policyName = policyName;
        this.depositRate = depositRate;
        this.cancellationFeeRate = cancellationFeeRate;
        this.promotionId = promotionId;
        this.promotionName = promotionName;
        this.discountAmount = discountAmount;
        this.status = status;
        this.note = note;
    }

    public AdminBookingDTO(String id, String customerId, String customerName, String customerPhone, String customerEmail,
                           String courtId, String courtName, String sportId, String sportName,
                           LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                           BigDecimal totalAmount, BigDecimal depositAmount, BigDecimal paidAmount, BigDecimal remainingAmount,
                           String policyId, String policyName, BigDecimal depositRate, BigDecimal cancellationFeeRate,
                           String promotionId, String promotionName, BigDecimal discountAmount,
                           String status, String note) {
        this(id, customerId, customerName, customerPhone, customerEmail,
             courtId, courtName, sportId, sportName,
             bookingDate, startTime, endTime,
             totalAmount, depositAmount, paidAmount, remainingAmount,
             BigDecimal.ZERO, null,
             policyId, policyName, depositRate, cancellationFeeRate,
             promotionId, promotionName, discountAmount,
             status, note);
    }

    public static class AdminBookingDTOBuilder {
        public AdminBookingDTOBuilder() {
        }
    }
}
