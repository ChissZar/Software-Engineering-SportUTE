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

    @org.springframework.beans.factory.annotation.Autowired
    private vn.edu.ute.sportute.service.BookingAmounts amounts;

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
                        Model model, jakarta.servlet.http.HttpSession session) {
        model.addAttribute("payments", paymentService.getAllPayments(keyword, type, method));
        model.addAttribute("stats", paymentService.getPaymentStatistics());
        var bookings = bookingRepository.findAll();
        model.addAttribute("bookings", bookings);
        var balances = new java.util.LinkedHashMap<String, java.util.Map<String,Object>>();
        for (var b : bookings) {
            BigDecimal paid = vn.edu.ute.sportute.service.BookingAmounts.paid(b);
            BigDecimal total = amounts.total(b);
            BigDecimal refunded = b.getPayments().stream().filter(vn.edu.ute.sportute.service.BookingAmounts::counted).filter(p -> "Hoàn tiền".equals(p.getTransactionType())).map(vn.edu.ute.sportute.entity.Payment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal deposit = b.getPayments().stream().filter(vn.edu.ute.sportute.service.BookingAmounts::counted).filter(p -> "Cọc".equals(p.getTransactionType())).map(vn.edu.ute.sportute.entity.Payment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            balances.put(b.getId(), java.util.Map.of("total",total,"paid",paid.add(refunded),"deposit",deposit,"refunded",refunded,"remaining","Đã hủy".equals(b.getStatus()) ? BigDecimal.ZERO : total.subtract(paid).max(BigDecimal.ZERO)));
        }
        model.addAttribute("balances", balances);
        String token = java.util.UUID.randomUUID().toString();
        session.setAttribute("paymentToken", token);
        model.addAttribute("paymentToken", token);
        model.addAttribute("currentKeyword", keyword != null ? keyword : "");
        model.addAttribute("currentType", type);
        model.addAttribute("currentMethod", method);

        return "payments/index";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable String id, @RequestParam boolean accept, RedirectAttributes flash) {
        try { paymentService.confirmPayment(id, accept); flash.addFlashAttribute("successMessage", "Đã cập nhật đối soát giao dịch"); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/admin/payments";
    }

    @PostMapping("/record")
    public String recordPayment(@RequestParam("bookingId") String bookingId,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam("transactionType") String transactionType,
                                @RequestParam("paymentMethod") String paymentMethod,
                                @RequestParam("requestToken") String requestToken, jakarta.servlet.http.HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            synchronized (session) {
                if (!requestToken.equals(session.getAttribute("paymentToken"))) throw new IllegalArgumentException("Yêu cầu đã xử lý hoặc hết hiệu lực. Vui lòng tải lại trang.");
                paymentService.recordPayment(bookingId, amount, transactionType, paymentMethod);
                session.removeAttribute("paymentToken");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận giao dịch " + transactionType + " thành công cho phiếu #" + bookingId + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể ghi nhận giao dịch: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }
}
