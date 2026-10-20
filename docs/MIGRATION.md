# P3-02 — migration riêng, runtime không nâng schema

Phạm vi đã thử: DB giả trong MariaDB Testcontainers, đúng image digest/JDK/dependency đang ghim. Không có provisioner, lịch migration hoặc thay grant trên DB đang dùng. Hướng dẫn này chưa được chạy trên AWS. Dev/test giữ đường auto-migration cho fixture; prod/ops bắt buộc migration riêng.

## Hai tiến trình, hai bộ secret

1. Người vận hành xác định đúng DB và chọn nhánh DB mới/DB đã migrate bên dưới trước khi cấp hoặc thu hồi quyền. Chuẩn bị hai tài khoản không root, mật khẩu khác nhau ngoài repo; với DB cũ, xác định tài khoản runtime hiện có trước khi thay đổi. Chỉ cấp quyền trong schema đó, không global/GRANT OPTION. Kiểm CURRENT_USER và SHOW GRANTS bằng công cụ quản trị riêng; guard username không kiểm được effective identity/grants. Không tạo secret trong shell history hoặc argv.
2. Với dữ liệu thật: dừng mọi writer, backup và full verify vào DB cách ly theo OPS trước khi nâng schema. Giữ app dừng nếu lỗi. Không tự repair, baseline, sửa migration cũ hoặc restore đè nguồn.
3. Dùng môi trường tiến trình riêng chỉ chứa MIGRATION_DB_URL, MIGRATION_DB_USER, MIGRATION_DB_PASSWORD cần thiết. URL chỉ nhận `jdbc:mariadb://127.0.0.1:PORT/SCHEMA` hoặc `[::1]`, không options/credential, không hostname/remote. Mật khẩu ít nhất16 ký tự, không placeholder. Chưa hỗ trợ/certify transport tới DB từ xa.

```bash
java -Duser.timezone=UTC -jar target/deadline-tracker-0.1.0-SNAPSHOT.jar migrate
```

Lệnh đọc MIGRATION_DB_* trực tiếp, không fallback DB_*, không nạp Spring context/web/session/ops, không nhận secret qua tham số. Flyway migrate V1–V4 rồi validate lịch sử và exact schema/trigger. Chỉ exit0 và MIGRATION_OK khi cả hai bước đạt. Lỗi exit khác0, thông báo ngoài không kèm URL/driver/secret. Migrate có thể đã áp một phần DDL trước lỗi: không suy ra rollback tự động từ mã thoát.

4. Đối chiếu migration thành công/installed_by, năm trigger/DEFINER và grant thực theo bảng identity bên dưới. Trên source W1 cũ là V1–V4; W1.1 source thêm V5–V9. Kết thúc cửa sổ migration: **chỉ khóa login hoặc cách ly credential của principal không còn dùng làm runtime**. Không khóa tài khoản đang phục vụ web/ops, kể cả khi nó đồng thời là definer của DB cũ. Người quản trị thu hồi DDL nâng schema theo quyền thực, nhưng **giữ tài khoản definer tồn tại và đủ quyền thực thi trigger**; không xóa account hay tự REVOKE ALL rồi mở app. Trên DB giả mới đã thử account migration riêng khóa login, chỉ giữ SELECT/INSERT/TRIGGER trên schema: Owner tự tạo, guard Owner và task/event vẫn chạy. Runtime DML cho V5–V9 cũng cần các quyền theo bảng dưới. Đây là fixture local, chưa chứng minh tối thiểu cho mọi thao tác/đích triển khai. Cửa sổ migration sau cần người có quyền mở lại login/cấp lại DDL đúng schema, rồi thu hồi lại; app không tự cấp quyền.
5. Khởi động web với profile prod hoặc lệnh ops trong môi trường khác, chỉ DB_URL/DB_USER/DB_PASSWORD runtime; prod thêm cấu hình bind/proxy theo DEPLOY. Không để MIGRATION_DB_* hoặc spring.flyway.url/user/password trong web/ops, kể cả Flyway đã tắt. Preflight từ chối bật spring.flyway.enabled. Runtime chỉ validate lịch sử Flyway và exact schema/trigger, không migrate/repair/create history. Lỗi thiếu migration, checksum hoặc metadata thì giữ đóng, không bỏ verifier.

## DB mới và DB đã migrate — IR-01

Người vận hành đối chiếu ba identity độc lập bằng công cụ quản trị và kết nối runtime tương ứng; không suy từ tên DB_USER hoặc MIGRATION_DB_USER rằng chúng trùng nhau. Ghi cả phần host của account khi kiểm principal/grants/definer; installed_by là lịch sử người chạy migration, không thay cho danh tính runtime hiện tại.

| Identity cần đối chiếu | Nguồn kiểm | Ý nghĩa khi chuyển đổi |
|---|---|---|
| Runtime thực | CURRENT_USER() trên kết nối dùng cấu hình/credential runtime thực và SHOW GRANTS của principal đó | Tài khoản web/ops còn cần đăng nhập; không ACCOUNT LOCK hoặc cách ly credential đang dùng |
| Người chạy migration lịch sử | installed_by của từng migration thành công trong flyway_schema_history | Giữ nguyên lịch sử; không rewrite để khớp migrator mới hoặc dùng nó quyết định khóa runtime |
| Definer của trigger hiện có | DEFINER của từng trigger trong information_schema.TRIGGERS và grants của account tương ứng | Giữ account tồn tại và quyền cần cho trigger; có thể trùng runtime cũ hoặc migration principal |

1. **DB mới:** dùng migration principal riêng áp các migration có trong artifact được duyệt. V4 không chỉ định DEFINER tường minh; đối chiếu definer thực sau khi tạo, không chỉ suy từ tên migrator. Trên W1.1 source kỳ vọng 9 migration và 5 trigger; version/schema contract phải khớp đúng jar. Chỉ khóa login migration/definer khi đã xác nhận principal đó không dùng làm runtime; vẫn giữ quyền trigger cần thiết. ACCOUNT LOCK trong ma trận dưới là trường hợp fixture DB mới này.
2. **DB đã migrate V1–V4:** không tạo lại DB hoặc chạy lại V4 chỉ để đổi definer. Migrator mới validate/migrate phần còn thiếu; việc đổi credential không tự đổi installed_by hoặc DEFINER hiện có. Nếu DB cũ dùng `shared@host` vừa làm runtime vừa là definer, giữ tài khoản đó đăng nhập được trong thời gian còn làm runtime, giữ definer và quyền trigger. Giảm quyền theo ma trận runtime và nhu cầu definer thực, không áp hàng khóa migration/definer một cách máy móc. Nếu cần chuyển runtime sang principal khác, đó là bước vận hành phải được duyệt riêng; chỉ cân nhắc khóa login account cũ sau khi xác nhận web/ops không còn dùng nó, vẫn không xóa definer.

Nếu migration/validate hoặc identity/grants chưa đạt, giữ app dừng để điều tra. Khi các kiểm đó đạt, khởi động web/ops bằng runtime theo bước 5 để kiểm có kiểm soát trên đích được duyệt: tạo nhóm sinh đúng Owner; thao tác làm mất Owner bị guard từ chối; ghi task/event cùng giao dịch và ca rollback không để dữ liệu dở dang. Chỉ mở lại cho sử dụng bình thường khi các ca kiểm đạt; nếu thất bại, dừng app để điều tra, không bỏ verifier, sửa V4, rewrite installed_by hoặc tự REVOKE ALL để thử. **Các kiểm identity/grants và hành vi trên đích này chưa chạy trong lượt sửa tài liệu IR-01**; không đổi thành acceptance server/Gate A.

## Ma trận quyền DB giả đã thử

| Principal/phạm vi | Quyền trong fixture | Ý nghĩa/giới hạn |
|---|---|---|
| Migration, cửa sổ migrate | ALL trên schema riêng, không global/GRANT OPTION | Áp V1–V9 cho W1.1 (V1–V4 trong hồ sơ cũ), ghi history, tạo trigger; chưa thu hẹp từng quyền DDL tối thiểu |
| Migration/definer riêng sau cửa sổ (DB mới) | SELECT, INSERT, TRIGGER trên schema; ACCOUNT LOCK chỉ khi không dùng làm runtime | Trigger vẫn chạy dưới definer đã khóa login trong fixture; không xóa principal. DB cũ có definer trùng runtime không áp khóa này |
| Runtime trên schema | SELECT, TRIGGER | Đọc business/history/metadata; TRIGGER **cho CREATE/DROP TRIGGER** |
| Runtime app_user, project_group, group_member, task, invitation | thêm INSERT, UPDATE | Không DELETE nghiệp vụ |
| Runtime task_event | thêm INSERT | Không UPDATE/DELETE event |
| Runtime task_comment | thêm INSERT, UPDATE | Tạo/sửa/ẩn mềm comment |
| Runtime task_comment_revision | thêm INSERT | Revision append-only |
| Runtime comment_mention | thêm INSERT, DELETE | Mention ID theo thành viên cùng nhóm |
| Runtime notification | thêm INSERT, UPDATE | Inbox create/read state |
| Runtime task_attachment | thêm INSERT, UPDATE | Metadata/staging/hide; blob ở filesystem riêng |
| Runtime task_pin | thêm INSERT, DELETE | Thêm/bỏ pin sau kiểm quyền service |
| Runtime notification_preference | thêm INSERT, UPDATE | Event preferences assignment/comment/mention và opt-in reminder offsets |
| Runtime push_preference, push_device, push_delivery | thêm INSERT, UPDATE | Opt-in, grant epochs và lease/retry; endpoint/key là secret |
| Runtime SPRING_SESSION và SPRING_SESSION_ATTRIBUTES | thêm INSERT, UPDATE, DELETE | Tạo/lưu/hủy và cleanup phiên |
| Runtime flyway_schema_history | chỉ SELECT | Không sửa history hoặc chạy migration mới |

Fixture chứng minh runtime bị từ chối CREATE TABLE, ALTER TABLE, DROP TABLE, sửa history và DELETE event; đồng thời CREATE/DROP trigger trên bảng probe **thành công**. Không gọi runtime là không DDL. Thiếu TRIGGER khiến verifier không thấy trigger và chặn startup; không bỏ quyền bằng cách bỏ kiểm tra. Nguy cơ runtime credential sửa trigger vẫn cần owner xem theo phạm vi server, lựa chọn A trước đây chỉ local/dữ liệu giả.

Fixture dùng schema/account ngẫu nhiên trong container disposable; host `%` chỉ là cách test kết nối container, không là mẫu cấp quyền public. Khi provisioning thật phải giới hạn host phù hợp, escape wildcard `_`/`%` đúng ngữ nghĩa schema GRANT và kiểm grant thực. Tài khoản backup/restore riêng theo OPS, không tái dùng runtime hoặc migration cho dump/restore.

## Bằng chứng và giới hạn

Xem [biên bản P3-02](verification/p3-02-remediation.md). Mô hình credential/grants phải được kiểm lại trên đích thật; preflight không tự phát hiện một non-root principal bị cấp quá rộng. Bằng chứng P3-02 lịch sử chỉ là V1–V4; W1.1 test riêng phần schema/grants ở V5–V9. Chưa thay JAR/DB local đang dùng, chưa deploy hoặc ký Gate A.
