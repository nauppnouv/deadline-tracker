# AGENTS.md — Deadline Tracker (chế độ AUTONOMOUS BUILD)

Đổi tên file này thành `AGENTS.md` để Codex đọc (giữ bản mentor là `AGENTS.mentor.md` nếu muốn).

Agent là kỹ sư chính, tự xây W1 từ đầu đến cuối khi chủ dự án vắng mặt. Quy tắc cốt lõi: **không có bằng chứng chạy thật thì không gọi là xong**. Bảo mật và toàn vẹn dữ liệu quan trọng hơn tốc độ và tính năng phụ.

## 1. Nguồn sự thật

`PROJECT_PLAN.md` (BR, NFR, UAT, ma trận quyền) > `BACKLOG.md` (story, tiêu chí) > `LEARNING_ROADMAP.md` (chỉ lấy mốc và thứ tự phụ thuộc; **bỏ phần "cách học/tự làm"**, vì chế độ này agent viết code). Mục "đề xuất" của tài liệu được chốt bằng mục 4 dưới đây. Mâu thuẫn còn lại: chọn phương án **an toàn hơn / chặt hơn**, ghi vào `docs/DECISIONS.md`, rồi làm tiếp. Không dừng chờ hỏi.

## 2. Tự chủ — khi nào được dừng

Agent chỉ dừng và báo cho chủ dự án với các việc **CHỈ NGƯỜI LÀM ĐƯỢC**:
1. Tạo/sửa/xóa tài nguyên tính phí hoặc cần thông tin đăng nhập (AWS, tên miền, email, API trả phí). Agent chỉ soạn hướng dẫn, dự toán và bước dọn dẹp.
2. Mời người dùng thật, xin đồng ý, phỏng vấn, pilot hai tuần (M7).
3. Việc ngoài thư mục repo, cài gói hệ thống, đụng dữ liệu thật.
4. Lệnh phá hủy (`rm -rf` ngoài thư mục build, `git reset --hard`, `git push --force`, `DROP`/`TRUNCATE` DB không phải DB thử).

Việc khác bị kẹt (không có mạng, không có Docker, thiếu quyền): ghi vào `docs/BLOCKERS.md` (việc, lỗi nguyên văn, cách người thử khắc phục), **bỏ qua phần bị kẹt và làm tiếp phần còn lại**. Test không chạy được phải ghi `chưa chạy`, tuyệt đối không ghi `đạt`.

## 3. Công nghệ (kiểm tra tài liệu chính thức tại thời điểm làm, ghi phiên bản đã chọn và lý do vào DECISIONS.md)

- Java **LTS** mới nhất mà Spring Boot hiện hành hỗ trợ chính thức. Không dùng bản không-LTS. Maven. Spring Boot bản ổn định mới nhất tương thích.
- Spring MVC + Thymeleaf (render phía máy chủ), Spring Security, Spring Session JDBC (để hủy được mọi phiên của một người dùng), Spring Data JPA, Flyway, MariaDB (InnoDB, utf8mb4), JUnit 5, Testcontainers (MariaDB) nếu có Docker.
- Mật khẩu: `PasswordEncoder` của Spring Security (Argon2 hoặc bcrypt). Không tự viết mật mã, token đăng nhập hay quản lý phiên.
- Cấu hình bằng biến môi trường + profile `dev`/`test`/`prod`. `docker-compose.yml` (app + MariaDB) chỉ cho máy cục bộ. Giữ code console cũ của chủ dự án trong `legacy-console/`, không xóa.
- JVM và DB chạy UTC; hiển thị `Asia/Ho_Chi_Minh` (BR-12). Logic thời gian nhận `Clock`.

## 4. Quyết định mặc định (chép vào docs/DECISIONS.md ở P0, không cần hỏi)

| # | Quyết định |
|---|---|
| K-1 | Owner được giao lại việc cho chính mình (về `TODO`) rồi mới đổi trạng thái. Chấp nhận, nhưng **bắt buộc** ghi `TaskEvent` loại `REASSIGNED` và nêu trong README. |
| K-2 | "Đổi hạn" = giá trị mới khác giá trị đang lưu khi so ở mức phút. Chỉ khi đổi hạn mới áp BR-04. Giao lại việc quá hạn mà không đổi hạn thì được phép. |
| K-3 | Giữ lời mời bằng token một lần, hết hạn 24 giờ (BR-16/17). |
| K-4 | Quyền xóa dữ liệu: không có xóa cứng trong app. Lệnh vận hành `ops anonymize-user <username>` (chạy bằng profile `ops`, không phải endpoint web): đổi username thành `deleted-<id>`, tên hiển thị "Đã xóa", vô hiệu hóa mật khẩu, xóa mọi phiên; giữ nguyên việc/lịch sử để không vỡ khóa ngoại. |
| K-5 | Quên mật khẩu: lệnh `ops reset-password <username>` đặt mật khẩu tạm, bật `must_change_password`, hủy mọi phiên. Không có trang quên mật khẩu tự phục vụ. |
| K-6 | Nơi chạy: không tạo tài nguyên cloud. Giao `docs/DEPLOY.md` (AWS và một lựa chọn rẻ khác) gồm kiến trúc, dự toán, HTTPS, biến cấu hình, sao lưu, dọn dẹp, để người thực hiện. |
| K-7 | Thêm UAT-15 (CSRF), UAT-16 (escape XSS), UAT-17 (giới hạn đăng nhập/đăng ký), UAT-18 (giao lại việc quá hạn không đổi hạn) vào `docs/EVIDENCE.md`, bên cạnh UAT-01…14. |
| K-8 | Người ngoài nhóm hoặc không đăng nhập gọi tài nguyên của nhóm nhận **404** (không lộ sự tồn tại). Đã đăng nhập, là thành viên nhưng thiếu quyền thao tác nhận 403. |
| K-9 | Rate limit đăng nhập/đăng ký theo (IP, username) ở mức bộ nhớ/DB đơn giản, một instance. Ghi giới hạn này vào README. |

## 5. Pha làm việc (làm tuần tự; mỗi pha có cổng, qua cổng mới sang pha sau)

- **P0 — Nền.** Repo Maven, `docs/{DECISIONS,PROGRESS,BLOCKERS,EVIDENCE}.md`, `scripts/check.sh` (mục 8), cấu hình profile, CI cục bộ chạy được.
- **P1 — Miền nghiệp vụ thuần + test (US-01…06).** Quy tắc BR-01…BR-11, BR-14/15, BR-19/20 bằng Java thuần, test mốc biên bằng `Clock` cố định.
- **P2 — Lưu trữ (US-07, 08).** Flyway, JPA, ràng buộc DB, giao dịch, khóa lạc quan, test restart/rollback/xung đột. Cổng DATA-01…07, 10.
- **P3 — Tài khoản và quyền (US-09…11).** Đăng ký/đăng nhập/đăng xuất/đổi mật khẩu, session, rate limit, dịch vụ phân quyền tập trung. Cổng SEC-01…06, 14.
- **P4 — Nhóm và lời mời (US-12…14).** Cổng SEC-07 và DATA-05.
- **P5 — Giao diện (US-15…18).** Thymeleaf, PRG sau khi lưu, lỗi tại trường. Cổng SEC-08, 09.
- **P6 — Cứng hóa và vận hành.** Header, xử lý lỗi, log, sao lưu/khôi phục, `ops`, deploy docs, README, `docs/ARCHITECTURE.md`, `docs/EXPLAIN.md` (giải thích bốn tình huống ở PLAN §15 để chủ dự án đọc sau). Chạy toàn bộ UAT-01…18. Cổng SEC-10…13, DATA-08, 09.
- **P7 — Rà soát độc lập.** Dừng, báo chủ dự án chạy prompt "Reviewer" trong `CODEX_BUILD_PROMPT.md` ở phiên mới. Sau đó sửa phát hiện và chạy lại cổng. Pilot người dùng (M7) là việc của người.

Mỗi story: viết test từ tiêu chí chấp nhận trước, làm cho test qua, chạy `scripts/check.sh`, cập nhật `docs/EVIDENCE.md` và `docs/PROGRESS.md`, commit nhỏ (message nêu story/BR/UAT).

## 6. Cổng bảo mật (mỗi mục phải có test tự động hoặc lệnh kiểm chứng ghi trong EVIDENCE)

- **SEC-01** Mặc định từ chối: mọi endpoint trừ `/login`, `/register`, `/error`, tài nguyên tĩnh đều đòi đăng nhập. Test duyệt toàn bộ mapping.
- **SEC-02** Danh tính lấy từ `SecurityContext`. Không DTO/form nào có trường `actorId`, `createdBy`, `role`, `status` khởi tạo (BR-05, BR-18). Test gửi thêm các trường này, kết quả không đổi.
- **SEC-03** Truy cập chéo nhóm: mọi truy vấn theo cặp (groupId, tư cách thành viên) ở một dịch vụ phân quyền duy nhất. Test ma trận 2 nhóm × {Owner A, Member A, Owner B, chưa đăng nhập} × mọi endpoint, kể cả đổi ID trong URL/body.
- **SEC-04** CSRF bật cho mọi phương thức thay đổi dữ liệu; test thiếu/sai token bị từ chối.
- **SEC-05** Mật khẩu theo BR-24, không strip, chỉ lưu bản băm; mật khẩu/token/ID phiên không xuất hiện trong log, phản hồi hay DB (test quét log).
- **SEC-06** Đăng nhập: thông báo chung, rate limit, chống session fixation, cookie `HttpOnly` + `Secure` (prod) + `SameSite`, timeout, đăng xuất và đổi/reset mật khẩu hủy mọi phiên của người đó.
- **SEC-07** Lời mời: token ≥256 bit từ `SecureRandom`, DB chỉ lưu hash (SHA-256), dùng một lần nguyên tử, không ghi log, trang có `Referrer-Policy: no-referrer`, xác nhận tham gia bằng POST.
- **SEC-08** XSS: chỉ `th:text`; cấm `th:utext` (kiểm bằng grep trong `check.sh`); test hiển thị chuỗi `<script>…`; CSP không có `unsafe-inline`, không script/style inline.
- **SEC-09** Header: CSP, `X-Content-Type-Options`, `frame-ancestors 'none'`, `Referrer-Policy`, HSTS ở prod.
- **SEC-10** SQL chỉ tham số hóa; `check.sh` grep cấm nối chuỗi vào câu truy vấn.
- **SEC-11** Lỗi: không lộ stack trace; có mã tham chiếu; actuator và mọi console debug tắt ở prod.
- **SEC-12** Bí mật: chỉ qua biến môi trường; `.env*` trong `.gitignore`; chạy `gitleaks` (hoặc grep mẫu khóa/mật khẩu nếu không có) trước mỗi commit cuối pha.
- **SEC-13** Phụ thuộc: khóa phiên bản; chạy quét lỗ hổng (OWASP Dependency-Check hoặc tương đương nếu có mạng); không còn lỗ hổng High/Critical chưa xử lý trừ khi ghi lý do.
- **SEC-14** Kiểm tra đầu vào ở máy chủ theo BR-01/02/15/23/24, giới hạn kích thước request.

## 7. Cổng toàn vẹn dữ liệu

- **DATA-01** Mọi thay đổi schema qua migration Flyway có version; không sửa migration đã áp dụng; `ddl-auto=validate`.
- **DATA-02** Ràng buộc ở DB, không chỉ ở code: khóa ngoại `RESTRICT`, `UNIQUE` username chuẩn hóa, `UNIQUE (group_id, user_id)`, đúng một `OWNER` mỗi nhóm (cột sinh `owner_marker` + unique, hoặc cách tương đương có test), `NOT NULL`, `CHECK` độ dài/enum.
- **DATA-03** Giao dịch nguyên tử cho: tạo nhóm + Owner; dùng lời mời + thêm thành viên; sửa việc + `TaskEvent`. Test ép lỗi giữa chừng, DB không còn bản ghi dở dang.
- **DATA-04** Khóa lạc quan (`@Version`). Xung đột trả thông báo tải lại. Test hai phiên cùng sửa và test "giao lại rồi người cũ ghi".
- **DATA-05** Test đồng thời nhiều luồng dùng một lời mời: đúng một luồng thành công. Thứ tự: kiểm tra đã là thành viên (không tiêu thụ lời mời), rồi `UPDATE … WHERE used_at IS NULL AND revoked_at IS NULL AND expires_at > now`, rồi chèn thành viên, cùng một giao dịch; ràng buộc UNIQUE làm chốt chặn.
- **DATA-06** Thời gian lưu UTC; hiển thị giờ Việt Nam; mọi mốc biên (BR-10/11/16) có test.
- **DATA-07** Không có xóa cứng trong ứng dụng, không có endpoint DELETE nghiệp vụ (BR-22).
- **DATA-08** `scripts/backup.sh` (`mysqldump --single-transaction`, file quyền 600, checksum, giữ N bản gần nhất, **không** nằm trong repo) và `scripts/verify-restore.sh` (khôi phục vào DB riêng, so số dòng/checksum từng bảng, kiểm tra ràng buộc). Chạy thật và ghi vào UAT-14. Sao lưu trước mỗi lần migrate trên môi trường có dữ liệu thật (ghi trong DEPLOY.md).
- **DATA-09** Lệnh `ops` của K-4/K-5 có test, không dùng được qua web.
- **DATA-10** Test: lưu → khởi động lại context → đọc lại; nạp lại một việc nay đã quá hạn (UAT-09).

## 8. `scripts/check.sh` (chạy trước mỗi commit cuối story)

Chạy lần lượt và dừng ở lỗi đầu tiên: build + toàn bộ test; grep cấm `th:utext` và nối chuỗi SQL; quét bí mật; quét phụ thuộc (nếu có mạng, nếu không thì in `BỎ QUA: không có mạng` thay vì im lặng); kiểm tra không còn migration bị sửa. Kết quả in rõ từng bước `OK/FAIL/BỎ QUA`.

## 9. Trung thực và báo cáo

- `docs/EVIDENCE.md`: mỗi UAT/SEC/DATA một dòng (`chưa chạy / đạt / không đạt / bỏ qua`), lệnh, ngày, kết quả thật. Không điền trước, không suy diễn.
- `docs/PROGRESS.md`: đã xong, đang làm, bước kế tiếp, quyết định mới. Phiên có thể bị ngắt bất kỳ lúc nào: đầu mỗi phiên đọc PROGRESS.md rồi làm tiếp, cuối mỗi phiên cập nhật nó.
- Báo cáo cuối: điều đã **chứng minh bằng chạy thật**, điều **chưa kiểm chứng** và lý do, các BLOCKERS, việc chủ dự án phải làm (mục 2). Không tuyên bố "an toàn" hay "đạt pilot"; chỉ nêu cổng nào đã qua với bằng chứng nào.
- Không tạo worklog, số liệu pilot hay bằng chứng cho việc chưa làm.
- Yêu cầu ngoài phạm vi (PLAN §5.5) không làm; ghi vào BACKLOG phần "để sau W1".

## 10. Hoàn thành

`scripts/check.sh` xanh trên bản clone mới (agent tự thử: clone vào thư mục tạm, làm theo README); UAT-01…18 `đạt` có bằng chứng; SEC-01…14 và DATA-01…10 có bằng chứng hoặc ghi rõ `bỏ qua` + lý do; README đủ để người khác chạy; P7 đã chạy.
