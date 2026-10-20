# Triển khai để người vận hành thực hiện

Chưa có tài nguyên cloud/domain/certificate/lịch backup nào được tạo. P7 phải review trong chat mới trước khi đưa dữ liệu/người dùng thật. Những bước dưới là runbook, **chưa được kiểm chứng trên máy chủ Internet**.

## Kiến trúc đề xuất

Một VPS Linux x86_64, JDK25, MariaDB11.8 (volume bền vững) và Caddy. Caddy trên host nhận443 → app trên host 127.0.0.1:8080 → MariaDB container bind127.0.0.1:3307. Một instance, không ALB/RDS/Kubernetes. Firewall cloud và host chỉ mở80/443 công khai, SSH22 chỉ IP người vận hành; không public8080/3307. Backup ngoài repo và có bản sao ngoài máy. Chọn đủ RAM và đo lại, không suy SLA từ máy phát triển.

## Chi phí và quyết định ngân sách

Tra nguồn chính thức ngày17/10/2026. Giả định dự toán, **không phải tỷ giá hiện hành**: USD=26.000đ, EUR=30.000đ; tháng730giờ, tuần168giờ. Tính bằng Python Decimal, chưa gồm domain/thuế/phí thẻ/backup ngoài máy/overage. Không giả định được Free Tier hoặc credit.

| Phương án | Giá cơ sở/tháng | Quy đổi dự toán | Mỗi tuần | Với dự phòng20%/tuần |
|---|---:|---:|---:|---:|
| AWS Lightsail Linux IPv4 2GB | $12 | 312.000đ | 71.803đ | 86.163đ |
| Hetzner CX23 EU + IPv4 | €5,49 + €0,50 | 179.700đ | 41.356đ | 49.627đ |
| Máy cá nhân, chỉ loopback | không thuê cloud | 0đ thuê máy | 0đ thuê máy | điện/mạng chưa tính |

AWS bundle2GB gồm60GB SSD và transfer theo điều kiện vùng; **vượt50k/tuần**, chỉ cân nhắc khi chủ dự án duyệt thêm ngân sách hoặc credit thực sự đủ. Không chọn gói512MB để ép Java+DB vào ngân sách. [Giá Lightsail](https://aws.amazon.com/lightsail/pricing/).

CX23 là lựa chọn rẻ hơn nhưng sát trần sau dự phòng, vùng EU có độ trễ từ Việt Nam, giá excl.VAT và **có thể hết hàng**; không thay Singapore/gói đắt hơn khi chưa tính lại. Giá mới€5,49 từ15/6/2026, IPv4€0,50 riêng. Xác nhận VAT, backup, domain và khả năng đăng ký/thanh toán trước đặt; nếu tổng vượt trần thì tiếp tục local. [Giá Hetzner](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/), [IPv4](https://docs.hetzner.com/cloud/servers/primary-ips/overview/), [cấu hình CX](https://www.hetzner.com/de/news/new-cloud-plans/).

## AWS Lightsail — thao tác thủ công

1. Chủ tài khoản kiểm tra vùng/giá, dự toán tổng, thông báo ngân sách và quyền tạo tài nguyên. Chỉ sau quyết định chi phí mới tạo Linux x86_64 2GB, SSH key riêng, static IP theo bundle. Không đặt credential trong user-data/repo.
2. Cập nhật OS, cài Docker/JDK25/Caddy theo nguồn chính thức; tạo user hệ thống không login cho app, thư mục app chỉ người vận hành ghi, file cấu hình secret ngoài repo quyền600. Hạn chế SSH bằng key và IP, không mở DB.
3. Lấy commit đã review, chạy check trên máy build rồi chuyển jar có SHA256 tới máy chủ. Không build ngay trên VPS2GB nếu thiếu RAM. Dùng đúng MariaDB image digest trong compose, `docker compose up -d db` sau khi thiết lập `.env` riêng tại máy chủ. Compose app là cấu hình **local dev**, không dùng nguyên file để public.
4. Chọn nhánh **DB mới/DB đã migrate** theo [MIGRATION — IR-01](MIGRATION.md#db-mới-và-db-đã-migrate--ir-01). DB mới tạo schema và hai principal riêng; DB cũ giữ dữ liệu/lịch sử/trigger hiện có, không tạo lại DB hoặc chạy lại V4 để đổi definer. Migration có quyền nâng schema trong cửa sổ bảo trì; runtime có quyền đọc, DML theo bảng và TRIGGER để kiểm metadata. Dừng app, backup/verify nếu có dữ liệu, rồi chạy tiến trình `java -Duser.timezone=UTC -jar target/deadline-tracker-0.1.0-SNAPSHOT.jar migrate` với MIGRATION_DB_* riêng. Sau MIGRATION_OK, đối chiếu **CURRENT_USER runtime thực, installed_by lịch sử và DEFINER từng trigger** cùng grants thực; không coi ba identity là một. Chỉ khóa login/cách ly credential của principal không còn dùng làm runtime: definer DB cũ có thể vẫn là runtime, nên không khóa nhầm hoặc xóa definer, không tự REVOKE ALL hay rewrite installed_by. Chỉ sang bước 5 khi migration/identity/grants đạt; sau khi khởi động để kiểm có kiểm soát, kiểm tạo nhóm → Owner, guard Owner và task/event/rollback theo MIGRATION trước khi mở lại cho sử dụng bình thường. Lỗi thì dừng app để điều tra. Prod/ops tắt auto-migration và từ chối credential migration trong môi trường runtime. TRIGGER vẫn cho phép sửa trigger; chưa có principal introspection riêng. Không cấp root/global cho app. DB dump/restore dùng principal riêng theo OPS; các kiểm grants/identity và hành vi trên máy chủ này **chưa được agent thực hiện**, không phải Gate A hoặc chấp thuận triển khai.
5. Đặt app host bằng service tự restart, chạy user không root, WorkingDirectory thư mục app, secret EnvironmentFile ngoài repo; profile prod, bind127.0.0.1, giới hạn bộ nhớ phù hợp. Đường dẫn jar tuyệt đối. Không ghi secret vào ExecStart; JVM UTC.
6. Cấu hình DNS/certificate/proxy dưới đây; kiểm thử bên ngoài, rồi backup/restore/quyền/rate limit. Chỉ mời người dùng sau P7 và chủ dự án quyết định pilot.

## Lựa chọn rẻ hơn — Hetzner hoặc local

Hetzner: chủ dự án tự đăng ký/xác minh và kiểm tra giá cuối. Chọn CX23 x86_64 EU chỉ nếu còn hàng và đủ ngân sách; tạo IPv4 riêng, firewall/SSHkey, Linux được hỗ trợ. Sau đó dùng cùng mô hình host app + DBloopback + Caddy và các bước2…6. Không chọn ARM nếu chưa kiểm tra image/JDK và đo lại. Không tự tạo server/API token. Nếu chưa thể trả phí, chạy compose loopback như README; không mở router/tunnel công khai để coi như đã deploy HTTPS.

## Cấu hình và HTTPS

| Biến | Giá trị/trách nhiệm |
|---|---|
| SPRING_PROFILES_ACTIVE | prod trên máy chủ, dev chỉ máy cá nhân |
| DB_URL | jdbc:mariadb://127.0.0.1:3307/deadline cho mô hình host |
| DB_USER / DB_PASSWORD | principal schema riêng của app / mật khẩu ít nhất 16 ký tự; secret ngoài repo |
| DB_ROOT_PASSWORD | chỉ cho bước bootstrap/provisioning DB riêng; không đưa vào EnvironmentFile hoặc môi trường JVM web |
| MIGRATION_DB_URL / MIGRATION_DB_USER / MIGRATION_DB_PASSWORD | chỉ tiến trình migrate riêng, không có trong EnvironmentFile của web/ops; URL loopback dạng số, không options |
| SERVER_ADDRESS / SERVER_PORT | 127.0.0.1 / 8080 trên host |
| TZ / JVM | UTC / -Duser.timezone=UTC |
| Forwarded headers / Tomcat trust | dùng nguyên khối EnvironmentFile bên dưới, chỉ cho Caddy trên host loopback |

Prod kiểm preflight trước khi tạo context/kết nối DB: thiếu DB_PASSWORD, placeholder mới `CHANGE_ME_...` hoặc placeholder cũ, dưới 16 ký tự đều bị từ chối. Mật khẩu datasource effective/override cũng phải hợp lệ. Username datasource effective `root` bị từ chối (không phân biệt hoa/thường); đây chỉ là chặn sai cấu hình, không chứng minh quyền DB thực. Web không yêu cầu hoặc đọc DB_ROOT_PASSWORD. Phải đặt tường minh SERVER_ADDRESS thành địa chỉ loopback dạng số; override server.address cũng phải loopback; forwarded headers phải native. application-prod mặc định bind 127.0.0.1 là lớp chặn bổ sung, không thay nghĩa vụ đặt SERVER_ADDRESS. Không in giá trị mật khẩu trong thông báo lỗi. Compose là **DEV ONLY**, không dùng nguyên tệp để public.

**P3-01/P3-02 — ranh giới provisioning:** người cấp DB/tài khoản kiểm tra principal schema, secret app/root/migration độc lập và đúng quyền bằng công cụ quản trị riêng; không chuyển root secret sang app để so chuỗi. Web/ops chỉ nhận DB_* của runtime; root secret thuộc bootstrap, MIGRATION_DB_* thuộc tiến trình migrate. Không dùng root giả để đi vòng kiểm. Chưa có provisioner tự động hoặc kiểm effective DB identity/grants mới; JDBC runtime có thể override danh tính nên kiểm username không thay kiểm principal thực. P3-02 tách đường chạy và đã thử quyền giảm trên DB giả; residual TRIGGER và quyết định cho môi trường server vẫn mở. Xem ma trận quyền và việc giữ definer trong MIGRATION.md.

Một khối cấu hình forwarded headers duy nhất để sao chép vào **EnvironmentFile của systemd** (giữ nguyên một backslash mỗi dấu chấm; đây không phải lệnh shell):

```ini
SERVER_FORWARD_HEADERS_STRATEGY=native
SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES='127\.0\.0\.1|::1'
SERVER_TOMCAT_REDIRECT_CONTEXT_ROOT=false
```

Giữ dấu nháy đơn quanh regex: EnvironmentFile giữ nguyên backslash bên trong chuỗi này theo [quy tắc systemd](https://github.com/systemd/systemd/blob/main/man/systemd.exec.xml). **Mô hình này chạy app trên host**. Nếu app chạy container, peer IP khác loopback; không sao chép nguyên trusted-proxy config rồi mở port. Rate limiter dùng địa chỉ Tomcat đã xác thực; không tự đọc header do client gửi. Kiểm giá trị bind thực sau triển khai. Preflight theo [EnvironmentPostProcessor của Boot 4.1.1](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/EnvironmentPostProcessor.html); kiểm cấu hình không thay thế kiểm HTTPS/proxy/IP ngoài Internet.

Caddyfile tối thiểu (thay bằng domain bạn sở hữu):

```caddyfile
deadline.example.com {
    request_body {
        max_size 16384
    }
    reverse_proxy 127.0.0.1:8080
}
```

Caddy xin/gia hạn certificate cho DNS public hợp lệ khi80/443 truy cập được; giữ data directory quyền phù hợp. Không bật access logs chứa invitation URL. Proxy phải ghi đè forwarded headers, app chỉ tin peer loopback. Tài liệu [Caddy HTTPS](https://caddyserver.com/docs/automatic-https), [reverse proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy), [Boot forwarded headers](https://docs.spring.io/spring-boot/how-to/webserver.html). HTTP phải redirect HTTPS; không bypass cảnh báo chứng chỉ.

Người vận hành kiểm tra bằng browser/curl HTTPS thật: chain hợp lệ, HSTS, CSP/no-inline, no-referrer, nosniff, cookie Secure/HttpOnly/SameSite, login/logout, token không vào access log. Thử giả X-Forwarded-For từ Internet, xác minh peer trust và limit không bị vượt/khóa tất cả người dùng thành một IP; test này **chưa chạy**. Không public đến khi đạt.

## Backup, phát hành và dọn dẹp

Theo [OPS](OPS.md): **backup + verify trước mỗi migration có dữ liệu thật**, dừng writes, file600 ngoài repo, checksum, retention, bản sao ngoài máy, lịch ngày có cảnh báo. Chưa cài lịch nên chưa cam kết RPO24h/RTO4h. Giữ jar trước, migration chỉ tiến; rollback code chỉ khi tương thích schema, nếu không điều tra và phục hồi vào DB riêng trước khi quyết định chuyển DB.

Theo dõi heap/OOM, disk/volume/backup dung lượng, DB connections, mã lỗi tham chiếu; không bật body/SQL DEBUG. Đặt cảnh báo chi phí/ngưỡng ngày theo ngân sách đã duyệt; dừng máy có thể vẫn bị tính disk/IP/snapshot.

Kết thúc lab: xuất/verify backup cần giữ → người vận hành xác định đúng project/server → gỡ service và tài nguyên đã tạo. AWS kiểm tra instance, staticIP tách rời, snapshot, disk, DNS và billing; Hetzner kiểm tra server, PrimaryIP, volume/snapshot/backup và hóa đơn. Đóng tài nguyên phụ cũng cần người xác nhận. Không tự xóa volume dữ liệu; không dùng global prune. Agent chưa thực hiện các thao tác này.

P7-04/R2-04: app giới hạn body thực nhận 16.384 byte, kể cả chunked; body quá cỡ trả 413. Content-Type cụ thể không được hỗ trợ (ví dụ application/octet-stream) trả 415; wildcard `application/*` và `*/*` với body form trả **400**, không ghi dữ liệu. Form mã hóa sai có thể trả 400; charset không được hỗ trợ trả 415. Đây là mô tả hành vi đã đo bằng HTTP localhost, không đổi mã xử lý request. Proxy Caddy áp cùng giới hạn theo [request_body](https://caddyserver.com/docs/caddyfile/directives/request_body). Cấu hình proxy/HTTPS thật chưa được chạy, phải xác minh khi triển khai.

P7-05/V4: migration principal cần CREATE TRIGGER. Backup/verify trước nâng cấp; V4 từ chối dữ liệu đã thiếu/sai membership Owner, không tự chọn Owner khác. Nếu thất bại, giữ app đóng và đối chiếu nhóm/tài khoản với chủ dự án, khôi phục quy tắc đúng trên DB cách ly trước khi xử lý migration thất bại theo Flyway. Không sửa migration cũ hoặc tự repair để bỏ qua kiểm dữ liệu. Tạo membership Owner do DB thực hiện cùng INSERT nhóm; UPDATE/DELETE làm mất Owner bị từ chối.

## W1.1 — chuẩn bị, chưa triển khai

W1.1 source cục bộ thêm migration V5–V9, comments/mentions/inbox, file đính kèm private/pin và reminder preferences. JAR launcher hiện hành chưa có các migration này; **không trỏ launcher đang dùng vào DB đã lên V5+**. Chỉ lấy JAR từ artifact commit W1.1 sau khi full `scripts/check.sh`, hash/candidate receipt, clean clone và quyền triển khai đã được owner xem xét riêng. Chưa có server/runtime grant thật hoặc quyết định mở public.

Trước M6, vận hành cần đặt `ATTACHMENT_ROOT` ra ngoài repo, webroot và thư mục release; giới hạn quyền filesystem, dung lượng/phân vùng, owner/quota và chính sách retention. Không bật file endpoints trước khi backup generation gồm DB+blobs chạy theo [OPS](OPS.md). Scanner malware chưa có; kiểm signature/OOXML không thay thế scanner, nên server phải có policy kiểm soát trước khi cho user tải file. Web Push local đã có endpoint, transport HTTPS/provider allowlist/DNS-at-socket, grants/epochs và outbox. Sender mặc định tắt; cấu hình/runtime policy và M6 checklist ở [W11_SERVER_RUNBOOK](W11_SERVER_RUNBOOK.md). Chưa có native popup/tab-closed trên thiết bị thật; chỉ bật gửi thật khi có scope M6 riêng.

Migration/runtime principal phải áp V5–V8 qua lệnh `migrate` riêng; giữ V1–V4 bất biến, xác nhận Flyway history V1–V8/exact metadata và cập nhật table-level grants theo [MIGRATION](MIGRATION.md). Đã thử runtime grants mới trong MariaDB disposable; chưa xác minh CURRENT_USER/SHOW GRANTS/DEFINER trên server mục tiêu. Không cấp app quyền đọc ngoài schema hoặc filesystem rộng. Mọi backup/restore thật dừng writers và giữ restore cách ly cho đến khi DB, blob hashes, quyền tài khoản và phục hồi đều được owner kiểm tra.
