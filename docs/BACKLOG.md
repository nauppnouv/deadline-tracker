# Backlog triển khai — Deadline Tracker

**Phiên bản:** 0.1 · **Ngày:** 17/10/2026  
**Căn cứ:** [Kế hoạch sản phẩm](PROJECT_PLAN.md) · [Lộ trình học](LEARNING_ROADMAP.md)

## Cập nhật sau build P6 (17/10/2026)

Bảng này cập nhật tình trạng; nhãn trong mô tả story bên dưới là snapshot đặc tả trước build. Không thay đổi tiêu chí chấp nhận.

| Story | Tình trạng hiện tại |
|---|---|
| US01…18 | Chức năng và kiểm thử cục bộ đã có; xem EVIDENCE cho phạm vi từng cổng |
| US19 | Hồi quy tự động, UAT01…18 trên dữ liệu giả; chưa review độc lập P7 |
| US20 | Một phần: compose local và DEPLOY runbook; chưa máy chủ HTTPS/người dùng truy cập độc lập |
| US21 | Một phần: scripts và restore drill thật trên DB thử; lịch hằng ngày/người vận hành/backup ngoài máy phải thiết lập khi deploy |
| US22 | Chưa: pilot/người dùng thật do chủ dự án tổ chức |

## Cách sử dụng

Một user story mô tả người cần tính năng, mục đích và kết quả phải đạt. Tiêu chí chấp nhận là điều kiện để biết tính năng hoạt động đúng, không phải danh sách tên phương thức cần viết.

Các story dưới đây đều phục vụ phạm vi đã đề xuất. Chúng chưa được ước lượng bằng vận tốc thực tế. Không làm theo số ID một cách máy móc: dùng mốc và phụ thuộc. Trạng thái phản ánh code đã đọc ngày 17/10/2026; tài liệu này không tự chứng minh story đã hoàn thành.

- **Must:** cần đạt trước khi mở pilot W1, trừ phần chỉ là bài học trung gian.
- **Should:** có thể giảm độ hoàn thiện nếu thiếu thời gian, nhưng phải giữ hành vi thiết yếu đã nêu.
- **Một phần:** có ví dụ Java, chưa đủ kiểm tra hoặc chưa có ở web.
- Mốc M0–M7 được định nghĩa trong lộ trình học.

## A. Nghiệp vụ và Java trên máy

### US-01 — Tạo nhóm có người sở hữu

**Nhu cầu:** Là người tổ chức nhóm, tôi muốn tạo nhóm để có nơi tập hợp thành viên và công việc.  
**Mốc:** M1 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** kiến thức class/constructor, mô hình User cơ bản.

**Tiêu chí chấp nhận:**

- Tạo nhóm với tên hợp lệ và mô tả tùy chọn; tên được chuẩn hóa theo BR-15.
- Tên `null`, rỗng, toàn khoảng trắng hoặc quá giới hạn bị từ chối; không có nhóm tạo dở dang.
- Người tạo có tư cách Owner và cũng được tính là thành viên.
- Hai nhóm cùng tên vẫn có định danh khác nhau.

**Bài đầu để người học tự viết:** class Group chỉ có tên, constructor và getter. Chưa cần hoàn thành cả story trong một lần.

### US-02 — Quản lý danh sách thành viên

**Nhu cầu:** Là thành viên, tôi muốn biết ai ở trong nhóm để giao việc đúng người.  
**Mốc:** M1 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-01, List/ArrayList, định danh User.

**Tiêu chí chấp nhận:**

- Danh sách thể hiện đúng thành viên và vai trò trong nhóm.
- Thêm lại cùng một user không tạo thành viên trùng; hai người cùng tên hiển thị vẫn là hai người khác nhau.
- Một user có thể ở hai nhóm với hai vai trò khác nhau.
- Hàm kiểm tra tư cách thành viên dùng định danh, không dùng tên hiển thị.
- Ở console, việc thêm người là dữ liệu giả lập để học; khi lên web chỉ lời mời hợp lệ mới thêm thành viên.

### US-03 — Tạo và giao công việc

**Nhu cầu:** Là thành viên, tôi muốn giao một đầu việc cho một người trong nhóm với hạn rõ ràng.  
**Mốc:** M2 · **Ưu tiên:** Must · **Trạng thái:** Một phần trong Task/Main · **Phụ thuộc:** US-01, US-02.

**Tiêu chí chấp nhận:**

- Với người tạo là thành viên, tên hợp lệ, người phụ trách thuộc nhóm và hạn ở tương lai, tạo được công việc `TODO`.
- Lưu đúng nhóm, người tạo và người phụ trách bằng định danh.
- Thiếu tên/người phụ trách/hạn, tên toàn khoảng trắng, người ngoài nhóm hoặc hạn không ở tương lai đều bị từ chối mà không thêm công việc.
- Mô tả để trống được; giới hạn tên/mô tả theo BR-01/BR-02.
- Người ngoài nhóm không thể tạo việc dù chỉ định đúng ID nhóm và một người trong nhóm.

### US-04 — Xem danh sách và tình trạng thời hạn

**Nhu cầu:** Là thành viên, tôi muốn tìm việc của mình và việc sắp trễ để biết cần xử lý gì.  
**Mốc:** M2 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-03, phép so sánh thời gian và danh sách.

**Tiêu chí chấp nhận:**

- Chỉ lấy việc trong đúng nhóm mà người xem tham gia.
- Lọc theo người phụ trách, trạng thái, sắp đến hạn hoặc quá hạn; các điều kiện kết hợp với nhau.
- Quá hạn/sắp đến hạn tính đúng BR-10/BR-11, không thêm giá trị mới vào enum TaskStatus.
- Việc DONE không nằm trong danh sách đang quá hạn dù hạn đã qua.
- Sắp theo hạn tăng dần, cùng hạn theo ID; trả danh sách rỗng đúng khi không có kết quả.
- Kiểm chứng được mốc ngay trước/bằng/ngay sau hạn bằng thời điểm thử cố định.

### US-05 — Đổi trạng thái của việc được giao

**Nhu cầu:** Là người được giao, tôi muốn cập nhật tiến độ để nhóm biết tình hình công việc.  
**Mốc:** M2 · **Ưu tiên:** Must · **Trạng thái:** Một phần, chưa có kiểm tra người thao tác · **Phụ thuộc:** US-02, US-03.

**Tiêu chí chấp nhận:**

- Người đang được giao được chuyển giữa ba trạng thái theo BR-07.
- Thành viên khác, người ngoài nhóm và Owner không được giao đều bị từ chối.
- DONE ghi thời điểm hoàn thành; mở lại xóa thời điểm hoàn thành hiện tại.
- Giá trị trạng thái không hợp lệ bị từ chối; gửi lại đúng trạng thái hiện có không tạo thay đổi giả.
- Thay đổi một công việc không làm đổi công việc khác.

### US-06 — Sửa thông tin và giao lại việc

**Nhu cầu:** Là người tạo việc hoặc chủ nhóm, tôi muốn điều chỉnh công việc khi kế hoạch thay đổi.  
**Mốc:** M2 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-03, US-05.

**Tiêu chí chấp nhận:**

- Quyền sửa đúng ma trận trong kế hoạch; không mặc định người được giao cũng là người được sửa hạn.
- Sửa tên/mô tả của việc đã quá hạn vẫn được; không bắt người dùng đổi hạn nếu họ không sửa trường hạn.
- Đổi hạn phải đưa ra hạn mới ở tương lai; đổi người phải chọn thành viên trong cùng nhóm.
- Giao lại sang người khác đặt trạng thái TODO; người cũ mất quyền đổi trạng thái.
- Việc tự đặt TODO là hệ quả của giao lại hợp lệ, không cho người giao quyền chọn một trạng thái tùy ý.
- Chọn lại cùng người không tự reset trạng thái.
- Với DONE, chỉ cho sửa tên/mô tả; muốn giao lại hoặc sửa hạn phải được người phụ trách mở lại trước.

## B. Lưu dữ liệu và tính nhất quán

### US-07 — Lưu và nạp lại dữ liệu

**Nhu cầu:** Là thành viên, tôi muốn dữ liệu còn nguyên khi ứng dụng tắt hoặc khởi động lại.  
**Mốc:** M3 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-01 đến US-06; SQL cơ bản.

**Tiêu chí chấp nhận:**

- Lưu và đọc lại User, Group, GroupMember, Task với định danh/quan hệ không đổi.
- Restart xong vẫn đọc được thông tin, trạng thái và thành viên đã lưu.
- Nạp được công việc nay đã quá hạn; không dùng lại kiểm tra tạo mới để từ chối dữ liệu cũ hợp lệ.
- Ràng buộc không cho tạo thành viên trùng, công việc tham chiếu nhóm/người không tồn tại.
- Có schema/migration và dữ liệu thử; không để mật khẩu DB thật trong code hoặc tài liệu.

### US-08 — Ghi nhất quán và tránh mất thay đổi

**Nhu cầu:** Là thành viên, tôi muốn thay đổi của mình không bị ghi đè âm thầm khi người khác cũng đang sửa.  
**Mốc:** M3, kiểm chứng lại M4 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-07.

**Tiêu chí chấp nhận:**

- Cập nhật công việc và TaskEvent tối thiểu cùng thành công hoặc cùng rollback.
- Event ghi được người thao tác, thời điểm và thay đổi chính; không chứa mật khẩu/token/phiên.
- Hai phiên bản cùng sửa: lần lưu dựa trên dữ liệu đã cũ bị từ chối với thông báo cần tải lại.
- Nếu việc vừa được giao lại, người cũ không thể ghi trạng thái dựa trên bản dữ liệu đã mở trước đó.
- Lưu thất bại không làm thay đổi trạng thái trong DB hoặc ghi event báo thành công sai.

## C. Tài khoản, web và quyền thực tế

### US-09 — Đăng ký tài khoản

**Nhu cầu:** Là người dùng thử, tôi muốn có tài khoản riêng để công việc gắn đúng người.  
**Mốc:** M4 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-07, hiểu HTTP và cấu hình bảo mật framework.

**Tiêu chí chấp nhận:**

- Đăng ký với username, tên hiển thị và mật khẩu đúng BR-23/BR-24.
- Username khác chữ hoa/thường sau chuẩn hóa không tạo hai tài khoản trùng.
- Mật khẩu được băm bằng thư viện phù hợp; không xuất hiện nguyên văn trong DB, log hoặc phản hồi.
- Lỗi đầu vào không tạo tài khoản dở dang; hai yêu cầu đăng ký cùng username không tạo hai tài khoản.
- Có giới hạn thử đăng ký và hướng dẫn bảo vệ tài khoản cho người tham gia pilot.

### US-10 — Đăng nhập, đăng xuất và đổi mật khẩu

**Nhu cầu:** Là người dùng, tôi muốn chỉ mình thao tác dưới danh tính của mình và có thể kết thúc phiên.  
**Mốc:** M4 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-09.

**Tiêu chí chấp nhận:**

- Đăng nhập đúng tạo phiên hợp lệ; sai thông tin có thông báo chung, không tiết lộ mật khẩu/tài khoản có tồn tại.
- Đăng xuất làm phiên không dùng tiếp được cho yêu cầu bảo vệ.
- Đổi mật khẩu cần mật khẩu hiện tại; hủy các phiên cũ và yêu cầu đăng nhập lại.
- Hết phiên thì yêu cầu đăng nhập; không thực hiện một phần thao tác đang bảo vệ.
- Có giới hạn thử đăng nhập; thao tác thay đổi dữ liệu được bảo vệ khỏi CSRF khi dùng phiên cookie.
- Quy trình reset thủ công trong pilot xác nhận đúng người, hủy phiên cũ và bắt đổi mật khẩu tạm trước khi dùng nhóm; chưa có trang quên mật khẩu tự phục vụ.

### US-11 — Kiểm tra quyền phía máy chủ

**Nhu cầu:** Là thành viên, tôi muốn dữ liệu nhóm không bị người khác xem hoặc sửa.  
**Mốc:** M4 · **Ưu tiên:** Must, chặn mở pilot nếu chưa đạt · **Trạng thái:** Chưa có · **Phụ thuộc:** US-03 đến US-10.

**Tiêu chí chấp nhận:**

- Danh tính người thao tác lấy từ phiên, không từ actorId gửi lên.
- Kiểm tra nhóm và công việc cụ thể ở mọi đường đọc/ghi, không chỉ kiểm tra đã đăng nhập.
- Với hai nhóm A/B, đổi ID trên URL/body vẫn không truy cập chéo; thử cả user là Owner của nhóm B.
- Gửi giả createdBy, role hoặc assignee ngoài nhóm không vượt được quyền.
- Phản hồi từ chối không lộ chi tiết dữ liệu của nhóm khác; không thay đổi DB.

### US-12 — Tạo và sửa nhóm qua web

**Nhu cầu:** Là người tổ chức, tôi muốn tạo nhóm và sửa thông tin nhóm trên trình duyệt.  
**Mốc:** M4 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-01, US-10, US-11.

**Tiêu chí chấp nhận:**

- Tạo nhóm và Owner trong cùng giao dịch; thành công thì nhóm xuất hiện trong danh sách của người tạo.
- Chỉ Owner sửa tên/mô tả; Member hoặc người ngoài nhóm bị từ chối cả khi tự gửi yêu cầu.
- Tên/mô tả hợp lệ theo BR-15; lỗi giữ dữ liệu đã nhập không nhạy cảm.
- Một tài khoản có thể là Owner của nhóm này và Member của nhóm khác.

### US-13 — Tạo và thu hồi lời mời

**Nhu cầu:** Là chủ nhóm, tôi muốn chia sẻ lời mời để người khác chủ động tham gia.  
**Mốc:** M4 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-12.

**Tiêu chí chấp nhận:**

- Chỉ Owner tạo/thu hồi lời mời của nhóm mình.
- Liên kết khó đoán, dùng một lần, hiển thị hạn 24 giờ và có thể sao chép để chia sẻ thủ công.
- Thu hồi xong không sử dụng được; không ảnh hưởng thành viên đã tham gia trước đó.
- Token không xuất hiện trong log; DB lưu dạng phù hợp để kiểm tra mà không lưu token rõ.

### US-14 — Tham gia qua lời mời

**Nhu cầu:** Là người được mời, tôi muốn biết nhóm nào đang mời trước khi xác nhận gia nhập.  
**Mốc:** M4 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-13, US-10.

**Tiêu chí chấp nhận:**

- Chưa đăng nhập thì yêu cầu đăng nhập/đăng ký; thao tác này không tiêu thụ lời mời.
- Sau đăng nhập, lời mời hợp lệ chỉ cho xem thông tin tối thiểu để xác nhận: tên nhóm và người mời; chưa được xem công việc.
- Xác nhận hợp lệ thêm Member và đánh dấu dùng lời mời trong cùng giao dịch.
- Hết hạn/đã dùng/thu hồi bị từ chối; đã là thành viên không thêm trùng hoặc làm tiêu thụ lời mời.
- Hai người xác nhận cùng token đồng thời chỉ một người được thêm.

## D. Trải nghiệm sử dụng

### US-15 — Xem nhóm của tôi và trang nhóm

**Nhu cầu:** Là thành viên, tôi muốn mở đúng nhóm và thấy mọi người cùng đầu việc liên quan.  
**Mốc:** M5 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-11, US-12, US-14.

**Tiêu chí chấp nhận:**

- Danh sách chỉ chứa nhóm đang tham gia, thể hiện tên và vai trò của mình.
- Mở nhóm xem được thành viên và công việc theo đúng quyền.
- Chưa có nhóm/chưa có công việc có thông báo cùng hành động tiếp theo hợp lệ.
- Không hiển thị chức năng chỉ dành cho Owner với Member; máy chủ vẫn kiểm tra lại.
- Tên nhóm/thành viên/công việc hiển thị như văn bản, không thực thi nội dung HTML/script do người dùng nhập.

### US-16 — Tạo, xem và sửa công việc trên web

**Nhu cầu:** Là thành viên, tôi muốn thực hiện luồng công việc mà không cần dùng console.  
**Mốc:** M5 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-03, US-05, US-06, US-11, US-15.

**Tiêu chí chấp nhận:**

- Form đầy đủ trường, giới hạn và lỗi rõ tại từng trường; chỉ chọn người thuộc nhóm.
- Chi tiết hiển thị tên, mô tả, người tạo, người phụ trách, hạn theo giờ Việt Nam và trạng thái.
- Các nút đổi trạng thái/sửa hiển thị đúng quyền; kiểm tra máy chủ không dựa vào việc nút có hiển thị hay không.
- Lưu lỗi giữ lại dữ liệu không nhạy cảm; xung đột thông báo cần tải lại.
- Tải lại trang sau thành công không tự gửi lại thao tác tạo; nếu dùng form web, điều hướng sau khi lưu để tránh gửi lại do refresh.

### US-17 — Bộ lọc và tổng quan tiến độ

**Nhu cầu:** Là thành viên, tôi muốn nhanh chóng tìm việc cần chú ý và nhìn tiến độ của nhóm.  
**Mốc:** M5 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-04, US-15, US-16.

**Tiêu chí chấp nhận:**

- Có bộ lọc người phụ trách/của tôi, trạng thái và tình trạng thời hạn; xóa lọc trở về danh sách đầy đủ.
- Tổng quan đếm TODO/IN_PROGRESS/DONE, quá hạn và tỷ lệ hoàn thành theo toàn nhóm.
- Số kết quả lọc hiển thị riêng; không làm người dùng hiểu nhầm tỷ lệ hoàn thành của nhóm.
- Nhóm 0 việc hiển thị 0% và thông báo, không lỗi chia cho 0.
- Nhãn thời hạn có chữ, không chỉ màu; dữ liệu thời hạn cập nhật khi tải lại hoặc thao tác mới, chưa cam kết tự cập nhật thời gian thực.

### US-18 — Giao diện dễ hiểu và phản hồi lỗi

**Nhu cầu:** Là người dùng thử, tôi muốn hiểu mình cần làm gì khi gặp lỗi và dùng được trên điện thoại.  
**Mốc:** M5–M6 · **Ưu tiên:** Should cho mức trau chuốt; Must cho khả năng dùng và lỗi thiết yếu · **Trạng thái:** Chưa có · **Phụ thuộc:** US-15 đến US-17.

**Tiêu chí chấp nhận:**

- Các màn hình chính dùng được ở chiều rộng khoảng 360 px và máy tính; nội dung chính không bị che.
- Trường nhập có nhãn, nút có tên rõ, đi qua thao tác chính bằng bàn phím được.
- Phân biệt dữ liệu trống, không khớp lọc, không có quyền, mất phiên và lỗi hệ thống.
- Lỗi hệ thống không hiện stack trace; có mã tham chiếu để người vận hành đối chiếu.
- Kiểu chữ, màu và khoảng cách có thể đơn giản; không dành thời gian hiệu ứng trước khi luồng chính ổn.

## E. Kiểm thử, vận hành và kiểm chứng giá trị

### US-19 — Kiểm tra hồi quy các luồng quan trọng

**Nhu cầu:** Là người phát triển, tôi muốn phát hiện khi sửa một tính năng làm sai nghiệp vụ hoặc quyền đã có.  
**Mốc:** xuyên suốt, chốt ở M6 · **Ưu tiên:** Must · **Trạng thái:** Chưa có bộ kiểm thử lưu trong project · **Phụ thuộc:** các story tương ứng.

**Tiêu chí chấp nhận:**

- Có kiểm tra quy tắc thời hạn bằng thời điểm cố định, thành viên, quyền trạng thái và giao lại việc.
- Có kiểm tra tích hợp lưu/restart, quan hệ không trùng, rollback và xung đột.
- Có kiểm tra web cho phiên đăng nhập, truy cập chéo nhóm và giả mạo actorId/role.
- Có bằng chứng chạy UAT-01 đến UAT-14; lỗi chặn pilot được sửa và chạy lại phần liên quan.
- Test xác minh hành vi; không thêm test chỉ để lặp lại getter/setter hoặc làm đẹp số lượng.

### US-20 — Triển khai môi trường dùng thử

**Nhu cầu:** Là người tham gia thử, tôi muốn truy cập độc lập trên trình duyệt trong thời gian thử đã thống nhất.  
**Mốc:** M6 · **Ưu tiên:** Must cho pilot truy cập độc lập · **Trạng thái:** Chưa có · **Phụ thuộc:** US-09 đến US-19, phương án chi phí và US-21.

**Tiêu chí chấp nhận:**

- Chọn nơi chạy có Java/DB phù hợp, HTTPS và lưu trữ bền vững; xác minh hạn mức/chu kỳ phí lúc triển khai.
- Không vượt ngân sách người dùng đã đặt; không mặc định có credit hoặc free tier.
- Cấu hình bí mật tách khỏi mã nguồn; có hướng dẫn khởi động, kiểm tra hoạt động, xem log, quay lại bản trước nếu cần.
- Chạy smoke test tài khoản–nhóm–việc trên môi trường thật; không dùng dữ liệu nhạy cảm cho demo.
- Công bố rõ thời gian hỗ trợ, giới hạn dịch vụ và kênh báo lỗi cho nhóm thử.
- Nếu chưa đủ điều kiện chạy độc lập, ghi rõ chỉ hoàn thành demo có người hỗ trợ; không đánh dấu story này xong.

### US-21 — Sao lưu và phục hồi

**Nhu cầu:** Là thành viên, tôi muốn có khả năng lấy lại dữ liệu nếu môi trường chạy gặp sự cố.  
**Mốc:** M6 · **Ưu tiên:** Must · **Trạng thái:** Chưa có · **Phụ thuộc:** US-07, lựa chọn nơi chạy cho US-20.

**Tiêu chí chấp nhận:**

- Có lịch và người chịu trách nhiệm sao lưu theo NFR-06; biết vị trí bản sao và cách bảo vệ nó.
- Phục hồi vào DB thử riêng, kiểm tra nhóm/thành viên/việc/quyền; không thử ghi đè DB có dữ liệu người dùng.
- Ghi thời điểm, thời gian phục hồi, dữ liệu có trong bản sao và các bước thực hiện.
- Có hướng dẫn xử lý khi sao lưu lỗi; chỉ tuyên bố khoảng dữ liệu có thể mất dựa trên lịch thực tế đã chạy.
- Việc kiểm tra phục hồi không đồng nghĩa được phép xóa hoặc reset dữ liệu pilot.

### US-22 — Dùng thử và quyết định bước tiếp theo

**Nhu cầu:** Là chủ sản phẩm, tôi muốn biết ứng dụng có giúp nhóm thật hay cần thay đổi.  
**Mốc:** M7 · **Ưu tiên:** Must để tuyên bố đã kiểm chứng người dùng · **Trạng thái:** Chưa có · **Phụ thuộc:** US-19, US-20, US-21.

**Tiêu chí chấp nhận:**

- Nhóm 4–5 người đồng ý dùng thử, hiểu giới hạn và biết cách báo lỗi.
- Chạy hai tuần với việc thật, tách dữ liệu demo khi thống kê.
- Ghi chỉ số mục 3.3 của kế hoạch bằng số liệu có thật và ghi cả trường hợp không đạt.
- Phỏng vấn cuối đợt, tổng hợp vấn đề, phân biệt lỗi với yêu cầu mới.
- Quyết định tiếp tục/sửa hướng/dừng có căn cứ; không tự thêm mọi tính năng được gợi ý.

## Truy vết nghiệm thu

| Nghiệm thu trong kế hoạch | Story cung cấp hành vi chính |
|---|---|
| UAT-01 | US-09, US-10 |
| UAT-02 | US-01, US-02, US-12, US-13, US-14 |
| UAT-03 | US-13, US-14 |
| UAT-04 | US-03, US-16 |
| UAT-05 | US-05, US-11 |
| UAT-06 | US-06, US-08, US-11 |
| UAT-07 | US-05, US-08 |
| UAT-08 | US-04, US-17 |
| UAT-09 | US-06, US-07 |
| UAT-10 | US-11 |
| UAT-11 | US-08, US-16 |
| UAT-12 | US-04, US-17 |
| UAT-13 | US-09, US-10, US-19 |
| UAT-14 | US-21 |

US-19 xác minh các hành vi trên; US-20/US-22 chứng minh môi trường chạy và việc dùng thử. Chạy test kỹ thuật không thay thế phản hồi người dùng.

## Định nghĩa bắt đầu và hoàn thành một việc

**Sẵn sàng bắt đầu:** hiểu người dùng cần gì, quy tắc và ngoại lệ; xác định phụ thuộc; chia đủ nhỏ để có thể hoàn thành một phần trong 1–2 buổi học.

**Hoàn thành:** hành vi đạt tiêu chí; đã chạy và xem lỗi/ngoại lệ quan trọng; người học giải thích được luồng dữ liệu và phần code mình sửa; cập nhật hướng dẫn nếu cách dùng thay đổi. Chưa đạt quyền hoặc lưu dữ liệu thì không đánh dấu xong bằng cách chỉ chụp giao diện.

## Danh sách để sau W1

Nhắc hạn chủ động, bình luận, lịch sử hiển thị, lưu trữ/khôi phục việc, rời/loại thành viên/chuyển Owner, tìm kiếm, xuất CSV, Kanban, quên mật khẩu tự phục vụ trước khi mở rộng cho người lạ. Chỉ tạo story chi tiết khi có nhu cầu được xác minh và quyết định ưu tiên.

Mỗi tuần chỉ chọn một story đang làm; có thể lấy một phần nhỏ từ story đó. Không mở đồng thời đăng nhập, frontend và database khi bài danh sách thành viên còn chưa hiểu.

## W1.1 — collaboration increment (được giao local ngày 21/10/2026)

Các story dưới đây mở rộng bản W1 cho checkout W1.1 riêng. Tiêu chí kỹ thuật chưa được kiểm đạt cho tới khi có receipt trên commit thực tế; dữ liệu/thử nghiệm đều phải synthetic.

### W1.1-C1 — Bình luận và timeline

- Thành viên đúng nhóm đọc timeline, tạo comment plain text tối đa 4.000 code points, sửa comment của mình bằng optimistic version và ẩn mềm comment theo quyền; Owner có thể ẩn comment điều phối.
- Revision lưu trước/sau và actor; nội dung hiện ẩn không được lộ ở timeline, API hoặc notification. Timeline kết hợp TaskEvent/comment/file, page size 20, thứ tự ổn định; TaskEvent giữ nguyên.

### W1.1-C2 — Mention và inbox

- Gợi ý/tìm trong tối đa 10 thành viên hoạt động cùng nhóm mỗi lượt; mention bằng ID, không `@everyone`, không tự cấp quyền.
- Recipient là assignee/creator hiện còn quyền và người được mention, loại actor, bỏ trùng. Sửa comment chỉ notify người được thêm.
- Ghi notification và delivery intent cùng transaction với comment. Inbox hỗ trợ read/unread, POST+CSRF, owner-only; quyền được kiểm lại khi mở/deep-link.

### W1.1-C3 — Attachment và pin

- Allowlist PDF, PNG, JPEG, TXT, DOCX, XLSX; giới hạn mỗi file 10 MiB, request 12 MiB/1 file, 20 file/task, 200 MiB/group. Tính file tombstone vào quota.
- Storage riêng ngoài webroot với server UUID, staged/ready/failed, signature/OOXML validation và reconciliation. Download quyền membership + attachment disposition + nosniff.
- Owner hoặc task creator ghim/bỏ ghim tối đa 5 item/task. File backup/restore đi cùng DB generation và SHA-256 manifest.

### W1.1-C4 — Desktop push và reminders

- Người dùng chủ động bật/tắt thiết bị, event preferences và reminder offsets 24h/1h. Không hỏi permission ngoài click; inbox hoạt động dù push không khả dụng.
- Durable outbox chạy sau commit, lease/retry có giới hạn, dedupe, xử lý HTTP provider class và kiểm lại recipient/grant/preference trước retry.
- Service worker chỉ push/click, không cache trang/API/file đăng nhập. Push payload chung; logout/reset/anonymize thu hồi grants ở server.

### W1.1-C5 — Server preparation

- Có migration/metadata contract/runtime-grant/backup restore runbooks, local jar/source hash và evidence giới hạn.
- Chưa tạo server/host/domain/cert/cloud, không gửi push thiết bị thật hay thay launcher. Scanner antivirus thật và HTTPS/browser E2E có trusted origin thuộc bước M6 với quyết định riêng.

Trạng thái W1.1-C1…C5 ngày21/10: implementation/local candidate và runbooks đã có; phạm vi author verification/receipt ở [w11-final](verification/w11-final.md). Desktop push thiết bị thật/server, scanner/offsite và owner acceptance vẫn chưa kiểm, không đổi các story lịch sử thành đã được owner nghiệm thu.
