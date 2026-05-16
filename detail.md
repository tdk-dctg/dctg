# 📅 BidHub — Chi Tiết Từng Tuần & Giải Đáp Thắc Mắc

> Phần này đi sâu vào từng tuần, giải thích code và trả lời câu hỏi thường gặp.

---

## Tuần 1 — Thiết Lập Môi Trường

### Mục tiêu
Cả nhóm clone project về **build được ngay**, không cần cài đặt gì thêm.

### Ai làm gì?

| Thành viên | Công việc | Tại sao cần? |
|------------|-----------|--------------|
| **Đăng** | Tạo Maven multi-module project | Chia project thành 3 phần (server, client, common) để làm song song |
| **Đăng** | Tạo `ConfigLoader` | Đọc cấu hình từ file `.properties` — không hardcode port, đường dẫn DB |
| **Quốc Minh** | CI/CD (`ci.yml`), CONTRIBUTING.md, PR template, API_PROTOCOL.md | Quy chuẩn làm việc nhóm, tự động test khi push code |
| **Công Minh** | JavaFX skeleton + LoginView | Khung giao diện desktop đầu tiên |
| **Khoa** | JUnit 5 + CalculatorTest + STYLE_GUIDE.md | Hạ tầng test + quy tắc code |

### Câu hỏi thường gặp

**Q: Maven multi-module là gì?**
> Chia 1 project lớn thành nhiều module nhỏ. Mỗi module có `pom.xml` riêng, build độc lập. Giống như chia 1 cuốn sách thành nhiều chương — mỗi người viết 1 chương nhưng ghép lại thành 1 cuốn.

**Q: ConfigLoader dùng để làm gì?**
> Thay vì viết `port = 9090` cứng trong code, ta đọc từ file `server.properties`. Khi cần đổi port, chỉ sửa file config, không sửa code.

**Q: CI/CD là gì?**
> Continuous Integration — mỗi khi ai đó push code lên GitHub, hệ thống **tự động chạy test**. Nếu test fail → biết ngay code bị lỗi, không cần chờ đến lúc chạy thủ công.

---

## Tuần 2 — OOP Domain Model & Exception

### Mục tiêu
Xây dựng **mô hình dữ liệu** (các class đại diện cho User, Item, Auction) và **hệ thống xử lý lỗi**.

### Ai làm gì?

| Thành viên | Công việc |
|------------|-----------|
| **Đăng** | `Entity` (base class), `User` → `Bidder`/`Seller`/`Admin`, `UserRole` |
| **Quốc Minh** | `Item` → `Electronics`/`Art`/`Vehicle`, `ItemType`, `Displayable`, Factory Method |
| **Công Minh** | `Auction`, `AuctionStatus` (State Machine), `BidTransaction` |
| **Khoa** | `BidHubException` + 7 exception con, `ValidationException`, test tích hợp |

### Các khái niệm OOP áp dụng

| Khái niệm | Ở đâu trong code | Giải thích |
|------------|-------------------|------------|
| **Abstraction** (Trừu tượng) | `Entity`, `User`, `Item` là `abstract` | Không thể tạo `new Entity()` trực tiếp — phải tạo `Bidder`, `Seller`... |
| **Inheritance** (Kế thừa) | `Bidder extends User`, `Electronics extends Item` | Lớp con kế thừa mọi thứ từ lớp cha + thêm đặc thù riêng |
| **Polymorphism** (Đa hình) | `user.getInfo()` trả về khác nhau tuỳ Bidder/Seller/Admin | Cùng 1 method nhưng hành vi khác nhau tuỳ loại object |
| **Encapsulation** (Đóng gói) | Mọi field là `private`, chỉ expose qua getter | Bảo vệ dữ liệu — không ai sửa trực tiếp được |
| **Interface** | `Displayable` | Ép mọi Item phải có method `printInfo()` |

### Câu hỏi thường gặp

**Q: Tại sao Entity là abstract?**
> Vì Entity chỉ là "khái niệm chung" — không ai tạo "một Entity" trong đời thực. Chỉ có Bidder, Auction, Item... Từ khoá `abstract` ngăn chặn `new Entity()`.

**Q: Factory Method Pattern là gì?**
> Thay vì viết `if type == "ART" then new Art()`, ta tạo class `ArtCreator` riêng. Khi cần thêm loại mới (VD: RealEstate), chỉ tạo thêm `RealEstateCreator` mà **không sửa code cũ**.

**Q: Tại sao cần cây Exception riêng?**
> Để server trả về **mã lỗi rõ ràng**. VD: `InvalidBidException` → "giá không hợp lệ", `AuctionClosedException` → "phiên đã đóng". Client nhận biết chính xác lỗi gì để hiển thị cho user.

---

## Tuần 3 — Database & DAO Layer

### Mục tiêu
Kết nối **SQLite database** và tạo lớp truy cập dữ liệu (DAO).

### Cấu trúc bảng database

| Bảng | Mô tả | Cột chính |
|------|-------|-----------|
| `users` | Người dùng | id, username, password_hash, email, role, is_locked |
| `items` | Sản phẩm | id, name, description, starting_price, seller_id, item_type |
| `auctions` | Phiên đấu giá | id, item_id, start_time, end_time, status, current_highest_bid |
| `bid_transactions` | Lịch sử đặt giá | id, auction_id, bidder_id, bid_amount, bid_time |
| `audit_logs` | Nhật ký hệ thống | id, user_id, action, details, created_at |

### DAO Pattern là gì?
**Data Access Object** — mỗi bảng có 1 class DAO tương ứng:
- `UserDao` → đọc/ghi bảng `users`
- `ItemDao` → đọc/ghi bảng `items`
- `AuctionDao` → đọc/ghi bảng `auctions`
- `BidDao` → đọc/ghi bảng `bid_transactions`

**Tại sao cần DAO?** → Tách biệt code SQL khỏi logic nghiệp vụ. Service layer gọi `userDao.findByUsername("alice")` mà không cần biết SQL bên trong.

---

## Tuần 4 — TCP Socket Server

### Mục tiêu
Client kết nối được tới Server qua mạng, gửi/nhận tin nhắn JSON.

### Cách hoạt động

```
1. Server khởi động → lắng nghe cổng 9090
2. Client kết nối → Server tạo 1 thread riêng cho client đó
3. Client gửi JSON → Server parse → xử lý → trả JSON
4. Client ngắt kết nối → Server dọn dẹp thread
```

### Các class quan trọng

| Class | Vai trò |
|-------|---------|
| `SocketServerCore` | Lắng nghe kết nối mới, tạo thread pool 30 thread |
| `ClientConnectionThread` | Xử lý 1 client: đọc JSON → gọi RequestHandler → gửi response |
| `Session` | Quản lý kết nối của 1 client (socket, sessionId, userId) |
| `RequestHandler` | Nhận JSON → phân loại command → gọi handler tương ứng |

### Câu hỏi thường gặp

**Q: Thread pool 30 nghĩa là gì?**
> Tối đa 30 client được xử lý đồng thời. Client thứ 31 phải chờ. Tránh tạo quá nhiều thread gây tràn bộ nhớ.

**Q: JSON là gì?**
> Format dữ liệu dạng text mà cả người và máy đều đọc được. VD: `{"type":"PING"}`. Client và Server "nói chuyện" bằng JSON.

---

## Tuần 5 — Đăng ký / Đăng nhập / Quản lý sản phẩm

### Mục tiêu
User đăng ký, đăng nhập, tạo/xem/xoá sản phẩm.

### Luồng xác thực (Authentication Flow)

```
1. Client gửi REGISTER → Server hash password (SHA-256) → lưu DB
2. Client gửi LOGIN → Server kiểm tra password → tạo token UUID → trả về client
3. Client gửi mọi request kèm token → Server kiểm tra token hợp lệ
4. Client gửi LOGOUT → Server xoá token
```

### Tại sao dùng Token?
> Giống như vé vào cổng. Sau khi đăng nhập, server phát cho bạn 1 "vé" (token). Mỗi lần gửi yêu cầu, bạn phải trình "vé" này. Khi đăng xuất, "vé" bị huỷ.

### Auth Guard là gì?
> Một số lệnh (CREATE_ITEM, PLACE_BID...) yêu cầu đăng nhập trước. `AUTH_REQUIRED` set chứa danh sách các lệnh này. Nếu gửi lệnh mà chưa đăng nhập → server từ chối ngay.

---

## Tuần 6 — Đấu Giá & Quản Trị

### Mục tiêu
Tạo phiên đấu giá, đặt giá, admin quản lý user.

### BidValidator — 5 điều kiện đặt giá

Trước khi chấp nhận 1 bid, hệ thống kiểm tra:

| # | Điều kiện | Lỗi nếu vi phạm |
|---|-----------|------------------|
| 1 | Auction phải đang RUNNING | `AuctionClosedException` |
| 2 | Không phải người dẫn đầu hiện tại | `InvalidBidException` — bạn đang dẫn đầu rồi |
| 3 | Không phải seller của sản phẩm | `InvalidBidException` — seller không tự đấu giá |
| 4 | Giá > giá hiện tại | `InvalidBidException` — giá quá thấp |
| 5 | Bước giá ≥ minimumIncrement | `InvalidBidException` — tăng không đủ |

### AuctionManager (Singleton)
- Lưu tất cả auction đang OPEN/RUNNING trong **RAM** (bộ nhớ)
- Dùng `ConcurrentHashMap` — an toàn khi nhiều thread truy cập
- `ScheduledExecutorService` chạy `AuctionLifecycleTask` mỗi **5 giây** để:
  - Chuyển OPEN → RUNNING khi đến giờ
  - Chuyển RUNNING → FINISHED khi hết giờ

### AdminUserService
- `listAllUsers()` — xem danh sách user
- `lockUser(targetId, adminId)` — khoá tài khoản (không cho đăng nhập)
- `unlockUser(targetId, adminId)` — mở khoá
- **Không thể khoá Admin** — bảo vệ tài khoản quản trị

---

## Tuần 7 — Realtime & Báo Cáo

### Mục tiêu
Thông báo realtime khi có bid mới, báo cáo, anti-sniping, kiểm tra dữ liệu.

### NotificationBroker (Observer Pattern)

```
1. Client gửi SUBSCRIBE_AUCTION → đăng ký theo dõi phiên
2. Khi có bid mới → NotificationBroker push BID_UPDATE đến TẤT CẢ client đang subscribe
3. Khi phiên kết thúc → push AUCTION_CLOSED
4. Khi client ngắt kết nối → tự động unsubscribe
```

**Tại sao dùng Observer Pattern?**
> Giống như đăng ký nhận thông báo YouTube. Bạn "subscribe" một kênh (auction), khi có video mới (bid mới), YouTube tự động thông báo cho bạn.

### Anti-Sniping Engine

| Tham số | Giá trị | Giải thích |
|---------|---------|------------|
| `snipe.threshold` | 60 giây | Khoảng thời gian "nguy hiểm" trước khi phiên kết thúc |
| `snipe.extension` | 60 giây | Thời gian gia hạn thêm |

**Cách hoạt động:** Nếu ai đó đặt giá trong **60 giây cuối** → phiên tự động **gia hạn thêm 60 giây**. Cho tất cả người tham gia có cơ hội phản hồi.

### ReportService

| Method | Chức năng |
|--------|-----------|
| `exportAuctionReport()` | Xuất báo cáo tất cả phiên đấu giá |
| `exportBidHistory(auctionId)` | Lịch sử đặt giá của 1 phiên |
| `exportAuditLog(limit)` | Nhật ký hệ thống gần đây |

### DataIntegrityService — Kiểm tra toàn vẹn dữ liệu

| Check | Mục đích |
|-------|----------|
| `checkBidConsistency()` | So sánh giá cao nhất trong bảng auctions vs MAX(bid) trong bid_transactions |
| `checkAuctionWinners()` | Phiên FINISHED có bids nhưng chưa xác định winner |
| `checkOrphanedItems()` | Sản phẩm có sellerId không tồn tại trong bảng users |

---

## Tuần 8+ — Client GUI & Tích Hợp

### Mục tiêu
Hoàn thiện giao diện JavaFX, tích hợp toàn bộ tính năng.

### Client Architecture

| Package | Nội dung |
|---------|----------|
| `controller` | Các controller cho từng màn hình FXML |
| `navigation` | Điều hướng giữa các màn hình |
| `network` | Kết nối TCP socket đến server |
| `service` | Logic phía client |
| `util` | Tiện ích |

---

## Tổng Kết Kỹ Thuật Java Đã Dùng

| Kỹ thuật | Nơi sử dụng |
|----------|-------------|
| **Abstract class** | Entity, User, Item, ItemCreator |
| **Interface** | Displayable |
| **Enum với abstract method** | AuctionStatus |
| **Generics** | `Optional<User>`, `List<Auction>`, `Map<String, Object>` |
| **Stream API** | Xử lý danh sách trong DAO/Service |
| **ConcurrentHashMap** | SessionManager, AuctionManager, NotificationBroker |
| **ReentrantLock** | Auction (lock khi bid/close) |
| **ScheduledExecutorService** | AuctionManager (lifecycle task 5s) |
| **CopyOnWriteArrayList** | NotificationBroker (subscriber list) |
| **SHA-256** | AuthService (hash password) |
| **UUID** | Entity (id), AuthService (token) |
| **Jackson** | MessageMapper (serialize/deserialize JSON) |
| **SLF4J + Logback** | Logging toàn hệ thống |
| **JUnit 5** | Unit test + Integration test |
| **SQLite + JDBC** | Database |

---

> **Nếu bạn có câu hỏi cụ thể về tuần nào hoặc phần nào, hãy hỏi thêm!**
