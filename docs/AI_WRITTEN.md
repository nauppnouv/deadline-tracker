# Phần AI viết — sửa phát hiện P7

Phạm vi do chủ dự án yêu cầu: sửa đúng năm phát hiện review, test FAIL trước sửa, PASS sau sửa và kiểm tra đầy đủ. Chưa tính các phần này là kỹ năng người học.

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| src/test/java/vn/deadline/tracker/BackupFailureTest.java | Test ép lỗi truy vấn backup/restore trên MariaDB tạm, kiểm tra mã thoát và giữ bản sao trước | chưa đọc |
| scripts/backup.sh, scripts/verify-restore.sh | Truy vấn metadata kiểm tra mã thoát; manifest chỉ công bố khi đủ; không retention khi backup lỗi | chưa đọc |
| src/test/java/vn/deadline/tracker/RestoredSessionTest.java | Backup trước logout, phục hồi DB riêng và gửi cookie cũ qua HTTP thật | chưa đọc |
| scripts/verify-restore.sh (P7-02), docs/OPS.md | Hủy phiên sau đối chiếu snapshot; quy trình cách ly và đối chiếu reset/vô hiệu hóa sau snapshot | chưa đọc |
| scripts/checksum.lib.sh; scripts/backup.sh (P7-02); BackupFailureTest.java; RestoredSessionTest.java | SHA-256 logic theo cột/hàng đã sắp; test metadata lỗi, dữ liệu bị đổi và fixture Owner/Member | chưa đọc |
| GroupVersionWebTest.java; Group.java; GroupAccess.java; WorkService.java; GroupController.java; templates/group-settings.html; TaskWebTest.java | P7-03 version từ lúc mở form, từ chối cũ 409, giữ bản nháp; test hai phiên và version sai | chưa đọc |
| ChunkedRequestTest.java; RequestSizeFilter.java; docs/DEPLOY.md | P7-04 test HTTP chunked thật; giới hạn byte trước binding, tái đọc form bằng converter Spring, giới hạn proxy mẫu | chưa đọc |
| V4__owner_membership_integrity.sql; WorkService.java (P7-05); OwnerIntegrityTest.java; BackupRestoreTest.java | Tạo/bảo vệ Owner bằng trigger InnoDB, kiểm dữ liệu trước migrate, test SQL/upgrade/restore; không thay migration cũ | chưa đọc |

## Review vòng 2 — phạm vi được chủ dự án giao trực tiếp (18/10/2026)

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| SchemaInvariantVerifier.java; scripts/schema-inventory.sql, schema-required.txt, schema.lib.sh; pom.xml; .gitattributes | Hợp đồng schema dùng chung, kiểm lúc khởi động prod/test và trước backup/sau restore, đóng gói tài nguyên LF | chưa đọc |
| scripts/backup.sh, verify-restore.sh; docs/OPS.md | Chặn thiếu trigger/quyền trước công bố/retention; luôn giữ bộ mới đã kiểm; probe Owner trong transaction rollback; ghi GRANT/definer | chưa đọc |
| SchemaStartupTest.java; Round2VariantsTest.java; DatabaseTest.java | Regression từ review và mở rộng mất trigger/constraint, trigger no-op, clock skew; quyền definer chỉ namespace DB thử trong container riêng | chưa đọc |
| scripts/check.sh, check-migrations.sh; MigrationGuardTest.java; README.md | R2-03 baseline/ref/error/explicit skip gate; test repo Git tạm qua check.sh thật với build/audit cô lập trong fixture | chưa đọc |
| HtmlForms.java; GroupVersionWebTest.java; TaskWebTest.java; ChunkedRequestTest.java; RepresentativeLoadTest.java | R2-02 đọc version từ HTML, kiểm validation/conflict/reload; mutation ba hidden input; số đo tải ghi rõ GET ngoài thời gian đo | chưa đọc |
| ProductionPreflight.java; META-INF/spring.factories; application-prod.properties; .env.example; docker-compose.yml; docs/DEPLOY.md | Mục 4: preflight môi trường prod trước context, bind loopback, ví dụ không hợp lệ, một cấu hình forwarded headers có quote đúng | chưa đọc |
| ProductionPreflightTest.java; ProductionSecurityTest.java; SchemaStartupTest.java | Mục 4: từng điều kiện bị từ chối, cấu hình tốt vẫn boot; fixture prod được nạp sớm trước preflight, giữ kiểm schema | chưa đọc |
| Round2VariantsTest.java (wildcardType); docs/DEPLOY.md | R2-04: test HTTP thật theo 400 và dữ liệu không đổi; sửa mô tả mã lỗi, không đổi mã xử lý | chưa đọc |
