package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.CourtDTO;
import vn.edu.ute.sportute.entity.Court;
import vn.edu.ute.sportute.entity.Sport;
import vn.edu.ute.sportute.repository.CourtRepository;
import vn.edu.ute.sportute.repository.SportRepository;
import vn.edu.ute.sportute.service.CourtService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourtServiceImpl implements CourtService {

    private final CourtRepository courtRepository;
    private final SportRepository sportRepository;

    public CourtServiceImpl(CourtRepository courtRepository, SportRepository sportRepository) {
        this.courtRepository = courtRepository;
        this.sportRepository = sportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtDTO> getAllCourts() {
        return courtRepository.findAll().stream().map(c -> CourtDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .sportId(c.getSport().getId())
                .sportName(c.getSport().getName())
                .location(c.getLocation())
                .status(c.getStatus())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CourtDTO getCourtById(String id) {
        Court c = courtRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sân với mã: " + id));
        return CourtDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .sportId(c.getSport().getId())
                .sportName(c.getSport().getName())
                .location(c.getLocation())
                .status(c.getStatus())
                .build();
    }

    @Override
    public void saveCourt(CourtDTO dto) {
        Sport sport = sportRepository.findById(dto.getSportId())
                .orElseThrow(() -> new IllegalArgumentException("Môn thể thao không tồn tại"));

        Court court;
        if (dto.getId() == null || dto.getId().trim().isEmpty()) {
            court = Court.builder()
                    .id(IdGenerator.generateId("SAN"))
                    .name(dto.getName())
                    .sport(sport)
                    .location(dto.getLocation())
                    .status(dto.getStatus() != null ? dto.getStatus() : "Hoạt động")
                    .build();
        } else {
            court = courtRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại: " + dto.getId()));
            court.setName(dto.getName());
            court.setSport(sport);
            court.setLocation(dto.getLocation());
            court.setStatus(dto.getStatus());
        }
        courtRepository.save(court);
    }

    @Override
    public void toggleCourtStatus(String id) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại"));
        if ("Hoạt động".equalsIgnoreCase(court.getStatus())) {
            court.setStatus("Ngừng hoạt động");
        } else {
            court.setStatus("Hoạt động");
        }
        courtRepository.save(court);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getCourtStatistics() {
        List<Court> courts = courtRepository.findAll();
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) courts.size());
        stats.put("active", courts.stream().filter(c -> "Hoạt động".equalsIgnoreCase(c.getStatus())).count());
        stats.put("inUse", courts.stream().filter(c -> "Đang sử dụng".equalsIgnoreCase(c.getStatus())).count());
        stats.put("maintenance", courts.stream().filter(c -> "Bảo trì".equalsIgnoreCase(c.getStatus())).count());
        return stats;
    }
}