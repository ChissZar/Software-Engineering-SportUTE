package vn.edu.ute.sportute.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PaymentDTO {
    private String id;
    private String bookingId;
    private String customerName;
    private String customerPhone;
    private String courtName;
    private String transactionType; // Cọc, Thu, Hoàn tiền
    private BigDecimal amount;
    private String paymentMethod;   // Tiền mặt, Chuyển khoản
    private String status;
    private LocalDateTime transactionDate;

    public PaymentDTO() {
    }

    public PaymentDTO(String id, String bookingId, String customerName, String customerPhone, String courtName,
                      String transactionType, BigDecimal amount, String paymentMethod, String status,
                      LocalDateTime transactionDate) {
        this.id = id;
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.courtName = courtName;
        this.transactionType = transactionType;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionDate = transactionDate;
    }
    public static class PaymentDTOBuilder {
        public PaymentDTOBuilder() {
        }
    }
}
