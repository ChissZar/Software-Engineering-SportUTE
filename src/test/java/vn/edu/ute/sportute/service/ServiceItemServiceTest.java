package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.sportute.dto.request.AddServiceOrderRequestDTO;
import vn.edu.ute.sportute.dto.request.ServiceItemDTO;
import vn.edu.ute.sportute.entity.Booking;
import vn.edu.ute.sportute.entity.BookingServiceDetail;
import vn.edu.ute.sportute.entity.ServiceItem;
import vn.edu.ute.sportute.repository.BookingRepository;
import vn.edu.ute.sportute.repository.BookingServiceDetailRepository;
import vn.edu.ute.sportute.repository.ServiceItemRepository;
import vn.edu.ute.sportute.service.impl.ServiceItemServiceImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceItemServiceTest {

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @Mock
    private BookingServiceDetailRepository bookingServiceDetailRepository;

    @Mock
    private BookingRepository bookingRepository;

    private ServiceItemServiceImpl serviceItemService;

    @BeforeEach
    void setUp() {
        serviceItemService = new ServiceItemServiceImpl(
                serviceItemRepository,
                bookingServiceDetailRepository,
                bookingRepository
        );
    }

    @Test
    @DisplayName("Lấy danh sách tất cả mặt hàng dịch vụ")
    void testGetAllServices() {
        ServiceItem item1 = ServiceItem.builder().id("DV01").name("Revive").price(BigDecimal.valueOf(15000)).build();
        ServiceItem item2 = ServiceItem.builder().id("DV02").name("Redbull").price(BigDecimal.valueOf(20000)).build();

        when(serviceItemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        List<ServiceItem> result = serviceItemService.getAllServices();
        assertEquals(2, result.size());
        assertEquals("Revive", result.get(0).getName());
    }

    @Test
    @DisplayName("Thêm món/dịch vụ thành công và tự động trừ tồn kho")
    void testAddServiceToBooking_Success_InventoryDeducted() {
        Booking booking = Booking.builder().id("DS01").status("Đã xác nhận").build();
        ServiceItem serviceItem = ServiceItem.builder()
                .id("DV01")
                .name("Revive")
                .category("Thức uống")
                .price(BigDecimal.valueOf(15000))
                .stock(100)
                .status("Đang bán")
                .build();

        when(bookingRepository.findById("DS01")).thenReturn(Optional.of(booking));
        when(serviceItemRepository.findById("DV01")).thenReturn(Optional.of(serviceItem));

        AddServiceOrderRequestDTO req = AddServiceOrderRequestDTO.builder()
                .bookingId("DS01")
                .serviceId("DV01")
                .quantity(3)
                .note("Ướp lạnh")
                .build();

        serviceItemService.addServiceToBooking(req);

        // Tồn kho phải giảm từ 100 xuống 97
        assertEquals(97, serviceItem.getStock());
        verify(serviceItemRepository, times(1)).save(serviceItem);
        verify(bookingServiceDetailRepository, times(1)).save(any(BookingServiceDetail.class));
    }

    @Test
    @DisplayName("Báo lỗi khi số lượng đặt vượt quá tồn kho hiện tại")
    void testAddServiceToBooking_InsufficientStock_ThrowsException() {
        Booking booking = Booking.builder().id("DS01").status("Đã xác nhận").build();
        ServiceItem serviceItem = ServiceItem.builder()
                .id("DV01")
                .name("Revive")
                .category("Thức uống")
                .price(BigDecimal.valueOf(15000))
                .stock(2)
                .status("Đang bán")
                .build();

        when(bookingRepository.findById("DS01")).thenReturn(Optional.of(booking));
        when(serviceItemRepository.findById("DV01")).thenReturn(Optional.of(serviceItem));

        AddServiceOrderRequestDTO req = AddServiceOrderRequestDTO.builder()
                .bookingId("DS01")
                .serviceId("DV01")
                .quantity(5)
                .build();

        assertThrows(IllegalStateException.class, () -> serviceItemService.addServiceToBooking(req));
        verify(bookingServiceDetailRepository, never()).save(any());
    }

    @Test
    @DisplayName("Xóa dịch vụ đã gọi thì tự động hoàn trả tồn kho về kho hàng")
    void testRemoveServiceFromBooking_RestoresStock() {
        ServiceItem serviceItem = ServiceItem.builder()
                .id("DV01")
                .name("Revive")
                .category("Thức uống")
                .price(BigDecimal.valueOf(15000))
                .stock(50)
                .build();

        BookingServiceDetail detail = BookingServiceDetail.builder()
                .id("CT01")
                .serviceItem(serviceItem)
                .quantity(4)
                .unitPrice(BigDecimal.valueOf(15000))
                .build();

        when(bookingServiceDetailRepository.findById("CT01")).thenReturn(Optional.of(detail));

        serviceItemService.removeServiceFromBooking("CT01");

        // Tồn kho phải được hoàn lại +4 (50 -> 54)
        assertEquals(54, serviceItem.getStock());
        verify(serviceItemRepository, times(1)).save(serviceItem);
        verify(bookingServiceDetailRepository, times(1)).delete(detail);
    }

    @Test
    @DisplayName("Tính tổng tiền dịch vụ của phiếu đặt sân chính xác")
    void testCalculateServiceTotalForBooking() {
        BookingServiceDetail d1 = BookingServiceDetail.builder()
                .unitPrice(BigDecimal.valueOf(15000))
                .quantity(2)
                .build();
        BookingServiceDetail d2 = BookingServiceDetail.builder()
                .unitPrice(BigDecimal.valueOf(50000))
                .quantity(1)
                .build();

        when(bookingServiceDetailRepository.findByBooking_Id("DS01"))
                .thenReturn(Arrays.asList(d1, d2));

        BigDecimal total = serviceItemService.getTotalServiceAmountByBookingId("DS01");
        // (15000 * 2) + (50000 * 1) = 80000
        assertEquals(BigDecimal.valueOf(80000), total);
    }

    @Test
    @DisplayName("Chuyển đổi trạng thái kinh doanh giữa 'Đang bán' và 'Tạm ngưng'")
    void testToggleServiceStatus() {
        ServiceItem item = ServiceItem.builder()
                .id("DV01")
                .name("Revive")
                .status("Đang bán")
                .build();

        when(serviceItemRepository.findById("DV01")).thenReturn(Optional.of(item));

        serviceItemService.toggleServiceStatus("DV01");
        assertEquals("Tạm ngưng", item.getStatus());

        serviceItemService.toggleServiceStatus("DV01");
        assertEquals("Đang bán", item.getStatus());

        verify(serviceItemRepository, times(2)).save(item);
    }
}
