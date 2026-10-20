package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.CustomerDTO;
import vn.edu.ute.sportute.dto.response.CustomerRentHistoryDTO;
import vn.edu.ute.sportute.entity.Booking;
import vn.edu.ute.sportute.entity.BookingDetail;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.repository.BookingRepository;
import vn.edu.ute.sportute.repository.CustomerRepository;
import vn.edu.ute.sportute.service.CustomerService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {
    @org.springframework.beans.factory.annotation.Autowired
    private IdGenerator idGenerator;


    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository, BookingRepository bookingRepository) {
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
    }

    private String calculateTier(int bookingCount) {
        if (bookingCount >= 30) return "VIP";
        if (bookingCount >= 10) return "Thân thiết";
        return "Thường";
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers(String keyword, String tier, String status) {
        List<Customer> all = customerRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();

        // Đếm số lượt đặt cho từng khách
        Map<String, Long> bookingCounts = allBookings.stream()
                .filter(b -> b.getCustomer() != null && b.getCustomer().getId() != null)
                .collect(Collectors.groupingBy(b -> b.getCustomer().getId(), Collectors.counting()));

        return all.stream()
                .map(c -> {
                    int count = bookingCounts.getOrDefault(c.getId(), 0L).intValue();
                    String t = calculateTier(count);
                    return CustomerDTO.builder()
                            .id(c.getId())
                            .fullName(c.getFullName())
                            .phone(c.getPhone())
                            .email(c.getEmail())
                            .address(c.getAddress())
                            .status(c.getStatus() != null ? c.getStatus() : "Hoạt động")
                            .tier(t)
                            .totalBookings(count)
                            .build();
                })
                .filter(dto -> {
                    if (tier != null && !tier.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(tier)) {
                        if (!tier.equalsIgnoreCase(dto.getTier())) return false;
                    }
                    if (status != null && !status.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(dto.getStatus())) return false;
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchId = dto.getId() != null && dto.getId().toLowerCase().contains(kw);
                        boolean matchName = dto.getFullName() != null && dto.getFullName().toLowerCase().contains(kw);
                        boolean matchPhone = dto.getPhone() != null && dto.getPhone().contains(kw);
                        boolean matchAddress = dto.getAddress() != null && dto.getAddress().toLowerCase().contains(kw);
                        return matchId || matchName || matchPhone || matchAddress;
                    }
                    return true;
                })
                .sorted((c1, c2) -> c1.getId().compareTo(c2.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(String id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng với mã: " + id));

        long count = bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer() != null && id.equalsIgnoreCase(b.getCustomer().getId()))
                .count();

        return CustomerDTO.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .address(c.getAddress())
                .status(c.getStatus() != null ? c.getStatus() : "Hoạt động")
                .tier(calculateTier((int) count))
                .totalBookings((int) count)
                .build();
    }

    @Override
    public CustomerDTO saveCustomer(CustomerDTO dto) {
        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên khách hàng không được để trống");
        }
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại khách hàng không được để trống");
        }
        String cleanPhone = dto.getPhone().trim().replaceAll("[\\s.-]", "");
        if (!cleanPhone.matches("^0\\d{9}$")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số hợp lệ");
        }

        Customer customer;
        if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
            // Cập nhật khách hàng (UC32)
            customer = customerRepository.findById(dto.getId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng: " + dto.getId()));

            // Nếu đổi SĐT, kiểm tra không được trùng với khách khác
            if (!dto.getPhone().trim().equals(customer.getPhone())) {
                if (customerRepository.existsByPhone(dto.getPhone().trim())) {
                    throw new IllegalArgumentException("Số điện thoại này đã được đăng ký bởi khách hàng khác!");
                }
            }
        } else {
            // Thêm khách hàng mới (UC30)
            if (customerRepository.existsByPhone(dto.getPhone().trim())) {
                throw new IllegalArgumentException("Số điện thoại này đã tồn tại trong hệ thống!");
            }
            customer = new Customer();
            customer.setId(idGenerator.generateId("KH"));
        }

        customer.setFullName(dto.getFullName().trim());
        customer.setPhone(dto.getPhone().trim());
        customer.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        customer.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        customer.setStatus(dto.getStatus() != null ? dto.getStatus() : "Hoạt động");

        Customer saved = customerRepository.save(customer);
        return CustomerDTO.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .phone(saved.getPhone())
                .email(saved.getEmail())
                .address(saved.getAddress())
                .status(saved.getStatus())
                .tier(dto.getTier() != null ? dto.getTier() : "Thường")
                .totalBookings(dto.getTotalBookings())
                .build();
    }

    @Override
    public void toggleCustomerStatus(String id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng: " + id));

        // Ngừng sử dụng hồ sơ khách hàng (UC33)
        if ("Hoạt động".equalsIgnoreCase(c.getStatus())) {
            c.setStatus("Ngừng hoạt động");
            if (c.getAccount() != null) {
                c.getAccount().setStatus("Bị khóa");
            }
        } else {
            c.setStatus("Hoạt động");
            if (c.getAccount() != null) {
                c.getAccount().setStatus("Hoạt động");
            }
        }
        customerRepository.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerRentHistoryDTO> getCustomerRentHistory(String customerId) {
        List<Booking> bookings = bookingRepository.findByCustomer_Id(customerId).stream()
                .sorted((b1, b2) -> b2.getId().compareTo(b1.getId()))
                .collect(Collectors.toList());

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        List<CustomerRentHistoryDTO> list = new ArrayList<>();

        for (Booking b : bookings) {
            String courtName = "N/A";
            String sportName = "N/A";
            LocalDate date = LocalDate.now();
            String timeSlot = "--:--";

            if (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty()) {
                BookingDetail detail = b.getBookingDetails().get(0);
                if (detail.getCourt() != null) {
                    courtName = detail.getCourt().getName();
                    if (detail.getCourt().getSport() != null) {
                        sportName = detail.getCourt().getSport().getName();
                    }
                }
                if (detail.getStartTime() != null) {
                    date = detail.getStartTime().toLocalDate();
                    timeSlot = detail.getStartTime().format(timeFmt) + " - " + 
                              (detail.getEndTime() != null ? detail.getEndTime().format(timeFmt) : "");
                }
            }

            BigDecimal total = BigDecimal.ZERO;
            if (b.getPayments() != null) {
                total = b.getPayments().stream()
                        .filter(p -> "Thu".equalsIgnoreCase(p.getTransactionType()) || "Cọc".equalsIgnoreCase(p.getTransactionType()))
                        .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            list.add(CustomerRentHistoryDTO.builder()
                    .bookingId(b.getId())
                    .courtName(courtName)
                    .sportName(sportName)
                    .bookingDate(date)
                    .timeSlot(timeSlot)
                    .totalAmount(total)
                    .status(b.getStatus() != null ? b.getStatus() : "Đã xác nhận")
                    .build());
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerStatistics() {
        List<CustomerDTO> all = getAllCustomers(null, null, null);
        Map<String, Object> map = new HashMap<>();

        long total = all.size();
        long vip = all.stream().filter(c -> "VIP".equalsIgnoreCase(c.getTier())).count();
        long loyal = all.stream().filter(c -> "Thân thiết".equalsIgnoreCase(c.getTier())).count();
        int totalBookings = all.stream().mapToInt(CustomerDTO::getTotalBookings).sum();

        map.put("totalCustomers", total);
        map.put("vipCustomers", vip);
        map.put("loyalCustomers", loyal);
        map.put("totalBookings", totalBookings);

        return map;
    }
}
