package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmployeeDTO {

    private String id;

    @NotBlank(message = "Vui lòng nhập họ và tên nhân viên")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    private String phone;

    @NotBlank(message = "Vui lòng nhập email nhân viên")
    private String email;

    @NotBlank(message = "Vui lòng chọn chức vụ")
    private String position; // Quản lý, Lễ tân, Vận hành, Kỹ thuật

    @Builder.Default
    private String status = "Đang làm"; // Đang làm, Nghỉ phép, Đã nghỉ

    private String username;
    private String roleId;
    private String roleName;

    public EmployeeDTO() {
    }

    public EmployeeDTO(String id, String fullName, String phone, String email, String position, String status, String username, String roleId, String roleName) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.position = position != null ? position : "Lễ tân";
        this.status = status != null ? status : "Đang làm";
        this.username = username;
        this.roleId = roleId;
        this.roleName = roleName;
    }
    public static class EmployeeDTOBuilder {
        public EmployeeDTOBuilder() {
        }
    }
}
