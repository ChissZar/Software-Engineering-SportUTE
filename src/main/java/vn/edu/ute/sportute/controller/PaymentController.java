package vn.edu.ute.sportute.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.repository.BookingRepository;
import vn.edu.ute.sportute.service.PaymentService;

import java.math.BigDecimal;

/**
 * Quản lý khoản thu, cọc và hoàn tiền gắn với phiếu đặt sân.
 */
@Controller
@RequestMapping("/admin/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;

    public PaymentController(PaymentService paymentService, BookingRepository bookingRepository) {
        this.paymentService = paymentService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public String index(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "type", required = false, defaultValue = "Tất cả") String type,
                        @RequestParam(value = "method", required = false, defaultValue = "Tất cả") String method,
                        Model model) {
        model.addAttribute("payments", paymentService.getAllPayments(keyword, type, method));
        model.addAttribute("stats", paymentService.getPaymentStatistics());
        model.addAttribute("bookings", bookingRepository.findAll());
        model.addAttribute("currentKeyword", keyword != null ? keyword : "");
        model.addAttribute("currentType", type);
        model.addAttribute("currentMethod", method);

        return "payments/index";
    }

    @PostMapping("/record")
    public String recordPayment(@RequestParam("bookingId") String bookingId,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam("transactionType") String transactionType,
                                @RequestParam("paymentMethod") String paymentMethod,
                                RedirectAttributes redirectAttributes) {
        try {
            paymentService.recordPayment(bookingId, amount, transactionType, paymentMethod);
            redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận giao dịch " + transactionType + " thành công cho phiếu #" + bookingId + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể ghi nhận giao dịch: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }
}
