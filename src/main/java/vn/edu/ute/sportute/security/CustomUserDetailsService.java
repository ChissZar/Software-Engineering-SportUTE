package vn.edu.ute.sportute.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.entity.Employee;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.repository.CustomerRepository;
import vn.edu.ute.sportute.repository.EmployeeRepository;

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(AccountRepository accountRepository,
                                    CustomerRepository customerRepository,
                                    EmployeeRepository employeeRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new UsernameNotFoundException("Thông tin đăng nhập không được để trống");
        }
        String clean = identifier.trim();

        // 1. Tìm trực tiếp theo Tên đăng nhập hoặc Email
        Optional<Account> accountOpt = accountRepository.findByUsernameOrEmail(clean, clean);
        if (accountOpt.isPresent()) {
            return new CustomUserDetails(accountOpt.get());
        }

        // 2. Tìm theo Số điện thoại Khách hàng (dành cho người dùng tạo tài khoản)
        Optional<Customer> customerOpt = customerRepository.findByPhone(clean);
        if (customerOpt.isPresent() && customerOpt.get().getAccount() != null) {
            String accId = customerOpt.get().getAccount().getId();
            Optional<Account> accFromDb = accountRepository.findById(accId);
            if (accFromDb.isPresent()) {
                return new CustomUserDetails(accFromDb.get());
            }
        }

        // 3. Tìm theo Số điện thoại Nhân viên
        Optional<Employee> employeeOpt = employeeRepository.findByPhone(clean);
        if (employeeOpt.isPresent() && employeeOpt.get().getAccount() != null) {
            String accId = employeeOpt.get().getAccount().getId();
            Optional<Account> accFromDb = accountRepository.findById(accId);
            if (accFromDb.isPresent()) {
                return new CustomUserDetails(accFromDb.get());
            }
        }

        throw new UsernameNotFoundException("Không tìm thấy tài khoản với thông tin: " + identifier);
    }
}