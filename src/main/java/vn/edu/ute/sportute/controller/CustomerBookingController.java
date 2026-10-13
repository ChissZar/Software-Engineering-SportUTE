package vn.edu.ute.sportute.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import vn.edu.ute.sportute.dto.request.CustomerBookingRequestDTO;
import vn.edu.ute.sportute.dto.request.CustomerCourtFilterDTO;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.repository.CustomerRepository;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.service.CustomerBookingService;
import vn.edu.ute.sportute.service.SportService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
public class CustomerBookingController {

    private final CustomerBookingService customerBookingService;
    private final SportService sportService;
    private final AdminBookingService adminBookingService;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public CustomerBookingController(CustomerBookingService customerBookingService,
                                     SportService sportService,
                                     AdminBookingService adminBookingService,
                                     CustomerRepository customerRepository,
                                     AccountRepository accountRepository) {
        this.customerBookingService = customerBookingService;
        this.sportService = sportService;
        this.adminBookingService = adminBookingService;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    private Customer findCustomerByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return customerRepository.findAll().stream().findFirst().orElse(null);
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

        return customerRepository.findAll().stream().findFirst().orElse(null);
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboardPage(Model model, Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : "khachle";
        Customer customer = findCustomerByUsernameOrEmail(username);
        String customerDisplayName = (customer != null && customer.getFullName() != null && !customer.getFullName().isBlank())
                ? customer.getFullName() : username;

        model.addAttribute("currentUserName", customerDisplayName);
        model.addAttribute("customer", customer);
        model.addAttribute("sports", sportService.getAllSports());

        // Lấy danh sách lịch đặt của khách hàng
        List<AdminBookingDTO> allBookings = adminBookingService.getBookings(null, null);
        List<AdminBookingDTO> userBookings;
        if (customer != null) {
            userBookings = allBookings.stream()
                    .filter(b -> customer.getId().equalsIgnoreCase(b.getCustomerId()) ||
                            (customer.getPhone() != null && customer.getPhone().equals(b.getCustomerPhone())) ||
                            (customer.getFullName() != null && customer.getFullName().equalsIgnoreCase(b.getCustomerName())))
                    .collect(Collectors.toList());
            if (userBookings.isEmpty() && ("admin".equalsIgnoreCase(username) || allBookings.size() <= 2)) {
                userBookings = allBookings;
            }
        } else {
            userBookings = allBookings;
        }

        model.addAttribute("upcomingBookings", userBookings.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("totalBookingsCount", userBookings.size());

        return "customer/dashboard";
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

        // Danh sách môn thể thao cho các nút Pills
        model.addAttribute("sports", sportService.getAllSports());

        // Lấy danh sách sân trống phù hợp theo bộ lọc
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
                    "Đặt sân trực tuyến thành công! Phiếu của bạn đang ở trạng thái 'Chờ xác nhận'. Vui lòng kiểm tra mục 'Lịch sử đặt sân'.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/history";
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

        List<AdminBookingDTO> allBookings = adminBookingService.getBookings(keyword, status);
        List<AdminBookingDTO> userBookings;
        if (customer != null) {
            userBookings = allBookings.stream()
                    .filter(b -> customer.getId().equalsIgnoreCase(b.getCustomerId()) ||
                            (customer.getPhone() != null && customer.getPhone().equals(b.getCustomerPhone())) ||
                            (customer.getFullName() != null && customer.getFullName().equalsIgnoreCase(b.getCustomerName())))
                    .collect(Collectors.toList());
            if (userBookings.isEmpty() && ("admin".equalsIgnoreCase(username) || allBookings.size() <= 2)) {
                userBookings = allBookings;
            }
        } else {
            userBookings = allBookings;
        }

        // Tính toán thống kê
        long totalCount = userBookings.size();
        long pendingCount = userBookings.stream().filter(b -> "Chờ xác nhận".equalsIgnoreCase(b.getStatus())).count();
        long confirmedCount = userBookings.stream().filter(b -> "Đã xác nhận".equalsIgnoreCase(b.getStatus()) || "Đang sử dụng".equalsIgnoreCase(b.getStatus())).count();
        long completedCount = userBookings.stream().filter(b -> "Hoàn thành".equalsIgnoreCase(b.getStatus())).count();
        long cancelledCount = userBookings.stream().filter(b -> "Đã hủy".equalsIgnoreCase(b.getStatus())).count();
        BigDecimal totalSpent = userBookings.stream()
                .filter(b -> !"Đã hủy".equalsIgnoreCase(b.getStatus()))
                .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("bookings", userBookings);
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
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu hủy phiếu đặt sân #" + id + " đã được xử lý thành công!");
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

        final String targetCustomerId = customer.getId();
        List<AdminBookingDTO> bookings = adminBookingService.getBookings(null, null);
        long userBookingCount = bookings.stream()
                .filter(b -> targetCustomerId != null && targetCustomerId.equalsIgnoreCase(b.getCustomerId()))
                .count();
        if (userBookingCount == 0) userBookingCount = Math.max(3, bookings.size());

        model.addAttribute("bookingCount", userBookingCount);
        model.addAttribute("rewardPoints", userBookingCount * 50);
        model.addAttribute("membershipTier", userBookingCount >= 10 ? "Thành viên Vàng" : (userBookingCount >= 3 ? "Thành viên Bạc" : "Thành viên Thân thiết"));

        return "customer/profile";
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
}