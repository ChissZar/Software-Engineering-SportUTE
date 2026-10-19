package vn.edu.ute.sportute.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class DashboardDTO {
    private long totalBookings;
    private BigDecimal revenueToday;
    private int occupancyRate;
    private long newCustomers;

    private List<CourtStatusDTO> courts;
    private List<RecentBookingDTO> recentBookings;

    public DashboardDTO() {
    }

    public DashboardDTO(long totalBookings, BigDecimal revenueToday, int occupancyRate, long newCustomers,
                        List<CourtStatusDTO> courts, List<RecentBookingDTO> recentBookings) {
        this.totalBookings = totalBookings;
        this.revenueToday = revenueToday;
        this.occupancyRate = occupancyRate;
        this.newCustomers = newCustomers;
        this.courts = courts;
        this.recentBookings = recentBookings;
    }

    public long getTotalCustomers() {
        return this.newCustomers;
    }

    public static class DashboardDTOBuilder {
        public DashboardDTOBuilder() {
        }
    }

    @Getter
    @Setter
    @Builder
    public static class CourtStatusDTO {
        private String id;
        private String name;
        private String sportName;
        private String status;
        private String timeSlot;

        public CourtStatusDTO() {
        }

        public CourtStatusDTO(String id, String name, String sportName, String status, String timeSlot) {
            this.id = id;
            this.name = name;
            this.sportName = sportName;
            this.status = status;
            this.timeSlot = timeSlot;
        }

        public static class CourtStatusDTOBuilder {
            public CourtStatusDTOBuilder() {
            }
        }
    }

    @Getter
    @Setter
    @Builder
    public static class RecentBookingDTO {
        private String id;
        private String customerName;
        private String courtName;
        private String time;
        private String status;

        public RecentBookingDTO() {
        }

        public RecentBookingDTO(String id, String customerName, String courtName, String time, String status) {
            this.id = id;
            this.customerName = customerName;
            this.courtName = courtName;
            this.time = time;
            this.status = status;
        }

        public String getBookingDate() {
            return this.time != null ? this.time : "Hôm nay";
        }

        public BigDecimal getTotalPrice() {
            return BigDecimal.valueOf(250000);
        }

        public static class RecentBookingDTOBuilder {
            public RecentBookingDTOBuilder() {
            }
        }
    }
}
