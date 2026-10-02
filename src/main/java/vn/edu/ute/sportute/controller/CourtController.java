package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.CourtDTO;
import vn.edu.ute.sportute.service.CourtService;
import vn.edu.ute.sportute.service.SportService;

@Controller
@RequestMapping("/admin/courts")
public class CourtController {

    private final CourtService courtService;
    private final SportService sportService;

    public CourtController(CourtService courtService, SportService sportService) {
        this.courtService = courtService;
        this.sportService = sportService;
    }

    @GetMapping
    public String index(@RequestParam(value = "editId", required = false) String editId, Model model) {
        model.addAttribute("courts", courtService.getAllCourts());
        model.addAttribute("sports", sportService.getAllSports());
        model.addAttribute("stats", courtService.getCourtStatistics());

        if (editId != null && !editId.trim().isEmpty()) {
            model.addAttribute("courtDTO", courtService.getCourtById(editId));
        } else {
            model.addAttribute("courtDTO", new CourtDTO());
        }
        return "courts/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("courtDTO") CourtDTO dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        if (result.hasErrors()) {
            model.addAttribute("courts", courtService.getAllCourts());
            model.addAttribute("sports", sportService.getAllSports());
            model.addAttribute("stats", courtService.getCourtStatistics());
            return "courts/index";
        }
        try {
            courtService.saveCourt(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin sân thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courts";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            courtService.toggleCourtStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái sân thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courts";
    }
}