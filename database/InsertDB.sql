USE [QuanLySanTheThao]
GO

-- 0. DỌN DẸP DỮ LIỆU CŨ (THEO THỨ TỰ RÀNG BUỘC KHÓA NGOẠI)
DELETE FROM [dbo].[GIAODICH];
DELETE FROM [dbo].[CHITIETDATSAN];
DELETE FROM [dbo].[PHIEUDATSAN];
DELETE FROM [dbo].[LICHBAOTRI];
DELETE FROM [dbo].[CHITIETBANGGIA];
DELETE FROM [dbo].[BANGGIA];
DELETE FROM [dbo].[SAN];
DELETE FROM [dbo].[MONTHETHAO];
DELETE FROM [dbo].[KHUYENMAI];
DELETE FROM [dbo].[CHINHSACH];
DELETE FROM [dbo].[KHACHHANG];
DELETE FROM [dbo].[NHANVIEN];
DELETE FROM [dbo].[TAIKHOANVAITRO];
DELETE FROM [dbo].[VAITROCHUCNANG];
DELETE FROM [dbo].[CHUCNANG];
DELETE FROM [dbo].[VAITRO];
DELETE FROM [dbo].[TAIKHOAN];
GO

-- ========================================================
-- 1. DANH MỤC CHỨC NĂNG & VAI TRÒ (PHÂN QUYỀN)
-- ========================================================
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

-- ========================================================
-- 2. TÀI KHOẢN NGƯỜI DÙNG (MẬT KHẨU: 12345678)
-- ========================================================
INSERT INTO [dbo].[TAIKHOAN] ([MaTK], [TenDangNhap], [MatKhau], [Email], [TrangThai]) VALUES
('TK001', 'admin',     '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi58cvs4xX1k42G3sBfO95vL.fK/k2q', 'admin@sporthub.vn',    N'Hoạt động'),
('VT001', 'manager',   '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi58cvs4xX1k42G3sBfO95vL.fK/k2q', 'manager@sporthub.vn',  N'Hoạt động'),
('TK002', 'letan01',   '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi58cvs4xX1k42G3sBfO95vL.fK/k2q', 'letan@sporthub.vn',    N'Hoạt động'),
('TK003', 'vanhanh01', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi58cvs4xX1k42G3sBfO95vL.fK/k2q', 'vanhanh@sporthub.vn',  N'Hoạt động'),
('TK004', 'minhanh',   '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi58cvs4xX1k42G3sBfO95vL.fK/k2q', 'minhanh@gmail.com',    N'Hoạt động'),
('TK005', 'quocthuy',  '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi58cvs4xX1k42G3sBfO95vL.fK/k2q', 'quocthuy@gmail.com',   N'Hoạt động');

INSERT INTO [dbo].[TAIKHOANVAITRO] ([MaTK], [MaVaiTro]) VALUES
('TK001', 'VT_ADMIN'),
('VT001', 'VT_QL'),
('TK002', 'VT_LT'),
('TK003', 'VT_VH'),
('TK004', 'VT_KHACH'),
('TK005', 'VT_KHACH');

-- ========================================================
-- 3. HỒ SƠ NHÂN VIÊN VÀ KHÁCH HÀNG
-- ========================================================
INSERT INTO [dbo].[NHANVIEN] ([MaNV], [HoTen], [SDT], [Email], [ChucVu], [TrangThai], [MaTK]) VALUES
('NV01', N'Trần Quốc Bảo',   '0901234567', 'admin@sporthub.vn',   N'Quản trị viên', N'Đang làm', 'TK001'),
('NV02', N'Lê Thị Thu Hà',   '0912345678', 'letan@sporthub.vn',   N'Lễ tân',        N'Đang làm', 'TK002'),
('NV03', N'Phạm Minh Tuấn',  '0933456789', 'vanhanh@sporthub.vn', N'Kỹ thuật',      N'Đang làm', 'TK003');

INSERT INTO [dbo].[KHACHHANG] ([MaKH], [HoTen], [SDT], [Email], [DiaChi], [TrangThai], [MaTK]) VALUES
('KH001', N'Nguyễn Minh Anh', '0908111222', 'minhanh@gmail.com',  N'TP. Thủ Đức, TP.HCM', N'Hoạt động', 'TK004'),
('KH002', N'Trần Quốc Huy',   '0918222333', 'quocthuy@gmail.com', N'Quận 1, TP.HCM',       N'Hoạt động', 'TK005'),
('KH003', N'Lê Minh Đức',     '0938333444', 'minhduc@gmail.com',  N'Dĩ An, Bình Dương',    N'Hoạt động', NULL),
('KH004', N'Phạm Gia Huy',    '0948444555', 'giahuy@gmail.com',   N'Thuận An, Bình Dương', N'Hoạt động', NULL),
('KH005', N'Võ Thị Mai Anh',  '0968555666', 'maianh@gmail.com',   N'Quận 9, TP.HCM',       N'Hoạt động', NULL);

-- ========================================================
-- 4. DANH MỤC MÔN THỂ THAO VÀ SÂN BÃI
-- ========================================================
INSERT INTO [dbo].[MONTHETHAO] ([MaMon], [TenMon], [MoTa], [TrangThai]) VALUES
('M_BD', N'Bóng đá',    N'Sân bóng đá cỏ nhân tạo tiêu chuẩn', N'Hoạt động'),
('M_CL', N'Cầu lông',   N'Sân thảm tiêu chuẩn BWF có máy lạnh', N'Hoạt động'),
('M_BR', N'Bóng rổ',    N'Sân bóng rổ sàn gỗ thi đấu trong nhà', N'Hoạt động'),
('M_PB', N'Pickleball', N'Sân pickleball phủ sơn tiêu chuẩn quốc tế', N'Hoạt động');

INSERT INTO [dbo].[SAN] ([MaSan], [TenSan], [MaMon], [ViTri], [TrangThai]) VALUES
('S01', N'Sân bóng đá 1',    'M_BD', N'Sân 5 người - Khu A',     N'Hoạt động'),
('S02', N'Sân bóng đá 2',    'M_BD', N'Sân 7 người - Khu A',     N'Đang sử dụng'),
('S03', N'Sân cầu lông 1',   'M_CL', N'Sân tiêu chuẩn - Nhà B1', N'Hoạt động'),
('S04', N'Sân cầu lông 2',   'M_CL', N'Sân tiêu chuẩn - Nhà B2', N'Đang sử dụng'),
('S05', N'Sân bóng rổ 1',    'M_BR', N'Nửa sân - Khu C',         N'Bảo trì'),
('S06', N'Sân bóng rổ 2',    'M_BR', N'Sân thi đấu - Khu C',     N'Hoạt động'),
('S07', N'Sân pickleball 1', 'M_PB', N'Sân tiêu chuẩn - Khu D',  N'Hoạt động'),
('S08', N'Sân pickleball 2', 'M_PB', N'Sân tiêu chuẩn - Khu D',  N'Đang sử dụng');

-- ========================================================
-- 5. BẢNG GIÁ VÀ CHI TIẾT BẢNG GIÁ
-- ========================================================
INSERT INTO [dbo].[BANGGIA] ([MaBangGia], [MaSan], [TuNgay], [DenNgay]) VALUES
('BG01', 'S01', '2026-01-01', NULL),
('BG02', 'S02', '2026-01-01', NULL),
('BG03', 'S03', '2026-01-01', NULL),
('BG04', 'S04', '2026-01-01', NULL),
('BG05', 'S05', '2026-01-01', NULL),
('BG06', 'S06', '2026-01-01', NULL),
('BG07', 'S07', '2026-01-01', NULL),
('BG08', 'S08', '2026-01-01', NULL);

INSERT INTO [dbo].[CHITIETBANGGIA] ([MaCTGia], [MaBangGia], [KhungGio], [DonGia]) VALUES
('CTG01', 'BG01', N'05:00 - 16:00', 250000),
('CTG02', 'BG01', N'16:00 - 23:00', 350000),
('CTG03', 'BG02', N'05:00 - 16:00', 400000),
('CTG04', 'BG02', N'16:00 - 23:00', 500000),
('CTG05', 'BG03', N'05:00 - 17:00', 90000),
('CTG06', 'BG03', N'17:00 - 23:00', 120000),
('CTG07', 'BG04', N'05:00 - 17:00', 90000),
('CTG08', 'BG04', N'17:00 - 23:00', 120000),
('CTG09', 'BG05', N'06:00 - 22:00', 150000),
('CTG10', 'BG06', N'06:00 - 22:00', 280000),
('CTG11', 'BG07', N'05:00 - 17:00', 100000),
('CTG12', 'BG07', N'17:00 - 23:00', 150000),
('CTG13', 'BG08', N'05:00 - 17:00', 100000),
('CTG14', 'BG08', N'17:00 - 23:00', 150000);

-- ========================================================
-- 6. CHÍNH SÁCH VÀ CHƯƠNG TRÌNH KHUYẾN MÃI
-- ========================================================
INSERT INTO [dbo].[CHINHSACH] ([MaCS], [TenCS], [TyLeCoc], [TyLeHuy]) VALUES
('CS01', N'Chính sách tiêu chuẩn', 30.00, 10.00),
('CS02', N'Chính sách ngày lễ/cao điểm', 50.00, 20.00),
('CS03', N'Đặt sân trực tiếp tại quầy', 0.00, 0.00);

INSERT INTO [dbo].[KHUYENMAI] ([MaKM], [TenKM], [GiaTriGiam], [TuNgay], [DenNgay]) VALUES
('KM_OPEN',    N'Ưu đãi khai trương cơ sở', 50000, '2026-01-01', '2026-12-31'),
('KM_FRIENDS', N'Khuyến mãi nhóm bạn thể thao', 20000, '2026-09-01', '2026-10-31');

-- ========================================================
-- 7. LỊCH BẢO TRÌ SÂN
-- ========================================================
INSERT INTO [dbo].[LICHBAOTRI] ([MaBaoTri], [MaSan], [MaNV], [NgayBD], [NgayKT], [LyDo]) VALUES
('BT01', 'S05', 'NV03', '2026-10-01 07:00:00', '2026-10-03 18:00:00', N'Bảo trì định kỳ và thay vành rổ tiêu chuẩn'),
('BT02', 'S01', 'NV03', '2026-10-15 08:00:00', '2026-10-15 12:00:00', N'Bảo dưỡng và thay bóng đèn LED chiếu sáng');

-- ========================================================
-- 8. PHIẾU ĐẶT SÂN, CHI TIẾT ĐẶT SÂN VÀ GIAO DỊCH
-- ========================================================
-- Phiếu 1: Khách đặt online, đã xác nhận và đã cọc
INSERT INTO [dbo].[PHIEUDATSAN] ([MaPhieu], [MaKH], [MaNV], [MaKM], [MaCS], [TrangThai]) VALUES
('DS0101', 'KH001', 'NV02', 'KM_FRIENDS', 'CS01', N'Đã xác nhận');

INSERT INTO [dbo].[CHITIETDATSAN] ([MaCT], [MaPhieu], [MaSan], [GioBD], [GioKT]) VALUES
('CT0101', 'DS0101', 'S03', '2026-10-02 18:00:00', '2026-10-02 20:00:00');

INSERT INTO [dbo].[GIAODICH] ([MaGD], [MaPhieu], [LoaiGD], [SoTien], [PhuongThuc]) VALUES
('GD0101', 'DS0101', N'Cọc', 70000, N'Chuyển khoản');

-- Phiếu 2: Đang sử dụng sân tại quầy
INSERT INTO [dbo].[PHIEUDATSAN] ([MaPhieu], [MaKH], [MaNV], [MaKM], [MaCS], [TrangThai]) VALUES
('DS0102', 'KH003', 'NV02', NULL, 'CS03', N'Đang sử dụng');

INSERT INTO [dbo].[CHITIETDATSAN] ([MaCT], [MaPhieu], [MaSan], [GioBD], [GioKT]) VALUES
('CT0102', 'DS0102', 'S02', '2026-10-02 08:00:00', '2026-10-02 10:00:00');

INSERT INTO [dbo].[GIAODICH] ([MaGD], [MaPhieu], [LoaiGD], [SoTien], [PhuongThuc]) VALUES
('GD0102', 'DS0102', N'Thu', 1000000, N'Tiền mặt');

-- Phiếu 3: Đã hoàn tất và thanh toán toàn bộ
INSERT INTO [dbo].[PHIEUDATSAN] ([MaPhieu], [MaKH], [MaNV], [MaKM], [MaCS], [TrangThai]) VALUES
('DS0103', 'KH002', 'NV02', 'KM_OPEN', 'CS01', N'Hoàn tất');

INSERT INTO [dbo].[CHITIETDATSAN] ([MaCT], [MaPhieu], [MaSan], [GioBD], [GioKT]) VALUES
('CT0103', 'DS0103', 'S07', '2026-09-30 19:00:00', '2026-09-30 21:00:00');

INSERT INTO [dbo].[GIAODICH] ([MaGD], [MaPhieu], [LoaiGD], [SoTien], [PhuongThuc]) VALUES
('GD0103', 'DS0103', N'Thu', 250000, N'Chuyển khoản');

-- Phiếu 4: Phiếu bị hủy và đã xử lý hoàn tiền
INSERT INTO [dbo].[PHIEUDATSAN] ([MaPhieu], [MaKH], [MaNV], [MaKM], [MaCS], [TrangThai]) VALUES
('DS0104', 'KH004', 'NV02', NULL, 'CS01', N'Đã hủy');

INSERT INTO [dbo].[CHITIETDATSAN] ([MaCT], [MaPhieu], [MaSan], [GioBD], [GioKT]) VALUES
('CT0104', 'DS0104', 'S01', '2026-09-29 17:00:00', '2026-09-29 18:30:00');

INSERT INTO [dbo].[GIAODICH] ([MaGD], [MaPhieu], [LoaiGD], [SoTien], [PhuongThuc]) VALUES
('GD0104', 'DS0104', N'Cọc', 150000, N'Chuyển khoản'),
('GD0105', 'DS0104', N'Hoàn tiền', 135000, N'Chuyển khoản');
GO