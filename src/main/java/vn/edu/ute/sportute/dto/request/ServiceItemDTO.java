package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItemDTO {

    private String id;

    @NotBlank(message = "Tên dịch vụ/sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Vui lòng chọn loại dịch vụ")
    private String category; // 'Thức uống', 'Đồ ăn nhẹ', 'Thuê dụng cụ', 'Phụ kiện'

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit; // 'Chai', 'Lon', 'Cái', 'Đôi', 'Giờ'

    @NotNull(message = "Đơn giá không được để trống")
    @Min(value = 0, message = "Đơn giá không được âm")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stock;

    private String imageUrl;

    private String status; // 'Đang bán', 'Tạm ngưng'
}
