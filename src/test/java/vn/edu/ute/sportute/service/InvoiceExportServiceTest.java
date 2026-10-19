package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import vn.edu.ute.sportute.entity.BookingServiceDetail;
import vn.edu.ute.sportute.entity.ServiceItem;
import vn.edu.ute.sportute.service.impl.InvoiceExportServiceImpl;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceExportServiceTest {

    @Mock
    private AdminBookingService adminBookingService;

    @Mock
    private ServiceItemService serviceItemService;

    private InvoiceExportServiceImpl invoiceExportService;

    private AdminBookingDTO mockBooking;

    @BeforeEach
    void setUp() {
        invoiceExportService = new InvoiceExportServiceImpl(adminBookingService, serviceItemService);

        mockBooking = AdminBookingDTO.builder()
                .id("DS0101")
                .customerName("Nguyễn Văn A")
                .customerPhone("0912345678")
                .courtName("Sân Cầu Lông 1")
                .sportName("Cầu lông")
                .bookingDate(LocalDate.of(2026, 10, 18))
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(19, 0))
                .serviceAmount(BigDecimal.valueOf(30000))
                .totalAmount(BigDecimal.valueOf(190000))
                .depositAmount(BigDecimal.valueOf(50000))
                .paidAmount(BigDecimal.valueOf(50000))
                .remainingAmount(BigDecimal.valueOf(140000))
                .status("Đang sử dụng")
                .build();
    }

    @Test
    @DisplayName("Xuất hóa đơn PDF thành công cho phiếu đặt sân")
    void testGenerateBookingPdfInvoice_Success() throws Exception {
        ServiceItem item = ServiceItem.builder().id("DV01").name("Revive Chanh Muối").unit("Chai").build();
        BookingServiceDetail detail = BookingServiceDetail.builder()
                .id("CT01")
                .serviceItem(item)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(15000))
                .note("Ướp lạnh")
                .build();

        when(adminBookingService.getBookingById("DS0101")).thenReturn(mockBooking);
        when(serviceItemService.getServicesByBookingId("DS0101")).thenReturn(Collections.singletonList(detail));

        ByteArrayInputStream pdfStream = invoiceExportService.generateBookingPdfInvoice("DS0101");

        assertNotNull(pdfStream, "PDF Stream không được null");
        byte[] bytes = pdfStream.readAllBytes();
        assertTrue(bytes.length > 1000, "Dung lượng PDF phải lớn hơn 1KB");

        // Kiểm tra PDF Header bắt đầu bằng %PDF
        String header = new String(bytes, 0, 4);
        assertEquals("%PDF", header, "Tập tin phải có header chuẩn PDF");
    }

    @Test
    @DisplayName("Xuất hóa đơn chi tiết Excel (XLSX) thành công cho phiếu đặt sân")
    void testGenerateBookingExcelInvoice_Success() throws Exception {
        when(adminBookingService.getBookingById("DS0101")).thenReturn(mockBooking);
        when(serviceItemService.getServicesByBookingId("DS0101")).thenReturn(Collections.emptyList());

        ByteArrayInputStream excelStream = invoiceExportService.generateBookingExcelInvoice("DS0101");

        assertNotNull(excelStream, "Excel Stream không được null");
        byte[] bytes = excelStream.readAllBytes();
        assertTrue(bytes.length > 500, "Dung lượng Excel phải lớn hơn 500 bytes");

        // Kiểm tra magic number của PKZIP (Office Open XML XLSX bắt đầu bằng 'PK')
        assertEquals('P', (char) bytes[0]);
        assertEquals('K', (char) bytes[1]);
    }

    @Test
    @DisplayName("Xuất báo cáo danh sách tất cả phiếu đặt sân ra Excel thành công")
    void testGenerateBookingsListExcel_Success() throws Exception {
        List<AdminBookingDTO> list = new ArrayList<>();
        list.add(mockBooking);

        ByteArrayInputStream excelStream = invoiceExportService.generateBookingsListExcel(list);

        assertNotNull(excelStream, "Excel list Stream không được null");
        byte[] bytes = excelStream.readAllBytes();
        assertTrue(bytes.length > 500, "Dung lượng báo cáo Excel phải lớn hơn 500 bytes");
        assertEquals('P', (char) bytes[0]);
        assertEquals('K', (char) bytes[1]);
    }
}
