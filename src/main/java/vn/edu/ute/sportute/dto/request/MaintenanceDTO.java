package vn.edu.ute.sportute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MaintenanceDTO {
    private String id;

    @NotBlank(message = "Vui lòng chọn sân cần bảo trì")
    private String courtId;
    private String courtName;

    private String employeeId;
    private String employeeName;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    @NotBlank(message = "Vui lòng nhập lý do bảo trì")
    private String reason;

    public MaintenanceDTO() {
    }

    public MaintenanceDTO(String id, String courtId, String courtName, String employeeId,
                          String employeeName, LocalDateTime startDate, LocalDateTime endDate, String reason) {
        this.id = id;
        this.courtId = courtId;
        this.courtName = courtName;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }
    public static class MaintenanceDTOBuilder {
        public MaintenanceDTOBuilder() {
        }
    }
}
