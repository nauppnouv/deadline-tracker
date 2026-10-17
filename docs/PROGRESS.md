# Tiến độ và điểm tiếp tục

Cập nhật17/10/2026. Nhánh feat/w1-autonomous-build, commit local, chưa push bản build này.

| Pha | Kết quả chạy thật |
|---|---|
| P0 | Java25/Maven/check, giữ nguyên console trong legacy-console; commit52fb6fe |
| P1 | Domain group/task/time/filter19 tests, test-first; commits20c563d/e027da9 |
| P2 | MariaDB thật, migrations, atomic/event/conflict/restart27 tests; commit06ec09c |
| P3 | Auth/session/CSRF/rate limit/authorization36 tests; commitb7cda3c |
| P4 | Group/invitation atomic42 tests; commit9b82b70 |
| P5 | Task UI/filter/errors/XSS48 tests; commitc559e9e |
| P6 | Đã chốt build/check cục bộ và clone sạch58tests; dừng trước P7 |
| P7/M7 | Chưa chạy; reviewer chat mới/pilot người dùng do chủ dự án thực hiện |

## Bằng chứng mới nhất

- target/p6-verified.log: check58 tests,0 failure/error/skip; static/gitleaks/migration/OWASP audit đều OK. Evidence và artifacts ở docs/verification.
- Tomcat11.0.24 audit High/Critical →vá11.0.27 →audit54entries/0findings; không suppress. OSSIndex tắt vì cần credential, NVD đã cập nhật thật.
- Ops4tests: reset/anonymize nonweb, giữ references, hủy phiên, force-change; chặn giao mới cho disabled.
- BackupRestoreTest chạy dump/restore/table inventory/count/checksum/compoundFK/file600/retention/guards; khởi động app trên DB phục hồi, owner đọc task và outsider404.
- Unknown500 có reference, captured log không chứa message nhạy cảm; healthy-log scan11literal passphrase giả không match.
- Browser IAB thật desktop/360px: login/create/status/filter, Tab/Enter, không tràn ngang/console warn/error. Ảnh trong docs/verification; demo đã dừng bằng marker, tab đã đóng.
- Compose project deadline-w1-e9918b41 chạy jar thật register/login/create-group/restart/session/UID10001; container/network thử đã gỡ, giữ volume trial theo report.
- NFR04 p95=169ms/max241ms,100request,5threads,3x5x200, MockMvc+MariaDB, không mạng/TLS/SLA.
- Lỗi p6-full do hai build ghi target đã sửa bằng chạy tuần tự; lỗi fixture GRANT khi mở rộng restore đã sửa MYSQL_PWD. Không tính các lượt lỗi là đạt.

## Đã dừng ở P6 — việc phiên sau

- Code runtime commit8d06499, ops e2af9bf, backup b4a4867. Clone sạch .tools/verification-a340b3c25bcd403f925cae3f085f7c2e check58tests/0failure/error/skip và toàn bộ check OK, không skip; docs/verification/clean-clone.txt có hash log/thời điểm audit. Chỉ tái dùng cache tải, DB và target đều mới.
- Biên bản cuối chỉ đổi docs. Không push/merge nhánh feat/w1-autonomous-build; không tạo cloud/domain/email/tài nguyên tính phí, không credential thật.
- Chủ dự án mở **chat mới** dùng Reviewer trong docs/CODEX_BUILD_PROMPT.md. Reviewer xem EVIDENCE, DECISIONS, BLOCKERS, đặc biệt D23 (Flyway/MariaDB warning) và D25 (session ID/CSRF/cookie bắt buộc theo framework, không tuyên bố nghĩa đen cấm mọi token trong DB/phản hồi).
- Sau review: sửa findings và chạy lại cổng liên quan/fullcheck; chỉ sau đó chủ dự án quyết định publish/deploy/ngân sách, HTTPS/proxy/IP trust/image-OS scan, lịch backup ngoài máy và pilot có đồng ý. US20/21 là một phần vận hành, US22 chưa làm.
- Các testcontainer tạm/demo đã đóng; hai Compose trial volume được giữ theo BLOCKERS, không chứa dữ liệu thật. Không dọn volume khác/global prune.

D20 username nội bộ44ký tự; D21 disabled assignee; D22 vá Tomcat; D23 cảnh báo Flyway; D24 logout-all/compoundFK; D25 diễn giải credential không rò rỉ trong các luồng được phép. JDK25 .tools/java25; Maven .m2-local; NVD .tools/dependency-check. Docker/Git Bash đã có quyền chạy. Không tiếp tục P7 trong cùng chat này.
