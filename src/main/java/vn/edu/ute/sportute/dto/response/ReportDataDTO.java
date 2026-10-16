package vn.edu.ute.sportute.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class ReportDataDTO {

    @Builder.Default
    private ReportSummaryDTO summary = new ReportSummaryDTO();

    @Builder.Default
    private List<SportBreakdownDTO> sportBreakdowns = new ArrayList<>();

    @Builder.Default
    private List<TopCourtDTO> topCourts = new ArrayList<>();

    @Builder.Default
    private List<FrequentCustomerReportDTO> frequentCustomers = new ArrayList<>();

    @Builder.Default
    private List<DailyRevenueDTO> dailyRevenues = new ArrayList<>();

    public ReportDataDTO() {
    }

    public ReportDataDTO(ReportSummaryDTO summary, List<SportBreakdownDTO> sportBreakdowns,
                         List<TopCourtDTO> topCourts, List<FrequentCustomerReportDTO> frequentCustomers,
                         List<DailyRevenueDTO> dailyRevenues) {
        this.summary = summary != null ? summary : new ReportSummaryDTO();
        this.sportBreakdowns = sportBreakdowns != null ? sportBreakdowns : new ArrayList<>();
        this.topCourts = topCourts != null ? topCourts : new ArrayList<>();
        this.frequentCustomers = frequentCustomers != null ? frequentCustomers : new ArrayList<>();
        this.dailyRevenues = dailyRevenues != null ? dailyRevenues : new ArrayList<>();
    }

    public static class ReportDataDTOBuilder {
        public ReportDataDTOBuilder() {
        }
    }

    @Getter
    @Setter
    @Builder
    public static class ReportSummaryDTO {
        @Builder.Default
        private BigDecimal totalThu = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal totalDeposit = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal totalRefund = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal netRevenue = BigDecimal.ZERO;
        private long completedBookings;
        private double averageCapacity;
        private long frequentCustomersCount;

        public ReportSummaryDTO() {
        }

        public ReportSummaryDTO(BigDecimal totalThu, BigDecimal totalDeposit, BigDecimal totalRefund,
                                BigDecimal netRevenue, long completedBookings, double averageCapacity,
                                long frequentCustomersCount) {
            this.totalThu = totalThu != null ? totalThu : BigDecimal.ZERO;
            this.totalDeposit = totalDeposit != null ? totalDeposit : BigDecimal.ZERO;
            this.totalRefund = totalRefund != null ? totalRefund : BigDecimal.ZERO;
            this.netRevenue = netRevenue != null ? netRevenue : BigDecimal.ZERO;
            this.completedBookings = completedBookings;
            this.averageCapacity = averageCapacity;
            this.frequentCustomersCount = frequentCustomersCount;
        }

        public static class ReportSummaryDTOBuilder {
            public ReportSummaryDTOBuilder() {
            }
        }
    }

    @Getter
    @Setter
    @Builder
    public static class SportBreakdownDTO {
        private String sportId;
        private String sportName;
        private String color;
        private long completedCount;
        private double percentage;
        private double capacity;

        public SportBreakdownDTO() {
        }

        public SportBreakdownDTO(String sportId, String sportName, String color, long completedCount,
                                 double percentage, double capacity) {
            this.sportId = sportId;
            this.sportName = sportName;
            this.color = color;
            this.completedCount = completedCount;
            this.percentage = percentage;
            this.capacity = capacity;
        }

        public static class SportBreakdownDTOBuilder {
            public SportBreakdownDTOBuilder() {
            }
        }
    }

    @Getter
    @Setter
    @Builder
    public static class TopCourtDTO {
        private int rank;
        private String courtId;
        private String courtName;
        private String sportName;
        private long completedCount;
        private double capacity;
        @Builder.Default
        private BigDecimal totalRevenue = BigDecimal.ZERO;

        public TopCourtDTO() {
        }

        public TopCourtDTO(int rank, String courtId, String courtName, String sportName,
                           long completedCount, double capacity, BigDecimal totalRevenue) {
            this.rank = rank;
            this.courtId = courtId;
            this.courtName = courtName;
            this.sportName = sportName;
            this.completedCount = completedCount;
            this.capacity = capacity;
            this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        }

        public static class TopCourtDTOBuilder {
            public TopCourtDTOBuilder() {
            }
        }
    }

    @Getter
    @Setter
    @Builder
    public static class FrequentCustomerReportDTO {
        private String customerId;
        private String fullName;
        private String phone;
        private long completedCount;
        private double totalHours;
        private String primaryType;
        private String favoriteSport;
        private String status;

        public FrequentCustomerReportDTO() {
        }

        public FrequentCustomerReportDTO(String customerId, String fullName, String phone,
                                         long completedCount, double totalHours, String primaryType,
                                         String favoriteSport, String status) {
            this.customerId = customerId;
            this.fullName = fullName;
            this.phone = phone;
            this.completedCount = completedCount;
            this.totalHours = totalHours;
            this.primaryType = primaryType;
            this.favoriteSport = favoriteSport;
            this.status = status;
        }

        public static class FrequentCustomerReportDTOBuilder {
            public FrequentCustomerReportDTOBuilder() {
            }
        }
    }

    @Getter
    @Setter
    @Builder
    public static class DailyRevenueDTO {
        private LocalDate date;
        private String dateStr;
        @Builder.Default
        private BigDecimal thu = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal hoan = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal netRevenue = BigDecimal.ZERO;

        public DailyRevenueDTO() {
        }

        public DailyRevenueDTO(LocalDate date, String dateStr, BigDecimal thu, BigDecimal hoan, BigDecimal netRevenue) {
            this.date = date;
            this.dateStr = dateStr;
            this.thu = thu != null ? thu : BigDecimal.ZERO;
            this.hoan = hoan != null ? hoan : BigDecimal.ZERO;
            this.netRevenue = netRevenue != null ? netRevenue : BigDecimal.ZERO;
        }

        public static class DailyRevenueDTOBuilder {
            public DailyRevenueDTOBuilder() {
            }
        }
    }
}
