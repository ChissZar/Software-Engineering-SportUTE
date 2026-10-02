package vn.edu.ute.sportute.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CustomerCourtFilterDTO {
    private String sportId; // Mã môn (Tất cả, M_BD, M_CL, M_BR, M_PB)

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate = LocalDate.now();

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime = LocalTime.of(18, 0); // Mặc định 06:00 PM

    private int durationMinutes = 120; // Mặc định 2 giờ (120 phút)

    private String courtType = "Tất cả loại sân";

    private String keyword; // Tìm kiếm theo tên hoặc mã sân
}