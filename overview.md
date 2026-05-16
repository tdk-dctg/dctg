# 📘 BidHub — Tổng Quan Hệ Thống Đấu Giá Trực Tuyến

> Tài liệu giải thích chi tiết cách hệ thống BidHub hoạt động, kèm sơ đồ UML.
> Viết cho người không có nền tảng kỹ thuật vẫn có thể hiểu được.

---

## 1. Hệ thống BidHub là gì?

BidHub là một **hệ thống đấu giá trực tuyến** — giống như trang eBay thu nhỏ. Người dùng có thể:

- **Đăng ký / Đăng nhập** tài khoản
- **Đăng sản phẩm** lên hệ thống (nếu là Người bán)
- **Tạo phiên đấu giá** cho sản phẩm
- **Đặt giá** (trả giá) trong phiên đấu giá đang diễn ra
- **Nhận thông báo** khi có người trả giá cao hơn (realtime)
- **Quản trị**: Admin có thể khoá/mở tài khoản, xem báo cáo

---

## 2. Kiến Trúc Tổng Quan (Nhìn từ trên cao)

### 2.1 Mô hình Client–Server

```mermaid
graph LR
    subgraph "Máy người dùng"
        C1["Client 1 (JavaFX)"]
        C2["Client 2 (JavaFX)"]
        C3["Client N..."]
    end

    subgraph "Máy chủ Server"
        S["BidHub Server"]
        DB["SQLite Database"]
    end

    C1 -- "JSON qua TCP Socket" --> S
    C2 -- "JSON qua TCP Socket" --> S
    C3 -- "JSON qua TCP Socket" --> S
    S -- "Đọc/Ghi dữ liệu" --> DB
```

**Giải thích đơn giản:**
- **Client** = Ứng dụng trên máy người dùng (giao diện JavaFX)
- **Server** = Máy chủ xử lý mọi yêu cầu
- **Database** = Nơi lưu trữ dữ liệu (SQLite)
- Client và Server nói chuyện qua **TCP Socket** bằng tin nhắn **JSON**

### 2.2 Kiến Trúc Bên Trong Server

```mermaid
graph TB
    subgraph "Tầng Mạng (Network Layer)"
        SSC["SocketServerCore<br/>Lắng nghe kết nối"]
        CCT["ClientConnectionThread<br/>Xử lý từng client"]
        RH["RequestHandler<br/>Phân phối yêu cầu"]
    end

    subgraph "Tầng Dịch Vụ (Service Layer)"
        AUTH["AuthService<br/>Xác thực"]
        SM["SessionManager<br/>Quản lý phiên"]
        AM["AuctionManager<br/>Quản lý đấu giá"]
        BV["BidValidator<br/>Kiểm tra đặt giá"]
        ASE["AntiSnipingEngine<br/>Chống đặt giá phút cuối"]
        NB["NotificationBroker<br/>Thông báo realtime"]
        AUS["AdminUserService<br/>Quản trị user"]
        RS["ReportService<br/>Báo cáo"]
        DIS["DataIntegrityService<br/>Kiểm tra dữ liệu"]
        ALS["AuditLogService<br/>Nhật ký"]
    end

    subgraph "Tầng Dữ Liệu (DAO Layer)"
        UD["UserDao"]
        ID["ItemDao"]
        AD["AuctionDao"]
        BD["BidDao"]
        ALD["AuditLogDao"]
    end

    subgraph "Database"
        DB["SQLite"]
    end

    SSC --> CCT --> RH
    RH --> AUTH & SM & AM & BV & AUS & RS & DIS & ALS
    AM --> ASE & NB
    AUTH & SM & AM & BV & AUS & RS & DIS & ALS --> UD & ID & AD & BD & ALD
    UD & ID & AD & BD & ALD --> DB
```

**Giải thích từng tầng:**

| Tầng | Vai trò | Ví dụ thực tế |
|------|---------|---------------|
| **Network** | Nhận/gửi tin nhắn từ client | Như lễ tân khách sạn — tiếp nhận yêu cầu |
| **Service** | Xử lý logic nghiệp vụ | Như bộ phận xử lý — kiểm tra, tính toán |
| **DAO** | Đọc/ghi database | Như thủ kho — lưu trữ và lấy dữ liệu |
| **Database** | Kho dữ liệu | Như tủ hồ sơ — nơi lưu trữ vĩnh viễn |

---

## 3. Sơ Đồ Lớp (Class Diagram) — Các Đối Tượng Chính

### 3.1 Hệ thống Người dùng (User Hierarchy)

```mermaid
classDiagram
    class Entity {
        <<abstract>>
        -String id (UUID)
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +getId()
        +getCreatedAt()
        #markUpdated()
    }

    class User {
        <<abstract>>
        -String username
        -String passwordHash
        -String email
        -UserRole role
        -boolean locked
        +getUsername()
        +getRole()
        +isLocked()
        +getInfo()*
    }

    class Bidder {
        +getInfo() "Người mua"
    }

    class Seller {
        +getInfo() "Người bán"
    }

    class Admin {
        +getInfo() "Quản trị"
    }

    class UserRole {
        <<enum>>
        BIDDER
        SELLER
        ADMIN
    }

    Entity <|-- User
    User <|-- Bidder
    User <|-- Seller
    User <|-- Admin
    User --> UserRole
```

**Giải thích:**
- **Entity**: Lớp gốc — mọi đối tượng đều có `id` (mã định danh duy nhất) và thời gian tạo/cập nhật
- **User**: Người dùng — có tên đăng nhập, mật khẩu (đã mã hoá), email, vai trò
- **Bidder**: Người mua — đặt giá trong các phiên đấu giá
- **Seller**: Người bán — đăng sản phẩm và tạo phiên đấu giá
- **Admin**: Quản trị viên — khoá/mở tài khoản, xem báo cáo

### 3.2 Hệ thống Sản phẩm (Item Hierarchy)

```mermaid
classDiagram
    class Item {
        <<abstract>>
        -String name
        -String description
        -double startingPrice
        -String sellerId
        -ItemType itemType
        +getCategoryDetails()*
    }

    class Electronics {
        -String brand
        -int warrantyMonths
    }

    class Art {
        -String artist
        -int year
    }

    class Vehicle {
        -String make
        -int mileage
    }

    class ItemType {
        <<enum>>
        ELECTRONICS
        ART
        VEHICLE
    }

    class Displayable {
        <<interface>>
        +printInfo()
    }

    class ItemCreator {
        <<abstract>>
        +createItem()*
        +forType()$ Factory Method
    }

    Entity <|-- Item
    Item <|-- Electronics
    Item <|-- Art
    Item <|-- Vehicle
    Displayable <|.. Item
    Item --> ItemType
    ItemCreator ..> Item : creates
```

**Giải thích:**
- Sản phẩm có 3 loại: **Đồ điện tử**, **Nghệ thuật**, **Xe cộ**
- Mỗi loại có thông tin riêng (VD: Electronics có thương hiệu, bảo hành)
- **ItemCreator** dùng **Factory Method Pattern** để tạo sản phẩm đúng loại

### 3.3 Hệ thống Đấu giá

```mermaid
classDiagram
    class Auction {
        -String itemId
        -LocalDateTime startTime
        -LocalDateTime endTime
        -double startingPrice
        -double currentHighestBid
        -String highestBidderId
        -AuctionStatus status
        -double minimumIncrement
        -ReentrantLock lock
        +transitionTo(newStatus)
        +isValidBid(amount)
        +extendEndTime(newEnd)
    }

    class BidTransaction {
        -String auctionId
        -String bidderId
        -double bidAmount
        -LocalDateTime bidTime
    }

    class AuctionStatus {
        <<enum>>
        OPEN
        RUNNING
        FINISHED
        PAID
        CANCELED
        +canBid()
        +isTerminal()
        +canTransitionTo()
    }

    Entity <|-- Auction
    Entity <|-- BidTransaction
    Auction --> AuctionStatus
    Auction "1" --> "*" BidTransaction
```

### 3.4 Vòng đời phiên đấu giá (State Machine)

```mermaid
stateDiagram-v2
    [*] --> OPEN : Tạo phiên mới
    OPEN --> RUNNING : Đến giờ bắt đầu
    RUNNING --> FINISHED : Hết thời gian
    FINISHED --> PAID : Thanh toán xong
    FINISHED --> CANCELED : Huỷ phiên

    OPEN : Chờ bắt đầu\nChưa ai đặt giá được
    RUNNING : Đang diễn ra\nNgười dùng đặt giá
    FINISHED : Đã kết thúc\nChờ thanh toán/huỷ
    PAID : Hoàn tất
    CANCELED : Đã huỷ
```

**Giải thích đơn giản:**
1. Seller tạo phiên → trạng thái **OPEN** (chờ)
2. Đến giờ → tự động chuyển sang **RUNNING** (đang chạy)
3. Hết giờ → tự động chuyển sang **FINISHED** (kết thúc)
4. Sau đó → **PAID** (đã thanh toán) hoặc **CANCELED** (huỷ)

---

## 4. Luồng Hoạt Động Chính

### 4.1 Đăng ký & Đăng nhập

```mermaid
sequenceDiagram
    actor U as Người dùng
    participant C as Client App
    participant S as Server
    participant DB as Database

    Note over U,DB: === ĐĂNG KÝ ===
    U->>C: Nhập username, password, email, role
    C->>S: {"type":"REGISTER", "payload":{...}}
    S->>S: Kiểm tra username trùng?
    S->>S: Mã hoá password (SHA-256)
    S->>DB: Lưu user mới
    S->>C: {"status":"OK", "payload":{"userId":"..."}}
    C->>U: Hiển thị "Đăng ký thành công"

    Note over U,DB: === ĐĂNG NHẬP ===
    U->>C: Nhập username, password
    C->>S: {"type":"LOGIN", "payload":{...}}
    S->>DB: Tìm user theo username
    S->>S: So sánh hash password
    S->>S: Tạo token UUID
    S->>C: {"status":"OK", "payload":{"token":"abc-123"}}
    C->>U: Vào trang chính
```

### 4.2 Tạo sản phẩm & Phiên đấu giá

```mermaid
sequenceDiagram
    actor Seller as Người bán
    participant C as Client
    participant S as Server
    participant DB as Database

    Seller->>C: Nhập tên, giá, loại sản phẩm
    C->>S: {"type":"CREATE_ITEM", "token":"...", "payload":{...}}
    S->>S: Kiểm tra role = SELLER
    S->>S: Factory Method tạo Item đúng loại
    S->>DB: Lưu sản phẩm
    S->>C: OK + itemId

    Seller->>C: Chọn item, đặt giờ bắt đầu/kết thúc
    C->>S: {"type":"CREATE_AUCTION", "payload":{...}}
    S->>DB: Lưu auction
    S->>S: Thêm vào AuctionManager (RAM)
    S->>C: OK + auctionId
```

### 4.3 Đặt giá (Place Bid) — Luồng quan trọng nhất

```mermaid
sequenceDiagram
    actor B as Người mua
    participant C as Client
    participant S as Server
    participant AM as AuctionManager
    participant BV as BidValidator
    participant ASE as AntiSniping
    participant NB as NotificationBroker
    participant DB as Database

    B->>C: Nhập số tiền đặt giá
    C->>S: {"type":"PLACE_BID", "payload":{"auctionId":"...", "bidAmount":5000000}}

    S->>AM: Lấy auction từ RAM
    S->>S: Lock auction (ReentrantLock)

    S->>BV: Validate 5 điều kiện
    Note right of BV: 1. Auction đang RUNNING?<br/>2. Không phải người dẫn đầu?<br/>3. Không phải seller?<br/>4. Giá > giá hiện tại?<br/>5. Bước giá đủ lớn?

    S->>DB: Lưu BidTransaction
    S->>AM: Cập nhật giá cao nhất (RAM)
    S->>DB: Cập nhật auction (DB)

    S->>ASE: Kiểm tra Anti-Sniping
    Note right of ASE: Nếu bid trong 60s cuối<br/>→ gia hạn thêm 60s

    S->>S: Unlock auction

    S->>NB: Phát thông báo BID_UPDATE
    NB-->>C: Push realtime đến tất cả client đang xem
    S->>C: OK + giá mới

    B->>B: Thấy giá cập nhật ngay lập tức
```

---

## 5. Design Patterns Sử Dụng

| Pattern | Nơi dùng | Giải thích đơn giản |
|---------|----------|---------------------|
| **Singleton** | `AuctionManager`, `SessionManager`, `NotificationBroker` | Chỉ có 1 instance duy nhất trong toàn hệ thống — như 1 "ông quản lý" duy nhất |
| **Factory Method** | `ItemCreator` → `ElectronicsCreator`, `ArtCreator`, `VehicleCreator` | Tạo sản phẩm đúng loại tự động — như nhà máy sản xuất theo đơn hàng |
| **Observer** | `NotificationBroker` (subscribe/publish) | Khi có bid mới → thông báo tất cả người đang xem — như đài phát thanh |
| **State Machine** | `AuctionStatus` (OPEN → RUNNING → FINISHED) | Phiên đấu giá tự động chuyển trạng thái — như đèn giao thông |
| **MVC** | `RequestHandler` (Controller), Service (Model), Client (View) | Tách riêng giao diện, xử lý, dữ liệu — dễ bảo trì |

---

## 6. Cây Exception (Xử lý lỗi)

```mermaid
classDiagram
    class RuntimeException {
        <<Java Built-in>>
    }
    class BidHubException {
        -String errorCode
        +getErrorCode()
    }
    class ValidationException {
        -List~String~ errors
    }
    class AuthenticationException
    class UserNotFoundException
    class DuplicateUsernameException
    class InvalidBidException
    class AuctionNotFoundException
    class AuctionClosedException

    RuntimeException <|-- BidHubException
    BidHubException <|-- ValidationException
    BidHubException <|-- AuthenticationException
    BidHubException <|-- UserNotFoundException
    BidHubException <|-- DuplicateUsernameException
    BidHubException <|-- InvalidBidException
    BidHubException <|-- AuctionNotFoundException
    BidHubException <|-- AuctionClosedException
```

---

## 7. Cấu Trúc Module (Maven Multi-Module)

```mermaid
graph TB
    Parent["bidhub-parent (POM gốc)"]
    Common["bidhub-common<br/>Code dùng chung:<br/>Entity, Exception, MessageMapper"]
    Server["bidhub-server<br/>Toàn bộ logic server:<br/>Model, DAO, Service, Network"]
    Client["bidhub-client<br/>Giao diện JavaFX"]

    Parent --> Common
    Parent --> Server
    Parent --> Client
    Server --> Common
    Client --> Common
```

---

## 8. Kỹ Thuật Quan Trọng

### 8.1 Thread Safety (An toàn đa luồng)

Khi nhiều người đặt giá **cùng lúc**, hệ thống dùng:
- **ReentrantLock** trên mỗi Auction → chỉ 1 bid được xử lý tại 1 thời điểm
- **ConcurrentHashMap** → nhiều thread đọc/ghi an toàn
- **CopyOnWriteArrayList** → duyệt danh sách subscriber an toàn

### 8.2 Anti-Sniping (Chống đặt giá phút cuối)

Nếu ai đó đặt giá trong **60 giây cuối** → phiên tự động **gia hạn thêm 60 giây**. Điều này ngăn chiến thuật "chờ giây cuối mới đặt giá".

### 8.3 Audit Log (Nhật ký)

Mọi hành động quan trọng đều được ghi lại: đăng nhập, tạo sản phẩm, đặt giá, khoá tài khoản... Admin có thể xem toàn bộ lịch sử.

---

## 9. Phân Chia Công Việc Theo Tuần

| Tuần | Nội dung chính | Thành viên |
|------|---------------|------------|
| **1** | Setup project Maven, CI/CD, JUnit, docs | Cả nhóm |
| **2** | OOP Domain Model (Entity, User, Item, Auction, Exception) | Cả nhóm |
| **3** | Database SQLite + DAO layer | Đăng, Quốc Minh |
| **4** | TCP Socket Server + Client kết nối + PING | Quốc Minh, Đăng |
| **5** | Login/Register/Logout + CREATE_ITEM + GET_ITEM_LIST | Quốc Minh, Khoa |
| **6** | CREATE_AUCTION + PLACE_BID + BidValidator + AdminUserService | Đăng, Quốc Minh, Khoa |
| **7** | Observer/NotificationBroker + ReportService + Anti-Sniping + DataIntegrityService | Quốc Minh, Khoa |
| **8+** | Client GUI hoàn chỉnh, tích hợp, testing | Công Minh, cả nhóm |

---

## 10. Bảng Tổng Hợp API Commands

| Command | Yêu cầu đăng nhập | Role | Mô tả |
|---------|-------------------|------|-------|
| `PING` | ❌ | Ai cũng được | Kiểm tra server còn sống |
| `REGISTER` | ❌ | — | Đăng ký tài khoản mới |
| `LOGIN` | ❌ | — | Đăng nhập, nhận token |
| `LOGOUT` | ✅ | Ai cũng được | Đăng xuất |
| `CREATE_ITEM` | ✅ | SELLER | Tạo sản phẩm mới |
| `GET_ITEM_LIST` | ❌ | Ai cũng được | Xem danh sách sản phẩm |
| `DELETE_ITEM` | ✅ | SELLER (chủ sở hữu) | Xoá sản phẩm |
| `CREATE_AUCTION` | ✅ | SELLER | Tạo phiên đấu giá |
| `PLACE_BID` | ✅ | Ai cũng được | Đặt giá |
| `GET_AUCTION_LIST` | ❌ | Ai cũng được | Xem danh sách phiên |
| `GET_AUCTION_DETAIL` | ✅ | Ai cũng được | Xem chi tiết phiên |
| `SUBSCRIBE_AUCTION` | ❌ | Ai cũng được | Đăng ký nhận thông báo realtime |
| `GET_USER_LIST` | ✅ | ADMIN | Xem danh sách user |
| `LOCK_USER` | ✅ | ADMIN | Khoá tài khoản |
| `UNLOCK_USER` | ✅ | ADMIN | Mở khoá tài khoản |
| `GET_AUCTION_REPORT` | ✅ | SELLER/ADMIN | Báo cáo đấu giá |
| `GET_BID_HISTORY_REPORT` | ✅ | Ai cũng được | Lịch sử đặt giá |
| `GET_AUDIT_LOG` | ✅ | ADMIN | Nhật ký hệ thống |
| `RUN_INTEGRITY_CHECK` | ✅ | ADMIN | Kiểm tra toàn vẹn dữ liệu |

---

## 11. Sơ Đồ Deployment (Triển khai)

```mermaid
graph TB
    subgraph "Máy người dùng 1"
        C1["BidHub Client (JavaFX)<br/>Port ngẫu nhiên"]
    end
    subgraph "Máy người dùng 2"
        C2["BidHub Client (JavaFX)<br/>Port ngẫu nhiên"]
    end
    subgraph "Máy chủ"
        Server["BidHub Server<br/>Port 9090"]
        SQLite["auction.db (SQLite)"]
        Server --- SQLite
    end

    C1 -- "TCP Socket" --> Server
    C2 -- "TCP Socket" --> Server
```

---

> **Tóm tắt**: BidHub là hệ thống đấu giá client-server viết bằng Java. Client dùng JavaFX, Server xử lý qua TCP Socket với JSON. Hệ thống áp dụng OOP, Design Patterns (Singleton, Factory, Observer, State Machine), đa luồng an toàn, và kiến trúc MVC phân tầng rõ ràng.
