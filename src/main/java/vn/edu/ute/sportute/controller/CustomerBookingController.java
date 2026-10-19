package vn.edu.ute.sportute.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import vn.edu.ute.sportute.dto.request.CustomerBookingRequestDTO;
import vn.edu.ute.sportute.dto.request.CustomerCourtFilterDTO;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.service.CustomerBookingService;
import vn.edu.ute.sportute.service.SportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
public class CustomerBookingController {

    private final CustomerBookingService customerBookingService;
    private final SportService sportService;
    private final AdminBookingService adminBookingService;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final CourtRepository courtRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PriceRepository priceRepository;
    private final vn.edu.ute.sportute.service.InvoiceExportService invoiceExportService;
    private final vn.edu.ute.sportute.service.CustomerUserDataService customerUserDataService;

    public CustomerBookingController(CustomerBookingService customerBookingService,
                                     SportService sportService,
                                     AdminBookingService adminBookingService,
                                     CustomerRepository customerRepository,
                                     AccountRepository accountRepository,
                                     CourtRepository courtRepository,
                                     BookingDetailRepository bookingDetailRepository,
                                     MaintenanceRepository maintenanceRepository,
                                     PriceRepository priceRepository,
                                     vn.edu.ute.sportute.service.InvoiceExportService invoiceExportService,
                                     vn.edu.ute.sportute.service.CustomerUserDataService customerUserDataService) {
        this.customerBookingService = customerBookingService;
        this.sportService = sportService;
        this.adminBookingService = adminBookingService;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.courtRepository = courtRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.priceRepository = priceRepository;
        this.invoiceExportService = invoiceExportService;
        this.customerUserDataService = customerUserDataService;
    }

    private Customer findCustomerByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return null;
        }
        Optional<Account> accountOpt = accountRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (accountOpt.isPresent()) {
            Optional<Customer> custOpt = customerRepository.findByAccount_Id(accountOpt.get().getId());
            if (custOpt.isPresent()) {
                return custOpt.get();
            }
        }
        Optional<Customer> byEmail = customerRepository.findByEmail(usernameOrEmail);
        if (byEmail.isPresent()) return byEmail.get();

        Optional<Customer> byPhone = customerRepository.findByPhone(usernameOrEmail);
        if (byPhone.isPresent()) return byPhone.get();

        return null;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboardPage(@ModelAttribute("filter") CustomerCourtFilterDTO filter,
                                Model model,
                                Authentication authentication) {
        return bookingPage(filter, model, authentication);
    }

    @GetMapping("/booking")
    public String bookingPage(@ModelAttribute("filter") CustomerCourtFilterDTO filter,
                              Model model,
                              Authentication authentication) {
        if (filter == null) {
            filter = new CustomerCourtFilterDTO();
        }

        String username = (authentication != null) ? authentication.getName() : "khachle";
        Customer customer = findCustomerByUsernameOrEmail(username);
        String customerDisplayName = (customer != null && customer.getFullName() != null && !customer.getFullName().isBlank())
                ? customer.getFullName() : username;

        model.addAttribute("currentUserName", customerDisplayName);
        model.addAttribute("customer", customer);

        // Thông tin hạng thành viên và tỷ lệ giảm giá theo điểm tích lũy
        CustomerTierInfo tierInfo = getCustomerTierInfo(customer);
        model.addAttribute("tierInfo", tierInfo);
        model.addAttribute("membershipTier", tierInfo.getTierName());
        model.addAttribute("tierDiscountPercent", tierInfo.getDiscountPercent());
        model.addAttribute("rewardPoints", tierInfo.getPoints());

        // Danh sách môn thể thao cho các nút Pills
        model.addAttribute("sports", sportService.getAllSports());

        // Danh sách tất cả sân hoạt động để hiển thị sân con và chuyển đổi
        model.addAttribute("allCourts", courtRepository.findAll());

        // Lấy danh sách sân trống phù hợp theo bộ lọc
        model.addAttribute("availableCourts", customerBookingService.searchAvailableCourts(filter));
        model.addAttribute("filter", filter);
        model.addAttribute("bookingRequest", new CustomerBookingRequestDTO());

        // Danh sách sân yêu thích lấy trực tiếp từ Server theo mã khách hàng (Customer ID bất biến)
        List<String> userFavorites = (customer != null && customer.getId() != null)
                ? customerUserDataService.getFavorites(customer.getId())
                : Collections.emptyList();
        model.addAttribute("userFavorites", userFavorites);
        model.addAttribute("customerId", customer != null ? customer.getId() : "");

        return "customer/booking";
    }

    @GetMapping("/booking/api/court-slots")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourtSlots(
            @RequestParam("courtId") String courtId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        Court court = courtRepository.findById(courtId).orElse(null);
        if (court == null) {
            return ResponseEntity.notFound().build();
        }

        BigDecimal hourlyRate = BigDecimal.valueOf(100000);
        var effectivePrice = priceRepository.findEffectivePrice(courtId, targetDate);
        if (effectivePrice.isPresent() && !effectivePrice.get().getPriceDetails().isEmpty()) {
            hourlyRate = effectivePrice.get().getPriceDetails().get(0).getPriceAmount();
        } else if (court.getSport() != null) {
            String mId = court.getSport().getId();
            if ("M_BD".equalsIgnoreCase(mId)) hourlyRate = BigDecimal.valueOf(100000);
            else if ("M_CL".equalsIgnoreCase(mId)) hourlyRate = BigDecimal.valueOf(120000);
            else if ("M_BR".equalsIgnoreCase(mId)) hourlyRate = BigDecimal.valueOf(150000);
            else if ("M_PB".equalsIgnoreCase(mId)) hourlyRate = BigDecimal.valueOf(120000);
        }

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);

        List<BookingDetail> details = bookingDetailRepository.findAll().stream()
                .filter(d -> d.getCourt() != null && d.getCourt().getId().equals(courtId))
                .filter(d -> d.getStartTime() != null && d.getStartTime().toLocalDate().equals(targetDate))
                .filter(d -> d.getBooking() != null && !"Đã hủy".equalsIgnoreCase(d.getBooking().getStatus()))
                .collect(Collectors.toList());

        List<Maintenance> maints = maintenanceRepository.findAll().stream()
                .filter(m -> m.getCourt() != null && m.getCourt().getId().equals(courtId))
                .filter(m -> m.getStartDate().isBefore(endOfDay) && m.getEndDate().isAfter(startOfDay))
                .collect(Collectors.toList());

        // 30-minute intervals from 05:00 to 23:00
        List<String> bookedSlots = new ArrayList<>();
        LocalTime current = LocalTime.of(5, 0);
        LocalTime closeTime = LocalTime.of(23, 0);

        while (!current.isAfter(closeTime.minusMinutes(30))) {
            LocalDateTime slotStart = LocalDateTime.of(targetDate, current);
            LocalDateTime slotEnd = slotStart.plusMinutes(30);
            String slotStr = String.format("%02d:%02d", current.getHour(), current.getMinute());

            boolean isBooked = details.stream()
                    .anyMatch(d -> d.getStartTime().isBefore(slotEnd) && d.getEndTime().isAfter(slotStart));
            boolean isMaint = maints.stream()
                    .anyMatch(m -> m.getStartDate().isBefore(slotEnd) && m.getEndDate().isAfter(slotStart));

            if (isBooked || isMaint) {
                bookedSlots.add(slotStr);
            }

            current = current.plusMinutes(30);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("courtId", court.getId());
        resp.put("courtName", court.getName());
        resp.put("sportId", court.getSport() != null ? court.getSport().getId() : "");
        resp.put("sportName", court.getSport() != null ? court.getSport().getName() : "");
        resp.put("location", (court.getLocation() != null && !court.getLocation().isBlank()) 
                ? court.getLocation() : "SportUTE - Cơ sở Võ Văn Ngân");
        resp.put("hourlyRate", hourlyRate);
        resp.put("date", targetDate.toString());
        resp.put("bookedSlots", bookedSlots);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/booking/confirm")
    public String confirmBooking(@ModelAttribute CustomerBookingRequestDTO request,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            String username = (authentication != null) ? authentication.getName() : "khachle";
            customerBookingService.bookCourtOnline(request, username);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Đặt sân trực tuyến thành công! Phiếu của bạn đã được xác nhận giữ chỗ ('Đã xác nhận'). Vui lòng kiểm tra mục 'Lịch sử đặt sân'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/history";
    }

    @PostMapping(value = "/booking/api/confirm", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmBookingApiJson(
            @RequestBody CustomerBookingRequestDTO request,
            Authentication authentication) {
        return handleConfirmBooking(request, authentication);
    }

    @PostMapping(value = "/booking/api/confirm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmBookingApi(
            @ModelAttribute CustomerBookingRequestDTO request,
            Authentication authentication) {
        return handleConfirmBooking(request, authentication);
    }

    private ResponseEntity<Map<String, Object>> handleConfirmBooking(
            CustomerBookingRequestDTO request,
            Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String username = (authentication != null) ? authentication.getName() : "khachle";
            vn.edu.ute.sportute.entity.Booking booking = customerBookingService.bookCourtOnline(request, username);
            resp.put("success", true);
            resp.put("bookingId", booking.getId());
            resp.put("courtId", request.getCourtId());
            resp.put("bookingDate", request.getBookingDate() != null ? request.getBookingDate().toString() : "");
            resp.put("startTime", request.getStartTime() != null ? request.getStartTime().toString() : "");
            resp.put("durationMinutes", request.getDurationMinutes());
            resp.put("totalAmount", request.getTotalAmount());
            resp.put("depositChoice", request.getDepositChoice());
            resp.put("message", "Đặt sân trực tuyến thành công!");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @GetMapping("/history")
    public String historyPage(@RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "keyword", required = false) String keyword,
                              Model model,
                              Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : "khachle";
        Customer customer = findCustomerByUsernameOrEmail(username);
        String customerDisplayName = (customer != null && customer.getFullName() != null && !customer.getFullName().isBlank())
                ? customer.getFullName() : username;

        model.addAttribute("currentUserName", customerDisplayName);
        model.addAttribute("customer", customer);

        String searchStatus = status;
        if ("Hoàn thành".equalsIgnoreCase(status) || "Hoàn tất".equalsIgnoreCase(status)) {
            searchStatus = "Hoàn tất";
        }

        // 1. Lấy toàn bộ lịch sử đặt sân của khách hàng (không bị phụ thuộc vào tab đang lọc) để tính đúng số lượng từng trạng thái
        List<AdminBookingDTO> rawAllBookings = adminBookingService.getBookings(null, "Tất cả");
        List<AdminBookingDTO> allCustomerBookings;
        if (customer != null) {
            allCustomerBookings = rawAllBookings.stream()
                    .filter(b -> customer.getId().equalsIgnoreCase(b.getCustomerId()) ||
                            (customer.getPhone() != null && customer.getPhone().equals(b.getCustomerPhone())) ||
                            (customer.getFullName() != null && customer.getFullName().equalsIgnoreCase(b.getCustomerName())))
                    .collect(Collectors.toList());
            if (allCustomerBookings.isEmpty() && ("admin".equalsIgnoreCase(username) || rawAllBookings.size() <= 2)) {
                allCustomerBookings = rawAllBookings;
            }
        } else {
            allCustomerBookings = rawAllBookings;
        }

        // Tính toán thống kê trên toàn bộ lịch sử đặt sân của khách hàng
        long totalCount = allCustomerBookings.size();
        long pendingCount = allCustomerBookings.stream().filter(b -> "Chờ xác nhận".equalsIgnoreCase(b.getStatus())).count();
        long confirmedCount = allCustomerBookings.stream().filter(b -> "Đã xác nhận".equalsIgnoreCase(b.getStatus()) || "Đang sử dụng".equalsIgnoreCase(b.getStatus())).count();
        long completedCount = allCustomerBookings.stream().filter(b -> "Hoàn tất".equalsIgnoreCase(b.getStatus()) || "Hoàn thành".equalsIgnoreCase(b.getStatus())).count();
        long cancelledCount = allCustomerBookings.stream().filter(b -> "Đã hủy".equalsIgnoreCase(b.getStatus())).count();
        BigDecimal totalSpent = allCustomerBookings.stream()
                .filter(b -> !"Đã hủy".equalsIgnoreCase(b.getStatus()))
                .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Lọc danh sách hiển thị theo trạng thái tab được chọn và từ khóa tìm kiếm
        final String finalSearchStatus = searchStatus;
        List<AdminBookingDTO> displayedBookings = allCustomerBookings.stream()
                .filter(b -> {
                    if (finalSearchStatus != null && !finalSearchStatus.isBlank() && !"Tất cả".equalsIgnoreCase(finalSearchStatus)) {
                        if ("Hoàn tất".equalsIgnoreCase(finalSearchStatus)) {
                            if (!"Hoàn tất".equalsIgnoreCase(b.getStatus()) && !"Hoàn thành".equalsIgnoreCase(b.getStatus())) return false;
                        } else if ("Đã xác nhận".equalsIgnoreCase(finalSearchStatus)) {
                            if (!"Đã xác nhận".equalsIgnoreCase(b.getStatus()) && !"Đang sử dụng".equalsIgnoreCase(b.getStatus())) return false;
                        } else {
                            if (!finalSearchStatus.equalsIgnoreCase(b.getStatus())) return false;
                        }
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchId = b.getId() != null && b.getId().toLowerCase().contains(kw);
                        boolean matchCourt = b.getCourtName() != null && b.getCourtName().toLowerCase().contains(kw);
                        return matchId || matchCourt;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        model.addAttribute("bookings", displayedBookings);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("selectedStatus", (status != null && !status.isEmpty()) ? status : "Tất cả");
        model.addAttribute("keyword", keyword);

        return "customer/history";
    }

    @PostMapping("/history/cancel/{id}")
    public String cancelBooking(@PathVariable("id") String id,
                                @RequestParam(value = "reason", required = false, defaultValue = "Khách hàng tự hủy trên website") String reason,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = (authentication != null) ? authentication.getName() : "khachle";
            customerBookingService.cancelBookingByCustomer(id, username, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu hủy phiếu đặt sân #" + id + " đã được xử lý thành công! Số tiền hoàn lại đã được ghi nhận theo chính sách của trung tâm.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy phiếu đặt sân: " + e.getMessage());
        }
        return "redirect:/customer/history";
    }

    @GetMapping("/profile")
    public String profilePage(Model model, Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : "khachle";
        Customer customer = findCustomerByUsernameOrEmail(username);
        if (customer == null) {
            customer = Customer.builder()
                    .id("KH001")
                    .fullName(username)
                    .phone("0912345678")
                    .email(username.contains("@") ? username : username + "@sporthub.vn")
                    .address("Số 1 Võ Văn Ngân, TP. Thủ Đức, TP. Hồ Chí Minh")
                    .status("Hoạt động")
                    .build();
        }

        String customerDisplayName = (customer.getFullName() != null && !customer.getFullName().isBlank())
                ? customer.getFullName() : username;

        model.addAttribute("currentUserName", customerDisplayName);
        model.addAttribute("customer", customer);

        CustomerTierInfo tierInfo = getCustomerTierInfo(customer);
        model.addAttribute("tierInfo", tierInfo);
        model.addAttribute("bookingCount", tierInfo.getBookingCount());
        model.addAttribute("totalSpent", tierInfo.getTotalSpent());
        model.addAttribute("rewardPoints", tierInfo.getPoints());
        model.addAttribute("membershipTier", tierInfo.getTierName());
        model.addAttribute("tierDiscountPercent", tierInfo.getDiscountPercent());

        return "customer/profile";
    }

    public static class CustomerTierInfo {
        private String tierKey;
        private String tierName;
        private int discountPercent;
        private int points;
        private long bookingCount;
        private BigDecimal totalSpent = BigDecimal.ZERO;
        private String nextTierGoal;
        private int nextTierPoints;
        private int progressPercent;
        private int pointsNeeded;
        private String cardGradient;
        private String cardBadgeClass;

        public String getTierKey() { return tierKey; }
        public String getTierName() { return tierName; }
        public int getDiscountPercent() { return discountPercent; }
        public int getPoints() { return points; }
        public long getBookingCount() { return bookingCount; }
        public BigDecimal getTotalSpent() { return totalSpent; }
        public String getNextTierGoal() { return nextTierGoal; }
        public int getNextTierPoints() { return nextTierPoints; }
        public int getProgressPercent() { return progressPercent; }
        public int getPointsNeeded() { return pointsNeeded; }
        public String getCardGradient() { return cardGradient; }
        public String getCardBadgeClass() { return cardBadgeClass; }
    }

    private CustomerTierInfo getCustomerTierInfo(Customer customer) {
        CustomerTierInfo info = new CustomerTierInfo();
        long bookingCount = 0;
        BigDecimal totalSpent = BigDecimal.ZERO;
        if (customer != null && customer.getId() != null) {
            String targetId = customer.getId();
            List<AdminBookingDTO> bookings = adminBookingService.getBookings(null, null).stream()
                    .filter(b -> targetId.equalsIgnoreCase(b.getCustomerId()) ||
                            (customer.getPhone() != null && customer.getPhone().equals(b.getCustomerPhone())))
                    .filter(b -> !"Đã hủy".equalsIgnoreCase(b.getStatus()))
                    .collect(Collectors.toList());
            bookingCount = bookings.size();
            for (AdminBookingDTO b : bookings) {
                if (b.getTotalAmount() != null) {
                    totalSpent = totalSpent.add(b.getTotalAmount());
                }
            }
        }

        info.bookingCount = bookingCount;
        info.totalSpent = totalSpent;

        // Điểm tích lũy tính tự động từ CSDL thật: 10.000 VNĐ chi tiêu = 1 điểm thưởng
        int points = totalSpent.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).intValue();
        info.points = points;

        if (points < 200) {
            info.tierKey = "BRONZE";
            info.tierName = "Thành viên Đồng";
            info.discountPercent = 0;
            info.nextTierGoal = "Hạng Bạc";
            info.nextTierPoints = 200;
            info.progressPercent = Math.min(100, Math.max(8, (points * 100) / 200));
            info.pointsNeeded = 200 - points;
            info.cardGradient = "linear-gradient(135deg, #78350f 0%, #b45309 50%, #92400e 100%)";
            info.cardBadgeClass = "bronze";
        } else if (points < 500) {
            info.tierKey = "SILVER";
            info.tierName = "Thành viên Bạc";
            info.discountPercent = 5;
            info.nextTierGoal = "Hạng Vàng";
            info.nextTierPoints = 500;
            info.progressPercent = Math.min(100, Math.max(8, ((points - 200) * 100) / 300));
            info.pointsNeeded = 500 - points;
            info.cardGradient = "linear-gradient(135deg, #1e293b 0%, #334155 50%, #475569 100%)";
            info.cardBadgeClass = "silver";
        } else if (points < 1000) {
            info.tierKey = "GOLD";
            info.tierName = "Thành viên Vàng";
            info.discountPercent = 10;
            info.nextTierGoal = "Hạng Kim Cương";
            info.nextTierPoints = 1000;
            info.progressPercent = Math.min(100, Math.max(8, ((points - 500) * 100) / 500));
            info.pointsNeeded = 1000 - points;
            info.cardGradient = "linear-gradient(135deg, #78350f 0%, #d97706 50%, #f59e0b 100%)";
            info.cardBadgeClass = "gold";
        } else {
            info.tierKey = "DIAMOND";
            info.tierName = "Thành viên Kim Cương";
            info.discountPercent = 15;
            info.nextTierGoal = "Đã đạt đỉnh cấp";
            info.nextTierPoints = 1000;
            info.progressPercent = 100;
            info.pointsNeeded = 0;
            info.cardGradient = "linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #2563eb 100%)";
            info.cardBadgeClass = "diamond";
        }
        return info;
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam("phone") String phone,
                                @RequestParam("email") String email,
                                @RequestParam("address") String address,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = (authentication != null) ? authentication.getName() : "khachle";
            Customer customer = findCustomerByUsernameOrEmail(username);
            if (customer != null) {
                customer.setFullName(fullName);
                customer.setPhone(phone);
                customer.setEmail(email);
                customer.setAddress(address);
                customerRepository.save(customer);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin hồ sơ cá nhân thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu thông tin: " + e.getMessage());
        }
        return "redirect:/customer/profile";
    }

    // Tải hóa đơn PDF cho khách hàng
    @GetMapping("/booking/export-pdf/{id}")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportCustomerPdf(@PathVariable String id) {
        var bis = invoiceExportService.generateBookingPdfInvoice(id);
        var headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=HoaDon_SPORTHUB_" + id + ".pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(new org.springframework.core.io.InputStreamResource(bis));
    }

    // =========================================================================
    // SERVER-SIDE USER DATA APIs: FAVORITES & AVATAR PERSISTENCE
    // =========================================================================

    @GetMapping("/booking/api/favorites")
    @ResponseBody
    public ResponseEntity<List<String>> getCustomerFavorites(Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : null;
        Customer customer = findCustomerByUsernameOrEmail(username);
        if (customer == null || customer.getId() == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(customerUserDataService.getFavorites(customer.getId()));
    }

    @PostMapping("/booking/api/favorites/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleCustomerFavorite(
            @RequestParam("courtId") String courtId,
            Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : null;
        Customer customer = findCustomerByUsernameOrEmail(username);
        if (customer == null || customer.getId() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng đăng nhập để lưu sân yêu thích!"));
        }
        List<String> updatedFavorites = customerUserDataService.toggleFavorite(customer.getId(), courtId);
        boolean isFav = updatedFavorites.contains(courtId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "courtId", courtId,
                "isFavorite", isFav,
                "favorites", updatedFavorites
        ));
    }

    @PostMapping("/api/avatar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : null;
        Customer customer = findCustomerByUsernameOrEmail(username);
        if (customer == null || customer.getId() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Chưa xác thực người dùng!"));
        }
        String dataUrl = payload.get("dataUrl");
        if (dataUrl == null || dataUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Dữ liệu ảnh không hợp lệ!"));
        }
        String effectiveUrl = customerUserDataService.saveAvatar(customer.getId(), dataUrl);
        if (effectiveUrl != null) {
            return ResponseEntity.ok(Map.of("success", true, "avatarUrl", effectiveUrl));
        } else {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Không thể lưu ảnh!"));
        }
    }

    @GetMapping(value = "/api/avatar/{customerId}")
    @ResponseBody
    public ResponseEntity<byte[]> getCustomerAvatar(@PathVariable("customerId") String customerId) {
        byte[] bytes = customerUserDataService.getAvatarBytes(customerId);
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noCache())
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }
}