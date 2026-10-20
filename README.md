# SportUTE — Hệ thống quản lý trung tâm thể thao

Đồ án Công nghệ phần mềm của Nhóm 08. Ứng dụng hỗ trợ khách hàng đặt sân, tìm đối thủ và theo dõi lịch sử; đồng thời cung cấp chức năng quản lý sân, nhân viên, bảo trì, khuyến mãi và thu tiền tại trung tâm. Giao diện sử dụng tên SportHub.

## Công nghệ

- Java 17, Maven, Spring Boot 3.5.16.
- Spring MVC, Thymeleaf, HTML/CSS/JavaScript và Bootstrap.
- Spring Data JPA/Hibernate, Microsoft SQL Server.
- Spring Security: đăng nhập bằng form và phân quyền theo vai trò.
- Spring Mail cho email/OTP; Cloudinary cho lưu ảnh khi được cấu hình.
- OpenPDF và Apache POI phục vụ xuất PDF/Excel.
- JUnit 5, Mockito và Spring Security Test cho kiểm thử.

## Chức năng chính

| Phân hệ | Chức năng |
|---|---|
| Khách hàng | Đăng ký, đăng nhập, khôi phục mật khẩu, quản lý hồ sơ, xem sân và đặt sân, lịch sử đặt sân, hóa đơn PDF. |
| Tìm đối thủ | Đăng/nhận/hủy kèo, xem kèo của mình, thông báo nhận kèo; hai bên tham gia xem thông tin liên hệ và lời nhắn. |
| Đặt sân tại quầy | Tạo phiếu, theo dõi lịch sân, nhận/trả sân, dịch vụ đi kèm và xử lý đổi/hủy theo nghiệp vụ. |
| Danh mục | Quản lý môn thể thao, sân, bảng giá, dịch vụ, khách hàng và khuyến mãi. |
| Bảo trì | Tạo, hủy hoặc hoàn tất lịch bảo trì; chặn đặt sân trong khoảng bảo trì còn hiệu lực. |
| Thanh toán | Ghi nhận thu cọc/thu tiền/hoàn tiền, xác nhận hoặc hủy khoản chờ và đối chiếu số còn thiếu. |
| Quản trị | Quản lý tài khoản, nhân viên, quyền truy cập, dashboard và báo cáo. |

Hệ thống có năm vai trò: `ADMIN`, `MANAGER`, `RECEPTIONIST`, `OPERATOR`, `CUSTOMER`. Quản lý được quản lý nhân viên thông thường; quyền chỉnh sửa tài khoản quản trị/quản lý được giới hạn ở server.

Một số quy tắc hiện tại:

- Lịch sử ưu tiên phiếu mới tạo. Phiếu cũ chưa có thời điểm tạo được xếp sau.
- Hỗ trợ đặt theo bước 30 phút; hai lượt sát giờ không bị xem là trùng. Ví dụ, có lượt 14:00–14:30 và 15:00–15:30 thì vẫn đặt được 14:30–15:00.
- Giá và khuyến mãi được tính lại ở server. Mỗi phiếu áp dụng một mã giảm tiền hoặc phần trăm hợp lệ, không cộng dồn với ưu đãi thành viên.
- Khai báo chuyển khoản online tạo khoản chờ xác nhận, chưa tính là tiền đã thu. Hiện chưa tích hợp đối soát tự động/webhook ngân hàng.

## Yêu cầu môi trường

- JDK 17 và Maven 3.9.x; có thể dùng Maven tích hợp trong Spring Tool Suite (STS).
- SQL Server đang hoạt động, bật TCP/IP và có tài khoản kết nối phù hợp.
- Internet để tải dependency lần đầu. Một số tài nguyên giao diện sử dụng CDN.
- SMTP hợp lệ nếu thử đăng ký/khôi phục mật khẩu qua email. Node.js chỉ cần khi chạy công cụ kiểm tra JavaScript.

Project hiện không kèm Maven Wrapper; các lệnh dưới đây dùng `mvn`.

## Cài đặt và chạy

### 1. Chuẩn bị database

**Với môi trường mới**, mở SQL Server Management Studio và chạy theo thứ tự:

1. `database/CreateDB.sql` — tạo database `QuanLySanTheThao` và các bảng.
2. `database/InsertDB.sql` — nạp dữ liệu mẫu.
3. Chọn database `QuanLySanTheThao`, chạy `database/VerifyData.sql` để kiểm tra tính nhất quán. Schema mới đã bao gồm bảng dịch vụ, kèo và bộ cấp mã; không cần chạy thêm migration khi cài mới.

**Lưu ý:** `CreateDB.sql` có lệnh xóa bảng, `InsertDB.sql` có lệnh xóa dữ liệu trước khi nạp mẫu. Không chạy lại hai script này trên database cần giữ dữ liệu.

**Với database đã có schema cũ**, sao lưu trước rồi chạy các migration theo thứ tự trong [hướng dẫn database](database/README.md). Ứng dụng dùng `ddl-auto=none`, không tự tạo hoặc nâng cấp schema. Bộ dữ liệu sạch có 12 phiếu đặt; ngày demo tính theo ngày chạy script. Mã mới dùng tiền tố + số tăng dần (ví dụ KH007, DS013), không dùng UUID.

### 2. Cấu hình môi trường

Tạo file `.env` ở thư mục gốc, cùng cấp với `pom.xml`. Khi chạy qua `SportuteApplication`, ứng dụng tự đọc file này từ thư mục làm việc. Có thể cấu hình tương đương trong STS → Run Configurations → Environment.

```dotenv
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=QuanLySanTheThao;encrypt=true;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=your_sql_username
SPRING_DATASOURCE_PASSWORD=your_sql_password
PORT=8081

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email
MAIL_PASSWORD=your_smtp_app_password

CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
```

Thay các giá trị mẫu bằng cấu hình máy chạy. Với Gmail, dùng mật khẩu ứng dụng cho SMTP. `trustServerCertificate=true` trong ví dụ dành cho môi trường local.

Dùng đúng `SPRING_DATASOURCE_USERNAME` và `SPRING_DATASOURCE_PASSWORD` để ghi đè cấu hình datasource hiện tại; `DB_USERNAME`/`DB_PASSWORD` trong `.env.example` cũ chưa được liên kết ở `application.yml`. Không đưa `.env` hoặc thông tin xác thực thật vào bản nộp hay repository. Loại bỏ thông tin xác thực máy cá nhân còn lưu trực tiếp trong `application.yml` trước khi chia sẻ source.

### 3. Chạy ứng dụng

Tại thư mục chứa `pom.xml`:

```bash
mvn spring-boot:run
```

Truy cập: http://localhost:8081/login (hoặc cổng đã đặt trong `PORT`).

Trong STS: Import → Maven → Existing Maven Projects, chọn JDK 17, đặt working directory là thư mục project rồi chạy `SportuteApplication` bằng Spring Boot App.

Để đóng gói và chạy JAR:

```bash
mvn clean package
java -jar target/sportute-0.0.1-SNAPSHOT.jar
```

Chạy JAR từ thư mục gốc để ứng dụng tìm đúng `.env`, `data/` và `uploads/`.

### 4. Tài khoản dữ liệu mẫu

Script `InsertDB.sql` khai báo mật khẩu mẫu `12345678` cho các tài khoản sau:

| Tài khoản | Vai trò |
|---|---|
| `admin` | Quản trị |
| `manager` | Quản lý |
| `letan01` | Lễ tân |
| `vanhanh01` | Vận hành |
| `minhanh`, `quocthuy`, `lananh` | Khách hàng |

Tài khoản trên database đã sử dụng có thể đã được đổi mật khẩu hoặc trạng thái; tham khảo dữ liệu thực tế thay vì chạy lại seed để khôi phục.

## Kiểm thử

```bash
mvn test
```

Nếu IDE đang biên dịch vào `target/classes`, có thể dùng thư mục build riêng:

```bash
mvn -Dsportute.build.directory=target/codex-verify test
```

Kiểm tra cú pháp JavaScript trong template, nếu giữ thư mục `tools/`:

```bash
node tools/check-js.cjs
```

Ở lần xác minh ngày 20/10/2026, 38 test Java và 29 khối JavaScript trong template đã qua. Các tình huống kiểm tra gồm tính giá/khuyến mãi ở server, đặt sát giờ, giao dịch chờ xác nhận, giới hạn thu tiền, quyền quản lý nhân viên, bảo trì và quyền xem liên hệ trong kèo. Kiểm tra trực tiếp SQL Server về đặt sát giờ và bảo trì đã rollback dữ liệu thử. Đây là kết quả tại thời điểm xác minh, không thay thế việc chạy lại kiểm thử khi sửa source.

## Cấu trúc thư mục

```text
src/main/java/vn/edu/ute/sportute/
  config/                 Cấu hình ứng dụng và bảo mật
  controller/             Nhận request, trả trang hoặc dữ liệu
  service/                Hợp đồng và xử lý nghiệp vụ
  repository/             Truy cập database
  entity/                 Mô hình JPA
  dto/                    Dữ liệu đầu vào/đầu ra
  integration/            Tích hợp dịch vụ ngoài
src/main/resources/
  templates/              Giao diện Thymeleaf đang sử dụng
  static/                 CSS, JavaScript, ảnh và tài nguyên tĩnh
  application.yml         Cấu hình Spring Boot
src/test/                 Kiểm thử tự động
database/                 Script tạo bảng, dữ liệu mẫu và migration
data/                     File kèo cũ, không còn là nguồn dữ liệu runtime
uploads/                  Ảnh đại diện và dữ liệu tùy chọn người dùng
docs/                     Báo cáo, tài liệu và giao diện mẫu
tools/                    Công cụ kiểm tra hỗ trợ
```

## Lưu ý về dữ liệu và phạm vi

- Kèo và trạng thái đọc thông báo nằm trong bảng `TIMDOITHU`, liên kết khóa ngoại với phiếu đặt và tài khoản. File `data/matchmaking.json` cũ không còn được đọc. `uploads/` vẫn lưu ảnh/sở thích người dùng; sao lưu cùng database khi chuyển máy, và dùng thư mục sạch khi reset bộ dữ liệu demo để tránh lẫn hồ sơ cũ.
- Một số ảnh tải lên có thể được lưu cục bộ trong `src/main/resources/static/uploads/`; giữ các ảnh còn được dữ liệu tham chiếu.
- Thông báo kèo được kiểm tra định kỳ mỗi 30 giây. Thông tin sân, thời gian và chủ kèo được lấy từ phiếu đặt trong SQL Server, không từ dữ liệu mẫu của trình duyệt.
- Project chưa có quan hệ trung tâm–nhân viên để phân quyền theo nhiều chi nhánh.
- Giao dịch cũ được đánh dấu `LEGACY`, không tự gán ngày tạo hoặc coi là đã đối soát ngân hàng. Khuyến mãi cũ mặc định là giảm số tiền; cần kiểm tra lại loại giảm nếu dữ liệu cũ dùng phần trăm.
- Các trang HTML trong `docs/` là giao diện mẫu, không phải các trang ứng dụng đang phục vụ. Một số tài liệu Markdown cũ trong `docs/` còn mô tả giai đoạn khung sườn; dùng README này và [database/README.md](database/README.md) cho phiên bản hiện tại.

Khi nộp mã nguồn, có thể loại `target/`, `bin/`, metadata IDE và `.git/` khỏi bản ZIP; giữ source, `pom.xml`, script database/migration và các tài nguyên cần cho bản demo. Không xóa dữ liệu chạy chỉ vì chúng nằm ngoài `src/`.
