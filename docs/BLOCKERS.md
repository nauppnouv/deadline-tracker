# Trở ngại môi trường

Ngày 17/10/2026.

| Việc | Trạng thái | Lỗi/nguyên nhân | Cách xử lý |
|---|---|---|---|
| Docker từ sandbox | Có đường xử lý | permission denied while trying to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine | Lệnh Docker được cấp quyền đã đọc được server 29.8.2; test Docker cần cùng quyền. |
| JDK LTS | Đang xử lý | Máy chỉ có JDK 26 và JDK 8 | Dùng JDK 25 portable có checksum trong .tools; không đổi hệ thống. |
| Pilot/triển khai thật/P7 | Chưa làm, không phải lỗi phần mềm | Cần người dùng và phiên review độc lập | Không tự tạo tài nguyên hoặc mời người dùng. |
