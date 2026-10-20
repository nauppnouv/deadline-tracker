## BR-24 min8 — 21/10/2026, sau release5ebb026

- Source worktree trên `fix/p3-01-web-root-secret` thay validator chung và form đăng ký/đổi mật khẩu từ15 xuống8; max128/Unicode/không strip/Argon2/hủy phiên giữ nguyên. DB/migration secrets không thay, không sửa V1–V4.
- RED `PasswordPolicyTest`:13 tests/4 failures/0 errors/skips, exit1. GREEN đầu:16 tests/0 failures/1 error vì XPath parser XML gặp HTML5; giữ log, không gọi là đạt hoặc lỗi sản phẩm. GREEN sau sửa matcher: **16 tests/0 failures/errors/skips**, exit0; gồm13 boundary tests và3 ca AuthWebTest trên Testcontainers DB giả riêng, có HTTP đăng ký7→400/8→thành công, đổi7→400/8→thành công, form min8, giữ spaces và hủy phiên.
- Build `bash scripts/mvn.sh -o -DskipTests package` exit0; không tính bước package là test. JAR mới chỉ tạo riêng,66 main classes khớp byte output đã test và hai form khớp source. [Lệnh/exit/hash/giới hạn](verification/password-min8.md). Không suite đầy đủ/audit mới hoặc kiểm trên app đang chạy; các kết quả release/CP006 lịch sử vẫn giữ snapshot gốc.

## P3-02 — DATA-01/02/09, migration riêng và runtime giảm quyền (20/10/2026)

## Hồ sơ bản local — 21/10/2026

Định danh và cách vận hành hiện tại: [LOCAL_RELEASE](LOCAL_RELEASE.md). Full new commit/parent và kiểm release ZIP nằm trong `release-manifest.json` ngoài repo. Các kết quả có ngày dưới đây vẫn thuộc revision/snapshot ban đầu; đợt finalization **không chạy test hành vi ở commit mới** và không thực hiện review độc lập.

| Hồ sơ | Revision/snapshot thực tế | Kết quả và giới hạn |
|---|---|---|
| P3-02 ngày 20/10 | Nền d992edc + candidate chưa commit theo [biên bản](verification/p3-02-remediation.md) | Full 230 tests / 29 classes; 0 failure/error/skip; audit dependency bỏ qua (không SEC-13 mới). Không gọi là suite tại new release commit |
| Review độc lập ngày 20/10 | 182-file snapshot trước overlay ba docs IR-01; bản sao REVIEW.md trong CP-006/evidence | Không phát hiện lỗi ứng dụng mới trong phạm vi review; IR-01 là khuyến nghị runbook thấp. Không phải review overlay/commit mới, không ký Gate A |
| IR-01 ngày 21/10 | Ba docs MIGRATION/DEPLOY/AI_WRITTEN sau review; CP-003…005 | Kiểm tĩnh wording/links/raw hashes; cùng tác giả, không review độc lập hoặc kiểm grants thật |
| CP-006 ngày 21/10 | `D:/Documents/coding/deadline-tracker-checkpoints/20261021T002924+0700`, raw snapshot182 + identity21 sau IR-01, HEAD nền d992edc | Focused **8 JUnit** / 0 failure/error/skip, Maven offline exit0; legacy probe **29 checks**, có provenance lặp, exit0; **66/66** fresh main classes khớp candidate JAR. DB giả cách ly, không DB cũ thật; không cộng thành 37 JUnit |
| Finalization ngày 21/10 | Năm docs trên cùng code/config/test/script/build byte CP-006, sau đó commit local toàn candidate | Chỉ kiểm docs/identity/byte/package. Lệnh, exit và hash mới trong evidence/finalization của ZIP; không build/test/rehearsal/audit mới |

JAR giữ SHA256 `f69e177d406861ce6347dd824b5a01f85fa1a10bc7d0254ac67a3ba47254dc2a`. Raw working-tree hashes CP-006 và Git blob/source hashes được tách trong manifest (Git áp line-ending attributes khi commit/export). Bytecode không được build lại cho docs. Gói chọn raw report/receipt/log đã kiểm, kèm XML projection của CP-006; projection bỏ properties/stdout, **không phải raw Surefire XML**, raw path/hash giữ trong focused-xml-receipt.json. Một số đường dẫn trong hồ sơ lịch sử chỉ có tại máy gốc.

Chưa kiểm mới: dependency audit/SEC-13, grants/server/proxy/public HTTPS/firewall, backup lịch/offsite/RPO/RTO, pilot thật hoặc mọi tình huống mất điện. Runtime có TRIGGER DDL; residual/owner comprehension/story completion và Gate A chưa được chấp thuận. Không sửa các dòng UAT/SEC/DATA hoặc ngày/revision lịch sử để biểu diễn acceptance mới.

- RED trước sửa: `bash scripts/mvn.sh -o '-Dtest=ProductionPreflightTest#rejectsMigrationInRuntime+rejectsRuntimeMigrationCredentials+runtimeProfilesDisableFlyway' test`, exit1; 9 tests, 9 failures/0 errors/0 skips. GREEN `bash scripts/mvn.sh -o -Dtest=ProductionPreflightTest,ProductionSecurityTest,OpsWorkflowTest,SchemaStartupTest test`, exit0; 53 tests/4 classes, 0 failures/errors/skips.
- `bash scripts/mvn.sh -o -Dtest=MigrationRuntimeSeparationTest test`, exit0; 8 tests/1 class. DB/schema/principal giả mới trong container disposable; migrate process riêng, idempotent, runtime/ops hoạt động với quyền giảm, definer khóa login vẫn tạo Owner/guard. Thiếu migration/TRIGGER, checksum sai và no-op trigger bị chặn. Runtime không CREATE/ALTER/DROP TABLE hoặc ghi history; **vẫn CREATE/DROP TRIGGER**, không gọi là không DDL. Lỗi biên dịch test lần đầu giữ log riêng, không tính product RED.
- Full `RUN_DEPENDENCY_AUDIT=0 AUDIT_SKIP_REASON='Offline scope; no dependency changes; not current SEC-13 evidence' MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh -o`, exit0; **230 tests/29 classes, 0 failures/errors/skips**; build/static/gitleaks/migration đạt; audit bỏ qua vì offline, không SEC-13 mới. Lượt này gồm test thay đổi cuối về URL loopback và launcher portable.
- JAR đóng gói: `python -X utf8 <evidence>/packaged-smoke.py`, exit0; migrate V1–V4 và chạy lại exit0, missing env exit1 dự kiến; web prod principal runtime không root/migration env, definer khóa login, GET /login trả200/form đúng. Container/anonymous volume giả mới đã dọn theo tên/label riêng.
- Hồ sơ D:/Documents/coding/security-revalidation-20261018/local-runs/p3-02-fix-20261020T125454Z-9d9f8c; receipt docs/verification/p3-02-remediation.md, runbook docs/MIGRATION.md. Worktree chưa commit; không đổi grants/DB/JAR đang dùng, dependency/JDK/image/migrations hoặc source gốc. Chưa review độc lập, chấp thuận residual server hoặc Gate A.

## P3-01 — SEC-12/SEC-06/SEC-09, root secret khỏi web (20/10/2026)

- RED `bash scripts/mvn.sh -o '-Dtest=ProductionPreflightTest#startsWithoutRootSecret+neverReadsRootSecret+rejectsRootRuntimeUsername' test`: exit1, 5 tests/3 failures/2 errors/0 skips trước sửa sản phẩm. Lỗi do root requirement/canary/guard username; không nhầm lỗi môi trường là RED.
- GREEN targeted `bash scripts/mvn.sh -o -Dtest=ProductionPreflightTest,ProductionSecurityTest test`: exit0, 29 tests/0 failures/errors/skips. Không root secret trong fixture web prod; vẫn kiểm effective password, bind/proxy, header/cookie/lỗi/log.
- Full `RUN_DEPENDENCY_AUDIT=0 MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh -o`: exit0, 213 tests/28 classes, 0 failures/errors/skips; build/static/gitleaks/migration đạt, dependency audit bỏ qua vì offline (không SEC-13 mới). [Biên bản và giới hạn](verification/p3-01-remediation.md). Một agent, không kết luận an toàn, Gate A hoặc triển khai AWS.

## F-08 — DATA-08/UAT-14, cleanup sau công bố current (19/10/2026)

- RED `bash scripts/mvn.sh -q '-Dtest=BackupVerifiedRetentionTest#interruptedPointerPublicationKeepsCurrent' test` trên e628fbd trước sửa sản phẩm: exit1, 4 tests/3 failures/0 errors/skips. Ngắt trước rename qua; sau rename, post-rename error và current-read failure đều fail đúng assertion con trỏ trỏ generation đã mất. Diff/hash/probe lưu trước execution.
- GREEN `bash scripts/mvn.sh -q -Dtest=BackupVerifiedRetentionTest test`: exit0, 14 tests/0 failures/errors/skips. Bốn ca F-08 giữ current resolvable, all3 cũ và actual backup/restore retry không sửa pointer; 10 hồi quy F-07 cùng chạy. Exit143/42 vẫn được truyền ra, không success giả.
- Full source `RUN_DEPENDENCY_AUDIT=1 MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`: exit0, 210 tests/28 lớp, 0 failures/errors/skips; build/static/gitleaks/dependency/migration đều OK. Log SHA256 `afd06fe7fde4a3193c01ecbc670295ec0cd53ad760d1526a0c8870c5f20e1278`. V1–V4 raw bytes khớp baseline cả worktree/index. Exact-commit clone receipt ngoài repo cùng run là nguồn kết quả clone, không dùng số liệu pin cũ.
- [Biên bản/UTC/exit/hash/XML/ledger và full checks](verification/f08-remediation.txt). Một agent thực hiện remediation; không đổi bằng chứng tác giả thành review độc lập, risk acceptance hay Gate A qua.

## F-07 — DATA-08/UAT-14, bảo toàn bộ đã full restore (19/10/2026)

- Đã chạy RED trước sửa trên HEAD `6748797c6e1e2d5991ed53a397d9768b2d373bd7`: no-op trigger và CHECK(1=1) trong dump hợp lệ gây mất cả ba file bộ tốt khi KEEP=1; 2 failures, 0 errors/skips. Replay `red-provenance` lưu diff/byte/hash test chưa tracked trước chạy.
- Đã chạy GREEN `bash scripts/mvn.sh -q -Dtest=BackupVerifiedRetentionTest test`: 10 tests, 0 failures/errors/skips. Dump sai bị restore chặn đúng schema; ba file protected giữ hash và phục hồi lại được. Có ca lỗi copy/current, khóa chồng lấn, xoay/recheck protected, bootstrap deferred, pointer/receipt/bytes hỏng.
- Full `RUN_DEPENDENCY_AUDIT=1 MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`: exit0, 206 tests/28 lớp, 0 failures/errors/skips; build/static/gitleaks/dependency/migration worktree+index đều OK. Log SHA-256 `4047be32ab5dd175f7c3b9bbb12d0c348888e9499f3a6fe9c467c5994de0a929`. Receipt clone mới của commit sửa được lưu ngoài repo cùng run, đối chiếu actual exit/XML/hash trong biên bản; không dùng số liệu clone của commit cũ.
- [Biên bản lệnh/UTC/exit/hash/XML và giới hạn](verification/f07-remediation.txt). Đây là bằng chứng triển khai của tác giả sửa; cần review độc lập bản sửa. Không chuyển suite xanh thành risk acceptance hay đề nghị Gate A qua.

## F-06 — phạm vi quyền MariaDB TRIGGER (19/10/2026)

- Sửa OPS: quyền `TRIGGER` cho phép CREATE/DROP/SHOW CREATE TRIGGER, không phải "không DDL"; vẫn không cấp DML hay DDL tổng quát. Test scratch với user/schema tạm: CREATE/DROP TRIGGER thành công, CREATE TABLE bị từ chối. `mvn -q -Dtest=TriggerPrivilegeTest test`: 1/0. Full cuối trên working tree gồm đủ 5 mục: `MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`, 196 test/27 lớp, 0 failure/error/skip; mọi cổng OK. [Bằng chứng/hash và giới hạn](verification/f06-remediation.txt). Chưa có review độc lập; Gate A chưa ký.
## F-03 — làm rõ câu chữ SEC-05 về phản hồi chia sẻ (19/10/2026)

- Chủ dự án đồng ý giữ luồng chia sẻ. Hai tài liệu đặc tả nói rõ raw token có thể lặp trong phản hồi xem trước/form cho holder của link hợp lệ; join thành công vẫn one-time theo BR-16. `mvn -q -Dtest=GroupInvitationWebTest test`: 2 test, 0 failure/error/skip. Hash và giới hạn: [F-03 remediation](verification/f03-remediation.txt). Không đổi bằng chứng trạng thái SEC-05 lịch sử; chưa có review độc lập, Gate A chưa ký.
## F-02 — exact body trigger V4 (19/10/2026)

- RED D28: 47 test/4 failure, tái hiện startup và backup nhận trigger no-op; GREEN D28 + RestoreOwnerGuards: 54/0; Round2Variants: 12/0. Full `MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`: 195 test/26 lớp, 0 failure/error/skip; mọi cổng OK. Lệnh/hash và giới hạn: [F-02 remediation](verification/f02-remediation.txt). Chưa có review độc lập; Gate A chưa ký.
# Bằng chứng kiểm chứng W1

## F-04 — hợp đồng định nghĩa cột (19/10/2026)

- RED 43 test/9 failure xác nhận thay đổi type/length/default được startup, backup và restore nhận; GREEN 43 test PASS, completeness exact-set 63 cột. Full `MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`: 191 test/26 lớp, 0 failure/error/skip, mọi bước OK. Log hashes và giới hạn: [F-04 remediation](verification/f04-remediation.txt). Chưa có review độc lập; Gate A chưa ký.

## F-01 — hợp đồng charset/collation (19/10/2026)

- RED: D28SchemaDefinitionTest trước sửa cho thấy startup prod/test nhận `task.status` với collation `utf8mb4_general_ci`; completeness không có TEXTDEF. Sau sửa, 34 test PASS, gồm startup, backup, restore, schema chuẩn và kiểm tra đủ 8 bảng. Full `MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`: 182 test/26 lớp, 0 failure/error/skip; mọi bước OK. Chi tiết lệnh, thời gian, log hash và giới hạn: [F-01 remediation](verification/f01-remediation.txt). Chưa có review độc lập cho commit này; Gate A chưa ký.

## Sửa D-28 bằng D-33 sau review độc lập Phase A — 18–19/10/2026

- Trạng thái review do chủ dự án cung cấp: REV-01…08 đạt trong phạm vi review 18/10, nhưng Gate A chưa được ký vì D-28; chủ dự án chọn sửa. Các kết quả dưới đây là kiểm chứng cục bộ cho bản sửa mới, **chưa có review độc lập cho commit mới**, không mở Gate A.
- Baseline thật trước sửa: `MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`, exit 0, **148 test/25 lớp**, 0 failure/error/skip; mọi bước OK. Log `target/d28-baseline.log`. Lượt gọi bash mặc định đầu tiên bị `Bash/E_ACCESSDENIED` trước check; baseline hợp lệ dùng Git Bash.
- RED: `bash scripts/mvn.sh test -Dtest=D28SchemaDefinitionTest`, exit 1, **29 test/25 failure/0 error/skip** (`target/d28-red.log`, XML `target/d28-red.xml`). Startup nhận sai 16 biến thể; backup công bố/retention sai 4 ca; 3 restore nhận sai, ca generated đổi giá trị chỉ bị checksum chặn; completeness chưa có dòng định nghĩa. Bốn ca dương tính/đích có sẵn qua.
- GREEN cùng lệnh: exit 0, **29/29**, 0 failure/error/skip (`target/d28-green.log`). Hai profile từ chối CHECK noop/gần giống, cột sinh đổi/cùng giá trị, kiểu/lưu trữ khác và đối tượng mới chưa có hợp đồng. Tái tạo đúng biểu thức vẫn qua. Restore từ chối CHECK và cột sinh bằng tên đối tượng, không `Mismatch:`/`RESTORE_OK`; ca `+0` có checksum group_member giống nguồn. Backup lỗi giữ nguyên toàn bộ bộ tốt; nguồn/đích có sẵn không đổi. Chủ dự án xác nhận giữ DB thử lỗi đã import để điều tra.
- Mutation: tạm tắt riêng hai nhánh CHECKDEF/GENERATEDDEF và dòng bắt buộc tương ứng trong hợp đồng dùng chung; cùng 29 test đỏ **25 failure/0 error/skip**, exit 1 (`target/d28-mutation.log`). Khôi phục đúng byte trong finally; Git diff với hai bản lưu exit **0,0**, hash trước/sau giống nhau (`target/d28-mutation-restored.txt`, `target/d28-mutation-{before,after}.json`). Sau đó full check dưới đây chứng minh bản khôi phục xanh.
- Full check cuối: `MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`, exit **0**, **177 test/26 lớp**, 0 failure/error/skip; build/static/gitleaks/dependency audit/migration worktree+index OK, không opt-out. Bắt đầu 19/10 00:01:58, kết thúc 00:08:42 Asia/Saigon, **403,661 giây**. `target/d28-full.log`, `target/d28-full.receipt`, `target/d28-full-summary.json`; SHA256 log `db61da0b3e830f95ddb233a1d77ff7916ae5d481f766c842dbd11f110c09e61d`.
- Giữ nguyên RestoreOwnerGuardsTest **7**, MigrationGuardTest **13**, Round2MatrixTest **1 / 45 HTTP assertions** (đã chạy lại thật); không sửa/skip test cũ. Không đổi V1–V4, không V5, không đổi image/Flyway/JDK/dependency; không tác động hồ sơ/tài nguyên review ngoài repo, không push/merge/deploy.
- Chuẩn 10 CHECK / 1 cột sinh lấy từ DB mới migrate V1→V4, image digest đã pin, MariaDB `11.8.9-MariaDB-ubu2404`; ck_username sau V3. Test completeness so đúng hai tập độc lập; đối tượng mới chưa có hợp đồng bị chặn. Cùng hợp đồng chạy startup prod/test, trước backup và sau restore import. [D-33 và biên bản chi tiết](verification/d28-remediation.txt); mọi tệp mới/sửa ghi AI_WRITTEN là `chưa đọc`.
- Giới hạn còn lại: chuẩn hóa MariaDB theo phiên bản; kiểu/độ dài/default cột thường, hai index thường và body trigger ở startup chưa thuộc bản sửa. Body năm trigger vẫn so khi restore; probe Owner, reset phiên, checksum/FK, lock/retention giữ nguyên. Chưa thêm probe INSERT CHECK tùy chọn. Cần harness độc lập chạy lại CHECK và generated ở cả startup/restore trên commit mới, gồm ca checksum không đổi. Các dòng UAT/SEC/DATA lịch sử dưới đây không được đổi thành reviewer chấp nhận.

## Sửa REV-02 của review độc lập pha A — 18/10/2026

- RED hợp lệ: `bash scripts/mvn.sh test -Dtest=MigrationGuardTest`:13 test,3 failure,0 error/skip. Hai ca index staged/committed bị working tree baseline che và Git index diff lỗi đều bị checker cũ nhận sai là đạt. Ref fixture dùng origin/main ghim; lượt trước dùng branch di chuyển có một failure precondition nên không dùng làm kết luận cho ca đó.
- GREEN cùng lệnh:13 test,0 failure/error/skip. Guard so riêng worktree/index với cùng baseline; từng view báo FAIL khi M/D/R/T hoặc Git lỗi. Giữ default origin/main, ref lỗi fail closed, none tường minh in BỎ QUA, thêm migration mới được phép khi so origin/main ghim (không dùng branch fixture đang di chuyển). Audit opt-out chỉ trong fixture Git cô lập; không dùng để gọi full gate đạt.
- Full `MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`:148 test/25 lớp,0 failure/error/skip; build/static/gitleaks/audit/migration OK, không skip. [Biên bản](verification/rev02-remediation.txt); log/XML/exit/hash: `D:/Documents/coding/security-revalidation-20261018/remediation-runs/fix-20261018T073702Z-5083274d/`.
- REV-01 được sửa riêng ở `dac7663007d6bbdd86828a6e65515a9cdaba686d`; không sửa V1–V4, không V5, không đổi JDK/dependency. Hai bản sửa **chưa phải review độc lập cổng A đạt**. Clean-clone cuối và prompt review được bàn giao ở packet remediation; chưa triển khai/pilot.


## Sửa REV-01 của review độc lập pha A — 18/10/2026

Review `run-20261018T063619Z-531425e2` vẫn là hồ sơ độc lập chưa qua cổng A. Theo EXECUTE mới, đã sửa REV-01 ở code; REV-02 chưa sửa tại mốc này. Không coi sửa xong là reviewer chấp nhận.

- RED: `bash scripts/mvn.sh test -Dtest=RestoreOwnerGuardsTest`: 6 test, 3 failure, 0 error/skip; restore nhận no-op stable/insert/delete trong khi tuple/rows vẫn hợp lệ.
- Sau sửa: `bash scripts/mvn.sh test -Dtest=RestoreOwnerGuardsTest,Round2VariantsTest,BackupRestoreTest,RestoredSessionTest`: 21 test PASS, 0 failure/error/skip. Cả năm no-op trigger bị từ chối; normal restore giữ count/Owner, probe rollback, metadata sửa được; đổi/xóa Owner SQLSTATE45000.
- Full: `MIGRATION_BASELINE_REF=0ed33309821c1fdd55cbd551246b89f1cd78d324 bash scripts/check.sh`: 145 test/25 lớp, 0 failure/error/skip; build/static/gitleaks/audit/migration OK, không opt-out.
- Test bổ sung `RestoreOwnerGuardsTest#literalWhitespaceIsNotFormatting` RED 1/1 failure vì bỏ whitespace làm thay chuỗi SQL bị coi là formatting; đã dùng SHA body nguyên văn, không chuẩn hóa chuỗi. Một lượt sau sửa chưa xanh do allowlist CRLF bị so sai; đã ghim LF rồi chạy lại. Không tính lượt đó là GREEN. Logs/XML/exit/hash ngoài nguồn: `D:/Documents/coding/security-revalidation-20261018/remediation-runs/fix-20261018T073702Z-5083274d/`; [biên bản](verification/rev01-remediation.txt).
- V1–V4, dependency/JDK không đổi; không cần V5. Cổng A chờ sửa REV-02 và phiên reviewer độc lập; chưa triển khai/pilot. D-25, inventory startup D-28, Flyway MariaDB11.8/verified11.7 và giới hạn SCA còn nguyên.


Ngày khởi tạo: 17/10/2026. Chỉ cập nhật khi đã chạy thật. Log local trong target/; kết quả có thể tái hiện bằng lệnh và tên test.

## Chốt review vòng 2 — 18/10/2026

Đã thực hiện bảy mục theo thứ tự yêu cầu trên `fix/p7-security-data-findings`, không push/merge. Mỗi mục có RED thật → GREEN → `bash scripts/check.sh`, EVIDENCE/PROGRESS và commit riêng. R2-01 có thêm một commit sửa race retention phát hiện trong lúc kiểm cuối; ca hai backup đồng thời đã đỏ (còn 0 bộ tốt) rồi xanh (còn 1). Không sửa V1–V4, không cần V5, không thay phiên bản JDK/dependency.

Clone sạch commit `c3364fbf2d8504cf8d3a0c0df5c6a8c7b25ff9a5`: `MIGRATION_BASELINE_REF=0ed3330 bash scripts/check.sh` **138 test, 24 lớp, 0 failure/error/skip**, build/static/gitleaks/dependency/migration đều OK, không bỏ qua cổng nào. Chạy riêng baseline mặc định `origin/main` cũng OK. NVD cập nhật thật 5.049 bản ghi; OWASP12.1.8 quét 54 dependency/0 finding; KEV còn cache dưới 24h, OSS Index tắt theo pom. Git status clone sạch sau check; chỉ tái dùng cache tải/công cụ, DB và target mới.

| Mục yêu cầu | Commit | Bằng chứng RED/GREEN và full check tại mục đó |
|---|---|---|
| 1. R2-01 schema startup, backup/restore, quyền TRIGGER và GRANT | aa74117; c3364fb bổ sung retention đồng thời | [r2-01](verification/r2-01.txt), [retention](verification/r2-01-lock.txt); full99 rồi138 test |
| 2. R2-03 migration baseline bất biến | 9568982 | [r2-02](verification/r2-02.txt); full109 test |
| 3. R2-02 version từ HTML mọi form | 17b4d92 | [r2-03](verification/r2-03.txt); bỏ hidden nhóm/task-form/status đỏ 2/4/3 failure; full112 test |
| 4. Prod fail-closed và cấu hình triển khai | 8a2d065 | [r2-04](verification/r2-04.txt); 18 cấu hình sai bị chặn trước tạo bean; full134 test |
| 5. R2-04 wildcard Content-Type 400 | be02058 | [r2-05](verification/r2-05.txt); actual400, dữ liệu không đổi, không đổi mã ứng dụng; full136 test |
| 6. Reset từng tài khoản sau restore trước mở web | 763db53 | [r2-06](verification/r2-06.txt); thông báo trước RESTORE_OK, không đổi dữ liệu tài khoản; full136 test |
| 7. Round2MatrixTest thường trực | cdd2a47 | [r2-07](verification/r2-07.txt); giữ45 assertion HTTP, negative control tháo membership guard đỏ; full137 test |

[Biên bản cuối](verification/r2-final.txt) ghi lệnh, mã commit, hash log, blob V1–V4 và giới hạn; [kết quả máy đọc](verification/r2-clean-summary.json) ghi từng lớp test và nguồn dữ liệu audit. Raw log cục bộ: `target/r2-clean-check.log`. Hash SHA256: `4b9527a7e5433ce2988f6f8862c6586d8a02a8f3c0af409c7334f91c125acb89`.

**Chưa kiểm chứng:** HTTPS/Caddy/forwarded IP/firewall trên Internet; lịch/alert/offsite/mã hóa backup và RPO/RTO thật; xác minh/reset tài khoản thật sau restore; pilot; browser/Compose smoke không chạy lại và image/OS chưa quét. Cảnh báo Flyway11.8/verified11.7 còn nguyên. Runtime cần quyền metadata TRIGGER (cũng cho sửa trigger); dump giữ DEFINER cần quyền trên schema phục hồi. OPS ghi cả hai và khóa backup còn lại sau crash/SIGKILL. Không suy từ test xanh thành được phép mở pilot/web. Phần AI viết ghi `docs/AI_WRITTEN.md`, trạng thái `chưa đọc`.

## Lịch sử P7 vòng 1 — sửa năm phát hiện review (18/10/2026)

Các kết quả P0–P6 bên dưới là lịch sử kiểm tra 58 test cũ. Review đã tái hiện lỗ hổng ngoài phạm vi các test đó. Các dòng P7 ghi RED thật và kết quả sau sửa; cổng đầy đủ cuối P7 đã chạy 79 test PASS, chưa thay thế kiểm chứng vận hành thật.

| Phát hiện | Trạng thái | Bằng chứng đã chạy |
|---|---|---|
| P7-01 metadata backup/restore, DATA-08/UAT-14 | đạt | RED: `bash scripts/mvn.sh test -Dtest=BackupFailureTest` — 8 test, 2 failure, 0 error/skip (`target/p7-01-red.log`). Sau sửa: `bash scripts/mvn.sh test -Dtest=BackupFailureTest,BackupRestoreTest` — 9 test PASS (`target/p7-01-green.log`). `bash scripts/check.sh` — 66 test PASS, static/gitleaks/dependency/migration OK, không bỏ qua (`target/p7-01-check.log`). Lỗi import harness trước RED không tính là tái hiện nghiệp vụ. DATA-08/UAT-14 còn mở P7-02. |
| P7-02 phiên sau restore, SEC-06/UAT-01/13/14 | đạt | RED `RestoredSessionTest`: HTTP 200 thay 302 (`target/p7-02-red.log`). Full đầu phiên vẫn FAIL 67/1 (`target/p7-resume-check.log`); chẩn đoán 16 hàng nguồn/restored giống hệt, native checksum khác. Manifest mới dùng SHA-256 logic, giữ count/FK, test tamper cùng row count bị chặn. `bash scripts/check.sh`: 70 test PASS, static/gitleaks/dependency/migration OK, không skip (`target/p7-02-final-check.log`). OPS có cổng đối chiếu credential/quyền sau snapshot, chưa thực hiện với dữ liệu thật. |
| P7-03 form nhóm cũ, DATA-04 | đạt | `bash scripts/mvn.sh test -Dtest=GroupVersionWebTest` RED 2 test/2 failure (`target/p7-03-red.log`); form cũ ghi đè, thiếu version vẫn 302. Sau sửa `-Dtest=GroupVersionWebTest,GroupInvitationWebTest,TaskWebTest` GREEN 10 test (`target/p7-03-green.log`). Full `bash scripts/check.sh` 72 test PASS, static/gitleaks/dependency/migration OK, không skip (`target/p7-03-check.log`); trả 409 giữ bản nháp và yêu cầu tải lại. |
| P7-04 request chunked, SEC-14 | đạt | RED `bash scripts/mvn.sh test -Dtest=ChunkedRequestTest`: 3 test/2 failure, oversized và unsupported type vẫn 302 (`target/p7-04-red.log`). GREEN `-Dtest=ChunkedRequestTest,AuthWebTest,GroupVersionWebTest`: 13 test PASS; body 16383/16384 được nhận, 16385/17000 bị 413, sai type 415; login UTF-8/CSRF còn đúng (`target/p7-04-green.log`). Full `bash scripts/check.sh` 75 test PASS, static/gitleaks/dependency/migration OK, không skip (`target/p7-04-check.log`); proxy Internet chưa chạy. |
| P7-05 Owner membership, DATA-02/BR-14 | đạt | RED `bash scripts/mvn.sh test -Dtest=OwnerIntegrityTest`: 4 test/4 failure (`target/p7-05-red.log`). V4 mới, không sửa V1/V2/V3. GREEN `-Dtest=OwnerIntegrityTest,PersistenceTest,BackupRestoreTest`: 12 test PASS; raw INSERT có Owner cùng giao dịch; chặn hạ/xóa/dời/đổi Owner; V3 hợp lệ giữ dữ liệu, V3 lỗi từ chối; trigger còn hoạt động sau restore (`target/p7-05-green.log`). Full `bash scripts/check.sh`: 79 test PASS, 0 failure/error/skip; static/gitleaks/dependency/migration OK (`target/p7-05-check.log`). |

Clone sạch commit `83dbb34b5fce2276b0a2553f45d70bbb6eb01f50`: `bash scripts/check.sh` đã PASS 79 test, 0 failure/error/skip; static/gitleaks/dependency/migration OK; audit 54 dependency entries/0 findings, không bước nào bỏ qua. Chỉ tái dùng cache tải; DB và target mới. Lệnh, hash log, giới hạn và biên bản: [verification/p7-final.txt](verification/p7-final.txt). Các dòng cổng ngày 17/10 phía dưới giữ dấu mốc lịch sử; toàn bộ test tương ứng cũng được chạy lại trong cổng P7 này.

| Cổng | Trạng thái | Lệnh/test | Ngày | Kết quả |
|---|---|---|---|---|
| UAT-01 | đạt | bash scripts/check.sh; P4 | 17/10/2026 | AuthWebTest sessionLifecycle; simulated accounts, not pilot |
| UAT-02 | đạt | bash scripts/check.sh; P4 | 17/10/2026 | GroupInvitationWebTest creates group then four distinct accounts join: 5 members; simulated accounts, not pilot |
| UAT-03 | đạt | bash scripts/check.sh; P4 | 17/10/2026 | InvitationTest expires/revokes/used token and two concurrent service joins; simulated accounts, not pilot |
| UAT-04 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.createAndValidation: real web create, invalid fields preserve description, forged fields ignored; test data only |
| UAT-05 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.statusPermission: Owner/creator rejected, assigned user completes; test data only |
| UAT-06 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.editAndConflict and TaskDomainTest: reassign/reset/revoke; test data only |
| UAT-07 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.statusPermission + PersistenceTest: same status idempotent, completedAt, reopening; test data only |
| UAT-08 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskDomainTest boundary Clock and TaskWebTest filter; test data only |
| UAT-09 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | RestartPersistenceTest + TaskWebTest edits overdue task; test data only |
| UAT-10 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.everyEndpointIsolation: foreign owner and anonymous across GET/POST routes, including malformed params; test data only |
| UAT-11 | đạt | MIGRATION_BASELINE_REF=0ed3330 bash scripts/check.sh; xem verification/r2-final.txt | 18/10/2026 | Clone sạch vòng2 138 test PASS: version từ HTML nhóm/tạo-sửa việc/trạng thái, validation/conflict/reload; hidden input mutation đỏ; dữ liệu thử |
| UAT-12 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.filtersAndPastDue totals independent of filters; zero/50% domain test; test data only |
| UAT-13 | đạt | bash scripts/check.sh; AuthWebTest.sessionLifecycle + OpsWorkflowTest.reset + ProductionSecurityTest.unknownError | 17/10/2026 | Đổi/reset/logout hủy phiên; force-change; healthy log scan và captured exception message không lộ marker; dữ liệu thử cục bộ |
| UAT-14 | đạt | MIGRATION_BASELINE_REF=0ed3330 bash scripts/check.sh; xem verification/r2-final.txt | 18/10/2026 | Clone sạch vòng2 138 test PASS: principal thiếu TRIGGER và dump bỏ trigger bị chặn, giữ backup trước; schema inventory/probe Owner rollback sau restore; clock skew/concurrent retention; reset từng tài khoản bắt buộc trước mở web; dữ liệu thử |
| UAT-15 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | AuthWebTest CSRF missing/invalid; GroupInvitationWebTest join POST; test data only |
| UAT-16 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.xssAndHeaders; test data only |
| UAT-17 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | AuthWebTest.rateLimit + RateLimitTest IP/username and expiry; test data only |
| UAT-18 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.filtersAndPastDue reassignment preserves past deadline; test data only |
| SEC-01 | đạt | bash scripts/check.sh; AuthWebTest.mappingInventory | 17/10/2026 | Duyệt toàn bộ current MVC mappings, mặc định từ chối anonymous; dữ liệu thử cục bộ |
| SEC-02 | đạt | bash scripts/check.sh; AuthWebTest + GroupInvitationWebTest + TaskWebTest.createAndValidation | 17/10/2026 | Actor từ SecurityContext; forged actorId/createdBy/role/status không ảnh hưởng; dữ liệu thử cục bộ |
| SEC-03 | đạt | bash scripts/check.sh; AuthWebTest.accessMatrix + TaskWebTest.everyEndpointIsolation + GroupInvitationWebTest | 17/10/2026 | Hai nhóm, owner/member/foreign/anonymous; GET/POST và malformed input chặn trước binding; permissions theo hành động; dữ liệu thử cục bộ |
| SEC-04 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | AuthWebTest.csrfProtection rejects missing/invalid token; valid registration succeeds; phạm vi build P3 |
| SEC-05 | đạt | bash scripts/check.sh; AuthWebTest.registrationRules + ProductionSecurityTest.unknownError + healthy-log literal scan | 17/10/2026 | Argon2/Unicode/password bounds; hash-only account, credentials erased; no sensitive exception marker; xem giới hạn log scan bên dưới; dữ liệu thử cục bộ |
| SEC-06 | đạt | bash scripts/check.sh; AuthWebTest + OpsWorkflowTest + ProductionSecurityTest + RestoredSessionTest | 18/10/2026 | Cổng P7 79 test PASS: logout/change/reset, fixation/expiry/flags; cookie đã logout bị từ chối sau restore; đối chiếu credential sau snapshot vẫn là cổng vận hành trong OPS |
| SEC-07 | đạt | InvitationTest + GroupInvitationWebTest; bash scripts/check.sh | 17/10/2026 | 256-bit token, hash-only DB, exact expiry, atomic join, no-referrer, POST+CSRF, no raw token in session |
| SEC-08 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.xssAndHeaders; group/task/detail/forms escape script payload; no unsafe-inline; source no th:utext |
| SEC-09 | đạt | bash scripts/check.sh; ProductionSecurityTest.prodHeaders + TaskWebTest.xssAndHeaders | 17/10/2026 | CSP/frame/no-referrer/nosniff; HSTS và Secure cookie qua secure request mô phỏng, chưa TLS Internet; dữ liệu thử cục bộ |
| SEC-10 | đạt | bash scripts/check.sh; scripts/check.sh + rà soát Repository/JdbcTemplate/InvitationStore | 17/10/2026 | Static scan cấm SQL concatenation; query bind params; DB scripts chỉ ghép identifier đã whitelist, không phải web input; dữ liệu thử cục bộ |
| SEC-11 | đạt | bash scripts/check.sh; ProductionSecurityTest.debugClosed/unknownError + OpsWorkflowTest.noWebOps | 17/10/2026 | Không actuator/debug endpoint; 500 có reference, không stacktrace/input message trong response/log; ops không HTTP; dữ liệu thử cục bộ |
| SEC-12 | đạt | bash scripts/check.sh; scripts/check.sh gitleaks | 17/10/2026 | gitleaks dir scan no leaks; .env/cache/tool/backup excluded; không đưa bí mật thật vào repo; dữ liệu thử cục bộ |
| SEC-13 | đạt | bash scripts/check.sh; scripts/check.sh OWASP12.1.8 | 17/10/2026 | Tomcat vá11.0.27 sau finding11.0.24; 54 dependency entries, 0 findings trong report mới; không suppress; OSS Index tắt vì cần credentials; dữ liệu thử cục bộ |
| SEC-14 | đạt | MIGRATION_BASELINE_REF=0ed3330 bash scripts/check.sh; xem verification/r2-final.txt | 18/10/2026 | Clone sạch vòng2 138 test PASS: chunked biên 16383/16384/16385/17000; unsupported415, wildcard application/* và */* 400 không ghi; chưa proxy Internet/DoS |
| DATA-01 | đạt | MIGRATION_BASELINE_REF=0ed3330 bash scripts/check.sh; xem verification/r2-final.txt | 18/10/2026 | Clone sạch vòng2 138 test PASS: V1–V4 không đổi so 0ed3330; Git baseline gate bắt sửa đã commit/M/D/R, thiếu ref/Git lỗi fail, none tường minh in BỎ QUA; Hibernate validate |
| DATA-02 | đạt | MIGRATION_BASELINE_REF=0ed3330 bash scripts/check.sh; xem verification/r2-final.txt | 18/10/2026 | Clone sạch vòng2 138 test PASS: startup prod/test từ chối thiếu/sai trigger, FK/CHECK/unique; restore inventory và hành vi Owner rollback; SQL Owner/V3 upgrade tiếp tục xanh |
| DATA-03 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.groupRollback/eventAtomicity: forced rollback and event idempotency; chạy lại toàn bộ ở P6 |
| DATA-04 | đạt | MIGRATION_BASELINE_REF=0ed3330 bash scripts/check.sh; xem verification/r2-final.txt | 18/10/2026 | Clone sạch vòng2 138 test PASS: optimistic writer và version từ HTML mọi form, sau validation/conflict/tải lại; tháo hidden input từng form làm test đỏ |
| DATA-05 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.invitationClaimPrimitive: actual InvitationStore, one of two joins; chạy lại toàn bộ ở P6 |
| DATA-06 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | TaskDomainTest.deadlineBoundaries and MariaDB UTC persistence; chạy lại toàn bộ ở P6 |
| DATA-07 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | Source scan: no DeleteMapping/RequestMethod.DELETE/business delete calls in current production source; chạy lại toàn bộ ở P6 |
| DATA-08 | đạt | MIGRATION_BASELINE_REF=0ed3330 bash scripts/check.sh; xem verification/r2-final.txt | 18/10/2026 | Clone sạch vòng2 138 test PASS: principal có/thiếu TRIGGER, dump CREATE TRIGGER, inventory/probe Owner và rollback, fail closed trước công bố/RESTORE_OK, giữ bộ tốt với clock skew/concurrency; lịch/offsite/RPO/RTO thật chưa kiểm |
| DATA-09 | đạt | bash scripts/check.sh; OpsWorkflowTest 4 tests | 17/10/2026 | Nonweb reset/anonymize, cần console/xác nhận, giữ references, invalidation/force-change, disabled assignee chặn; không HTTP; dữ liệu thử cục bộ |
| DATA-10 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | RestartPersistenceTest: close context, reopen, read same committed overdue task; chạy lại toàn bộ ở P6 |

## P0 — bằng chứng đã chạy

- Lệnh: Git Bash `bash scripts/check.sh` (JDK portable được script chọn).
- Kết quả 17/10/2026: BUILD SUCCESS; BuildSmokeTest 1 test, 0 lỗi; template/SQL static checks OK; gitleaks scanned ~126150 bytes, no leaks found; migration check OK.
- Dependency audit: BỎ QUA, chưa có kết quả SEC-13; sẽ chạy trước cổng P6. Không suy ra bất kỳ UAT chức năng nào từ smoke test build.

## P1 US-01/02 — Group

- RED: bash scripts/mvn.sh test -Dtest=GroupDomainTest; compiler báo package domain chưa có (target/p1-group-red.log). Lần thử sandbox trước đó lỗi Git Bash NtCreateDirectoryObject, không được tính là RED nghiệp vụ; đã chạy lại với quyền phù hợp.
- GREEN: bash scripts/check.sh; 9 tests PASS (8 case Group + 1 build), 0 lỗi; gitleaks/static checks OK (target/p1-group-green.log).
- Đã đối chiếu: nhóm có owner/member, chuẩn hóa/giới hạn tên và mô tả, nhóm cùng tên khác ID, không thêm trùng, vai trò theo nhóm, chặn rename bởi member/outsider, snapshot thành viên không sửa từ ngoài.
- Chưa phải UAT web/DB: US-01/02 chỉ qua phần miền Java thuần; UAT-02 vẫn chưa chạy end-to-end.

## P1 US-03…06 — Task và deadline

- RED: bash scripts/mvn.sh test -Dtest=TaskDomainTest; compiler báo thiếu Task/TaskStatus/TaskFilter/Progress (target/p1-task-red.log).
- GREEN: bash scripts/check.sh; 19 tests PASS, 0 lỗi (target/p1-task-green.log), gitleaks/static checks OK.
- Đối chiếu US-03: tạo TODO, tên/mô tả và giới hạn, người tạo/người được giao trong nhóm, hạn null/bằng/quá khứ bị từ chối.
- US-04: lọc kết hợp, quá hạn strict >, sắp đến hạn gồm đúng 24h, DONE không tính quá hạn, tỷ lệ 0/50% đúng.
- US-05: chỉ assignee, chặn owner/creator/member/outsider khác, đủ chuyển trạng thái và gửi lại không thay đổi, completedAt đúng.
- US-06: quyền sửa, giữ trạng thái khi cùng assignee, giao lại reset/revoke, DONE hạn chế sửa, nạp/sửa/giao lại việc quá hạn, so hạn cùng phút giữ thời điểm đã lưu.
- Cổng P1 miền thuần đã qua; các UAT web/DB vẫn chưa coi là đạt chỉ từ domain tests.

## P2 US-07/08 — bằng chứng

- RED: target/p2-red.log báo thiếu persistence/WorkService.
- GREEN cuối: bash scripts/check.sh, target/p2-final.log: 27 tests, 0 failure/error/skip, BUILD SUCCESS; gitleaks/static checks OK.
- MariaDB thực tế 11.8.9, image digest sha256:f16d7a344413e519dff518f27bdb221388f84b0d274c97c4fb5f2cb0dc1b34fa. Testcontainers 2.0.5; JDK 25 portable.
- DATA-05 kiểm tra primitive store trong DB thật; lời mời token/expiry/ủy quyền web sẽ kiểm chứng P4.
- DATA-07 là scan source build hiện tại, không phải bằng chứng rằng giao diện W1 đã hoàn thành.
- Cổng backup/ops và toàn bộ UAT web còn chưa chạy; SEC-13 audit đang tải NVD, chưa có kết quả.

## P3 US-09…11 — tài khoản và quyền

- RED: target/p3-red.log thiếu AccountService/security.
- GREEN cuối target/p3-gates-final.log: 36 tests, 0 failures/errors/skips; check/gitleaks/static checks OK.
- Test bổ sung đã phát hiện và sửa cookie thiếu HttpOnly/SameSite trong mock servlet context; CookieSerializer khai báo rõ, prod Secure.
- Expiration test dùng LAST_ACCESS_TIME và EXPIRY_TIME đúng cơ chế Spring Session; kiểm tra cookie cũ không vào account được.
- Chưa có toàn bộ routes W1; SEC mapping/object matrix được chạy lại sau P5. SEC-13 còn chờ NVD scan.

## P4 US-12…14 — nhóm và lời mời

- RED service: target/p4-red.log thiếu InvitationService; RED view target/p4-web-red.log thiếu invitation template và phát hiện rollback-only do bắt exception membership trong transaction. Đã thay bằng truy vấn membership không ném exception.
- GREEN cuối target/p4-final.log: 42 tests PASS, không error/failure/skip; gitleaks/check OK.
- 4 service tests lời mời: hash/token, quyền, join đã-là-thành-viên không tiêu thụ, đã dùng từ chối, mốc 24h, thu hồi, hai người đồng thời.
- Web flow đủ 5 thành viên, giả owner/role không hiệu lực; token không trong SPRING_SESSION_ATTRIBUTES; redirect chỉ chấp nhận đường dẫn lời mời nội bộ hợp lệ.

## P5 US-15…18 — giao diện và quyền

- RED target/p5-red.log: task routes/forms chưa có. GREEN target/p5-green.log: 48 tests PASS, gitleaks/static checks OK.
- Tạo/sửa/detail/status, giữ form lỗi và stale input 409; filter/progress; XSS escaped; foreign/anonymous bị chặn trước binding, kể cả malformed status.
- Giao diện responsive CSS cục bộ, không inline JS/style; chưa tuyên bố NFR-05 đã quan sát bằng browser.
- Dependency audit thực tế đã chạy, báo Tomcat 11.0.24 High/Critical. Đây là RED SEC-13 cần sửa P6, không bị suppress hoặc coi đạt.

## P6 — cổng đã chạy trên máy phát triển

- `bash scripts/check.sh`, log local target/p6-verified.log: **58 tests, 0 failures/errors/skips**; static/gitleaks/audit/migration OK. Tóm tắt lưu [tests-local](verification/tests-local.txt).
- Ops RED target/p6-ops-red.log trước implementation; backup RED target/p6-backup-red.log thiếu scripts; logout-all RED target/p6-logout-red.log. Production error capture bổ sung sau implementation là kiểm chứng hồi quy, không ghi là RED giả.
- Khi mở rộng kiểm tra app sau restore, fixture GRANT thiếu MYSQL_PWD bị từ chối; sửa môi trường tiến trình con rồi chạy lại toàn bộ thành công. Không tính lần thất bại là đạt.
- Backup/restore thật [restore-example](verification/restore-example.txt), bao gồm app đọc DB vừa phục hồi và từ chối outsider. Backup ở /tmp/deadline-backups trong container, ngoài project. Không restore đè nguồn.
- Scan successful log so với 11 literal passphrase giả: không khớp; ProductionSecurityTest dùng captured output để kiểm tra exception chứa marker nhạy cảm không bị log. Không suy ra mọi tình huống log tương lai đều không có bí mật.
- OWASP report mới không findings; [summary](verification/dependency-audit.json) có thời điểm/hash raw report. NVD tải lần đầu khoảng35phút; Tomcat11.0.24 đã bị báo High/Critical, vá11.0.27 rồi audit lại. Không quét image/OS bằng SCA Java này.
- [Đo tải](verification/load-local.txt):100 requests,75view/25save,5threads,3groups×5users×200tasks; JVM ổn định, MockMvc+MariaDB trên Windows CPU12/Docker memory11,8GB. Không có mạng/TLS/browser timing; không SLA.
- Browser IAB thật: login fake account, group overview, create task bằng Enter, Tab tới description, change status bằng Enter, DONE filter còn1kết quả nhưng overview3task/33%; 360px document/client/scroll đều360, không input thiếu label trên dashboard; console không warn/error. [Desktop](verification/group-desktop.png), [360px](verification/group-360.png), [detail](verification/task-360.png). Không phải kiểm toán accessibility toàn diện.
- Compose thật [smoke](verification/compose-local.txt): packaged jar HTTP register/login/create group, restart còn group+session, UID10001; loopbackports. Chỉ dừng/gỡ container/network project thử, giữ volume trial được ghi trong report.
- Flyway vẫn cảnh báo DB11.8 mới hơn mức verified11.7; chức năng trên pinned11.8.9 đã test, không coi là chứng nhận vendor. Xem D23/BLOCKERS.
- Tại mốc P6 cục bộ này chưa chạy clean clone; kết quả chạy sau được ghi ở mục cuối. Chưa deploy HTTPS thật, chưa lịch backup thật, chưa pilot/P7.

### Giới hạn diễn giải SEC-05 (D25)

Ghi chú 19/10/2026: câu chữ cổng đã được làm rõ bởi D-34 theo phê duyệt của chủ dự án; bằng chứng test và dòng trạng thái SEC-05 trong bảng không đổi. Câu chữ mới chưa có review độc lập; không suy thành đạt nghĩa đen hay Gate A đã qua.

Kết quả đạt trong bảng là kiểm chứng password thô/invitation raw token không lưu DB, principal credential bị erase và không log bí mật ở các luồng đã thử. Spring Session JDBC vẫn lưu session ID/CSRF/attributes và trả session cookie cho browser; token lời mời phải xuất trong phản hồi phân phối cho Owner. **Chưa/không tuyên bố đạt nghĩa đen cấm mọi token/session ID trong mọi phản hồi và DB**, vì mâu thuẫn công nghệ/luồng được yêu cầu. Không tự triển khai session thay framework. Reviewer P7 cần xác nhận diễn giải này; không coi đây là gate bị âm thầm bỏ qua.

- Chốt US21: target/p6-backup-story.log check58tests/0failure/error/skip, SCA/gitleaks/migration OK. Restore artifact có UTC bắt đầu và elapsed milliseconds thực đo (không gồm app boot); không suy RTO môi trường thật.

- Compose chạy lại từ jar sau check US21: PASS project deadline-w1-7c56fed6, account/group persisted sau restart, session còn hiệu lực, UID10001. Volume của cả hai lượt trial được giữ theo BLOCKERS. Console legacy biên dịch/chạy lại bằng JDK25 thành công, hai task/status và lỗi tên trống đúng. Markdown links local đã kiểm tra, không missing.

## Chốt P6 — clone sạch

- Đã clone local commit8d06499 vào thư mục mới trong .tools, không copy target hoặc DB. JDK25/Maven cache/NVD cache được tái sử dụng bằng biến môi trường, không user credential.
- `bash scripts/check.sh` từ clone:58 tests,0 failure/error/skip; build/static/gitleaks/OWASP/migration đều OK, **không bước nào bị bỏ qua**. Git status clone sạch. [Biên bản](verification/clean-clone.txt), [đo tải clone](verification/load-clean-clone.txt), [restore clone](verification/restore-clean-clone.txt).
- Mã runtime ở8d06499 đã được kiểm chứng; commit biên bản cuối chỉ cập nhật docs, không sửa source/migration/dependency.
- P6 kết thúc trong phạm vi xây dựng và kiểm chứng cục bộ. P7 chưa chạy theo yêu cầu dừng; US20 còn deploy thật, US21 còn lịch/backup ngoài máy, US22 còn pilot. Giới hạn D23/Flyway và D25/SEC05 vẫn công khai, không suy ra chứng nhận sản phẩm an toàn.

## Vòng 2 — mục 01 (18/10/2026)

R2-01: kiểm schema lúc startup prod/test; chặn backup thiếu quyền/trigger trước retention; restore kiểm kê và probe Owner rồi rollback. GRANT sao lưu đã ghi OPS. UAT-02/14 DATA-02/08 đã chạy trên DB giả.

- RED: `target/r2-01-red.log`; GREEN: `target/r2-01-green.log`. Lệnh và hash log: [biên bản](verification/r2-01.txt).
- `bash scripts/check.sh`: 99/0/0/0 (tests/failures/errors/skipped), build/static/gitleaks/dependency/migration OK. Phạm vi tự động cục bộ; không suy thành đạt pilot.

## Vòng 2 — mục 02 (18/10/2026)

R2-03 DATA-01: migration so baseline origin/main hoặc MIGRATION_BASELINE_REF; M/D/R/T của file baseline bị chặn kể cả đã commit. Git/ref lỗi fail rõ; none tường minh in BỎ QUA. Repo Git tạm tái hiện đã chạy.

- RED: `target/r2-02-red.log`; GREEN: `target/r2-02-green.log`. Lệnh và hash log: [biên bản](verification/r2-02.txt).
- `bash scripts/check.sh`: 109/0/0/0 (tests/failures/errors/skipped), build/static/gitleaks/dependency/migration OK. Phạm vi tự động cục bộ; không suy thành đạt pilot.

## Vòng 2 — mục 03 (18/10/2026)

R2-02 DATA-04 UAT-04/07/11: version mọi form lấy từ HTML đúng action, gồm validation/conflict/reload; status lỗi dùng GET tải lại; chunked và load cũng đọc HTML. Ba mutation bỏ hidden input đều đỏ; không đổi mã ứng dụng.

- RED: `target/r2-03-red-1.log`; GREEN: `target/r2-03-green.log`. Lệnh và hash log: [biên bản](verification/r2-03.txt).
- `bash scripts/check.sh`: 112/0/0/0 (tests/failures/errors/skipped), build/static/gitleaks/dependency/migration OK. Phạm vi tự động cục bộ; không suy thành đạt pilot.

## Vòng 2 — mục 04 (18/10/2026)

Triển khai fail-closed SEC-06/09/12: preflight prod trước context/DB; từ chối secret mẫu/ngắn/trùng, thiếu hoặc non-loopback bind, forwarded headers khác native. Mặc định prod loopback; compose DEV ONLY, placeholder CHANGE_ME, một khối cấu hình proxy.

- RED: `target/r2-04-red.log`; GREEN: `target/r2-04-green.log`. Lệnh và hash log: [biên bản](verification/r2-04.txt).
- `bash scripts/check.sh`: 134/0/0/0 (tests/failures/errors/skipped), build/static/gitleaks/dependency/migration OK. Phạm vi tự động cục bộ; không suy thành đạt pilot.

## Vòng 2 — mục 05 (18/10/2026)

R2-04 SEC-14: HTTP thật application/* và */* trả 400, nhóm không đổi. Sửa kỳ vọng test và DEPLOY; không sửa RequestSizeFilter hay mã ứng dụng.

- RED: `target/r2-05-red.log`; GREEN: `target/r2-05-green.log`. Lệnh và hash log: [biên bản](verification/r2-05.txt).
- `bash scripts/check.sh`: 136/0/0/0 (tests/failures/errors/skipped), build/static/gitleaks/dependency/migration OK. Phạm vi tự động cục bộ; không suy thành đạt pilot.

## Vòng 2 — mục 06 (18/10/2026)

DATA-08 SEC-06: sau restore mọi credential không tin cậy; thông báo bắt buộc reset từng tài khoản trước RESTORE_OK; OPS áp dụng mọi tài khoản còn được phép, giữ disabled. Chỉ thông báo/tài liệu, không đổi hành vi dữ liệu tài khoản.

- RED: `target/r2-06-red.log`; GREEN: `target/r2-06-green.log`. Lệnh và hash log: [biên bản](verification/r2-06.txt).
- `bash scripts/check.sh`: 136/0/0/0 (tests/failures/errors/skipped), build/static/gitleaks/dependency/migration OK. Phạm vi tự động cục bộ; không suy thành đạt pilot.

## Vòng 2 — mục 07 (18/10/2026)

UAT-10 SEC-02/03: Round2MatrixTest thường trực giữ 45 assertion HTTP, hai nhóm/bốn actor, forged fields và đổi cặp group/task; version từ HTML. Negative control tháo membership guard đỏ 404->200; khôi phục guard rồi GREEN.

- RED: `target/r2-07-red.log`; GREEN: `target/r2-07-green.log`. Lệnh và hash log: [biên bản](verification/r2-07.txt).
- `bash scripts/check.sh`: 137/0/0/0 (tests/failures/errors/skipped), build/static/gitleaks/dependency/migration OK. Phạm vi tự động cục bộ; không suy thành đạt pilot.

## Vòng 2 — mục 01-lock (18/10/2026)

R2-01 DATA-08 UAT-14 bổ sung ca retention đồng thời: RED hai lượt exit 0 nhưng còn 0 backup. Khóa mkdir nguyên tử chặn lượt chồng lấn trước file mới; GREEN còn 1 bộ tốt và lượt kế tiếp thành công. OPS ghi stale-lock; DEPLOY khớp quyền introspection.

- RED: `target/r2-01-lock-red.log`; GREEN: `target/r2-01-lock-green.log`. Lệnh và hash log: [biên bản](verification/r2-01-lock.txt).
- `bash scripts/check.sh`: 138/0/0/0 (tests/failures/errors/skipped), build/static/gitleaks/dependency/migration OK. Phạm vi tự động cục bộ; không suy thành đạt pilot.
