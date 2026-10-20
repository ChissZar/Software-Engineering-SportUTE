package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.AddServiceOrderRequestDTO;
import vn.edu.ute.sportute.dto.request.ServiceItemDTO;
import vn.edu.ute.sportute.entity.Booking;
import vn.edu.ute.sportute.entity.BookingServiceDetail;
import vn.edu.ute.sportute.entity.ServiceItem;
import vn.edu.ute.sportute.repository.BookingRepository;
import vn.edu.ute.sportute.repository.BookingServiceDetailRepository;
import vn.edu.ute.sportute.repository.ServiceItemRepository;
import vn.edu.ute.sportute.service.ServiceItemService;
import vn.edu.ute.sportute.util.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ServiceItemServiceImpl implements ServiceItemService {
    @org.springframework.beans.factory.annotation.Autowired
    private IdGenerator idGenerator;


    private final ServiceItemRepository serviceItemRepository;
    private final BookingServiceDetailRepository bookingServiceDetailRepository;
    private final BookingRepository bookingRepository;

    public ServiceItemServiceImpl(ServiceItemRepository serviceItemRepository,
                                  BookingServiceDetailRepository bookingServiceDetailRepository,
                                  BookingRepository bookingRepository) {
        this.serviceItemRepository = serviceItemRepository;
        this.bookingServiceDetailRepository = bookingServiceDetailRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceItem> getAllServices() {
        return serviceItemRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceItem> getActiveServices() {
        return serviceItemRepository.findByStatus("Đang bán");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceItem> getServicesByCategory(String category) {
        if (category == null || category.trim().isEmpty() || "Tất cả".equalsIgnoreCase(category)) {
            return serviceItemRepository.findAll();
        }
        return serviceItemRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceItem getServiceById(String id) {
        return serviceItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ/sản phẩm với mã: " + id));
    }

    @Override
    public ServiceItem saveService(ServiceItemDTO dto) {
        ServiceItem item;
        if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
            item = serviceItemRepository.findById(dto.getId().trim())
                    .orElseGet(() -> {
                        ServiceItem ni = new ServiceItem();
                        ni.setId(dto.getId().trim());
                        return ni;
                    });
        } else {
            item = new ServiceItem();
            item.setId(idGenerator.generateId("DV"));
        }

        item.setName(dto.getName().trim());
        item.setCategory(dto.getCategory().trim());
        item.setUnit(dto.getUnit().trim());
        item.setPrice(dto.getPrice());
        item.setStock(dto.getStock() != null ? dto.getStock() : 0);
        item.setImageUrl(dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty() ? dto.getImageUrl().trim() : null);
        item.setStatus(dto.getStatus() != null && !dto.getStatus().trim().isEmpty() ? dto.getStatus().trim() : "Đang bán");

        return serviceItemRepository.save(item);
    }

    @Override
    public void toggleServiceStatus(String id) {
        ServiceItem item = getServiceById(id);
        if ("Đang bán".equalsIgnoreCase(item.getStatus())) {
            item.setStatus("Tạm ngưng");
        } else {
            item.setStatus("Đang bán");
        }
        serviceItemRepository.save(item);
    }

    @Override
    public void deleteService(String id) {
        ServiceItem item = getServiceById(id);
        if (bookingServiceDetailRepository.existsByServiceItem_Id(id)) {
            item.setStatus("Ngừng kinh doanh");
            serviceItemRepository.save(item);
        } else {
            serviceItemRepository.delete(item);
        }
    }

    @Override
    public BookingServiceDetail addServiceToBooking(AddServiceOrderRequestDTO request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt sân: " + request.getBookingId()));

        ServiceItem item = getServiceById(request.getServiceId());

        if (item.getStock() != null && item.getStock() < request.getQuantity()) {
            throw new IllegalStateException("Số lượng tồn kho không đủ (Hiện còn: " + item.getStock() + " " + item.getUnit() + ")");
        }

        // Trừ tồn kho nếu là đồ ăn / thức uống / phụ kiện bán đứt
        if (!"Thuê dụng cụ".equalsIgnoreCase(item.getCategory())) {
            item.setStock(item.getStock() - request.getQuantity());
            serviceItemRepository.save(item);
        }

        BookingServiceDetail detail = BookingServiceDetail.builder()
                .id(idGenerator.generateId("CTDV"))
                .booking(booking)
                .serviceItem(item)
                .quantity(request.getQuantity())
                .unitPrice(item.getPrice())
                .note(request.getNote())
                .orderTime(LocalDateTime.now())
                .build();

        return bookingServiceDetailRepository.save(detail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingServiceDetail> getServicesByBookingId(String bookingId) {
        return bookingServiceDetailRepository.findByBooking_Id(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalServiceAmountByBookingId(String bookingId) {
        List<BookingServiceDetail> list = bookingServiceDetailRepository.findByBooking_Id(bookingId);
        return list.stream()
                .map(BookingServiceDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public void removeServiceFromBooking(String detailId) {
        BookingServiceDetail detail = bookingServiceDetailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết dịch vụ: " + detailId));

        // Hoàn lại tồn kho
        ServiceItem item = detail.getServiceItem();
        if (item != null && !"Thuê dụng cụ".equalsIgnoreCase(item.getCategory())) {
            item.setStock(item.getStock() + detail.getQuantity());
            serviceItemRepository.save(item);
        }

        bookingServiceDetailRepository.delete(detail);
    }
}
