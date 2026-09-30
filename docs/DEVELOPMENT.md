# Hướng dẫn phát triển tiếp
## Ví dụ thêm chức năng quản lý sân
1. Chốt thuộc tính/quan hệ Court và Sport theo báo cáo, thêm @Entity/@Table, ràng buộc và migration.
2. Hoàn thiện CourtRepository extends JpaRepository<Court, Long>.
3. Tạo DTO request/response và validation; không binding trực tiếp request vào entity.
4. Khai báo phương thức ở CourtService, triển khai trong CourtServiceImpl, thêm @Service và constructor injection.
5. Đặt transaction tại tầng service khi cần. Phương thức chưa làm phải báo rõ chưa hỗ trợ, không trả kết quả thành công giả.
6. Thêm @Controller và route vào CourtController, kiểm tra quyền theo bảng quyền đã thống nhất.
7. Tạo template tương ứng; tận dụng fragments/layout-example.html và Bootstrap WebJar.
8. Viết kiểm thử hành vi trước khi hợp nhất.

## Thứ tự triển khai gợi ý
Chốt entity/schema và phân quyền → đăng nhập → danh mục → đặt sân → thu/hoàn tiền → bảo trì/khuyến mãi → báo cáo → cập nhật thời gian thực.
Đây chỉ là gợi ý phụ thuộc kỹ thuật; nhóm tự phân công thành viên.

## Các kiểm tra quan trọng khi có nghiệp vụ
- Một nhân viên nhiều vai trò; khách không xem được phiếu của khách khác.
- Hai request cùng đặt một sân không cùng thành công khi trùng thời gian.
- Bảo trì và giờ hoạt động được kiểm tra khi tạo/đổi/gia hạn lịch.
- Giá, cọc và chính sách được lưu theo thời điểm xác nhận.
- Thu/hoàn tiền không làm mất lịch sử; xử lý gửi lặp request.
- JWT hết hạn/sai chữ ký, CSRF và subscribe WebSocket trái quyền bị từ chối.
- Upload giới hạn kích thước/định dạng/quyền, không đưa Cloudinary API secret xuống client.

## Trạng thái xác minh khung
Có thể kiểm tra cấu trúc, cú pháp XML và sự hiện diện các lớp mà không có database.
Build đầy đủ cần Maven/JDK phù hợp và tải dependency; chạy ứng dụng cần SQL Server.
Không coi việc có file lớp là chức năng đã hoạt động.
