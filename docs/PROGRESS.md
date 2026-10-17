# Tiến độ và điểm tiếp tục

Cập nhật 18/10/2026. Đang sửa bảy mục review vòng 2 trên nhánh `fix/p7-security-data-findings`; chưa push/merge. Các mục P7 bên dưới là lịch sử vòng 1; trạng thái vòng 2 ghi cuối tệp.

## P7 — công việc hiện tại

- Chấp nhận cả năm phát hiện review; xử lý ba Chặn pilot trước hai Nên sửa.
- P7-01 hoàn tất phần sửa: RED thật 8 test/2 failure → GREEN 9 test; full `scripts/check.sh` 66 test PASS, static/gitleaks/dependency/migration OK, không bỏ qua. Logs `target/p7-01-{red,green,check}.log`.
- Đã tiếp tục theo yêu cầu mới của chủ dự án; gói handoff cũ là snapshot, không cập nhật bằng trạng thái phiên này.
- P7-02 hoàn tất phần sửa: RED cookie HTTP 200 → GREEN HTTP 302; native checksum lệch đã điều tra bằng dữ liệu hàng giống hệt. Manifest mới SHA-256 logic, giữ count/FK, tamper bị chặn. Full `scripts/check.sh` 70 test PASS, static/gitleaks/dependency/migration OK, không skip (`target/p7-02-final-check.log`). OPS giữ cổng credential/quyền sau snapshot cho người vận hành.
- P7-03 RED 2 failure → GREEN 10 test, form có version, 409 giữ bản nháp. Full check 72 test PASS, static/gitleaks/dependency/migration OK, không skip (`target/p7-03-check.log`).
- P7-04 RED 3 test/2 failure → GREEN 13 test (HTTP chunked thật, size biên, unsupported type, UTF-8/CSRF). Full check 75 test PASS, static/gitleaks/dependency/migration OK, không skip (`target/p7-04-check.log`). DEPLOY có giới hạn Caddy mẫu; proxy Internet chưa triển khai.
- P7-05 RED 4 failure → GREEN 12 test: V4 bảo vệ Owner, kiểm nâng cấp V3 và trigger sau restore. Full `bash scripts/check.sh` 79 test PASS, 0 failure/error/skip; static/gitleaks/dependency/migration OK (`target/p7-05-check.log`). Không sửa V1/V2/V3.
- Đã chốt P7-05 ở commit `83dbb34`. Clone sạch `.tools/p7-clean-83dbb34` chạy `bash scripts/check.sh`: 79 test PASS, 0 failure/error/skip; static/gitleaks/dependency/migration OK, không bỏ qua; git status clone sạch. Chỉ dùng lại cache tải, không tái dùng DB/target. Biên bản và hash log: `docs/verification/p7-final.txt`.
- Bước kế tiếp: chủ dự án mở phiên reviewer độc lập kiểm lại đúng năm bản sửa trên nhánh này. Không tự triển khai hoặc mở rộng tính năng. Nếu review có phát hiện mới, tái hiện bằng test trước rồi xử lý trong phạm vi được giao. Deploy/pilot thật và các cổng vận hành trong BLOCKERS do người thực hiện.
- Không thay phiên bản thư viện/JDK, không chạy tài nguyên trả phí hoặc dữ liệu thật. Các mục P0–P6 phía dưới là lịch sử trước review; chưa có pilot.

| Pha | Kết quả chạy thật |
|---|---|
| P0 | Java25/Maven/check, giữ nguyên console trong legacy-console; commit52fb6fe |
| P1 | Domain group/task/time/filter19 tests, test-first; commits20c563d/e027da9 |
| P2 | MariaDB thật, migrations, atomic/event/conflict/restart27 tests; commit06ec09c |
| P3 | Auth/session/CSRF/rate limit/authorization36 tests; commitb7cda3c |
| P4 | Group/invitation atomic42 tests; commit9b82b70 |
| P5 | Task UI/filter/errors/XSS48 tests; commitc559e9e |
| P6 | Đã chốt build/check cục bộ và clone sạch58tests; dừng trước P7 |
| P7/M7 | P7 đã nhận review, sửa năm phát hiện, full check và clone sạch 79 test PASS; bàn giao reviewer kiểm lại. M7 chưa chạy, cần người dùng thật đồng ý. |

## Lịch sử bằng chứng P6 (trước review P7)

- target/p6-verified.log: check58 tests,0 failure/error/skip; static/gitleaks/migration/OWASP audit đều OK. Evidence và artifacts ở docs/verification.
- Tomcat11.0.24 audit High/Critical →vá11.0.27 →audit54entries/0findings; không suppress. OSSIndex tắt vì cần credential, NVD đã cập nhật thật.
- Ops4tests: reset/anonymize nonweb, giữ references, hủy phiên, force-change; chặn giao mới cho disabled.
- BackupRestoreTest chạy dump/restore/table inventory/count/checksum/compoundFK/file600/retention/guards; khởi động app trên DB phục hồi, owner đọc task và outsider404.
- Unknown500 có reference, captured log không chứa message nhạy cảm; healthy-log scan11literal passphrase giả không match.
- Browser IAB thật desktop/360px: login/create/status/filter, Tab/Enter, không tràn ngang/console warn/error. Ảnh trong docs/verification; demo đã dừng bằng marker, tab đã đóng.
- Compose project deadline-w1-e9918b41 chạy jar thật register/login/create-group/restart/session/UID10001; container/network thử đã gỡ, giữ volume trial theo report.
- NFR04 p95=169ms/max241ms,100request,5threads,3x5x200, MockMvc+MariaDB, không mạng/TLS/SLA.
- Lỗi p6-full do hai build ghi target đã sửa bằng chạy tuần tự; lỗi fixture GRANT khi mở rộng restore đã sửa MYSQL_PWD. Không tính các lượt lỗi là đạt.

## Lịch sử điểm dừng P6 (đã tiếp tục ở P7 phía trên)

- Code runtime commit8d06499, ops e2af9bf, backup b4a4867. Clone sạch .tools/verification-a340b3c25bcd403f925cae3f085f7c2e check58tests/0failure/error/skip và toàn bộ check OK, không skip; docs/verification/clean-clone.txt có hash log/thời điểm audit. Chỉ tái dùng cache tải, DB và target đều mới.
- Biên bản cuối chỉ đổi docs. Không push/merge nhánh feat/w1-autonomous-build; không tạo cloud/domain/email/tài nguyên tính phí, không credential thật.
- Chủ dự án mở **chat mới** dùng Reviewer trong docs/CODEX_BUILD_PROMPT.md. Reviewer xem EVIDENCE, DECISIONS, BLOCKERS, đặc biệt D23 (Flyway/MariaDB warning) và D25 (session ID/CSRF/cookie bắt buộc theo framework, không tuyên bố nghĩa đen cấm mọi token trong DB/phản hồi).
- Sau review: sửa findings và chạy lại cổng liên quan/fullcheck; chỉ sau đó chủ dự án quyết định publish/deploy/ngân sách, HTTPS/proxy/IP trust/image-OS scan, lịch backup ngoài máy và pilot có đồng ý. US20/21 là một phần vận hành, US22 chưa làm.
- Các testcontainer tạm/demo đã đóng; hai Compose trial volume được giữ theo BLOCKERS, không chứa dữ liệu thật. Không dọn volume khác/global prune.

D20 username nội bộ44ký tự; D21 disabled assignee; D22 vá Tomcat; D23 cảnh báo Flyway; D24 logout-all/compoundFK; D25 diễn giải credential không rò rỉ trong các luồng được phép. JDK25 .tools/java25; Maven .m2-local; NVD .tools/dependency-check. Docker/Git Bash đã có quyền chạy. D-26/D-27 ghi quyết định checksum/restore và Owner trong P7.

## Sửa review vòng 2 — mục 01

- R2-01: kiểm schema lúc startup prod/test; chặn backup thiếu quyền/trigger trước retention; restore kiểm kê và probe Owner rồi rollback. GRANT sao lưu đã ghi OPS. UAT-02/14 DATA-02/08 đã chạy trên DB giả.
- RED → GREEN; `bash scripts/check.sh`: 99/0/0/0, mọi cổng OK. Bằng chứng: `docs/verification/r2-01.txt`.
- Tiếp tục đúng thứ tự yêu cầu; không push/merge, không thay V1–V4, không thay phiên bản. Deploy/pilot thật chưa chạy.

## Sửa review vòng 2 — mục 02

- R2-03 DATA-01: migration so baseline origin/main hoặc MIGRATION_BASELINE_REF; M/D/R/T của file baseline bị chặn kể cả đã commit. Git/ref lỗi fail rõ; none tường minh in BỎ QUA. Repo Git tạm tái hiện đã chạy.
- RED → GREEN; `bash scripts/check.sh`: 109/0/0/0, mọi cổng OK. Bằng chứng: `docs/verification/r2-02.txt`.
- Tiếp tục đúng thứ tự yêu cầu; không push/merge, không thay V1–V4, không thay phiên bản. Deploy/pilot thật chưa chạy.

## Sửa review vòng 2 — mục 03

- R2-02 DATA-04 UAT-04/07/11: version mọi form lấy từ HTML đúng action, gồm validation/conflict/reload; status lỗi dùng GET tải lại; chunked và load cũng đọc HTML. Ba mutation bỏ hidden input đều đỏ; không đổi mã ứng dụng.
- RED → GREEN; `bash scripts/check.sh`: 112/0/0/0, mọi cổng OK. Bằng chứng: `docs/verification/r2-03.txt`.
- Tiếp tục đúng thứ tự yêu cầu; không push/merge, không thay V1–V4, không thay phiên bản. Deploy/pilot thật chưa chạy.

## Sửa review vòng 2 — mục 04

- Triển khai fail-closed SEC-06/09/12: preflight prod trước context/DB; từ chối secret mẫu/ngắn/trùng, thiếu hoặc non-loopback bind, forwarded headers khác native. Mặc định prod loopback; compose DEV ONLY, placeholder CHANGE_ME, một khối cấu hình proxy.
- RED → GREEN; `bash scripts/check.sh`: 134/0/0/0, mọi cổng OK. Bằng chứng: `docs/verification/r2-04.txt`.
- Tiếp tục đúng thứ tự yêu cầu; không push/merge, không thay V1–V4, không thay phiên bản. Deploy/pilot thật chưa chạy.

## Sửa review vòng 2 — mục 05

- R2-04 SEC-14: HTTP thật application/* và */* trả 400, nhóm không đổi. Sửa kỳ vọng test và DEPLOY; không sửa RequestSizeFilter hay mã ứng dụng.
- RED → GREEN; `bash scripts/check.sh`: 136/0/0/0, mọi cổng OK. Bằng chứng: `docs/verification/r2-05.txt`.
- Tiếp tục đúng thứ tự yêu cầu; không push/merge, không thay V1–V4, không thay phiên bản. Deploy/pilot thật chưa chạy.

## Sửa review vòng 2 — mục 06

- DATA-08 SEC-06: sau restore mọi credential không tin cậy; thông báo bắt buộc reset từng tài khoản trước RESTORE_OK; OPS áp dụng mọi tài khoản còn được phép, giữ disabled. Chỉ thông báo/tài liệu, không đổi hành vi dữ liệu tài khoản.
- RED → GREEN; `bash scripts/check.sh`: 136/0/0/0, mọi cổng OK. Bằng chứng: `docs/verification/r2-06.txt`.
- Tiếp tục đúng thứ tự yêu cầu; không push/merge, không thay V1–V4, không thay phiên bản. Deploy/pilot thật chưa chạy.
