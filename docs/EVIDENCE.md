# Bằng chứng kiểm chứng W1

Ngày khởi tạo: 17/10/2026. Chỉ cập nhật khi đã chạy thật. Log local trong target/; kết quả có thể tái hiện bằng lệnh và tên test.

| Cổng | Trạng thái | Lệnh/test | Ngày | Kết quả |
|---|---|---|---|---|
| UAT-01 | đạt | bash scripts/check.sh; P4 | 17/10/2026 | AuthWebTest sessionLifecycle; simulated accounts, not pilot |
| UAT-02 | đạt | bash scripts/check.sh; P4 | 17/10/2026 | GroupInvitationWebTest creates group then four distinct accounts join: 5 members; simulated accounts, not pilot |
| UAT-03 | đạt | bash scripts/check.sh; P4 | 17/10/2026 | InvitationTest expires/revokes/used token and two concurrent service joins; simulated accounts, not pilot |
| UAT-04 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.createAndValidation: real web create, invalid fields preserve description, forged fields ignored; test data only |
| UAT-05 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.statusPermission: Owner/creator rejected, assigned user completes; test data only |
| UAT-06 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.editAndConflict and TaskDomainTest: reassign/reset/revoke; test data only |
| UAT-07 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.statusPermission + PersistenceTest: same status idempotent, completedAt, reopening; test data only |
| UAT-08 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskDomainTest boundary Clock and TaskWebTest filter; test data only |
| UAT-09 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | RestartPersistenceTest + TaskWebTest edits overdue task; test data only |
| UAT-10 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.everyEndpointIsolation: foreign owner and anonymous across GET/POST routes, including malformed params; test data only |
| UAT-11 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.editAndConflict preserves stale form, returns 409; DB concurrent writer test; test data only |
| UAT-12 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.filtersAndPastDue totals independent of filters; zero/50% domain test; test data only |
| UAT-13 | đạt | bash scripts/check.sh; AuthWebTest.sessionLifecycle + OpsWorkflowTest.reset + ProductionSecurityTest.unknownError | 17/10/2026 | Đổi/reset/logout hủy phiên; force-change; healthy log scan và captured exception message không lộ marker; dữ liệu thử cục bộ |
| UAT-14 | đạt | bash scripts/check.sh; BackupRestoreTest.restoreAndGuards | 17/10/2026 | Dump/restore thật, đủ bảng/count/checksum/compound FK; app context trên DB phục hồi owner đọc được và outsider404; guard/retention/file600; dữ liệu thử cục bộ |
| UAT-15 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | AuthWebTest CSRF missing/invalid; GroupInvitationWebTest join POST; test data only |
| UAT-16 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.xssAndHeaders; test data only |
| UAT-17 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | AuthWebTest.rateLimit + RateLimitTest IP/username and expiry; test data only |
| UAT-18 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.filtersAndPastDue reassignment preserves past deadline; test data only |
| SEC-01 | đạt | bash scripts/check.sh; AuthWebTest.mappingInventory | 17/10/2026 | Duyệt toàn bộ current MVC mappings, mặc định từ chối anonymous; dữ liệu thử cục bộ |
| SEC-02 | đạt | bash scripts/check.sh; AuthWebTest + GroupInvitationWebTest + TaskWebTest.createAndValidation | 17/10/2026 | Actor từ SecurityContext; forged actorId/createdBy/role/status không ảnh hưởng; dữ liệu thử cục bộ |
| SEC-03 | đạt | bash scripts/check.sh; AuthWebTest.accessMatrix + TaskWebTest.everyEndpointIsolation + GroupInvitationWebTest | 17/10/2026 | Hai nhóm, owner/member/foreign/anonymous; GET/POST và malformed input chặn trước binding; permissions theo hành động; dữ liệu thử cục bộ |
| SEC-04 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | AuthWebTest.csrfProtection rejects missing/invalid token; valid registration succeeds; phạm vi build P3 |
| SEC-05 | đạt | bash scripts/check.sh; AuthWebTest.registrationRules + ProductionSecurityTest.unknownError + healthy-log literal scan | 17/10/2026 | Argon2/Unicode/password bounds; hash-only account, credentials erased; no sensitive exception marker; xem giới hạn log scan bên dưới; dữ liệu thử cục bộ |
| SEC-06 | đạt | bash scripts/check.sh; AuthWebTest.sessionLifecycle/fixationAndExpiry/rateLimit + OpsWorkflowTest + ProductionSecurityTest.prodHeaders | 17/10/2026 | Logout/change/reset hủy mọi phiên; rotation/expiry/force-change; Secure HttpOnly SameSite prod; rate limit Clock; dữ liệu thử cục bộ |
| SEC-07 | đạt | InvitationTest + GroupInvitationWebTest; bash scripts/check.sh | 17/10/2026 | 256-bit token, hash-only DB, exact expiry, atomic join, no-referrer, POST+CSRF, no raw token in session |
| SEC-08 | đạt | bash scripts/check.sh; P5 | 17/10/2026 | TaskWebTest.xssAndHeaders; group/task/detail/forms escape script payload; no unsafe-inline; source no th:utext |
| SEC-09 | đạt | bash scripts/check.sh; ProductionSecurityTest.prodHeaders + TaskWebTest.xssAndHeaders | 17/10/2026 | CSP/frame/no-referrer/nosniff; HSTS và Secure cookie qua secure request mô phỏng, chưa TLS Internet; dữ liệu thử cục bộ |
| SEC-10 | đạt | bash scripts/check.sh; scripts/check.sh + rà soát Repository/JdbcTemplate/InvitationStore | 17/10/2026 | Static scan cấm SQL concatenation; query bind params; DB scripts chỉ ghép identifier đã whitelist, không phải web input; dữ liệu thử cục bộ |
| SEC-11 | đạt | bash scripts/check.sh; ProductionSecurityTest.debugClosed/unknownError + OpsWorkflowTest.noWebOps | 17/10/2026 | Không actuator/debug endpoint; 500 có reference, không stacktrace/input message trong response/log; ops không HTTP; dữ liệu thử cục bộ |
| SEC-12 | đạt | bash scripts/check.sh; scripts/check.sh gitleaks | 17/10/2026 | gitleaks dir scan no leaks; .env/cache/tool/backup excluded; không đưa bí mật thật vào repo; dữ liệu thử cục bộ |
| SEC-13 | đạt | bash scripts/check.sh; scripts/check.sh OWASP12.1.8 | 17/10/2026 | Tomcat vá11.0.27 sau finding11.0.24; 54 dependency entries, 0 findings trong report mới; không suppress; OSS Index tắt vì cần credentials; dữ liệu thử cục bộ |
| SEC-14 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | Registration validation, server domain bounds, RequestSizeFilter rejects 17KB; phạm vi build P3 |
| DATA-01 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | Flyway V1/V2/V3, Hibernate validate; PersistenceTest.constraints; chạy lại toàn bộ ở P6 |
| DATA-02 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.constraints: FK/unique/owner/check rejects invalid writes; chạy lại toàn bộ ở P6 |
| DATA-03 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.groupRollback/eventAtomicity: forced rollback and event idempotency; chạy lại toàn bộ ở P6 |
| DATA-04 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.actualOptimisticLock/staleWrite: exactly one concurrent writer commits; chạy lại toàn bộ ở P6 |
| DATA-05 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.invitationClaimPrimitive: actual InvitationStore, one of two joins; chạy lại toàn bộ ở P6 |
| DATA-06 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | TaskDomainTest.deadlineBoundaries and MariaDB UTC persistence; chạy lại toàn bộ ở P6 |
| DATA-07 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | Source scan: no DeleteMapping/RequestMethod.DELETE/business delete calls in current production source; chạy lại toàn bộ ở P6 |
| DATA-08 | đạt | bash scripts/check.sh; BackupRestoreTest.restoreAndGuards | 17/10/2026 | Hai script chạy trong DB thử; restore/count/checksum/compound FK+app quyền; quyền600/retention/guard được assert; dữ liệu thử cục bộ |
| DATA-09 | đạt | bash scripts/check.sh; OpsWorkflowTest 4 tests | 17/10/2026 | Nonweb reset/anonymize, cần console/xác nhận, giữ references, invalidation/force-change, disabled assignee chặn; không HTTP; dữ liệu thử cục bộ |
| DATA-10 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | RestartPersistenceTest: close context, reopen, read same committed overdue task; chạy lại toàn bộ ở P6 |

## P0 — bằng chứng đã chạy

- Lệnh: Git Bash `bash scripts/check.sh` (JDK portable được script chọn).
- Kết quả 17/10/2026: BUILD SUCCESS; BuildSmokeTest 1 test, 0 lỗi; template/SQL static checks OK; gitleaks scanned ~126150 bytes, no leaks found; migration check OK.
- Dependency audit: BỎ QUA, chưa có kết quả SEC-13; sẽ chạy trước cổng P6. Không suy ra bất kỳ UAT chức năng nào từ smoke test build.

## P1 US-01/02 — Group

- RED: bash scripts/mvn.sh test -Dtest=GroupDomainTest; compiler báo package domain chưa có (target/p1-group-red.log). Lần thử sandbox trước đó lỗi Git Bash NtCreateDirectoryObject, không được tính là RED nghiệp vụ; đã chạy lại với quyền phù hợp.
- GREEN: bash scripts/check.sh; 9 tests PASS (8 case Group + 1 build), 0 lỗi; gitleaks/static checks OK (target/p1-group-green.log).
- Đã đối chiếu: nhóm có owner/member, chuẩn hóa/giới hạn tên và mô tả, nhóm cùng tên khác ID, không thêm trùng, vai trò theo nhóm, chặn rename bởi member/outsider, snapshot thành viên không sửa từ ngoài.
- Chưa phải UAT web/DB: US-01/02 chỉ qua phần miền Java thuần; UAT-02 vẫn chưa chạy end-to-end.

## P1 US-03…06 — Task và deadline

- RED: bash scripts/mvn.sh test -Dtest=TaskDomainTest; compiler báo thiếu Task/TaskStatus/TaskFilter/Progress (target/p1-task-red.log).
- GREEN: bash scripts/check.sh; 19 tests PASS, 0 lỗi (target/p1-task-green.log), gitleaks/static checks OK.
- Đối chiếu US-03: tạo TODO, tên/mô tả và giới hạn, người tạo/người được giao trong nhóm, hạn null/bằng/quá khứ bị từ chối.
- US-04: lọc kết hợp, quá hạn strict >, sắp đến hạn gồm đúng 24h, DONE không tính quá hạn, tỷ lệ 0/50% đúng.
- US-05: chỉ assignee, chặn owner/creator/member/outsider khác, đủ chuyển trạng thái và gửi lại không thay đổi, completedAt đúng.
- US-06: quyền sửa, giữ trạng thái khi cùng assignee, giao lại reset/revoke, DONE hạn chế sửa, nạp/sửa/giao lại việc quá hạn, so hạn cùng phút giữ thời điểm đã lưu.
- Cổng P1 miền thuần đã qua; các UAT web/DB vẫn chưa coi là đạt chỉ từ domain tests.

## P2 US-07/08 — bằng chứng

- RED: target/p2-red.log báo thiếu persistence/WorkService.
- GREEN cuối: bash scripts/check.sh, target/p2-final.log: 27 tests, 0 failure/error/skip, BUILD SUCCESS; gitleaks/static checks OK.
- MariaDB thực tế 11.8.9, image digest sha256:f16d7a344413e519dff518f27bdb221388f84b0d274c97c4fb5f2cb0dc1b34fa. Testcontainers 2.0.5; JDK 25 portable.
- DATA-05 kiểm tra primitive store trong DB thật; lời mời token/expiry/ủy quyền web sẽ kiểm chứng P4.
- DATA-07 là scan source build hiện tại, không phải bằng chứng rằng giao diện W1 đã hoàn thành.
- Cổng backup/ops và toàn bộ UAT web còn chưa chạy; SEC-13 audit đang tải NVD, chưa có kết quả.

## P3 US-09…11 — tài khoản và quyền

- RED: target/p3-red.log thiếu AccountService/security.
- GREEN cuối target/p3-gates-final.log: 36 tests, 0 failures/errors/skips; check/gitleaks/static checks OK.
- Test bổ sung đã phát hiện và sửa cookie thiếu HttpOnly/SameSite trong mock servlet context; CookieSerializer khai báo rõ, prod Secure.
- Expiration test dùng LAST_ACCESS_TIME và EXPIRY_TIME đúng cơ chế Spring Session; kiểm tra cookie cũ không vào account được.
- Chưa có toàn bộ routes W1; SEC mapping/object matrix được chạy lại sau P5. SEC-13 còn chờ NVD scan.

## P4 US-12…14 — nhóm và lời mời

- RED service: target/p4-red.log thiếu InvitationService; RED view target/p4-web-red.log thiếu invitation template và phát hiện rollback-only do bắt exception membership trong transaction. Đã thay bằng truy vấn membership không ném exception.
- GREEN cuối target/p4-final.log: 42 tests PASS, không error/failure/skip; gitleaks/check OK.
- 4 service tests lời mời: hash/token, quyền, join đã-là-thành-viên không tiêu thụ, đã dùng từ chối, mốc 24h, thu hồi, hai người đồng thời.
- Web flow đủ 5 thành viên, giả owner/role không hiệu lực; token không trong SPRING_SESSION_ATTRIBUTES; redirect chỉ chấp nhận đường dẫn lời mời nội bộ hợp lệ.

## P5 US-15…18 — giao diện và quyền

- RED target/p5-red.log: task routes/forms chưa có. GREEN target/p5-green.log: 48 tests PASS, gitleaks/static checks OK.
- Tạo/sửa/detail/status, giữ form lỗi và stale input 409; filter/progress; XSS escaped; foreign/anonymous bị chặn trước binding, kể cả malformed status.
- Giao diện responsive CSS cục bộ, không inline JS/style; chưa tuyên bố NFR-05 đã quan sát bằng browser.
- Dependency audit thực tế đã chạy, báo Tomcat 11.0.24 High/Critical. Đây là RED SEC-13 cần sửa P6, không bị suppress hoặc coi đạt.

## P6 — cổng đã chạy trên máy phát triển

- `bash scripts/check.sh`, log local target/p6-verified.log: **58 tests, 0 failures/errors/skips**; static/gitleaks/audit/migration OK. Tóm tắt lưu [tests-local](verification/tests-local.txt).
- Ops RED target/p6-ops-red.log trước implementation; backup RED target/p6-backup-red.log thiếu scripts; logout-all RED target/p6-logout-red.log. Production error capture bổ sung sau implementation là kiểm chứng hồi quy, không ghi là RED giả.
- Khi mở rộng kiểm tra app sau restore, fixture GRANT thiếu MYSQL_PWD bị từ chối; sửa môi trường tiến trình con rồi chạy lại toàn bộ thành công. Không tính lần thất bại là đạt.
- Backup/restore thật [restore-example](verification/restore-example.txt), bao gồm app đọc DB vừa phục hồi và từ chối outsider. Backup ở /tmp/deadline-backups trong container, ngoài project. Không restore đè nguồn.
- Scan successful log so với 11 literal passphrase giả: không khớp; ProductionSecurityTest dùng captured output để kiểm tra exception chứa marker nhạy cảm không bị log. Không suy ra mọi tình huống log tương lai đều không có bí mật.
- OWASP report mới không findings; [summary](verification/dependency-audit.json) có thời điểm/hash raw report. NVD tải lần đầu khoảng35phút; Tomcat11.0.24 đã bị báo High/Critical, vá11.0.27 rồi audit lại. Không quét image/OS bằng SCA Java này.
- [Đo tải](verification/load-local.txt):100 requests,75view/25save,5threads,3groups×5users×200tasks; JVM ổn định, MockMvc+MariaDB trên Windows CPU12/Docker memory11,8GB. Không có mạng/TLS/browser timing; không SLA.
- Browser IAB thật: login fake account, group overview, create task bằng Enter, Tab tới description, change status bằng Enter, DONE filter còn1kết quả nhưng overview3task/33%; 360px document/client/scroll đều360, không input thiếu label trên dashboard; console không warn/error. [Desktop](verification/group-desktop.png), [360px](verification/group-360.png), [detail](verification/task-360.png). Không phải kiểm toán accessibility toàn diện.
- Compose thật [smoke](verification/compose-local.txt): packaged jar HTTP register/login/create group, restart còn group+session, UID10001; loopbackports. Chỉ dừng/gỡ container/network project thử, giữ volume trial được ghi trong report.
- Flyway vẫn cảnh báo DB11.8 mới hơn mức verified11.7; chức năng trên pinned11.8.9 đã test, không coi là chứng nhận vendor. Xem D23/BLOCKERS.
- Chưa chạy clean-clone check tại mốc ghi này; chưa deploy HTTPS thật, chưa lịch backup thật, chưa pilot/P7.

### Giới hạn diễn giải SEC-05 (D25)

Kết quả đạt trong bảng là kiểm chứng password thô/invitation raw token không lưu DB, principal credential bị erase và không log bí mật ở các luồng đã thử. Spring Session JDBC vẫn lưu session ID/CSRF/attributes và trả session cookie cho browser; token lời mời phải xuất trong phản hồi phân phối cho Owner. **Chưa/không tuyên bố đạt nghĩa đen cấm mọi token/session ID trong mọi phản hồi và DB**, vì mâu thuẫn công nghệ/luồng được yêu cầu. Không tự triển khai session thay framework. Reviewer P7 cần xác nhận diễn giải này; không coi đây là gate bị âm thầm bỏ qua.

- Chốt US21: target/p6-backup-story.log check58tests/0failure/error/skip, SCA/gitleaks/migration OK. Restore artifact có UTC bắt đầu và elapsed milliseconds thực đo (không gồm app boot); không suy RTO môi trường thật.
