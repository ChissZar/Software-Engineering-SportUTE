package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.*;
import vn.edu.ute.sportute.dto.response.*;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.repository.BookingPolicyRepository;
import vn.edu.ute.sportute.repository.CustomerRepository;
import vn.edu.ute.sportute.repository.PromotionRepository;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.service.CourtService;
import vn.edu.ute.sportute.service.SportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;
    private final CourtService courtService;
    private final CustomerRepository customerRepository;
    private final SportService sportService;
    private final BookingPolicyRepository bookingPolicyRepository;
    private final PromotionRepository promotionRepository;

    public AdminBookingController(AdminBookingService adminBookingService,
                                  CourtService courtService,
                                  CustomerRepository customerRepository,
                                  SportService sportService,
                                  BookingPolicyRepository bookingPolicyRepository,
                                  PromotionRepository promotionRepository) {
        this.adminBookingService = adminBookingService;
        this.courtService = courtService;
        this.customerRepository = customerRepository;
        this.sportService = sportService;
        this.bookingPolicyRepository = bookingPolicyRepository;
        this.promotionRepository = promotionRepository;
    }

    @GetMapping
    public String index(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "status", required = false, defaultValue = "Tất cả") String status,
                        @RequestParam(value = "editId", required = false) String editId,
                        @RequestParam(value = "scheduleDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate,
                        @RequestParam(value = "sportId", required = false) String sportId,
                        Model model) {
        // Dữ liệu bảng và bộ lọc
        model.addAttribute("bookings", adminBookingService.getBookings(keyword, status));
        model.addAttribute("stats", adminBookingService.getBookingStatistics());
        model.addAttribute("courts", courtService.getAllCourts());
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("sports", sportService.getAllSports());
        model.addAttribute("policies", bookingPolicyRepository.findAll());
        model.addAttribute("promotions", promotionRepository.findAll());
        model.addAttribute("currentKeyword", keyword != null ? keyword : "");
        model.addAttribute("currentStatus", status);

        LocalDate targetScheduleDate = (scheduleDate != null) ? scheduleDate : LocalDate.now();
        model.addAttribute("scheduleDate", targetScheduleDate);
        model.addAttribute("selectedSportId", (sportId != null && !sportId.isEmpty()) ? sportId : "ALL");

        // Dữ liệu Timeline thật từ database cho Lịch sân
        model.addAttribute("timelineCourts", adminBookingService.getTimeline(targetScheduleDate, sportId));

        // Nạp form bên phải (Chỉnh sửa hoặc Thêm mới)
        if (editId != null && !editId.trim().isEmpty()) {
            model.addAttribute("bookingDTO", adminBookingService.getBookingById(editId));
        } else {
            AdminBookingDTO newDTO = AdminBookingDTO.builder()
                    .bookingDate(LocalDate.now())
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(10, 0))
                    .totalAmount(BigDecimal.valueOf(150000))
                    .depositAmount(BigDecimal.valueOf(45000))
                    .status("Chờ xác nhận")
                    .policyId("CS01")
                    .build();
            model.addAttribute("bookingDTO", newDTO);
        }
        return "bookings/index";
    }

    // Duyệt / Xác nhận đặt sân
    @PostMapping("/confirm/{id}")
    public String confirmQuick(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.confirmBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận đặt sân thành công cho phiếu " + id + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // Nhận sân (UC21 - Check-in)
    @PostMapping("/checkin/{id}")
    public String checkIn(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.checkInBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Khách đã nhận sân thành công cho phiếu #" + id + "! Trạng thái chuyển sang 'Đang sử dụng'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể nhận sân: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // Trả sân (UC22 - Check-out)
    @PostMapping("/checkout/{id}")
    public String checkOut(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.checkOutBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận trả sân thành công cho phiếu #" + id + "! Trạng thái chuyển sang 'Hoàn tất'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể trả sân: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // Gia hạn sân (UC23)
    @PostMapping("/extend")
    public String extendBooking(@ModelAttribute ExtendBookingDTO dto, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.extendBooking(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Gia hạn thêm giờ chơi thành công cho phiếu #" + dto.getBookingId() + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gia hạn thất bại: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // Đổi sân hoặc giờ thuê (UC19)
    @PostMapping("/change-schedule")
    public String changeSchedule(@ModelAttribute ChangeScheduleDTO dto, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.changeSchedule(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi sân và giờ thuê thành công cho phiếu #" + dto.getBookingId() + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đổi sân/giờ thất bại: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // Hủy phiếu với chính sách cọc & hoàn tiền (UC20)
    @PostMapping("/cancel")
    public String cancelWithPolicy(@ModelAttribute CancelBookingDTO dto, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.cancelBookingWithPolicy(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy phiếu #" + dto.getBookingId() + " thành công theo chính sách hủy sân!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy phiếu: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // Hủy nhanh
    @PostMapping("/cancel/{id}")
    public String cancelQuick(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy phiếu đặt sân #" + id + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // Lưu / Tạo mới / Cập nhật phiếu đặt sân (UC17)
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("bookingDTO") AdminBookingDTO dto,
                       BindingResult result,
                       @RequestParam(value = "newCustomerPhone", required = false) String newCustomerPhone,
                       @RequestParam(value = "newCustomerName", required = false) String newCustomerName,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {

        // Hỗ trợ tạo nhanh khách hàng nếu lễ tân nhập số điện thoại mới
        if (("NEW".equalsIgnoreCase(dto.getCustomerId()) || dto.getCustomerId() == null || dto.getCustomerId().isBlank()) 
                && newCustomerPhone != null && !newCustomerPhone.isBlank()) {
            try {
                Customer newCust = adminBookingService.findOrCreateCustomer(newCustomerPhone, newCustomerName, null, null);
                dto.setCustomerId(newCust.getId());
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo hồ sơ khách hàng: " + ex.getMessage());
                return "redirect:/admin/bookings";
            }
        }

        if (result.hasErrors()) {
            String firstError = result.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", firstError != null ? firstError : "Dữ liệu đặt sân không hợp lệ!");
            return "redirect:/admin/bookings" + (dto.getId() != null ? "?editId=" + dto.getId() : "");
        }

        if (dto.getCustomerId() == null || dto.getCustomerId().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn hoặc nhập thông tin khách hàng!");
            return "redirect:/admin/bookings" + (dto.getId() != null ? "?editId=" + dto.getId() : "");
        }

        try {
            String employeeAccountId = (authentication != null) ? authentication.getName() : null;
            adminBookingService.saveBooking(dto, employeeAccountId);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin đặt sân thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
                return "redirect:/admin/bookings?editId=" + dto.getId().trim();
            }
        }
        return "redirect:/admin/bookings";
    }

    // =========================================================================
    // REST APIs cho tương tác AJAX
    // =========================================================================

    // API lấy ma trận lịch sân theo ngày & môn thể thao
    @GetMapping("/api/timeline")
    @ResponseBody
    public ResponseEntity<List<CourtTimelineDTO>> getTimelineApi(
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(value = "sportId", required = false) String sportId) {
        return ResponseEntity.ok(adminBookingService.getTimeline(date, sportId));
    }

    // API tra cứu khách hàng nhanh theo SĐT (dành cho lễ tân tại quầy)
    @GetMapping("/api/customer-by-phone")
    @ResponseBody
    public ResponseEntity<?> getCustomerByPhone(@RequestParam("phone") String phone) {
        Optional<Customer> cust = customerRepository.findByPhone(phone.trim());
        if (cust.isPresent()) {
            Customer c = cust.get();
            Map<String, Object> resp = new HashMap<>();
            resp.put("found", true);
            resp.put("id", c.getId());
            resp.put("fullName", c.getFullName());
            resp.put("phone", c.getPhone());
            resp.put("email", c.getEmail() != null ? c.getEmail() : "");
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(Map.of("found", false));
        }
    }

    // API tính dự toán tiền sân và tiền cọc
    @PostMapping("/api/estimate-price")
    @ResponseBody
    public ResponseEntity<PriceEstimateResponseDTO> estimatePrice(@RequestBody PriceEstimateRequestDTO req) {
        return ResponseEntity.ok(adminBookingService.calculateEstimate(req));
    }

    // API lấy chi tiết phiếu đặt sân cho Modal
    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public ResponseEntity<AdminBookingDTO> getBookingDetail(@PathVariable String id) {
        try {
            return ResponseEntity.ok(adminBookingService.getBookingById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}