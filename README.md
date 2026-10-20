# Deadline Tracker — W1

Quản lý công việc cho nhóm đồ án 4–5 người: giao việc, tiến độ, lọc hạn và lưu lịch sử. Bản local P3-01/P3-02 được chốt ngày **21/10/2026**, gồm triển khai candidate hiện có và tài liệu IR-01 trên nền `d992edc4172afc8eff050312066a7b33af35b6b5`. Full commit phát hành và kiểm gói nằm trong `release-manifest.json` của gói bàn giao ngoài repo; không dùng riêng commit nền để định danh candidate.

Đọc [LOCAL_RELEASE](docs/LOCAL_RELEASE.md) để dùng bộ launcher **đã có trên máy này**, xem phiên bản/JAR, sao lưu và giới hạn. Candidate dùng **https://127.0.0.1:8443**, tách khỏi bộ local cũ 8080. Launcher ghim **repo gốc** và phụ thuộc cấu hình/secret/DB/JDK của máy hiện tại; ZIP không phải bộ cài một lần bấm cho máy mới. Hướng dẫn dựng mới ở phần dưới và LOCAL_RELEASE.

Bằng chứng [EVIDENCE](docs/EVIDENCE.md) giữ ngày/snapshot gốc: CP-006 có **8 JUnit đạt và 29 probe checks** (có lặp), không phải 37 JUnit. Đợt chốt chỉ kiểm tài liệu, danh tính, byte và gói; không chạy lại ứng dụng/test tại commit mới. Chưa triển khai công khai, chưa pilot, chưa ký Gate A hoặc chấp thuận residual. Kiểm thử không phải chứng nhận bảo mật.

## Dựng mới cục bộ từ source

Cần [JDK 25 LTS](https://adoptium.net/temurin/releases/?version=25), Maven 3.9+, Git Bash (Windows) hoặc Bash (Linux), Docker Desktop/Engine đang chạy Linux containers. Maven tải dependency từ Maven Central; Testcontainers tải MariaDB. Không cần tài khoản cloud/API key/Docker login. JDK 26 không phù hợp cổng LTS. Script nhận JDK portable ở `.tools/java25/<jdk>/`, nếu không dùng `JAVA_HOME`.

```bash
# Từ thư mục đã giải nén gói release; chưa chạy trong đợt chốt này.
git clone -b fix/p3-01-web-root-secret repository.bundle deadline-tracker-new
cd deadline-tracker-new
# Đối chiếu full HEAD với release-manifest.json trước khi build.
MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh
```

Gói local chứa source của commit mới ở `source/` và lịch sử Git ở `repository.bundle`; nhánh phát hành là `fix/p3-01-web-root-secret`. Không giả định commit này đã có trên remote. Nên clone bundle để giữ refs/lịch sử cho migration guard; thư mục `source/` đơn thuần không có `.git` nên chưa đủ chạy cổng Git của check. Git export dùng byte blob đã chuẩn hóa line ending theo thuộc tính repo; manifest đối chiếu riêng byte working tree CP-006 và byte source đã commit. Các bước build/dựng mới dưới đây dành cho môi trường mới được chuẩn bị riêng, chưa được thực thi lại trong đợt chốt. Check chạy test trên MariaDB tạm, đóng gói jar, kiểm tra template/SQL, quét bí mật/dependency và migration. Lần đầu NVD có thể mất hàng chục phút, cần mạng. Nếu mất mạng: `RUN_DEPENDENCY_AUDIT=0 AUDIT_SKIP_REASON='không có mạng' bash scripts/check.sh`; đây là **bỏ qua SEC-13**, không phải audit đạt. Cài gitleaks từ nguồn chính thức để có bộ quét đầy đủ; fallback chỉ dò mẫu khóa phổ biến.

PowerShell: gọi Bash bằng `& 'C:/Program Files/Git/bin/bash.exe' scripts/check.sh`. Docker pipe bị từ chối thì kiểm tra Docker Desktop/quyền terminal; không bỏ integration test để lấy build xanh.

Cổng migration so `git diff --name-status` với `MIGRATION_BASELINE_REF` (mặc định `origin/main`), gồm cả sửa đã commit. File đã có ở baseline bị sửa/xóa/đổi tên thì FAIL; chỉ thêm migration mới. Thiếu Git/ref hoặc lỗi diff cũng FAIL, không tự tải ref hay coi là đạt. Có thể đặt ref release đã tin cậy để bảo vệ cả migration chưa có trên main, ví dụ `MIGRATION_BASELINE_REF=0ed3330 bash scripts/check.sh` cho vòng review này. `MIGRATION_BASELINE_REF=none` chỉ khi người chạy chủ động đặt, in **BỎ QUA**, không phải DATA-01 đạt. Test MigrationGuardTest dùng repo Git tạm, chỉ cô lập các cổng khác trong fixture; lượt check toàn repo vẫn chạy đủ.

Sau khi build:

1. Copy `.env.example` thành `.env`, thay hai mật khẩu DB bằng hai giá trị ngẫu nhiên khác nhau. Chỉ là mật khẩu DB cục bộ, không phải tài khoản ứng dụng. Không commit `.env`. `DB_ROOT_PASSWORD` chỉ dành cho bootstrap container DB; không truyền root secret vào JVM web, kể cả profile prod. Xem ranh giới provisioning trong [DEPLOY](docs/DEPLOY.md).
2. Chạy `docker compose up --build -d`.
3. Mở **http://127.0.0.1:8080/register**, tạo tài khoản thử (mật khẩu 15–128 ký tự), đăng nhập và tạo nhóm/công việc. Không có admin/mật khẩu mặc định.
4. Owner mở cài đặt nhóm → tạo liên kết → chuyển riêng cho đúng người. Mỗi liên kết dùng một lần, hạn 24 giờ; cần bốn liên kết cho bốn người.

Compose chỉ bind máy cá nhân: app 8080, DB 3307. DB trong named volume giữ qua restart. Dừng `docker compose stop`, chạy lại `docker compose start`. `docker compose down` giữ volume; **không thêm `-v` nếu cần giữ dữ liệu**. Không đổi `.env` khi volume đã tồn tại mà chưa cập nhật tài khoản DB.

Chạy Java trên host: `docker compose up -d db`, đặt biến môi trường `DB_PASSWORD` cùng giá trị DB, `DB_URL=jdbc:mariadb://127.0.0.1:3307/deadline`, `DB_USER=deadline`; chạy `java -Duser.timezone=UTC -jar target/deadline-tracker-0.1.0-SNAPSHOT.jar`. Profile mặc định dev bind loopback. Chỉ chạy một bản app cùng lúc.

Dev/Compose vẫn auto-migrate DB local. **Prod và ops không auto-migrate**: dùng hai principal và hai môi trường tiến trình theo [MIGRATION](docs/MIGRATION.md), chạy lệnh `migrate` riêng trước, rồi web/ops bằng credential runtime. Không đưa secret migration/root vào web. Quyền TRIGGER vẫn cho phép sửa trigger; kết quả DB giả chưa là chấp thuận triển khai.

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
- [LOCAL_RELEASE](docs/LOCAL_RELEASE.md): bản local đã chốt, launcher máy hiện tại, dựng mới và bằng chứng.
- [DEPLOY](docs/DEPLOY.md), [MIGRATION](docs/MIGRATION.md), [OPS](docs/OPS.md): chi phí, HTTPS, tài khoản, migration và phục hồi.
- [EVIDENCE](docs/EVIDENCE.md), [PROGRESS](docs/PROGRESS.md), [BLOCKERS](docs/BLOCKERS.md).
- [CODEX_BUILD_PROMPT](docs/CODEX_BUILD_PROMPT.md): **chat mới**, chạy Reviewer P7 trước deploy/pilot.

Đã chọn Java 25.0.4.1, Boot 4.1.1, Tomcat 11.0.27, MariaDB 11.8.9, Testcontainers 2.0.5; Jupiter theo BOM Boot (6, D-15). Parent BOM khóa dependency, không dùng version range/SNAPSHOT thư viện. Audit chỉ phản ánh dữ liệu lỗ hổng tại lần chạy.

Console cũ: `javac -encoding UTF-8 -d out legacy-console/Main.java legacy-console/TaskStatus.java legacy-console/Task.java`, rồi `java -cp out Main`.
