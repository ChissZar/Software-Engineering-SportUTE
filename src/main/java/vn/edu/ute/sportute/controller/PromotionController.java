package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.PromotionDTO;
import vn.edu.ute.sportute.service.PromotionService;

/**
 * Quản trị chương trình khuyến mãi (UC15, UC16).
 */
@Controller
@RequestMapping("/admin/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public String index(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "status", required = false, defaultValue = "Tất cả") String status,
                        @RequestParam(value = "editId", required = false) String editId,
                        Model model) {
        model.addAttribute("promotions", promotionService.getAllPromotions(keyword, status));
        model.addAttribute("stats", promotionService.getPromotionStatistics());
        model.addAttribute("currentKeyword", keyword != null ? keyword : "");
        model.addAttribute("selectedStatus", status);

        if (editId != null && !editId.trim().isEmpty()) {
            model.addAttribute("promotionDTO", promotionService.getPromotionById(editId));
        } else {
            model.addAttribute("promotionDTO", new PromotionDTO());
        }

        return "promotions/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("promotionDTO") PromotionDTO dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin khuyến mãi không hợp lệ!");
            return "redirect:/admin/promotions";
        }
        try {
            promotionService.savePromotion(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu chương trình khuyến mãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa chương trình khuyến mãi #" + id + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }
}
