package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.PriceDTO;
import vn.edu.ute.sportute.service.CourtService;
import vn.edu.ute.sportute.service.PriceService;

@Controller
@RequestMapping("/admin/prices")
public class PriceController {

    private final PriceService priceService;
    private final CourtService courtService;

    public PriceController(PriceService priceService, CourtService courtService) {
        this.priceService = priceService;
        this.courtService = courtService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("prices", priceService.getAllPrices());
        model.addAttribute("courts", courtService.getAllCourts());
        PriceDTO priceDTO = new PriceDTO();
        priceDTO.getDetails().add(new PriceDTO.DetailDTO());
        model.addAttribute("priceDTO", priceDTO);
        return "prices/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("priceDTO") PriceDTO dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin bảng giá không hợp lệ!");
            return "redirect:/admin/prices";
        }
        try {
            priceService.savePrice(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu bảng giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/prices";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            priceService.deletePrice(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bảng giá!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/prices";
    }
}