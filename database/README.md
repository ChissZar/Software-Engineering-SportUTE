# Database kiểm thử SportUTE

Bộ dữ liệu nhỏ, có liên kết khóa ngoại và đủ các trạng thái chính để trình diễn. Đây là dữ liệu mẫu được ghi thật vào SQL Server; giao diện khách và nhân viên đọc cùng bảng, không tạo danh sách đơn mẫu riêng trong trình duyệt.

## Cài bộ dữ liệu sạch

1. Dừng ứng dụng, sao lưu database và thư mục `uploads/` hiện tại nếu cần giữ.
2. Chạy `CreateDB.sql` trong SSMS: tạo lại schema đầy đủ của `QuanLySanTheThao`, bao gồm dịch vụ, giao dịch, kèo và sequence cấp mã.
3. Chạy `InsertDB.sql`: nạp toàn bộ dữ liệu mẫu trong một transaction. Ngày lịch sân/khuyến mãi/bảo trì tính theo ngày chạy script.
4. Chọn đúng database đích, chạy `VerifyData.sql`. Chỉ khi không có lỗi `THROW` và có dòng `PASS` mới tiếp tục.
5. Cấu hình kết nối và khởi động ứng dụng. Đăng nhập lại để tránh dùng phiên đăng nhập cũ.

**Hai script CreateDB và InsertDB thay thế dữ liệu hiện tại. Không chạy trên database cần giữ.** Nếu muốn giữ database cũ và tạo bản demo riêng, đổi tên `QuanLySanTheThao` trong cả hai script sang cùng một tên mới, rồi đổi `DB_URL` của ứng dụng. `VerifyData.sql` chạy trên database đang chọn, không tự đổi database.

`CreateDB.sql` mới đã chứa đầy đủ schema; không cần chạy thêm migration khi cài mới. Nếu chỉ chạy lại `InsertDB.sql`, ứng dụng phải dừng vì script đặt lại bộ cấp mã theo dữ liệu mẫu.

File `data/matchmaking.json` cũ không còn được đọc bởi ứng dụng. Kèo hiện nằm trong bảng `TIMDOITHU`; không sao chép JSON cũ vào bộ mẫu mới. Dữ liệu sở thích/ảnh đại diện trong `uploads/` vẫn là dữ liệu cục bộ: dùng thư mục sạch khi demo với database mới để tránh gán dữ liệu cũ cho mã khách được sử dụng lại. Giữ bản sao trước khi thay thư mục này.

## Tài khoản và mã

Mật khẩu mẫu cho tất cả tài khoản: **12345678**.

| Tài khoản | Mã tài khoản | Hồ sơ | Vai trò |
|---|---|---|---|
| admin | TK001 | NV001 | Quản trị |
| manager | TK002 | NV002 | Quản lý |
| letan01 | TK003 | NV003 | Lễ tân |
| vanhanh01 | TK004 | NV004 | Vận hành |
| minhanh | TK005 | KH001 | Khách hàng |
| quocthuy | TK006 | KH002 | Khách hàng |
| lananh | TK007 | KH003 | Khách hàng |

KH004–KH006 là khách tại quầy, chưa có tài khoản; KH006 đang ngừng hoạt động. Email mẫu dùng miền `example.test`, không nhận thư thật. Muốn thử OTP/gửi email, tạo tài khoản bằng email có thật và cấu hình SMTP.

Mã nghiệp vụ dùng tiền tố và số tăng dần, tối thiểu ba chữ số: `KH001`, `TK001`, `NV001`, `S001`, `DS001`, `GD001`, `DV001`, `CTDV001`, `KEO001`… Sau 999 sẽ là 1000, không quay vòng. SQL Server sequence cấp mã an toàn khi nhiều yêu cầu đồng thời; rollback có thể làm khuyết số, không có nghĩa mất dữ liệu. Không còn cấp mã khách/phiếu bằng UUID.

Mã môn (`M_BD`, `M_CL`, `M_BR`, `M_PB`), vai trò (`VT_ADMIN`…) và chính sách (`CS01`–`CS03`) là mã danh mục cố định đang được nghiệp vụ sử dụng, giữ nguyên có chủ đích.

## Dữ liệu để thử

- 7 tài khoản, 4 nhân viên, 6 khách hàng.
- 4 môn, 8 sân đang hoạt động; mỗi sân có một khung giá 05:00–23:00, tránh giá không khớp giữa các luồng.
- 5 dịch vụ: đủ nhóm thức uống/đồ ăn/thuê dụng cụ/phụ kiện, có mặt hàng hết tồn và tạm ngưng.
- 4 khuyến mãi: giảm tiền, giảm phần trăm, hết hạn, chưa bắt đầu.
- 4 lịch bảo trì: đang thực hiện, sắp tới, hoàn tất, đã hủy. Không khóa vĩnh viễn trạng thái sân để giả lập bảo trì.
- 12 phiếu đặt, 12 chi tiết sân, 12 giao dịch và 1 chi tiết dịch vụ; số tiền tổng/đã thu/hoàn tiền có thể đối chiếu.
- 2 kèo: một chờ nhận, một đã ghép có lời nhắn và liên hệ.

Trong bảng dưới, **D là ngày chạy InsertDB.sql**:

| Phiếu | Khách | Tình huống |
|---|---|---|
| DS001 | KH001 / minhanh | S003, D+1, 14:00–14:30; đã cọc 15.000/50.000; kèo KEO001 chờ nhận. |
| DS002 | KH002 / quocthuy | S003, D+1, 15:00–15:30; đã cọc. Khoảng **14:30–15:00** giữa hai phiếu vẫn đặt được. |
| DS003 | KH001 / minhanh | Chờ xác nhận, chuyển khoản cọc 90.000 ở trạng thái PENDING; thử đối soát thu cọc. |
| DS004 | KH002 / quocthuy | Giảm 20.000 bằng KM001; kèo KEO002 đã được lananh nhận, có lời nhắn. |
| DS005 | KH003 / lananh | Giảm 10% bằng KM002; tổng tiền sân 90.000, đã cọc 27.000. |
| DS006 | KH001 / minhanh | Hoàn tất: tiền sân 100.000 + hai chai nước 20.000 = đã thu 120.000. |
| DS007 | KH002 / quocthuy | Hoàn tất, đã thu đủ 300.000; dùng kiểm tra lịch sử/báo cáo. |
| DS008 | KH003 / lananh | Đã hủy: cọc 36.000, hoàn 32.400, giữ lại phí 3.600. |
| DS009 | KH004 / tại quầy | Chờ xác nhận, chưa có giao dịch thu. |
| DS010 | KH001 / minhanh | Đã hủy, khoản cọc VOID không tính vào tiền đã thu. |
| DS011 | KH002 / quocthuy | S008, D+1, 10:00–11:00; lịch bảo trì trùng khoảng đã hủy nên đặt được. |
| DS012 | KH004 / tại quầy | Đang sử dụng S002 nếu nạp trong 05:00–22:30; ngoài giờ thì là phiếu đã xác nhận lúc 08:00 ngày mai. Đã thu đủ 200.000. |

Lịch bảo trì BT002 chặn S006 vào D+1, 16:00–18:00. Mã KM003/KM004 phải bị từ chối khi áp dụng ở ngày D.

## Kiểm tra đồng bộ trên giao diện

1. Đăng nhập minhanh, xem lịch sử: phải có DS001, DS003, DS006, DS010.
2. Đăng nhập letan01, bỏ bộ lọc từ khóa/trạng thái ở danh sách đặt sân: phải thấy đủ 12 phiếu, bao gồm bốn mã trên với cùng trạng thái và số tiền.
3. Tạo một phiếu mới bằng minhanh; mã phiếu vừa tạo phải xuất hiện ngay trong danh sách nhân viên khi tải lại. Không được xuất hiện trong lịch sử quocthuy.
4. Kiểm tra DS006 ở lịch sử, chi tiết phiếu và thanh toán: tổng cùng là 120.000, gồm tiền dịch vụ.
5. Nhận KEO001 bằng quocthuy hoặc lananh; minhanh thấy thông báo/lời nhắn, người ngoài không thấy liên hệ riêng.
6. Chọn đúng D+1 khi xem lịch sân; chế độ lịch ngày hôm nay không hiển thị lịch ngày mai là hành vi lọc, không phải mất đơn.

## Nâng cấp database cũ mà không reset

Chạy các migration còn thiếu trên database đích:

1. `migrations/20261020_booking_improvements.sql`
2. `migrations/20261020_service_tables.sql`
3. `migrations/20261020_matchmaking_database.sql`
4. `migrations/20261020_numeric_identifiers.sql`

Migration không tự đổi khóa của bản ghi cũ hoặc nạp bộ mẫu. Các mã UUID cũ vẫn còn nếu chọn nâng cấp thay vì cài sạch. Kèo JSON cũ không tự import vì có thể không khớp phiếu/tài khoản; file cũ được giữ nguyên để đối chiếu nếu cần chuyển dữ liệu lịch sử. Bộ mẫu sạch là lựa chọn để kiểm thử theo bảng trên.

## Kết quả xác minh

Đã chạy CreateDB, InsertDB và VerifyData trên một database kiểm thử riêng. Ứng dụng khởi động với Hibernate `ddl-auto=validate`; đã đối chiếu lịch sử ba tài khoản khách với danh sách nhân viên, tổng tiền có dịch vụ, phiếu online mới, kèo trong SQL, bảo trì và cấp 80 mã khách đồng thời không trùng. Giao dịch thử trong ứng dụng được rollback. Database đang dùng của người phát triển không bị reset bởi lần kiểm thử này.
