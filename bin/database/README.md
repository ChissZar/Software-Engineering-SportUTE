# Cơ sở dữ liệu SQL Server
Chưa chốt schema, chưa tạo bảng hoặc dữ liệu mẫu. Entity hiện chỉ là lớp Java mẫu.
Tạo database rỗng tên sportute bằng SSMS, cấu hình TCP/IP và tài khoản SQL Server có quyền phù hợp.
Đặt DB_URL, DB_USERNAME, DB_PASSWORD trong cấu hình chạy của STS.
Không chạy ddl-auto=create/update trên database dùng chung.
Khi nhóm thống nhất mô hình, bổ sung DDL hoặc migration có version tại thư mục này.
Mỗi nhân viên có nhiều vai trò: cần mô hình quan hệ nhiều-nhiều tài khoản/vai trò.
Khách đặt tại quầy có thể không có tài khoản. Không xóa dữ liệu đã được giao dịch tham chiếu.
