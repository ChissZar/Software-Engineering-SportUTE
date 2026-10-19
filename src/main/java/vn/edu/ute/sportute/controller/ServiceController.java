package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.ServiceItemDTO;
import vn.edu.ute.sportute.entity.ServiceItem;
import vn.edu.ute.sportute.service.ServiceItemService;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/services")
public class ServiceController {

    private final ServiceItemService serviceItemService;
    private final vn.edu.ute.sportute.integration.cloudinary.CloudinaryStorageService cloudinaryStorageService;

    public ServiceController(ServiceItemService serviceItemService,
                             vn.edu.ute.sportute.integration.cloudinary.CloudinaryStorageService cloudinaryStorageService) {
        this.serviceItemService = serviceItemService;
        this.cloudinaryStorageService = cloudinaryStorageService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String editId,
                        Model model) {
        List<ServiceItem> list = serviceItemService.getAllServices();

        // Filter by category
        if (category != null && !category.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(category)) {
            list = list.stream()
                    .filter(s -> category.equalsIgnoreCase(s.getCategory()))
                    .collect(Collectors.toList());
        }

        // Filter by keyword
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            list = list.stream()
                    .filter(s -> (s.getId() != null && s.getId().toLowerCase().contains(kw))
                            || (s.getName() != null && s.getName().toLowerCase().contains(kw))
                            || (s.getCategory() != null && s.getCategory().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        // Calculate KPI stats
        List<ServiceItem> all = serviceItemService.getAllServices();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());
        stats.put("drinks", all.stream().filter(s -> "Thức uống".equalsIgnoreCase(s.getCategory())).count());
        stats.put("food", all.stream().filter(s -> "Đồ ăn nhẹ".equalsIgnoreCase(s.getCategory())).count());
        stats.put("gear", all.stream().filter(s -> "Thuê dụng cụ".equalsIgnoreCase(s.getCategory())).count());
        stats.put("accessories", all.stream().filter(s -> "Phụ kiện".equalsIgnoreCase(s.getCategory())).count());

        ServiceItemDTO serviceDTO = new ServiceItemDTO();
        if (editId != null && !editId.trim().isEmpty()) {
            try {
                ServiceItem editItem = serviceItemService.getServiceById(editId.trim());
                serviceDTO = ServiceItemDTO.builder()
                        .id(editItem.getId())
                        .name(editItem.getName())
                        .category(editItem.getCategory())
                        .unit(editItem.getUnit())
                        .price(editItem.getPrice())
                        .stock(editItem.getStock())
                        .imageUrl(editItem.getImageUrl())
                        .status(editItem.getStatus())
                        .build();
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("services", list);
        model.addAttribute("serviceDTO", serviceDTO);
        model.addAttribute("stats", stats);
        model.addAttribute("currentCategory", category != null ? category : "Tất cả");
        model.addAttribute("currentKeyword", keyword);

        return "services/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("serviceDTO") ServiceItemDTO dto,
                       BindingResult bindingResult,
                       @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/services";
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = cloudinaryStorageService.uploadImage(imageFile, "services");
                dto.setImageUrl(uploadedUrl);
            }
            serviceItemService.saveService(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin dịch vụ / sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lưu dịch vụ: " + e.getMessage());
        }

        return "redirect:/admin/services";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            serviceItemService.toggleServiceStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái kinh doanh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đổi trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            serviceItemService.deleteService(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa / chuyển trạng thái ngừng kinh doanh cho dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }
}
