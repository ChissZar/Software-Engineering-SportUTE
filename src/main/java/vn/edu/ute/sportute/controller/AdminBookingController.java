package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.service.CourtService;
import vn.edu.ute.sportute.repository.CustomerRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;
    private final CourtService courtService;
    private final CustomerRepository customerRepository;

    public AdminBookingController(AdminBookingService adminBookingService,
                                  CourtService courtService,
                                  CustomerRepository customerRepository) {
        this.adminBookingService = adminBookingService;
        this.courtService = courtService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public String index(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "status", required = false, defaultValue = "Tất cả") String status,
                        @RequestParam(value = "editId", required = false) String editId,
                        Model model) {
        // Dữ liệu bảng và bộ lọc[cite: 3]
        model.addAttribute("bookings", adminBookingService.getBookings(keyword, status));
        model.addAttribute("stats", adminBookingService.getBookingStatistics());
        model.addAttribute("courts", courtService.getAllCourts());
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("currentKeyword", keyword != null ? keyword : "");
        model.addAttribute("currentStatus", status);

        // Nạp form bên phải (Chỉnh sửa hoặc Thêm mới)[cite: 3]
        if (editId != null && !editId.trim().isEmpty()) {
            model.addAttribute("bookingDTO", adminBookingService.getBookingById(editId));
        } else {
            AdminBookingDTO newDTO = AdminBookingDTO.builder()
                    .bookingDate(LocalDate.now())
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(10, 0))
                    .totalAmount(BigDecimal.valueOf(150000))
                    .status("Chờ xác nhận")
                    .build();
            model.addAttribute("bookingDTO", newDTO);
        }
        return "bookings/index";
    }

 // Route xác nhận nhanh trực tiếp từ bảng danh sách[cite: 3]
    @PostMapping("/confirm/{id}")
    public String confirmQuick(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.confirmBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận đặt sân thành công cho phiếu " + id + "!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("bookingDTO") AdminBookingDTO dto,
                       BindingResult result,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ các trường bắt buộc!");
            return "redirect:/admin/bookings" + (dto.getId() != null ? "?editId=" + dto.getId() : "");
        }
        try {
            String employeeAccountId = (authentication != null) ? authentication.getName() : null;
            adminBookingService.saveBooking(dto, employeeAccountId);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin đặt sân thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // Giữ nguyên editId trên URL để không bị nhảy về form Thêm mới[cite: 11]
            if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
                return "redirect:/admin/bookings?editId=" + dto.getId().trim();
            }
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy phiếu đặt sân!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }
}