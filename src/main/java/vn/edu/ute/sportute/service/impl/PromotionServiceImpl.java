package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.PromotionDTO;
import vn.edu.ute.sportute.entity.Promotion;
import vn.edu.ute.sportute.repository.PromotionRepository;
import vn.edu.ute.sportute.service.PromotionService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    private String calculateStatus(Promotion p) {
        LocalDate today = LocalDate.now();
        if (p.getFromDate() != null && today.isBefore(p.getFromDate())) {
            return "Sắp diễn ra";
        }
        if (p.getToDate() != null && today.isAfter(p.getToDate())) {
            return "Đã kết thúc";
        }
        return "Đang áp dụng";
    }

    private PromotionDTO mapToDTO(Promotion p) {
        return PromotionDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .discountAmount(p.getDiscountAmount())
                .fromDate(p.getFromDate())
                .toDate(p.getToDate())
                .status(calculateStatus(p))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDTO> getAllPromotions(String keyword, String status) {
        return promotionRepository.findAll().stream()
                .map(this::mapToDTO)
                .filter(dto -> {
                    if (status != null && !status.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(dto.getStatus())) return false;
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchId = dto.getId() != null && dto.getId().toLowerCase().contains(kw);
                        boolean matchName = dto.getName() != null && dto.getName().toLowerCase().contains(kw);
                        return matchId || matchName;
                    }
                    return true;
                })
                .sorted((p1, p2) -> p2.getId().compareTo(p1.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDTO getPromotionById(String id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chương trình khuyến mãi: " + id));
        return mapToDTO(p);
    }

    @Override
    public PromotionDTO savePromotion(PromotionDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên chương trình khuyến mãi không được để trống");
        }
        if (dto.getDiscountAmount() == null) {
            throw new IllegalArgumentException("Số tiền giảm giá không được để trống");
        }

        Promotion promotion;
        if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
            // Cập nhật khuyến mãi (UC16)
            promotion = promotionRepository.findById(dto.getId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi: " + dto.getId()));
        } else {
            // Thêm khuyến mãi mới (UC15)
            promotion = new Promotion();
            promotion.setId(IdGenerator.generateId("KM"));
        }

        promotion.setName(dto.getName().trim());
        promotion.setDiscountAmount(dto.getDiscountAmount());
        promotion.setFromDate(dto.getFromDate());
        promotion.setToDate(dto.getToDate());

        Promotion saved = promotionRepository.save(promotion);
        return mapToDTO(saved);
    }

    @Override
    public void deletePromotion(String id) {
        // Xóa / Ngừng áp dụng khuyến mãi (UC16)
        if (!promotionRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy khuyến mãi để xóa: " + id);
        }
        promotionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPromotionStatistics() {
        List<PromotionDTO> all = getAllPromotions(null, null);
        Map<String, Object> map = new HashMap<>();

        long active = all.stream().filter(p -> "Đang áp dụng".equalsIgnoreCase(p.getStatus())).count();
        long upcoming = all.stream().filter(p -> "Sắp diễn ra".equalsIgnoreCase(p.getStatus())).count();
        long ended = all.stream().filter(p -> "Đã kết thúc".equalsIgnoreCase(p.getStatus())).count();
        long total = all.size();

        map.put("totalCount", total);
        map.put("activeCount", active);
        map.put("upcomingCount", upcoming);
        map.put("endedCount", ended);

        return map;
    }
}
