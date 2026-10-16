package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.response.ReportDataDTO;

import java.time.LocalDate;

/**
 * Hợp đồng nghiệp vụ Báo cáo & Thống kê (UC35 - UC39).
 */
public interface ReportService {

    /**
     * Tạo báo cáo tổng hợp dòng tiền thu ròng (UC35), lượt thuê & công suất (UC36),
     * xếp hạng sân (UC37), khách hàng thường xuyên (UC38).
     */
    ReportDataDTO generateReport(LocalDate fromDate, LocalDate toDate, String sportId, String courtId, String bookingType);

    /**
     * Xuất báo cáo theo định dạng CSV/XLSX (UC39) hỗ trợ tiếng Việt có dấu.
     */
    byte[] exportReportToCsv(LocalDate fromDate, LocalDate toDate, String sportId, String courtId, String bookingType);
}
