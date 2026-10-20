# P3-01 — root secret khỏi web — 20/10/2026

Phạm vi: EXECUTE P3-01, worktree D:/Documents/coding/deadline-tracker-p3-01, nhánh fix/p3-01-web-root-secret, baseline d992edc4172afc8eff050312066a7b33af35b6b5. Chưa commit, không tuyên bố exact new commit đã review.

Hồ sơ mới: D:/Documents/coding/security-revalidation-20261018/local-runs/p3-01-fix-d992edc-20261020T123411Z-834c2e. Ledger ghi UTC, argv, cwd, actual exit, XML và SHA256; manifest không là chữ ký. Report P3-01 cũ đã đối chiếu 66/66 artifact, giữ nguyên chỉ đọc.

## Bằng chứng đã chạy

- RED: `bash scripts/mvn.sh -o '-Dtest=ProductionPreflightTest#startsWithoutRootSecret+neverReadsRootSecret+rejectsRootRuntimeUsername' test` exit1; 5 tests, 3 failures/2 errors/0 skips. Product file lúc chạy RED byte-identical với d992edc; red-before-product.diff chỉ có thay đổi test.
- GREEN targeted: `bash scripts/mvn.sh -o -Dtest=ProductionPreflightTest,ProductionSecurityTest test` exit0; 29 tests/2 classes, 0 failures/errors/skips. Prod context không cần root secret; canary cấm đọc root secret không bị kích hoạt; thiếu/short/placeholder app/effective password và bind/proxy sai vẫn chặn trước beans. Header/cookie prod vẫn đạt trong MockMvc/DB fixture.
- Full `RUN_DEPENDENCY_AUDIT=0 AUDIT_SKIP_REASON="Offline authorized scope; dependencies unchanged; current SEC-13 not evaluated" MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh -o`: exit0, 213 tests/28 classes, 0 failures/errors/skips; build/static/gitleaks/migration đạt, dependency audit bỏ qua vì offline (không SEC-13 mới). Log full.log, XML full-surefire/, SHA256 trong ledger ngoài repo.

## Giới hạn

Chỉ sửa preflight/config docs/test. Không tách datasource Flyway/runtime, thêm kiểm grants, thay grant trên DB local/server hoặc sửa schema. Testcontainers dùng DB fixture mới và các setup/cleanup đã có trong test; không trỏ vào DB local người dùng. Native runtime username guard không xác minh DB CURRENT_USER, tài khoản có quyền quá rộng hoặc override danh tính trong JDBC URL. Root provisioning vẫn do người vận hành bằng công cụ riêng; chưa tự động hóa hoặc chạy provisioning thật.

Dependency/JDK/image/migration giữ nguyên. Build/test offline dùng cache cũ; dependency audit tắt với lý do rõ, không ghi SEC-13 đạt mới. Repo gốc và JAR local đang chạy giữ nguyên; chưa cập nhật local launcher sang nhánh mới. P3-02/R2/Gate A/AWS vẫn chưa hoàn tất.
