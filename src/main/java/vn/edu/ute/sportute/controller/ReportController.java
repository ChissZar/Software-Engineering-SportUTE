package vn.edu.ute.sportute.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.ute.sportute.dto.response.ReportDataDTO;
import vn.edu.ute.sportute.repository.CourtRepository;
import vn.edu.ute.sportute.repository.SportRepository;
import vn.edu.ute.sportute.service.ReportService;

import java.time.LocalDate;

/**
 * Controller phục vụ Báo cáo & Thống kê (UC35 - UC39).
 */
@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    private final ReportService reportService;
    private final SportRepository sportRepository;
    private final CourtRepository courtRepository;

    public ReportController(ReportService reportService,
                            SportRepository sportRepository,
                            CourtRepository courtRepository) {
        this.reportService = reportService;
        this.sportRepository = sportRepository;
        this.courtRepository = courtRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(value = "sportId", required = false) String sportId,
            @RequestParam(value = "courtId", required = false) String courtId,
            @RequestParam(value = "bookingType", required = false, defaultValue = "Tất cả") String bookingType,
            Model model) {

        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate == null) {
            fromDate = toDate.minusDays(30);
        }

        ReportDataDTO reportData = reportService.generateReport(fromDate, toDate, sportId, courtId, bookingType);

        model.addAttribute("reportData", reportData);
        model.addAttribute("sports", sportRepository.findAll());
        model.addAttribute("courts", courtRepository.findAll());
        model.addAttribute("selectedFromDate", fromDate);
        model.addAttribute("selectedToDate", toDate);
        model.addAttribute("selectedSportId", sportId != null ? sportId : "Tất cả");
        model.addAttribute("selectedCourtId", courtId != null ? courtId : "Tất cả");
        model.addAttribute("selectedBookingType", bookingType);

        return "reports/index";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(value = "sportId", required = false) String sportId,
            @RequestParam(value = "courtId", required = false) String courtId,
            @RequestParam(value = "bookingType", required = false, defaultValue = "Tất cả") String bookingType) {

        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate == null) {
            fromDate = toDate.minusDays(30);
        }

        byte[] csvData = reportService.exportReportToCsv(fromDate, toDate, sportId, courtId, bookingType);
        String filename = "BaoCao_SportHub_" + fromDate + "_" + toDate + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
