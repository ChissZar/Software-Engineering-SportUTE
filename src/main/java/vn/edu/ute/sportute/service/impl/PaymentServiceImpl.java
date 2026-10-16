package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.response.PaymentDTO;
import vn.edu.ute.sportute.entity.Booking;
import vn.edu.ute.sportute.entity.Payment;
import vn.edu.ute.sportute.repository.BookingRepository;
import vn.edu.ute.sportute.repository.PaymentRepository;
import vn.edu.ute.sportute.service.PaymentService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDTO> getAllPayments(String keyword, String transactionType, String method) {
        List<Payment> list = paymentRepository.findAll();

        return list.stream()
                .filter(p -> {
                    if (transactionType != null && !transactionType.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(transactionType)) {
                        if (!transactionType.equalsIgnoreCase(p.getTransactionType())) return false;
                    }
                    if (method != null && !method.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(method)) {
                        if (!method.equalsIgnoreCase(p.getPaymentMethod())) return false;
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchId = p.getId() != null && p.getId().toLowerCase().contains(kw);
                        boolean matchBooking = p.getBooking() != null && p.getBooking().getId().toLowerCase().contains(kw);
                        boolean matchCust = p.getBooking() != null && p.getBooking().getCustomer() != null &&
                                p.getBooking().getCustomer().getFullName().toLowerCase().contains(kw);
                        return matchId || matchBooking || matchCust;
                    }
                    return true;
                })
                .sorted((p1, p2) -> p2.getId().compareTo(p1.getId()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(String id) {
        Payment p = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán: " + id));
        return mapToDTO(p);
    }

    @Override
    public void recordPayment(String bookingId, BigDecimal amount, String transactionType, String paymentMethod) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + bookingId));

        Payment payment = Payment.builder()
                .id(IdGenerator.generateId("GD"))
                .booking(booking)
                .amount(amount)
                .transactionType(transactionType != null ? transactionType : "Thu")
                .paymentMethod(paymentMethod != null ? paymentMethod : "Tiền mặt")
                .build();

        paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatistics() {
        List<Payment> list = paymentRepository.findAll();
        Map<String, Object> stats = new HashMap<>();

        BigDecimal totalDeposit = list.stream()
                .filter(p -> "Cọc".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalThu = list.stream()
                .filter(p -> "Thu".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefund = list.stream()
                .filter(p -> "Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // RB4: Thu ròng = (Thu + Cọc) - Hoàn tiền
        BigDecimal netRevenue = totalThu.add(totalDeposit).subtract(totalRefund);

        stats.put("totalTransactions", (long) list.size());
        stats.put("totalDeposit", totalDeposit);
        stats.put("totalThu", totalThu);
        stats.put("totalRefund", totalRefund);
        stats.put("netRevenue", netRevenue);

        return stats;
    }

    private PaymentDTO mapToDTO(Payment p) {
        Booking b = p.getBooking();
        String custName = "N/A";
        String custPhone = "";
        String courtName = "N/A";

        if (b != null) {
            if (b.getCustomer() != null) {
                custName = b.getCustomer().getFullName();
                custPhone = b.getCustomer().getPhone();
            }
            if (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty()) {
                courtName = b.getBookingDetails().get(0).getCourt().getName();
            }
        }

        return PaymentDTO.builder()
                .id(p.getId())
                .bookingId(b != null ? b.getId() : "N/A")
                .customerName(custName)
                .customerPhone(custPhone)
                .courtName(courtName)
                .transactionType(p.getTransactionType() != null ? p.getTransactionType() : "Thu")
                .amount(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod() : "Tiền mặt")
                .status("Thành công")
                .transactionDate(LocalDateTime.now())
                .build();
    }
}
