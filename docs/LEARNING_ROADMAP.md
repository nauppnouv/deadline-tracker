# Lộ trình học và thực hiện — Deadline Tracker

**Phiên bản:** 0.1 · **Ngày:** 17/10/2026  
**Đọc cùng:** [Kế hoạch sản phẩm](PROJECT_PLAN.md) · [Backlog](BACKLOG.md)

## 1. Cách phối hợp với AI

Mục tiêu là tự xử lý được một tính năng nhỏ từ yêu cầu đến kết quả chạy. AI hỗ trợ giải thích, đặt câu hỏi, chỉ ra lỗi và review; tốc độ sinh code không phải thước đo tiến bộ.

Một buổi học khoảng 60–90 phút, có thể điều chỉnh:

1. **5–10 phút:** tự viết đầu vào, kết quả mong muốn và một trường hợp lỗi.
2. **15–20 phút:** học đúng một khái niệm cần dùng.
3. **25–40 phút:** tự viết và chạy phần nhỏ.
4. **10–15 phút:** thử dữ liệu sai và nhờ AI review nếu cần.
5. **5 phút:** ghi hiểu được gì, mắc ở đâu, bước sau là gì.

Khi mắc: ghi điều mong đợi → điều thực tế xảy ra → nguyên văn lỗi → đoạn liên quan → điều đã thử. Hỏi AI giải thích/gợi ý trước; nếu cần xem lời giải thì sau đó tự viết lại một biến thể và dự đoán kết quả.

Mỗi tuần chọn một phần đã học và làm lại không dùng sinh code. Có thể tra tài liệu/cú pháp. Không cần học thuộc cả chương trình.

## 2. Tiến độ hiện tại

Bạn đã tự tạo project, biên dịch/chạy Java, dùng enum và gán lại biến. Task/Main đã được AI bổ sung thành ví dụ có năm thuộc tính và kiểm tra dữ liệu. Phần đã được viết hộ cần đọc và tự sửa thử trước khi tính là kỹ năng mình làm được.

Chưa có Group, danh sách thành viên, lưu trữ, đăng nhập hoặc web. Bài tiếp theo là M1, sau khi hoàn thành kiểm tra hiểu ngắn ở M0.

## 3. Các mốc và điều kiện chuyển tiếp

### M0 — Nắm chương trình đang có

**Kiến thức:** biến và thuộc tính, class/đối tượng, constructor, this, getter, enum, if, null, phương thức, lỗi đầu vào.

**Tự làm:**

1. Chạy Main và giải thích chương trình đi từ đâu đến đâu.
2. Tạo công việc thứ ba bằng code tự viết.
3. Đổi một trạng thái; dự đoán công việc nào bị ảnh hưởng.
4. Thử tên toàn khoảng trắng, hạn hôm qua và mô tả null; giải thích kết quả.
5. Đọc `try/catch`, `final` và phương thức `printTask`; hỏi từng phần chưa hiểu.

**Qua mốc khi:** tự sửa ví dụ và giải thích được constructor/getter/updateStatus. Không cần nhớ mọi cú pháp.

### M1 — Nhóm, định danh và danh sách thành viên

**Story:** US-01, US-02. **Khái niệm mới:** List/ArrayList, vòng lặp, ID và quan hệ giữa đối tượng.

**Thứ tự bài nhỏ:**

1. Tự tạo Group chỉ có tên, constructor kiểm tra tên, getter.
2. Tạo hai nhóm; chứng minh dữ liệu của chúng độc lập.
3. Học List/ArrayList bằng ví dụ ba tên, thêm phần tử và duyệt để in.
4. Tạo User tối giản có ID và tên hiển thị; dùng hai người cùng tên để hiểu vì sao cần ID.
5. Gắn thành viên với nhóm; kiểm tra có thuộc nhóm không và tránh thêm trùng.
6. Biểu diễn Owner/Member theo từng nhóm, không đặt một vai trò duy nhất lên toàn bộ User.

**Qua mốc khi:** tự thêm thành viên, duyệt danh sách và giải thích vì sao so tên không đủ để xác định cùng một người.

**Giới hạn:** đây là người dùng giả lập. Chọn ID trong console không phải xác thực.

### M2 — Nghiệp vụ công việc qua console

**Story:** US-03 đến US-06. **Khái niệm mới:** điều phối nghiệp vụ, tách trách nhiệm, nhập dữ liệu, so sánh ngày giờ.

**Thứ tự bài nhỏ:**

1. Đọc lại ma trận quyền; tự viết ba ví dụ được phép và ba ví dụ bị từ chối.
2. Gắn Task với nhóm, người tạo và người phụ trách qua ID/tham chiếu đã hiểu ở M1.
3. Tạo một nơi điều phối thao tác, dự kiến TaskService; Main gọi vào đó thay vì tự chứa mọi quy tắc.
4. Làm tạo việc và kiểm tra thành viên trước.
5. Làm đổi trạng thái theo người thao tác giả lập.
6. Làm sửa thông tin/giao lại và các ngoại lệ của việc quá hạn/DONE.
7. Làm lọc của tôi/trạng thái/quá hạn/sắp đến hạn với thời điểm thử truyền vào hoặc nguồn thời gian thay thế được.
8. Sau khi hiểu vòng lặp/điều kiện, thêm menu console nhỏ để chọn thao tác. Không cần giao diện console đẹp.

**Qua mốc khi:** tự trình diễn người ngoài nhóm bị từ chối và người phụ trách cũ mất quyền sau khi giao lại. Giải thích được “quá hạn” khác trạng thái.

**Cách review:** chạy một luồng bình thường và một luồng lỗi cho mỗi quy tắc. Bắt đầu viết kiểm thử tự động cho quy tắc quan trọng khi đã hiểu phương thức và kỳ vọng; không dành thời gian kiểm thử máy móc getter.

### M3 — SQL và dữ liệu tồn tại sau khi restart

**Story:** US-07, US-08. **Khái niệm mới:** bảng/khóa chính/khóa ngoại, SQL, JDBC, giao dịch, phiên bản bản ghi.

**Thứ tự bài nhỏ:**

1. Vẽ quan hệ User–GroupMember–Group–Task bằng lời và vài dòng dữ liệu mẫu.
2. Học SELECT/INSERT/UPDATE cùng WHERE trên DB thử riêng.
3. Chuyển project sang Maven để quản lý thư viện; giữ chương trình vẫn chạy sau chuyển đổi.
4. Học JDBC qua một bảng nhỏ, sau đó tạo lớp lưu/đọc dữ liệu. Nghiệp vụ gọi nơi lưu trữ thay vì gọi SQL trực tiếp từ mọi chỗ.
5. Lưu nhóm/thành viên rồi công việc; restart và đọc lại.
6. Tách kiểm tra hạn khi tạo mới khỏi việc dựng lại Task đã lưu từ trước.
7. Thử rollback, quan hệ không trùng và cập nhật dựa trên phiên bản; thêm ghi vết thay đổi chính.
8. Tự sao lưu/khôi phục dữ liệu mẫu vào DB khác.

**Qua mốc khi:** dữ liệu còn sau restart; việc đã quá hạn vẫn đọc được; giải thích một tình huống cần giao dịch và một tình huống xung đột.

**Lựa chọn kỹ thuật đề xuất:** tiếp tục Java và MariaDB, học JDBC đủ để hiểu đường đi dữ liệu. Không yêu cầu học cả JDBC và JPA đồng thời; nếu sau này dùng JPA phải có lý do và bài chuyển đổi riêng. Không cần viết lại toàn bộ sản phẩm hai lần.

### M4 — HTTP, backend web, đăng nhập và lời mời

**Story:** US-09 đến US-14; kiểm chứng lại US-08/US-11. **Khái niệm mới:** request/response, session, xác thực so với phân quyền, endpoint/controller, dữ liệu người dùng gửi lên không đáng tin.

**Thứ tự bài nhỏ:**

1. Tự giải thích một request có địa chỉ, phương thức, dữ liệu đầu vào và kết quả gì.
2. Chuyển lớp giao tiếp sang Spring Boot; giữ lại nghiệp vụ đã kiểm chứng, không chép lại kiểm tra trong mỗi màn hình.
3. Làm một luồng đọc dữ liệu mẫu để hiểu controller → nghiệp vụ → lưu trữ.
4. Học đăng ký/đăng nhập/đăng xuất qua framework bảo mật; không tự thiết kế thuật toán mật mã hoặc tự viết hệ thống token.
5. Lấy danh tính từ phiên và kiểm tra quyền ở máy chủ với hai nhóm dữ liệu.
6. Làm tạo/sửa nhóm qua web; sau đó lời mời một lần và chấp nhận lời mời.
7. Thử hết phiên, CSRF, giả actorId, truy cập chéo nhóm, lời mời hết hạn và tranh chấp sử dụng.
8. Hoàn thiện đổi mật khẩu và quy trình hỗ trợ reset trong pilot.

**Qua mốc khi:** tự mô tả được vì sao đăng nhập thành công không đồng nghĩa được đọc mọi Task; dùng hai tài khoản chứng minh quyền hoạt động ngay cả khi tự gửi yêu cầu sai.

**Điểm phải kiểm tra tại thời điểm thực hiện:** chọn phiên bản Java phù hợp với Spring Boot và thư viện đang dùng. Máy hiện có JDK 26.0.1; đây không phải quyết định chốt phiên bản chạy web. Không tự nâng/hạ JDK theo một ví dụ cũ trên mạng.

### M5 — Giao diện tối thiểu và một luồng hoàn chỉnh

**Story:** US-15 đến US-18. **Khái niệm mới:** HTML/form, kết quả lưu, lỗi tại trường, trạng thái màn hình.

**Lựa chọn đề xuất:** một ứng dụng Spring Boot, trang HTML dựng phía máy chủ bằng Thymeleaf và phiên đăng nhập. Lý do là ưu tiên backend và giảm số phần phải học cùng lúc. Đây là đề xuất, chưa cài thư viện hay chốt phiên bản. Nếu chọn React, cần thêm thời gian học JavaScript, luồng API và xác thực; không mặc định đó là điều kiện để MVP có giá trị.

**Thứ tự bài nhỏ:**

1. Nhóm của tôi → mở nhóm → danh sách công việc.
2. Tạo công việc → hiển thị lỗi đầu vào → lưu thành công → xem chi tiết.
3. Đổi trạng thái và sửa việc theo quyền.
4. Lọc và tổng quan theo đúng định nghĩa; phân biệt nhóm trống và kết quả lọc trống.
5. Màn hình lời mời và thiết lập nhóm/đổi mật khẩu.
6. Kiểm tra màn hình nhỏ, nhãn nhập liệu và bàn phím; tránh đầu tư hiệu ứng lúc này.

**Qua mốc khi:** một người thử làm trọn luồng tạo nhóm/mời/giao việc/cập nhật trên trình duyệt với mức hỗ trợ đã ghi nhận. Giao diện không thay thế kiểm tra phía máy chủ.

### M6 — Kiểm tra, triển khai và khôi phục

**Story:** US-19 đến US-21. **Khái niệm mới:** môi trường chạy, cấu hình/bí mật, log, smoke test, sao lưu và phục hồi.

**Thứ tự bài nhỏ:**

1. Chạy các UAT và bộ kiểm tra hồi quy; sửa lỗi quyền/dữ liệu trước.
2. So sánh một số phương án hosting dựa trên yêu cầu thật và giá hiện tại; chưa đủ ngân sách thì giữ demo có quan sát.
3. Viết hướng dẫn biến cấu hình, chạy ứng dụng và migration; dùng dữ liệu mẫu để thử.
4. Triển khai có HTTPS; chạy smoke test luồng chính với tài khoản thử.
5. Thử bản sao lưu/khôi phục vào DB riêng; xác định rõ người vận hành làm gì khi lỗi.
6. Đo mục tiêu hiệu năng trong môi trường ghi rõ; công bố giới hạn, không coi gói miễn phí là SLA.

**Qua mốc khi:** một người khác làm theo tài liệu chạy được bản mẫu; UAT quan trọng đạt; có phương án giữ dữ liệu trong thời gian pilot. Chưa có nơi chạy độc lập thì chưa đánh dấu hoàn thành pilot web.

### M7 — Hai tuần người dùng thật và tổng kết

**Story:** US-22. **Khái niệm mới:** quan sát người dùng, tách lỗi khỏi yêu cầu, đo kết quả và quản lý thay đổi.

**Tự làm:**

1. Thống nhất với nhóm phạm vi và cách báo lỗi; xin phép dùng công việc thật không nhạy cảm.
2. Ghi việc họ thực sự làm, chỗ mắc và mức trợ giúp; không hướng dẫn thay mọi thao tác.
3. Ưu tiên sửa lỗi chặn; yêu cầu tính năng mới ghi vào backlog để đánh giá.
4. Cuối đợt tổng hợp chỉ số thật, cả kết quả không đạt; phỏng vấn lý do tiếp tục/bỏ dùng.
5. Viết bài tổng kết: vấn đề, lựa chọn, ví dụ lỗi đã sửa, kết quả, giới hạn và quyết định tiếp theo.

**Qua mốc khi:** giải thích được sản phẩm có/không tạo giá trị ở đâu bằng bằng chứng; tự sửa được một yêu cầu nhỏ từ đầu đến kiểm chứng.

## 4. AWS sau luồng chính

Mục tiêu học AWS vẫn được giữ, nhưng không thêm nhiều dịch vụ vào trước khi hiểu nghiệp vụ và dữ liệu. Sau M6 hoặc khi đã có điều kiện triển khai, chọn **một bài thực hành có đầu ra**: ví dụ triển khai một bản mẫu và giải thích đường truy cập, cấu hình, log, sao lưu và cách dọn tài nguyên.

Trước bài đó mới xác minh tài khoản, quyền, khả năng miễn phí, giá và giới hạn hiện tại; lập dự toán và bước dọn dẹp. Dùng dữ liệu thử. Việc cần triển khai tính phí phải có phương án cụ thể trong ngân sách, không tự coi kế hoạch này là lệnh tạo tài nguyên.

Không cần dùng SQS, Kafka, Kubernetes hoặc microservices để chứng minh có kinh nghiệm AWS ở dự án này. Chỉ chọn thêm khi có vấn đề và yêu cầu cụ thể.

## 5. Tự đánh giá mỗi tuần

Ghi ngắn trong một file nhật ký do bạn tự viết:

| Câu hỏi | Bằng chứng nên có |
|---|---|
| Tuần này tự làm được gì? | Một thay đổi cụ thể và kết quả chạy |
| Có phần nào AI viết nhưng mình chưa hiểu? | Tên phần và điều chưa hiểu |
| Đã gặp lỗi gì, tìm ra bằng cách nào? | Triệu chứng, nguyên nhân và cách kiểm chứng |
| Có tự dự đoán đầu ra trước khi chạy không? | Một ví dụ đúng/sai và lý do |
| Bước tiếp theo nhỏ nhất là gì? | Một bài hoàn thành được trong 1–2 buổi |

Sau M1 và M3, ước lượng lại lịch. Nếu phải kéo dài vì học, ghi lại trung thực. Không tạo worklog hay bằng chứng sử dụng từ những việc chưa làm.

## 6. Việc bắt đầu ngay

1. Đọc mục 2, 4, 5 và 7 của kế hoạch để biết phần nào đã chốt/phần nào còn đề xuất.
2. Hoàn thành M0 nếu còn dòng code trong Task/Main chưa hiểu.
3. Tự viết Group có tên, constructor và getter theo bài US-01.
4. Gửi phần tự viết để review, rồi học List/ArrayList qua ví dụ nhỏ trước khi đưa vào Group.
5. Song song, hỏi nhóm 3 câu quan trọng nhất: đang ghi việc ở đâu, lần trễ gần nhất thế nào, có chịu thử hai tuần không.

Không cần tạo tất cả bảng, endpoint và màn hình từ đầu. Backlog là bản đồ; bài hiện tại chỉ là một đoạn nhỏ trên bản đồ.

## 7. Khi muốn nhờ Claude review độc lập

Hiện chưa có phần nào bị chặn cần Claude làm thay. Nếu muốn thêm một góc nhìn, gửi ba tài liệu kế hoạch này kèm prompt sau; không cần gửi dữ liệu cá nhân hoặc bí mật của nhóm.

> Hãy review kế hoạch BA cho ứng dụng quản lý deadline nhóm đồ án 4–5 người, do một sinh viên đầu năm 3 làm một mình để học backend và phục vụ nhóm thật. Ngân sách tối đa 50.000 đồng/tuần; hiện chỉ có project Java console cơ bản. Hãy tìm mâu thuẫn giữa scope, ma trận quyền, quy tắc nghiệp vụ, dữ liệu, user story và UAT; chỉ ra yêu cầu còn thiếu làm chặn luồng chính; đánh giá lịch có quá sức không. Ưu tiên cắt phạm vi hợp lý và nêu ảnh hưởng. Phân biệt lỗi cần sửa ngay với đề xuất tùy chọn. Trả kết quả thành bảng: phát hiện, mức ảnh hưởng, dẫn chiếu mục/ID, đề nghị sửa, lý do. Không viết code và không tự mở rộng thành nền tảng quản lý dự án đầy đủ.

Nhận review xong, đối chiếu từng phát hiện với mục tiêu và cập nhật bản kế hoạch có lý do; không hợp nhất mọi gợi ý một cách tự động.
