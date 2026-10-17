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

- P1 hoàn tất phần miền US-01…06: check 19 tests PASS. Chuyển P2 schema/JPA/giao dịch và test MariaDB thật. Không kết luận cổng web đã qua.

## P2 đã qua cổng lưu trữ

- Check cuối P2 27 tests PASS trên MariaDB thật; migrations V1/V2, atomic writes + events, optimistic conflicts, atomic invitation claim, restart overdue load.
- Testcontainers image pinned by digest. Hibernate connection metadata logging suppressed to avoid JDBC credentials in INFO logs.
- JUnit theo Boot BOM do JUnit 5 pin gây NoSuchMethodError (D-15); READ_COMMITTED cho pool (D-16).
- Tiếp theo P3: tài khoản, phiên, rate limit, bảo vệ route và test ma trận quyền.

## P3 đã qua cổng hiện tại

- Account/session/CSRF/rate limit/central group access có test. 36 tests PASS; cookie flags/fixation/expiration và revoke all sessions đã kiểm chứng.
- Maven/user home/Docker client config được cách ly trong repo; credential principal bị erase trước khi lưu session.
- Tiếp P4: lời mời bằng token, hết hạn mốc biên, thu hồi, atomic join và luồng web.

## P4 đã qua cổng

- 42 tests PASS. Group create/settings, token invites, role checks, expiry/revoke/concurrent join có bằng chứng.
- Màn hình nhóm/lời mời cơ bản đã chạy qua MVC; chưa đánh giá hình thức/responsive bằng trình duyệt.
- Tiếp P5: task forms/details/status, dashboard/filter, XSS/headers và ma trận mọi route nhóm.

## P5 đã qua kiểm tra chức năng

- 48 tests PASS; UI task/filter/progress + XSS/object access đã kiểm chứng bằng MockMvc và MariaDB.
- P6 đang bắt đầu: vá Tomcat theo audit, ops/backup/restore, tài liệu, browser/clean clone và chạy lại toàn bộ cổng.
- SEC-13 hiện KHÔNG ĐẠT: Tomcat 11.0.24 theo dependency-check, report ở target/dependency-check-report.json.
- Chưa có deploy thật, pilot hoặc review P7.
