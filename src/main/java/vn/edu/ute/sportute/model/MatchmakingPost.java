package vn.edu.ute.sportute.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MatchmakingPost {
    private String id;              // Mã kèo: KEO001, KEO002,...
    private String bookingId;       // Liên kết bắt buộc với phiếu đặt sân
    private String sportName;       // Bóng đá, Cầu lông, Pickleball, Bóng rổ
    private String courtName;       // Sân bóng đá 1, Sân cầu lông 2,...
    private String courtId;         // S001, S002,...
    private String matchDate;       // yyyy-MM-dd
    private String startTime;       // HH:mm
    private String endTime;         // HH:mm
    @Builder.Default
    private int durationMinutes = 120;

    // Người tạo kèo (Chủ sân)
    private String creatorAccountId;
    private String opponentAccountId;
    private boolean creatorNotificationRead;
    private boolean opponentNotificationRead;
    private boolean owner;
    private boolean participant;
    private String creatorName;
    private String creatorPhone;
    private String creatorEmail;
    private String skillLevel;      // Mới chơi / Giao lưu, Trung bình / Phong trào, Khá / Bán chuyên
    private String teamSize;        // Đơn, Đôi, 5 vs 5, 7 vs 7
    private String message;         // Lời nhắn giao lưu
    @Builder.Default
    private String status = "WAITING"; // WAITING (Chờ đối thủ), MATCHED (Đã có đối thủ), CANCELLED (Đã hủy)

    // Đối thủ nhận kèo
    private String opponentName;
    private String opponentPhone;
    private String opponentEmail;
    private String opponentMessage;
    private String acceptedAt;

    // Báo cáo vi phạm / Bùng kèo
    private boolean reported;
    private String reportReason;
    private String reportedBy;
    private String reportedAt;

    private String createdAt;
}
