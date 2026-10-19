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
    private final vn.edu.ute.sportute.service.ServiceItemService serviceItemService;
    private final vn.edu.ute.sportute.service.InvoiceExportService invoiceExportService;

    public AdminBookingController(AdminBookingService adminBookingService,
                                  CourtService courtService,
                                  CustomerRepository customerRepository,
                                  SportService sportService,
                                  BookingPolicyRepository bookingPolicyRepository,
                                  PromotionRepository promotionRepository,
                                  vn.edu.ute.sportute.service.ServiceItemService serviceItemService,
                                  vn.edu.ute.sportute.service.InvoiceExportService invoiceExportService) {
        this.adminBookingService = adminBookingService;
        this.courtService = courtService;
        this.customerRepository = customerRepository;
        this.sportService = sportService;
        this.bookingPolicyRepository = bookingPolicyRepository;
        this.promotionRepository = promotionRepository;
        this.serviceItemService = serviceItemService;
        this.invoiceExportService = invoiceExportService;
    }

    @GetMapping
    public String index(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "status", required = false, defaultValue = "Tất cả") String status,
                        @RequestParam(value = "editId", required = false) String editId,
                        @RequestParam(value = "view", required = false) String view,
                        @RequestParam(value = "scheduleDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate,
                        @RequestParam(value = "sportId", required = false) String sportId,
                        Model model) {
        // Dữ liệu bảng và bộ lọc
        model.addAttribute("activeMenu", "schedule".equalsIgnoreCase(view) ? "schedule" : "bookings");
        model.addAttribute("currentView", view != null ? view : "list");
        model.addAttribute("bookings", adminBookingService.getBookings(keyword, status));
        model.addAttribute("stats", adminBookingService.getBookingStatistics());
        model.addAttribute("courts", courtService.getAllCourts());
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("sports", sportService.getAllSports());
        model.addAttribute("policies", bookingPolicyRepository.findAll());
        model.addAttribute("promotions", promotionRepository.findAll());
        model.addAttribute("activeServices", serviceItemService.getActiveServices());
        model.addAttribute("currentKeyword", keyword != null ? keyword : "");
        model.addAttribute("currentStatus", status);

        LocalDate targetScheduleDate = (scheduleDate != null) ? scheduleDate : LocalDate.now();
        List<CourtTimelineDTO> timeline = adminBookingService.getTimeline(targetScheduleDate, sportId);

        model.addAttribute("timeline", timeline);
        model.addAttribute("timelineCourts", timeline);
        model.addAttribute("currentScheduleDate", targetScheduleDate);
        model.addAttribute("scheduleDate", targetScheduleDate);
        model.addAttribute("currentSportId", (sportId != null && !sportId.isEmpty()) ? sportId : "");
        model.addAttribute("selectedSportId", (sportId != null && !sportId.isEmpty()) ? sportId : "ALL");

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
    public String checkOut(@PathVariable String id,
                           @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                           @RequestParam(value = "note", required = false) String note,
                           RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.checkOutBooking(id, paymentMethod, note);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận trả sân thành công cho phiếu #" + id + "! Trạng thái chuyển sang 'Hoàn tất' và sân đã sẵn sàng đón lượt khách mới.");
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

    // Thêm dịch vụ / nước uống / thuê dụng cụ vào phiếu đặt sân
    @PostMapping("/add-service")
    public String addServiceToBooking(@ModelAttribute vn.edu.ute.sportute.dto.request.AddServiceOrderRequestDTO req,
                                      RedirectAttributes redirectAttributes) {
        try {
            serviceItemService.addServiceToBooking(req);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm món/dịch vụ vào phiếu #" + req.getBookingId() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi thêm dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // Xóa dịch vụ khỏi phiếu đặt sân
    @PostMapping("/delete-service/{detailId}")
    public String removeServiceFromBooking(@PathVariable String detailId,
                                           @RequestParam String bookingId,
                                           RedirectAttributes redirectAttributes) {
        try {
            serviceItemService.removeServiceFromBooking(detailId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa dịch vụ khỏi phiếu #" + bookingId + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // API Thêm dịch vụ / nước uống theo dạng Batch qua AJAX (cho quầy POS siêu tốc)
    @PostMapping("/api/add-services")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addServicesAjax(@RequestBody List<vn.edu.ute.sportute.dto.request.AddServiceOrderRequestDTO> requests) {
        try {
            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Danh sách món đặt rỗng!"));
            }
            int count = 0;
            String bookingId = null;
            for (var req : requests) {
                if (req.getQuantity() != null && req.getQuantity() > 0) {
                    serviceItemService.addServiceToBooking(req);
                    count++;
                    bookingId = req.getBookingId();
                }
            }
            AdminBookingDTO updatedBooking = (bookingId != null) ? adminBookingService.getBookingById(bookingId) : null;
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("count", count);
            resp.put("bookingId", bookingId != null ? bookingId : "");
            resp.put("totalAmount", updatedBooking != null ? updatedBooking.getTotalAmount() : 0);
            resp.put("paidAmount", updatedBooking != null ? updatedBooking.getPaidAmount() : 0);
            resp.put("remainingAmount", updatedBooking != null ? updatedBooking.getRemainingAmount() : 0);
            resp.put("serviceAmount", updatedBooking != null ? updatedBooking.getServiceAmount() : 0);
            resp.put("message", "Đã thêm thành công " + count + " món vào phiếu #" + bookingId + "!");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi thêm món: " + e.getMessage()));
        }
    }

    // API Xóa món khỏi phiếu qua AJAX (cho quầy POS siêu tốc)
    @PostMapping("/api/delete-service/{detailId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeServiceAjax(@PathVariable String detailId,
                                                                 @RequestParam(required = false) String bookingId) {
        try {
            serviceItemService.removeServiceFromBooking(detailId);
            AdminBookingDTO updatedBooking = (bookingId != null && !bookingId.isBlank()) ? adminBookingService.getBookingById(bookingId) : null;
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Đã hủy món thành công!");
            resp.put("totalAmount", updatedBooking != null ? updatedBooking.getTotalAmount() : 0);
            resp.put("paidAmount", updatedBooking != null ? updatedBooking.getPaidAmount() : 0);
            resp.put("remainingAmount", updatedBooking != null ? updatedBooking.getRemainingAmount() : 0);
            resp.put("serviceAmount", updatedBooking != null ? updatedBooking.getServiceAmount() : 0);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi xóa món: " + e.getMessage()));
        }
    }

    // API lấy danh sách dịch vụ đã gọi của phiếu
    @GetMapping("/api/services/{bookingId}")
    @ResponseBody
    public ResponseEntity<?> getBookingServicesApi(@PathVariable String bookingId) {
        var list = serviceItemService.getServicesByBookingId(bookingId);
        var resp = list.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("detailId", d.getId());
            map.put("serviceName", d.getServiceItem() != null ? d.getServiceItem().getName() : "N/A");
            map.put("category", d.getServiceItem() != null ? d.getServiceItem().getCategory() : "");
            map.put("unit", d.getServiceItem() != null ? d.getServiceItem().getUnit() : "");
            map.put("quantity", d.getQuantity());
            map.put("unitPrice", d.getUnitPrice());
            map.put("subtotal", d.getSubtotal());
            map.put("note", d.getNote() != null ? d.getNote() : "");
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    // Check-in siêu tốc qua mã QR Code
    @GetMapping("/checkin-qr")
    public String checkInViaQr(@RequestParam("id") String id, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.checkInBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Quét mã QR thành công! Khách hàng đã nhận sân (Phiếu #" + id + " chuyển sang 'Đang sử dụng').");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể nhận sân qua QR: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // 1. Xuất hóa đơn PDF cho phiếu đặt sân (OpenPDF)
    @GetMapping("/export-pdf/{id}")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportPdf(@PathVariable String id) {
        var bis = invoiceExportService.generateBookingPdfInvoice(id);
        var headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=HoaDon_SPORTHUB_" + id + ".pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(new org.springframework.core.io.InputStreamResource(bis));
    }

    // 2. Xuất hóa đơn Excel cho 1 phiếu đặt sân (Apache POI)
    @GetMapping("/export-excel/{id}")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportExcelSingle(@PathVariable String id) {
        var bis = invoiceExportService.generateBookingExcelInvoice(id);
        var headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=HoaDon_SPORTHUB_" + id + ".xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new org.springframework.core.io.InputStreamResource(bis));
    }

    // 3. Xuất danh sách toàn bộ phiếu đặt sân ra Excel (Apache POI)
    @GetMapping({"/export-excel", "/export-excel/all"})
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportExcelList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false, defaultValue = "Tất cả") String status) {
        var list = adminBookingService.getBookings(keyword, status);
        var bis = invoiceExportService.generateBookingsListExcel(list);
        var headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=DanhSach_DatSan_SPORTHUB.xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new org.springframework.core.io.InputStreamResource(bis));
    }

    // =========================================================================
    // FRONT-DESK RECEPTIONIST WORKSTATION APIS
    // =========================================================================

    // 1. Tra cứu siêu tốc cho thanh Lễ tân (Front-Desk Fast Bar)
    @GetMapping("/api/quick-search")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> quickSearchApi(@RequestParam(value = "q", required = false) String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        String cleanQ = query.trim().toLowerCase();
        LocalDate today = LocalDate.now();

        List<AdminBookingDTO> allBookings = adminBookingService.getBookings(null, "Tất cả");
        List<Map<String, Object>> results = allBookings.stream()
                .filter(b -> {
                    boolean matchId = b.getId() != null && b.getId().toLowerCase().contains(cleanQ);
                    boolean matchPhone = b.getCustomerPhone() != null && b.getCustomerPhone().contains(cleanQ);
                    boolean matchName = b.getCustomerName() != null && b.getCustomerName().toLowerCase().contains(cleanQ);
                    return matchId || matchPhone || matchName;
                })
                .sorted((b1, b2) -> {
                    boolean b1Today = today.equals(b1.getBookingDate());
                    boolean b2Today = today.equals(b2.getBookingDate());
                    if (b1Today && !b2Today) return -1;
                    if (!b1Today && b2Today) return 1;
                    return b2.getBookingDate().compareTo(b1.getBookingDate());
                })
                .limit(8)
                .map(b -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", b.getId());
                    m.put("customerName", b.getCustomerName());
                    m.put("customerPhone", b.getCustomerPhone());
                    m.put("courtName", b.getCourtName());
                    m.put("sportName", b.getSportName());
                    m.put("bookingDate", b.getBookingDate() != null ? b.getBookingDate().toString() : "");
                    m.put("isToday", today.equals(b.getBookingDate()));
                    m.put("startTime", b.getStartTime() != null ? b.getStartTime().toString() : "");
                    m.put("endTime", b.getEndTime() != null ? b.getEndTime().toString() : "");
                    m.put("status", b.getStatus());
                    m.put("totalAmount", b.getTotalAmount());
                    m.put("paidAmount", b.getPaidAmount());
                    m.put("remainingAmount", b.getRemainingAmount());
                    m.put("canCheckIn", "Đã xác nhận".equalsIgnoreCase(b.getStatus()) || "Chờ xác nhận".equalsIgnoreCase(b.getStatus()));
                    m.put("isInUse", "Đang sử dụng".equalsIgnoreCase(b.getStatus()));
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(results);
    }

    // 2. Nhận sân siêu tốc 1-click qua AJAX cho Lễ tân
    @PostMapping("/api/checkin/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkInAjax(@PathVariable String id) {
        try {
            adminBookingService.checkInBooking(id);
            AdminBookingDTO updated = adminBookingService.getBookingById(id);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("id", id);
            resp.put("status", updated.getStatus());
            resp.put("customerName", updated.getCustomerName());
            resp.put("courtName", updated.getCourtName());
            resp.put("message", "Nhận sân thành công! Khách " + updated.getCustomerName() + " (" + updated.getCourtName() + ") đã vào thi đấu.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi nhận sân: " + e.getMessage()));
        }
    }

    // 3. Quy trình Đặt Sân Lễ Tân Đa Bước (Front-Desk Booking Wizard)
    @PostMapping("/api/receptionist-create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> receptionistCreateBooking(
            @RequestBody vn.edu.ute.sportute.dto.request.ReceptionistBookingWizardDTO req,
            Authentication authentication) {
        try {
            // 1. Khách hàng: tạo mới ngay nếu là khách lần đầu hoặc khách lẻ
            String customerId = req.getCustomerId();
            if (customerId == null || customerId.isBlank() || "NEW".equalsIgnoreCase(customerId)) {
                String phone = (req.getNewCustomerPhone() != null && !req.getNewCustomerPhone().isBlank()) 
                        ? req.getNewCustomerPhone().trim() 
                        : "09" + String.valueOf(System.currentTimeMillis()).substring(5);
                String name = (req.getNewCustomerName() != null && !req.getNewCustomerName().isBlank()) 
                        ? req.getNewCustomerName().trim() 
                        : "Khách lẻ " + phone;
                String email = (req.getNewCustomerEmail() != null && !req.getNewCustomerEmail().isBlank())
                        ? req.getNewCustomerEmail().trim()
                        : null;
                Customer newCust = adminBookingService.findOrCreateCustomer(phone, name, email, req.getNewCustomerNote());
                customerId = newCust.getId();
            }

            // 2. Trạng thái phiếu
            String status;
            if (req.isAutoCheckIn()) {
                status = "Đang sử dụng"; // Khách vào nhận sân ngay tại quầy
            } else if (req.getPaidAmount() != null && req.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                status = "Đã xác nhận";
            } else {
                status = "Chờ xác nhận";
            }

            AdminBookingDTO dto = AdminBookingDTO.builder()
                    .customerId(customerId)
                    .courtId(req.getCourtId())
                    .bookingDate(req.getBookingDate() != null ? req.getBookingDate() : LocalDate.now())
                    .startTime(req.getStartTime())
                    .endTime(req.getEndTime())
                    .policyId(req.getPolicyId())
                    .promotionId(req.getPromotionId())
                    .totalAmount(req.getTotalAmount() != null ? req.getTotalAmount() : BigDecimal.ZERO)
                    .status(status)
                    .build();

            String employeeAccountId = (authentication != null) ? authentication.getName() : null;
            adminBookingService.saveBooking(dto, employeeAccountId);
            String bookingId = dto.getId();

            // 3. Nếu có dịch vụ F&B / Nước uống đi kèm
            int serviceCount = 0;
            if (req.getServices() != null && !req.getServices().isEmpty()) {
                for (var sReq : req.getServices()) {
                    if (sReq.getQuantity() != null && sReq.getQuantity() > 0) {
                        sReq.setBookingId(bookingId);
                        serviceItemService.addServiceToBooking(sReq);
                        serviceCount++;
                    }
                }
            }

            AdminBookingDTO saved = adminBookingService.getBookingById(bookingId);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("bookingId", bookingId);
            resp.put("customerName", saved.getCustomerName());
            resp.put("customerPhone", saved.getCustomerPhone());
            resp.put("courtName", saved.getCourtName());
            resp.put("bookingDate", saved.getBookingDate().toString());
            resp.put("startTime", saved.getStartTime().toString());
            resp.put("endTime", saved.getEndTime().toString());
            resp.put("status", saved.getStatus());
            resp.put("totalAmount", saved.getTotalAmount());
            resp.put("paidAmount", saved.getPaidAmount());
            resp.put("remainingAmount", saved.getRemainingAmount());
            resp.put("serviceCount", serviceCount);
            resp.put("autoCheckIn", req.isAutoCheckIn());
            resp.put("autoPrint", req.isAutoPrint());
            resp.put("message", req.isAutoCheckIn() 
                    ? "Lập phiếu #" + bookingId + " thành công và khách đã nhận sân thi đấu ngay!" 
                    : "Lập phiếu đặt sân #" + bookingId + " tại quầy thành công!");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi lập phiếu tại quầy: " + e.getMessage()));
        }
    }
}