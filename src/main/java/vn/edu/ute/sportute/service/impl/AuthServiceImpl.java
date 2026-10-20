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
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {
    @Override
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword, String confirmation) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Vui lòng đăng nhập lại");
        Account account = accountRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, account.getPassword()))
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8
                || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Mật khẩu mới cần ít nhất 8 ký tự và không vượt quá 72 byte UTF-8");
        if (!newPassword.equals(confirmation))
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp");
        if (passwordEncoder.matches(newPassword, account.getPassword()))
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private IdGenerator idGenerator;


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

        Optional<Customer> existingByPhone = customerRepository.findByPhone(request.getPhone());
        if (existingByPhone.isPresent() && existingByPhone.get().getAccount() != null) {
            throw new IllegalArgumentException("Số điện thoại này đã được liên kết với một tài khoản đang hoạt động!");
        }

        Optional<Customer> existingByEmail = customerRepository.findByEmail(request.getEmail());
        if (existingByEmail.isPresent()) {
            if (existingByPhone.isEmpty() || !existingByEmail.get().getId().equals(existingByPhone.get().getId())) {
                throw new IllegalArgumentException("Địa chỉ email này đã được sử dụng bởi một khách hàng khác!");
            }
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
                .id(idGenerator.generateId("TK"))
                .username(username)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status("Hoạt động")
                .roles(Collections.singleton(customerRole))
                .build();
        Account savedAccount = accountRepository.save(account);

        // 4. Liên kết với hồ sơ KHACHHANG cũ (nếu từng đặt tại quầy) hoặc tạo mới
        if (existingByPhone.isPresent()) {
            Customer customer = existingByPhone.get();
            customer.setAccount(savedAccount);
            customer.setEmail(request.getEmail());
            if (request.getFullName() != null && !request.getFullName().isBlank()) {
                customer.setFullName(request.getFullName().trim());
            }
            customerRepository.save(customer);
        } else {
            Customer customer = Customer.builder()
                    .id(idGenerator.generateId("KH"))
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .status("Hoạt động")
                    .account(savedAccount)
                    .build();
            customerRepository.save(customer);
        }

        return savedAccount;
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email này!"));

        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }
}
