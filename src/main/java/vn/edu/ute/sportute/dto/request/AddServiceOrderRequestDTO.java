package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddServiceOrderRequestDTO {

    @NotBlank(message = "Mã phiếu không được để trống")
    private String bookingId;

    @NotBlank(message = "Vui lòng chọn dịch vụ hoặc sản phẩm")
    private String serviceId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng ít nhất là 1")
    private Integer quantity;

    private String note;
}
