# Phần AI viết — sửa phát hiện P7

Phạm vi do chủ dự án yêu cầu: sửa đúng năm phát hiện review, test FAIL trước sửa, PASS sau sửa và kiểm tra đầy đủ. Chưa tính các phần này là kỹ năng người học.

| Tệp | Mô tả | Trạng thái |
|---|---|---|
| src/test/java/vn/deadline/tracker/BackupFailureTest.java | Test ép lỗi truy vấn backup/restore trên MariaDB tạm, kiểm tra mã thoát và giữ bản sao trước | chưa đọc |
| scripts/backup.sh, scripts/verify-restore.sh | Truy vấn metadata kiểm tra mã thoát; manifest chỉ công bố khi đủ; không retention khi backup lỗi | chưa đọc |
| src/test/java/vn/deadline/tracker/RestoredSessionTest.java | Backup trước logout, phục hồi DB riêng và gửi cookie cũ qua HTTP thật | chưa đọc |
| scripts/verify-restore.sh (P7-02), docs/OPS.md | Hủy phiên sau đối chiếu snapshot; quy trình cách ly và đối chiếu reset/vô hiệu hóa sau snapshot | chưa đọc |
| scripts/checksum.lib.sh; scripts/backup.sh (P7-02); BackupFailureTest.java; RestoredSessionTest.java | SHA-256 logic theo cột/hàng đã sắp; test metadata lỗi, dữ liệu bị đổi và fixture Owner/Member | chưa đọc |
