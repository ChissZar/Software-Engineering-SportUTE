package vn.edu.ute.sportute.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import vn.edu.ute.sportute.entity.BookingServiceDetail;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.service.InvoiceExportService;
import vn.edu.ute.sportute.service.ServiceItemService;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceExportServiceImpl implements InvoiceExportService {

    private final AdminBookingService adminBookingService;
    private final ServiceItemService serviceItemService;

    // Helper format tiền tệ VNĐ
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount) + " đ";
    }

    // Lấy font tiếng Việt Arial hoặc fallback Helvetica
    private Font getFont(float size, int style, Color color) {
        try {
            File fontFile = new File("C:/Windows/Fonts/arial.ttf");
            if (fontFile.exists()) {
                BaseFont bf = BaseFont.createFont(fontFile.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(bf, size, style, color);
            }
        } catch (Exception e) {
            log.warn("Không thể tải font Arial: {}. Dùng Helvetica fallback.", e.getMessage());
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    @Override
    public ByteArrayInputStream generateBookingPdfInvoice(String bookingId) {
        AdminBookingDTO booking = adminBookingService.getBookingById(bookingId);
        List<BookingServiceDetail> services = serviceItemService.getServicesByBookingId(bookingId);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Color palette
            Color primaryGreen = new Color(5, 150, 105);
            Color darkSlate = new Color(15, 23, 42);
            Color grayText = new Color(100, 116, 139);
            Color lightBg = new Color(248, 250, 252);
            Color borderGray = new Color(226, 232, 240);

            // 1. Header Trung tâm
            Font brandFont = getFont(18, Font.BOLD, primaryGreen);
            Paragraph brand = new Paragraph("TRUNG TÂM THỂ THAO SPORTHUB UTE", brandFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);

            Font subFont = getFont(10, Font.NORMAL, grayText);
            Paragraph sub = new Paragraph("Số 1 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh\nHotline: 1900 6868 • Email: contact@sporthub.ute.edu.vn", subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(12);
            document.add(sub);

            // Đường phân cách
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            PdfPCell divCell = new PdfPCell(new Phrase(""));
            divCell.setBorder(Rectangle.BOTTOM);
            divCell.setBorderColor(primaryGreen);
            divCell.setBorderWidth(2f);
            divider.addCell(divCell);
            divider.setSpacingAfter(14);
            document.add(divider);

            // 2. Tiêu đề Hóa Đơn
            Font titleFont = getFont(15, Font.BOLD, darkSlate);
            Paragraph title = new Paragraph("HÓA ĐƠN DỊCH VỤ & THUÊ SÂN THỂ THAO", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font invoiceMetaFont = getFont(10, Font.ITALIC, grayText);
            String issueTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            Paragraph meta = new Paragraph("Mã hóa đơn: #" + booking.getId() + " • Ngày xuất: " + issueTime, invoiceMetaFont);
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(16);
            document.add(meta);

            // 3. Khung Thông tin Khách hàng & Sân đấu (2 cột)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1f, 1f});
            infoTable.setSpacingAfter(16);

            Font infoLabelFont = getFont(10, Font.BOLD, darkSlate);
            Font infoValFont = getFont(10, Font.NORMAL, darkSlate);

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBackgroundColor(lightBg);
            leftCell.setBorderColor(borderGray);
            leftCell.setPadding(10);
            leftCell.addElement(new Paragraph("THÔNG TIN KHÁCH HÀNG", getFont(11, Font.BOLD, primaryGreen)));
            leftCell.addElement(new Paragraph("Họ và tên: " + booking.getCustomerName(), infoValFont));
            leftCell.addElement(new Paragraph("Số điện thoại: " + (booking.getCustomerPhone() != null ? booking.getCustomerPhone() : "N/A"), infoValFont));
            leftCell.addElement(new Paragraph("Email: " + (booking.getCustomerEmail() != null ? booking.getCustomerEmail() : "N/A"), infoValFont));
            infoTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBackgroundColor(lightBg);
            rightCell.setBorderColor(borderGray);
            rightCell.setPadding(10);
            rightCell.addElement(new Paragraph("THÔNG TIN ĐẶT SÂN", getFont(11, Font.BOLD, primaryGreen)));
            rightCell.addElement(new Paragraph("Sân: " + booking.getCourtName() + " (" + booking.getSportName() + ")", infoValFont));
            rightCell.addElement(new Paragraph("Ngày đá/chơi: " + (booking.getBookingDate() != null ? booking.getBookingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "--/--/----"), infoValFont));
            rightCell.addElement(new Paragraph("Khung giờ: " + (booking.getStartTime() != null ? booking.getStartTime() : "") + " – " + (booking.getEndTime() != null ? booking.getEndTime() : ""), infoValFont));
            infoTable.addCell(rightCell);

            document.add(infoTable);

            // 4. Bảng Chi tiết các khoản mục (Thuê sân + Dịch vụ F&B)
            PdfPTable itemTable = new PdfPTable(5);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{0.6f, 3.2f, 1f, 1.4f, 1.6f});
            itemTable.setSpacingAfter(14);

            // Table Header
            Font thFont = getFont(10, Font.BOLD, Color.WHITE);
            String[] headers = {"STT", "Nội dung / Dịch vụ", "Số lượng", "Đơn giá", "Thành tiền"};
            for (String h : headers) {
                PdfPCell th = new PdfPCell(new Phrase(h, thFont));
                th.setBackgroundColor(primaryGreen);
                th.setPadding(8);
                th.setHorizontalAlignment(Element.ALIGN_CENTER);
                th.setVerticalAlignment(Element.ALIGN_MIDDLE);
                th.setBorderColor(primaryGreen);
                itemTable.addCell(th);
            }

            int stt = 1;
            BigDecimal totalAmount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal serviceAmount = booking.getServiceAmount() != null ? booking.getServiceAmount() : BigDecimal.ZERO;
            BigDecimal courtAmount = totalAmount.subtract(serviceAmount).max(BigDecimal.ZERO);

            Font tdFont = getFont(10, Font.NORMAL, darkSlate);
            Font tdBoldFont = getFont(10, Font.BOLD, darkSlate);

            // Dòng 1: Tiền thuê sân
            addTableRow(itemTable, String.valueOf(stt++),
                    "Thuê sân " + booking.getCourtName() + " (" + booking.getStartTime() + " - " + booking.getEndTime() + ")",
                    "1 Lượt", formatCurrency(courtAmount), formatCurrency(courtAmount), tdFont, tdBoldFont, lightBg);

            // Dòng 2..N: Dịch vụ F&B đi kèm
            if (services != null && !services.isEmpty()) {
                for (BookingServiceDetail s : services) {
                    String sName = s.getServiceItem() != null ? s.getServiceItem().getName() : "Dịch vụ phát sinh";
                    String unit = s.getServiceItem() != null ? s.getServiceItem().getUnit() : "";
                    String noteStr = (s.getNote() != null && !s.getNote().trim().isEmpty()) ? " (" + s.getNote() + ")" : "";
                    addTableRow(itemTable, String.valueOf(stt++),
                            sName + noteStr,
                            s.getQuantity() + " " + unit,
                            formatCurrency(s.getUnitPrice()),
                            formatCurrency(s.getSubtotal()),
                            tdFont, tdBoldFont, Color.WHITE);
                }
            }

            document.add(itemTable);

            // 5. Bảng Tổng kết tài chính (Subtotal, Cọc, Còn lại)
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setWidths(new float[]{1.5f, 1.2f});
            summaryTable.setSpacingAfter(18);

            BigDecimal paid = booking.getPaidAmount() != null ? booking.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal remaining = booking.getRemainingAmount() != null ? booking.getRemainingAmount() : BigDecimal.ZERO;

            addSummaryRow(summaryTable, "Tiền thuê sân:", formatCurrency(courtAmount), getFont(10, Font.NORMAL, grayText), getFont(10, Font.NORMAL, darkSlate));
            addSummaryRow(summaryTable, "Tiền đồ ăn / nước uống:", formatCurrency(serviceAmount), getFont(10, Font.NORMAL, grayText), getFont(10, Font.NORMAL, new Color(21, 128, 61)));
            addSummaryRow(summaryTable, "TỔNG CỘNG THANH TOÁN:", formatCurrency(totalAmount), getFont(11, Font.BOLD, darkSlate), getFont(12, Font.BOLD, primaryGreen));
            addSummaryRow(summaryTable, "Đã đặt cọc / thanh toán:", formatCurrency(paid), getFont(10, Font.NORMAL, grayText), getFont(10, Font.BOLD, new Color(21, 128, 61)));
            addSummaryRow(summaryTable, "CÒN LẠI CẦN THU:", formatCurrency(remaining), getFont(11, Font.BOLD, new Color(185, 28, 28)), getFont(12, Font.BOLD, new Color(185, 28, 28)));

            document.add(summaryTable);

            // 6. Chữ ký & Lời cảm ơn
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{1f, 1f});

            PdfPCell signLeft = new PdfPCell();
            signLeft.setBorder(Rectangle.NO_BORDER);
            signLeft.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph pLeft = new Paragraph("KHÁCH HÀNG\n(Ký và ghi rõ họ tên)\n\n\n\n" + booking.getCustomerName(), getFont(10, Font.NORMAL, darkSlate));
            pLeft.setAlignment(Element.ALIGN_CENTER);
            signLeft.addElement(pLeft);
            signTable.addCell(signLeft);

            PdfPCell signRight = new PdfPCell();
            signRight.setBorder(Rectangle.NO_BORDER);
            signRight.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph pRight = new Paragraph("ĐẠI DIỆN TRUNG TÂM SPORTHUB\n(Thu ngân ký xác nhận)\n\n\n\nQuản trị hệ thống", getFont(10, Font.NORMAL, darkSlate));
            pRight.setAlignment(Element.ALIGN_CENTER);
            signRight.addElement(pRight);
            signTable.addCell(signRight);

            document.add(signTable);

            // Footer note
            Paragraph footerNote = new Paragraph("\n* Quý khách vui lòng kiểm tra kỹ dịch vụ trước khi rời quầy. Cảm ơn Quý khách đã lựa chọn SportHub UTE!", getFont(9, Font.ITALIC, grayText));
            footerNote.setAlignment(Element.ALIGN_CENTER);
            document.add(footerNote);

            document.close();
        } catch (Exception e) {
            log.error("Lỗi tạo hóa đơn PDF cho phiếu #{}: {}", bookingId, e.getMessage(), e);
            throw new RuntimeException("Lỗi xuất hóa đơn PDF: " + e.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addTableRow(PdfPTable table, String col1, String col2, String col3, String col4, String col5,
                             Font normalFont, Font boldFont, Color bgColor) {
        PdfPCell c1 = new PdfPCell(new Phrase(col1, normalFont));
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        c1.setBackgroundColor(bgColor);
        c1.setPadding(6);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(col2, normalFont));
        c2.setBackgroundColor(bgColor);
        c2.setPadding(6);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(col3, normalFont));
        c3.setHorizontalAlignment(Element.ALIGN_CENTER);
        c3.setBackgroundColor(bgColor);
        c3.setPadding(6);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(col4, normalFont));
        c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c4.setBackgroundColor(bgColor);
        c4.setPadding(6);
        table.addCell(c4);

        PdfPCell c5 = new PdfPCell(new Phrase(col5, boldFont));
        c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c5.setBackgroundColor(bgColor);
        c5.setPadding(6);
        table.addCell(c5);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(3);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(3);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);
    }

    // =========================================================================
    // EXCEL EXPORT (APACHE POI)
    // =========================================================================

    @Override
    public ByteArrayInputStream generateBookingExcelInvoice(String bookingId) {
        AdminBookingDTO booking = adminBookingService.getBookingById(bookingId);
        List<BookingServiceDetail> services = serviceItemService.getServicesByBookingId(bookingId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Hóa đơn #" + bookingId);
            sheet.setDisplayGridlines(true);

            // Cell Styles
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            headerStyle.setFillForegroundColor(new XSSFColor(new Color(5, 150, 105), null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont headerFont = (XSSFFont) workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle titleStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont titleFont = (XSSFFont) workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(new XSSFColor(new Color(5, 150, 105), null));
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle boldStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont bFont = (XSSFFont) workbook.createFont();
            bFont.setBold(true);
            boldStyle.setFont(bFont);

            XSSFCellStyle currencyStyle = (XSSFCellStyle) workbook.createCellStyle();
            DataFormat df = workbook.createDataFormat();
            currencyStyle.setDataFormat(df.getFormat("#,##0 \"đ\""));
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            XSSFCellStyle boldCurrencyStyle = (XSSFCellStyle) workbook.createCellStyle();
            boldCurrencyStyle.setDataFormat(df.getFormat("#,##0 \"đ\""));
            boldCurrencyStyle.setFont(bFont);
            boldCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Title row
            Row titleRow = sheet.createRow(1);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("TRUNG TÂM THỂ THAO SPORTHUB UTE - HÓA ĐƠN DỊCH VỤ");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 5));

            // Meta Info
            int r = 3;
            createKeyValueRow(sheet, r++, "Mã phiếu đặt:", "#" + booking.getId(), boldStyle);
            createKeyValueRow(sheet, r++, "Khách hàng:", booking.getCustomerName() + " (" + (booking.getCustomerPhone() != null ? booking.getCustomerPhone() : "N/A") + ")", null);
            createKeyValueRow(sheet, r++, "Sân & Môn thi đấu:", booking.getCourtName() + " - " + booking.getSportName(), null);
            createKeyValueRow(sheet, r++, "Thời gian thi đấu:", (booking.getBookingDate() != null ? booking.getBookingDate().toString() : "") + " | " + booking.getStartTime() + " - " + booking.getEndTime(), null);
            createKeyValueRow(sheet, r++, "Trạng thái:", booking.getStatus(), boldStyle);
            r++;

            // Table Header
            Row thRow = sheet.createRow(r++);
            thRow.setHeightInPoints(24);
            String[] headers = {"STT", "Hạng mục / Dịch vụ", "Số lượng", "Đơn vị tính", "Đơn giá", "Thành tiền"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = thRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int stt = 1;
            BigDecimal totalAmount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal serviceAmount = booking.getServiceAmount() != null ? booking.getServiceAmount() : BigDecimal.ZERO;
            BigDecimal courtAmount = totalAmount.subtract(serviceAmount).max(BigDecimal.ZERO);

            // Row 1: Thuê sân
            Row courtRow = sheet.createRow(r++);
            courtRow.createCell(0).setCellValue(stt++);
            courtRow.createCell(1).setCellValue("Thuê sân " + booking.getCourtName() + " (" + booking.getStartTime() + " - " + booking.getEndTime() + ")");
            courtRow.createCell(2).setCellValue(1);
            courtRow.createCell(3).setCellValue("Lượt");
            Cell cUnitPrice = courtRow.createCell(4);
            cUnitPrice.setCellValue(courtAmount.doubleValue());
            cUnitPrice.setCellStyle(currencyStyle);
            Cell cSubtotal = courtRow.createCell(5);
            cSubtotal.setCellValue(courtAmount.doubleValue());
            cSubtotal.setCellStyle(boldCurrencyStyle);

            // Rows 2..N: Dịch vụ F&B
            if (services != null) {
                for (BookingServiceDetail s : services) {
                    Row sRow = sheet.createRow(r++);
                    sRow.createCell(0).setCellValue(stt++);
                    String name = (s.getServiceItem() != null ? s.getServiceItem().getName() : "Dịch vụ") + (s.getNote() != null ? " (" + s.getNote() + ")" : "");
                    sRow.createCell(1).setCellValue(name);
                    sRow.createCell(2).setCellValue(s.getQuantity());
                    sRow.createCell(3).setCellValue(s.getServiceItem() != null ? s.getServiceItem().getUnit() : "");
                    Cell pCell = sRow.createCell(4);
                    pCell.setCellValue(s.getUnitPrice() != null ? s.getUnitPrice().doubleValue() : 0);
                    pCell.setCellStyle(currencyStyle);
                    Cell subCell = sRow.createCell(5);
                    subCell.setCellValue(s.getSubtotal() != null ? s.getSubtotal().doubleValue() : 0);
                    subCell.setCellStyle(boldCurrencyStyle);
                }
            }

            r++;
            // Summary rows
            createSummaryRow(sheet, r++, "Tổng cộng:", totalAmount.doubleValue(), boldCurrencyStyle);
            createSummaryRow(sheet, r++, "Đã đặt cọc / thanh toán:", booking.getPaidAmount() != null ? booking.getPaidAmount().doubleValue() : 0, boldCurrencyStyle);
            createSummaryRow(sheet, r++, "Còn lại cần thu:", booking.getRemainingAmount() != null ? booking.getRemainingAmount().doubleValue() : 0, boldCurrencyStyle);

            // Auto-size columns
            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 3800));
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            log.error("Lỗi xuất hóa đơn Excel cho #{}: {}", bookingId, e.getMessage(), e);
            throw new RuntimeException("Lỗi tạo file Excel: " + e.getMessage());
        }
    }

    private void createKeyValueRow(Sheet sheet, int r, String key, String val, CellStyle style) {
        Row row = sheet.createRow(r);
        Cell k = row.createCell(1);
        k.setCellValue(key);
        Cell v = row.createCell(2);
        v.setCellValue(val);
        if (style != null) {
            k.setCellStyle(style);
            v.setCellStyle(style);
        }
    }

    private void createSummaryRow(Sheet sheet, int r, String label, double amount, CellStyle style) {
        Row row = sheet.createRow(r);
        Cell cLabel = row.createCell(4);
        cLabel.setCellValue(label);
        Cell cVal = row.createCell(5);
        cVal.setCellValue(amount);
        cVal.setCellStyle(style);
    }

    @Override
    public ByteArrayInputStream generateBookingsListExcel(List<AdminBookingDTO> bookings) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh sách đặt sân");
            sheet.setDisplayGridlines(true);

            // Header Style
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            headerStyle.setFillForegroundColor(new XSSFColor(new Color(5, 150, 105), null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont headerFont = (XSSFFont) workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle currencyStyle = (XSSFCellStyle) workbook.createCellStyle();
            DataFormat df = workbook.createDataFormat();
            currencyStyle.setDataFormat(df.getFormat("#,##0 \"đ\""));
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("BÁO CÁO DANH SÁCH PHIẾU ĐẶT SÂN & DỊCH VỤ - SPORTHUB UTE");

            String[] columns = {"Mã đặt", "Khách hàng", "Số điện thoại", "Sân", "Môn", "Ngày đặt", "Khung giờ", "Tiền thuê sân", "Tiền F&B", "Tổng tiền", "Đã thanh toán", "Còn thiếu", "Trạng thái"};
            Row hRow = sheet.createRow(2);
            hRow.setHeightInPoints(24);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = hRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (AdminBookingDTO b : bookings) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(b.getId());
                row.createCell(1).setCellValue(b.getCustomerName() != null ? b.getCustomerName() : "");
                row.createCell(2).setCellValue(b.getCustomerPhone() != null ? b.getCustomerPhone() : "");
                row.createCell(3).setCellValue(b.getCourtName() != null ? b.getCourtName() : "");
                row.createCell(4).setCellValue(b.getSportName() != null ? b.getSportName() : "");
                row.createCell(5).setCellValue(b.getBookingDate() != null ? b.getBookingDate().toString() : "");
                row.createCell(6).setCellValue((b.getStartTime() != null ? b.getStartTime() : "") + " - " + (b.getEndTime() != null ? b.getEndTime() : ""));

                BigDecimal total = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal serv = b.getServiceAmount() != null ? b.getServiceAmount() : BigDecimal.ZERO;
                BigDecimal court = total.subtract(serv).max(BigDecimal.ZERO);

                Cell cCourt = row.createCell(7);
                cCourt.setCellValue(court.doubleValue());
                cCourt.setCellStyle(currencyStyle);

                Cell cServ = row.createCell(8);
                cServ.setCellValue(serv.doubleValue());
                cServ.setCellStyle(currencyStyle);

                Cell cTotal = row.createCell(9);
                cTotal.setCellValue(total.doubleValue());
                cTotal.setCellStyle(currencyStyle);

                Cell cPaid = row.createCell(10);
                cPaid.setCellValue(b.getPaidAmount() != null ? b.getPaidAmount().doubleValue() : 0);
                cPaid.setCellStyle(currencyStyle);

                Cell cRem = row.createCell(11);
                cRem.setCellValue(b.getRemainingAmount() != null ? b.getRemainingAmount().doubleValue() : 0);
                cRem.setCellStyle(currencyStyle);

                row.createCell(12).setCellValue(b.getStatus() != null ? b.getStatus() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 3200));
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            log.error("Lỗi xuất Excel danh sách đặt sân: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tạo Excel danh sách: " + e.getMessage());
        }
    }
}
