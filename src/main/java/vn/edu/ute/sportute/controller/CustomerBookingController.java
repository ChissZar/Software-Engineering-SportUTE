package vn.edu.ute.sportute.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.CustomerBookingRequestDTO;
import vn.edu.ute.sportute.dto.request.CustomerCourtFilterDTO;
import vn.edu.ute.sportute.service.CustomerBookingService;
import vn.edu.ute.sportute.service.SportService;

@Controller
@RequestMapping("/customer")
public class CustomerBookingController {

    private final CustomerBookingService customerBookingService;
    private final SportService sportService;

    public CustomerBookingController(CustomerBookingService customerBookingService, SportService sportService) {
        this.customerBookingService = customerBookingService;
        this.sportService = sportService;
    }

    @GetMapping("/booking")
    public String bookingPage(@ModelAttribute("filter") CustomerCourtFilterDTO filter,
                              Model model,
                              Authentication authentication) {
        if (filter == null) {
            filter = new CustomerCourtFilterDTO();
        }

        // Tên hiển thị người dùng trên Header
        String customerName = (authentication != null) ? authentication.getName() : "Khách hàng";
        model.addAttribute("currentUserName", customerName);

        // Danh sách môn thể thao cho các nút Pills (Số 3 Hình 5-3)[cite: 3]
        model.addAttribute("sports", sportService.getAllSports());

        // Lấy danh sách sân trống phù hợp theo bộ lọc[cite: 3]
        model.addAttribute("availableCourts", customerBookingService.searchAvailableCourts(filter));
        model.addAttribute("filter", filter);
        model.addAttribute("bookingRequest", new CustomerBookingRequestDTO());

        return "customer/booking";
    }

    @PostMapping("/booking/confirm")
    public String confirmBooking(@ModelAttribute CustomerBookingRequestDTO request,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            String username = (authentication != null) ? authentication.getName() : "khachle";
            customerBookingService.bookCourtOnline(request, username);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Đặt sân trực tuyến thành công! Phiếu của bạn đang ở trạng thái 'Chờ xác nhận'. Vui lòng kiểm tra lịch sử đặt sân.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/booking";
    }
}