package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.SportDTO;
import vn.edu.ute.sportute.service.SportService;

@Controller
@RequestMapping("/admin/sports")
public class SportController {

    private final SportService sportService;

    public SportController(SportService sportService) {
        this.sportService = sportService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("sports", sportService.getAllSports());
        model.addAttribute("sportDTO", new SportDTO());
        return "sports/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("sportDTO") SportDTO dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        if (result.hasErrors()) {
            model.addAttribute("sports", sportService.getAllSports());
            return "sports/index";
        }
        try {
            sportService.saveSport(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu môn thể thao thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sports";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            sportService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái môn thể thao!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sports";
    }
}