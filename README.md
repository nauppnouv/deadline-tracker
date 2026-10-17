# Deadline Tracker — W1

Quản lý công việc cho nhóm đồ án 4–5 người: giao việc, tiến độ, lọc hạn và lưu lịch sử. Bản web đã có mã nguồn/kiểm thử; kết quả chạy thật ở [EVIDENCE](docs/EVIDENCE.md). P7 đã nhận review và sửa năm phát hiện, chờ reviewer kiểm lại bản sửa. Chưa triển khai công khai, chưa pilot. Kiểm thử không phải chứng nhận bảo mật.

## Chạy cục bộ

Cần [JDK 25 LTS](https://adoptium.net/temurin/releases/?version=25), Maven 3.9+, Git Bash (Windows) hoặc Bash (Linux), Docker Desktop/Engine đang chạy Linux containers. Maven tải dependency từ Maven Central; Testcontainers tải MariaDB. Không cần tài khoản cloud/API key/Docker login. JDK 26 không phù hợp cổng LTS. Script nhận JDK portable ở `.tools/java25/<jdk>/`, nếu không dùng `JAVA_HOME`.

```bash
git clone https://github.com/nauppnouv/deadline-tracker.git
cd deadline-tracker
# Chuyển sang fix/p7-security-data-findings khi chủ repo đã push nhánh đó.
bash scripts/check.sh
```

Build tự chủ ban đầu ở `feat/w1-autonomous-build`, bản sửa P7 ở `fix/p7-security-data-findings`; remote chỉ có phiên bản này sau khi chủ repo push nhánh. Clone local nhánh này cũng dùng được. Check chạy test trên MariaDB tạm, đóng gói jar, kiểm tra template/SQL, quét bí mật/dependency và migration. Lần đầu NVD có thể mất hàng chục phút, cần mạng. Nếu mất mạng: `RUN_DEPENDENCY_AUDIT=0 AUDIT_SKIP_REASON='không có mạng' bash scripts/check.sh`; đây là **bỏ qua SEC-13**, không phải audit đạt. Cài gitleaks từ nguồn chính thức để có bộ quét đầy đủ; fallback chỉ dò mẫu khóa phổ biến.

PowerShell: gọi Bash bằng `& 'C:/Program Files/Git/bin/bash.exe' scripts/check.sh`. Docker pipe bị từ chối thì kiểm tra Docker Desktop/quyền terminal; không bỏ integration test để lấy build xanh.

Sau khi build:

1. Copy `.env.example` thành `.env`, thay hai mật khẩu DB bằng hai giá trị ngẫu nhiên khác nhau. Chỉ là mật khẩu DB cục bộ, không phải tài khoản ứng dụng. Không commit `.env`.
2. Chạy `docker compose up --build -d`.
3. Mở **http://127.0.0.1:8080/register**, tạo tài khoản thử (mật khẩu 15–128 ký tự), đăng nhập và tạo nhóm/công việc. Không có admin/mật khẩu mặc định.
4. Owner mở cài đặt nhóm → tạo liên kết → chuyển riêng cho đúng người. Mỗi liên kết dùng một lần, hạn 24 giờ; cần bốn liên kết cho bốn người.

Compose chỉ bind máy cá nhân: app 8080, DB 3307. DB trong named volume giữ qua restart. Dừng `docker compose stop`, chạy lại `docker compose start`. `docker compose down` giữ volume; **không thêm `-v` nếu cần giữ dữ liệu**. Không đổi `.env` khi volume đã tồn tại mà chưa cập nhật tài khoản DB.

Chạy Java trên host: `docker compose up -d db`, đặt biến môi trường `DB_PASSWORD` cùng giá trị DB, `DB_URL=jdbc:mariadb://127.0.0.1:3307/deadline`, `DB_USER=deadline`; chạy `java -Duser.timezone=UTC -jar target/deadline-tracker-0.1.0-SNAPSHOT.jar`. Profile mặc định dev bind loopback. Chỉ chạy một bản app cùng lúc.

Có Python3 thì chạy thêm `python scripts/smoke-compose.py` để thử jar bằng HTTP và restart trong Compose project riêng. Script dùng mật khẩu giả ngẫu nhiên, dừng/gỡ container/network thử, giữ volume thử và ghi tên ở target/compose-smoke.txt; không xóa volume khác. Test này tốn thời gian và không nằm trong check mặc định.

## Quy tắc

- Danh tính từ session. Người ngoài nhóm nhận 404; thành viên thiếu quyền nhận 403.
- Mọi thành viên tạo việc, chỉ giao thành viên hoạt động; việc mới TODO. Owner hoặc người tạo sửa thông tin; chỉ assignee đổi trạng thái.
- Owner **được giao lại cho chính mình**, reset TODO và có REASSIGNED event; sau đó mới đổi trạng thái với tư cách assignee.
- DONE chỉ sửa tên/mô tả. Muốn đổi hạn/giao lại phải mở lại trước. Giao cùng người không reset.
- Hạn tạo/đổi phải tương lai. Sửa/giao lại việc quá hạn giữ nguyên hạn ở mức phút được phép. Quá hạn là nhãn suy ra; đúng hạn chưa quá hạn, sắp đến hạn gồm đúng 24 giờ.
- Tổng quan luôn tính cả nhóm, không phụ thuộc lọc. Lưu UTC, hiển thị giờ Việt Nam.
- Form cũ bị từ chối 409 và giữ nội dung để sao chép/tải lại. Không xóa cứng nghiệp vụ.

## Vận hành và giới hạn

Argon2, CSRF, session JDBC 30 phút, cookie HttpOnly/SameSite; Secure/HSTS ở prod qua HTTPS. Đăng xuất, đổi/reset mật khẩu hủy mọi phiên tài khoản. Không tự phục vụ quên mật khẩu; xem [OPS](docs/OPS.md).

**Một instance**, rate limit bộ nhớ mất khi restart: login 5 lần/username và 30 lần/IP/15 phút; đăng ký 5 lần/username và 10 lần/IP/giờ. Không tin địa chỉ chuyển tiếp mặc định; cấu hình proxy theo [DEPLOY](docs/DEPLOY.md), đo lại limit sau proxy. Username 3–30 ký tự a-z0-9._-, chuẩn hóa chữ thường; mật khẩu giữ nguyên khoảng trắng.

Ops đổi định danh không xóa PII trong mô tả/tên người dùng đã nhập hay backup. Chưa chuyển Owner/rời nhóm/loại thành viên; khóa Owner khiến nhóm không quản lý lời mời được. Không có email, nhắc hạn tự động, bình luận, Kanban, file hoặc AI chia việc.

## Cấu trúc và tài liệu

- `domain`: Java thuần và Clock; `persistence`: JPA/Flyway; `service`: quyền/giao dịch.
- `security`: session/filter/rate limit; `web`: controller; `ops`: lệnh không HTTP.
- `src/main/resources`: templates/static/profiles/migrations; `src/test`: domain/MariaDB/MVC/ops/restore/tải.
- `scripts`: check/Maven/backup/restore; `legacy-console`: nguyên bản của chủ dự án.
- [PROJECT_PLAN](docs/PROJECT_PLAN.md), [BACKLOG](docs/BACKLOG.md), [DECISIONS](docs/DECISIONS.md).
- [ARCHITECTURE](docs/ARCHITECTURE.md), [EXPLAIN](docs/EXPLAIN.md): kiến trúc và bốn tình huống cần hiểu.
- [DEPLOY](docs/DEPLOY.md), [OPS](docs/OPS.md): chi phí, HTTPS, tài khoản và phục hồi.
- [EVIDENCE](docs/EVIDENCE.md), [PROGRESS](docs/PROGRESS.md), [BLOCKERS](docs/BLOCKERS.md).
- [CODEX_BUILD_PROMPT](docs/CODEX_BUILD_PROMPT.md): **chat mới**, chạy Reviewer P7 trước deploy/pilot.

Đã chọn Java 25.0.4.1, Boot 4.1.1, Tomcat 11.0.27, MariaDB 11.8.9, Testcontainers 2.0.5; Jupiter theo BOM Boot (6, D-15). Parent BOM khóa dependency, không dùng version range/SNAPSHOT thư viện. Audit chỉ phản ánh dữ liệu lỗ hổng tại lần chạy.

Console cũ: `javac -encoding UTF-8 -d out legacy-console/Main.java legacy-console/TaskStatus.java legacy-console/Task.java`, rồi `java -cp out Main`.
