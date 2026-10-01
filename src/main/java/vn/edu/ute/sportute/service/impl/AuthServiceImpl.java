package vn.edu.ute.sportute.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.RegisterRequest;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.entity.Role;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.repository.CustomerRepository;
import vn.edu.ute.sportute.repository.RoleRepository;
import vn.edu.ute.sportute.service.AuthService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.util.Collections;

@Service
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AccountRepository accountRepository,
                           CustomerRepository customerRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Account registerCustomer(RegisterRequest request) {
        // 1. Kiểm tra ràng buộc trùng lặp
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email này đã được đăng ký tài khoản!");
        }
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng!");
        }

        // 2. Tìm hoặc khởi tạo vai trò mặc định: ROLE_CUSTOMER
        Role customerRole = roleRepository.findById("VT_KHACH")
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .id("VT_KHACH")
                            .name("ROLE_CUSTOMER")
                            .description("Vai trò khách hàng đặt sân trực tuyến")
                            .build();
                    return roleRepository.save(newRole);
                });

        // 3. Tạo TAIKHOAN (lấy email hoặc phần trước @ làm username)
        String username = request.getEmail().split("@")[0];
        if (accountRepository.existsByUsername(username)) {
            username = username + "_" + (System.currentTimeMillis() % 1000);
        }

        Account account = Account.builder()
                .id(IdGenerator.generateId("TK"))
                .username(username)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status("Hoạt động")
                .roles(Collections.singleton(customerRole))
                .build();
        Account savedAccount = accountRepository.save(account);

        // 4. Tạo hồ sơ KHACHHANG liên kết với TAIKHOAN vừa tạo
        Customer customer = Customer.builder()
                .id(IdGenerator.generateId("KH"))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status("Hoạt động")
                .account(savedAccount)
                .build();
        customerRepository.save(customer);

        return savedAccount;
    }
}