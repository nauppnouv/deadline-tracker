# Tiến độ W1

Ngày 17/10/2026. Nhánh feat/w1-autonomous-build.

- Đang làm P0: kiểm chứng công nghệ, bảo toàn legacy-console, dựng build/check và bảng bằng chứng.
- P1–P6: chưa triển khai/chưa kiểm chứng.
- Docker Desktop 29.8.2 đã kiểm tra được khi được quyền truy cập pipe. Trong sandbox: permission denied while trying to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine.
- Java hệ thống 26 không phải LTS: đang tải JDK 25 portable trong .tools; không cài hệ thống.
- Bước tiếp: hoàn thành build/check P0, test trước cho US-01/02 rồi cổng P1.
- P7 và pilot M7 chưa thực hiện, thuộc phiên review/người dùng sau P6.

## P0 đã qua cổng build nền

- Build/check chạy thật thành công với JDK 25; đã có jar và test runtime 1/1.
- P1 kế tiếp: test trước cho Group/Task/Filters; chưa triển khai nghiệp vụ W1.
- Audit dependency chưa chạy; SEC-13 chưa kiểm chứng.

- P1: US-01/02 domain Group đã có test RED→GREEN, check 9 tests PASS. Tiếp theo: Task create/status/edit/filter (US-03…06).
