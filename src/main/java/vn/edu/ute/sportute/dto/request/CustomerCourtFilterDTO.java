package vn.edu.ute.sportute.dto.request;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class CustomerCourtFilterDTO {
    private String sportId; // Mã môn (Tất cả, M_BD, M_CL, M_BR, M_PB)

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Builder.Default
    private LocalDate bookingDate = LocalDate.now();

    @DateTimeFormat(pattern = "HH:mm")
    @Builder.Default
    private LocalTime startTime = LocalTime.of(18, 0); // Mặc định 06:00 PM

    @Builder.Default
    private int durationMinutes = 120; // Mặc định 2 giờ (120 phút)

    @Builder.Default
    private String courtType = "Tất cả loại sân";

    private String keyword; // Tìm kiếm theo tên hoặc mã sân

    public CustomerCourtFilterDTO() {
        this.bookingDate = LocalDate.now();
        this.startTime = LocalTime.of(18, 0);
        this.durationMinutes = 120;
        this.courtType = "Tất cả loại sân";
    }

    public CustomerCourtFilterDTO(String sportId, LocalDate bookingDate, LocalTime startTime,
                                  int durationMinutes, String courtType, String keyword) {
        this.sportId = sportId;
        this.bookingDate = bookingDate != null ? bookingDate : LocalDate.now();
        this.startTime = startTime != null ? startTime : LocalTime.of(18, 0);
        this.durationMinutes = durationMinutes;
        this.courtType = courtType != null ? courtType : "Tất cả loại sân";
        this.keyword = keyword;
    }
    public static class CustomerCourtFilterDTOBuilder {
        public CustomerCourtFilterDTOBuilder() {
        }
    }
}
