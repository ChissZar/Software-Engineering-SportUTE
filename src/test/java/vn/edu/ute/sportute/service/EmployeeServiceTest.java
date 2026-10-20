package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.ute.sportute.dto.request.EmployeeDTO;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.entity.Employee;
import vn.edu.ute.sportute.entity.Role;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.repository.EmployeeRepository;
import vn.edu.ute.sportute.repository.RoleRepository;
import vn.edu.ute.sportute.service.impl.EmployeeServiceImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee sampleEmployee;

    @BeforeEach
    void setUp() {
        TestIds.inject(employeeService);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin","",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        sampleEmployee = Employee.builder()
                .id("NV001")
                .fullName("Lê Thị Thu Hà")
                .phone("0912345678")
                .email("ha.le@sporthub.vn")
                .position("Lễ tân")
                .status("Đang làm")
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    void clearAuth() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }

    @Test
    @DisplayName("UC05, UC07: Thêm mới nhân viên và tự động cấp tài khoản vai trò VT_LT")
    void testSaveNewEmployeeWithAccount() {
        EmployeeDTO dto = EmployeeDTO.builder()
                .fullName("Lê Thị Thu Hà")
                .phone("0912345678")
                .email("ha.le@sporthub.vn")
                .position("Lễ tân")
                .username("ha.le")
                .status("Đang làm")
                .build();

        Role roleReception = Role.builder().id("VT_LT").name("Lễ tân").build();

        when(employeeRepository.findByPhone("0912345678")).thenReturn(Optional.empty());
        when(accountRepository.findByUsername("ha.le")).thenReturn(Optional.empty());
        when(roleRepository.findById("VT_LT")).thenReturn(Optional.of(roleReception));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword123");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeDTO saved = employeeService.saveEmployee(dto);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("NV"));
        assertEquals("ha.le", saved.getUsername());
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("UC08: Khóa tài khoản khi ngừng làm việc nhân viên")
    void testToggleEmployeeStatus() {
        Account account = Account.builder().id("TK002").status("Hoạt động").build();
        sampleEmployee.setAccount(account);

        when(employeeRepository.findById("NV001")).thenReturn(Optional.of(sampleEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        employeeService.toggleEmployeeStatus("NV001");

        assertEquals("Đã nghỉ", sampleEmployee.getStatus());
        assertEquals("Khóa", account.getStatus());
        verify(accountRepository, times(1)).save(account);
    }
}
