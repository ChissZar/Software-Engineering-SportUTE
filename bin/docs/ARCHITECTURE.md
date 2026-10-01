# Kiến trúc và phạm vi
## Cấu trúc theo tầng
src/main/java/vn/edu/ute/sportute/
- config: cấu hình Spring và điểm mở rộng tích hợp.
- controller: nhận request, kiểm tra đầu vào, trả view/DTO.
- service: hợp đồng nghiệp vụ.
- service/impl: xử lý nghiệp vụ và ranh giới transaction.
- repository: truy cập dữ liệu bằng JPA khi entity hoàn chỉnh.
- entity: mô hình lưu trữ; hiện chỉ có lớp mẫu và ID kế thừa.
- dto/request, dto/response: dữ liệu vào/ra.
- mapper: chuyển đổi DTO/entity.
- enums: 5 vai trò ADMIN, MANAGER, RECEPTIONIST, OPERATOR, CUSTOMER.
- security: khung JWT và tải thông tin tài khoản.
- websocket: khung sự kiện và thông báo.
- integration/cloudinary: khung lưu ảnh.
- exception: điểm mở rộng xử lý lỗi.

src/main/resources/templates: giao diện Thymeleaf do nhóm phát triển.
src/main/resources/static: CSS, JS, ảnh tĩnh.
database: vị trí DDL/migration sau khi nhóm chốt thiết kế.
src/test: vị trí kiểm thử sau khi triển khai.

Controller gọi Service, Service gọi Repository. Không đặt nghiệp vụ trong template hoặc controller.
Không trả entity trực tiếp qua API. Không tin giá tiền hoặc userId do trình duyệt gửi lên.

## Đối chiếu module
| Module | Lớp gốc | Phạm vi |
|---|---|---|
| Tài khoản và quyền | Account | Đăng nhập; tài khoản nhân viên có nhiều vai trò |
| Khách hàng | Customer | Hồ sơ, khách online và khách tại quầy |
| Nhân viên | Employee | Hồ sơ và liên kết tài khoản |
| Môn thể thao | Sport | Bóng đá, bóng rổ, cầu lông, pickleball |
| Sân | Court | Danh mục sân thuộc môn |
| Giờ hoạt động | OperatingHour | Thời gian phục vụ |
| Bảng giá | Price | Giá thuê theo quy định nhóm triển khai |
| Chính sách | BookingPolicy | Cọc, đổi, hủy, hoàn tiền |
| Đặt sân | Booking | Online/tại quầy, nhận/trả, gia hạn |
| Thu tiền | Payment | Khoản thu, phiếu thu |
| Hoàn tiền | Refund | Khoản hoàn gắn giao dịch |
| Bảo trì | Maintenance | Lịch bảo trì, phiếu đặt bị ảnh hưởng |
| Khuyến mãi | Promotion | Giảm tiền/phần trăm và phạm vi |
| Báo cáo | Report | Thu ròng, lượt thuê, công suất, khách thường xuyên |
| Thông báo | Notification | Cập nhật lịch và thông báo riêng |

Report và Notification có controller/service mẫu; chưa áp đặt bảng riêng.
Mô hình quan hệ, DTO chi tiết và bảng quyền cần được nhóm chốt trước khi code.
Không có CRUD giả trả thành công hoặc dữ liệu giả được trình bày như dữ liệu thật.

## Lưu ý từ báo cáo
Khách truy cập website phải đăng nhập; khách đặt trực tiếp tại quầy vẫn có thể không có tài khoản, lễ tân nhập thay.
Lưu giá và chính sách tại thời điểm xác nhận phiếu; thay đổi bảng giá không được sửa lịch sử.
Không xóa mất giao dịch đã phát sinh. Có vài mô tả giao diện trong báo cáo đề cập nút Xóa; cần thống nhất với yêu cầu bảo toàn lịch sử trước khi triển khai.
