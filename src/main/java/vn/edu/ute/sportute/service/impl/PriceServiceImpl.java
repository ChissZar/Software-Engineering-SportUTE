package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.PriceDTO;
import vn.edu.ute.sportute.entity.Court;
import vn.edu.ute.sportute.entity.Price;
import vn.edu.ute.sportute.entity.PriceDetail;
import vn.edu.ute.sportute.repository.CourtRepository;
import vn.edu.ute.sportute.repository.PriceDetailRepository;
import vn.edu.ute.sportute.repository.PriceRepository;
import vn.edu.ute.sportute.service.PriceService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PriceServiceImpl implements PriceService {

    private final PriceRepository priceRepository;
    private final CourtRepository courtRepository;
    private final PriceDetailRepository priceDetailRepository;

    public PriceServiceImpl(PriceRepository priceRepository, CourtRepository courtRepository, PriceDetailRepository priceDetailRepository) {
        this.priceRepository = priceRepository;
        this.courtRepository = courtRepository;
        this.priceDetailRepository = priceDetailRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceDTO> getAllPrices() {
        return priceRepository.findAll().stream().map(p -> PriceDTO.builder()
                .id(p.getId())
                .courtId(p.getCourt().getId())
                .courtName(p.getCourt().getName())
                .fromDate(p.getFromDate())
                .toDate(p.getToDate())
                .details(p.getPriceDetails().stream().map(d -> PriceDTO.DetailDTO.builder()
                        .id(d.getId())
                        .timeSlot(d.getTimeSlot())
                        .priceAmount(d.getPriceAmount())
                        .build()).collect(Collectors.toList()))
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PriceDTO getPriceById(String id) {
        Price p = priceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bảng giá: " + id));
        return PriceDTO.builder()
                .id(p.getId())
                .courtId(p.getCourt().getId())
                .courtName(p.getCourt().getName())
                .fromDate(p.getFromDate())
                .toDate(p.getToDate())
                .details(p.getPriceDetails().stream().map(d -> PriceDTO.DetailDTO.builder()
                        .id(d.getId())
                        .timeSlot(d.getTimeSlot())
                        .priceAmount(d.getPriceAmount())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    @Override
    public void savePrice(PriceDTO dto) {
        if (dto.getToDate() != null && dto.getFromDate().isAfter(dto.getToDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc!");
        }

        Court court = courtRepository.findById(dto.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại"));

        Price price;
        if (dto.getId() == null || dto.getId().trim().isEmpty()) {
            price = Price.builder()
                    .id(IdGenerator.generateId("BG"))
                    .court(court)
                    .fromDate(dto.getFromDate())
                    .toDate(dto.getToDate())
                    .build();
        } else {
            price = priceRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Bảng giá không tồn tại: " + dto.getId()));
            price.setCourt(court);
            price.setFromDate(dto.getFromDate());
            price.setToDate(dto.getToDate());
            price.getPriceDetails().clear();
        }

        Price savedPrice = priceRepository.save(price);

        if (dto.getDetails() != null) {
            for (PriceDTO.DetailDTO detailDTO : dto.getDetails()) {
                PriceDetail detail = PriceDetail.builder()
                        .id(IdGenerator.generateId("CT"))
                        .price(savedPrice)
                        .timeSlot(detailDTO.getTimeSlot())
                        .priceAmount(detailDTO.getPriceAmount())
                        .build();
                priceDetailRepository.save(detail);
            }
        }
    }

    @Override
    public void deletePrice(String id) {
        priceRepository.deleteById(id);
    }
}