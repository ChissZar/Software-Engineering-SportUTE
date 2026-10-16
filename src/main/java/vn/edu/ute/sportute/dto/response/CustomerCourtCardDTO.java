package vn.edu.ute.sportute.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
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
    private String imageUrl;
    private String spec;
    private String extraAmenity;
    private String operatingHours;

    public CustomerCourtCardDTO() {
    }

    public CustomerCourtCardDTO(String id, String name, String sportId, String sportName, String location,
                                String status, BigDecimal pricePerHour, List<String> amenities,
                                List<String> timeSlots, String colorHeaderClass,
                                String imageUrl, String spec, String extraAmenity, String operatingHours) {
        this.id = id;
        this.name = name;
        this.sportId = sportId;
        this.sportName = sportName;
        this.location = location;
        this.status = status;
        this.pricePerHour = pricePerHour;
        this.amenities = amenities;
        this.timeSlots = timeSlots;
        this.colorHeaderClass = colorHeaderClass;
        this.imageUrl = imageUrl;
        this.spec = spec;
        this.extraAmenity = extraAmenity;
        this.operatingHours = operatingHours;
    }

    public static class CustomerCourtCardDTOBuilder {
        public CustomerCourtCardDTOBuilder() {
        }
    }
}
