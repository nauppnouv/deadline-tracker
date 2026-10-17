# Phần AI viết — sửa phát hiện P7

Phạm vi do chủ dự án yêu cầu: sửa đúng năm phát hiện review, test FAIL trước sửa, PASS sau sửa và kiểm tra đầy đủ. Chưa tính các phần này là kỹ năng người học.

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| src/test/java/vn/deadline/tracker/BackupFailureTest.java | Test ép lỗi truy vấn backup/restore trên MariaDB tạm, kiểm tra mã thoát và giữ bản sao trước | chưa đọc |
| scripts/backup.sh, scripts/verify-restore.sh | Truy vấn metadata kiểm tra mã thoát; manifest chỉ công bố khi đủ; không retention khi backup lỗi | chưa đọc |
