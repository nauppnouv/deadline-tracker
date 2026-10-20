# Bản local P3-01/P3-02 — 21/10/2026

Đợt chốt này giao source được định danh bằng một commit local, JAR có hash cố định và ZIP chọn lọc đã kiểm. Không đồng nghĩa hoàn thành toàn bộ dự án, học tập, pilot hoặc Gate A. Đọc [PROGRESS](PROGRESS.md), [EVIDENCE](EVIDENCE.md), [OPS](OPS.md) và [MIGRATION](MIGRATION.md) cùng runbook này. Nội dung AI viết **chưa đọc** trong [AI_WRITTEN](AI_WRITTEN.md).

## Phiên bản và phạm vi

- Repo gốc trên máy tác giả: `D:/Documents/coding/deadline-tracker`, nhánh `fix/p7-security-data-findings`, giữ sạch ở `d992edc4172afc8eff050312066a7b33af35b6b5`.
- Candidate: `D:/Documents/coding/deadline-tracker-p3-01`, nhánh `fix/p3-01-web-root-secret`. Trước finalization cùng HEAD nền và 21 đường dẫn sửa/thêm chưa commit; CP-006 snapshot182 sau overlay IR-01. Commit local mới bao gồm **toàn bộ triển khai candidate**, các docs có sẵn và năm docs finalization; dự kiến 22 changed paths so parent.
- Full new commit/parent, trạng thái Git, danh sách/hash source và kiểm gói nằm ở `release-manifest.json` cùng ZIP, tránh tự tham chiếu hash trong commit. Dùng manifest đó để định danh bản giao; không gọi riêng d992edc là source candidate đầy đủ.
- JAR gốc: `D:/Documents/coding/deadline-tracker-candidate-local/runtime/candidate.jar`; bản trong gói: `runtime/candidate.jar`. SHA256: `f69e177d406861ce6347dd824b5a01f85fa1a10bc7d0254ac67a3ba47254dc2a`. Copy nguyên byte, không build lại vì chỉ đổi docs. CP-006 đã so 66 main classes biên dịch mới với JAR.
- Code/resources/tests/scripts/build config và mọi tệp ngoài năm docs giữ raw bytes CP-006 trên worktree. Source trong gói lấy Git blob của commit mới; Git áp chuẩn hóa CRLF/LF đã có. Manifest phân biệt SHA256 raw worktree với SHA256 blob/source export, không gọi hai biểu diễn line ending là raw byte giống nhau.

## Dùng bộ launcher đã có trên chính máy này

Các lệnh dưới đây là **hướng dẫn cho người dùng**, không được thực thi trong đợt chốt. Thư mục bộ chạy: `D:/Documents/coding/deadline-tracker-candidate-local`. Mở Docker Desktop trước. Bộ này đã được chuẩn bị với JDK/image/DB/grants/secret/TLS/state riêng tại máy gốc.

| Thao tác | Tệp bấm trong thư mục launcher | Lệnh PowerShell tương đương |
|---|---|---|
| Mở candidate/browser | `Mo candidate.cmd` | `python 'D:/Documents/coding/deadline-tracker-candidate-local/candidate-control.py' start --open` |
| Xem trạng thái/URL | `Trang thai candidate.cmd` | `python 'D:/Documents/coding/deadline-tracker-candidate-local/candidate-control.py' status` |
| Dừng đúng candidate, giữ volume | `Dung candidate.cmd` | `python 'D:/Documents/coding/deadline-tracker-candidate-local/candidate-control.py' stop` |

URL candidate: **https://127.0.0.1:8443** (đăng ký tại `/register`). Bộ local cũ **http://127.0.0.1:8080** có app/DB khác; không dùng Compose stop/down để điều khiển nhầm bộ. Không có admin/mật khẩu ứng dụng mặc định; chỉ tạo tài khoản và dữ liệu giả, không dùng lại mật khẩu thật.

TLS là chứng chỉ tự ký cho loopback, có thời hạn 30 ngày theo hướng dẫn bộ chạy; không cài trust toàn hệ thống hoặc tự cấp lại. Chỉ cân nhắc ngoại lệ trình duyệt tại đúng địa chỉ loopback của candidate; nếu không được phép thì giữ nguyên bảo mật. Đợt này không kiểm lại browser/certificate/HTTP. Khi hết hạn cần xử lý riêng được giao, không sửa để lấy kiểm xanh.

`candidate-control.py` đối chiếu `settings.json` với **source_repo là repo gốc**, `source_pin=d992edc…`, `source_branch=fix/p7-security-data-findings` và repo gốc sạch, cùng JAR/cert/JDK/Docker/container/migration receipt. Commit candidate trên worktree P3 không thay precondition này. Không repoint launcher sang candidate commit mới, không sửa settings/secret/certificate/state hoặc runtime data. Nếu báo drift/cổng bận, giữ trạng thái và điều tra đúng lỗi; không repin, bỏ guard hoặc giết tiến trình khác.

Lệnh stop chỉ nhằm Java/container thuộc candidate và giữ volume theo controller; các kiểm identity có thể từ chối khi khác điều kiện máy. Đây là launcher **machine-specific**, không phải installer hoặc provisioner portable. File `HUONG-DAN.md` và artifact lịch sử trong bộ launcher giữ nguyên, có thể còn mô tả “candidate chưa commit” theo thời điểm cũ; bản giao hiện tại dùng manifest ngoài repo.

## Dựng mới trên máy khác

ZIP **không** chứa launcher/settings/secret/certificate private/state/DB của máy gốc. Không copy đường dẫn hoặc volume máy gốc để giả lập bộ cài. Cần người nhận chuẩn bị môi trường riêng:

1. Đọc `START-HERE.md`, `HANDOFF.md`, manifest/inventory và kiểm SHA256 trước. Git dùng được để verify/clone `repository.bundle`; full HEAD clone phải trùng new commit trong manifest. `source/` là export source/docs không `.git`, vì vậy không đủ một mình cho cổng migration/Git của check.
2. Prerequisites theo [README](../README.md): JDK **25 LTS** (bằng chứng cũ dùng **25.0.4.1+1**), Maven **3.9+** (CP-006 dùng **3.9.15**), Bash/Git Bash, Git, Docker Engine/Desktop Linux containers. Python 3 cần khi dùng các helper Python. Stack đã chọn Boot **4.1.1**, Tomcat **11.0.27**, MariaDB **11.8.9**, Testcontainers **2.0.5**, Flyway **12.4** theo pom/receipts; giữ pin/image digest trong source. Không nâng dependency/JDK/image trong scope này.
3. Toolchains, Docker image, Maven/NVD caches không có trong ZIP. Build/test mới cần tải dependencies/image từ nguồn tương ứng hoặc cache được chuẩn bị độc lập; offline `-o` chỉ chạy khi cache đủ. Không có cloud account/API key bắt buộc cho local. Không coi hash/package verification là dependency audit hoặc khả năng build offline trên mọi máy.
4. **Dev/Compose mới:** làm theo README; tạo `.env` riêng ngoài version control từ `.env.example`, dùng secret mới. Compose chỉ cho local và tự migrate dev, dùng app8080/DB3307. Kiểm cổng/volume/principal riêng trước khi chạy; không trỏ vào hai bộ đang dùng tại máy gốc. Những lệnh này chưa được chạy lại trong finalization.
5. **Prod/ops mới hoặc dùng JAR có sẵn:** tham khảo [DEPLOY](DEPLOY.md) và [MIGRATION](MIGRATION.md). JAR cần Java25 và DB MariaDB được provision riêng; không thể chạy một mình mà thiếu DB/config/secret. Migration là tiến trình riêng với MIGRATION_DB_*; web/ops chỉ DB_* runtime, không root/migration secret, prod/ops không auto-migrate. Ví dụ entry point với JAR đóng gói là `java -Duser.timezone=UTC -jar runtime/candidate.jar migrate`, rồi môi trường web riêng `java -Duser.timezone=UTC -jar runtime/candidate.jar` với profile/bind/TLS được người nhận chuẩn bị. Không đưa secret vào argv/history/log; runbook không tự cấp grants/certificate hoặc chứng nhận cấu hình mới.
6. DB đã migrate không được dựng lại/chạy lại V4/đổi installed_by để khớp migrator. Đối chiếu CURRENT_USER, installed_by và DEFINER; không khóa login principal còn làm runtime. Giữ definer tồn tại và đủ quyền trigger theo hai nhánh IR-01. Chưa kiểm grants/transport/server thật; migration hiện chỉ nhận URL loopback dạng số như MIGRATION mô tả.

Các bước dựng mới là hướng dẫn, không phải receipt đã thực thi trên máy nhận. Gói này phục vụ tiếp tục hoặc dựng lại có chuẩn bị; không hứa một lần bấm hoặc mang theo DB demo.

## Sao lưu, khôi phục và giữ dữ liệu

Xem [OPS](OPS.md) cho `scripts/backup.sh`, `scripts/verify-restore.sh`, verified retention/current pointer, quyền introspection/TRIGGER và reset/reconciliation tài khoản sau restore; xem [DEPLOY](DEPLOY.md) và MIGRATION cho backup trước migrate. Chuẩn bị secret và đường dẫn backup **ngoài repo/web root**; chỉ restore vào DB cách ly được giao, không restore đè nguồn hoặc tự xóa volume.

JAR/source/ZIP không phải bản sao DB. Stop/start giữ volume theo launcher, không thay backup. Chưa cài lịch backup/người phụ trách/offsite/alert mới, chưa đo RPO/RTO thật; không ghi đã có chỉ vì script/test lịch sử đạt. `RESTORE_OK` cũng chưa cho phép mở web: phải làm reset mọi tài khoản còn được phép và xác minh quyền theo OPS. Không tự sửa stale lock/current pointer hoặc xóa generation dư. Đợt chốt không chạy backup/restore mới và giữ nguyên mọi app/DB/grants/process.

## Bằng chứng và việc vẫn mở

| Mốc | Bằng chứng gốc | Giới hạn |
|---|---|---|
| P3-02 20/10 | [Biên bản](verification/p3-02-remediation.md): full 230 tests/29 classes, 0 failure/error/skip | Working-tree candidate khi đó; dependency audit bỏ qua, không test ở commit phát hành mới |
| Review độc lập 20/10 | REVIEW.md đã lưu trong CP-006/evidence và gói chọn lọc | Snapshot trước overlay IR-01; khuyến nghị DEFINER thấp. Không review overlay/commit mới hoặc ký Gate A |
| IR-01 21/10 | MIGRATION/DEPLOY/AI_WRITTEN, CP-003…005 | Kiểm wording/tĩnh, cùng tác giả, không grants/server test |
| CP-006 21/10 | REHEARSAL-REPORT, raw snapshot182/identity21, Maven/XML receipt, legacy ledger/log, class comparison | **8 JUnit đạt**, **29 probe checks** có lặp, **66 classes** khớp JAR; DB giả cách ly. Không phải 37 JUnit, không DB cũ thật |
| Finalization 21/10 | Manifest/checkpoints/verification trong gói | Docs/identity/raw-byte/commit/bundle/ZIP checks; không suite/rehearsal/app mới. Hash không phải chữ ký |

XML portable là projection bỏ properties/stdout, raw XML giữ local-only path/hash trong receipt. Những đường dẫn tuyệt đối trong bằng chứng gốc là provenance trên máy tác giả, không phụ thuộc được mang theo. Không recursively đóng gói isolated-source, checkpoint workspace, caches hoặc build outputs.

Runtime vẫn có **TRIGGER DDL**; effective privilege audit/provisioner, Flyway/MariaDB support/canonicalization, F-05/private-store durability/trust/capacity và owner residual decisions vẫn cần phạm vi riêng. Chưa audit SEC-13 mới, server/public HTTPS/proxy/firewall, vận hành backup thật, kiểm độc lập ở new commit hoặc pilot. US-20/21 còn phần vận hành; US-22/M7 cần nhóm thật 4–5 người trong hai tuần. AI-written/owner comprehension chưa được nhận thay; không đánh dấu mọi story/mốc học/Gate A hoàn tất.

Điểm dừng của đợt này là commit local và ZIP đã kiểm được bàn giao. Không push/merge/tag/deploy hoặc tự bắt đầu review/rehearsal kế tiếp.
