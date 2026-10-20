# P3-02 — migration riêng khỏi runtime — 20/10/2026

Phạm vi EXECUTE P3-02 trên worktree D:/Documents/coding/deadline-tracker-p3-01, nhánh fix/p3-01-web-root-secret, baseline d992edc4172afc8eff050312066a7b33af35b6b5 cùng thay đổi P3-01 chưa commit. Không có exact commit mới đã review.

Hồ sơ ngoài repo: D:/Documents/coding/security-revalidation-20261018/local-runs/p3-02-fix-20261020T125454Z-9d9f8c. Ledger ghi argv/cwd/UTC/exit/XML/hash; SHA256 chỉ chứng minh byte nhất quán, không phải chữ ký. Đã kiểm 57 artifact và 12 tệp P3-01 đầu vào trước thay đổi. Hồ sơ cũ giữ nguyên.

## Thay đổi

- Lệnh migrate chạy Flyway và exact verifier bằng MIGRATION_DB_* riêng, không Spring/web/ops; không root/fallback credential runtime. URL loopback dạng số, không options; lỗi ngoài không in secret/JDBC exception.
- Prod/ops tắt auto-migration, từ chối bật lại hoặc nhận credential migration. Runtime validate lịch sử và exact schema/trigger; schema trống, checksum sai, thiếu TRIGGER/no-op trigger phải chặn, không repair/migrate.
- Fixture runtime SELECT/TRIGGER trên schema, INSERT/UPDATE bảng nghiệp vụ, INSERT event, DML session; không CREATE/ALTER/DROP TABLE, sửa history hoặc DELETE event. Runtime vẫn CREATE/DROP trigger được: residual TRIGGER chưa được giải quyết bởi việc tách migration.
- Definer migration giữ tồn tại/SELECT INSERT TRIGGER khi khóa login và thu hồi quyền nâng schema; đã thử Owner/task/event/session/ops. Không cấp/revoke quyền DB đang dùng, không provision tự động.

## Bằng chứng chạy mới

- RED trước sửa product: `bash scripts/mvn.sh -o '-Dtest=ProductionPreflightTest#rejectsMigrationInRuntime+rejectsRuntimeMigrationCredentials+runtimeProfilesDisableFlyway' test`, exit1; 9 tests, 9 failures/0 errors/0 skips.
- GREEN tập trung: `bash scripts/mvn.sh -o -Dtest=ProductionPreflightTest,ProductionSecurityTest,OpsWorkflowTest,SchemaStartupTest test`, exit0; 53 tests/4 classes, 0 failures/errors/skips.
- Integration DB giả: `bash scripts/mvn.sh -o -Dtest=MigrationRuntimeSeparationTest test`, exit0; 8 tests/1 class, 0 failures/errors/skips. Lần đầu lỗi biên dịch vì kiểu JdbcSession nội bộ không public; log giữ integration-attempt1-compile.log, không gọi lỗi harness này là product RED.
- Full sau thay đổi cuối về numeric-loopback và portable java launcher: `RUN_DEPENDENCY_AUDIT=0 AUDIT_SKIP_REASON='Offline scope; no dependency changes; not current SEC-13 evidence' MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh -o`, exit0; 230 tests/29 classes, 0 failures/errors/skips; build/static/gitleaks/migration đạt. Log full.log và XML full-surefire/. Dependency audit bỏ qua, không SEC-13 mới.
- `python -X utf8 <evidence>/packaged-smoke.py`, exit0: JAR đóng gói migrate V1–V4 bằng principal riêng, lặp lại exit0, installed_by đúng migration user; thiếu env migration exit1 như dự kiến. Web prod dùng runtime, không root/MIGRATION_DB_* env, definer khóa login; GET /login trên loopback port ngẫu nhiên trả200 và có form. Tắt đúng tiến trình smoke, dọn container/anonymous volume giả theo tên/label riêng, không đụng app/volume đang dùng. Log/argv/exit/hash ở packaged-smoke.json và packaged-*.log.

## Giới hạn

Đã đọc code/config/runbook và chạy fixture, không kiểm grants trên DB người dùng/server. Non-root principal quá quyền hoặc JDBC runtime override danh tính chưa có effective privilege audit tự động. Migration dùng ALL chỉ schema trong fixture, chưa chứng minh tập DDL tối thiểu. Model server/remote DB, HTTPS/proxy/cookie, lịch backup/bản sao ngoài máy/alert/RTO và scope residual server vẫn cần kiểm/owner quyết riêng.

Dependency audit bỏ qua do offline, không SEC-13 mới; dependency/JDK/image/migration giữ nguyên. Repo gốc vẫn phải đúng pin/sạch/177 tệp tracked byte không đổi ở kiểm cuối. Bản local đang chạy giữ JAR cũ; candidate chưa cài. Không commit/push/merge, không Gate A hoặc AWS. Các kết quả lịch sử P3-01/F-08 không chuyển thành acceptance cho thay đổi này.
