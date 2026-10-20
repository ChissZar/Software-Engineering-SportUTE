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
        if (!model.containsAttribute("maintenanceDTO")) model.addAttribute("maintenanceDTO", new MaintenanceDTO());
        return "maintenance/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("maintenanceDTO") MaintenanceDTO dto,
                       BindingResult result,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("maintenanceDTO", dto);
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", result.getAllErrors().stream().map(org.springframework.validation.ObjectError::getDefaultMessage).collect(java.util.stream.Collectors.joining("; ")));
            return "redirect:/admin/maintenance";
        }
        try {
            String accountId = authentication != null ? authentication.getName() : null;
            maintenanceService.createMaintenance(dto, accountId);
            redirectAttributes.addFlashAttribute("maintenanceDTO", new MaintenanceDTO());
            redirectAttributes.addFlashAttribute("successMessage", "Tạo lịch bảo trì thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/maintenance";
    }

    @PostMapping("/complete/{id}")
    public String complete(@PathVariable String id, RedirectAttributes flash) {
        try { maintenanceService.completeMaintenance(id); flash.addFlashAttribute("successMessage", "Đã hoàn tất bảo trì"); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/admin/maintenance";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            maintenanceService.deleteMaintenance(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy lịch bảo trì; lịch sử được giữ lại.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/maintenance";
    }
}