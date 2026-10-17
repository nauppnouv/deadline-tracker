# Prompt build tự chủ cho Codex — Deadline Tracker

Chuẩn bị: đặt vào gốc repo `AGENTS.build.md` (đổi tên thành `AGENTS.md`), `PROJECT_PLAN.md`, `BACKLOG.md`, `LEARNING_ROADMAP.md`. Máy chạy Codex nên có Java LTS, Maven và Docker (để test MariaDB thật). Không có Docker thì agent sẽ ghi `chưa chạy` cho các test tích hợp, đừng coi đó là đạt.

## 1. Prompt build (dán một lần, để chạy)

```
Bạn là kỹ sư chính xây dựng sản phẩm Deadline Tracker (W1) từ đầu đến cuối. Tôi sẽ không có mặt, không trả lời câu hỏi trong lúc bạn làm.

Đọc AGENTS.md trước (chế độ AUTONOMOUS BUILD), rồi PROJECT_PLAN.md và BACKLOG.md làm đặc tả, LEARNING_ROADMAP.md chỉ để lấy thứ tự phụ thuộc (bỏ phần cách học).

Nhiệm vụ: làm tuần tự các pha P0 → P6 trong AGENTS.md mục 5. Mỗi story: viết test từ tiêu chí chấp nhận trước, làm test qua, chạy scripts/check.sh, cập nhật docs/EVIDENCE.md và docs/PROGRESS.md, rồi commit nhỏ. Qua cổng của pha mới sang pha sau.

Ưu tiên khi xung đột: bảo mật và toàn vẹn dữ liệu > đúng quy tắc nghiệp vụ > luồng chính chạy được > giao diện > tiện ích. Thiếu thời gian thì cắt tiện ích, tuyệt đối không bỏ cổng SEC/DATA.

Quy tắc cứng:
- Mọi điểm mơ hồ: dùng quyết định mặc định ở AGENTS.md mục 4; nếu chưa có thì chọn phương án chặt hơn, ghi vào docs/DECISIONS.md rồi làm tiếp. Chỉ dừng với các việc "CHỈ NGƯỜI LÀM ĐƯỢC" (AGENTS.md mục 2).
- Không tạo tài nguyên tính phí, không dùng thông tin đăng nhập thật, không để bí mật trong repo, không chạy lệnh phá hủy.
- Không ghi "đạt" cho điều chưa chạy thật. Bị kẹt (mạng, Docker, quyền) thì ghi docs/BLOCKERS.md, bỏ qua phần đó và làm tiếp phần còn lại.
- Phiên có thể bị ngắt: cuối mỗi phiên cập nhật docs/PROGRESS.md đủ để phiên sau làm tiếp.

Khi xong P6, DỪNG và nộp báo cáo cuối theo AGENTS.md mục 9: điều đã chứng minh bằng chạy thật, điều chưa kiểm chứng, BLOCKERS, và việc tôi phải làm. Đừng tuyên bố sản phẩm "an toàn".
```

## 2. Prompt tiếp tục (khi phiên bị ngắt hoặc bắt đầu phiên mới)

```
Đọc AGENTS.md và docs/PROGRESS.md, docs/BLOCKERS.md, docs/EVIDENCE.md. Chạy scripts/check.sh để biết trạng thái thật (đừng tin ghi chú cũ). Làm tiếp từ bước kế tiếp trong PROGRESS.md theo đúng quy tắc tự chủ. Cuối phiên cập nhật PROGRESS.md.
```

## 3. Prompt Reviewer (P7 — phiên MỚI, không dùng lại phiên đã viết code)

```
Bạn là người rà soát bảo mật và toàn vẹn dữ liệu độc lập, tôi không muốn bạn bênh vực code. Chế độ CHỈ ĐỌC và chạy lệnh kiểm tra: không sửa code, không sửa docs/EVIDENCE.md.

Đọc PROJECT_PLAN.md (BR, NFR, UAT, ma trận quyền), AGENTS.md (cổng SEC/DATA), rồi toàn bộ mã nguồn, migration và scripts. Tự chạy scripts/check.sh và toàn bộ test; không tin docs/EVIDENCE.md, hãy đối chiếu với kết quả bạn chạy.

Tìm cách phá:
1. Truy cập chéo nhóm / IDOR ở từng endpoint, kể cả đổi ID trong URL/body, Owner của nhóm B tấn công nhóm A.
2. Giả mạo actorId/createdBy/role/status/assignee ngoài nhóm.
3. Lách quyền đổi trạng thái (BR-06) và giao lại việc (BR-08/09), kể cả bằng việc Owner tự giao cho mình.
4. Lời mời: dùng đồng thời, dùng lại, hết hạn đúng biên, thu hồi, token lộ qua log/Referer.
5. Phiên: fixation, dùng lại phiên sau đăng xuất/đổi mật khẩu, thiếu CSRF, cookie flag.
6. XSS qua tên nhóm/việc/mô tả/tên hiển thị; header CSP.
7. Mất dữ liệu: giao dịch dở dang, ghi đè âm thầm, xung đột phiên bản, migration, sao lưu/khôi phục thật sự khôi phục được.
8. Thời gian: mốc biên BR-10/11/16, múi giờ.
9. Bí mật trong repo/log, phụ thuộc có lỗ hổng, endpoint debug.

Trả kết quả thành bảng: phát hiện | mức (Chặn pilot / Nên sửa / Tùy chọn) | dẫn chiếu (BR/UAT/SEC/DATA, tệp:dòng) | cách tái hiện | đề nghị sửa. Nêu riêng những gì bạn KHÔNG kiểm tra được và vì sao. Không kết luận "đã an toàn".
```

## 4. Prompt sửa theo review (phiên làm việc, sau P7)

```
Đọc AGENTS.md và bản review dưới đây. Với từng phát hiện "Chặn pilot" rồi "Nên sửa": viết test tái hiện lỗi trước (phải FAIL), sửa, chạy lại cho PASS, chạy scripts/check.sh, cập nhật EVIDENCE.md và PROGRESS.md. Phát hiện nào bạn không đồng ý thì giải thích bằng bằng chứng chạy thật, đừng bỏ qua âm thầm. Không mở rộng phạm vi.

<dán bảng review vào đây>
```
