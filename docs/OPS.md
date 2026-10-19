# Vận hành W1

Các ví dụ chạy trên dữ liệu thử. Người vận hành chịu trách nhiệm xác định đúng máy/DB/tài khoản trước thao tác thật. Không ghi mật khẩu/token/session vào issue, shell history, log hay repo.

## Reset và đổi định danh

Chuẩn bị JDK25, jar đã qua check, biến DB_URL/DB_USER/DB_PASSWORD trỏ đúng DB. Mở terminal tương tác riêng; không pipe/redirect/ghi hình console có mật khẩu tạm.

```bash
java -Duser.timezone=UTC -jar target/deadline-tracker-0.1.0-SNAPSHOT.jar --spring.profiles.active=ops ops reset-password username
java -Duser.timezone=UTC -jar target/deadline-tracker-0.1.0-SNAPSHOT.jar --spring.profiles.active=ops ops anonymize-user username
```

Profile ops không mở web. Reset tạo mật khẩu tạm ngẫu nhiên, chỉ hiện console, lưu Argon2, bật must_change_password, hủy mọi phiên. Chuyển riêng cho đúng người sau xác minh danh tính; đăng nhập phải đổi trước khi dùng nhóm. Không đưa mật khẩu vào tham số CLI. Console không tương tác bị từ chối.

Anonymize yêu cầu gõ lại đúng username. Đổi thành deleted-UUID, tên Đã xóa, disabled, hash ngẫu nhiên không biết mật khẩu và hủy phiên; giữ task/membership/history. Không có undo trong UI. Không thực hiện nếu chưa xác nhận đúng người/quyền yêu cầu. Nội dung free text và backup có thể còn PII; cần rà soát riêng theo yêu cầu thật. W1 không chuyển Owner: khóa Owner có thể khiến nhóm không tạo lời mời được. Không có endpoint HTTP cho hai lệnh.

## Sao lưu

Cần Bash, GNU coreutils/find, gzip, client `mariadb` và `mariadb-dump` tương thích server. Script dùng MYSQL_PWD trong môi trường tiến trình con, không đưa mật khẩu vào argv; giới hạn quyền host và không bật shell xtrace. Backup là dữ liệu nhạy cảm dù không chứa mật khẩu thô.

Đặt DB_HOST, DB_PORT (mặc định3306), DB_USER, DB_PASSWORD, DB_NAME bằng secret store/envfile ngoài repo quyền600. Dùng user backup có đủ quyền dump bảng/routines/triggers; ví dụ local có thể dùng root DB thử. Đặt BACKUP_DIR là đường dẫn tuyệt đối **ngoài repo**, BACKUP_KEEP=7. Không dùng volume DB làm nơi giữ bản sao duy nhất.

Với MariaDB 11.8 đã pin, tài khoản sao lưu riêng (ví dụ `deadline_backup` từ host loopback) cần các GRANT sau; thay `deadline` bằng schema thật. Tạo tài khoản/mật khẩu ngoài repo trước, không chép bí mật vào SQL mẫu:

```sql
GRANT SELECT, SHOW VIEW, TRIGGER ON deadline.* TO 'deadline_backup'@'127.0.0.1';
GRANT SELECT ON mysql.proc TO 'deadline_backup'@'127.0.0.1';
```

`SELECT mysql.proc` phục vụ `--routines` trên image đã pin. Principal sao lưu không có DML hoặc DDL tổng quát; riêng quyền `TRIGGER` cho phép chạy `CREATE TRIGGER`, `DROP TRIGGER` và `SHOW CREATE TRIGGER` trên bảng được cấp quyền, nên không mô tả quyền này là "không DDL". Test F-06 dùng database và user tạm trong MariaDB test container để xác nhận phạm vi, không đổi quyền trên DB vận hành. `--single-transaction` không cần LOCK TABLES. Tài khoản chỉ SELECT/SHOW VIEW không đủ: script kiểm đủ năm trigger V4 theo tên/bảng/thời điểm/sự kiện và SHA-256 chính xác của ACTION_STATEMENT trong hợp đồng tin cậy; đồng thời kiểm đủ CREATE TRIGGER trong dump. Cùng hợp đồng body trigger chạy tại startup prod/test, trước backup và sau import restore. Thiếu quyền/đối tượng hoặc query lỗi thì không công bố BACKUP_FILE, không chạy retention, giữ bộ tốt trước đó. Retention chỉ chạy sau khi bộ mới qua kiểm tra và giữ ít nhất một bộ tốt (BACKUP_KEEP ≥ 1).

Hợp đồng `scripts/schema-required.txt` và `schema-inventory.sql` cũng đóng gói trong jar: profile prod/test từ chối khởi động khi mất trigger hoặc FK/CHECK/unique/NOT NULL/InnoDB then chốt. D-33/F-01/F-04/F-02 bổ sung exact-set SHA-256 định nghĩa CHECK/cột sinh/body trigger, collation cột văn bản và thuộc tính cột; từ chối đối tượng/định nghĩa chưa có hợp đồng. Cùng kiểm tra chạy trước dump và sau import restore, độc lập với checksum dữ liệu. Runtime principal cần nhìn thấy metadata trigger (TRIGGER trên schema). Không sửa V1–V4; không cần migration mới. [Metadata trigger MariaDB](https://mariadb.com/docs/server/reference/system-tables/information-schema/information-schema-tables/information-schema-triggers-table).

Chuẩn hợp đồng được lấy từ schema mới migrate V1→V4 (ck_username sau V3), trên MariaDB `11.8.9-MariaDB-ubu2404`, image `mariadb@sha256:f16d7a344413e519dff518f27bdb221388f84b0d274c97c4fb5f2cb0dc1b34fa` dùng chung với docker-compose.yml và DatabaseTest. Giá trị thực tế owner_marker: COLUMN_TYPE=`int(11)`, EXTRA=`VIRTUAL GENERATED`. Hash phụ thuộc cách MariaDB chuẩn hóa metadata; phiên bản khác có thể fail closed dù SQL tương đương. Không bỏ hash hay nới so sánh để chạy qua. Nâng phiên bản/migration sau cần dẫn xuất lại chuẩn từ schema tin cậy và review hợp đồng. Index thường ix_task_group_due/ix_event_task chưa thuộc hợp đồng.

```bash
bash scripts/backup.sh
```

Script từ chối backup trong repo, umask077, thư mục700, dump/sha256/manifest600. Dump single-transaction, routines/triggers/hex-blob rồi gzip; checksum SHA-256; chỉ giữ N file dump theo mẫu tên script tạo, giữ nguyên file khác. Trả BACKUP_FILE, không in mật khẩu. Nếu bị ngắt, file chưa có đủ checksum/manifest không được dùng và cần người kiểm tra. Không có lệnh xóa thư mục đệ quy.

Một thư mục backup chỉ chạy một lượt công bố/retention tại một thời điểm: khóa `.deadline-backup.lock` tạo bằng mkdir nguyên tử trước khi tạo file; lượt chồng lấn exit khác 0, không công bố hay retention. Khóa được gỡ khi thoát bình thường/lỗi có trap. Nếu máy chết hoặc SIGKILL để lại khóa, không tự phá khóa: người vận hành xác nhận không còn tiến trình backup, kiểm bộ dở rồi mới gỡ **thư mục khóa rỗng đó** bằng rmdir. Không xóa backup để giải quyết khóa. Test hai lượt song song đã tái hiện việc xóa chéo trước sửa và kiểm bản tốt còn sau sửa.

**Dừng writes trong lúc tạo bộ backup dùng để verify** (stop app, gồm mọi instance; vẫn để DB chạy): dump nhất quán nhưng manifest count/checksum đọc sau dump. Khi writes tiếp tục, manifest có thể khác snapshot và restore test sẽ báo mismatch. Sau dump thành công có thể mở lại app. Không đổi schema giữa dump/manifest. Lưu cả ba file cùng nhau.

Trước mỗi migrate trên môi trường có dữ liệu thật: ngừng app → backup → verify vào DB thử → ghi nhận kết quả → mới chạy jar mới/Flyway. Flyway không tự backup. Nếu migration lỗi, giữ nguyên bằng chứng, không sửa migration đã áp, không tự hạ schema hoặc chạy restore đè nguồn.

## Thử phục hồi

Giữ DB nguồn nguyên vẹn. Chọn RESTORE_DB mới có prefix `deadline_restore_`, khác DB_NAME, chưa tồn tại. User phục hồi phải có quyền tạo DB thử, import DDL/trigger/routine và CREATE ROUTINE/EXECUTE/ALTER ROUTINE trên DB thử để chạy rồi gỡ procedure kiểm hành vi; không dùng quyền đó cho web app. Procedure chỉ chạy trên RESTORE_DB mới, mọi hàng probe đều rollback. Dump giữ DEFINER của trigger: tài khoản đó phải tồn tại và có quyền cần thiết trên đúng schema phục hồi (TRIGGER và DML mà trigger dùng) trước khi kiểm hành vi. Người vận hành chuẩn bị quyền trên DB cách ly; script không tự cấp quyền hay đổi definer. Thiếu quyền cũng phải fail, không bỏ phép thử.

```bash
export RESTORE_DB=deadline_restore_trial_20261017
export BACKUP_FILE=/absolute/path/outside/repository/deadline-....sql.gz
bash scripts/verify-restore.sh
```

Script xác minh SHA trước import, không overwrite DB tồn tại. Import vào DB mới; kiểm kê trigger/FK/CHECK/unique/NOT NULL/InnoDB và đối chiếu exact-set CHECKDEF/GENERATEDDEF/TEXTDEF/COLDEF/TRIGGERDEF theo hợp đồng dùng chung, rồi thử tạo Owner, chặn hạ/đổi/xóa Owner và cho sửa metadata hợp lệ trong transaction rollback; so từng bảng với manifest count/checksum SHA-256 logic, kiểm tra orphan cho mọi FK. **Sau khi so snapshot**, script xóa mọi phiên Spring Session trong DB phục hồi (attributes xóa theo FK cascade), kiểm tra hai bảng phiên đều rỗng rồi in SESSIONS_CLEARED và RESTORE_OK. Dữ liệu nghiệp vụ giữ nguyên; mọi người phải đăng nhập lại. Tắt cleanup session khi so checksum. Không tự drop DB thử hay DB nguồn. Nếu định nghĩa sai, exit khác 0 trước probe/checksum/reset phiên, không RESTORE_OK; DB thử mới đã import được giữ cách ly để điều tra (chủ dự án đã xác nhận D-33). DB nguồn và đích có sẵn không đổi; restore không công bố backup hoặc chạy retention.

**RESTORE_OK chưa cho phép mở DB ra web.** Giữ ứng dụng cách ly, không phục vụ người dùng thật, rồi thực hiện:

**Bắt buộc: đặt lại mật khẩu từng tài khoản bằng ops reset-password trước khi mở lại web.** Mọi thông tin đăng nhập trong snapshot là không tin cậy, kể cả tài khoản không ghi nhận đổi mật khẩu sau snapshot. Sau xác minh đúng ID và quyền hiện tại, reset **tất cả tài khoản còn được phép đăng nhập**, cấp mật khẩu tạm qua kênh riêng và buộc đổi bằng luồng sẵn có. Giữ tài khoản bị vô hiệu hóa ở trạng thái vô hiệu hóa; không bật lại chỉ để reset. Sổ vận hành hỗ trợ xác minh và áp lại quyết định khóa, không miễn bước reset cho tài khoản nào còn hoạt động. Script chỉ in yêu cầu này; không tự đổi hash, enabled hoặc must_change_password.

1. Đối chiếu sổ vận hành tin cậy lưu ngoài bộ backup: thời điểm, ID tài khoản và loại thao tác reset/đổi mật khẩu/vô hiệu hóa sau snapshot; không ghi mật khẩu, token hoặc cookie vào sổ. Snapshot cũng khôi phục password hash, enabled và must_change_password về quá khứ; xóa phiên không tự sửa các giá trị này.
2. Xác nhận đúng ID trước khi áp lại quyết định vô hiệu hóa bằng lệnh ops `anonymize-user`; không dùng tên hiển thị làm danh tính. Xác minh trực tiếp từng người còn được phép rồi chạy `ops reset-password` cho từng tài khoản đó trên DB phục hồi, cấp mật khẩu tạm riêng và bắt buộc đổi. Áp dụng cho tất cả, không chỉ tài khoản đã đổi/reset mật khẩu sau snapshot. Không dùng khả năng đăng nhập bằng mật khẩu cũ làm bằng chứng xác minh.
3. Nếu mất sổ hoặc chưa xác định được trạng thái quyền hiện tại, **không mở web**. Chủ dự án phải xác minh lại từng tài khoản; vô hiệu hóa tài khoản không còn được phép, reset các tài khoản được xác nhận. Script không thể suy ra sự kiện xảy ra sau thời điểm bản sao lưu.
4. Chạy app cách ly trỏ DB phục hồi, xác nhận cookie cũ bị từ chối, tài khoản bị vô hiệu hóa không đăng nhập được, mật khẩu trước reset bị từ chối, quyền nhóm/create/save đúng. Ghi thời gian phục hồi, kiểm tra backup ngoài máy, rồi mới quyết định chuyển nguồn.

RestoredSessionTest tạo phiên thật, backup trước logout, restore DB riêng và gửi lại cookie cũ qua HTTP tới app phục hồi. Test yêu cầu redirect về login và không còn phiên authenticated trong DB.

BackupRestoreTest chạy hai script thật trong container MariaDB disposable, backup ở `/tmp/deadline-backups` ngoài thư mục repo trong container. Đã kiểm tra quyền600, row/checksum từng bảng, FK, từ chối đích nguồn/đích tồn tại/file sai hash, retention và giữ file khác. Đây không phải lịch backup thật hoặc diễn tập sự cố máy chủ công khai.

## Lịch và theo dõi

Người triển khai tạo lịch hằng ngày ở giờ ngừng writes được nhóm đồng ý; không có cron được cài bởi agent. Mỗi lần lưu mã thoát, ngày và đường dẫn file (không bí mật), cảnh báo nếu quá24h chưa có backup thành công. Sao chép bộ backup sang thiết bị khác có mã hóa và quyền truy cập phù hợp; kiểm tra lại SHA. Lập lịch restore drill định kỳ. Chỉ nói mục tiêu mất tối đa khoảng24h khi lịch này đã chạy và được giám sát. Chưa đo RTO4h trên môi trường thật.

Log ứng dụng không bật request/access/body/SQL/parameter DEBUG. Unknown error chỉ log reference và loại exception, trả thông báo có reference; không log toàn bộ throwable vì message có thể chứa input. Reverse proxy không bật access log chứa URI lời mời; nếu cần log phải loại token/cookie/header trước khi bật. Chỉ console reset được hiện mật khẩu tạm; không lưu session recording.

### Checksum dữ liệu sau review P7

Manifest mới ghi `sha256:` từ dữ liệu logic: tên cột theo ordinal; giá trị HEX có dấu phân biệt NULL; SHA-256 từng hàng được sắp ổn định rồi băm cả bảng. Bao gồm cột sinh và kiểm tra số hàng. Manifest cũ có checksum số vẫn được đối chiếu CHECKSUM TABLE và từ chối khi lệch; không tự bỏ qua mismatch. Lỗi truy vấn metadata/dữ liệu phải làm thất bại; bộ backup dở không được công bố hay chạy retention.

### F-02 — body trigger tại startup, backup và restore

`TRIGGERDEF` trong `scripts/schema-required.txt` là SHA-256 nguyên văn ACTION_STATEMENT của năm trigger V4, kèm tuple name/table/timing/event; được dẫn xuất từ schema tin cậy sau migrate V1–V4 trên image đã pin, không từ dump/DB có thể bị sửa. Startup prod/test, trước backup và sau restore dùng cùng exact-set; khoảng trắng trong chuỗi SQL cũng có ý nghĩa. Đây là so định nghĩa, không phải chứng minh tương đương ngữ nghĩa SQL. Dòng tuple `TRIGGER` vẫn kiểm riêng cấu trúc trigger. Không sửa V1–V4 để làm hash khớp; migration sau cần cập nhật hợp đồng qua review riêng. Không còn cần file allowlist thứ hai.
