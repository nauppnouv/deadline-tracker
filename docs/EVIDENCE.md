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
| SEC-01 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-02 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-03 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-04 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-05 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-06 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-07 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-08 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-09 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-10 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-11 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-12 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-13 | chưa chạy | — | — | Chưa có bằng chứng |
| SEC-14 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-01 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-02 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-03 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-04 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-05 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-06 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-07 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-08 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-09 | chưa chạy | — | — | Chưa có bằng chứng |
| DATA-10 | chưa chạy | — | — | Chưa có bằng chứng |

## P0 — bằng chứng đã chạy

- Lệnh: Git Bash `bash scripts/check.sh` (JDK portable được script chọn).
- Kết quả 17/10/2026: BUILD SUCCESS; BuildSmokeTest 1 test, 0 lỗi; template/SQL static checks OK; gitleaks scanned ~126150 bytes, no leaks found; migration check OK.
- Dependency audit: BỎ QUA, chưa có kết quả SEC-13; sẽ chạy trước cổng P6. Không suy ra bất kỳ UAT chức năng nào từ smoke test build.

## P1 US-01/02 — Group

- RED: bash scripts/mvn.sh test -Dtest=GroupDomainTest; compiler báo package domain chưa có (target/p1-group-red.log). Lần thử sandbox trước đó lỗi Git Bash NtCreateDirectoryObject, không được tính là RED nghiệp vụ; đã chạy lại với quyền phù hợp.
- GREEN: bash scripts/check.sh; 9 tests PASS (8 case Group + 1 build), 0 lỗi; gitleaks/static checks OK (target/p1-group-green.log).
- Đã đối chiếu: nhóm có owner/member, chuẩn hóa/giới hạn tên và mô tả, nhóm cùng tên khác ID, không thêm trùng, vai trò theo nhóm, chặn rename bởi member/outsider, snapshot thành viên không sửa từ ngoài.
- Chưa phải UAT web/DB: US-01/02 chỉ qua phần miền Java thuần; UAT-02 vẫn chưa chạy end-to-end.
