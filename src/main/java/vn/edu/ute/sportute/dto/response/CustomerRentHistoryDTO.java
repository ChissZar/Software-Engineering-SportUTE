package vn.edu.ute.sportute.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CustomerRentHistoryDTO {
    private String bookingId;
    private String courtName;
    private String sportName;
    private LocalDate bookingDate;
    private String timeSlot;
    private BigDecimal totalAmount;
    private String status;

    public CustomerRentHistoryDTO() {
    }

    public CustomerRentHistoryDTO(String bookingId, String courtName, String sportName, LocalDate bookingDate, String timeSlot, BigDecimal totalAmount, String status) {
        this.bookingId = bookingId;
        this.courtName = courtName;
        this.sportName = sportName;
        this.bookingDate = bookingDate;
        this.timeSlot = timeSlot;
        this.totalAmount = totalAmount;
        this.status = status;
    }
    public static class CustomerRentHistoryDTOBuilder {
        public CustomerRentHistoryDTOBuilder() {
        }
    }
}
