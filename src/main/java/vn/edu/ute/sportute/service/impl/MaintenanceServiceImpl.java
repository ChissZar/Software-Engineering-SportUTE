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
                .reason(m.getReason())
                .build()).collect(Collectors.toList());
    }

    @Override
    public void createMaintenance(MaintenanceDTO dto, String employeeId) {
        if (!dto.getStartDate().isBefore(dto.getEndDate())) {
            throw new IllegalArgumentException("Thời gian bắt đầu bảo trì phải trước thời gian kết thúc!");
        }

        Court court = courtRepository.findById(dto.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sân: " + dto.getCourtId()));

        Employee employee = null;
        if (employeeId != null) {
            employee = employeeRepository.findByAccount_Id(employeeId).orElse(null);
        }
        if (employee == null) {
            employee = employeeRepository.findAll().stream().findFirst().orElse(null);
        }

        // Quy tắc toàn vẹn UC45 / RB2: Kiểm tra lịch bảo trì có chồng lấn lịch đặt sân hay không
        boolean hasBookingConflict = bookingDetailRepository.existsOverlappingBooking(
                court.getId(), dto.getStartDate(), dto.getEndDate()
        );
        if (hasBookingConflict) {
            throw new IllegalArgumentException("Không thể lập lịch bảo trì: Sân đã có lịch đặt sân trong khung giờ này!");
        }

        Maintenance maintenance = Maintenance.builder()
                .id(IdGenerator.generateId("BT"))
                .court(court)
                .employee(employee)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .build();

        maintenanceRepository.save(maintenance);

        // Chuyển trạng thái sân sang "Bảo trì" nếu thời gian hiện tại nằm trong khung bảo trì
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(dto.getStartDate()) && !now.isAfter(dto.getEndDate())) {
            court.setStatus("Bảo trì");
            courtRepository.save(court);
        }
    }

    @Override
    public void deleteMaintenance(String id) {
        Maintenance m = maintenanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lịch bảo trì không tồn tại"));
        Court court = m.getCourt();
        maintenanceRepository.delete(m);
        // Trả lại trạng thái cho sân nếu đang bảo trì
        if ("Bảo trì".equalsIgnoreCase(court.getStatus())) {
            court.setStatus("Hoạt động");
            courtRepository.save(court);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getMaintenanceStatistics() {
        LocalDateTime now = LocalDateTime.now();
        List<Maintenance> all = maintenanceRepository.findAll();
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) all.size());
        stats.put("inProgress", all.stream().filter(m -> !now.isBefore(m.getStartDate()) && !now.isAfter(m.getEndDate())).count());
        stats.put("upcoming", all.stream().filter(m -> now.isBefore(m.getStartDate())).count());
        return stats;
    }
}