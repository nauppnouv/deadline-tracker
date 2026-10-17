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
| P6 | Cổng kỹ thuật cục bộ đã qua58tests; chờ clone sạch trước chốt pha |
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

## Điểm tiếp tục

1. Ops/hardening đã commit e2af9bf. US21 check58tests ở target/p6-backup-story.log xanh, có thời điểm/thời lượng restore thật; đang commit scripts/test/OPS.
2. Đóng gói Docker/README/ARCHITECTURE/EXPLAIN/OPS/DEPLOY và evidence; chạy kiểm tra link/legacy.
3. Clone local nhánh vào .tools/verification-UUID, dùng JDK25 và cache Maven/NVD ngoài clone, chạy scripts/check.sh thật từ clone. Chỉ sau kết quả mới đánh dấu P6 hoàn tất.
4. Dừng P6. P7 cần chủ dự án mở chat mới chạy Reviewer; US20 chưa deploy thật, US21 chưa lịch backup thật, US22 chưa pilot. Không tự push/merge/tạo tài nguyên.

D20 username nội bộ44ký tự; D21 disabled assignee; D22 vá Tomcat; D23 Flyway warning11.8 newer than verified11.7 (giữ cảnh báo, reviewer cần đánh giá); D24 logout all và restore compositeFK. JDK25 .tools/java25; Maven .m2-local; NVD .tools/dependency-check. Docker/Git Bash cần quyền pipe/process ngoài sandbox đã chạy được; không credential thật.
