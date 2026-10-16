package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.PromotionDTO;

import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ quản lý chương trình khuyến mãi (UC15, UC16).
 */
public interface PromotionService {

    List<PromotionDTO> getAllPromotions(String keyword, String status);

    PromotionDTO getPromotionById(String id);

    PromotionDTO savePromotion(PromotionDTO dto);

    void deletePromotion(String id);

    Map<String, Object> getPromotionStatistics();
}
