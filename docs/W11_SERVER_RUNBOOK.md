# W1.1 — candidate, migration, push và backup/rollback

Ngày 21/10/2026. AI viết: **chưa đọc**. Tài liệu chuẩn bị M6; chưa chạy trên server/DB/người dùng thật. Source/candidate receipt ở [w11-final](verification/w11-final.md). Không chuyển JAR này vào launcher8443 bằng runbook này.

## 1. Điều kiện trước khi mở server

Owner chọn host/domain/budget, operator, private storage/disk/quota, scanner policy, backup retention/offsite và quyết định residual TRIGGER. Cần review đúng candidate commit, HTTPS certificate được browser tin cậy, reverse proxy với app/DB loopback, effective grants và kiểm IP trust/cookies theo [DEPLOY](DEPLOY.md)/[MIGRATION](MIGRATION.md). Mô hình một instance; không chạy nhiều app writer cùng storage hoặc bật sender ở hai bản phục hồi.

Tệp runtime nằm ngoài repo/release: DB runtime credentials, VAPID private key, attachments, backups. Linux: thư mục private `0700`, blobs/key `0600`, chỉ service UID và operator có quyền; Windows cần ACL tương đương trên thư mục cha và toàn bộ dữ liệu. Không dùng tmpdir cho storage thật. Endpoint/key subscription là secret, kể cả DB dump; không bật HTTP wire logs, SQL parameter logs hoặc log request body.

## 2. Cấu hình có thể review

| Biến | Giá trị / policy |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod`; migration/ops chạy bằng tiến trình và credential riêng |
| `JAVA_HOME` | JDK25 LTS; không đổi Java hệ thống chỉ để chạy app |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Runtime principal được kiểm theo MIGRATION; truyền qua môi trường được bảo vệ |
| `SERVER_ADDRESS` | `127.0.0.1`, reverse proxy theo cấu hình DEPLOY |
| `ATTACHMENT_ROOT` | Absolute private persistent directory ngoài source/JAR/webroot |
| `PUSH_ENABLED` | Mặc định `false`; chỉ `true` khi VAPID/provider/browser scope M6 được giao |
| `PUSH_PUBLIC_KEY` | Base64url P-256 public key; có thể trả cho browser đã đăng nhập |
| `PUSH_PRIVATE_KEY_FILE` | Absolute private file ngoài workspace; chứa duy nhất base64url private scalar; không đưa key vào argv/log/ZIP |
| `PUSH_SUBJECT` | `mailto:` hoặc HTTPS contact hợp lệ của operator |
| `app.attachments.scanner-required` | `true` khi server policy yêu cầu scanner; không có scanner CLEAN thì upload bị từ chối |

Runtime không sinh VAPID key tự động. Operator dùng công cụ mật mã đã duyệt, lưu key bền ngoài source; kiểm public/private cùng cặp bằng preflight. Rotation làm subscription cũ không còn phù hợp: tắt sender, thu hồi device grants, buộc enable lại bằng click. VAPID private key backup riêng với quyền tương ứng, không nằm trong generation/release package.

Provider registry hiện chỉ HTTPS443: `fcm.googleapis.com`, `updates.push.services.mozilla.com`, một label `[a-z0-9-]+.notify.windows.com`. Provider khác fail closed. Transport kiểm DNS/IP khi tạo socket, không follow redirect, proxy hệ thống, HTTP retry hoặc tái dùng connection. Server firewall/egress bổ sung nên giới hạn outbound HTTPS phù hợp; không mở URL arbitrary. Kiểm provider thực trên browser/origin mục tiêu trước khi mở gửi.

## 3. Migration và quyền mới

1. Chốt full commit/JAR SHA256, dừng mọi web app, reminder/sender/reconciler/ops writer. Xác nhận DB/attachments thuộc đúng instance.
2. Backup W1 trước nâng schema bằng script phù hợp phiên bản. Khi đã có W1.1, dùng generation pair bên dưới. Không xóa bản cũ và không sửa V1–V4.
3. Chạy migration-only với migrator riêng theo MIGRATION, áp V5–V9. Đối chiếu 9 migrations thành công, installed_by và definer thực; giữ 5 trigger và exact schema verifier. Runtime không có migration credential.
4. Ngoài quyền nền, runtime cần INSERT/UPDATE `task_comment`, `notification`, `task_attachment`, `notification_preference`, `push_preference`, `push_device`, `push_delivery`; INSERT `task_comment_revision`; INSERT/DELETE `comment_mention`, `task_pin`. SELECT vẫn theo principal/scoped schema, không UPDATE/DELETE `task_event` hoặc write migration history. Kiểm effective grants trên DB thử trước; không copy GRANT với wildcard schema không escape.
5. Khởi động candidate với `PUSH_ENABLED=false`; kiểm startup/schema, quyền outsider/member/owner, upload/download/inbox và storage. Bật push riêng sau khi preflight key/provider/origin đạt và owner giao scope. Bật reminders chỉ do từng user opt-in.

Runtime TRIGGER vẫn cho sửa trigger và là residual mở. Test reduced privileges không tự phê duyệt triển khai.

## 4. Browser và dữ liệu thông báo

`/account` có lựa chọn assignment/comment/mention cho inbox mới và push retry; inbox cũ vẫn giữ. Có lựa chọn nhắc24h/1h riêng. Trang giải thích desktop tùy chọn; chỉ click **Bật thông báo** gọi native permission. Denied/unsupported/subscribe-failed vẫn có inbox. Có danh sách tối đa5 thiết bị và nút thu hồi; re-enable tạo epoch mới. Quota đăng ký phía server theo tài khoản và rate limit bộ nhớ một instance.

Logout/change/reset/anonymize thu hồi tất cả device grants và tăng account epoch. Request subscribe cũ không thể dùng epoch trước logout; đổi tài khoản không chuyển ownership endpoint sang user mới. Khi tắt một thiết bị chỉ owner được thao tác. Pending retry kiểm lại account/membership/preference/epoch/comment hidden/reminder due+version+assignee+DONE.

Payload desktop chỉ opaque notification UUID và nội dung chung; không task title/body/filename. Service worker không có fetch handler hoặc offline cache. Click mở route cùng origin `/notifications/<uuid>`; qua login giữ đúng route allowlist, GET không đánh dấu read. Chỉ POST+CSRF mở task/đánh read.

2xx từ provider chỉ được ghi `ACCEPTED`; không phải popup đã hiện. M6 phải ghi browser/version/Windows/origin, opt-in thật, foreground/tab đóng/denied, actual popup và deep-link của chính user. Không dùng fake transport hoặc localhost screenshot để điền ca OS popup.

## 5. Worker, giám sát và lỗi

Web dev/prod có scheduling; ops và test không tự chạy scheduler. Reminder quét cửa sổ60s ở mốc24h/1h, không flood catch-up khi downtime; có thể bỏ lỡ nhắc nếu app offline. Không hứa đúng giờ khi browser/OS/offline.

Outbox batch20, lease120s, timeout connect3s/socket5s, tối đa6 attempts, expiry24h. At-least-once có thể lặp khi crash sau HTTP trước DB commit; SW dùng notification UUID/tag giảm trùng. 401/403 vào CONFIG_ERROR để operator kiểm key/subject, không retry loop; 404/410 thu hồi thiết bị; 429 Retry-After clamp60..3600s; 5xx/network bounded backoff; 3xx FAILED và không follow Location. Inbox không mất khi push lỗi.

Monitor bằng query chỉ metadata/count, không endpoint/key: `push_delivery` theo state, min created_at/next_attempt_at, attempts và last_status; inbox write failure; disk/quota; `AttachmentReconciler` báo số failed/orphan/parts; backup age và restore receipts. CONFIG_ERROR không tự requeue hàng loạt sau đổi key: cần kiểm cause, grant/account/membership và quy trình retry được giao riêng.

## 6. Files, reconciliation và scanner

10MiB/file,12MiB/request,1file/request,20file/task,200MiB/group,5pin/task. Tệp hidden vẫn tính quota. File có thể gắn comment cùng task; ẩn comment chặn đọc/ghim và bỏ pin file liên quan, không xóa bytes. Server UUID path, streaming, magic/UTF8/OOXML bounds; validation không phải malware scan. `AttachmentScanner` là hook cho scanner do server owner triển khai. Default UNAVAILABLE không được gọi CLEAN. Nếu policy required thì missing/unavailable/rejected chặn upload.

Web tự reconciliation định kỳ: so STAGED/READY blob size/hash, promote STAGED hợp lệ hoặc FAILED, bỏ pin tới file failed và báo orphan/parts. Filesystem move không rollback với DB: crash/DB rollback có thể để blob mồ côi; không tự xóa. Dừng writers và kiểm thủ công bằng ops khi cần:

```bash
# Runtime env/credential và ATTACHMENT_ROOT đã được nạp riêng.
java -jar candidate-w11.jar --spring.profiles.active=ops --app.ops.execute=false --app.attachments.reconcile=true
```

Nếu còn FAILED/orphan/parts, giữ bytes cách ly, điều tra DB/ref/hash rồi chọn policy cleanup được duyệt. Không xóa metadata/bytes để làm backup xanh. Không có scanner thật, purge policy hoặc power-loss durability được chứng minh trong lượt local.

## 7. Backup/restore cùng generation

1. Dừng mọi writer, kể cả scheduler/ops. Operator xác nhận thực tế rồi đặt `BACKUP_WRITES_PAUSED=1`. Script không tự khóa app; đừng đặt cờ khi writer còn chạy.
2. Dùng `scripts/backup-generation.sh` với DB backup principal, `DB_NAME`, `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `BACKUP_DIR`, `ATTACHMENT_ROOT` theo OPS. Chỉ root ngoài source; không có symlink. Tất cả referenced rows phải READY và root chỉ có đúng blobs được ghép; orphan/parts/missing/hash mismatch chặn.
3. Giữ trọn generation: DB dump/checksum/table manifest, `attachments.tsv`, blob bytes và `generation.manifest`. `GENERATION_BACKUP_READY` chưa là restore verified/offsite/durable. Chép sang offsite bằng kênh/quyền riêng do operator chọn, kiểm hash sau copy; giữ VAPID private key ngoài generation.
4. Restore DB prefix `deadline_restore_...` mới và `RESTORE_ATTACHMENT_ROOT` chưa tồn tại, bằng `scripts/verify-generation-restore.sh`, `GENERATION_DIR` trỏ generation. Restore target không phải source. Script từ chối overwrite/missing/extra/hash mismatch; hậu kiểm inventory/FK/Owner/checksum và blob metadata/hash.
5. Sau so snapshot, script clear sessions, disable push devices/preferences, tăng account/device epoch và CANCEL pending/leased/config-error deliveries. Inbox giữ lại. Reset credential từng account theo OPS, giữ disabled accounts disabled; user phải login/enable device lại. Không dùng grant/subscription cũ chỉ vì backup có chúng.
6. `RESTORE_OK_PAIR` chỉ cho biết bộ DB/files đã đối chiếu trên target cách ly. Kiểm app exact JAR trên target, permission/download và policy account trước khi giao việc mở writers. Nếu lỗi, giữ target/generation để điều tra; không drop/overwrite để retry.

## 8. Rollback

Tắt sender/reminders và dừng mọi writer trước khi rollback. Giữ DB/files/generation và receipts lỗi. Không chạy JAR W1 cũ trên schema W1.1 khi chưa có bằng chứng tương thích; không downgrade/drop bảng hay rewrite Flyway history.

Rollback được giao riêng có thể dùng app/schema đã kiểm tương thích hoặc restore một generation DB+files vào DB/storage mới rồi kiểm và chuyển ingress có kiểm soát. Với lỗi nâng W1→W1.1, cần bộ backup trước migrate của W1 và app W1 cùng pin; không ghép DB W1.1 với file snapshot khác. Khôi phục không tự cho phép mở lại server hoặc giữ grants push cũ.
