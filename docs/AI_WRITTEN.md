# Phần AI viết — sửa phát hiện P7

## Local release finalization — 21/10/2026

EXECUTE cho phép đúng năm đường dẫn tài liệu dưới đây, một commit local chứa toàn bộ candidate có sẵn và gói ZIP chọn lọc ngoài hai checkout. Không viết nhật ký học, không nhận thay owner đã đọc/hiểu hoặc chấp thuận residual. Các mục lịch sử dưới đây giữ phạm vi/ngày gốc.

| Tệp | Phần AI viết trong lượt này | Trạng thái |
|---|---|---|
| README.md | Trạng thái local, link runbook, clone bundle/dựng mới và giới hạn bằng chứng | chưa đọc |
| docs/PROGRESS.md | Khối hiện tại đặt trước lịch sử, scope commit/package, story/Gate A còn mở | chưa đọc |
| docs/EVIDENCE.md | Bảng provenance full/review/IR-01/CP-006, 8 JUnit và 29 checks, giới hạn kiểm mới | chưa đọc |
| docs/AI_WRITTEN.md | Registry finalization, giữ các registry lịch sử | chưa đọc |
| docs/LOCAL_RELEASE.md | Runbook launcher trên máy hiện tại, dựng mới, backup/restore references và handoff | chưa đọc |

Các hướng dẫn/manifest/checkpoint/prompt do AI tạo ngoài repo trong gói bàn giao cũng **chưa đọc**; manifest chỉ ghi kết quả máy kiểm, không là chứng nhận bảo mật. Implementation/test đã tồn tại giữ registry P3-01/P3-02 bên dưới; không đổi trạng thái đọc.

## IR-01 — runbook DB mới/DB đã migrate — 21/10/2026

Phạm vi tài liệu riêng được chủ dự án đồng ý trong chat sau CP-002. Chỉ sửa ba tệp dưới; không đổi code, V1–V4, installed_by, grants hoặc JAR/DB đang dùng; không commit/triển khai. Kiểm tài liệu/diff không phải test hành vi, kiểm grants thật hoặc ký Gate A.

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| docs/MIGRATION.md | Hai nhánh DB, bảng CURRENT_USER/installed_by/DEFINER, điều kiện khóa principal không làm runtime; giữ definer/quyền trigger, nêu kiểm đích chưa chạy | chưa đọc |
| docs/DEPLOY.md | Bước provisioning dẫn chiếu hai nhánh; không tạo lại DB cũ, không khóa nhầm runtime, điều kiện kiểm trước mở writer | chưa đọc |
| docs/AI_WRITTEN.md | Ghi phạm vi IR-01 và giới hạn bằng chứng; không viết nhật ký học thay chủ dự án | chưa đọc |

## P3-02 — migration riêng, runtime giảm quyền — 20/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| src/main/java/vn/deadline/tracker/DeadlineTrackerApplication.java; src/main/java/vn/deadline/tracker/ops/MigrationCommand.java | Entry point migrate trước Spring, credential riêng, loopback-only, Flyway rồi verifier, không mở web | chưa đọc |
| src/main/java/vn/deadline/tracker/security/ProductionPreflight.java; src/main/resources/application-prod.properties; src/main/resources/application-ops.properties | Prod/ops tắt auto-migration và từ chối migration config/credential trong runtime | chưa đọc |
| src/main/java/vn/deadline/tracker/service/SchemaInvariantVerifier.java | Validate Flyway checksum không ghi schema/history; thêm ops, giữ exact schema/trigger | chưa đọc |
| src/test/java/vn/deadline/tracker/ProductionPreflightTest.java; src/test/java/vn/deadline/tracker/ProductionSecurityTest.java; src/test/java/vn/deadline/tracker/MigrationRuntimeSeparationTest.java | RED trước sửa, fixture migrate riêng; grant tạm, tiến trình riêng, runtime/ops, definer khóa login, TRIGGER và drift bị từ chối | chưa đọc |
| README.md; docs/MIGRATION.md; docs/DEPLOY.md; docs/OPS.md | Runbook secret/process riêng, quyền theo bảng, definer và residual TRIGGER | chưa đọc |
| docs/DECISIONS.md; docs/EVIDENCE.md; docs/PROGRESS.md; docs/verification/p3-02-remediation.md; docs/AI_WRITTEN.md | D-43, bằng chứng, giới hạn, trạng thái chưa triển khai/Gate A | chưa đọc |

Công cụ/bằng chứng ngoài repo tại D:/Documents/coding/security-revalidation-20261018/local-runs/p3-02-fix-20261020T125454Z-9d9f8c cũng chưa đọc. Không viết nhật ký học thay người học.

## P3-01 — web không cần root secret — 20/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| src/main/java/vn/deadline/tracker/security/ProductionPreflight.java | Bỏ đọc/so DB_ROOT_PASSWORD; giữ kiểm DB_PASSWORD/effective password và bind/proxy; chặn effective runtime username root, không thay kiểm quyền DB thực | chưa đọc |
| src/test/java/vn/deadline/tracker/ProductionPreflightTest.java | RED/GREEN không root secret, canary phát hiện đọc root, root username và hồi quy secret/config | chưa đọc |
| src/test/java/vn/deadline/tracker/ProductionSecurityTest.java; src/test/java/vn/deadline/tracker/SchemaStartupTest.java | Bỏ root secret khỏi fixture context prod; giữ kiểm header/cookie/schema | chưa đọc |
| .env.example; README.md; docs/DEPLOY.md | Tách root secret của bootstrap DB khỏi môi trường web; kiểm secret/principal ở provisioning riêng, nêu P3-02/R2 còn mở | chưa đọc |
| docs/DECISIONS.md; docs/EVIDENCE.md; docs/PROGRESS.md; docs/verification/p3-01-remediation.md; docs/AI_WRITTEN.md | Quyết định kỹ thuật đúng EXECUTE, lệnh/exit/giới hạn và hồ sơ kiểm ngoài repo; không ký Gate A | chưa đọc |

Công cụ thu log/XML/hash ngoài repo tại D:/Documents/coding/security-revalidation-20261018/local-runs/p3-01-fix-d992edc-20261020T123411Z-834c2e cũng chưa đọc. Không viết nhật ký học thay người học.

## F-04 — hợp đồng kiểu và thuộc tính cột — 19/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| scripts/schema-inventory.sql; scripts/schema-required.txt; scripts/schema.lib.sh; SchemaInvariantVerifier.java | COLDEF exact-set cho 63 cột: kiểu, độ dài/precision, nullable, default, EXTRA; kiểm startup, backup, restore | chưa đọc |
| D28SchemaDefinitionTest.java | Kiểm độc lập 63 cột; mutation độ dài/default tại startup prod/test, backup, restore | chưa đọc |
| docs/verification/f04-remediation.txt; docs/EVIDENCE.md; docs/PROGRESS.md; docs/DECISIONS.md; docs/AI_WRITTEN.md | Ghi RED/GREEN/full-check, hash log và giới hạn F-04 | chưa đọc |

## F-01 — hợp đồng charset/collation — 19/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| scripts/schema-inventory.sql; scripts/schema-required.txt; scripts/schema.lib.sh; SchemaInvariantVerifier.java | TEXTDEF exact-set charset/collation cho 8 bảng nghiệp vụ/session tại startup prod/test, backup và restore; không sửa migration | chưa đọc |
| D28SchemaDefinitionTest.java | Đối chiếu completeness độc lập; mutation collation sai ở startup prod/test, backup, restore | chưa đọc |
| docs/verification/f01-remediation.txt; docs/EVIDENCE.md; docs/PROGRESS.md; docs/DECISIONS.md; docs/AI_WRITTEN.md | Ghi RED/GREEN/full-check, hash log, giới hạn và quyết định F-01 | chưa đọc |

## D-34 — chỉ tài liệu, câu chữ SEC-05 đã được chủ dự án duyệt — 19/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| AGENTS.md | Thay đúng định nghĩa SEC-05 §6 bằng câu đã duyệt, giữ nguyên cấm ghi log | chưa đọc |
| docs/PROJECT_PLAN.md | Bổ sung cùng định nghĩa SEC-05 tại §10 vì trước phiên chưa có mục này | chưa đọc |
| docs/DECISIONS.md | Thêm D-34 cuối file: lý do, chênh lệch trước sửa, câu chữ mới và giới hạn chưa review độc lập; giữ nguyên D-25/D-28/D-33 | chưa đọc |
| docs/EVIDENCE.md | Chỉ thêm ghi chú ngắn ở giới hạn diễn giải D25; không đổi dòng SEC-05/trạng thái/bằng chứng test | chưa đọc |
| docs/PROGRESS.md | Khối tiếp tục D-34, kết quả kiểm tra thực và trạng thái Gate A chưa qua | chưa đọc |
| docs/AI_WRITTEN.md | Ghi phạm vi tài liệu phiên D-34; mọi tệp ở trạng thái chưa đọc | chưa đọc |

## Sửa D-28 theo D-33 sau review Phase A — 18–19/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| scripts/schema-inventory.sql; scripts/schema-required.txt; .gitattributes | Hợp đồng định nghĩa 10 CHECK / 1 cột sinh dẫn xuất từ schema V1→V4 trên image MariaDB đã pin, SHA-256 nguyên biểu thức metadata; giữ dòng inventory cũ; ghim LF biên bản mới | chưa đọc |
| scripts/schema.lib.sh; src/main/java/vn/deadline/tracker/service/SchemaInvariantVerifier.java | Hai nơi đọc cùng hợp đồng từ chối cả thiếu/sai và thừa CHECK/cột sinh; lỗi có tên bảng.đối_tượng; không ghi DB ở startup | chưa đọc |
| src/test/java/vn/deadline/tracker/D28SchemaDefinitionTest.java | 29 ca test mới: prod/test, CHECK noop/gần giống, cột sinh khác/cùng giá trị/kiểu/lưu trữ, đối tượng thừa, completeness, backup/restore, tái tạo đúng và đích có sẵn | chưa đọc |
| docs/DECISIONS.md; docs/OPS.md; docs/EVIDENCE.md; docs/PROGRESS.md; docs/verification/d28-remediation.txt; docs/AI_WRITTEN.md | Quyết định D-33, giới hạn/hướng dẫn chuẩn hóa MariaDB, bằng chứng RED/GREEN/mutation/full check và trạng thái chờ review độc lập | chưa đọc |

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
| scripts/verify-restore.sh; docs/OPS.md; BackupRestoreTest.java | Mục 6: thông báo/reset mọi tài khoản còn được phép, kiểm thông báo trước RESTORE_OK; không thay logic dữ liệu | chưa đọc |
| Round2MatrixTest.java | Mục 7: đưa test review vào src/test, giữ 45 assertion HTTP, bổ sung mã UAT/SEC và version từ HTML; negative control kiểm guard rồi khôi phục nguyên mã nghiệp vụ | chưa đọc |
| scripts/backup.sh; Round2VariantsTest.java (concurrentRetention); docs/OPS.md; docs/DEPLOY.md | Bổ sung R2-01: tái hiện xóa chéo retention thật, khóa mkdir nguyên tử và release; hướng dẫn stale lock, quyền runtime metadata nhất quán | chưa đọc |
| docs/DECISIONS.md, BLOCKERS.md, EVIDENCE.md, PROGRESS.md, verification/r2-final.txt và r2-clean-summary.json | Biên bản clone sạch/giới hạn thực tế/đối chiếu bảy mục và quyết định được giao; không thêm code sau commit đã kiểm | chưa đọc |

## Sửa REV-01 sau review độc lập pha A — 18/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| scripts/schema.lib.sh; scripts/schema-trigger-bodies.sha256; .gitattributes | Allowlist SHA-256 body năm trigger gốc V4, giữ nguyên body, kể cả khoảng trắng trong SQL literal, ghim LF; dùng khi restore, không đổi migration | chưa đọc |
| scripts/verify-restore.sh; RestoreOwnerGuardsTest.java | Từ chối no-op mọi trigger; probe rollback đổi/xóa Owner, ca tốt và row counts; RED 6/3 -> GREEN 21/full145 | chưa đọc |
| docs/OPS.md, EVIDENCE.md, PROGRESS.md, verification/rev01-remediation.txt, AI_WRITTEN.md | Phạm vi thay đổi, bằng chứng chạy và giới hạn; chưa phải review cổng A đạt | chưa đọc |

## Sửa REV-02 sau review độc lập pha A — 18/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| scripts/check-migrations.sh | So baseline với worktree và index độc lập; lỗi từng diff fail closed; không đổi none/default/new migration policy | chưa đọc |
| src/test/java/vn/deadline/tracker/MigrationGuardTest.java | Index staged/committed khác working tree; lỗi Git riêng index; ref fixture ghim. RED13/3 -> GREEN13, full148 | chưa đọc |
| docs/EVIDENCE.md, PROGRESS.md, AI_WRITTEN.md, verification/rev02-remediation.txt | Chứng cứ thực và handoff chờ reviewer; không tự mở cổng A/B | chưa đọc |

## F-02 — exact body trigger V4 — 19/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| scripts/schema-inventory.sql; scripts/schema-required.txt; scripts/schema.lib.sh; scripts/verify-restore.sh; SchemaInvariantVerifier.java; .gitattributes; scripts/schema-trigger-bodies.sha256 | Một hợp đồng TRIGGERDEF exact-set so tuple và SHA-256 ACTION_STATEMENT dùng tại startup, backup, restore; bỏ allowlist trùng | chưa đọc |
| D28SchemaDefinitionTest.java; RestoreOwnerGuardsTest.java; Round2VariantsTest.java | Mutation no-op ở startup/backup/restore; completeness body; cập nhật restore assertions sang fail sớm qua contract chung | chưa đọc |
| docs/OPS.md; docs/verification/f02-remediation.txt; docs/EVIDENCE.md; docs/PROGRESS.md; docs/DECISIONS.md; docs/AI_WRITTEN.md | Ghi cách vận hành, RED/GREEN/full, hash log và giới hạn F-02 | chưa đọc |
## F-03 — câu chữ SEC-05 và regression luồng chia sẻ — 19/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| AGENTS.md; docs/PROJECT_PLAN.md | Đồng bộ câu SEC-05 về raw token có thể xuất hiện lại trong preview/form của holder link; one-time áp dụng cho join thành công | chưa đọc |
| GroupInvitationWebTest.java | Kiểm tra GET preview lặp lại vẫn hoạt động trước POST join trong luồng hiện có | chưa đọc |
| docs/verification/f03-remediation.txt; docs/EVIDENCE.md; docs/PROGRESS.md; docs/DECISIONS.md; docs/AI_WRITTEN.md | Ghi kiểm thử và quyết định D-38; không tuyên bố cổng bảo mật đạt | chưa đọc |
## F-06 — phạm vi quyền TRIGGER MariaDB — 19/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| docs/OPS.md | Sửa mô tả: TRIGGER cho phép CREATE/DROP/SHOW CREATE TRIGGER; không cấp DML hoặc DDL tổng quát | chưa đọc |
| TriggerPrivilegeTest.java | User/schema tạm trên MariaDB Testcontainers; trigger DDL được phép, CREATE TABLE bị từ chối, fixture được dọn | chưa đọc |
| docs/verification/f06-remediation.txt; docs/EVIDENCE.md; docs/PROGRESS.md; docs/DECISIONS.md; docs/AI_WRITTEN.md | Lệnh, hash, kết quả và nguồn MariaDB chính thức | chưa đọc |

## F-07 — giữ riêng bộ full-restore-verified — 19/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| scripts/backup.sh; scripts/verify-restore.sh; scripts/verified-backup.lib.sh | Ứng viên tách bộ protected; khóa chung, copy/digest/receipt, đổi current nguyên tử trước khi bỏ generation cũ; hoãn retention khi bootstrap | chưa đọc |
| src/test/java/vn/deadline/tracker/BackupVerifiedRetentionTest.java | RED no-op/CHECK dump thực, giữ ba file và phục hồi lại protected; lỗi copy/current, đồng thời, bootstrap, receipt/bytes/pointer sai | chưa đọc |
| docs/OPS.md; docs/DECISIONS.md | Vận hành, D-40, ranh giới quyền/trust/durability; không chấp nhận rủi ro hoặc ký Gate A | chưa đọc |
| docs/verification/f07-remediation.txt; docs/EVIDENCE.md; docs/PROGRESS.md; docs/AI_WRITTEN.md | Biên bản RED/GREEN/full, đường dẫn log/XML/ledger và giới hạn review | chưa đọc |

## F-08 — interruption sau đổi current — 19/10/2026

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| scripts/verified-backup.lib.sh | Cleanup kiểm current trên đĩa; giữ generation đã công bố hoặc chưa xác định an toàn; không che exit lỗi/ngắt | chưa đọc |
| src/test/java/vn/deadline/tracker/BackupVerifiedRetentionTest.java | Bốn ca trước/sau rename, post-rename error, lỗi đọc current; kiểm all3/current/lock, backup và actual restore retry không can thiệp pointer | chưa đọc |
| docs/OPS.md; docs/DECISIONS.md | Hành vi interruption, generation dư, D-41 và giới hạn vận hành | chưa đọc |
| docs/verification/f08-remediation.txt; docs/EVIDENCE.md; docs/PROGRESS.md; docs/AI_WRITTEN.md | Bằng chứng RED/GREEN/full/clone, hash/ledger và giới hạn review của tác giả; một agent thực hiện | chưa đọc |
