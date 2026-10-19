package vn.edu.ute.sportute.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.ute.sportute.dto.response.CurrentUserDTO;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.entity.Employee;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.repository.CustomerRepository;
import vn.edu.ute.sportute.repository.EmployeeRepository;
import vn.edu.ute.sportute.security.CustomUserDetails;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalUserAdvice {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final vn.edu.ute.sportute.service.CustomerUserDataService customerUserDataService;

    public GlobalUserAdvice(AccountRepository accountRepository,
                            EmployeeRepository employeeRepository,
                            CustomerRepository customerRepository,
                            vn.edu.ute.sportute.service.CustomerUserDataService customerUserDataService) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.customerUserDataService = customerUserDataService;
    }

    @ModelAttribute("currentUser")
    public CurrentUserDTO populateCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String username = authentication.getName();
        Set<String> roleCodes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String accountId = null;
        String email = null;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            Account account = userDetails.getAccount();
            if (account != null) {
                accountId = account.getId();
                email = account.getEmail();
            }
        }

        if (accountId == null) {
            Optional<Account> accOpt = accountRepository.findByUsername(username);
            if (accOpt.isPresent()) {
                accountId = accOpt.get().getId();
                email = accOpt.get().getEmail();
            }
        }

        String fullName = username;
        String phone = "";
        String roleName = determineRoleName(roleCodes);
        String customerId = null;

        if (accountId != null) {
            Optional<Employee> empOpt = employeeRepository.findByAccount_Id(accountId);
            if (empOpt.isPresent()) {
                Employee emp = empOpt.get();
                fullName = emp.getFullName();
                phone = emp.getPhone() != null ? emp.getPhone() : "";
                if (emp.getEmail() != null) {
                    email = emp.getEmail();
                }
                roleName = mapPositionOrRole(emp.getPosition(), roleCodes);
            } else {
                Optional<Customer> custOpt = customerRepository.findByAccount_Id(accountId);
                if (custOpt.isPresent()) {
                    Customer cust = custOpt.get();
                    customerId = cust.getId();
                    fullName = cust.getFullName();
                    phone = cust.getPhone() != null ? cust.getPhone() : "";
                    if (cust.getEmail() != null) {
                        email = cust.getEmail();
                    }
                    roleName = "Khách hàng";
                }
            }
        }

        String primaryRole = "ROLE_USER";
        if (roleCodes.contains("ROLE_ADMIN")) {
            primaryRole = "ROLE_ADMIN";
        } else if (roleCodes.contains("ROLE_MANAGER")) {
            primaryRole = "ROLE_MANAGER";
        } else if (roleCodes.contains("ROLE_RECEPTIONIST")) {
            primaryRole = "ROLE_RECEPTIONIST";
        } else if (roleCodes.contains("ROLE_OPERATOR")) {
            primaryRole = "ROLE_OPERATOR";
        } else if (roleCodes.contains("ROLE_CUSTOMER")) {
            primaryRole = "ROLE_CUSTOMER";
        }

        String avatarInitials = extractInitials(fullName);

        String customAvatarUrl = null;
        if (customerId != null) {
            customAvatarUrl = customerUserDataService.getAvatarUrl(customerId);
        }

        return CurrentUserDTO.builder()
                .customerId(customerId)
                .accountId(accountId)
                .username(username)
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .roleName(roleName)
                .primaryRole(primaryRole)
                .avatarInitials(avatarInitials)
                .customAvatarUrl(customAvatarUrl)
                .roles(roleCodes)
                .build();
    }

    private String determineRoleName(Set<String> roles) {
        if (roles.contains("ROLE_ADMIN")) return "Quản trị viên";
        if (roles.contains("ROLE_MANAGER")) return "Quản lý trung tâm";
        if (roles.contains("ROLE_RECEPTIONIST")) return "Lễ tân quầy";
        if (roles.contains("ROLE_OPERATOR")) return "Kỹ thuật & Vận hành";
        if (roles.contains("ROLE_CUSTOMER")) return "Khách hàng";
        return "Nhân viên";
    }

    private String mapPositionOrRole(String position, Set<String> roles) {
        if (roles.contains("ROLE_ADMIN")) return "Quản trị viên";
        if (roles.contains("ROLE_MANAGER")) return "Quản lý trung tâm";
        if (roles.contains("ROLE_RECEPTIONIST")) return "Lễ tân quầy";
        if (roles.contains("ROLE_OPERATOR")) return "Kỹ thuật & Vận hành";
        return (position != null && !position.isBlank()) ? position : "Nhân viên";
    }

    private String extractInitials(String name) {
        if (name == null || name.isBlank()) return "SH";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        if (parts.length >= 2) {
            String mid = parts[parts.length - 2].substring(0, 1);
            String last = parts[parts.length - 1].substring(0, 1);
            return (mid + last).toUpperCase();
        }
        return parts[0].substring(0, 1).toUpperCase();
    }
}
