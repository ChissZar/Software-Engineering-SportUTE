# Thiết lập bằng Spring Tool Suite
## Java và Maven
Chọn JDK 21 để nhóm thống nhất môi trường. Spring Boot 3.5 hỗ trợ Java 21.
Máy được kiểm tra khi tạo khung đang có Java 26; cần cài/chọn JDK 21 riêng cho dự án, không cần gỡ Java hiện tại.

1. STS → Window → Preferences → Java → Installed JREs: thêm thư mục JDK 21.
2. File → Import → Maven → Existing Maven Projects → chọn thư mục sportute chứa pom.xml.
3. Project Properties → Java Build Path và Java Compiler: dùng Java 21.
4. Maven → Update Project để tải dependency. STS có Maven tích hợp; không bắt buộc cài Maven dòng lệnh.
5. Run Configurations → Spring Boot App: chọn SportuteApplication, tab JRE chọn JDK 21.

## SQL Server
Dùng SQL Server Developer/Express hoặc instance đã có, quản lý bằng SSMS.
Tạo database rỗng sportute. Bật TCP/IP, xác định cổng instance đang lắng nghe.
URL mẫu dùng localhost:1433; nếu dùng SQL Express có cổng khác thì sửa DB_URL.
Cấu hình SQL authentication phù hợp; khung chưa cấu hình Windows Integrated Authentication.
Trong Run Configurations → Environment đặt:
- DB_URL: jdbc:sqlserver://localhost:1433;databaseName=sportute;encrypt=true;trustServerCertificate=true
- DB_USERNAME: tài khoản SQL Server của bạn
- DB_PASSWORD: mật khẩu tương ứng

trustServerCertificate=true chỉ là cấu hình local; môi trường triển khai cần chứng chỉ hợp lệ và đặt false.
Không commit mật khẩu. File .env.example KHÔNG được tự động nạp.
ddl-auto=none: ứng dụng không tự tạo/sửa bảng. Nhóm cần chốt schema và quản lý DDL/migration.

## Build và chạy
Trong STS: Run As → Maven build → Goals: clean verify. Chọn JDK 21.
Sau đó Run As → Spring Boot App (khi SQL Server và biến môi trường đã sẵn sàng).
Nếu có Maven ngoài STS: mvn clean verify, rồi mvn spring-boot:run.
Chưa cung cấp Maven Wrapper; các lệnh mvnw chưa tồn tại.
Hiện chưa có test tự động; verify chủ yếu kiểm tra biên dịch/đóng gói.
Truy cập localhost:8080 sẽ bị chặn vì chưa có luồng đăng nhập hoặc trang chức năng.

## Cloudinary và JWT
Đã có dependency. JWT dùng các thành phần JwtEncoder/JwtDecoder từ Spring Security OAuth2 JOSE.
Các biến JWT_SECRET, CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET chỉ dành cho bước triển khai tiếp; chưa có bean đọc chúng.
Không gửi secret vào chat, không đặt trong HTML/JS, không commit vào source.

## Docker
Không kèm Docker vì nhóm chọn khung đơn giản và chưa cần môi trường đóng gói. Có thể bổ sung sau khi thống nhất cách chạy.
