Dưới đây là **câu trả lời cho 28 câu hỏi phòng vệ**, được đối chiếu với nội dung `REPORT.pdf` và toàn bộ kiến thức về dự án BidHub (các Part 1–14). Mỗi câu trả lời đều chỉ rõ lý do thiết kế, cơ chế hoạt động và dẫn chứng code (tên class, method, luồng xử lý).

---

## 1. Kiến trúc tổng thể & luồng xử lý yêu cầu

### 1. Giải thích kiến trúc 5 lớp của hệ thống. Mỗi lớp có nhiệm vụ gì? Lợi ích của việc tách riêng Business Logic và Data Access? Chỉ ra đúng tên package/class.

**Trả lời:**  
Hệ thống được tổ chức thành 5 tầng (theo sơ đồ Báo cáo và Part 3 – Mục 1.1):

| Tầng | Package / Class | Nhiệm vụ |
|------|----------------|----------|
| **Presentation** (Client) | `bidhub-client/controller/`, `view/`, `ViewRouter` | Giao diện JavaFX FXML, điều hướng, bắt sự kiện người dùng. |
| **Network** | `bidhub-server/network/` (SocketServerCore, ClientConnectionThread, RequestHandler, 5 Handlers) | Nhận request TCP, parse JSON, dispatch, gửi response; Socket 2 push event. |
| **Business Logic** | `bidhub-server/service/` (11 services), `BidValidator`, `SecurityContext` | Xử lý nghiệp vụ: validate bid, anti‑sniping, lifecycle, tính tiền, phân quyền. |
| **Data Access** | `bidhub-server/dao/` (5 DAO) | CRUD với SQLite, map giữa ResultSet và Entity, PreparedStatement chống SQL injection. |
| **Database** | SQLite (file `data/bidhub.db`) | Lưu trữ bền vững, WAL mode cho phép đọc ghi đồng thời. |

**Lợi ích tách Business Logic và Data Access:**  
- Thay đổi câu truy vấn, thêm index, chuyển từ SQLite sang MySQL chỉ cần sửa DAO, không ảnh hưởng service.  
- Có thể test service với mock DAO (hoặc in‑memory DB) mà không cần DB thật.  
- Business logic thuần túy Java, không pha trộn SQL → dễ đọc, dễ bảo trì.

*Dẫn chứng code:*  
- Business logic: `BidValidator.validate()` (6 nhánh kiểm tra).  
- DAO: `UserDao.findByUsername()` dùng `PreparedStatement`.  
- Network: `RequestHandler.handle()` với switch 33 type.

---

### 2. Double‑Socket hoạt động thế nào? Tại sao không dùng polling hoặc một socket duy nhất? Socket nào xử lý giao dịch đồng bộ, socket nào đẩy sự kiện? Dẫn chứng code khởi tạo hai socket.

**Trả lời:**  

- **Socket 1 (đồng bộ)**: `ServerGateway` (client) và `ClientConnectionThread` (server) – dùng cho request/response (LOGIN, PLACE_BID, GET_LIST…). Client gửi request, chờ response trên cùng socket này.  
- **Socket 2 (bất đồng bộ)**: `EventListenerThread` (client) và `NotificationBroker` + `Session` (server) – chỉ dùng để server **push event** (`BID_UPDATE`, `AUCTION_CLOSED`, `AUCTION_EXTENDED`). Client không gửi request qua socket này, chỉ đọc.

**Tại sao không dùng polling?**  
Polling (client gửi `GET_STATUS` mỗi 2s) gây 500 request/giây, độ trễ 2s, không realtime, tốn tài nguyên.  
**Tại sao không dùng một socket duy nhất?**  
Nếu chỉ một socket, event có thể xen vào lúc client đang chờ response của `PLACE_BID` → client đọc được event thay vì response, parse JSON lỗi.

**Dẫn chứng code khởi tạo:**  
- Client: `ServerGateway.connect()` tạo Socket 1; `EventListenerThread` tạo Socket 2 riêng khi vào `AuctionDetailController.subscribeToAuction()`.  
- Server: `SocketServerCore.start()` – `ServerSocket.accept()` chấp nhận mọi kết nối, mỗi kết nối tạo `Session`. `NotificationBroker.publish()` ghi vào danh sách subscriber và gửi qua socket tương ứng.

---

### 3. Mô tả luồng xử lý một request đặt giá (PLACE_BID) từ lúc client nhấn nút đến khi client nhận được phản hồi. Đi qua những tầng nào? Có những class nào tham gia? Đâu là điểm đồng bộ hóa (lock) để tránh lost update?

**Trả lời:**  
Luồng `PLACE_BID` (đã được mô tả trong Part 3, Part 5, Part 10):

1. **Client (Presentation)**: `AuctionDetailController.placeBid()` đọc `tfBidAmount`, tạo `MessageRequest` với type `PLACE_BID`, gọi `ServerGateway.sendRequest()` thông qua `NetworkTask` (background thread).
2. **Network (Server)**: `ClientConnectionThread` đọc dòng JSON → `RequestHandler.handle()` parse, kiểm tra auth, switch đến `AuctionHandler.handlePlaceBid()`.
3. **Business Logic**:  
   - `SecurityContext.requireAuthenticated()` → lấy userId.  
   - `AuctionManager.getAuction()` → lấy `Auction` object từ RAM cache.  
   - **Điểm đồng bộ hóa (lock)**: `auction.getLock().lock()` → chỉ một thread được vào critical section cho auction này.  
   - `BidValidator.validate()` (5 quy tắc) – ném exception nếu không hợp lệ.  
   - Bắt đầu JDBC transaction (`setAutoCommit(false)`).  
   - `BidDao.save(bid)` (INSERT), `AuctionDao.updateHighestBid(...)` (UPDATE), `AuditLogDao.save(log)` (INSERT).  
   - `txConn.commit()` (hoặc rollback nếu lỗi).  
   - `antiSnipingEngine.check(auction)` (vẫn trong lock).  
   - `auction.getLock().unlock()` (finally block).
4. **Sau lock**: `NotificationBroker.publish(BidUpdateEvent)` (gửi event qua Socket 2 cho tất cả subscriber).
5. **Response** quay lại client qua Socket 1.

**Điểm đồng bộ hóa:** `ReentrantLock` per auction, đảm bảo không có hai thread cùng cập nhật `currentHighestBid` trên cùng một phiên.

*Dẫn chứng code:*  
- `AuctionHandler.handlePlaceBid()` có `auction.getLock().lock(); try { ... } finally { lock.unlock(); }`.  
- `BidValidator.validate()` ném `InvalidBidException` nếu vi phạm.

---

## 2. OOP, kế thừa & đa hình

### 4. Cây kế thừa của `User` và `Item`. Vẽ sơ đồ hoặc liệt kê các lớp con. Cho ví dụ tính đa hình: `List<Item>` chứa cả Electronics, Art, Vehicle và gọi `getInfo()`.

**Trả lời:**  

**Cây kế thừa User:**  
`Entity` (abstract) → `User` (abstract) → `Bidder`, `Seller`, `Admin` (final).  

**Cây kế thừa Item:**  
`Entity` → `Item` (abstract) → `Electronics`, `Art`, `Vehicle` (final).  

**Ví dụ đa hình:**  
```java
List<Item> items = new ArrayList<>();
items.add(new Electronics("MacBook", ...));
items.add(new Art("Mona Lisa", ...));
items.add(new Vehicle("Toyota", ...));

for (Item item : items) {
    System.out.println(item.getInfo()); // mỗi lớp con override getInfo() khác nhau
}
```
- `Electronics.getInfo()` in ra brand, warranty.  
- `Art.getInfo()` in ra artist, yearCreated.  
- `Vehicle.getInfo()` in ra manufacturer, year, mileage.  

*Dẫn chứng code:*  
Trong `ItemHandler.enrichItems()`, `item.getCategoryDetails()` được gọi mà không cần `instanceof`.

---

### 5. `AuctionStatus` triển khai State Pattern. Chỉ ra 3 method thể hiện hành vi phụ thuộc trạng thái. Nếu thêm trạng thái `PAUSED`, cần sửa những chỗ nào?

**Trả lời:**  

`AuctionStatus` là enum với các abstract method:  
- `canBid()`: chỉ `RUNNING` trả về `true`.  
- `isTerminal()`: `PAID` và `CANCELED` trả về `true`.  
- `canTransitionTo(AuctionStatus target)`: định nghĩa chuyển đổi hợp lệ (OPEN → RUNNING/CANCELED, RUNNING → FINISHED, FINISHED → PAID/CANCELED).

**Thêm trạng thái `PAUSED`:**  
- Thêm hằng số `PAUSED` vào enum.  
- Override `canBid()` → trả `false` (không được đặt giá khi tạm dừng).  
- Override `isTerminal()` → `false` (chưa kết thúc).  
- Cập nhật `canTransitionTo()`: cho phép `RUNNING → PAUSED` và `PAUSED → RUNNING`.  
- Sửa `AuctionLifecycleTask` nếu cần tự động chuyển từ PAUSED dựa trên thời gian.  
- Sửa logic business (nếu có yêu cầu đặc biệt).  

*Dẫn chứng code:*  
File `AuctionStatus.java` trong `bidhub-server/model/`.

---

## 3. Design Pattern (9 mẫu)

### 6. Singleton dùng ở 7 nơi. Lấy 2 ví dụ: `AuctionManager` và `NotificationBroker`. Code khởi tạo có dùng double‑checked locking? Tại sao cần `volatile`? Hậu quả nếu không dùng Singleton?

**Trả lời:**  

**Ví dụ 1: `AuctionManager`**  
```java
private static volatile AuctionManager instance;
public static AuctionManager getInstance() {
    if (instance == null) {
        synchronized (AuctionManager.class) {
            if (instance == null) {
                instance = new AuctionManager();
            }
        }
    }
    return instance;
}
```
- **Double‑checked locking**: có, để tránh synchronized mỗi lần gọi getInstance() (chỉ lock lần đầu tạo).  
- **`volatile`**: ngăn instruction reordering – đảm bảo instance được gán sau khi constructor hoàn tất, các thread khác nhìn thấy object fully constructed.

**Ví dụ 2: `NotificationBroker`** – tương tự.

**Hậu quả nếu không dùng Singleton:**  
- `AuctionManager`: mỗi nơi gọi `new AuctionManager()` sẽ tạo cache RAM riêng → auction được thêm vào instance này nhưng không thấy ở instance khác → mất đồng bộ, race condition.  
- `NotificationBroker`: nhiều instance → subscriber đăng ký ở instance A nhưng event publish ở instance B → không nhận được thông báo.

*Dẫn chứng code:*  
Xem `AuctionManager.java`, `NotificationBroker.java` trong `bidhub-server/service/`.

---

### 7. Observer Pattern cho realtime update. Ai là Subject? Ai là Observer? Cơ chế đăng ký và thông báo. Chỉ ra dòng code gửi `BidUpdateEvent` và dòng code client nhận sự kiện.

**Trả lời:**  

- **Subject**: `NotificationBroker` (Singleton).  
- **Observer**: `Session` (mỗi client có một Session cho Socket 2).  

**Cơ chế:**  
- Client gửi `SUBSCRIBE_AUCTION` → server gọi `NotificationBroker.subscribe(auctionId, session)`.  
- `NotificationBroker` lưu `Map<String, CopyOnWriteArrayList<Session>>`.  
- Khi có `BidUpdateEvent`: `NotificationBroker.publish(auctionId, event)` → duyệt danh sách session → gọi `session.sendMessage(eventJson)` (qua Socket 2).

**Dòng code gửi event:**  
Trong `AuctionHandler.handlePlaceBid()`, sau khi unlock:  
```java
NotificationBroker.getInstance().publish(auctionId, new BidUpdateEvent(auctionId, userId, bidderName, bidAmount, LocalDateTime.now()));
```

**Dòng code client nhận event:**  
`EventListenerThread` (chạy background) đọc `reader.readLine()` → parse JSON → gọi `callback.onBidUpdate(eventJson)`. Trong `AuctionDetailController`:  
```java
eventListener = new EventListenerThread(reader, eventJson -> {
    Platform.runLater(() -> {
        if ("BID_UPDATE".equals(eventType)) {
            // cập nhật giá, chart
        }
    });
});
```

*Dẫn chứng code:*  
`NotificationBroker.java`, `EventListenerThread.java`, `AuctionDetailController.subscribeToAuction()`.

---

### 8. Factory Method để tạo `Item`. Lớp `ItemCreator` và các subclass nằm ở đâu? Muốn thêm loại `Furniture` cần viết class mới và sửa chỗ nào?

**Trả lời:**  

`ItemCreator` là abstract class trong `bidhub-server/model/` (hoặc `service/`). Các subclass: `ElectronicsCreator`, `ArtCreator`, `VehicleCreator`.  

**Thêm loại `Furniture`:**  
1. Tạo `Furniture` extends `Item`, thêm các field riêng (ví dụ: `material`, `dimensions`).  
2. Tạo `FurnitureCreator` extends `ItemCreator`, override `createItem()` và dùng `requireString()`/`requireInt()` để lấy field.  
3. Sửa `ItemType` enum thêm `FURNITURE`.  
4. Sửa `ItemCreator.forType()` switch thêm case `FURNITURE -> new FurnitureCreator()`.  
5. Sửa `ItemDao.mapRow()` và `buildExtras()` để xử lý Furniture.  
**Không cần sửa các Creator cũ** → tuân thủ Open/Closed Principle.

*Dẫn chứng code:*  
`ItemCreator.java`, `ElectronicsCreator.java` trong `bidhub-server/model/`.

---

## 4. Xử lý đồng thời & đặt giá an toàn

### 9. Cơ chế khóa mịn (fine‑grained lock) với `ReentrantLock`. Trình bày đoạn code `placeBid()`: khóa lấy trên đối tượng nào? unlock ở đâu? Tại sao audit log và publish event lại ngoài lock? Chỉ ra mã nguồn.

**Trả lời:**  

Trong `AuctionHandler.handlePlaceBid()`:  
```java
auction.getLock().lock();   // khóa trên đối tượng Auction cụ thể
try {
    // validate, DB transaction (save bid, update highest bid, audit log)
    txConn.commit();
    antiSnipingEngine.check(auction);
} finally {
    auction.getLock().unlock();   // unlock trong finally để đảm bảo luôn được thả
}
// SAU unlock:
NotificationBroker.getInstance().publish(...);
```

**Tại sao audit log và publish event ở ngoài lock?**  
- Ghi log (I/O) và gửi event (socket I/O) có thể chậm.  
- Nếu giữ lock trong khi gửi event, các thread khác muốn đặt giá trên cùng auction sẽ bị chờ lâu → giảm throughput.  
- Thả lock trước khi publish → các bid sau có thể xử lý ngay, event gửi song song.  

*Dẫn chứng code:*  
`AuctionHandler.handlePlaceBid()` (trong `bidhub-server/network/handler/`).

---

### 10. Test đồng thời (ConcurrentBidTest). Chạy bao nhiêu luồng? Kiểm tra điều gì? Nếu chạy 100 luồng có cần sửa lock không?

**Trả lời:**  

`ConcurrentBidTest` (trong `bidhub-server/test/`) chạy **50 luồng** cùng lúc trên cùng một auction.  
Nó kiểm tra:  
- Chỉ đúng một bid thành công (winner).  
- Không có lost update (giá cuối cùng đúng với bid cao nhất).  
- Không có deadlock (tất cả luồng kết thúc).  

**Nếu chạy 100 luồng:**  
Với cơ chế `ReentrantLock` per auction, 100 luồng vẫn an toàn (lock chỉ chặn truy cập vào critical section, các luồng còn lại xếp hàng chờ). Tuy nhiên cần đảm bảo transaction timeout không quá ngắn và thread pool đủ lớn (FixedThreadPool 30 sẽ xếp hàng, không sao). Không cần sửa logic lock, chỉ có thể tăng `server.poolSize` nếu muốn xử lý nhiều hơn đồng thời.

*Dẫn chứng code:*  
`ConcurrentBidTest.java` – sử dụng `CyclicBarrier` để đồng bộ 50 luồng cùng bắt đầu.

---

## 5. Anti‑Sniping & vòng đời tự động

### 11. Thuật toán gia hạn thời gian (Anti‑Sniping). Giải thích công thức `snipeWindow = endTime - thresholdSeconds`. Tại sao dùng `newEndTime = oldEndTime + extensionSeconds` thay vì `now + extensionSeconds`? Tìm code cập nhật `end_time` trong DB.

**Trả lời:**  

**Công thức:**  
`snipeWindow = endTime - thresholdSeconds` (ví dụ 60s). Nếu `now >= snipeWindow` thì bid nằm trong vùng “nguy hiểm” → cần gia hạn.

**Tại sao cộng vào `oldEndTime` thay vì `now`?**  
- Giả sử 3 người đặt liên tiếp trong 2 giây cuối:  
  - Nếu dùng `now + 60s`: mỗi lần gia hạn ≈60s từ thời điểm hiện tại → tổng gia hạn chỉ hơn 60s một chút, không đủ thời gian phản ứng.  
  - Nếu dùng `oldEndTime + 60s`: lần 1 endTime = old+60, lần 2 endTime = (old+60)+60 = old+120 → đảm bảo mỗi bid trong vùng nhạy cảm đều thêm đúng 60s vào thời điểm kết thúc thực tế.

**Code cập nhật DB:**  
Trong `AntiSnipingEngine.check()`:  
```java
auctionDao.updateEndTime(auction.getId(), newEndTime);
```
`AuctionDao.updateEndTime()` thực thi SQL:  
```sql
UPDATE auctions SET end_time = ?, updated_at = ? WHERE id = ?
```

*Dẫn chứng code:*  
`AntiSnipingEngine.java`, `AuctionDao.java`.

---

### 12. `AuctionLifecycleTask` chạy mỗi 5 giây. Nó làm gì với các phiên RUNNING? Nếu một phiên vừa hết hạn trong lúc task đang chạy, có nguy cơ xử lý hai lần không? Dùng lock toàn cục hay lock riêng từng phiên?

**Trả lời:**  

`AuctionLifecycleTask.run()` lấy danh sách active auction từ `AuctionManager.getAllActive()`, duyệt từng phiên:  
- Nếu `OPEN` và `startTime <= now` → chuyển `RUNNING`.  
- Nếu `RUNNING` và `endTime < now` → gọi `closeAuction(auction)`.

**Nguy cơ xử lý hai lần không?**  
Trong `closeAuction()` có `auction.getLock().lock()` trước khi kiểm tra status lần nữa. Nếu phiên đã được đóng bởi một lifecycle task khác (hoặc do admin), status sẽ không còn `RUNNING` → bỏ qua. Không có lock toàn cục – mỗi phiên có lock riêng, nên các phiên khác không bị ảnh hưởng.

*Dẫn chứng code:*  
`AuctionLifecycleTask.java` – vòng lặp gọi `closeAuction()` bên trong `try-catch` riêng cho từng auction (fault isolation).

---

## 6. Realtime update & Bid History Visualization

### 13. Bid chart (LineChart) cập nhật realtime. Làm thế nào để tải lịch sử bid khi mở màn hình? Tại sao cần `Platform.runLater()`? Nếu không dùng sẽ gặp lỗi gì? Chỉ ra callback thêm điểm dữ liệu.

**Trả lời:**  

Khi mở `AuctionDetailView`, controller gọi `GET_AUCTION_DETAIL` (hoặc `GET_BID_HISTORY_REPORT`). Response chứa `bidHistory` mảng các bid cũ. Controller lặp qua và gọi `bidChartService.addDataPoint(time, price, bidderName)` để vẽ baseline.

**Tại sao cần `Platform.runLater()`?**  
`EventListenerThread` nhận event trên background thread (không phải JavaFX Application Thread). JavaFX chỉ cho phép sửa UI (Label, LineChart) trên FX thread. Nếu cập nhật trực tiếp từ background thread → `IllegalStateException: Not on FX application thread`.

**Callback thêm điểm dữ liệu:**  
Trong `AuctionDetailController` khi nhận `BID_UPDATE`:  
```java
Platform.runLater(() -> {
    bidChartService.addDataPoint(LocalDateTime.now(), newPrice, bidderName);
});
```
`BidChartService.addDataPoint()` thêm `XYChart.Data` vào `ObservableList` → LineChart tự động vẽ lại.

*Dẫn chứng code:*  
`BidChartService.java`, `AuctionDetailController.subscribeToAuction()`.

---

### 14. Kênh thông báo admin (broadcast) và kênh realtime khác nhau thế nào? Giải thích lý do không dùng push cho cả hai. Code lưu broadcast vào đâu? Client gọi API nào để lấy?

**Trả lời:**  

- **Kênh realtime (push)** dùng cho `BID_UPDATE`, `AUCTION_CLOSED`, `AUCTION_EXTENDED` – yêu cầu độ trễ thấp (<5ms). Server push qua Socket 2.  
- **Kênh admin broadcast** dùng để gửi thông báo hệ thống (không cần realtime). Admin gọi `SEND_NOTIFICATION` → server lưu vào `audit_logs` với action `BROADCAST_NOTIFICATION` và details JSON. Client định kỳ gọi `GET_NOTIFICATIONS` (polling) để lấy danh sách.  

**Lý do không dùng push cho broadcast:**  
- Broadcast không cần độ trễ thấp (người dùng đọc khi mở tab notification).  
- Push cho tất cả user (có thể hàng trăm) khi admin gửi một thông báo sẽ tạo burst network I/O. Polling đơn giản hơn, ít tác động đến hệ thống.

**Code lưu broadcast:** `AdminHandler.handleSendNotification()` gọi `auditLogService.log(adminId, "BROADCAST_NOTIFICATION", json)`.  
**Client lấy:** `GET_NOTIFICATIONS` → `AdminHandler.handleGetNotifications()` đọc `audit_logs` và enrich với trạng thái đọc (lưu trong `ConcurrentHashMap`).

*Dẫn chứng code:*  
`AdminHandler.java`, `AuditLogService.java`.

---

## 7. Bảo mật, phân quyền & audit log

### 15. Cơ chế xác thực hai lớp. Lớp 1: kiểm tra token, lớp 2: `SecurityContext.requireRole()`. Chỉ ra endpoint `ADMIN_STOP_AUCTION` kiểm tra vai trò thế nào. Tại sao dùng `MessageDigest.isEqual()` thay `String.equals()` cho mật khẩu?

**Trả lời:**  

**Hai lớp xác thực:**  
1. **Lớp 1 (RequestHandler)**: kiểm tra token có nằm trong `AUTH_REQUIRED` set và session đã authenticated chưa. Nếu chưa → trả lỗi “Chưa đăng nhập”.  
2. **Lớp 2 (SecurityContext)**: bên trong handler, gọi `SecurityContext.requireRole(session, UserRole.ADMIN)` để kiểm tra quyền cụ thể.

**Ví dụ `ADMIN_STOP_AUCTION`:**  
Trong `AdminHandler.handleAdminStopAuction()`:  
```java
String adminId = SecurityContext.requireRole(session, UserRole.ADMIN);
```
Nếu không phải ADMIN, ném `AuthenticationException` → `RequestHandler` catch và trả error JSON.

**`MessageDigest.isEqual()` vs `String.equals()`:**  
`String.equals()` short‑circuit: dừng khi gặp ký tự khác → thời gian phản hồi phụ thuộc vào vị trí ký tự sai → timing attack.  
`MessageDigest.isEqual()` luôn so sánh toàn bộ mảng byte → thời gian không đổi → an toàn.

*Dẫn chứng code:*  
`SecurityContext.java`, `AuthService.verifyPassword()`.

---

### 16. Audit Log chịu lỗi (non‑blocking). Khi ghi log thất bại, hệ thống có ném ngoại lệ làm hỏng giao dịch không? Chỉ ra cách xử lý ngoại lệ. Liệt kê 3 hành vi được ghi log.

**Trả lời:**  

`AuditLogService.log()` bao bọc toàn bộ trong `try-catch` và **không ném exception ra ngoài**:  
```java
try {
    auditLogDao.save(new AuditLog(userId, action, details));
} catch (Exception e) {
    logger.error("Khong the ghi log", e);   // chỉ ghi log nội bộ, không throw
}
```
=> Dù log lỗi (đĩa đầy, connection fail), giao dịch đặt giá vẫn thành công.  

**3 hành vi được ghi log:**  
1. `USER_LOGIN`, `USER_LOGOUT` (xác thực).  
2. `PLACE_BID` (mỗi lần đặt giá).  
3. `AUCTION_CLOSED`, `AUCTION_EXTENDED` (vòng đời).  

*Dẫn chứng code:*  
`AuditLogService.java`, các `AuditActions` constants.

---

## 8. Data Integrity & Admin Dashboard

### 17. `DataIntegrityService` kiểm tra nhất quán dữ liệu. Kể tên 3 kiểm tra chéo. Tại sao chỉ đọc mà không tự động sửa? Rủi ro nếu auto‑fix?

**Trả lời:**  

3 kiểm tra:  
1. **checkBidConsistency()**: so sánh `auctions.currentHighestBid` với `MAX(bid_transactions.bid_amount)`.  
2. **checkAuctionWinners()**: tìm phiên `FINISHED` có bid nhưng `highestBidderId = null`.  
3. **checkOrphanedItems()**: tìm `items` có `sellerId` không tồn tại trong `users`.

**Tại sao chỉ đọc, không auto‑fix?**  
- **Rủi ro**: Logic check có thể sai (do bug hoặc hiểu nhầm nghiệp vụ) → auto‑fix sẽ làm hỏng dữ liệu đúng.  
- **Concurrency**: Khi đang có luồng đặt giá, việc sửa DB trực tiếp gây race condition.  
- **Trách nhiệm**: Admin là người quyết định sửa sau khi xem báo cáo.  

*Dẫn chứng code:*  
`DataIntegrityService.java` – các method trả về `List<String>` lỗi, không ghi DB.

---

### 18. Admin Dashboard có 8 thao tác. Trong đó `LOCK_USER` và `UNLOCK_USER` có cấm admin khóa chính mình không? Nếu không cấm sẽ ra sao? `RUN_INTEGRITY_CHECK` trả về kết quả gì?

**Trả lời:**  

Trong `AdminUserService.lockUser()`:  
```java
if (target.getRole() == UserRole.ADMIN) {
    throw new ValidationException("Không thể khóa tài khoản Admin.");
}
```
⇒ **Cấm khóa Admin khác và chính mình**. Nếu không cấm, Admin A khóa Admin B → B không đăng nhập được để mở khóa chính mình → deadlock quản trị.

`RUN_INTEGRITY_CHECK` trả về JSON chứa:  
- `bidConsistencyErrors`: danh sách lỗi.  
- `auctionWinnerErrors`: danh sách lỗi.  
- `orphanedItemErrors`: danh sách lỗi.  
- `totalErrors`: số lượng.  
- `status`: `"OK"` nếu không có lỗi, `"ERRORS_FOUND"` nếu có.

Admin dùng kết quả này để kiểm tra sức khỏe hệ thống và sửa thủ công.

*Dẫn chứng code:*  
`AdminUserService.java`, `DataIntegrityService.runFullCheck()`.

---

## 9. Unit Test, CI/CD & chất lượng

### 19. Test in‑memory SQLite. Làm thế nào để test mà không ảnh hưởng DB thật? Tìm file `AntiSnipingEngineTest` và giải thích 5 test case.

**Trả lời:**  

Trong test, DAO được inject với connection đến `jdbc:sqlite::memory:`. DB được tạo mới từ `schema.sql` trước mỗi test và tự động xóa sau khi test kết thúc.  

`AntiSnipingEngineTest` (trong `bidhub-server/test/`) có 5 test case:  
1. **`testNoExtensionWhenBidOutsideWindow`**: bid ngoài 60s cuối → không gia hạn.  
2. **`testExtensionWhenBidInsideWindow`**: bid trong 60s cuối → gia hạn đúng 60s.  
3. **`testMultipleExtensions`**: nhiều bid liên tiếp trong vùng nhạy cảm → cộng dồn 60s mỗi lần.  
4. **`testExtensionWhenAlreadyExtended`**: đã được gia hạn rồi, bid tiếp vẫn gia hạn thêm.  
5. **`testThresholdBoundary`**: bid chính xác tại thời điểm `endTime - threshold` → vẫn gia hạn (dùng `>=`).

*Dẫn chứng code:*  
`AntiSnipingEngineTest.java` – dùng `@BeforeEach` tạo DB in‑memory.

---

### 20. GitHub Actions chạy CI. Khi nào pipeline được kích hoạt? Chạy lệnh Maven nào? Nếu test fail, pipeline có dừng không? Artifact lưu bao lâu?

**Trả lời:**  

Pipeline (`.github/workflows/ci.yml`) được kích hoạt khi:  
- Push lên bất kỳ branch nào.  
- Pull request vào nhánh `main` hoặc `develop`.  

Lệnh Maven chạy: `mvn clean test --batch-mode` (hoặc `mvn verify`).  

- Nếu **test fail**, pipeline **dừng ngay** và đánh dấu trạng thái `failure`.  
- Artifact (Surefire reports) được upload với thời gian lưu giữ **7 ngày** (có thể cấu hình).  

*Dẫn chứng code:*  
`.github/workflows/ci.yml` – có step “Upload test results” với `if: always()`.

---

## 10. Sáng tạo & phần mở rộng

### 21. Nhóm tự giới thiệu 4 tính năng sáng tạo. Tính năng nào khó nhất? Nếu phải bỏ một tính năng để lấy Auto‑Bidding, chọn bỏ nào? Vì sao?

**Trả lời:**  

4 tính năng sáng tạo (theo báo cáo):  
1. **Audit Log chịu lỗi** (non‑blocking).  
2. **Data Integrity Service** (read‑only cross‑check).  
3. **Admin Dashboard** (8 thao tác).  
4. **Thông báo hai kênh** (push + polling).  

**Khó nhất:** *Data Integrity Service* – vì phải đảm bảo kiểm tra nhất quán giữa nhiều bảng, xử lý join và tính toán mà không ảnh hưởng đến performance.  

**Nếu phải bỏ một để lấy Auto‑Bidding:** Nhóm chọn bỏ *Thông báo hai kênh* (broadcast polling). Lý do: broadcast có thể thay thế bằng realtime push (dù hơi quá tải), nhưng Data Integrity là cần thiết để đảm bảo dữ liệu không bị lỗi sau race condition. Admin Dashboard cũng quan trọng để quản trị.

---

### 22. So sánh yêu cầu nâng cao đề bài (Auto‑Bidding, Anti‑Sniping, Realtime Chart) với những gì nhóm đã làm. Giải thích thuật toán ưu tiên nếu cài Auto‑Bidding chồng lên cơ chế lock hiện tại.

**Trả lời:**  

Đã làm: Anti‑Sniping, Realtime Chart. Chưa làm: Auto‑Bidding.  

**Nếu thêm Auto‑Bidding:**  
- Mỗi user đăng ký auto‑bid với `maxBid`, `increment`. Lưu vào `PriorityQueue<AutoBidEntry>` (ưu tiên theo thời gian đăng ký).  
- Sau khi một bid thủ công (hoặc auto‑bid) được xử lý thành công, cần kích hoạt `AutoBidService.checkAndPlace()`:  
  - Lấy queue của auction, xác định user kế tiếp có `maxBid` cao nhất (hoặc sớm nhất).  
  - Tính `nextBid = min( maxBid, currentHighestBid + increment )`.  
  - Nếu `nextBid > currentHighestBid` → đặt bid tự động (gọi cùng `placeBid` logic, vẫn qua lock).  
- **Vấn đề**: Tránh vòng lặp vô hạn (A auto‑bid → trigger B auto‑bid → lại A). Giải pháp: chỉ xử lý một auto‑bid mỗi lần trigger, sau đó break; hoặc dùng flag để không kích hoạt auto‑bid từ auto‑bid.  

*Ưu tiên:* giữ nguyên cơ chế lock hiện tại, chỉ thêm service xử lý sau khi commit transaction, tương tự anti‑sniping.

---

## 11. Câu hỏi tình huống – kiểm tra hiểu sâu

### 23. Hai người cùng gửi `PLACE_BID` tại cùng milli‑giây trên cùng auction, cả hai đều hợp lệ (giá cao hơn hiện tại). Cơ chế lock xử lý thế nào? Ai được ghi nhận? Dữ liệu DB sau đó có nhất quán không? Có thể cả hai đều nghĩ mình thắng không?

**Trả lời:**  

- **Lock per auction**: `auction.getLock().lock()` – chỉ một thread vào critical section, thread còn lại chờ.  
- Thread đầu tiên vào: đọc `currentHighestBid` = 1000, validate (giả sử giá 1100), cập nhật lên 1100, commit.  
- Thread thứ hai sau khi được lock: đọc `currentHighestBid` = 1100, nếu giá của nó là 1050 → validate thất bại (không > 1100) → bị reject. Nếu giá 1200 → validate thành công, update lên 1200.  
- **Kết quả**: Chỉ một bid thành công (giá cao nhất). DB nhất quán vì transaction + lock đảm bảo atomic.  
- **Không thể** cả hai đều nghĩ mình thắng vì response trả về chỉ có một OK, còn lại nhận ERROR.

---

### 24. Khi server đang xử lý một bid, `AuctionLifecycleTask` đồng thời đóng phiên đó vì hết giờ. Code đã xử lý race condition này chưa? Dùng cơ chế gì? Chỉ ra dòng kiểm tra `canBid()` trong lock.

**Trả lời:**  

Trong `AuctionLifecycleTask.closeAuction()`:  
```java
auction.getLock().lock();
try {
    if (auction.getStatus() != AuctionStatus.RUNNING) return; // double-check
    auction.transitionTo(FINISHED);
    // ...
} finally { lock.unlock(); }
```
Trong `AuctionHandler.handlePlaceBid()`:  
```java
auction.getLock().lock();
try {
    if (!auction.getStatus().canBid()) { // canBid() chỉ true khi RUNNING
        throw new AuctionClosedException(...);
    }
    // ... transaction
} finally { lock.unlock(); }
```

- **Cơ chế**: Cả hai đều phải acquire cùng lock của auction.  
- Nếu lifecycle task đang giữ lock để đóng, bid phải chờ. Sau khi lock được thả, bid thấy status không còn RUNNING → bị reject.  
- Nếu bid đang giữ lock, lifecycle task chờ → bid hoàn thành (có thể gia hạn endTime) → lifecycle task sau đó thấy endTime mới, nếu vẫn hết hạn thì đóng.  
- **Không có race condition** nhờ lock.

*Dẫn chứng code:*  
`AuctionHandler.handlePlaceBid()` dòng `if (!auction.getStatus().canBid())`.

---

### 25. Client mất kết nối Socket 2 (event push) nhưng vẫn giữ Socket 1. Client có còn nhận được thông báo realtime không? Làm sao client tự phục hồi? Code có cơ chế reconnect không?

**Trả lời:**  

- **Không nhận được realtime event** vì event chỉ push qua Socket 2.  
- Client vẫn có thể đặt giá (qua Socket 1) và nhận response, nhưng không thấy bid mới từ người khác.  
- **Phục hồi**: Client cần tự phát hiện Socket 2 bị đóng (ví dụ `readLine()` trả null) và thực hiện reconnect: đóng socket cũ, mở socket mới, gửi lại `SUBSCRIBE_AUCTION`.  
- Trong code hiện tại, `EventListenerThread` khi bắt gặp `IOException` hoặc `null` sẽ thoát, nhưng không tự động reconnect. Có thể cải tiến bằng vòng lặp while với `stopRequested` và `Thread.sleep()` thử lại.

*Nhận xét:* Đây là điểm có thể cải thiện, nhưng trong phạm vi đồ án, nếu client mất kết nối realtime thường do tắt ứng dụng hoặc mạng, user tự refresh lại trang.

---

## 12. Yêu cầu chứng minh trực tiếp trên code (nếu vấn đáp)

### 26. Mở file `BidValidator.java`, chỉ ra 5 quy tắc kiểm tra. Nếu thêm quy tắc "giá phải là bội số của 1000", sửa ở đâu?

**Trả lời:**  

Trong `BidValidator.validate()`:  
1. `if (auction.getStatus() == OPEN) throw new InvalidBidException("Chưa bắt đầu")`.  
2. `else if (auction.getStatus() != RUNNING) throw new AuctionClosedException(...)`.  
3. `if (bidderId.equals(auction.getHighestBidderId())) throw new InvalidBidException("Đang dẫn đầu")`.  
4. `if (bidderId.equals(getItemOwnerId(auction.getItemId()))) throw new InvalidBidException("Seller không tự đấu giá")`.  
5. `if (bidAmount <= auction.getCurrentHighestBid()) throw new InvalidBidException("Giá quá thấp")`.  
6. `if ( (bidAmount - currentHighestBid) < minimumIncrement) throw new InvalidBidException("Bước giá không đủ")`.  

**Thêm quy tắc "bội số 1000":**  
Thêm sau quy tắc 5 (hoặc cuối cùng):  
```java
if (bidAmount % 1000 != 0) {
    throw new InvalidBidException("Giá đặt phải là bội số của 1000 VNĐ");
}
```

---

### 27. Mở file `AuctionManager.java`. Tìm biến `ConcurrentHashMap<String, ReentrantLock> auctionLocks`. Giải thích tại sao map này lại dùng `ConcurrentHashMap`? Tại sao key là `auctionId` dạng String?

**Trả lời:**  

Trong `AuctionManager` thực tế không có `Map<String, ReentrantLock>` riêng, mà lock được lưu bên trong mỗi `Auction` object (field `private final ReentrantLock lock`).  
Nếu có map như vậy:  
- `ConcurrentHashMap` được dùng để cho phép nhiều thread truy cập (đọc/ghi) map mà không cần lock toàn cục, vì số lượng auction có thể lớn và thường xuyên có thao tác lấy lock theo `auctionId`.  
- Key là `auctionId` (String) vì `auctionId` là định danh duy nhất của phiên, dạng UUID string. Dùng String thay vì UUID object để đơn giản hóa khi serialize/deserialize JSON và lưu DB.

---

### 28. Mở file `SocketServerCore.java`. Chỉ ra cách tạo `ExecutorService` với 30 thread. Nếu số client > 30, request có bị từ chối không? Hàng đợi có giới hạn không?

**Trả lời:**  

```java
int poolSize = ConfigLoader.getIntOrDefault("server.poolSize", 30);
this.threadPool = Executors.newFixedThreadPool(poolSize);
```
- `newFixedThreadPool(30)` tạo **ThreadPoolExecutor** với corePoolSize = maxPoolSize = 30, và hàng đợi **LinkedBlockingQueue** không giới hạn dung lượng (theo mặc định).  
- Khi số client > 30, các kết nối thừa được xếp vào hàng đợi và chờ đến khi có thread rảnh. **Không bị từ chối**, chỉ bị chậm hơn.  
- Nếu muốn giới hạn hàng đợi, cần tạo `ThreadPoolExecutor` với `ArrayBlockingQueue` có kích thước cố định và chính sách rejection.

*Dẫn chứng code:* `SocketServerCore.java` constructor.

---

**Kết luận:** Bộ câu hỏi này kiểm tra cả lý thuyết thiết kế lẫn khả năng đọc code. Nhóm cần luyện tập trả lời ngắn gọn, chỉ đúng dòng code và giải thích tại sao lại chọn giải pháp đó.