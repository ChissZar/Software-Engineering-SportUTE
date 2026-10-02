package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.SportDTO;
import vn.edu.ute.sportute.entity.Sport;
import vn.edu.ute.sportute.repository.CourtRepository;
import vn.edu.ute.sportute.repository.SportRepository;
import vn.edu.ute.sportute.service.SportService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SportServiceImpl implements SportService {

    private final SportRepository sportRepository;
    private final CourtRepository courtRepository;

    public SportServiceImpl(SportRepository sportRepository, CourtRepository courtRepository) {
        this.sportRepository = sportRepository;
        this.courtRepository = courtRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportDTO> getAllSports() {
        return sportRepository.findAll().stream().map(s -> SportDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .status(s.getStatus())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SportDTO getSportById(String id) {
        Sport s = sportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn thể thao: " + id));
        return SportDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .status(s.getStatus())
                .build();
    }

    @Override
    public void saveSport(SportDTO dto) {
        Sport sport;
        if (dto.getId() == null || dto.getId().trim().isEmpty()) {
            if (sportRepository.findByName(dto.getName()).isPresent()) {
                throw new IllegalArgumentException("Tên môn thể thao đã tồn tại!");
            }
            sport = Sport.builder()
                    .id(IdGenerator.generateId("M"))
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .status(dto.getStatus() != null ? dto.getStatus() : "Hoạt động")
                    .build();
        } else {
            sport = sportRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Môn không tồn tại: " + dto.getId()));
            sport.setName(dto.getName());
            sport.setDescription(dto.getDescription());
            sport.setStatus(dto.getStatus());
        }
        sportRepository.save(sport);
    }

    @Override
    public void toggleStatus(String id) {
        Sport sport = sportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Môn thể thao không tồn tại"));
        // Quy tắc UC10: Không xóa vật lý làm mất lịch sử, chỉ đổi trạng thái
        if ("Hoạt động".equalsIgnoreCase(sport.getStatus())) {
            sport.setStatus("Ngừng kinh doanh");
        } else {
            sport.setStatus("Hoạt động");
        }
        sportRepository.save(sport);
    }
}