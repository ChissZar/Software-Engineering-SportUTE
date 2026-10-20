SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO
USE [QuanLySanTheThao];
GO
-- BỘ DỮ LIỆU KIỂM THỬ: thay toàn bộ dữ liệu nghiệp vụ trong database đích.
-- Dừng ứng dụng và sao lưu trước khi chạy. Không chạy trên dữ liệu cần giữ.
-- Ngày kiểm thử tính theo ngày chạy script, không bị cố định ở tháng/năm cũ.
SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;
DELETE FROM dbo.TIMDOITHU;
DELETE FROM dbo.CHITIETDICHVU;
DELETE FROM dbo.DICHVU;
DELETE FROM dbo.GIAODICH;
DELETE FROM dbo.CHITIETDATSAN;
DELETE FROM dbo.PHIEUDATSAN;
DELETE FROM dbo.LICHBAOTRI;
DELETE FROM dbo.CHITIETBANGGIA;
DELETE FROM dbo.BANGGIA;
DELETE FROM dbo.SAN;
DELETE FROM dbo.MONTHETHAO;
DELETE FROM dbo.KHUYENMAI;
DELETE FROM dbo.CHINHSACH;
DELETE FROM dbo.KHACHHANG;
DELETE FROM dbo.NHANVIEN;
DELETE FROM dbo.TAIKHOANVAITRO;
DELETE FROM dbo.VAITROCHUCNANG;
DELETE FROM dbo.CHUCNANG;
DELETE FROM dbo.VAITRO;
DELETE FROM dbo.TAIKHOAN;
INSERT INTO [dbo].[CHUCNANG] ([MaCN], [TenCN], [MoTa]) VALUES
('CN01', N'Quản lý môn thể thao', N'Xem, thêm, sửa, ngừng kinh doanh môn thể thao'),
('CN02', N'Quản lý sân thể thao', N'Xem, thêm, sửa cấu hình và ngừng khai thác sân'),
('CN03', N'Quản lý bảng giá', N'Cấu hình khung giờ và đơn giá thuê sân'),
('CN04', N'Lập phiếu đặt sân', N'Tiếp nhận và tạo phiếu đặt sân tại quầy'),
('CN05', N'Nhận và trả sân', N'Ghi nhận giờ nhận sân, trả sân và gia hạn sân'),
('CN06', N'Quản lý thanh toán', N'Lập hóa đơn, ghi nhận thu tiền cọc và hoàn tiền'),
('CN07', N'Quản lý lịch bảo trì', N'Lập và theo dõi lịch bảo dưỡng cơ sở vật chất'),
('CN08', N'Quản lý khuyến mãi', N'Tạo và cấu hình các chương trình ưu đãi'),
('CN09', N'Báo cáo thống kê', N'Xem doanh thu, công suất sử dụng và xếp hạng sân'),
('CN10', N'Đặt sân trực tuyến', N'Khách hàng tự tra cứu và đặt sân trên hệ thống');

INSERT INTO [dbo].[VAITRO] ([MaVaiTro], [TenVaiTro], [MoTa]) VALUES
('VT_ADMIN', 'ROLE_ADMIN', N'Quản trị viên toàn quyền hệ thống'),
('VT_QL',    'ROLE_MANAGER', N'Quản lý trung tâm thể thao'),
('VT_LT',    'ROLE_RECEPTIONIST', N'Nhân viên lễ tân tiếp nhận quầy'),
('VT_VH',    'ROLE_OPERATOR', N'Nhân viên vận hành sân bãi'),
('VT_KHACH', 'ROLE_CUSTOMER', N'Khách hàng đặt sân trực tuyến');

-- Gán quyền cho các vai trò
INSERT INTO [dbo].[VAITROCHUCNANG] ([MaVaiTro], [MaCN]) VALUES
('VT_ADMIN', 'CN01'), ('VT_ADMIN', 'CN02'), ('VT_ADMIN', 'CN03'), ('VT_ADMIN', 'CN04'),
('VT_ADMIN', 'CN05'), ('VT_ADMIN', 'CN06'), ('VT_ADMIN', 'CN07'), ('VT_ADMIN', 'CN08'), ('VT_ADMIN', 'CN09'),
('VT_QL', 'CN01'), ('VT_QL', 'CN02'), ('VT_QL', 'CN03'), ('VT_QL', 'CN06'), ('VT_QL', 'CN07'), ('VT_QL', 'CN08'), ('VT_QL', 'CN09'),
('VT_LT', 'CN04'), ('VT_LT', 'CN06'),
('VT_VH', 'CN05'), ('VT_VH', 'CN07'),
('VT_KHACH', 'CN10');


-- Mật khẩu demo: 12345678. Email example.test không dùng gửi thư thật.
INSERT INTO dbo.TAIKHOAN (MaTK,TenDangNhap,MatKhau,Email,TrangThai) VALUES
(N'TK001',N'admin',N'$2a$10$5mj9bFipI7qxCmW8HsHqGeOlynJMCCBb4xRA7PpU1zRaGrz6h3m0C',N'admin@example.test',N'Hoạt động'),
(N'TK002',N'manager',N'$2a$10$5mj9bFipI7qxCmW8HsHqGeOlynJMCCBb4xRA7PpU1zRaGrz6h3m0C',N'manager@example.test',N'Hoạt động'),
(N'TK003',N'letan01',N'$2a$10$5mj9bFipI7qxCmW8HsHqGeOlynJMCCBb4xRA7PpU1zRaGrz6h3m0C',N'letan01@example.test',N'Hoạt động'),
(N'TK004',N'vanhanh01',N'$2a$10$5mj9bFipI7qxCmW8HsHqGeOlynJMCCBb4xRA7PpU1zRaGrz6h3m0C',N'vanhanh01@example.test',N'Hoạt động'),
(N'TK005',N'minhanh',N'$2a$10$5mj9bFipI7qxCmW8HsHqGeOlynJMCCBb4xRA7PpU1zRaGrz6h3m0C',N'minhanh@example.test',N'Hoạt động'),
(N'TK006',N'quocthuy',N'$2a$10$5mj9bFipI7qxCmW8HsHqGeOlynJMCCBb4xRA7PpU1zRaGrz6h3m0C',N'quocthuy@example.test',N'Hoạt động'),
(N'TK007',N'lananh',N'$2a$10$5mj9bFipI7qxCmW8HsHqGeOlynJMCCBb4xRA7PpU1zRaGrz6h3m0C',N'lananh@example.test',N'Hoạt động');

INSERT INTO dbo.TAIKHOANVAITRO (MaTK,MaVaiTro) VALUES
(N'TK001',N'VT_ADMIN'),
(N'TK002',N'VT_QL'),
(N'TK003',N'VT_LT'),
(N'TK004',N'VT_VH'),
(N'TK005',N'VT_KHACH'),
(N'TK006',N'VT_KHACH'),
(N'TK007',N'VT_KHACH');

INSERT INTO dbo.NHANVIEN (MaNV,HoTen,SDT,Email,ChucVu,TrangThai,MaTK) VALUES
(N'NV001',N'Trần Quốc Bảo',N'0901000001',N'admin@example.test',N'Quản trị viên',N'Đang làm',N'TK001'),
(N'NV002',N'Nguyễn Hải Nam',N'0901000002',N'manager@example.test',N'Quản lý',N'Đang làm',N'TK002'),
(N'NV003',N'Lê Thị Thu Hà',N'0901000003',N'letan01@example.test',N'Lễ tân',N'Đang làm',N'TK003'),
(N'NV004',N'Phạm Minh Tuấn',N'0901000004',N'vanhanh01@example.test',N'Kỹ thuật',N'Đang làm',N'TK004');

INSERT INTO dbo.KHACHHANG (MaKH,HoTen,SDT,Email,DiaChi,TrangThai,MaTK) VALUES
(N'KH001',N'Nguyễn Minh Anh',N'0902000001',N'minhanh@example.test',N'TP. Hồ Chí Minh',N'Hoạt động',N'TK005'),
(N'KH002',N'Trần Quốc Huy',N'0902000002',N'quocthuy@example.test',N'TP. Hồ Chí Minh',N'Hoạt động',N'TK006'),
(N'KH003',N'Nguyễn Lan Anh',N'0902000003',N'lananh@example.test',N'TP. Hồ Chí Minh',N'Hoạt động',N'TK007'),
(N'KH004',N'Lê Minh Đức',N'0902000004',NULL,N'TP. Hồ Chí Minh',N'Hoạt động',NULL),
(N'KH005',N'Phạm Gia Huy',N'0902000005',NULL,N'TP. Hồ Chí Minh',N'Hoạt động',NULL),
(N'KH006',N'Võ Mai Anh',N'0902000006',NULL,N'TP. Hồ Chí Minh',N'Ngừng hoạt động',NULL);

INSERT INTO dbo.MONTHETHAO (MaMon,TenMon,MoTa,TrangThai) VALUES
(N'M_BD',N'Bóng đá',N'Môn thể thao tại trung tâm',N'Hoạt động'),
(N'M_CL',N'Cầu lông',N'Môn thể thao tại trung tâm',N'Hoạt động'),
(N'M_BR',N'Bóng rổ',N'Môn thể thao tại trung tâm',N'Hoạt động'),
(N'M_PB',N'Pickleball',N'Môn thể thao tại trung tâm',N'Hoạt động');

INSERT INTO dbo.SAN (MaSan,TenSan,MaMon,ViTri,TrangThai) VALUES
(N'S001',N'Sân bóng đá 1',N'M_BD',N'Khu A',N'Hoạt động'),
(N'S002',N'Sân bóng đá 2',N'M_BD',N'Khu A',N'Hoạt động'),
(N'S003',N'Sân cầu lông 1',N'M_CL',N'Khu B',N'Hoạt động'),
(N'S004',N'Sân cầu lông 2',N'M_CL',N'Khu B',N'Hoạt động'),
(N'S005',N'Sân bóng rổ 1',N'M_BR',N'Khu C',N'Hoạt động'),
(N'S006',N'Sân bóng rổ 2',N'M_BR',N'Khu C',N'Hoạt động'),
(N'S007',N'Sân pickleball 1',N'M_PB',N'Khu D',N'Hoạt động'),
(N'S008',N'Sân pickleball 2',N'M_PB',N'Khu D',N'Hoạt động');

DECLARE @today datetime2 = CONVERT(date, GETDATE());
DECLARE @tomorrow datetime2 = DATEADD(day,1,@today);
DECLARE @now datetime2 = SYSDATETIME();

INSERT INTO dbo.BANGGIA (MaBangGia,MaSan,TuNgay,DenNgay) VALUES
(N'BG001',N'S001',DATEADD(year,-1,@today),NULL),
(N'BG002',N'S002',DATEADD(year,-1,@today),NULL),
(N'BG003',N'S003',DATEADD(year,-1,@today),NULL),
(N'BG004',N'S004',DATEADD(year,-1,@today),NULL),
(N'BG005',N'S005',DATEADD(year,-1,@today),NULL),
(N'BG006',N'S006',DATEADD(year,-1,@today),NULL),
(N'BG007',N'S007',DATEADD(year,-1,@today),NULL),
(N'BG008',N'S008',DATEADD(year,-1,@today),NULL);

-- Một khung giá 05:00–23:00/sân để tổng phiếu khớp tất cả màn hình.
INSERT INTO dbo.CHITIETBANGGIA (MaCTGia,MaBangGia,KhungGio,DonGia) VALUES
(N'CTG001',N'BG001',N'05:00 - 23:00',300000),
(N'CTG002',N'BG002',N'05:00 - 23:00',400000),
(N'CTG003',N'BG003',N'05:00 - 23:00',100000),
(N'CTG004',N'BG004',N'05:00 - 23:00',100000),
(N'CTG005',N'BG005',N'05:00 - 23:00',200000),
(N'CTG006',N'BG006',N'05:00 - 23:00',250000),
(N'CTG007',N'BG007',N'05:00 - 23:00',120000),
(N'CTG008',N'BG008',N'05:00 - 23:00',120000);

INSERT INTO dbo.CHINHSACH (MaCS,TenCS,TyLeCoc,TyLeHuy) VALUES
(N'CS01',N'Tiêu chuẩn: cọc 30%',30,10),
(N'CS02',N'Cao điểm: cọc 50%',50,20),
(N'CS03',N'Thanh toán tại quầy',0,0);

INSERT INTO dbo.KHUYENMAI (MaKM,TenKM,GiaTriGiam,TuNgay,DenNgay,DiscountType) VALUES
(N'KM001',N'Giảm 20.000 đồng',20000,DATEADD(day,-7,@today),DATEADD(day,30,@today),N'AMOUNT'),
(N'KM002',N'Giảm 10 phần trăm',10,DATEADD(day,-7,@today),DATEADD(day,30,@today),N'PERCENT'),
(N'KM003',N'Ưu đãi đã hết hạn',15000,DATEADD(day,-30,@today),DATEADD(day,-1,@today),N'AMOUNT'),
(N'KM004',N'Ưu đãi chưa bắt đầu',15,DATEADD(day,7,@today),DATEADD(day,30,@today),N'PERCENT');

INSERT INTO dbo.DICHVU (MaDV,TenDV,LoaiDV,DonViTinh,DonGia,TonKho,HinhAnh,TrangThai) VALUES
(N'DV001',N'Nước suối 500ml',N'Thức uống',N'Chai',10000,48,NULL,N'Đang bán'),
(N'DV002',N'Bánh năng lượng',N'Đồ ăn nhẹ',N'Gói',20000,30,NULL,N'Đang bán'),
(N'DV003',N'Thuê vợt cầu lông',N'Thuê dụng cụ',N'Cái',30000,10,NULL,N'Đang bán'),
(N'DV004',N'Quấn cán vợt',N'Phụ kiện',N'Cái',25000,0,NULL,N'Đang bán'),
(N'DV005',N'Nước thể thao',N'Thức uống',N'Chai',15000,10,NULL,N'Tạm ngưng');

INSERT INTO dbo.LICHBAOTRI (MaBaoTri,MaSan,MaNV,NgayBD,NgayKT,LyDo,LifecycleStatus) VALUES
(N'BT001',N'S005',N'NV004',DATEADD(hour,-1,@now),DATEADD(hour,2,@now),N'Kiểm tra vành rổ đang thực hiện',N'SCHEDULED'),
(N'BT002',N'S006',N'NV004',DATEADD(hour,16,@tomorrow),DATEADD(hour,18,@tomorrow),N'Thay đèn chiếu sáng',N'SCHEDULED'),
(N'BT003',N'S007',N'NV004',DATEADD(day,-2,@today),DATEADD(hour,2,DATEADD(day,-2,@today)),N'Đã vệ sinh mặt sân',N'COMPLETED'),
(N'BT004',N'S008',N'NV004',DATEADD(hour,10,@tomorrow),DATEADD(hour,12,@tomorrow),N'Lịch đã hủy để kiểm tra khả dụng',N'CANCELLED');

INSERT INTO dbo.PHIEUDATSAN (MaPhieu,MaKH,MaNV,MaKM,MaCS,TrangThai,CreatedAt,QuotedAmount,DiscountApplied) VALUES
(N'DS001',N'KH001',NULL,NULL,N'CS01',N'Đã xác nhận',DATEADD(minute,1,DATEADD(day,-3,@today)),50000,0),
(N'DS002',N'KH002',NULL,NULL,N'CS01',N'Đã xác nhận',DATEADD(minute,2,DATEADD(day,-3,@today)),50000,0),
(N'DS003',N'KH001',NULL,NULL,N'CS01',N'Chờ xác nhận',DATEADD(minute,3,DATEADD(day,-3,@today)),300000,0),
(N'DS004',N'KH002',NULL,N'KM001',N'CS01',N'Đã xác nhận',DATEADD(minute,4,DATEADD(day,-3,@today)),100000,20000),
(N'DS005',N'KH003',NULL,N'KM002',N'CS01',N'Đã xác nhận',DATEADD(minute,5,DATEADD(day,-3,@today)),90000,10000),
(N'DS006',N'KH001',N'NV003',NULL,N'CS03',N'Hoàn tất',DATEADD(minute,6,DATEADD(day,-3,@today)),100000,0),
(N'DS007',N'KH002',N'NV003',NULL,N'CS03',N'Hoàn tất',DATEADD(minute,7,DATEADD(day,-3,@today)),300000,0),
(N'DS008',N'KH003',NULL,NULL,N'CS01',N'Đã hủy',DATEADD(minute,8,DATEADD(day,-3,@today)),120000,0),
(N'DS009',N'KH004',N'NV003',NULL,N'CS03',N'Chờ xác nhận',DATEADD(minute,9,DATEADD(day,-3,@today)),400000,0),
(N'DS010',N'KH001',NULL,NULL,N'CS01',N'Đã hủy',DATEADD(minute,10,DATEADD(day,-3,@today)),100000,0),
(N'DS011',N'KH002',N'NV003',NULL,N'CS03',N'Đã xác nhận',DATEADD(minute,11,DATEADD(day,-3,@today)),120000,0);

INSERT INTO dbo.CHITIETDATSAN (MaCT,MaPhieu,MaSan,GioBD,GioKT) VALUES
(N'CT001',N'DS001',N'S003',DATEADD(minute,840,DATEADD(day,1,@today)),DATEADD(minute,870,DATEADD(day,1,@today))),
(N'CT002',N'DS002',N'S003',DATEADD(minute,900,DATEADD(day,1,@today)),DATEADD(minute,930,DATEADD(day,1,@today))),
(N'CT003',N'DS003',N'S001',DATEADD(minute,1080,DATEADD(day,1,@today)),DATEADD(minute,1140,DATEADD(day,1,@today))),
(N'CT004',N'DS004',N'S007',DATEADD(minute,1140,DATEADD(day,1,@today)),DATEADD(minute,1200,DATEADD(day,1,@today))),
(N'CT005',N'DS005',N'S004',DATEADD(minute,1080,DATEADD(day,2,@today)),DATEADD(minute,1140,DATEADD(day,2,@today))),
(N'CT006',N'DS006',N'S003',DATEADD(minute,1080,DATEADD(day,-1,@today)),DATEADD(minute,1140,DATEADD(day,-1,@today))),
(N'CT007',N'DS007',N'S001',DATEADD(minute,1080,DATEADD(day,-2,@today)),DATEADD(minute,1140,DATEADD(day,-2,@today))),
(N'CT008',N'DS008',N'S007',DATEADD(minute,600,DATEADD(day,2,@today)),DATEADD(minute,660,DATEADD(day,2,@today))),
(N'CT009',N'DS009',N'S002',DATEADD(minute,600,DATEADD(day,1,@today)),DATEADD(minute,660,DATEADD(day,1,@today))),
(N'CT010',N'DS010',N'S004',DATEADD(minute,960,DATEADD(day,3,@today)),DATEADD(minute,1020,DATEADD(day,3,@today))),
(N'CT011',N'DS011',N'S008',DATEADD(minute,600,DATEADD(day,1,@today)),DATEADD(minute,660,DATEADD(day,1,@today)));

INSERT INTO dbo.CHITIETDICHVU (MaCTDV,MaPhieu,MaDV,SoLuong,DonGia,GhiChu,ThoiGian) VALUES
(N'CTDV001',N'DS006',N'DV001',2,10000,N'Hai chai nước đã sử dụng',DATEADD(hour,18,DATEADD(day,-1,@today)));

INSERT INTO dbo.GIAODICH (MaGD,MaPhieu,LoaiGD,SoTien,PhuongThuc,CreatedAt,PaymentStatus) VALUES
(N'GD001',N'DS001',N'Cọc',15000,N'Chuyển khoản',DATEADD(minute,62,DATEADD(day,-3,@today)),N'CONFIRMED'),
(N'GD002',N'DS002',N'Cọc',15000,N'Chuyển khoản',DATEADD(minute,64,DATEADD(day,-3,@today)),N'CONFIRMED'),
(N'GD003',N'DS003',N'Cọc',90000,N'Chuyển khoản',DATEADD(minute,66,DATEADD(day,-3,@today)),N'PENDING'),
(N'GD004',N'DS004',N'Cọc',30000,N'Chuyển khoản',DATEADD(minute,68,DATEADD(day,-3,@today)),N'CONFIRMED'),
(N'GD005',N'DS005',N'Cọc',27000,N'Chuyển khoản',DATEADD(minute,70,DATEADD(day,-3,@today)),N'CONFIRMED'),
(N'GD006',N'DS006',N'Thu',120000,N'Tiền mặt',DATEADD(minute,72,DATEADD(day,-3,@today)),N'CONFIRMED'),
(N'GD007',N'DS007',N'Thu',300000,N'Tiền mặt',DATEADD(minute,74,DATEADD(day,-3,@today)),N'CONFIRMED'),
(N'GD008',N'DS008',N'Cọc',36000,N'Chuyển khoản',DATEADD(minute,76,DATEADD(day,-3,@today)),N'CONFIRMED'),
(N'GD009',N'DS008',N'Hoàn tiền',32400,N'Tiền mặt',DATEADD(minute,77,DATEADD(day,-3,@today)),N'CONFIRMED'),
(N'GD010',N'DS010',N'Cọc',30000,N'Chuyển khoản',DATEADD(minute,80,DATEADD(day,-3,@today)),N'VOID'),
(N'GD011',N'DS011',N'Thu',120000,N'Tiền mặt',DATEADD(minute,82,DATEADD(day,-3,@today)),N'CONFIRMED');

-- Phiếu đang sử dụng trong giờ mở cửa; ngoài giờ thì là lượt đã xác nhận sáng mai.
DECLARE @slotStart datetime2 = CASE WHEN CONVERT(time,@now)>='05:00' AND CONVERT(time,@now)<'22:30'
 THEN DATEADD(minute,(DATEDIFF(minute,@today,@now)/30)*30,@today) ELSE DATEADD(hour,8,@tomorrow) END;
INSERT INTO dbo.PHIEUDATSAN (MaPhieu,MaKH,MaNV,MaCS,TrangThai,CreatedAt,QuotedAmount,DiscountApplied)
VALUES ('DS012','KH004','NV003','CS03',CASE WHEN @slotStart<=@now THEN N'Đang sử dụng' ELSE N'Đã xác nhận' END,DATEADD(minute,-30,@slotStart),200000,0);
INSERT INTO dbo.CHITIETDATSAN (MaCT,MaPhieu,MaSan,GioBD,GioKT)
VALUES ('CT012','DS012','S002',@slotStart,DATEADD(minute,30,@slotStart));
INSERT INTO dbo.GIAODICH (MaGD,MaPhieu,LoaiGD,SoTien,PhuongThuc,CreatedAt,PaymentStatus)
VALUES ('GD012','DS012',N'Thu',200000,N'Tiền mặt',DATEADD(minute,-15,@slotStart),'CONFIRMED');
-- Phiếu hoàn tất: ghi nhận thanh toán sau khi sử dụng dịch vụ.
UPDATE dbo.GIAODICH SET CreatedAt=DATEADD(minute,1,(SELECT MAX(d.GioKT) FROM dbo.CHITIETDATSAN d WHERE d.MaPhieu=GIAODICH.MaPhieu))
WHERE MaPhieu IN ('DS006','DS007');
-- KEO001 chờ nhận (minhanh), KEO002 đã ghép (quocthuy / lananh).
INSERT INTO dbo.TIMDOITHU (MaKeo,MaPhieu,MaDoiThu,Payload)
SELECT 'KEO001','DS001',NULL,(SELECT 'KEO001' AS id,'DS001' AS bookingId,'TK005' AS creatorAccountId,
 'WAITING' AS status,N'Trung bình / Phong trào' AS skillLevel,N'Đánh đôi' AS teamSize,
 N'Tìm bạn giao lưu, chia đôi tiền sân' AS message,CONVERT(varchar(33),@now,126) AS createdAt
 FOR JSON PATH, WITHOUT_ARRAY_WRAPPER);
INSERT INTO dbo.TIMDOITHU (MaKeo,MaPhieu,MaDoiThu,Payload)
SELECT 'KEO002','DS004','TK007',(SELECT 'KEO002' AS id,'DS004' AS bookingId,'TK006' AS creatorAccountId,
 'TK007' AS opponentAccountId,N'Nguyễn Lan Anh' AS opponentName,'0902000003' AS opponentPhone,
 'lananh@example.test' AS opponentEmail,N'Hẹn gặp tại cổng sân trước 10 phút' AS opponentMessage,
 'MATCHED' AS status,N'Giao lưu' AS skillLevel,N'Đánh đôi' AS teamSize,
 CONVERT(varchar(33),@now,126) AS createdAt,CONVERT(varchar(33),@now,126) AS acceptedAt
 FOR JSON PATH, WITHOUT_ARRAY_WRAPPER);
-- Reset sequence CHỈ trong bộ seed thay toàn bộ dữ liệu, không dùng trong migration.
DECLARE @n_TK bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaTK],3,10))),0)+1 FROM dbo.[TAIKHOAN] WHERE [MaTK] LIKE 'TK[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_TK RESTART WITH '+@n_TK);
DECLARE @n_KH bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaKH],3,10))),0)+1 FROM dbo.[KHACHHANG] WHERE [MaKH] LIKE 'KH[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_KH RESTART WITH '+@n_KH);
DECLARE @n_NV bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaNV],3,10))),0)+1 FROM dbo.[NHANVIEN] WHERE [MaNV] LIKE 'NV[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_NV RESTART WITH '+@n_NV);
DECLARE @n_S bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaSan],2,10))),0)+1 FROM dbo.[SAN] WHERE [MaSan] LIKE 'S[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_S RESTART WITH '+@n_S);
DECLARE @n_M bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaMon],2,10))),0)+1 FROM dbo.[MONTHETHAO] WHERE [MaMon] LIKE 'M[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_M RESTART WITH '+@n_M);
DECLARE @n_BG bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaBangGia],3,10))),0)+1 FROM dbo.[BANGGIA] WHERE [MaBangGia] LIKE 'BG[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_BG RESTART WITH '+@n_BG);
DECLARE @n_CTG bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaCTGia],4,10))),0)+1 FROM dbo.[CHITIETBANGGIA] WHERE [MaCTGia] LIKE 'CTG[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_CTG RESTART WITH '+@n_CTG);
DECLARE @n_BT bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaBaoTri],3,10))),0)+1 FROM dbo.[LICHBAOTRI] WHERE [MaBaoTri] LIKE 'BT[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_BT RESTART WITH '+@n_BT);
DECLARE @n_KM bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaKM],3,10))),0)+1 FROM dbo.[KHUYENMAI] WHERE [MaKM] LIKE 'KM[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_KM RESTART WITH '+@n_KM);
DECLARE @n_DS bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaPhieu],3,10))),0)+1 FROM dbo.[PHIEUDATSAN] WHERE [MaPhieu] LIKE 'DS[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_DS RESTART WITH '+@n_DS);
DECLARE @n_BL bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaPhieu],3,10))),0)+1 FROM dbo.[PHIEUDATSAN] WHERE [MaPhieu] LIKE 'BL[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_BL RESTART WITH '+@n_BL);
DECLARE @n_CT bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaCT],3,10))),0)+1 FROM dbo.[CHITIETDATSAN] WHERE [MaCT] LIKE 'CT[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_CT RESTART WITH '+@n_CT);
DECLARE @n_GD bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaGD],3,10))),0)+1 FROM dbo.[GIAODICH] WHERE [MaGD] LIKE 'GD[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_GD RESTART WITH '+@n_GD);
DECLARE @n_DV bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaDV],3,10))),0)+1 FROM dbo.[DICHVU] WHERE [MaDV] LIKE 'DV[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_DV RESTART WITH '+@n_DV);
DECLARE @n_CTDV bigint = (SELECT COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING([MaCTDV],5,10))),0)+1 FROM dbo.[CHITIETDICHVU] WHERE [MaCTDV] LIKE 'CTDV[0-9]%');
EXEC('ALTER SEQUENCE dbo.SEQ_CTDV RESTART WITH '+@n_CTDV);
DECLARE @n_KEO bigint = 3;
EXEC('ALTER SEQUENCE dbo.SEQ_KEO RESTART WITH '+@n_KEO);
COMMIT;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT > 0 ROLLBACK;
 THROW;
END CATCH;
GO
