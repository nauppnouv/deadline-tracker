# Trở ngại, giới hạn và việc người làm

Cập nhật17/10/2026. Không có build/test blocker đang mở ở mốc check58tests. Clone sạch còn chờ thực hiện.

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
| P7/pilot | Chưa chạy, cần người/phiên mới | Mở chat mới Reviewer, sửa findings rồi chạy lại cổng; pilot2tuần chỉ sau đồng ý của nhóm |
| Volume thử Compose | Cố ý giữ deadline-w1-e9918b41_database và deadline-w1-7c56fed6_database, chỉ dữ liệu giả; container/network đã gỡ | Chủ máy có thể xem report và quyết định dọn riêng volume thử; không global prune |

Không có cloud/account/email/domain/resource tính phí nào được agent tạo, không dùng credential thật. AWS2GB dự toán vượt50k/tuần; lựa chọn rẻ hơn vẫn cần xác nhận tổng phí và tình trạng còn hàng.
