package vn.edu.ute.sportute.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CustomerCourtCardDTO {
    private String id;
    private String name;
    private String sportId;
    private String sportName;
    private String location;
    private String status;
    private BigDecimal pricePerHour;
    private List<String> amenities; // Tiện ích: Cỏ FIFA, Đèn LED, Có mái che...
    private List<String> timeSlots; // Các khung giờ gợi ý
    private String colorHeaderClass; // Màu gradient theo từng môn
}