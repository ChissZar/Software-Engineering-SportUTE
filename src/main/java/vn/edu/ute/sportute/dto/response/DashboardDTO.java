package vn.edu.ute.sportute.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardDTO {
    // 4 thẻ KPI tổng quan (Số 8 - Hình 5-1)
    private long totalBookings;
    private BigDecimal revenueToday;
    private int occupancyRate;
    private long newCustomers;

    // Danh sách sân và trạng thái hiện tại (Số 14)
    private List<CourtStatusDTO> courts;

    // Đặt sân gần đây (Số 15)
    private List<RecentBookingDTO> recentBookings;

    @Data
    @Builder
    public static class CourtStatusDTO {
        private String id;
        private String name;
        private String sportName;
        private String status;
        private String timeSlot;
    }

    @Data
    @Builder
    public static class RecentBookingDTO {
        private String id;
        private String customerName;
        private String courtName;
        private String time;
        private String status;
    }
}