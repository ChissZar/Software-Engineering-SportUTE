package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CustomerDTO {

    private String id;

    @NotBlank(message = "Vui lòng nhập họ và tên khách hàng")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    private String phone;

    private String email;

    private String address;

    @Builder.Default
    private String status = "Hoạt động";

    @Builder.Default
    private String tier = "Thường"; // VIP, Thân thiết, Thường

    @Builder.Default
    private int totalBookings = 0;

    public CustomerDTO() {
    }

    public CustomerDTO(String id, String fullName, String phone, String email, String address, String status, String tier, int totalBookings) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.status = status != null ? status : "Hoạt động";
        this.tier = tier != null ? tier : "Thường";
        this.totalBookings = totalBookings;
    }
    public static class CustomerDTOBuilder {
        public CustomerDTOBuilder() {
        }
    }
}
