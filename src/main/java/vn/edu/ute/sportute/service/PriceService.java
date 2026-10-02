package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.PriceDTO;
import java.util.List;

public interface PriceService {
    List<PriceDTO> getAllPrices();
    PriceDTO getPriceById(String id);
    void savePrice(PriceDTO dto);
    void deletePrice(String id);
}