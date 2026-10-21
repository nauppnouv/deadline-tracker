# Trở ngại, giới hạn và việc người làm

Cập nhật 18/10/2026. Bảy mục review vòng2 đã có RED → GREEN, commit riêng và clone sạch138 test PASS/0 failure/error/skip, mọi cổng OK (verification/r2-final.txt). Không còn bước sửa cục bộ nào bị chặn. Các kết quả58/79 test phía dưới là lịch sử P6/vòng1; giới hạn vận hành thật vẫn còn như PROGRESS/EVIDENCE và danh sách này. Không push/merge hoặc tạo tài nguyên thật.

| Việc | Trạng thái / bằng chứng | Hành động |
|---|---|---|
| Docker sandbox | Đã có đường chạy; lỗi ban đầu: permission denied while trying to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine | Terminal có quyền Docker Desktop; không bỏ test DB |
| Git Bash sandbox | Đã có đường chạy; NtCreateDirectoryObject … 0xC0000022 | Lệnh được cấp quyền đã chạy check thật |
| JDK26 không LTS | Đã xử lý bằng Temurin25.0.4.1 portable có SHA256, không cài hệ thống | Người clone cài/chỉ JAVA_HOME JDK25 |
| Docker Compose plugin trong config cách ly | Lần đầu unknown flag: --project-name do plugin không được tìm thấy | Smoke script dùng cliPluginsExtraDirs từ Docker Desktop vendor, auths rỗng; đã PASS |
| NVD/SCA | Tải đầu mất khoảng35phút; scan Tomcat11.0.24 có High/Critical | Đã vá11.0.27 và audit lại0findings; scan Java không thay image/OS scan; cần mạng/cập nhật lại |
| Flyway/MariaDB | Runtime WARN: Using MariaDB 11.8 which is newer than the version Flyway has been verified with. The latest verified version of MariaDB is 11.7. | Giữ cảnh báo D23; migrations/validate/restart/restore chạy thật pinned11.8.9. Reviewer P7 đánh giá tương thích trước deploy; không gọi vendor certified |
| Lịch backup thật | Chưa cài; mới có scripts và drill DB thử | Chủ vận hành chọn nơi giữ ngoài máy, lịch/người chịu trách nhiệm và cảnh báo; chưa cam kết RPO24h/RTO4h |
| HTTPS/proxy/rate-limit Internet | Chưa triển khai | Chủ dự án quyết định chi phí/domain; kiểm tra certificate, forwarded headers/IP thật và cookie trên đúng môi trường |
| Câu chữ SEC05 / D25 | Có giới hạn diễn giải công khai: framework phải lưu session ID/CSRF và trả cookie, raw invite cần phản hồi phân phối | Reviewer xác nhận phạm vi; không tuyên bố nghĩa đen cấm mọi ID/token trong mọi DB/response đã đạt |
| P7/pilot | Review đã tìm năm phát hiện, bản sửa và clone sạch đã qua 79 test/toàn bộ check; biên bản verification/p7-final.txt; pilot chưa chạy | Reviewer kiểm lại bản sửa trong phiên độc lập; pilot2tuần chỉ sau đồng ý của nhóm và kiểm chứng vận hành |
| Volume thử Compose | Cố ý giữ deadline-w1-e9918b41_database và deadline-w1-7c56fed6_database, chỉ dữ liệu giả; container/network đã gỡ | Chủ máy có thể xem report và quyết định dọn riêng volume thử; không global prune |

Không có cloud/account/email/domain/resource tính phí nào được agent tạo, không dùng credential thật. AWS2GB dự toán vượt50k/tuần; lựa chọn rẻ hơn vẫn cần xác nhận tổng phí và tình trạng còn hàng.

## W1.1 — trạng thái hiện tại, 21/10/2026

| Việc | Trạng thái / bằng chứng | Hành động |
|---|---|---|
| Web Push endpoint và sender | Đã bổ sung provider allowlist, DNS/IP checks ngay resolver của socket, TLS hostname gốc, không proxy/redirect/retry ngầm; fake transport và blocked-DNS tests có receipt W1.1 riêng. Sender mặc định tắt. | M6 cần kiểm provider/browser/origin/egress thực; không suy provider accepted là popup đã hiện. |
| Desktop push E2E | Chưa chạy; chưa có trusted HTTPS origin/browser/provider được cấp phạm vi. Không bật browser permission trên profile thật. | Cần M6 riêng với origin, browser test context được phép và quyết định host/budget. |
| Antivirus / file scanning | Chưa có scanner. Kiểm tra magic/OOXML chỉ xác nhận định dạng giới hạn, không phải malware scan. | Server owner chọn scanner/policy và vận hành hook; không tuyên bố file an toàn. |
| Backup generation pair | Local test `BackupRestoreTest#pairedAttachmentGeneration` chạy DB synthetic + blob synthetic thành công; script đòi operator xác nhận writers đã pause. Chưa có kiểm chứng trên volume/app thực hoặc offsite. | Trước backup thật dừng mọi app writer; restore vào DB/root mới và giữ cách ly tới khi hậu kiểm xong. |

P7-02: full check đầu phiên tái hiện `Mismatch: group_member`. So 16 hàng/cột xác nhận dữ liệu nguồn/restored giống nhau; thay checksum native bằng SHA-256 logic cho manifest mới, giữ đếm hàng/FK và từ chối dữ liệu thay đổi. Full check bản sửa 70 test PASS, static/gitleaks/dependency/migration OK (target/p7-02-final-check.log); lỗi test này đã giải quyết.


## W11-R1 — dependency audit NVD (21/10/2026)

AI viết: **chưa đọc**. Docker hiện chạy được; regression HTTP/MariaDB và toàn274test/34class của bản sửa đều qua. Full `scripts/check.sh` có receipt exit127 ở dependency-check12.1.8: log báo `NVD API request failures are occurring; retrying request`. Hai thread dump cho thấy Maven đang chờ NvdCveClient future; cache chưa update.

Sau15phút riêng audit, tiến trình Maven audit có command line thuộc evidence root được dừng và ghi `AUDIT-TIMEOUT.json`; không đổi dependencies, threshold, tắt audit/update hoặc dùng PASS cũ. Build/package/static/gitleaks trước đó qua; migration guard sau audit chưa được full chạy tới, kiểm riêng exit0. Đây là blocker audit/SEC-13, không phải behavioral RED R1 và không phải full PASS.

Receipt: `D:/Documents/coding/security-revalidation-w11-r1-20261021T213445+0700/full-worktree-receipt.json`. Chạy lại audit/check khi NVD API truy cập ổn định; nếu operator có NVD API key thì cấu hình ngoài source, không ghi key vào log. Không mở pilot/Gate A từ kết quả test này.
