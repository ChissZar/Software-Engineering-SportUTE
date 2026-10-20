package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.MaintenanceDTO;
import vn.edu.ute.sportute.entity.Court;
import vn.edu.ute.sportute.entity.Employee;
import vn.edu.ute.sportute.entity.Maintenance;
import vn.edu.ute.sportute.repository.BookingDetailRepository;
import vn.edu.ute.sportute.repository.CourtRepository;
import vn.edu.ute.sportute.repository.EmployeeRepository;
import vn.edu.ute.sportute.repository.MaintenanceRepository;
import vn.edu.ute.sportute.service.MaintenanceService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaintenanceServiceImpl implements MaintenanceService {
    @org.springframework.beans.factory.annotation.Autowired
    private IdGenerator idGenerator;


    private final MaintenanceRepository maintenanceRepository;
    private final CourtRepository courtRepository;
    private final EmployeeRepository employeeRepository;
    private final BookingDetailRepository bookingDetailRepository;

    public MaintenanceServiceImpl(MaintenanceRepository maintenanceRepository,
                                  CourtRepository courtRepository,
                                  EmployeeRepository employeeRepository,
                                  BookingDetailRepository bookingDetailRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.courtRepository = courtRepository;
        this.employeeRepository = employeeRepository;
        this.bookingDetailRepository = bookingDetailRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceDTO> getAllMaintenances() {
        return maintenanceRepository.findAll().stream().map(m -> MaintenanceDTO.builder()
                .id(m.getId())
                .courtId(m.getCourt().getId())
                .courtName(m.getCourt().getName())
                .employeeId(m.getEmployee() != null ? m.getEmployee().getId() : null)
                .employeeName(m.getEmployee() != null ? m.getEmployee().getFullName() : "N/A")
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .status(status(m))
                .reason(m.getReason())
                .build()).collect(Collectors.toList());
    }

    @Override
    public void createMaintenance(MaintenanceDTO dto, String employeeId) {
        if (!dto.getStartDate().isBefore(dto.getEndDate())) {
            throw new IllegalArgumentException("Thời gian bắt đầu bảo trì phải trước thời gian kết thúc!");
        }

        Court court = courtRepository.findForUpdate(dto.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sân: " + dto.getCourtId()));

        Employee employee = employeeRepository.findByAccount_Username(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa liên kết hồ sơ nhân viên. Vui lòng bổ sung hồ sơ trước khi lập lịch."));
        if (dto.getReason().length() > 200) throw new IllegalArgumentException("Lý do tối đa 200 ký tự");
        if (!dto.getEndDate().isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Thời gian kết thúc phải ở tương lai");
        if (maintenanceRepository.existsOverlappingMaintenance(court.getId(), dto.getStartDate(), dto.getEndDate()))
            throw new IllegalArgumentException("Sân đã có lịch bảo trì trong khoảng này");

        // Quy tắc toàn vẹn UC45 / RB2: Kiểm tra lịch bảo trì có chồng lấn lịch đặt sân hay không
        boolean hasBookingConflict = bookingDetailRepository.existsOverlappingBooking(
                court.getId(), dto.getStartDate(), dto.getEndDate()
        );
        if (hasBookingConflict) {
            throw new IllegalArgumentException("Không thể lập lịch bảo trì: Sân đã có lịch đặt sân trong khung giờ này!");
        }

        Maintenance maintenance = Maintenance.builder()
                .id(idGenerator.generateId("BT"))
                .court(court)
                .employee(employee)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .build();

        maintenanceRepository.save(maintenance);

        // Availability is derived from the interval, never from a permanent court status.
    }

    private String status(Maintenance m) {
        if ("CANCELLED".equals(m.getLifecycleStatus())) return "Đã hủy";
        if ("COMPLETED".equals(m.getLifecycleStatus()) || !LocalDateTime.now().isBefore(m.getEndDate())) return "Hoàn tất";
        return LocalDateTime.now().isBefore(m.getStartDate()) ? "Sắp tới" : "Đang bảo trì";
    }

    public void completeMaintenance(String id) {
        Maintenance m = maintenanceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch"));
        if (!"Đang bảo trì".equals(status(m))) throw new IllegalArgumentException("Chỉ hoàn tất lịch đang bảo trì");
        m.setLifecycleStatus("COMPLETED");
        maintenanceRepository.save(m);
    }

    @Override
    public void deleteMaintenance(String id) {
        Maintenance m = maintenanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lịch bảo trì không tồn tại"));
        if (!"Sắp tới".equals(status(m))) throw new IllegalArgumentException("Chỉ hủy lịch chưa bắt đầu");
        m.setLifecycleStatus("CANCELLED");
        maintenanceRepository.save(m);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getMaintenanceStatistics() {
        LocalDateTime now = LocalDateTime.now();
        List<Maintenance> all = maintenanceRepository.findAll();
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) all.size());
        stats.put("inProgress", all.stream().filter(m -> "Đang bảo trì".equals(status(m))).count());
        stats.put("upcoming", all.stream().filter(m -> "Sắp tới".equals(status(m))).count());
        return stats;
    }
}