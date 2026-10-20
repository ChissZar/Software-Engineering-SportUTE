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
    @org.springframework.beans.factory.annotation.Autowired
    private IdGenerator idGenerator;


    @org.springframework.beans.factory.annotation.Autowired
    private vn.edu.ute.sportute.service.BookingAmounts amounts;

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
                        if (!transactionType.equalsIgnoreCase(p.getTransactionType()) && !("Thu".equals(transactionType) && "Thanh toán".equals(p.getTransactionType()))) return false;
                    }
                    if (method != null && !method.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(method)) {
                        if (p.getPaymentMethod() == null || !p.getPaymentMethod().startsWith(method)) return false;
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
                .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Payment::getId))
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

        Booking booking = bookingRepository.findForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + bookingId));

        if (!Set.of("Thu", "Cọc", "Hoàn tiền").contains(transactionType) || !Set.of("Tiền mặt", "Chuyển khoản").contains(paymentMethod))
            throw new IllegalArgumentException("Loại giao dịch hoặc phương thức không hợp lệ");
        validateAmount(booking, amount, transactionType, null);
        Payment payment = Payment.builder()
                .id(idGenerator.generateId("GD"))
                .booking(booking)
                .amount(amount)
                .transactionType(transactionType != null ? transactionType : "Thu")
                .paymentMethod(paymentMethod != null ? paymentMethod : "Tiền mặt")
                .build();

        paymentRepository.save(payment);
        booking.getPayments().add(payment);
        confirmBookingAfterReceipt(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatistics() {
        List<Payment> all = paymentRepository.findAll();
        List<Payment> list = all.stream().filter(vn.edu.ute.sportute.service.BookingAmounts::counted).toList();
        Map<String, Object> stats = new HashMap<>();

        BigDecimal totalDeposit = list.stream()
                .filter(p -> "Cọc".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalThu = list.stream()
                .filter(p -> p.getTransactionType() != null &&
                        ("Thu".equalsIgnoreCase(p.getTransactionType()) ||
                         "Thanh toán".equalsIgnoreCase(p.getTransactionType())))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefund = list.stream()
                .filter(p -> "Hoàn tiền".equalsIgnoreCase(p.getTransactionType()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // RB4: Thu ròng = (Thu + Cọc) - Hoàn tiền
        BigDecimal netRevenue = totalThu.add(totalDeposit).subtract(totalRefund);

        stats.put("totalTransactions", (long) all.size());
        stats.put("totalDeposit", totalDeposit);
        stats.put("totalThu", totalThu);
        stats.put("totalRefund", totalRefund);
        stats.put("netRevenue", netRevenue);

        return stats;
    }

    private void validateAmount(Booking b, BigDecimal value, String type, String excludeId) {
        BigDecimal paid = vn.edu.ute.sportute.service.BookingAmounts.paid(b);
        if ("Hoàn tiền".equals(type)) {
            BigDecimal reserved = b.getPayments().stream().filter(p -> "PENDING".equals(p.getPaymentStatus()) && "Hoàn tiền".equals(p.getTransactionType()) && !p.getId().equals(excludeId))
                .map(Payment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            if (value.compareTo(paid.subtract(reserved)) > 0) throw new IllegalArgumentException("Số hoàn vượt số đã thu còn lại");
        } else {
            if ("Đã hủy".equals(b.getStatus())) throw new IllegalArgumentException("Không thể thu tiền phiếu đã hủy");
            BigDecimal reserved = b.getPayments().stream().filter(p -> "PENDING".equals(p.getPaymentStatus()) && !"Hoàn tiền".equals(p.getTransactionType()) && !p.getId().equals(excludeId))
                .map(Payment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            if (value.compareTo(amounts.total(b).subtract(paid).subtract(reserved)) > 0)
                throw new IllegalArgumentException("Số thu vượt khoản còn thiếu hoặc đã có khoản chờ xác nhận. Hãy đối soát khoản chờ trước.");
        }
    }
    private void confirmBookingAfterReceipt(Booking b) {
        BigDecimal rate = b.getBookingPolicy() != null && b.getBookingPolicy().getDepositRate() != null ? b.getBookingPolicy().getDepositRate() : BigDecimal.valueOf(30);
        // Online checkout offers an explicit 30% deposit; the counter uses its selected policy.
        if (b.getEmployee() == null) rate = BigDecimal.valueOf(30);
        BigDecimal required = amounts.total(b).multiply(rate).divide(BigDecimal.valueOf(100),0,java.math.RoundingMode.HALF_UP);
        if ("Chờ xác nhận".equals(b.getStatus()) && vn.edu.ute.sportute.service.BookingAmounts.paid(b).compareTo(required) >= 0) {
            b.setStatus("Đã xác nhận"); bookingRepository.save(b);
        }
    }
    public void confirmPayment(String id, boolean accept) {
        Payment initial = paymentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch"));
        Booking b = bookingRepository.findForUpdate(initial.getBooking().getId()).orElseThrow();
        Payment p = b.getPayments().stream().filter(x -> x.getId().equals(id)).findFirst().orElseThrow();
        if (!"PENDING".equals(p.getPaymentStatus())) throw new IllegalArgumentException("Giao dịch đã được xử lý");
        if (accept) validateAmount(b,p.getAmount(),p.getTransactionType(),id);
        p.setPaymentStatus(accept ? "CONFIRMED" : "VOID");
        paymentRepository.save(p);
        if (accept) confirmBookingAfterReceipt(b);
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
                .status("PENDING".equals(p.getPaymentStatus()) ? "Chờ xác nhận" : "VOID".equals(p.getPaymentStatus()) ? "Đã hủy" : "CONFIRMED".equals(p.getPaymentStatus()) ? "Đã xác nhận" : "Dữ liệu cũ • chưa đối soát")
                .transactionDate(p.getCreatedAt())
                .build();
    }
}
