# Bốn tình huống cần tự giải thích

## 1. Tạo công việc

GET `/groups/{group}/tasks/new` qua login và GroupMembershipFilter. Form chỉ có title, description, assigneeId, dueAt, version; không nhận actorId/createdBy/role/status khởi tạo.

POST có CSRF vào TaskController. Actor từ TrackerPrincipal, Bean validation kiểm tra field, TimeView đổi giờ Việt Nam sang Instant. WorkService kiểm tra GroupAccess/người được giao hoạt động rồi Task.create kiểm tra tên/độ dài/hạn > Clock.instant và đặt TODO. Lưu task và CREATED event cùng transaction; lỗi rollback cả hai. Thành công redirect detail để refresh không tạo lại.

Đọc TaskWebTest.createAndValidation, TaskDomainTest, PersistenceTest.eventAtomicity. Đổi giới hạn tên phải đối chiếu domain, form, schema và test; không chỉ maxlength HTML.

## 2. Người ngoài nhóm đổi ID

Owner B gọi `/groups/A/tasks/X`: GroupMembershipFilter dùng actor từ session kiểm tra membership A trước binding. Không thuộc nhóm →404, controller không đọc task. Dùng group B với task X của A: query findByIdAndGroupId không tìm thấy →404. Không được query chỉ task ID rồi tin group từ form.

Member A xem được nhưng không phải assignee đổi status →403. Owner cũng chịu quy tắc này. Owner muốn tự làm phải reassign cho mình, có REASSIGNED event, rồi mới đổi status.

Đọc TaskWebTest.everyEndpointIsolation, AuthWebTest.accessMatrix, TaskWebTest.statusPermission. Ẩn nút chỉ hỗ trợ giao diện, quyền thực ở máy chủ.

## 3. Quá hạn vẫn nạp được

Việc tạo hôm qua hạn tối qua từng hợp lệ. Hôm nay DB nạp qua Task.restore, không chạy validation tạo mới. Overdue = status != DONE && now > dueAt; đúng hạn chưa overdue. Không cần job sửa trạng thái DB khi thời gian qua hạn.

Sửa tên hoặc reassign giữ hạn cũ vẫn được. Form chỉ có phút; domain so phút để phân biệt có đổi hạn không. Chỉ khi phút mới khác mới yêu cầu tương lai. DONE chỉ sửa title/description đến khi assignee mở lại.

Đọc RestartPersistenceTest, TaskDomainTest.deadlineBoundaries, TaskWebTest.filtersAndPastDue. Test restart đóng context rồi mở lại cùng DB, không chỉ đọc repository hai lần.

## 4. Hai người cùng sửa

Hai form đọc version3. A reassign, lưu task version4 cùng event. B gửi version3: conflict409, giữ phần nhập để sao chép/reload. Nếu cả hai transaction đều qua precheck version3, @Version làm UPDATE có điều kiện version=3, chỉ một bên cập nhật được. Bên thua rollback event, không retry bằng cách ghi đè.

Assignee cũ gửi sau reassign bị từ chối quyền nếu dùng version mới, hoặc conflict nếu version cũ. Đọc PersistenceTest.actualOptimisticLock/staleWrite, TaskWebTest.editAndConflict. Demo dùng hai tab đã mở trước khi lưu tab đầu; refresh tab thứ hai trước submit làm mất điều kiện stale.

## Bắt đầu đọc

Domain → WorkService/GroupAccess → controller → template → test. `bash scripts/mvn.sh test -Dtest=TaskDomainTest` chạy một test; sửa nghiệp vụ xong chạy `bash scripts/check.sh`. Thử thật với nhóm là bước khác, không thay bằng dữ liệu giả.
