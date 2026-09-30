# SportUTE
Khung sườn website quản lý trung tâm thể thao đa môn, dựa trên báo cáo CNPM_Nhóm 8.docx. Chỉ có cấu trúc, lớp và file mẫu; nhóm tự phát triển nghiệp vụ và giao diện.

## Công nghệ
Java 21, Spring Boot 3.5.16, Maven, Spring Tool Suite, Thymeleaf, Bootstrap 5.3.8, Spring Data JPA, SQL Server, Spring Security/JWT, WebSocket và Cloudinary.
Java 21 nằm trong phạm vi tương thích của Spring Boot 3.5.16: https://docs.spring.io/spring-boot/3.5/system-requirements.html
Phiên bản Spring Boot và Cloudinary đã được đối chiếu Maven Central khi tạo khung.

## Những gì đã có
- Cấu trúc theo tầng controller → service → repository → entity.
- Lớp mẫu cho toàn bộ nhóm chức năng; enum 5 vai trò.
- Dependency và cấu hình kết nối SQL Server qua biến môi trường.
- Các điểm mở rộng JWT, WebSocket, Cloudinary.
- Thư mục Thymeleaf, fragment Bootstrap mẫu và CSS/JS trống.
- Hướng dẫn môi trường, kiến trúc và cách bổ sung chức năng.

## Những gì chưa triển khai
Không có đăng nhập, tài khoản demo, CRUD, đặt sân, tính giá, thanh toán, upload ảnh, kết nối WebSocket hoặc màn hình chức năng.
Entity chưa có @Entity; repository chưa kế thừa JpaRepository; service/controller chỉ là khung.
Không có schema hoàn chỉnh, quan hệ dữ liệu, DDL, seed hoặc ERD đã chốt.
SecurityConfig chặn mọi HTTP request. Đây là mặc định tạm thời, không phải cơ chế JWT hoàn chỉnh.
Ứng dụng cần SQL Server và thông tin kết nối để khởi động. Sau khi khởi động vẫn chưa có trang sử dụng.

## Bắt đầu
1. Đọc docs/SETUP.md để cấu hình JDK 21, STS và SQL Server.
2. Đọc docs/ARCHITECTURE.md để hiểu quy ước và module.
3. Đọc docs/DECISIONS.md để hiểu JWT, WebSocket và cách chạy.
4. Đọc docs/DEVELOPMENT.md trước khi viết chức năng đầu tiên.

Không lưu mật khẩu hoặc API secret vào Git. .env.example chỉ là mẫu tham khảo, Spring Boot không tự nạp file này.
