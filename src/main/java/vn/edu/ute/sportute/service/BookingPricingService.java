package vn.edu.ute.sportute.service;

import java.math.*;
import java.time.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import vn.edu.ute.sportute.entity.*;
import vn.edu.ute.sportute.repository.*;

@Service
@RequiredArgsConstructor
public class BookingPricingService {
    private final PriceRepository prices;
    private final CourtRepository courts;
    private final PromotionRepository promotions;

    public BigDecimal hourlyRate(String courtId, LocalDate date) {
        var price = prices.findEffectivePrice(courtId, date);
        if (price.isPresent() && !price.get().getPriceDetails().isEmpty())
            return price.get().getPriceDetails().get(0).getPriceAmount();
        Court court = courts.findById(courtId).orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại"));
        String sport = court.getSport() == null ? "" : court.getSport().getId();
        return switch (sport) {
            case "M_BD" -> BigDecimal.valueOf(350000);
            case "M_CL", "M_PB" -> BigDecimal.valueOf(120000);
            case "M_BR" -> BigDecimal.valueOf(200000);
            default -> BigDecimal.valueOf(150000);
        };
    }
    public Promotion promotion(String code) {
        if (code == null || code.isBlank()) return null;
        Promotion p = promotions.findAll().stream()
            .filter(x -> x.getId().equalsIgnoreCase(code.trim()))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại"));
        LocalDate now = LocalDate.now();
        if ((p.getFromDate() != null && now.isBefore(p.getFromDate())) ||
            (p.getToDate() != null && now.isAfter(p.getToDate())))
            throw new IllegalArgumentException("Mã khuyến mãi chưa có hiệu lực hoặc đã hết hạn");
        return p;
    }
    public BigDecimal discount(Promotion p, BigDecimal raw) {
        if (p == null) return BigDecimal.ZERO;
        BigDecimal value = p.getDiscountAmount();
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("Giá trị giảm không hợp lệ");
        if ("PERCENT".equals(p.getDiscountType())) {
            if (value.compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("Tỷ lệ giảm không vượt 100%");
            value = raw.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
        return value.min(raw);
    }
    public Quote quote(String court, LocalDate date, LocalTime start, LocalTime end, String code) {
        long minutes = Duration.between(start,end).toMinutes();
        if (minutes < 30 || minutes % 30 != 0 || start.getMinute() % 30 != 0 || start.getSecond() != 0 || end.getSecond() != 0)
            throw new IllegalArgumentException("Chọn thời lượng theo bước 30 phút, tối thiểu 30 phút");
        if (start.isBefore(LocalTime.of(5,0)) || end.isAfter(LocalTime.of(23,0)))
            throw new IllegalArgumentException("Giờ đặt sân phải trong khoảng 05:00–23:00");
        BigDecimal rate = hourlyRate(court,date);
        BigDecimal raw = rate.multiply(BigDecimal.valueOf(minutes)).divide(BigDecimal.valueOf(60),0,RoundingMode.HALF_UP);
        Promotion p = promotion(code);
        BigDecimal discount = discount(p,raw);
        return new Quote(rate,raw,discount,raw.subtract(discount),p);
    }
    public record Quote(BigDecimal rate, BigDecimal raw, BigDecimal discount, BigDecimal total, Promotion promotion) {}
}
