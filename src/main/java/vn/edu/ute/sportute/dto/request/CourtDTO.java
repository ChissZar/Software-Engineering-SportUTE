package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtDTO {
    private String id;

    @NotBlank(message = "Tên sân không được để trống")
    @Size(max = 50, message = "Tên sân tối đa 50 ký tự")
    private String name;

    @NotBlank(message = "Vui lòng chọn môn thể thao")
    private String sportId;
    private String sportName;

    @Size(max = 100, message = "Vị trí tối đa 100 ký tự")
    private String location; // Ví dụ: Sân 5 người, Sân tiêu chuẩn

    @NotBlank(message = "Trạng thái không được để trống")
    private String status; // "Hoạt động", "Đang sử dụng", "Bảo trì", "Ngừng hoạt động"
}