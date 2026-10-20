SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO
-- Chạy trong database vừa nạp mẫu; thất bại sẽ THROW thay vì báo thành công giả.
SET NOCOUNT ON;
IF EXISTS (SELECT 1 FROM dbo.PHIEUDATSAN b WHERE NOT EXISTS (SELECT 1 FROM dbo.CHITIETDATSAN d WHERE d.MaPhieu=b.MaPhieu) AND b.MaPhieu NOT LIKE 'BL%')
 THROW 51000, N'Phiếu đặt thiếu chi tiết sân',1;
IF EXISTS (SELECT 1 FROM dbo.KHACHHANG WHERE MaKH NOT LIKE 'KH[0-9][0-9][0-9]%' OR SUBSTRING(MaKH,3,10) LIKE '%[^0-9]%')
 THROW 51001, N'Mã khách không đúng quy cách KH + số',1;
IF EXISTS (SELECT MaTK FROM dbo.KHACHHANG WHERE MaTK IS NOT NULL GROUP BY MaTK HAVING COUNT(*)>1)
 THROW 51002, N'Một tài khoản liên kết nhiều hồ sơ khách',1;
IF EXISTS (SELECT 1 FROM dbo.NHANVIEN n JOIN dbo.TAIKHOAN t ON t.MaTK=n.MaTK WHERE n.Email<>t.Email)
 THROW 51003, N'Email nhân viên và tài khoản không khớp',1;
IF EXISTS (SELECT 1 FROM dbo.KHACHHANG k JOIN dbo.TAIKHOAN t ON t.MaTK=k.MaTK WHERE k.Email<>t.Email)
 THROW 51004, N'Email khách và tài khoản không khớp',1;
IF EXISTS (
 SELECT 1 FROM dbo.CHITIETDATSAN a JOIN dbo.PHIEUDATSAN ba ON ba.MaPhieu=a.MaPhieu
 JOIN dbo.CHITIETDATSAN b ON a.MaSan=b.MaSan AND a.MaCT<b.MaCT AND a.GioBD<b.GioKT AND a.GioKT>b.GioBD
 JOIN dbo.PHIEUDATSAN bb ON bb.MaPhieu=b.MaPhieu
 WHERE ba.TrangThai<>N'Đã hủy' AND bb.TrangThai<>N'Đã hủy')
 THROW 51005, N'Hai phiếu đặt cùng sân bị trùng giờ',1;
IF EXISTS (
 SELECT 1 FROM dbo.CHITIETDATSAN d JOIN dbo.PHIEUDATSAN b ON b.MaPhieu=d.MaPhieu
 JOIN dbo.LICHBAOTRI m ON m.MaSan=d.MaSan AND d.GioBD<m.NgayKT AND d.GioKT>m.NgayBD
 WHERE b.TrangThai<>N'Đã hủy' AND m.LifecycleStatus='SCHEDULED')
 THROW 51006, N'Phiếu đặt trùng lịch bảo trì',1;
IF EXISTS (
 SELECT 1 FROM dbo.PHIEUDATSAN b
 OUTER APPLY (SELECT COALESCE(SUM(SoLuong*DonGia),0) total FROM dbo.CHITIETDICHVU d WHERE d.MaPhieu=b.MaPhieu) s
 OUTER APPLY (SELECT COALESCE(SUM(CASE WHEN LoaiGD=N'Hoàn tiền' THEN -SoTien ELSE SoTien END),0) net
   FROM dbo.GIAODICH g WHERE g.MaPhieu=b.MaPhieu AND PaymentStatus IN ('CONFIRMED','LEGACY')) p
 WHERE p.net<0 OR p.net>b.QuotedAmount+s.total OR (b.TrangThai=N'Hoàn tất' AND p.net<>b.QuotedAmount+s.total))
 THROW 51007, N'Thu/hoàn tiền không khớp tổng phiếu và dịch vụ',1;
IF EXISTS (
 SELECT 1 FROM dbo.TIMDOITHU k JOIN dbo.PHIEUDATSAN b ON b.MaPhieu=k.MaPhieu JOIN dbo.KHACHHANG c ON c.MaKH=b.MaKH
 WHERE c.MaTK IS NULL OR JSON_VALUE(k.Payload,'$.creatorAccountId')<>c.MaTK
 OR COALESCE(JSON_VALUE(k.Payload,'$.opponentAccountId'),'')<>COALESCE(k.MaDoiThu,''))
 THROW 51008, N'Kèo không khớp tài khoản của phiếu đặt',1;
IF EXISTS (SELECT 1 FROM dbo.PHIEUDATSAN WHERE CreatedAt IS NULL OR QuotedAmount IS NULL OR DiscountApplied IS NULL)
 THROW 51009, N'Phiếu mẫu thiếu ngày tạo hoặc giá đã chốt',1;
IF EXISTS (SELECT 1 FROM dbo.GIAODICH WHERE CreatedAt IS NULL OR PaymentStatus NOT IN ('CONFIRMED','PENDING','VOID'))
 THROW 51010, N'Giao dịch mẫu thiếu thời điểm hoặc trạng thái rõ ràng',1;
IF EXISTS (SELECT 1 FROM dbo.TAIKHOAN t JOIN dbo.TAIKHOANVAITRO r ON r.MaTK=t.MaTK
 WHERE r.MaVaiTro='VT_KHACH' AND NOT EXISTS (SELECT 1 FROM dbo.KHACHHANG c WHERE c.MaTK=t.MaTK))
 THROW 51011, N'Tài khoản khách thiếu hồ sơ liên kết',1;

-- Đây cũng là cách đối chiếu lịch sử khách với danh sách của nhân viên: cùng MaPhieu.
SELECT t.TenDangNhap,k.MaKH,b.MaPhieu,b.TrangThai,d.MaSan,d.GioBD,d.GioKT,b.QuotedAmount
FROM dbo.PHIEUDATSAN b JOIN dbo.KHACHHANG k ON k.MaKH=b.MaKH
LEFT JOIN dbo.TAIKHOAN t ON t.MaTK=k.MaTK
LEFT JOIN dbo.CHITIETDATSAN d ON d.MaPhieu=b.MaPhieu
ORDER BY k.MaKH,b.CreatedAt DESC;
PRINT N'PASS: quan hệ dữ liệu, mã khách, lịch sân, bảo trì, giao dịch và kèo nhất quán.';
