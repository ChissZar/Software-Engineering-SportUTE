package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.MaintenanceDTO;
import vn.edu.ute.sportute.service.CourtService;
import vn.edu.ute.sportute.service.MaintenanceService;

@Controller
@RequestMapping("/admin/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final CourtService courtService;

    public MaintenanceController(MaintenanceService maintenanceService, CourtService courtService) {
        this.maintenanceService = maintenanceService;
        this.courtService = courtService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("maintenances", maintenanceService.getAllMaintenances());
        model.addAttribute("courts", courtService.getAllCourts());
        model.addAttribute("stats", maintenanceService.getMaintenanceStatistics());
        model.addAttribute("maintenanceDTO", new MaintenanceDTO());
        return "maintenance/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("maintenanceDTO") MaintenanceDTO dto,
                       BindingResult result,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ các trường bắt buộc!");
            return "redirect:/admin/maintenance";
        }
        try {
            String accountId = authentication != null ? authentication.getName() : null;
            maintenanceService.createMaintenance(dto, accountId);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo lịch bảo trì thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/maintenance";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            maintenanceService.deleteMaintenance(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa lịch bảo trì và hoàn tất trạng thái sân!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/maintenance";
    }
}