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
