package vn.edu.ute.sportute.service.impl;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.response.RoleSummaryDTO;
import vn.edu.ute.sportute.dto.response.UserAccountDTO;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.entity.Employee;
import vn.edu.ute.sportute.entity.Feature;
import vn.edu.ute.sportute.entity.Role;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.repository.CustomerRepository;
import vn.edu.ute.sportute.repository.EmployeeRepository;
import vn.edu.ute.sportute.repository.FeatureRepository;
import vn.edu.ute.sportute.repository.RoleRepository;
import vn.edu.ute.sportute.service.PermissionService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private final RoleRepository roleRepository;
    private final FeatureRepository featureRepository;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;

    public PermissionServiceImpl(RoleRepository roleRepository,
                                 FeatureRepository featureRepository,
                                 AccountRepository accountRepository,
                                 EmployeeRepository employeeRepository,
                                 CustomerRepository customerRepository) {
        this.roleRepository = roleRepository;
        this.featureRepository = featureRepository;
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feature> getAllFeatures() {
        return featureRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccountDTO> getAllUserAccountDTOs() {
        List<Account> accounts = getAllAccounts();
        List<UserAccountDTO> dtos = new ArrayList<>();
        int index = 1;

        for (Account acc : accounts) {
            Optional<Employee> empOpt = employeeRepository.findByAccount_Id(acc.getId());
            Optional<Customer> custOpt = customerRepository.findByAccount_Id(acc.getId());

            String fullName = acc.getUsername();
            String phone = "—";
            String accountType = "Tài khoản hệ thống";
            boolean isStaff = false;

            if (empOpt.isPresent()) {
                Employee emp = empOpt.get();
                fullName = emp.getFullName();
                phone = emp.getPhone();
                accountType = (emp.getPosition() != null && !emp.getPosition().isBlank())
                        ? emp.getPosition() + " (" + emp.getId() + ")"
                        : "Nhân viên (" + emp.getId() + ")";
                isStaff = true;
            } else if (custOpt.isPresent()) {
                Customer cust = custOpt.get();
                fullName = cust.getFullName();
                phone = cust.getPhone();
                accountType = "Khách hàng (" + cust.getId() + ")";
            } else {
                if ("manager".equalsIgnoreCase(acc.getUsername())) {
                    fullName = "Quản lý trung tâm";
                    phone = "0909998877";
                    accountType = "Quản lý cơ sở";
                    isStaff = true;
                } else if ("admin".equalsIgnoreCase(acc.getUsername())) {
                    fullName = "Trần Quốc Bảo";
                    phone = "0901234567";
                    accountType = "Quản trị viên";
                    isStaff = true;
                }
            }

            Set<String> roleIds = acc.getRoles().stream()
                    .map(Role::getId)
                    .collect(Collectors.toSet());

            Set<String> roleNames = acc.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());

            if (roleIds.contains("VT_ADMIN") || roleIds.contains("VT_QL") ||
                roleIds.contains("VT_LT") || roleIds.contains("VT_VH")) {
                isStaff = true;
            }

            List<String> filterParts = new ArrayList<>(roleIds);
            if (isStaff) {
                filterParts.add("STAFF");
            }
            String roleFilterKeys = String.join(" ", filterParts);

            String searchData = String.join(" ",
                    acc.getUsername(),
                    acc.getId(),
                    fullName,
                    acc.getEmail() != null ? acc.getEmail() : "",
                    phone,
                    accountType,
                    String.join(" ", roleNames)
            ).toLowerCase();

            String avatarInitials = getInitials(fullName, acc.getUsername());
            String avatarClass = "av" + ((index % 5) + 1);
            index++;

            UserAccountDTO dto = UserAccountDTO.builder()
                    .id(acc.getId())
                    .username(acc.getUsername())
                    .email(acc.getEmail())
                    .status(acc.getStatus())
                    .fullName(fullName)
                    .phone(phone)
                    .accountType(accountType)
                    .avatarClass(avatarClass)
                    .avatarInitials(avatarInitials)
                    .roleIds(roleIds)
                    .roleNames(roleNames)
                    .roleFilterKeys(roleFilterKeys)
                    .searchData(searchData)
                    .build();

            dtos.add(dto);
        }

        return dtos;
    }

    private String getInitials(String fullName, String username) {
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length >= 2) {
                return (parts[parts.length - 2].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
            } else if (parts.length == 1 && parts[0].length() >= 2) {
                return parts[0].substring(0, 2).toUpperCase();
            }
        }
        if (username != null && username.length() >= 2) {
            return username.substring(0, 2).toUpperCase();
        }
        return "US";
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleSummaryDTO> getRoleSummaries() {
        List<Role> roles = getAllRoles();
        List<Account> accounts = getAllAccounts();
        int totalFeatures = (int) featureRepository.count();

        List<RoleSummaryDTO> list = new ArrayList<>();
        for (Role role : roles) {
            int accountCount = 0;
            for (Account acc : accounts) {
                if (acc.getRoles().stream().anyMatch(r -> r.getId().equals(role.getId()))) {
                    accountCount++;
                }
            }

            int featureCount = role.getFeatures() != null ? role.getFeatures().size() : 0;
            int featurePercent = totalFeatures > 0 ? (int) Math.round((double) featureCount / totalFeatures * 100) : 0;

            String badgeLabel = "Vai trò";
            String colorClass = "customer";

            switch (role.getId()) {
                case "VT_ADMIN":
                    badgeLabel = "Toàn quyền";
                    colorClass = "admin";
                    break;
                case "VT_QL":
                    badgeLabel = "Quản lý";
                    colorClass = "manager";
                    break;
                case "VT_LT":
                    badgeLabel = "Lễ tân";
                    colorClass = "reception";
                    break;
                case "VT_VH":
                    badgeLabel = "Vận hành sân";
                    colorClass = "operator";
                    break;
                case "VT_KHACH":
                    badgeLabel = "Khách hàng";
                    colorClass = "customer";
                    break;
            }

            RoleSummaryDTO dto = RoleSummaryDTO.builder()
                    .id(role.getId())
                    .name(role.getName())
                    .description(role.getDescription())
                    .badgeLabel(badgeLabel)
                    .colorClass(colorClass)
                    .featureCount(featureCount)
                    .totalFeatures(totalFeatures)
                    .featurePercent(featurePercent)
                    .accountCount(accountCount)
                    .build();

            list.add(dto);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Set<String>> getRoleFeatureMatrix() {
        List<Role> roles = getAllRoles();
        Map<String, Set<String>> matrix = new HashMap<>();

        for (Role role : roles) {
            Set<String> featureIds = role.getFeatures() != null
                    ? role.getFeatures().stream().map(Feature::getId).collect(Collectors.toSet())
                    : new HashSet<>();
            matrix.put(role.getId(), featureIds);
        }

        return matrix;
    }

    @Override
    public void updateRoleFeatures(String roleId, List<String> featureIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò: " + roleId));

        if (featureIds == null || featureIds.isEmpty()) {
            role.getFeatures().clear();
        } else {
            List<Feature> features = featureRepository.findAllById(featureIds);
            role.setFeatures(new HashSet<>(features));
        }

        roleRepository.save(role);
    }

    @Override
    public void saveFullMatrix(Map<String, List<String>> matrix) {
        if (matrix == null) return;

        for (Map.Entry<String, List<String>> entry : matrix.entrySet()) {
            String roleId = entry.getKey();
            List<String> featureIds = entry.getValue();

            roleRepository.findById(roleId).ifPresent(role -> {
                if (featureIds == null || featureIds.isEmpty()) {
                    role.getFeatures().clear();
                } else {
                    List<Feature> features = featureRepository.findAllById(featureIds);
                    role.setFeatures(new HashSet<>(features));
                }
                roleRepository.save(role);
            });
        }
    }

    @Override
    public void resetMatrixToDefault() {
        Map<String, List<String>> defaultMatrix = new HashMap<>();
        defaultMatrix.put("VT_ADMIN", Arrays.asList("CN01", "CN02", "CN03", "CN04", "CN05", "CN06", "CN07", "CN08", "CN09"));
        defaultMatrix.put("VT_QL", Arrays.asList("CN01", "CN02", "CN03", "CN06", "CN07", "CN08", "CN09"));
        defaultMatrix.put("VT_LT", Arrays.asList("CN04", "CN06"));
        defaultMatrix.put("VT_VH", Arrays.asList("CN05", "CN07"));
        defaultMatrix.put("VT_KHACH", Collections.singletonList("CN10"));

        saveFullMatrix(defaultMatrix);
    }

    @Override
    public void updateAccountRoles(String accountId, List<String> roleIds) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + accountId));

        Set<Role> roles = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            roles.addAll(roleRepository.findAllById(roleIds));
        }

        account.setRoles(roles);
        accountRepository.save(account);
    }

    @Override
    public String toggleAccountStatus(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + accountId));

        String currentStatus = account.getStatus();
        String newStatus = "Hoạt động".equalsIgnoreCase(currentStatus) ? "Bị khóa" : "Hoạt động";
        account.setStatus(newStatus);
        accountRepository.save(account);
        return newStatus;
    }

    @Override
    public void createRole(String id, String name, String description) {
        if (roleRepository.existsById(id)) {
            throw new IllegalArgumentException("Mã vai trò đã tồn tại: " + id);
        }
        Role role = Role.builder()
                .id(id)
                .name(name)
                .description(description)
                .features(new HashSet<>())
                .build();
        roleRepository.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPermissionStatistics() {
        List<Account> accounts = getAllAccounts();
        int totalAccounts = accounts.size();

        long staffCount = accounts.stream().filter(acc ->
                acc.getRoles().stream().anyMatch(r ->
                        Arrays.asList("VT_ADMIN", "VT_QL", "VT_LT", "VT_VH").contains(r.getId()))
        ).count();

        long customerCount = totalAccounts - staffCount;

        long activeCount = accounts.stream()
                .filter(acc -> "Hoạt động".equalsIgnoreCase(acc.getStatus()))
                .count();

        int activePercent = totalAccounts > 0
                ? (int) Math.round((double) activeCount / totalAccounts * 100)
                : 100;

        long totalRoles = roleRepository.count();
        long totalFeatures = featureRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAccounts", totalAccounts);
        stats.put("staffAccounts", staffCount);
        stats.put("customerAccounts", customerCount);
        stats.put("activeAccounts", activeCount);
        stats.put("activePercent", activePercent);
        stats.put("totalRoles", totalRoles);
        stats.put("totalFeatures", totalFeatures);

        return stats;
    }
}
