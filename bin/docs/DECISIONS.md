# Giải thích các lựa chọn
## JWT với Thymeleaf
Đề xuất JWT trong cookie HttpOnly vì website dùng trang do server render. Trình duyệt tự gửi cookie khi điều hướng và gửi form, không cần gắn Authorization header bằng JavaScript cho mọi trang.

Luồng dự kiến: đăng nhập → server xác minh mật khẩu → ký JWT → đặt cookie → filter kiểm tra JWT ở các request sau → phân quyền.
Đây là thiết kế đề xuất, chưa triển khai.
Cookie cần HttpOnly, SameSite phù hợp và Secure khi dùng HTTPS. Không lưu JWT trong localStorage.
Giữ CSRF cho các thao tác thay đổi dữ liệu; HttpOnly không ngăn CSRF. Không dùng GET cho thao tác ghi.
Cần xác minh chữ ký, thời hạn, issuer/audience; thống nhất thời hạn token và cơ chế đăng xuất/thu hồi. Xóa cookie chỉ xóa token trên trình duyệt đó; token đã bị sao chép vẫn còn hiệu lực nếu không có cơ chế thu hồi.
Khi khóa tài khoản/đổi quyền cần quy định hiệu lực đối với token đã phát hành.
Session thông thường đơn giản hơn cho Thymeleaf, nhưng JWT cookie được đề xuất để giữ đúng công nghệ bạn yêu cầu. Chưa cần kết hợp thêm một luồng đăng nhập session độc lập.

Tài liệu: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html

## Khu vực khách hàng và nhân viên
Đây là các nhóm trang trong cùng website, không phải nhiều ứng dụng:
- customer: khách xem sân, đặt sân và lịch sử của mình.
- staff: quản lý, lễ tân, vận hành xem chức năng được phân quyền.
- admin: quản lý tài khoản và quyền.
- auth: trang đăng nhập; trang này bắt buộc cho phép người chưa đăng nhập truy cập.
Không xây dựng trang giới thiệu/tìm sân công khai. Sau này chỉ mở trang đăng nhập, tài nguyên cần cho trang đó và xử lý lỗi phù hợp.
Ẩn nút trên giao diện không thay thế kiểm tra quyền ở server. Khách chỉ được đọc/sửa dữ liệu của chính mình.
Thư mục hiện chỉ là gợi ý vị trí, nhóm tự quyết định thiết kế trang và URL.

## WebSocket
Đề xuất hai mục đích:
1. Báo lịch sân thay đổi khi có đặt/hủy/nhận/trả sân hoặc bảo trì.
2. Gửi thông báo riêng cho khách liên quan và nhân viên có quyền.

Dự kiến dùng STOMP qua WebSocket, endpoint /ws; chưa bật endpoint.
Topic chung chỉ chứa thông tin lịch không nhạy cảm. Thông báo riêng dùng user destination, ví dụ /user/queue/notifications.
Cần xác thực handshake, kiểm tra quyền SEND/SUBSCRIBE, giới hạn origin và gửi CSRF token khi STOMP CONNECT.
Không cho client tự phát sự kiện đặt sân thành công vào broker.
Phát sự kiện sau transaction commit. Khi reconnect, tải lại dữ liệu qua HTTP để bù sự kiện bị mất.
WebSocket chỉ giúp cập nhật màn hình; chống đặt trùng cần transaction/locking và kiểm tra database.
MVP chưa cần chat. Chưa bắt buộc lưu thông báo vào bảng riêng.

Tài liệu: https://docs.spring.io/spring-security/reference/6.5/servlet/integrations/websocket.html

## Chạy bằng STS hay Docker
STS: ứng dụng chạy từ IDE, kết nối SQL Server đã cài trên máy hoặc server chung. Dễ debug và phù hợp giai đoạn học tập, vì vậy khung này chọn cách đó.
Docker Compose: khai báo các dịch vụ và chạy chúng trong container để đồng bộ môi trường. Cần Docker, tài nguyên máy và cấu hình thêm.
Docker là cách vận hành, không thay thế Java, Maven hoặc SQL Server. Có thể thêm sau, chưa cần cài để sử dụng khung.
