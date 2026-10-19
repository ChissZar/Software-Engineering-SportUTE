package vn.edu.ute.sportute.service;

import java.io.ByteArrayInputStream;
import java.util.List;
import vn.edu.ute.sportute.dto.request.AdminBookingDTO;

public interface InvoiceExportService {
    // 1. Xuất hóa đơn PDF cho 1 phiếu đặt sân (kèm chi tiết thuê sân + dịch vụ F&B)
    ByteArrayInputStream generateBookingPdfInvoice(String bookingId);

    // 2. Xuất hóa đơn Excel cho 1 phiếu đặt sân chi tiết
    ByteArrayInputStream generateBookingExcelInvoice(String bookingId);

    // 3. Xuất danh sách tất cả các phiếu đặt sân ra file Excel (cho kế toán/quản lý)
    ByteArrayInputStream generateBookingsListExcel(List<AdminBookingDTO> bookings);
}
