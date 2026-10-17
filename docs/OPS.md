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

```bash
bash scripts/backup.sh
```

Script từ chối backup trong repo, umask077, thư mục700, dump/sha256/manifest600. Dump single-transaction, routines/triggers/hex-blob rồi gzip; checksum SHA-256; chỉ giữ N file dump theo mẫu tên script tạo, giữ nguyên file khác. Trả BACKUP_FILE, không in mật khẩu. Nếu bị ngắt, file chưa có đủ checksum/manifest không được dùng và cần người kiểm tra. Không có lệnh xóa thư mục đệ quy.

**Dừng writes trong lúc tạo bộ backup dùng để verify** (stop app, gồm mọi instance; vẫn để DB chạy): dump nhất quán nhưng manifest count/checksum đọc sau dump. Khi writes tiếp tục, manifest có thể khác snapshot và restore test sẽ báo mismatch. Sau dump thành công có thể mở lại app. Không đổi schema giữa dump/manifest. Lưu cả ba file cùng nhau.

Trước mỗi migrate trên môi trường có dữ liệu thật: ngừng app → backup → verify vào DB thử → ghi nhận kết quả → mới chạy jar mới/Flyway. Flyway không tự backup. Nếu migration lỗi, giữ nguyên bằng chứng, không sửa migration đã áp, không tự hạ schema hoặc chạy restore đè nguồn.

## Thử phục hồi

Giữ DB nguồn nguyên vẹn. Chọn RESTORE_DB mới có prefix `deadline_restore_`, khác DB_NAME, chưa tồn tại. User phục hồi phải có quyền tạo DB thử; không dùng quyền đó cho web app.

```bash
export RESTORE_DB=deadline_restore_trial_20261017
export BACKUP_FILE=/absolute/path/outside/repository/deadline-....sql.gz
bash scripts/verify-restore.sh
```

Script xác minh SHA trước import, không overwrite DB tồn tại. Import vào DB mới; so từng bảng với manifest count/checksum EXTENDED, kiểm tra orphan cho mọi FK. In TABLE_OK/FK_OK và RESTORE_OK, giữ DB thử để xem. Không tự drop DB thử hay DB nguồn. Với dữ liệu thật, người vận hành còn phải chạy app cách ly trỏ DB thử, thử login/quyền nhóm/create/save, ghi thời gian phục hồi và kiểm tra backup ngoài máy trước quyết định chuyển nguồn. Tắt cleanup session khi so checksum.

BackupRestoreTest chạy hai script thật trong container MariaDB disposable, backup ở `/tmp/deadline-backups` ngoài thư mục repo trong container. Đã kiểm tra quyền600, row/checksum từng bảng, FK, từ chối đích nguồn/đích tồn tại/file sai hash, retention và giữ file khác. Đây không phải lịch backup thật hoặc diễn tập sự cố máy chủ công khai.

## Lịch và theo dõi

Người triển khai tạo lịch hằng ngày ở giờ ngừng writes được nhóm đồng ý; không có cron được cài bởi agent. Mỗi lần lưu mã thoát, ngày và đường dẫn file (không bí mật), cảnh báo nếu quá24h chưa có backup thành công. Sao chép bộ backup sang thiết bị khác có mã hóa và quyền truy cập phù hợp; kiểm tra lại SHA. Lập lịch restore drill định kỳ. Chỉ nói mục tiêu mất tối đa khoảng24h khi lịch này đã chạy và được giám sát. Chưa đo RTO4h trên môi trường thật.

Log ứng dụng không bật request/access/body/SQL/parameter DEBUG. Unknown error chỉ log reference và loại exception, trả thông báo có reference; không log toàn bộ throwable vì message có thể chứa input. Reverse proxy không bật access log chứa URI lời mời; nếu cần log phải loại token/cookie/header trước khi bật. Chỉ console reset được hiện mật khẩu tạm; không lưu session recording.
