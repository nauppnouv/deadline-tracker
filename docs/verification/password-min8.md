# BR-24 — mật khẩu tài khoản min8 — 21/10/2026

Yêu cầu trực tiếp của chủ dự án thay min15 bằng8. Base HEAD `5ebb0267f2fa4ce92141ac2c2340706b89caca28`, branch `fix/p3-01-web-root-secret`, source min8 được chốt theo yêu cầu push riêng sau các kiểm dưới đây. Repo gốc vẫn `d992edc4172afc8eff050312066a7b33af35b6b5`, sạch. Evidence local-only: `D:/Documents/coding/deadline-tracker-p3-01/target/password-min8-20261021T011151+0700`; raw log/XML chứa fixture của DB thử, không dùng làm log app thật hoặc tự đưa vào ZIP cũ.

## Lệnh và kết quả thực chạy

1. `bash scripts/mvn.sh -o -Dtest=PasswordPolicyTest -DfailIfNoTests=true test`: exit1;13 tests/4 failures/0 errors/skips trước sửa validator. `red.log`, red-receipt.json và red-surefire/ giữ nguyên.
2. `bash scripts/mvn.sh -o '-Dtest=PasswordPolicyTest,AuthWebTest#passwordMinimumSupportsRegisterAndChange+registrationRules+sessionLifecycle' -DfailIfNoTests=true test`: lượt đầu exit1,16 tests/0 failures/1 error. XPath đòi XML gặp HTML5; thay riêng matcher HTML trong test. Đây là lỗi test, không kết luận hành vi đạt/thất bại. Giữ green.log/green-receipt.json/green-surefire/.
3. Cùng lệnh sau sửa matcher: exit0, **16 tests/0 failures/errors/skips**, lúc01:14:39+07:00. Log `green-final.log`, receipt `green-final-receipt.json`, XML `green-final-surefire/`; SHA256 log `9646e5647aefb6272da54ba8acaa4631354809bbe5e37715e3d91daa6283e7c1`.
4. `bash scripts/mvn.sh -o -DskipTests package`: exit0, chỉ package sau focused tests, không test thêm. Build log SHA256 `0f8b63cf9103411716cf1487e9f076e2193c69d2e1b79388b4ab10d4880ace60`. JAR riêng `D:/Documents/coding/deadline-tracker-p3-01/target/password-min8-20261021T011151+0700/candidate-min8.jar`, SHA256 `c3c3465dadf5d973be4399a1324268892077deb92f6c66ad0914141509d722db`. new-jar-receipt.json ghi66 main classes khớp output đã test và hai form khớp source.

## Phạm vi

13 unit invocations kiểm ASCII8/14/15/128 chấp nhận,7/129 từ chối, Unicode code points8/128 và7/129, null/rỗng,8 ký tự có spaces đầu/cuối. Ba ca web/service gồm registrationRules/sessionLifecycle và ca mới: form min8, POST register7 không tạo account,8 thành công/login được; đổi7 không làm mất phiên hợp lệ, đổi8 giữ spaces, hủy phiên cũ, login mới được/old password từ chối. DB Testcontainers mới theo DatabaseTest/image pin, không trỏ DB người dùng. Không đổi dependency/JDK/image/migration, ngưỡng DB/migration secrets hoặc password hash hiện có.

Chốt source theo yêu cầu push riêng ngày21/10; chưa triển khai JAR min8 cho launcher/bản đang chạy; JAR release cũ SHA256 f69e177d406861ce6347dd824b5a01f85fa1a10bc7d0254ac67a3ba47254dc2a và ZIP đã giao giữ nguyên. Không full suite, audit dependency mới, independent review hoặc Gate A; không gán các test này cho release5ebb026/CP006. AI material chưa đọc.

## Chốt source để push — 21/10/2026

User yêu cầu push bản sửa min8 lên GitHub. Nhánh đích `origin/fix/p3-01-web-root-secret`, parent local `5ebb0267f2fa4ce92141ac2c2340706b89caca28`; commit có đủ13 paths implementation/tests/docs của min8, không gồm plan W1.1 ngoài checkout. Full parent/new HEAD và remote verification được lưu ngoài repo tại `D:/Documents/coding/deadline-tracker-checkpoints/min8-push-20261021T013041+0700/` sau commit/push, tránh self-referencing hash.

16focused tests ở trên đã chạy trước commit; lượt push đối chiếu raw business/resource/test bytes với snapshot đã test, kiểm docs/diff/secret/migration. Không lặp fullsuite/rehearsal/dependency audit trong lượt push; không ghi test đã chạy ở new commit. Repo gốc/JAR/launcher/settings/cert/state/process và ZIP release cũ giữ nguyên; không merge/tag/deploy hoặc nhận Gate A/owner acceptance.
