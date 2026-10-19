package vn.edu.ute.sportute.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.EmployeeDTO;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.entity.Employee;
import vn.edu.ute.sportute.entity.Role;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.repository.EmployeeRepository;
import vn.edu.ute.sportute.repository.RoleRepository;
import vn.edu.ute.sportute.service.EmployeeService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               AccountRepository accountRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private EmployeeDTO mapToDTO(Employee e) {
        String username = null;
        String roleId = null;
        String roleName = "Chưa phân quyền";

        if (e.getAccount() != null) {
            username = e.getAccount().getUsername();
            if (e.getAccount().getRoles() != null && !e.getAccount().getRoles().isEmpty()) {
                Role r = e.getAccount().getRoles().iterator().next();
                roleId = r.getId();
                roleName = r.getName();
            }
        }

        return EmployeeDTO.builder()
                .id(e.getId())
                .fullName(e.getFullName())
                .phone(e.getPhone())
                .email(e.getEmail())
                .position(e.getPosition())
                .status(e.getStatus() != null ? e.getStatus() : "Đang làm")
                .username(username)
                .roleId(roleId)
                .roleName(roleName)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees(String keyword, String position, String status) {
        return employeeRepository.findAll().stream()
                .map(this::mapToDTO)
                .filter(dto -> {
                    if (position != null && !position.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(position)) {
                        if (!position.equalsIgnoreCase(dto.getPosition())) return false;
                    }
                    if (status != null && !status.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(dto.getStatus())) return false;
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchId = dto.getId() != null && dto.getId().toLowerCase().contains(kw);
                        boolean matchName = dto.getFullName() != null && dto.getFullName().toLowerCase().contains(kw);
                        boolean matchPhone = dto.getPhone() != null && dto.getPhone().contains(kw);
                        boolean matchEmail = dto.getEmail() != null && dto.getEmail().toLowerCase().contains(kw);
                        boolean matchUser = dto.getUsername() != null && dto.getUsername().toLowerCase().contains(kw);
                        return matchId || matchName || matchPhone || matchEmail || matchUser;
                    }
                    return true;
                })
                .sorted((e1, e2) -> e1.getId().compareTo(e2.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeById(String id) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên: " + id));
        return mapToDTO(e);
    }

    @Override
    public EmployeeDTO saveEmployee(EmployeeDTO dto) {
        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên nhân viên không được để trống");
        }
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại nhân viên không được để trống");
        }

        Employee employee;
        if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
            // Cập nhật hồ sơ nhân viên (UC06)
            employee = employeeRepository.findById(dto.getId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên: " + dto.getId()));

            if (!dto.getPhone().trim().equals(employee.getPhone())) {
                if (employeeRepository.findByPhone(dto.getPhone().trim()).isPresent()) {
                    throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi nhân viên khác!");
                }
            }
        } else {
            // Thêm nhân viên mới (UC05)
            if (employeeRepository.findByPhone(dto.getPhone().trim()).isPresent()) {
                throw new IllegalArgumentException("Số điện thoại này đã tồn tại trong danh mục nhân sự!");
            }
            employee = new Employee();
            employee.setId(IdGenerator.generateId("NV"));
        }

        employee.setFullName(dto.getFullName().trim());
        employee.setPhone(dto.getPhone().trim());
        employee.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : (employee.getId() + "@sporthub.vn"));
        employee.setPosition(dto.getPosition() != null ? dto.getPosition().trim() : "Lễ tân");
        employee.setStatus(dto.getStatus() != null ? dto.getStatus() : "Đang làm");

        // Liên kết hoặc tạo/cập nhật tài khoản hệ thống cho nhân viên (UC07, TC_EMP_04)
        Account account = employee.getAccount();
        if (dto.getUsername() != null && !dto.getUsername().trim().isEmpty()) {
            String uname = dto.getUsername().trim();
            Optional<Account> accOpt = accountRepository.findByUsername(uname);
            if (accOpt.isPresent()) {
                account = accOpt.get();
            } else if (account == null) {
                account = Account.builder()
                        .id(IdGenerator.generateId("TK"))
                        .username(uname)
                        .password(passwordEncoder.encode("123456"))
                        .email(employee.getEmail())
                        .status("Hoạt động")
                        .build();
            } else {
                account.setUsername(uname);
            }
        }

        if (account != null) {
            // Xác định vai trò theo chức vụ hoặc roleId được chỉ định (TC_EMP_04: Cập nhật chức vụ tự động nâng/hạ quyền)
            String targetRoleId = dto.getRoleId();
            if (targetRoleId == null || targetRoleId.trim().isEmpty()) {
                if ("Quản lý".equalsIgnoreCase(employee.getPosition())) {
                    targetRoleId = "VT_QL";
                } else if ("Kỹ thuật".equalsIgnoreCase(employee.getPosition()) || "Vận hành".equalsIgnoreCase(employee.getPosition())) {
                    targetRoleId = "VT_VH";
                } else {
                    targetRoleId = "VT_LT";
                }
            } else if ("Quản lý".equalsIgnoreCase(employee.getPosition())) {
                targetRoleId = "VT_QL";
            }

            if (targetRoleId != null) {
                Optional<Role> roleOpt = roleRepository.findById(targetRoleId);
                if (roleOpt.isPresent()) {
                    account.setRoles(new HashSet<>(Collections.singletonList(roleOpt.get())));
                }
            }
            account = accountRepository.save(account);
            employee.setAccount(account);
        }

        Employee saved = employeeRepository.save(employee);
        return mapToDTO(saved);
    }

    @Override
    public void toggleEmployeeStatus(String id) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên: " + id));

        // Ngừng hoạt động nhân viên / tài khoản (UC08)
        if ("Đang làm".equalsIgnoreCase(e.getStatus())) {
            e.setStatus("Đã nghỉ");
            if (e.getAccount() != null) {
                e.getAccount().setStatus("Khóa");
                accountRepository.save(e.getAccount());
            }
        } else {
            e.setStatus("Đang làm");
            if (e.getAccount() != null) {
                e.getAccount().setStatus("Hoạt động");
                accountRepository.save(e.getAccount());
            }
        }
        employeeRepository.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEmployeeStatistics() {
        List<Employee> all = employeeRepository.findAll();
        Map<String, Object> map = new HashMap<>();

        long total = all.size();
        long active = all.stream().filter(e -> "Đang làm".equalsIgnoreCase(e.getStatus())).count();
        long onLeave = all.stream().filter(e -> "Nghỉ phép".equalsIgnoreCase(e.getStatus())).count();
        long managers = all.stream().filter(e -> "Quản lý".equalsIgnoreCase(e.getPosition())).count();

        map.put("totalEmployees", total);
        map.put("activeEmployees", active);
        map.put("onLeaveEmployees", onLeave);
        map.put("managerCount", managers);

        return map;
    }
}
