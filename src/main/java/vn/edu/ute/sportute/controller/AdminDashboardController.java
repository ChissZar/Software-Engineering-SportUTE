package vn.edu.ute.sportute.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.ute.sportute.dto.response.DashboardDTO;
import vn.edu.ute.sportute.service.DashboardService;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardDTO dashboardData = dashboardService.getAdminDashboardData();
        model.addAttribute("data", dashboardData);
        return "admin/dashboard";
    }
}