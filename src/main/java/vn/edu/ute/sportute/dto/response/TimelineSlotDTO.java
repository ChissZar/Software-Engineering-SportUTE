package vn.edu.ute.sportute.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineSlotDTO {
    private String time;                 // e.g. "06:00"
    private String status;               // "FREE", "BUSY", "IN_USE", "MAINT"
    private String statusLabel;          // "Trống", "Đã đặt", "Đang dùng", "Bảo trì"
    private String cssClass;             // "slot-free", "slot-busy", "slot-inuse", "slot-maint"
    private String bookingId;            // e.g. "DS0101"
    private String customerId;           // e.g. "KH002"
    private String customerName;         // e.g. "Nguyễn Quốc Thụy"
    private String customerShortName;    // e.g. "Thụy" (chỉ tên không đệm và họ)
    private String customerDisplayName;  // e.g. "KH002 - Thụy"
    private String customerPhone;        // e.g. "0918222333"
    private String timeRange;            // e.g. "06:00 - 07:30"
    private String note;                 // Ghi chú / lý do bảo trì
    
    @Builder.Default
    private int span = 1;                // Số ô 30 phút kéo dài (mặc định 1)
    
    @Builder.Default
    private boolean isSpanContinuation = false; // true nếu ô này đã được gộp vào ô bắt đầu
}
