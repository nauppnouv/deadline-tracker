# Quyết định triển khai W1

Ngày 17/10/2026. Phạm vi tự chủ P0–P6; không tạo tài nguyên tính phí hoặc dùng dữ liệu thật. Giữ nguyên bài console trong legacy-console/.

## Quyết định mặc định đã được giao

- K-1: Owner được tự nhận việc thông qua giao lại hợp lệ; reset TODO và ghi REASSIGNED.
- K-2: So hạn ở mức phút; gửi lại cùng phút không coi là đổi hạn. Giao lại việc đã quá hạn không buộc sửa hạn.
- K-3: Lời mời một lần, hiệu lực 24 giờ, hash SHA-256; token ngẫu nhiên 256 bit.
- K-4: Chỉ lệnh ops vô hiệu hóa/đổi định danh tài khoản; không xóa việc và lịch sử. Đây là giả danh hóa tài khoản, không hứa xóa mọi dữ liệu cá nhân trong nội dung tự do.
- K-5: Reset mật khẩu bằng ops; yêu cầu mật khẩu hiện tại khi đổi bình thường; reset tạo mật khẩu tạm và hủy phiên.
- K-6: Chỉ hướng dẫn triển khai AWS/phương án khác, không tạo tài nguyên.
- K-7: Bổ sung UAT-15 CSRF, UAT-16 XSS, UAT-17 rate limit, UAT-18 giao lại việc quá hạn.
- K-8: Tài nguyên nhóm không được truy cập trả 404; thành viên thiếu quyền hành động trả 403.
- K-9: Rate limit một instance, theo IP và username; ghi giới hạn khi triển khai.

## Diễn giải mâu thuẫn

- D-09: SEC-05 không thể cấm Spring Session JDBC lưu session ID trong bảng phiên trong khi đã bắt buộc dùng module này. Cấm mật khẩu/token mời dạng rõ trong DB và cấm bí mật trong log/phản hồi; chỉ cho phép ID và dữ liệu phiên trong bảng phiên framework quản lý. Cookie phiên là kênh chuyển session ID cần thiết. Nguồn: https://docs.spring.io/spring-session/reference/configuration/jdbc.html.
- D-10: CSRF được kiểm tra trước phân quyền: POST thiếu/sai CSRF trả 403 kể cả không đăng nhập. GET tài nguyên nhóm của người ngoài trả 404; POST có CSRF hợp lệ mới kiểm tra 404/403 theo K-8. Không nới CSRF để cố đồng nhất mọi lỗi thành 404.
- D-11: Ràng buộc unique owner_marker chỉ đảm bảo tối đa một Owner. Đảm bảo tối thiểu một Owner bằng FK project_group.owner_id -> app_user và giao dịch tạo membership; không cung cấp thao tác xóa/chuyển Owner. Test kiểm tra cả hai hướng, không gọi một unique index là bằng chứng đủ.
- D-12: Rate limit kiểm tra IP và username riêng để đổi username không vượt được hạn IP và đổi IP không vượt được hạn tài khoản. Không tin X-Forwarded-For từ client; deployment phải cấu hình proxy đáng tin riêng.
- D-13: Profile ops bắt buộc không khởi động web, dùng Console để nhập bí mật thay vì nhận mật khẩu qua argv/log.
- D-14: PasswordEncoder dùng Argon2 để không gặp cắt ngầm mật khẩu dài ở mốc 72 byte của bcrypt; hỗ trợ 15–128 ký tự theo BR-24.

## Nguồn công nghệ đang kiểm chứng

- Spring Boot system requirements: https://docs.spring.io/spring-boot/system-requirements.html
- Java LTS roadmap: https://www.oracle.com/java/technologies/java-se-support-roadmap.html
- JUnit 5.14.4: https://docs.junit.org/5.14.4/overview.html

Phiên bản khóa và checksum công cụ được bổ sung sau khi tải/resolve thành công. Java hệ thống không bị thay đổi.

## Phiên bản P0 đã resolve/chạy

- Temurin JDK 25.0.4.1+1 LTS portable; archive SHA-256: 00c847d804f4a78e9f04f2683faf14fed898535b177b7fc704486cb0284e9283. Tải từ Adoptium API, đối chiếu checksum trước giải nén.
- Spring Boot 4.1.1 (stable trên tài liệu chính thức, artifact Maven Central resolve thành công). Không chọn release metadata 4.2.0-M2 vì đó là milestone.
- Maven hệ thống 3.9.15; dependency cache riêng .m2-local. JUnit Jupiter 5.14.4 override BOM để đúng yêu cầu JUnit 5.
- P0 check: JDK 25 runtime test 1/1 PASS; jar đóng gói được; gitleaks không phát hiện rò rỉ trong working tree. Audit phụ thuộc chưa chạy, không đánh dấu SEC-13 đạt.

- D-15 (P2, bằng chứng): Spring Test của Boot 4.1.1 gọi ExtensionContext.Store.computeIfAbsent, không có trong JUnit 5.14.4; lần chạy P2 thực tế lỗi NoSuchMethodError. Dùng JUnit Jupiter do Boot BOM quản lý (6.x) thay ghim 5 để bảo đảm bộ kiểm thử chạy với Spring hiện hành; không hạ Spring hoặc giấu lỗi. API test vẫn là org.junit.jupiter. Quyết định này thay pin JUnit ở P0.
- Testcontainers dùng các module 2.0.5 do Boot BOM quản lý, theo tài liệu chính thức https://java.testcontainers.org/modules/databases/mariadb/; tránh trộn module 1.x với core 2.x.
- Cascade chỉ dùng cho bảng phiên framework; FK nghiệp vụ đều RESTRICT. Xóa phiên không đồng nghĩa xóa dữ liệu công việc.

- D-16: Pool dùng READ_COMMITTED; test hai giao dịch đồng thời ở default REPEATABLE_READ của MariaDB 11.8.9 trả lỗi record-changed thay vì số hàng 0 cho optimistic version. READ_COMMITTED cùng @Version giúp báo xung đột rõ; không thay đổi server DB chung. Các thao tác quan trọng vẫn có predicate nguyên tử/unique/FK và giao dịch.

- D-17: CookieSerializer khai báo HttpOnly=true, SameSite=Lax, Secure ở prod; không dựa vào default servlet context của môi trường.
- D-18: Không dùng SavedRequestCache để tránh lưu URL token mời vào bảng phiên. P4 chỉ chuyển tiếp đường dẫn lời mời hợp lệ qua form đăng nhập, không lưu vào session/flash. Token phải được chuyển tới người nhận qua liên kết đúng luồng; cấm log và lưu rõ trong DB, không cấm chính phản hồi phân phối liên kết.
- D-19: Maven settings, user home trong test và Docker client config nằm trong .tools/repo; Docker auths rỗng, không dùng credentials Docker/Maven của người dùng.

- D-20: username public vẫn 3–30; deleted-<UUID> dài 44 nên V3 mở cột lên 44 và chỉ cho dạng dài này khi enabled=false. Giữ nguyên V1/V2 đã áp dụng. Đổi định danh không hứa xóa PII trong mô tả tự do/lịch sử.
- D-21: Không giao mới/giao lại cho tài khoản đã bị vô hiệu hóa; giữ tham chiếu cũ để đọc/lưu lịch sử.
- D-22: Dependency-check thực tế phát hiện Tomcat 11.0.24 có CVE High/Critical; khóa toàn bộ tomcat.version=11.0.27, bản ổn định mới nhất hiện có trong Maven Central. Nguồn vendor: https://tomcat.apache.org/security-11.html và https://tomcat.apache.org/whichversion.html. Không suppress finding; chạy lại SCA.

- D-23: Flyway runtime cảnh báo MariaDB11.8 mới hơn mức đã verified11.7. Giữ image11.8.9 đã pin vì migrations/validate/restart/backup-restore đã kiểm chứng trên đúng image; không coi đây là vendor certification. Ghi giới hạn trong BLOCKERS/DEPLOY, reviewer P7 cần đánh giá trước môi trường thật. Nguồn upstream: https://github.com/flyway/flyway/issues/4247 và https://documentation.red-gate.com/flyway/reference/database-driver-reference/mariadb . Không suppress cảnh báo.
- D-24: Logout hủy mọi session theo principal để tuân SEC-06 chặt hơn, không chỉ session hiện tại. Test RED p6-logout-red rồi GREEN p6-final-functional. Backup kiểm tra compound FK theo cả tuple và từ chối thiếu/thừa bảng; retention chỉ tính bộ backup đủ dump/hash/manifest.
