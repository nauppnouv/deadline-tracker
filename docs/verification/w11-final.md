# W1.1 — M0–M5 local candidate, 21/10/2026

AI viết: **chưa đọc**. Một agent thực hiện theo EXECUTE; đây là bằng chứng tác giả, không là review độc lập/owner acceptance/Gate A. M6 server và push trên thiết bị thật chưa chạy.

## Danh tính

| Mục | Giá trị |
|---|---|
| Worktree | `D:\Documents\coding\deadline-tracker-w11-20261021T015624+0700` |
| Branch | `feat/w11-collaboration-webpush` |
| Parent | `d2d1a97bd3a523db3fc9e41aa908319c996763a1` (min8 commit; parent lịch sử `5ebb026`) |
| Snapshot nguồn được chạy | `D:\Documents\coding\deadline-tracker-checkpoints\w11-final-20261021T035528+0700/SOURCE-SNAPSHOT.json` — SHA256 `846bd8305f044f21fbd16315699c4749af5ce2019418733e5da00f2a10e1a68f` |
| Checkout kiểm mới | `D:\Documents\coding\deadline-tracker-checkpoints\w11-final-20261021T035528+0700/clean-source` — clone d2d1a97 rồi copy allowlist raw snapshot, không copy secrets/launcher/target |
| New commit | Full HEAD/parent sau commit ở `D:\Documents\coding\deadline-tracker-checkpoints\w11-final-20261021T035528+0700/FINAL-CHECKPOINT.json`; code/scripts/pom raw snapshot được đối chiếu với bản đã kiểm, tài liệu cuối bổ sung sau kiểm |
| JAR mới | `D:\Documents\coding\deadline-tracker-w11-20261021T015624+0700\target\w11-candidate-20261021T035528+0700\candidate-w11.jar` |
| JAR SHA256 | `db03fc0b30dbe6e73d8db06e6eec4a108814400ee9dc83e5fe08fea1f384828d` |

Không chuyển kết quả release/CP-006/min8 cũ sang W1.1. Min8 được kế thừa từ parent, không copy lại overlay hoặc commit trên P3. Root `d992edc`, P3 `d2d1a97`, launcher public bytes và hai JAR cũ được kiểm trước/sau ở checkpoint; V1–V4 giữ nguyên bytes. Hai EOL markers min8 được giữ raw; Git blob hashes ghi riêng trong checkpoint cuối.

## Kiểm đã chạy

Lệnh full từ checkout mới, Java25 portable hiện có (không đổi Java hệ thống), Maven cache tải dùng lại, target/DB/container mới:

```powershell
$env:JAVA_HOME='D:\Documents\coding\deadline-tracker\.tools\java25\jdk-25.0.4.1+1'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:MAVEN_REPO_LOCAL='D:/Documents/coding/deadline-tracker-w11-20261021T015624+0700/.m2-local'
$env:MIGRATION_BASELINE_REF='d2d1a97bd3a523db3fc9e41aa908319c996763a1'
& 'C:/Program Files/Git/bin/bash.exe' scripts/check.sh
```

| Phạm vi | Kết quả / bằng chứng |
|---|---|
| Full regression/integration + package | 266 JUnit, 33 classes; 0 failures/errors/skips; exit0; `full-final-receipt.json`, `full-final.log`, `full-final-xml/`, `test-summary.json` |
| JAR identity | 156 class/resource entries khớp byte target đã kiểm, gồm116 main classes; `JAR-CLASS-RESOURCE-PROOF.json`. Spring Boot giữ META-INF/spring.factories tại JAR root; helper đã sửa mapping, không bỏ qua file |
| Template/SQL static; secrets; migrations | Các cổng check cuối OK; secret scan cuối trước commit có receipt riêng, migration guard so d2d1a97; không sửa V1–V4 |
| OWASP dependency audit | Network audit cuối OK, không opt-out/suppress mới; báo cáo/hash ở checkpoint. Phạm vi dependency Java, không phải OS/image scan |
| M1/M2 | TaskWebTest: quyền/cross-group,409 version,plain text/revisions/ẩn,pagination20,mention mới/self/dedupe; inbox ownership/read/open và atomic notification/outbox rollback |
| M3 | AttachmentLifecycleTest: interrupted stream/rollback orphan,quota hidden bytes/concurrent uploads,signature/OOXML/stream cap,reconcile corrupt/missing,file đúng comment/task và ẩn theo comment |
| M3 HTTP/DB/restore | ChunkedRequestTest chạy HTTP chunked multipart/JSON thật,limits/type/CSRF; migration/runtime grants/9 versions/exact contract; paired DB+blob generation restore,hash/extra-file refusal,grant revocation/inbox retention |
| M4 | PushDeliveryTest: commit-only intent,lease/retry/status class/epoch/account/preferences,ownership/CSRF/logout/switch/opaque deep-link,blocked DNS-at-socket/SSRF,encryption; ReminderSchedulerTest dùng Fixed Clock 24h/1h/opt-out/stale due/assignee/DONE |
| Browser app | Chromium isolated trên localhost18080 + MariaDB synthetic; login/task/comment/mention/edit/revision/upload/download/pin/unpin/inbox/read/open/preferences; 1440x1000 và360x800, không tràn ngang (scroll345/viewport360), không lỗi/warn console; ẩn comment loại cả file liên quan |
| Browser permission | Native API và provider được mock trong contexts riêng: default chỉ hỏi sau click1lần; granted0prompt; denied lặp0prompt; unsupported disabled; subscribe failure0registration; enabled/disable UI và fallback đạt. `browser-permissions.log`, `browser-preferences.log`, `browser-ui.log`, screenshots |
| Service worker | `node check-service-worker.cjs <service-worker.js>` exit0: malformed ID không notify, generic body/tag UUID, cùng-origin opaque click, không fetch handler. Chỉ event harness, không native popup |

Browser preview/context/container đã đóng và port18080 không còn LISTEN. Không thao tác profile/permission thật hoặc gửi tới endpoint thật.

Audit preflight cùng pom đã tải cập nhật NVD trong phiên này,62dependency/0findings (receipt trong author-checks, preflight report trong checkpoint). Cache được copy khi plugin đã đóng sang checkout cuối trước bước audit; `AUDIT-CACHE-PROVENANCE.json` giữ hashes. Bước audit cuối dùng chính cache cập nhật đó theo TTL mặc định, không tắt update hoặc hạ threshold. Browser receipt giữ đường dẫn script thực thi ban đầu và bản archive, nguồn và log hashes riêng.

## Các lượt lỗi và ranh giới bằng chứng

- Các full checks trước khi tiếp tục đã bị ngắt; một lượt Java26 không đạt BuildSmoke. Không tính PASS.
- Focused1: mapping API401 còn kỳ vọng302; focused2: fixture file quota vượt DB CHECK10MiB, upload inventory thiếu content-type, Bash CRLF và pagination regex đếm nhầm. Focused3 còn paired backup bị schema-required CRLF. Sửa fixture/assertion/LF, paired-final exit0; giữ receipts/log/XML cũ ở `author-checks/`. Một vài receipt cũ mô tả selection bằng tên thay argv đầy đủ; không dùng chúng thay full receipt cuối.
- Full check ở worktree từ03:38:39 đến03:47:20 exit1,264 tests/51 failures/0errors/0skips: migration/classes đã compile trước khi agent bổ sung event preferences/comment attachment, nhưng các backup probes đọc scripts/schema-required mới khi chạy. XML chỉ rõ missing các cột mới. Đây là lượt snapshot không nhất quán do tác giả chỉnh source trong lúc suite chạy; không gọi product RED hoặc bỏ qua51fail. Lượt cuối chạy snapshot cố định trong checkout riêng và đối chiếu code/scripts/pom raw bytes trước bàn giao.
- Lượt snapshot cố định đầu tiên có3fail ở D28SchemaDefinitionTest: hai query independent metadata còn liệt kê8bảng cũ, hai hard counts còn10CHECK/63cột. Mở rộng danh sách18bảng,15CHECK/152cột và giữ so sánh exact-set, không skip/bớt invariant. Log/XML/receipt gốc ở `author-checks/first-fixed-snapshot-full/`; sau sửa chạy lại toàn bộ check trong checkout snapshot mới, không gắn nhãn PASS cho lượt fail này.
- Browser dùng runtime của snapshot cố định đầu; lần sửa sau chỉ D28SchemaDefinitionTest và tài liệu. Main source/resources/scripts/pom được đối chiếu raw với snapshot cuối. Không gọi đây là OS/provider E2E trên JAR đã triển khai.
- Browser open đầu tiên ERR_CONNECTION_REFUSED do preview chưa ready; run-code inline đầu tiên lỗi parser rồi đổi sang file. Các lỗi setup này không phải ca hành vi đạt. Scripts/logs cuối lưu riêng ở checkpoint.
- MariaDB11.8.9 mới hơn mức Flyway12.4 đã verified(11.7) vẫn cảnh báo; không sửa toolchain để che. TRIGGER/runtime residual, F-05 và các quyết định owner từ hồ sơ nền không được tự ký bằng lượt kiểm này.

## Chưa kiểm / bước thuộc M6

- Trusted HTTPS/origin/provider/browser/Windows desktop popup thật, tab đóng, notification click sau logout và offline/device delivery. HTTP2xx chỉ ACCEPTED, không phải người dùng đã nhận.
- Scanner antivirus thật, scanner service policy, offsite backup/retention, ACL/egress/proxy/grants và power-loss durability trên server/volume thật; generation cần operator quiesce writers.
- Owner đọc/hiểu code và tài liệu, review độc lập đúng commit, residual/Gate A và pilot. Không push/merge/deploy/thay launcher trong M0–M5.

Runbook: [W11_SERVER_RUNBOOK](../W11_SERVER_RUNBOOK.md), [MIGRATION](../MIGRATION.md), [OPS](../OPS.md), [DEPLOY](../DEPLOY.md). Các kết quả này chỉ chứng minh các ca đã chạy trên snapshot và môi trường ghi ở receipt.
