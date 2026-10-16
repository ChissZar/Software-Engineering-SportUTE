package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
public class SportDTO {
    private String id;

    @NotBlank(message = "Tên môn thể thao không được để trống")
    @Size(max = 50, message = "Tên môn tối đa 50 ký tự")
    private String name;

    @Size(max = 200, message = "Mô tả tối đa 200 ký tự")
    private String description;

    @NotBlank(message = "Trạng thái không được để trống")
    private String status; // "Hoạt động" hoặc "Ngừng kinh doanh"

    public SportDTO() {
    }

    public SportDTO(String id, String name, String description, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }
    public static class SportDTOBuilder {
        public SportDTOBuilder() {
        }
    }
}
