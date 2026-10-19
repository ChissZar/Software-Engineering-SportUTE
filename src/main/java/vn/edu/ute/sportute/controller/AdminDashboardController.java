package vn.edu.ute.sportute.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.ute.sportute.dto.response.DashboardDTO;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.service.CourtService;
import vn.edu.ute.sportute.service.DashboardService;
import vn.edu.ute.sportute.service.MaintenanceService;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final CourtService courtService;
    private final MaintenanceService maintenanceService;
    private final AdminBookingService adminBookingService;

    public AdminDashboardController(DashboardService dashboardService,
                                  CourtService courtService,
                                  MaintenanceService maintenanceService,
                                  AdminBookingService adminBookingService) {
        this.dashboardService = dashboardService;
        this.courtService = courtService;
        this.maintenanceService = maintenanceService;
        this.adminBookingService = adminBookingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardDTO dashboardData = dashboardService.getAdminDashboardData();
        model.addAttribute("data", dashboardData);
        model.addAttribute("courts", courtService.getAllCourts());
        model.addAttribute("maintenanceStats", maintenanceService.getMaintenanceStatistics());
        model.addAttribute("maintenances", maintenanceService.getAllMaintenances());
        model.addAttribute("bookingStats", adminBookingService.getBookingStatistics());
        model.addAttribute("todayTimeline", adminBookingService.getTimeline(LocalDate.now(), null));
        return "admin/dashboard";
    }
}