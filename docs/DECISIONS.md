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

- D-25: SEC05 được kiểm chứng theo loại credential: mật khẩu chỉ Argon2 trong app_user, invitation chỉ SHA256, không log password/raw-token/session cookie; raw invite chỉ phản hồi phân phối D18, mật khẩu tạm chỉ ops console. Spring Session JDBC bắt buộc lưu session ID/CSRF/session attributes và Set-Cookie phải trả ID cho browser để hoạt động. Không diễn giải câu cấm mọi ID trong DB/phản hồi thành chứng minh đã đạt; lưu rõ giới hạn này ở EVIDENCE, không tự viết session/mật mã để giấu mâu thuẫn.

- D-26 (P7-02): CHECKSUM TABLE trên fixture group_member trả khác dù 16 hàng/cột nguồn và restore giống nhau (target/p7-02-checksum-diagnostic.txt). Dùng SHA-256 logic xác định theo cột/hàng đã sắp cho backup mới; không bỏ checksum/số hàng/FK, vẫn kiểm manifest số cũ theo thuật toán cũ. Test dữ liệu bị đổi nhưng cùng row count và SHA file mới phải bị từ chối. Xóa toàn bộ phiên sau khi đối chiếu snapshot; giữ DB cách ly để đối chiếu các quyết định credential/quyền sau snapshot theo OPS.

- D-27 (P7-05, thay D-11): FK owner_id -> app_user chỉ chứng minh tài khoản tồn tại. V4 từ chối dữ liệu cũ thiếu/sai Owner; AFTER INSERT nhóm tạo membership Owner cùng statement/giao dịch InnoDB; trigger chặn thay identity/Owner, xóa/dời/hạ role Owner và role không khớp owner_id. Unique owner_marker tiếp tục bảo đảm tối đa một. Không thêm tính năng chuyển Owner/rời nhóm. Theo [MariaDB Trigger Overview](https://mariadb.com/docs/server/server-usage/triggers-events/triggers/trigger-overview), trigger InnoDB thuộc giao dịch của statement gọi nó. Không sửa V1/V2/V3.

## Quyết định triển khai review vòng 2 (18/10/2026, theo phạm vi chủ dự án giao)

- D-28 / R2-01: giữ nguyên V1–V4. Hợp đồng schema dùng chung trong scripts và jar; startup prod/test kiểm tên/bảng/timing/event năm trigger V4, FK theo tuple/rule, unique theo cột, CHECK theo tên/có clause, NOT NULL/InnoDB và cột sinh. Đây là inventory, không chứng minh toàn bộ nội dung mọi trigger/CHECK chống sửa có chủ đích. Restore bổ sung probe hành vi tạo Owner/hạ Owner transaction rollback trước RESTORE_OK. Runtime principal cần nhìn metadata TRIGGER; dump giữ DEFINER cần quyền trên schema phục hồi; không tự cấp quyền. OPS ghi GRANT SELECT/SHOW VIEW/TRIGGER trên schema và SELECT mysql.proc cho backup MariaDB đã pin.
- D-29 / R2-03: so migration với MIGRATION_BASELINE_REF, mặc định origin/main; ref/Git lỗi thì FAIL, none chỉ explicit và in BỎ QUA. Clone cuối chọn ref0ed3330 đã chứa V1–V4 để bảo vệ đủ schema vòng này. Thêm mới được phép; sửa/xóa/đổi tên file baseline bị chặn cả khi đã commit.
- D-30 / prod: preflight sau nạp config trước context/DB/listener, kiểm DB/root/effective datasource secret và loopback/forwarded native; application-prod default bind loopback vẫn yêu cầu SERVER_ADDRESS tường minh. Không đổi framework/mật mã/version. DEPLOY chỉ một EnvironmentFile forwarded block, nháy đơn bảo toàn regex; chưa xác minh systemd/Caddy/Internet thật.
- D-31 / phục hồi: mọi credential snapshot không tin cậy. Script chỉ thông báo, OPS yêu cầu xác minh ID/quyền rồi reset từng tài khoản còn được phép bằng ops trước mở web; giữ disabled. Không thêm cập nhật hash/enabled/must_change_password trong restore.
- D-32 / retention R2-01: không dựa mtime để giữ bộ mới; giữ bộ mới đã kiểm trước danh sách cũ. Ca đồng thời thật chứng minh hai lượt có thể xóa chéo còn0; khóa mkdir trên BACKUP_DIR từ chối chồng lấn trước tạo file và gỡ qua EXIT trap. Crash/SIGKILL còn khóa thì người vận hành kiểm tiến trình/bộ dở trước gỡ khóa rỗng; không tự phá khóa.

## D-33 — sửa khoảng hở định nghĩa D-28 (18–19/10/2026)

- Chủ dự án chọn sửa, không chấp nhận rủi ro D-28. Giữ D-28 cũ làm lịch sử. Hợp đồng dùng chung `schema-inventory.sql` / `schema-required.txt` bổ sung CHECKDEF (bảng, tên, SHA-256 CHECK_CLAUSE) và GENERATEDDEF (bảng, cột, COLUMN_TYPE, EXTRA, SHA-256 GENERATION_EXPRESSION). Giữ dòng CHECK/GENERATED cũ. Cùng hợp đồng được dùng ở startup prod/test, trước backup và sau import restore; startup chỉ đọc metadata.
- Chuẩn tin cậy được dẫn xuất từ schema mới migrate V1→V4 trên `mariadb@sha256:f16d7a344413e519dff518f27bdb221388f84b0d274c97c4fb5f2cb0dc1b34fa`, không từ dump có thể bị sửa hay biểu thức V1 riêng lẻ. MariaDB thực tế `11.8.9-MariaDB-ubu2404`: 10 CHECK, 1 cột sinh; owner_marker là `int(11)` / `VIRTUAL GENERATED`. `ck_username` lấy sau V3. Raw metadata và biên bản: `target/d28-metadata.txt`, `docs/verification/d28-remediation.txt`.
- Chọn chặt hơn inventory một chiều: hai nơi đọc hợp đồng từ chối cả thiếu/sai định nghĩa và CHECK/cột sinh thừa chưa được đưa vào hợp đồng. Vì vậy cần sửa cả consumer Java và shell; chỉ bổ sung hai file hợp đồng sẽ chưa đáp ứng fail closed đối với đối tượng mới. SQL vẫn là một SELECT/UNION ALL; các nhánh định nghĩa dùng collation utf8mb4_bin tường minh. Thông báo nêu tên đối tượng, không in biểu thức hay secret.
- Hash so byte biểu thức MariaDB đã chuẩn hóa, không chứng minh tương đương SQL. Server khác phiên bản có thể fail closed; không nới so sánh để vượt lỗi. Migration sau phải cập nhật hợp đồng qua review riêng. Không sửa V1–V4, không thêm V5, không đổi image/Flyway/JDK/dependency.
- Giới hạn còn lại ngoài D-33: kiểu/độ dài/default cột thường, index thường `ix_task_group_due` / `ix_event_task`, body trigger ở startup (body chỉ so khi restore). Owner còn phòng thủ chồng bằng trigger V4 và uq_group_member; không coi checksum dữ liệu tình cờ lệch là kiểm biểu thức cột sinh. Giữ hash body năm trigger, probe Owner rollback, reset phiên sau so snapshot, retention và lock. Không thêm probe INSERT CHECK tùy chọn: kiểm định nghĩa dùng chung là lớp chặn của phạm vi này.
- Chủ dự án đã xác nhận trong phiên: restore lỗi giữ DB thử mới đã import để điều tra; DB nguồn và đích có sẵn không đổi. Không tự drop DB lỗi, không staging/pilot/triển khai. Test riêng kiểm nguồn/bộ backup tốt không đổi và từ chối overwrite đích có sentinel.
- Chưa có review độc lập cho commit D-33. Bằng chứng RED/GREEN/mutation/full check được ghi riêng; không tự ký Gate A hay sửa dòng UAT/SEC/DATA lịch sử. SEC-05/D-25 chỉ đề xuất ngoại lệ câu chữ cho chủ dự án duyệt, không sửa AGENTS.md/PROJECT_PLAN.md.

## D-34 — làm rõ câu chữ cổng SEC-05 theo D-25 (19/10/2026)

- Vấn đề: review độc lập 18/10/2026 chỉ ra câu cũ có thể cấm cả session ID trong DB/phản hồi, trong khi Spring Session JDBC phải lưu phiên và browser cần cookie. D-25 là diễn giải đã ghi, không phải bằng chứng đạt câu cấm theo nghĩa đen. Chủ dự án đã duyệt câu chữ dưới đây; chỉ sửa tài liệu, không tự viết lại session/mật mã hay thay đổi hành vi ứng dụng.
- Đối chiếu trước sửa: `AGENTS.md` §6 có định nghĩa SEC-05 cũ; `docs/PROJECT_PLAN.md` chưa có mục SEC-05, chỉ có BR-24/NFR-03 liên quan. Thay câu cũ ở AGENTS và bổ sung cùng câu đã duyệt tại PROJECT_PLAN §10 để đồng bộ nguồn đặc tả. D-09/D-25/D-28/D-33 giữ nguyên làm lịch sử. Chọn chặt hơn khi đọc các giới hạn: mọi ngoại lệ chỉ trong kênh được nêu, không có ngoại lệ cho log; không mở rộng phạm vi phân phối token.

Câu chữ mới được chủ dự án duyệt:

> SEC-05 Mật khẩu theo BR-24, không strip, chỉ lưu bản băm. Mật khẩu, token và ID phiên không xuất hiện trong log (test quét log). DB không lưu mật khẩu hay token lời mời thô (chỉ hash). Ngoại lệ có chủ đích theo D-25: (a) Spring Session JDBC lưu ID phiên và thuộc tính phiên, gồm CSRF token, trong bảng SPRING_SESSION*; (b) cookie phiên và CSRF token do framework phát cho browser; (c) token lời mời thô chỉ xuất hiện một lần trong phản hồi phân phối cho Owner; (d) mật khẩu tạm chỉ hiện trên console ops, không qua web. Không ngoại lệ nào áp dụng cho log.

- Ảnh hưởng/công sức: cập nhật câu chữ, ghi chú giới hạn và hồ sơ tiến độ/AI; chạy lại toàn bộ check theo baseline được giao, không đổi source/test/migration/scripts/dependency/JDK/image. Bằng chứng test SEC-05 và dòng trạng thái lịch sử không đổi; không suy kết quả check thành đạt SEC-05 theo nghĩa đen.
- **câu chữ này chưa có review độc lập**. Reviewer cần đối chiếu câu chữ mới và bằng chứng trong phiên riêng. Gate A chưa qua; không mở Phase B, không push/merge/deploy. Quyết định này thay trạng thái chờ duyệt câu chữ được ghi ở cuối D-33, không sửa nội dung lịch sử D-33.

## D-35 — hợp đồng charset/collation F-01 (19/10/2026)

- Chọn từ chối schema có charset/collation khác chuẩn thay vì chỉ kiểm dữ liệu hiện có. Thêm TEXTDEF cho mọi cột văn bản trong 8 bảng nghiệp vụ/session và dùng chung lúc startup prod/test, trước backup, sau restore. Chuẩn lấy từ schema V1–V4 mới migrate trên MariaDB image đã pin; migration cũ giữ nguyên.
- Kiểm tra exact-set chặn khác biệt collation dù dữ liệu hiện tại và checksum chưa đổi, đồng thời chặn cột văn bản chưa có hợp đồng. Metadata khác chuẩn server có thể fail closed; khi đổi phiên bản phải dẫn xuất chuẩn mới từ schema tin cậy rồi review.
- Bằng chứng cục bộ ở `docs/verification/f01-remediation.txt`. Chưa có review độc lập; không ký Gate A.

## D-36 — hợp đồng kiểu và thuộc tính cột F-04 (19/10/2026)

- Bổ sung exact-set cho toàn bộ cột của 8 bảng nghiệp vụ/session: COLUMN_TYPE/DATA_TYPE, độ dài, numeric và datetime precision, NULL, default và EXTRA. Cùng contract dùng tại startup prod/test, trước backup và sau import restore.
- Metadata chuẩn được dẫn xuất độc lập từ schema V1–V4 mới migrate trên MariaDB image đã pin. Thay đổi độ dài/default vẫn bị từ chối dù không đổi dữ liệu; cột mới ngoài chuẩn cũng bị từ chối. Không sửa migration cũ hoặc dùng JPA validate làm chuẩn schema.
- Kết quả cục bộ trong `docs/verification/f04-remediation.txt`; chưa có review độc lập cho commit này, không ký Gate A.

## D-37 — exact body trigger V4 ở mọi cổng schema (19/10/2026)

- F-02 sửa khoảng hở đã ghi sau D-33: body trigger không còn chỉ được kiểm khi restore. Hợp đồng `TRIGGERDEF` trong `schema-required.txt` ghép tên/bảng/timing/event với SHA-256 chính xác của ACTION_STATEMENT, và `schema-inventory.sql`/consumer dùng chung tại startup prod/test, trước backup và sau restore import.
- Bỏ file hash riêng để tránh hai allowlist có thể lệch. Backup vẫn kiểm lệnh CREATE TRIGGER trong dump theo các trigger của hợp đồng chung. Restore no-op bị từ chối trước probe/checksum/session cleanup.
- Chuẩn lấy độc lập từ schema mới migrate V1–V4 trên MariaDB image đã pin. Không sửa migration/dependency/image/JDK. Full 195 test/26 lớp và các lệnh mục tiêu, hash trong `docs/verification/f02-remediation.txt`.
- Metadata MariaDB khác phiên bản có thể fail closed; không nới hash. Chưa có review độc lập, không ký Gate A và không coi đây là phê duyệt deploy.
## D-38 — làm rõ phạm vi xuất hiện raw invite theo luồng chia sẻ đã duyệt (19/10/2026)

- Chủ dự án duyệt giữ luồng chia sẻ hiện tại. D-34(c) được làm chính xác hơn: token thô có thể xuất hiện ở phản hồi Owner khi phát hành và phản hồi xem trước/form lặp lại cho người giữ liên kết hợp lệ. Đây là phản hồi bắt buộc để hiển thị/thực hiện liên kết hiện có, không mở thêm kênh chia sẻ.
- Tách rõ token xuất hiện lại khỏi quy tắc dùng một lần: tham gia thành công vẫn tối đa một lần theo BR-16; hết hạn/thu hồi và POST giữ nguyên. DB chỉ lưu hash; không cho ngoại lệ log, no-referrer giữ nguyên.
- Cập nhật câu SEC-05 đồng bộ ở AGENTS.md §6 và PROJECT_PLAN §10, thêm regression GET preview lặp. Không đổi mã ứng dụng/schema. `GroupInvitationWebTest` 2/0; biên bản ở `docs/verification/f03-remediation.txt`. Chưa có review độc lập; không kết luận SEC-05 đạt, Gate A chưa qua.
## D-39 — mô tả chính xác grant TRIGGER của MariaDB (19/10/2026)

- Sửa câu OPS cũ "không cấp DML/DDL/root": quyền `TRIGGER` được grant cho backup principal thật sự cho phép CREATE/DROP/SHOW CREATE TRIGGER trên bảng trong phạm vi đó. Giữ nguyên grants; không cấp thêm quyền.
- Tài liệu MariaDB: [GRANT](https://mariadb.com/docs/server/reference/sql-statements/account-management-sql-statements/grant), [CREATE TRIGGER](https://mariadb.com/docs/server/server-usage/triggers-events/triggers/create-trigger), [DROP TRIGGER](https://mariadb.com/docs/server/reference/sql-statements/data-definition/drop/drop-trigger). Test dùng schema/user giả lập trong container, kiểm tra thêm CREATE TABLE vẫn bị từ chối; không chạm DB vận hành.
- `TriggerPrivilegeTest` 1/0. Full check cuối trên working tree gồm đủ năm mục: 196 test/27 lớp, không failure/error/skip; log/hash trong `docs/verification/f06-remediation.txt`. Chưa có review độc lập, không suy thành Gate A/deploy.

## D-40 — bảo vệ riêng bộ full-restore-verified để sửa F-07 (19/10/2026)

- Theo EXECUTE của chủ dự án và phương án thứ hai trong REVIEW.md: giữ một bộ đã qua full restore tách khỏi N ứng viên. Backup vẫn tạo ứng viên; không tuyên bố kiểm artifact đầy đủ ngay lúc dump, không cấp thêm quyền restore DDL cho principal backup.
- Verify-restore giữ khóa chung, kiểm schema/Owner/data/FK/session rồi copy đúng input ba file và receipt. Công bố generation/current trước khi bỏ bộ protected cũ. Chưa có receipt thì hoãn mọi candidate retention; lỗi protected bytes/receipt fail closed. OPS ghi bootstrap, dung lượng, recovery và ranh giới trust.
- Không sửa V1–V4, không thay JDK/dependency/image. RED thực với no-op trigger và CHECK(1=1), GREEN và full check được ghi trong `docs/verification/f07-remediation.txt`. Đây là lựa chọn triển khai trong phạm vi sửa lỗi được giao, không là acceptance F-05/TRIGGER/Flyway/canonicalization hay ký Gate A; cần review độc lập bản sửa.

## D-41 — cleanup dựa trên current đã công bố để sửa F-08 (19/10/2026)

- Chủ dự án yêu cầu tiếp tục sửa với một agent. Review e628fbd phát hiện TERM sau rename current làm EXIT xóa generation đã công bố, khiến con trỏ treo. Giữ review cũ làm lịch sử; không sửa verdict hoặc hồ sơ đó.
- Không suy trạng thái filesystem từ biến verified_unpublished: cleanup chỉ bỏ generation khi current vắng hoặc con trỏ hợp lệ trỏ rõ tới generation khác. Current tham chiếu chính generation đó, sai kiểu/symlink/không đọc được/không xác định an toàn thì giữ lại để kiểm tra. Giữ nguyên real exit lỗi/ngắt và không báo thành công giả; không chặn signal để che lỗi.
- Regression trước/sau rename, rename thành công rồi trả lỗi và current read failure; đối chiếu bộ cũ, current mới, cleanup/lock và actual automatic backup/restore retry. OPS công khai generation dư sau ngắt và giới hạn fsync/trust/dung lượng. Không sửa V1–V4, grant, dependency/JDK/image; không tự chấp nhận residual hoặc ký Gate A. Biên bản `docs/verification/f08-remediation.txt`.
