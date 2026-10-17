# Bằng chứng kiểm chứng W1

Ngày khởi tạo: 17/10/2026. Chỉ cập nhật khi đã chạy thật. Log local trong target/; kết quả có thể tái hiện bằng lệnh và tên test.

| Cổng | Trạng thái | Lệnh/test | Ngày | Kết quả |
|---|---|---|---|---|
| UAT-01 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-02 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-03 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-04 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-05 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-06 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-07 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-08 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-09 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-10 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-11 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-12 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-13 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-14 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-15 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-16 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-17 | chưa chạy | — | — | Chưa có bằng chứng |
| UAT-18 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-01 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | AuthWebTest.mappingInventory enumerates current MVC routes; re-run after new routes; phạm vi build P3 |
| SEC-02 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | Explicit auth form parameters ignore forged actorId/role; task create binding rechecked P5; phạm vi build P3 |
| SEC-03 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | AuthWebTest.accessMatrix: Owner A, Member A, Owner B, anonymous against protected group probe; phạm vi build P3 |
| SEC-04 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | AuthWebTest.csrfProtection rejects missing/invalid token; valid registration succeeds; phạm vi build P3 |
| SEC-05 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | AuthWebTest.registrationRules: Argon2, lengths, Unicode; successful-run log scan no plain test password; phạm vi build P3 |
| SEC-06 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | AuthWebTest.sessionLifecycle/fixationAndExpiry/rateLimit: JDBC cookies, rotation, expiration, logout/change; RateLimitTest injected Clock; phạm vi build P3 |
| SEC-07 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-08 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-09 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-10 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-11 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-12 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-13 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-14 | đạt | bash scripts/check.sh; AuthWebTest/RateLimitTest | 17/10/2026 | Registration validation, server domain bounds, RequestSizeFilter rejects 17KB; phạm vi build P3 |
| DATA-01 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | Flyway V1/V2, Hibernate validate; PersistenceTest.constraints; phạm vi build P2, kiểm tra lại sau web |
| DATA-02 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.constraints: FK/unique/owner/check rejects invalid writes; phạm vi build P2, kiểm tra lại sau web |
| DATA-03 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.groupRollback/eventAtomicity: forced rollback and event idempotency; phạm vi build P2, kiểm tra lại sau web |
| DATA-04 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.actualOptimisticLock/staleWrite: exactly one concurrent writer commits; phạm vi build P2, kiểm tra lại sau web |
| DATA-05 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | PersistenceTest.invitationClaimPrimitive: actual InvitationStore, one of two joins; phạm vi build P2, kiểm tra lại sau web |
| DATA-06 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | TaskDomainTest.deadlineBoundaries and MariaDB UTC persistence; phạm vi build P2, kiểm tra lại sau web |
| DATA-07 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | Source scan: no DeleteMapping/RequestMethod.DELETE/business delete calls in current production source; phạm vi build P2, kiểm tra lại sau web |
| DATA-08 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-09 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-10 | đạt | bash scripts/check.sh / P2 | 17/10/2026 | RestartPersistenceTest: close context, reopen, read same committed overdue task; phạm vi build P2, kiểm tra lại sau web |

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
