package vn.edu.ute.sportute.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
public class TimelineSlotDTO {
    private String time;         // e.g. "06:00"
    private String status;       // "free", "confirmed", "inuse", "maintenance"
    private String statusLabel;  // "Trống", "Đã đặt", "Đang dùng", "Bảo trì"
    private String cssClass;     // "slot-free", "slot-busy", "slot-inuse", "slot-maint"
    private String bookingId;    // e.g. "DS0101" (if booked/in use)
    private String customerName; // e.g. "Nguyễn Minh Anh"
    private String note;         // Lý do bảo trì hoặc thông tin thêm

    public TimelineSlotDTO() {
    }

    public TimelineSlotDTO(String time, String status, String statusLabel, String cssClass,
                           String bookingId, String customerName, String note) {
        this.time = time;
        this.status = status;
        this.statusLabel = statusLabel;
        this.cssClass = cssClass;
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.note = note;
    }
    public static class TimelineSlotDTOBuilder {
        public TimelineSlotDTOBuilder() {
        }
    }
}
