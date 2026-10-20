package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.response.PaymentDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PaymentService {
    List<PaymentDTO> getAllPayments(String keyword, String transactionType, String method);
    PaymentDTO getPaymentById(String id);
    void recordPayment(String bookingId, BigDecimal amount, String transactionType, String paymentMethod);
    Map<String, Object> getPaymentStatistics();
    void confirmPayment(String id, boolean accept);
}
