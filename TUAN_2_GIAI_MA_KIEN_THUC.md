# 📘 Tuần 2 – Giải mã toàn bộ kiến thức & thiết kế OOP Domain Model

> **Mục tiêu của tài liệu này:** Giúp mọi thành viên trong nhóm hiểu sâu về các khái niệm OOP (Abstract class, Interface, Factory Pattern, Enum, Exception Hierarchy), thiết kế domain model, lý do mỗi quyết định thiết kế, và cách các class kết nối với nhau. Sau khi đọc tài liệu này, bất kỳ ai cũng có thể giải thích được code của người khác và thấy được kiến trúc tổng thể của hệ thống.

---

## 1. Tổng quan tuần 2 – Tại sao phải làm những việc này?

### 🎯 Mục tiêu chính của tuần 2

Tuần 2 là tuần xây **bộ xương domain** - toàn bộ các class và cây kế thừa mà tất cả các tuần sau đều phụ thuộc vào. Khác với tuần 1 (setup infrastructure), tuần 2 tập trung vào **business domain** - mô hình hóa thế giới thực của hệ thống đấu giá.

Cuối tuần 2, cả nhóm phải có:
- ✅ Module `bidhub-common` mới (chứa Entity và Exception)
- ✅ Cây kế thừa `Entity → User → Bidder/Seller/Admin`
- ✅ Cây kế thừa `Entity → Item → Electronics/Art/Vehicle` + `ItemFactory`
- ✅ `Auction`, `AuctionStatus` (state machine), `BidTransaction`
- ✅ `BidHubException` hierarchy với 7 subclasses
- ✅ Tổng ≥ 40 test cases (15 từ tuần 1 + 25 mới)

### 📊 Đóng góp vào barem điểm

Tuần này là tuần **QUAN TRỌNG NHẤT** về mặt điểm số:

| Tiêu chí | Điểm | Công việc tuần 2 |
|----------|------|------------------|
| **Thiết kế lớp & cây kế thừa** | 0.5đ | Entity, User, Item hierarchy |
| **4 trụ cột OOP** | 1.0đ | Encapsulation, Inheritance, Polymorphism, Abstraction |
| **Xử lý lỗi & ngoại lệ** | 1.0đ | BidHubException hierarchy + ValidationException |

→ **Làm đúng tuần 2 = 2.5đ trong tay** (¼ tổng điểm bài tập lớn!)

### ⚠️ Hậu quả nếu tuần 2 không hoàn thành đúng

**Tuần 2 là domain foundation** - nếu sai ở đây, mọi thứ sau này sẽ sai theo:

- **Tuần 3 (DAO):** `UserDao` không thể save/load `User` nếu cấu trúc User sai
- **Tuần 4 (Socket):** Server không serialize được Entity thành JSON nếu thiếu getter
- **Tuần 5-6 (Auth, Bidding):** Logic nghiệp vụ sụp đổ nếu Entity không có `equals()`
- **Tuần 7 (Concurrent):** ReentrantLock dựa trên `Auction.getId()` - nếu getId() không unique → deadlock
- **Tuần 9-10 (Demo):** Giảng viên hỏi "Tại sao Entity là abstract?" → không trả lời được → 0 điểm cả nhóm

**Nguyên tắc vàng:** Code tuần 2 phải đúng ngay từ đầu. Sửa model sau = sửa dây chuyền 8 tuần.

---

## 2. Những khái niệm nền tảng phải học trước khi code

### 2.1. Abstract Class vs Interface - Khi nào dùng cái gì?

#### 🎓 Bản chất trong Java

**Abstract Class:**
- Là class, có thể chứa **state** (fields) và **behavior** (methods)
- Có thể có constructor
- Không thể `new` trực tiếp
- Chỉ extends được 1 abstract class (đơn kế thừa)

**Interface:**
- Chỉ định nghĩa **contract** (methods phải implement)
- Không có state (trước Java 8), sau Java 8 có thể có `default` methods
- Không có constructor
- Có thể implements nhiều interfaces (đa kế thừa)

#### 🔍 Khi nào dùng Abstract Class?

**Dùng khi:**
1. Các subclass **chia sẻ trạng thái chung** (fields)
2. Muốn cung cấp **implementation mặc định** cho một số methods
3. Có relationship **"IS-A"** rõ ràng

**Ví dụ trong BidHub:**

```java
// ✅ Entity là abstract class - VÌ:
// 1. Có state chung: id, createdAt, updatedAt
// 2. Có implementation: equals(), hashCode(), markUpdated()
// 3. Relationship: "User IS-A Entity", "Item IS-A Entity"

public abstract class Entity {
    private final String id;  // ← STATE CHUNG
    private final LocalDateTime createdAt;
    
    // ← IMPLEMENTATION CHUNG
    @Override
    public boolean equals(Object o) {
        return Objects.equals(id, ((Entity) o).id);
    }
}
```

**Nếu Entity là interface → BỊ PHÁ VỠ:**
```java
// ❌ KHÔNG THỂ làm thế này với interface
public interface Entity {
    // Interface không có fields!
    // private final String id; // ← compile error
    
    String getId();  // Mỗi class phải tự implement → lặp code
}
```

#### 🔍 Khi nào dùng Interface?

**Dùng khi:**
1. Định nghĩa **behavior** mà nhiều class không liên quan có thể implement
2. Cần **đa kế thừa** (implements nhiều interfaces)
3. Chỉ quan tâm **CÁI GÌ** class có thể làm, không quan tâm **NÓ LÀ GÌ**

**Ví dụ trong BidHub:**

```java
// Interface: defines WHAT an object can do
public interface Displayable {
    String getDisplayInfo();  // Contract: phải có method này
}

// Item có thể implements Displayable
// User cũng có thể implements Displayable
// → Không có relationship "IS-A" giữa Item và User
```

#### 💡 Kết hợp Abstract Class + Interface

**Class có thể vừa extends abstract class, vừa implements interface:**

```java
public abstract class Item extends Entity implements Displayable {
    // Kế thừa state từ Entity (id, timestamps)
    // Kế thừa behavior từ Entity (equals, hashCode)
    // Phải implement method từ Displayable (getDisplayInfo)
}
```

**Quy tắc thumb:**
- **Abstract class** → describes **WHAT the object IS**
- **Interface** → describes **WHAT the object CAN DO**

---

### 2.2. Factory Method Pattern - Tại sao cần nó?

#### 🎓 Bản chất của Pattern

Factory Method Pattern giải quyết vấn đề: **"Làm sao tạo object mà không hardcode class cụ thể?"**

**Vấn đề khi KHÔNG có Factory:**

```java
// ❌ TỆ: Client code phải biết tất cả subclasses
public Item createItem(String type, String title, double price) {
    if (type.equals("ELECTRONICS")) {
        return new Electronics(title, price, ...);
    } else if (type.equals("ART")) {
        return new Art(title, price, ...);
    } else if (type.equals("VEHICLE")) {
        return new Vehicle(title, price, ...);
    }
    // Thêm JEWELRY → phải sửa CODE NÀY
}
```

**Với Factory Pattern:**

```java
// ✅ TỐT: Client chỉ gọi factory, không cần biết subclasses
Item item = ItemFactory.create(ItemType.ELECTRONICS, data);
// Thêm JEWELRY → chỉ sửa TRONG factory, client code không đổi
```

#### 🔍 Tại sao BidHub cần Factory?

**Tình huống thực tế:**

Tuần 5, Server nhận JSON từ Client:
```json
{
  "type": "CREATE_ITEM",
  "itemType": "ELECTRONICS",
  "title": "iPhone 15",
  "startingPrice": 20000000,
  "warranty": 12
}
```

**Không có Factory:**
```java
// ❌ Server phải có khối if-else khổng lồ
if (json.get("itemType").equals("ELECTRONICS")) {
    return new Electronics(
        json.get("title"),
        json.get("startingPrice"),
        json.get("warranty")  // Chỉ Electronics mới có
    );
} else if (json.get("itemType").equals("ART")) {
    return new Art(
        json.get("title"),
        json.get("startingPrice"),
        json.get("artist")  // Chỉ Art mới có
    );
}
// 20 dòng if-else...
```

**Có Factory:**
```java
// ✅ Server gọi 1 dòng
Item item = ItemFactory.create(
    ItemType.valueOf(json.get("itemType")),
    json  // Factory tự parse JSON thành Item
);
```

#### 💡 Static Factory vs Factory Method Pattern

**2 khái niệm khác nhau:**

1. **Static Factory Method:**
   - 1 static method trả về object
   - Không có inheritance, không có creator class
   
```java
// Static Factory
public class Item {
    public static Item createElectronics(...) {
        return new Electronics(...);
    }
}
```

2. **Factory Method Pattern (từ Gang of Four):**
   - Có Creator abstract class
   - Subclass override factoryMethod()
   
```java
// Factory Method Pattern (GoF)
abstract class ItemCreator {
    abstract Item createItem();  // subclass override
}
class ElectronicsCreator extends ItemCreator {
    Item createItem() { return new Electronics(); }
}
```

**BidHub dùng cái nào?**

→ **Static Factory Method** (đơn giản hơn, đủ cho nhu cầu)

```java
public final class ItemFactory {
    private ItemFactory() {}  // Utility class
    
    public static Item create(ItemType type, Map<String, Object> data) {
        // Logic ở đây
    }
}
```

---

### 2.3. Java Enum với Abstract Method - State Machine

#### 🎓 Bản chất của Enum trong Java

**Java enum KHÔNG chỉ là constant:**
- Enum là 1 class đặc biệt
- Mỗi enum value là 1 **singleton instance** của class đó
- Có thể có fields, methods, thậm chí abstract methods

**Ví dụ đơn giản:**
```java
public enum UserRole {
    BIDDER("Người đặt giá"),
    SELLER("Người bán");
    
    private final String displayName;
    
    UserRole(String displayName) {  // Constructor
        this.displayName = displayName;
    }
    
    public String getDisplayName() {  // Method
        return displayName;
    }
}
```

#### 🔍 Enum với Abstract Method - Tại sao cần?

**Tình huống: `AuctionStatus` có 5 trạng thái, mỗi trạng thái có behavior khác nhau**

**Cách 1 - KHÔNG dùng abstract method (TỆ):**
```java
public enum AuctionStatus {
    OPEN, RUNNING, FINISHED, PAID, CANCELED;
    
    public boolean canBid() {
        // ❌ Phải dùng if-else hoặc switch
        if (this == RUNNING) return true;
        return false;
    }
}
```

**Cách 2 - Dùng abstract method (TỐT):**
```java
public enum AuctionStatus {
    OPEN {
        @Override
        public boolean canBid() { return false; }
    },
    RUNNING {
        @Override
        public boolean canBid() { return true; }
    },
    FINISHED {
        @Override
        public boolean canBid() { return false; }
    },
    PAID {
        @Override
        public boolean canBid() { return false; }
    },
    CANCELED {
        @Override
        public boolean canBid() { return false; }
    };
    
    // Abstract method - mỗi enum value PHẢI implement
    public abstract boolean canBid();
}
```

**Lợi ích:**
1. Không cần if-else → code sạch hơn
2. Compiler ép mọi enum value phải implement → không quên case nào
3. Mỗi state tự quản lý behavior của nó → Single Responsibility

#### 💡 State Machine trong AuctionStatus

**State Machine là gì?**

Một object có thể ở nhiều trạng thái (states), chỉ có thể chuyển từ state này sang state khác theo **rules** nhất định.

**Ví dụ Auction:**
```
OPEN → RUNNING → FINISHED → PAID
                     ↓
                  CANCELED
```

**Rules:**
- OPEN có thể → RUNNING hoặc CANCELED
- RUNNING có thể → FINISHED hoặc CANCELED
- FINISHED có thể → PAID hoặc CANCELED
- PAID và CANCELED là **terminal states** (không chuyển tiếp được nữa)

**Implementation:**
```java
public void transitionTo(AuctionStatus newStatus) {
    if (!canTransitionTo(newStatus)) {
        throw new IllegalStateException(
            "Cannot transition from " + status + " to " + newStatus
        );
    }
    this.status = newStatus;
    markUpdated();
}

private boolean canTransitionTo(AuctionStatus newStatus) {
    return switch (status) {
        case OPEN -> newStatus == RUNNING || newStatus == CANCELED;
        case RUNNING -> newStatus == FINISHED || newStatus == CANCELED;
        case FINISHED -> newStatus == PAID || newStatus == CANCELED;
        case PAID, CANCELED -> false;  // Terminal states
    };
}
```

---

### 2.4. Exception Hierarchy - Checked vs Unchecked

#### 🎓 Bản chất

**Java có 2 loại Exception:**

1. **Checked Exception** (extends `Exception`):
   - Compiler ép phải handle (try-catch hoặc throws)
   - Dùng cho lỗi **có thể recover**
   - Ví dụ: `IOException`, `SQLException`

2. **Unchecked Exception** (extends `RuntimeException`):
   - Không bắt buộc phải handle
   - Dùng cho lỗi **logic/programming error**
   - Ví dụ: `NullPointerException`, `IllegalArgumentException`

#### 🔍 Tại sao BidHub dùng RuntimeException?

**Tình huống: User đặt giá không hợp lệ**

**Nếu dùng Checked Exception:**
```java
// ❌ TỆ: Mọi nơi phải try-catch
public void placeBid(String auctionId, double amount) 
    throws InvalidBidException {  // Checked
    // Logic
}

// Client code:
try {
    auctionService.placeBid(id, amount);
} catch (InvalidBidException e) {
    // Phải handle
}

// 50 chỗ gọi placeBid() → 50 try-catch blocks!
```

**Nếu dùng Unchecked Exception:**
```java
// ✅ TỐT: Chỉ handle ở tầng cao nhất
public void placeBid(String auctionId, double amount) {
    // Logic ném InvalidBidException (unchecked)
}

// Client code:
auctionService.placeBid(id, amount);
// Không cần try-catch ở đây

// Chỉ cần 1 global exception handler:
@ExceptionHandler(BidHubException.class)
public ResponseEntity<ErrorResponse> handleBidHubException(BidHubException e) {
    return ResponseEntity.status(e.getStatusCode())
                         .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
}
```

**Lý do chọn Unchecked:**
1. **Domain logic errors** không phải network errors → unchecked hợp lý
2. **Centralized error handling** → catch 1 lần ở RequestHandler (tuần 4)
3. **Cleaner code** → không có try-catch rải rác khắp nơi

#### 💡 Tại sao cần Exception Hierarchy?

**Không có hierarchy:**
```java
// ❌ Phải catch từng exception riêng
try {
    // ...
} catch (InvalidBidException e) {
    // ...
} catch (AuctionClosedException e) {
    // ...
} catch (ItemNotFoundException e) {
    // ...
}
// 7 catch blocks!
```

**Có hierarchy:**
```java
// ✅ Catch 1 lần, handle tất cả
try {
    // ...
} catch (BidHubException e) {
    // Handle tất cả 7 subclasses
    logger.error("Error code: {}, Message: {}", e.getErrorCode(), e.getMessage());
}
```

**Cấu trúc:**
```
RuntimeException
  └── BidHubException (abstract)
        ├── InvalidBidException
        ├── AuctionClosedException
        ├── ItemNotFoundException
        ├── UserNotFoundException
        ├── UnauthorizedException
        ├── DuplicateEntityException
        └── ValidationException (có thêm List<String> errors)
```

---

### 2.5. Encapsulation - Tại sao field phải private?

#### 🎓 Bản chất

Encapsulation = **Đóng gói dữ liệu** + **Kiểm soát truy cập**

**Nguyên tắc:**
- Fields: `private` (hoặc `protected` nếu subclass cần)
- Methods: `public` nếu cần expose, `private` nếu internal

#### 🔍 Tại sao quan trọng?

**Không có Encapsulation:**
```java
// ❌ TỆ
public class User {
    public String username;  // Public field
}

// Ai cũng có thể sửa:
user.username = "";  // Rỗng!
user.username = null;  // Null!
user.username = "x".repeat(1000);  // Quá dài!
```

**Có Encapsulation:**
```java
// ✅ TỐT
public class User {
    private String username;  // Private field
    
    public User(String username) {
        validateUsername(username);  // Validate ở constructor
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }
    
    // Không có setter → username immutable
}
```

**Lợi ích:**
1. **Validation tập trung:** Chỉ validate ở setter/constructor
2. **Thay đổi implementation không ảnh hưởng client:** Field đổi từ `String` → `Enum` → client code không cần sửa
3. **Immutability:** Field `final` + không có setter → thread-safe

---

### 2.6. Polymorphism - Một interface, nhiều hình thái

#### 🎓 Bản chất

Polymorphism = **Cùng method call, khác behavior tùy object**

#### 🔍 Ví dụ trong BidHub

```java
// User là abstract, có abstract method getInfo()
public abstract class User {
    public abstract String getInfo();
}

// 3 subclasses override khác nhau
public class Bidder extends User {
    @Override
    public String getInfo() {
        return "Bidder: " + username + " - " + totalBidsPlaced + " bids";
    }
}

public class Seller extends User {
    @Override
    public String getInfo() {
        return "Seller: " + username + " - " + totalItemsListed + " items";
    }
}

public class Admin extends User {
    @Override
    public String getInfo() {
        return "Admin: " + username + " - Level " + adminLevel;
    }
}

// Client code:
List<User> users = List.of(
    new Bidder("alice", ...),
    new Seller("bob", ...),
    new Admin("eve", ...)
);

for (User user : users) {
    System.out.println(user.getInfo());  // Polymorphism!
    // Output khác nhau tùy type runtime
}
```

**Lợi ích:**
- Code generic hơn: `List<User>` thay vì `List<Bidder> + List<Seller> + List<Admin>`
- Dễ mở rộng: Thêm `Moderator extends User` → không cần sửa code cũ

---

## 3. Phân tích cấu trúc thư mục & tổ chức code

### 3.1. Tại sao cần module `bidhub-common`?

**Vấn đề trước khi có `bidhub-common`:**

```
bidhub-server/
  └── Entity.java  // Server có

bidhub-client/
  └── ??? // Client cần Entity để deserialize JSON từ server
      // Nhưng Entity ở trong bidhub-server!
      // Client phải depend vào bidhub-server
      // → Kéo theo SQLite, Server code thừa vào Client!
```

**Giải pháp: Tạo module chung**

```
bidhub-common/          ← Module KHÔNG có dependency nặng
  └── Entity.java       ← Cả server VÀ client đều dùng
  └── BidHubException   ← Client cần để hiển thị lỗi

bidhub-server/
  ├── depends on: bidhub-common
  └── User.java         ← Extends Entity từ common

bidhub-client/
  ├── depends on: bidhub-common
  └── LoginController   ← Dùng Entity để display data
```

**Nguyên tắc `bidhub-common`:**
- ✅ Có: Entity, Exception, interfaces
- ❌ KHÔNG có: SQLite, Jackson, JavaFX, Server-specific logic

### 3.2. Cây thư mục chi tiết tuần 2

```
BidHub/
├── pom.xml                           ← Parent POM (thêm module bidhub-common)
│
├── bidhub-common/                    ← MODULE MỚI
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/bidhub/common/
│       │   ├── model/
│       │   │   └── Entity.java       ← Abstract base: id, timestamps
│       │   └── exception/
│       │       ├── BidHubException.java         ← Abstract exception base
│       │       ├── InvalidBidException.java
│       │       ├── AuctionClosedException.java
│       │       ├── ItemNotFoundException.java
│       │       ├── UserNotFoundException.java
│       │       ├── UnauthorizedException.java
│       │       ├── DuplicateEntityException.java
│       │       └── ValidationException.java     ← Có List<String> errors
│       └── test/java/
│           └── ... (tests)
│
├── bidhub-server/
│   └── src/main/java/com/bidhub/server/
│       └── model/
│           ├── UserRole.java         ← Enum: BIDDER, SELLER, ADMIN
│           ├── User.java             ← Abstract extends Entity
│           ├── Bidder.java           ← Concrete: totalBidsPlaced
│           ├── Seller.java           ← Concrete: totalItemsListed
│           ├── Admin.java            ← Concrete: adminLevel
│           ├── ItemType.java         ← Enum: ELECTRONICS, ART, VEHICLE
│           ├── Item.java             ← Abstract extends Entity
│           ├── Electronics.java      ← Concrete: warranty
│           ├── Art.java              ← Concrete: artist
│           ├── Vehicle.java          ← Concrete: mileage
│           ├── ItemFactory.java      ← Static factory
│           ├── AuctionStatus.java    ← Enum with abstract methods
│           ├── Auction.java          ← Extends Entity
│           └── BidTransaction.java   ← Extends Entity
```

---

## 4. Giải thích từng class / thành phần chính

### 4.1. `Entity.java` (Đăng) - Nền tảng của mọi thứ

#### 📌 Trách nhiệm duy nhất

Cung cấp **ID duy nhất** và **timestamps** cho tất cả domain objects trong hệ thống.

#### 📌 Tại sao là abstract class?

```java
// ❌ Không có nghĩa trong domain
Entity e = new Entity();

// ✅ Có nghĩa trong domain
User user = new Bidder(...);  // User extends Entity
Item item = new Electronics(...);  // Item extends Entity
```

**3 lý do chính:**

1. **Ngăn tạo instance vô nghĩa:** Không ai "tạo một Entity" - chỉ tạo Bidder, Auction, etc.
2. **Chia sẻ state chung:** Mọi entity đều có `id`, `createdAt`, `updatedAt`
3. **Chia sẻ behavior chung:** `equals()`, `hashCode()` dựa trên `id`

#### 📌 Tại sao cần 2 constructors?

```java
// Constructor 1: Tạo mới entity (chưa có trong DB)
protected Entity() {
    this.id = UUID.randomUUID().toString();  // Tạo ID mới
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
}

// Constructor 2: Load entity từ DB (đã có id và timestamps)
protected Entity(String id, LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;  // Giữ nguyên ID từ DB
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
}
```

**Khi nào dùng cái nào?**

```java
// Tuần 2-5: Chỉ dùng constructor 1
User newUser = new Bidder("alice", "hash", "alice@mail.com");
// → UUID tự động tạo

// Tuần 6 trở đi (có DB): Dùng constructor 2
// UserDao load từ DB:
User existingUser = new Bidder(
    "550e8400-e29b-...",  // ID từ DB
    LocalDateTime.parse("2026-04-01T10:00:00"),  // createdAt từ DB
    LocalDateTime.parse("2026-04-15T14:30:00"),  // updatedAt từ DB
    "alice",
    "hash",
    "alice@mail.com"
);
```

#### 📌 Tại sao `getId()` là `final`?

```java
public final String getId() {
    return id;
}
```

**Ngăn subclass override:**

```java
// ❌ Nếu không có final
public class MaliciousBidder extends Bidder {
    @Override
    public String getId() {
        return "HACKED";  // Luôn trả về "HACKED"
    }
}

// Bug khủng khiếp:
Bidder bidder1 = new Bidder(...);
Bidder bidder2 = new Bidder(...);
System.out.println(bidder1.getId());  // "550e8400-..."
System.out.println(bidder2.getId());  // "7a3f2b1c-..."

MaliciousBidder hacker = new MaliciousBidder(...);
System.out.println(hacker.getId());  // "HACKED" → conflict với bidder khác!
```

**Với `final`:**
- Compiler ngăn override → `getId()` luôn trả về UUID thật
- Critical cho uniqueness trong DB và `equals()`

#### 📌 Tại sao `equals()` và `hashCode()` dựa trên `id`?

**Vấn đề nếu không override:**

```java
Bidder bidder1 = new Bidder("alice", "hash", "alice@mail.com");
Bidder bidder2 = new Bidder("alice", "hash", "alice@mail.com");

// Không override equals() → dùng Object.equals() → so sánh reference
System.out.println(bidder1.equals(bidder2));  // false (2 objects khác nhau trong memory)

// Nhưng trong domain: 2 Bidder cùng ID = cùng 1 người
```

**Với override dựa trên `id`:**

```java
@Override
public boolean equals(Object o) {
    if (!(o instanceof Entity other)) return false;
    return Objects.equals(id, other.id);
}
```

**Use case quan trọng:**

```java
// Tuần 6: Load User từ DB
User userFromDb = userDao.findById("550e8400-...");

// Tuần 7: Tạo User object mới với cùng ID (từ cache)
User userFromCache = new Bidder("550e8400-...", ...);

// Cần check: 2 objects này là cùng 1 user?
if (userFromDb.equals(userFromCache)) {
    // Đúng rồi, cùng ID → cùng 1 người
}

// Dùng trong Set:
Set<User> users = new HashSet<>();
users.add(userFromDb);
users.add(userFromCache);
System.out.println(users.size());  // 1 (không duplicate vì cùng ID)
```

---

### 4.2. `User.java` (Đăng) - Abstract parent của Bidder/Seller/Admin

#### 📌 Trách nhiệm

Chứa **state và behavior chung** của mọi người dùng trong hệ thống.

#### 📌 Tại sao `username` là `final`?

```java
private final String username;  // final = không thể thay đổi sau khi tạo
```

**Lý do:**
- Username là **unique identifier** (ngoài ID) trong business domain
- Cho phép đổi username → phải update index trong DB, cache, etc. → phức tạp
- Quy tắc: Username không đổi → `final` thể hiện rõ intent

**So sánh:**
```java
private final String username;       // Không đổi
private String email;                // Có thể đổi (có setter)
private String passwordHash;         // Có thể đổi (đổi mật khẩu)
```

#### 📌 Tại sao `passwordHash` thay vì `password`?

**NGUYÊN TẮC BẢO MẬT CƠ BẢN:**

```java
// ❌ NGUY HIỂM - KHÔNG BAO GIỜ LÀM THẾ NÀY
private String password;  // Plaintext password trong memory và DB

// Ai có quyền truy cập DB/memory → đọc được mật khẩu của tất cả users
// Log file có thể vô tình print password
```

**✅ AN TOÀN:**
```java
private String passwordHash;  // SHA-256 hash

// User nhập: "myPassword123"
// Lưu vào DB: "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
// → Không thể reverse hash để lấy password gốc
```

**Flow tuần 5 (Auth):**
```java
// Register:
String password = "myPassword123";
String hash = SHA256.hash(password);  // "5e884..."
user.setPasswordHash(hash);
userDao.save(user);

// Login:
String inputPassword = "myPassword123";
String inputHash = SHA256.hash(inputPassword);
if (inputHash.equals(user.getPasswordHash())) {
    // Login success
}
```

#### 📌 Tại sao có abstract method `getInfo()`?

```java
public abstract String getInfo();
```

**Demo polymorphism:**

```java
// Mỗi subclass có cách hiển thị riêng
public class Bidder extends User {
    @Override
    public String getInfo() {
        return String.format("Bidder %s - Đã đặt %d lần", 
                             getUsername(), totalBidsPlaced);
    }
}

public class Seller extends User {
    @Override
    public String getInfo() {
        return String.format("Seller %s - %d sản phẩm", 
                             getUsername(), totalItemsListed);
    }
}

// Client code:
List<User> users = getAllUsers();
for (User user : users) {
    System.out.println(user.getInfo());  // Polymorphism!
}
```

---

### 4.3. `ItemFactory.java` (Quốc Minh) - Static Factory Pattern

#### 📌 Trách nhiệm

Tạo các `Item` subclass dựa trên `ItemType` mà không để client code phụ thuộc vào concrete classes.

#### 📌 Tại sao dùng `Map<String, Object> extras`?

**Vấn đề với cách thông thường:**

```java
// ❌ Phải có 3 methods khác nhau
public static Item createElectronics(String title, double price, int warranty) {
    return new Electronics(title, price, warranty);
}

public static Item createArt(String title, double price, String artist) {
    return new Art(title, price, artist);
}

public static Item createVehicle(String title, double price, int mileage) {
    return new Vehicle(title, price, mileage);
}

// Client phải biết gọi method nào:
Item item1 = ItemFactory.createElectronics("iPhone", 20000000, 12);
Item item2 = ItemFactory.createArt("Mona Lisa", 1000000000, "Da Vinci");
```

**Với `Map<String, Object>`:**

```java
// ✅ Chỉ cần 1 method
public static Item create(ItemType type, Map<String, Object> data) {
    String title = (String) data.get("title");
    double price = (double) data.get("startingPrice");
    
    return switch (type) {
        case ELECTRONICS -> new Electronics(title, price, 
            (int) data.get("warranty"));
        case ART -> new Art(title, price, 
            (String) data.get("artist"));
        case VEHICLE -> new Vehicle(title, price, 
            (int) data.get("mileage"));
    };
}

// Client gọi đồng nhất:
Map<String, Object> data1 = Map.of(
    "title", "iPhone",
    "startingPrice", 20000000.0,
    "warranty", 12
);
Item item1 = ItemFactory.create(ItemType.ELECTRONICS, data1);

Map<String, Object> data2 = Map.of(
    "title", "Mona Lisa",
    "startingPrice", 1000000000.0,
    "artist", "Da Vinci"
);
Item item2 = ItemFactory.create(ItemType.ART, data2);
```

**Lợi ích:**
1. **Dễ parse từ JSON:** Server nhận JSON → convert thành Map → pass vào factory
2. **Extensible:** Thêm field mới → chỉ sửa trong factory, không ảnh hưởng client
3. **Uniform interface:** Mọi type đều gọi `create(type, data)`

**Trade-off:**
- ❌ Mất type safety: `Map<String, Object>` không check type compile-time
- ❌ Phải cast: `(int) data.get("warranty")` có thể throw `ClassCastException`
- ✅ Nhưng: Flexibility cao hơn, phù hợp với JSON deserialization

---

### 4.4. `AuctionStatus.java` (Công Minh) - Enum State Machine

#### 📌 Trách nhiệm

Định nghĩa 5 trạng thái của Auction và **rules chuyển đổi** giữa chúng.

#### 📌 Cấu trúc enum với abstract method

```java
public enum AuctionStatus {
    
    OPEN {
        @Override
        public boolean canBid() {
            return false;  // Chưa mở đặt giá
        }
        
        @Override
        public boolean isTerminal() {
            return false;  // Có thể chuyển sang RUNNING
        }
    },
    
    RUNNING {
        @Override
        public boolean canBid() {
            return true;  // Đang cho phép đặt giá
        }
        
        @Override
        public boolean isTerminal() {
            return false;  // Có thể chuyển sang FINISHED
        }
    },
    
    FINISHED {
        @Override
        public boolean canBid() {
            return false;  // Hết thời gian
        }
        
        @Override
        public boolean isTerminal() {
            return false;  // Có thể chuyển sang PAID
        }
    },
    
    PAID {
        @Override
        public boolean canBid() {
            return false;
        }
        
        @Override
        public boolean isTerminal() {
            return true;  // Terminal: không chuyển nữa
        }
    },
    
    CANCELED {
        @Override
        public boolean canBid() {
            return false;
        }
        
        @Override
        public boolean isTerminal() {
            return true;  // Terminal
        }
    };
    
    // Abstract methods - mỗi enum value PHẢI implement
    public abstract boolean canBid();
    public abstract boolean isTerminal();
}
```

#### 📌 Tại sao không dùng if-else?

**Cách cũ (không tốt):**
```java
public boolean canBid(AuctionStatus status) {
    if (status == AuctionStatus.RUNNING) {
        return true;
    }
    return false;
}
```

**Vấn đề:**
1. Logic nằm **bên ngoài** enum → phân tán khắp code
2. Thêm status mới → phải tìm và sửa tất cả if-else
3. Compiler không check → dễ quên case

**Cách mới (tốt):**
```java
// Logic nằm BÊN TRONG mỗi enum value
AuctionStatus.RUNNING.canBid();  // true
AuctionStatus.FINISHED.canBid();  // false

// Thêm status mới → compiler ép phải implement canBid()
```

#### 📌 State transition rules

```java
public boolean canTransitionTo(AuctionStatus newStatus) {
    return switch (this) {
        case OPEN -> newStatus == RUNNING || newStatus == CANCELED;
        case RUNNING -> newStatus == FINISHED || newStatus == CANCELED;
        case FINISHED -> newStatus == PAID || newStatus == CANCELED;
        case PAID, CANCELED -> false;  // Terminal states
    };
}
```

**Diagram:**

```
     ┌──────┐
     │ OPEN │
     └──┬───┘
        │ start()
        ↓
   ┌─────────┐
   │ RUNNING │───→ CANCELED
   └────┬────┘
        │ timeout / close
        ↓
   ┌──────────┐
   │ FINISHED │───→ CANCELED
   └────┬─────┘
        │ payment received
        ↓
     ┌──────┐
     │ PAID │ (terminal)
     └──────┘

   CANCELED (terminal)
```

---

### 4.5. `BidHubException` Hierarchy (Khoa)

#### 📌 Cấu trúc 7 + 1 exceptions

```
RuntimeException
  └── BidHubException (abstract)
        ├── InvalidBidException          (errorCode: "INVALID_BID")
        ├── AuctionClosedException       (errorCode: "AUCTION_CLOSED")
        ├── ItemNotFoundException        (errorCode: "ITEM_NOT_FOUND")
        ├── UserNotFoundException        (errorCode: "USER_NOT_FOUND")
        ├── UnauthorizedException        (errorCode: "UNAUTHORIZED")
        ├── DuplicateEntityException     (errorCode: "DUPLICATE_ENTITY")
        └── ValidationException          (errorCode: "VALIDATION_ERROR")
                                         (thêm: List<String> errors)
```

#### 📌 Tại sao cần `errorCode`?

**Vấn đề với chỉ có `message`:**

```java
throw new RuntimeException("Bid amount must be greater than current bid");
```

**Client nhận được:**
```json
{
  "error": "Bid amount must be greater than current bid"
}
```

**Vấn đề:**
1. Message bằng tiếng Anh → phải translate trên UI
2. Client không biết **loại lỗi** → không thể hiển thị icon/color phù hợp
3. Message thay đổi → client code phải sửa

**Với `errorCode`:**

```java
throw new InvalidBidException("Bid amount must be greater than current bid");
```

**Server response:**
```json
{
  "errorCode": "INVALID_BID",
  "message": "Bid amount must be greater than current bid"
}
```

**Client code:**
```javascript
if (error.errorCode === "INVALID_BID") {
    showErrorDialog("Số tiền đặt không hợp lệ", "error-icon.png");
} else if (error.errorCode === "AUCTION_CLOSED") {
    showErrorDialog("Phiên đấu giá đã đóng", "warning-icon.png");
}
```

**Lợi ích:**
- Error code **stable** (không đổi), message có thể đổi
- Client dễ dàng localize (hiển thị message tiếng Việt)
- Có thể log/monitor theo error code

#### 📌 Tại sao `ValidationException` đặc biệt?

**Use case: Form validation**

```java
// User submit form tạo Item
String title = form.get("title");  // ""
double price = form.get("price");  // -1
String description = form.get("description");  // null

// Nhiều lỗi cùng lúc:
List<String> errors = new ArrayList<>();
if (title.isBlank()) {
    errors.add("Title không được rỗng");
}
if (price <= 0) {
    errors.add("Giá khởi điểm phải > 0");
}
if (description == null) {
    errors.add("Mô tả không được null");
}

if (!errors.isEmpty()) {
    throw new ValidationException(errors);
}
```

**Không có `ValidationException`:**
```java
// ❌ Chỉ ném 1 lỗi đầu tiên
if (title.isBlank()) {
    throw new IllegalArgumentException("Title không được rỗng");
    // User fix title, submit lại → lỗi price
    // User fix price, submit lại → lỗi description
    // → Phải submit 3 lần mới biết hết lỗi!
}
```

**Có `ValidationException`:**
```javascript
// Client nhận 1 lần, hiển thị tất cả lỗi:
{
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    "Title không được rỗng",
    "Giá khởi điểm phải > 0",
    "Mô tả không được null"
  ]
}

// UI hiển thị:
// ❌ Title không được rỗng
// ❌ Giá khởi điểm phải > 0
// ❌ Mô tả không được null
```

#### 📌 Tại sao `getErrors()` trả về unmodifiable list?

```java
public List<String> getErrors() {
    return Collections.unmodifiableList(errors);
}
```

**Vấn đề nếu trả về mutable list:**

```java
ValidationException e = new ValidationException(List.of("Lỗi 1"));
List<String> errors = e.getErrors();

// ❌ Client có thể sửa nội dung exception!
errors.add("Lỗi giả mạo");
errors.clear();

// Bug khủng khiếp: Exception object bị mutation sau khi ném
```

**Với unmodifiable list:**
```java
errors.add("Lỗi giả mạo");  // → UnsupportedOperationException
```

**Nguyên tắc:** Exception phải **immutable** sau khi tạo → không ai sửa được message/errors.

---

## 5. Flow xử lý nghiệp vụ chính trong tuần 2

### 5.1. Flow: Tạo User mới và lưu vào danh sách

```
1. Client tạo Bidder mới
   ↓
   Bidder bidder = new Bidder("alice", "hash123", "alice@example.com");
   ↓
2. Constructor gọi super() → User constructor
   ↓
3. User constructor gọi super() → Entity constructor
   ↓
4. Entity constructor:
   - Tạo UUID: "550e8400-e29b-41d4-..."
   - Set createdAt = LocalDateTime.now()
   - Set updatedAt = createdAt
   ↓
5. User constructor:
   - Validate username (≥3 chars)
   - Set username, passwordHash, email, role
   ↓
6. Bidder constructor:
   - Set totalBidsPlaced = 0
   ↓
7. Object hoàn chỉnh:
   Bidder {
     id: "550e8400-...",
     createdAt: "2026-04-15T10:00:00",
     updatedAt: "2026-04-15T10:00:00",
     username: "alice",
     passwordHash: "hash123",
     email: "alice@example.com",
     role: BIDDER,
     totalBidsPlaced: 0
   }
```

### 5.2. Flow: Polymorphism khi display User info

```
1. Có List<User> chứa 3 types:
   List<User> users = List.of(
       new Bidder("alice", ...),
       new Seller("bob", ...),
       new Admin("eve", ...)
   );
   ↓
2. Loop qua list:
   for (User user : users) {
       System.out.println(user.getInfo());
   }
   ↓
3. Lần 1: user = Bidder instance
   → JVM gọi Bidder.getInfo()
   → Output: "Bidder alice - 5 bids"
   ↓
4. Lần 2: user = Seller instance
   → JVM gọi Seller.getInfo()
   → Output: "Seller bob - 12 items"
   ↓
5. Lần 3: user = Admin instance
   → JVM gọi Admin.getInfo()
   → Output: "Admin eve - Level 3"
   ↓
6. Polymorphism: Cùng method call `getInfo()`,
   nhưng behavior khác nhau tùy runtime type
```

### 5.3. Flow: Factory tạo Item từ JSON

```
1. Server nhận JSON từ Client:
   {
     "type": "CREATE_ITEM",
     "itemType": "ELECTRONICS",
     "title": "iPhone 15 Pro",
     "startingPrice": 25000000,
     "warranty": 24
   }
   ↓
2. RequestHandler parse JSON:
   Map<String, Object> data = parseJson(request);
   ItemType type = ItemType.valueOf(data.get("itemType"));
   ↓
3. Gọi Factory:
   Item item = ItemFactory.create(type, data);
   ↓
4. Factory switch trên type:
   case ELECTRONICS:
     → new Electronics(
         data.get("title"),
         data.get("startingPrice"),
         data.get("warranty")
       )
   ↓
5. Electronics constructor:
   - Gọi super(...) → Item constructor
   - Item gọi super() → Entity constructor
   - Entity tạo UUID
   - Set warranty field
   ↓
6. Return Electronics instance (nhưng type là Item)
   ↓
7. Client code chỉ biết: Item item
   Không biết đó là Electronics, Art hay Vehicle
   → Loose coupling
```

### 5.4. Flow: Auction state transition

```
1. Tạo Auction mới:
   Auction auction = new Auction(item, seller, ...);
   → status = OPEN (default)
   ↓
2. Seller start auction:
   auction.transitionTo(AuctionStatus.RUNNING);
   ↓
3. transitionTo() check:
   if (!status.canTransitionTo(RUNNING)) {
       throw new IllegalStateException(...);
   }
   ↓
4. Check rule:
   OPEN.canTransitionTo(RUNNING)?
   → switch(OPEN):
      case OPEN -> newStatus == RUNNING || CANCELED
   → RUNNING == RUNNING? YES
   → Return true
   ↓
5. Transition thành công:
   this.status = RUNNING;
   markUpdated();  // updatedAt = now
   ↓
6. User thử đặt giá:
   if (auction.getStatus().canBid()) {
       // Place bid
   }
   ↓
7. Check:
   RUNNING.canBid()?
   → RUNNING override canBid() { return true; }
   → Cho phép đặt giá
   ↓
8. Auction timeout → server auto:
   auction.transitionTo(AuctionStatus.FINISHED);
   ↓
9. User thử đặt giá lại:
   if (auction.getStatus().canBid()) {
       // Place bid
   }
   → FINISHED.canBid() = false
   → Không cho phép
   ↓
10. Winner thanh toán:
    auction.transitionTo(AuctionStatus.PAID);
    → FINISHED.canTransitionTo(PAID)? YES
    → Status = PAID
    → PAID.isTerminal() = true
    → Không thể transition tiếp
```

---

## 6. Các quyết định thiết kế quan trọng

### 6.1. Tại sao Entity có `equals()` và `hashCode()` dựa trên `id`?

**Quyết định:**
```java
@Override
public boolean equals(Object o) {
    if (!(o instanceof Entity other)) return false;
    return Objects.equals(id, other.id);
}

@Override
public int hashCode() {
    return Objects.hash(id);
}
```

**Lý do chi tiết:**

**1. Domain semantics:**
```java
// Trong business domain:
// 2 Bidder objects cùng id = cùng 1 người trong thế giới thực
// Dù 2 objects khác nhau trong memory

Bidder bidder1 = userDao.findById("abc123");
Bidder bidder2 = cache.get("abc123");

// bidder1 và bidder2 là 2 objects khác nhau
// nhưng đại diện cho CÙNG 1 người
// → equals() phải return true
```

**2. Collections behavior:**
```java
Set<User> users = new HashSet<>();
User user1 = new Bidder("alice", ...);  // id = "abc123"
User user2 = new Bidder("alice", ...);  // id = "abc123" (load từ DB)

users.add(user1);
users.add(user2);

// Không override equals/hashCode → Set nghĩ 2 objects khác nhau → size = 2
// Override dựa trên id → Set biết cùng 1 user → size = 1 (correct!)
```

**3. Database synchronization:**
```java
// Tuần 6: Update user trong DB
User userFromUi = new Bidder("abc123", "alice", ...);  // UI data
User userFromDb = userDao.findById("abc123");          // DB data

if (userFromUi.equals(userFromDb)) {
    // Không cần update DB
} else {
    // Có thay đổi, update DB
}

// Nếu equals() dựa trên ALL fields → mỗi lần updatedAt thay đổi → equals false
// Nếu equals() dựa trên id → check đúng nghĩa domain
```

### 6.2. Tại sao `User` có abstract method `getInfo()`?

**Quyết định:**
```java
public abstract class User {
    public abstract String getInfo();  // Force subclasses implement
}
```

**Lý do:**

**1. Polymorphism demo:**
```java
// Cùng 1 method signature, behavior khác nhau
List<User> users = getAllUsers();
for (User user : users) {
    System.out.println(user.getInfo());  // Dynamic dispatch
}

// Output:
// Bidder alice - 15 bids
// Seller bob - 8 items
// Admin eve - Level 5
```

**2. Compiler enforcement:**
```java
// Nếu không có abstract method:
public class User {
    public String getInfo() {
        return "User: " + username;  // Generic
    }
}

// Subclass CÓ THỂ override, nhưng không BẮT BUỘC
public class Bidder extends User {
    // Quên override → getInfo() trả về generic string
}

// Với abstract method:
public class Bidder extends User {
    // Compiler error nếu không override getInfo()
}
```

**3. Design intent:**
```java
// Abstract method nói rằng:
// "User concept quá chung chung để có info cụ thể.
//  Mỗi loại user (Bidder/Seller/Admin) phải tự define info của mình."
```

### 6.3. Tại sao `ItemFactory` dùng `Map<String, Object>` thay vì typed parameters?

**Quyết định:**
```java
public static Item create(ItemType type, Map<String, Object> data) {
    // Extract từ Map
}
```

**Thay vì:**
```java
// Option 1: Overload methods
public static Item createElectronics(String title, double price, int warranty);
public static Item createArt(String title, double price, String artist);
public static Item createVehicle(String title, double price, int mileage);

// Option 2: Builder pattern
Item item = new ItemBuilder()
    .title("iPhone")
    .price(20000000)
    .warranty(12)
    .build();
```

**Lý do chọn Map:**

**1. JSON integration:**
```java
// Server nhận JSON request
String jsonString = receiveFromClient();
Map<String, Object> data = JsonParser.parse(jsonString);

// Dễ dàng pass thẳng vào factory
Item item = ItemFactory.create(ItemType.ELECTRONICS, data);

// Không cần:
// int warranty = (int) data.get("warranty");
// String title = (String) data.get("title");
// ...
// Item item = ItemFactory.createElectronics(title, price, warranty);
```

**2. Extensibility:**
```java
// Thêm field mới cho Electronics: "color"
// Chỉ cần:
// 1. Thêm field trong Electronics class
// 2. Update Electronics constructor
// 3. Update factory extract logic

// KHÔNG CẦN thay đổi factory method signature
// KHÔNG CẦN client code sửa lại
```

**3. Uniform interface:**
```java
// Mọi item type đều tạo bằng cùng 1 cách
Item item1 = ItemFactory.create(ELECTRONICS, data1);
Item item2 = ItemFactory.create(ART, data2);
Item item3 = ItemFactory.create(VEHICLE, data3);

// Không phải nhớ: method nào dùng cho type nào
```

**Trade-off:**
- ❌ **Mất type safety:** Compile-time không check keys trong Map
- ❌ **Runtime errors:** `ClassCastException` nếu type sai
- ✅ **Flexibility:** Dễ integrate với JSON, dễ extend

→ Chấp nhận trade-off vì **benefit > cost** trong context BidHub.

### 6.4. Tại sao `AuctionStatus` là enum thay vì String constants?

**Quyết định:**
```java
public enum AuctionStatus {
    OPEN, RUNNING, FINISHED, PAID, CANCELED;
}
```

**Thay vì:**
```java
public class AuctionStatus {
    public static final String OPEN = "OPEN";
    public static final String RUNNING = "RUNNING";
    ...
}
```

**Lý do:**

**1. Type safety:**
```java
// Với String constants:
auction.setStatus("RUNNNNING");  // Typo! Runtime mới phát hiện

// Với enum:
auction.setStatus(AuctionStatus.RUNNNNING);  // Compile error
auction.setStatus(AuctionStatus.RUNNING);     // OK
```

**2. Limited set of values:**
```java
// String có thể là bất kỳ giá trị nào
String status = "INVALID_STATUS";  // Không có lỗi

// Enum chỉ có 5 giá trị cố định
// Không thể tạo AuctionStatus không hợp lệ
```

**3. Switch statement exhaustiveness:**
```java
// Với enum, compiler check switch có đủ cases
String displayName = switch(status) {
    case OPEN -> "Chờ bắt đầu";
    case RUNNING -> "Đang diễn ra";
    // Nếu thiếu case → compile error (với Java 17+)
};
```

**4. Behavior attachment:**
```java
// Enum có thể có methods
public enum AuctionStatus {
    OPEN {
        @Override
        public boolean canBid() { return false; }
    },
    ...
    
    public abstract boolean canBid();
}

// String constants không thể có behavior
```

### 6.5. Tại sao `BidHubException` extends `RuntimeException` (unchecked)?

**Quyết định:**
```java
public abstract class BidHubException extends RuntimeException {
    // Unchecked exception
}
```

**Thay vì:**
```java
public abstract class BidHubException extends Exception {
    // Checked exception
}
```

**Lý do:**

**1. Domain logic errors ≠ I/O errors:**
```java
// Checked exceptions phù hợp cho I/O, network:
try {
    FileInputStream fis = new FileInputStream("file.txt");
} catch (FileNotFoundException e) {
    // File không tồn tại - có thể recovery
}

// Domain logic errors:
if (bidAmount <= currentBid) {
    throw new InvalidBidException("Bid too low");
    // Không thể recovery - người dùng phải sửa input
}
```

**2. Cleaner code:**
```java
// Với checked exception:
public void placeBid(...) throws InvalidBidException,
                                 AuctionClosedException,
                                 UnauthorizedException {
    // Logic
}

// Mỗi caller phải:
try {
    service.placeBid(...);
} catch (InvalidBidException | AuctionClosedException | UnauthorizedException e) {
    // Handle
}

// 50 chỗ gọi → 50 try-catch blocks!

// Với unchecked:
public void placeBid(...) {
    // Logic - throw runtime exceptions
}

// Caller không bắt buộc try-catch
service.placeBid(...);

// Chỉ catch ở top-level handler:
@ExceptionHandler(BidHubException.class)
public void handleBidHubException(BidHubException e) {
    // Centralized handling
}
```

**3. Spring/framework best practice:**
```java
// Spring MVC, JAX-RS, etc. đều prefer unchecked exceptions
// Throw từ service layer → framework tự catch và convert thành HTTP response
```

### 6.6. Tại sao `Electronics`, `Art`, `Vehicle` là `final class`?

**Quyết định:**
```java
public final class Electronics extends Item {
    // Cannot be extended
}
```

**Lý do:**

**1. Domain completeness:**
```java
// Domain model: Electronics, Art, Vehicle là leaf nodes
// Không có concept "subtype của Electronics"
// → final thể hiện rõ design intent

// Nếu không final:
public class Smartphone extends Electronics {
    // Có cần thiết không?
    // Smartphone và Laptop đều là Electronics
    // Nhưng không cần subclass riêng
    // → Dùng extras field trong Item là đủ
}
```

**2. Prevent fragile base class:**
```java
// Nếu cho phép extends:
public class SpecialElectronics extends Electronics {
    @Override
    public double calculateShippingFee() {
        // Override behavior → có thể break logic của Item
    }
}
```

**3. Implementation detail:**
```java
// Electronics, Art, Vehicle là implementation details
// Client chỉ nên biết đến Item interface/abstract class
// → final ngăn client tạo thêm subtypes
```

---

## 7. Những lỗi thường gặp khi code tuần 2 và cách tránh

### 7.1. Lỗi: Quên gọi `super()` trong constructor của subclass

**Triệu chứng:**
```java
public class Bidder extends User {
    public Bidder(String username, String passwordHash, String email) {
        // Quên gọi super(...)
        this.totalBidsPlaced = 0;
    }
}

// Compile error: "There is no default constructor available in 'User'"
```

**Nguyên nhân:**
- Java tự động thêm `super()` (không tham số) nếu không gọi explicit
- Nhưng `User` không có constructor không tham số
- → Compile error

**Cách fix:**
```java
public Bidder(String username, String passwordHash, String email) {
    super(username, passwordHash, email, UserRole.BIDDER);  // Gọi User constructor
    this.totalBidsPlaced = 0;
}
```

### 7.2. Lỗi: Không override `equals()` và `hashCode()` cùng nhau

**Triệu chứng:**
```java
public class Entity {
    @Override
    public boolean equals(Object o) {
        // Override equals
        return Objects.equals(id, ((Entity) o).id);
    }
    
    // QUÊN override hashCode()
}

// Bug:
Set<Entity> set = new HashSet<>();
Entity e1 = new Bidder("alice", ...);
Entity e2 = new Bidder("alice", ...);  // Cùng id

set.add(e1);
set.add(e2);
System.out.println(set.size());  // 2 (WRONG! Phải là 1)
```

**Nguyên nhân:**
- `HashSet` dùng `hashCode()` để tìm bucket
- Nếu không override `hashCode()` → dùng default (dựa trên memory address)
- `e1` và `e2` có hashCode khác nhau → vào 2 buckets khác nhau
- → Set nghĩ là 2 objects khác nhau

**Cách fix:**
```java
@Override
public boolean equals(Object o) {
    return Objects.equals(id, ((Entity) o).id);
}

@Override
public int hashCode() {
    return Objects.hash(id);  // PHẢI override cùng
}
```

**Quy tắc vàng:** Override `equals()` → BẮT BUỘC override `hashCode()` với cùng fields.

### 7.3. Lỗi: Constructor không validate input

**Triệu chứng:**
```java
public class User extends Entity {
    public User(String username, ...) {
        this.username = username;  // Không validate
    }
}

// Bug:
User user = new User("", "hash", "email");  // Username rỗng!
User user2 = new User(null, "hash", "email");  // Username null!
```

**Cách fix:**
```java
public User(String username, ...) {
    validateUsername(username);  // Validate TRƯỚC khi assign
    this.username = username;
}

private static void validateUsername(String username) {
    if (username == null || username.isBlank()) {
        throw new IllegalArgumentException("Username không được null hoặc rỗng");
    }
    if (username.length() < 3) {
        throw new IllegalArgumentException("Username phải ≥ 3 ký tự");
    }
}
```

**Nguyên tắc:** Constructor là entry point duy nhất → validate tất cả inputs ở đây.

### 7.4. Lỗi: `ValidationException` trả về mutable `List<String>`

**Triệu chứng:**
```java
public class ValidationException extends BidHubException {
    private final List<String> errors;
    
    public List<String> getErrors() {
        return errors;  // Trả về reference trực tiếp
    }
}

// Bug:
ValidationException e = new ValidationException(List.of("Lỗi 1"));
List<String> errors = e.getErrors();
errors.add("Lỗi giả");  // Sửa được nội dung exception!
```

**Cách fix:**
```java
public List<String> getErrors() {
    return Collections.unmodifiableList(errors);  // Trả về unmodifiable view
}

// Hoặc:
public List<String> getErrors() {
    return List.copyOf(errors);  // Trả về defensive copy
}
```

### 7.5. Lỗi: `ItemFactory` không check null trong Map

**Triệu chứng:**
```java
public static Item create(ItemType type, Map<String, Object> data) {
    String title = (String) data.get("title");  // Có thể null
    double price = (double) data.get("startingPrice");  // NPE nếu null
    
    return new Electronics(title, price, ...);
}

// Bug:
Map<String, Object> badData = Map.of("title", "iPhone");
// Thiếu "startingPrice" key
Item item = ItemFactory.create(ELECTRONICS, badData);
// → NullPointerException
```

**Cách fix:**
```java
public static Item create(ItemType type, Map<String, Object> data) {
    // Check required keys
    requireKey(data, "title");
    requireKey(data, "startingPrice");
    
    String title = (String) data.get("title");
    double price = ((Number) data.get("startingPrice")).doubleValue();
    
    return switch (type) {
        case ELECTRONICS -> {
            requireKey(data, "warranty");
            int warranty = ((Number) data.get("warranty")).intValue();
            yield new Electronics(title, price, warranty);
        }
        // ...
    };
}

private static void requireKey(Map<String, Object> data, String key) {
    if (!data.containsKey(key) || data.get(key) == null) {
        throw new IllegalArgumentException("Missing required key: " + key);
    }
}
```

### 7.6. Lỗi: `Auction.transitionTo()` không check rule

**Triệu chứng:**
```java
public void transitionTo(AuctionStatus newStatus) {
    this.status = newStatus;  // Không check rule
}

// Bug:
auction.setStatus(AuctionStatus.FINISHED);
auction.transitionTo(AuctionStatus.OPEN);  // KHÔNG HỢP LỆ nhưng vẫn được
```

**Cách fix:**
```java
public void transitionTo(AuctionStatus newStatus) {
    if (!status.canTransitionTo(newStatus)) {
        throw new IllegalStateException(
            String.format("Cannot transition from %s to %s", status, newStatus)
        );
    }
    this.status = newStatus;
    markUpdated();
}
```

### 7.7. Lỗi: Test không đủ coverage

**Triệu chứng:**
```bash
mvn test
# Tests run: 25

# Nhưng:
# - Không test edge case: username = null
# - Không test exception: new User(null, ...)
# - Không test polymorphism: List<User> với 3 types
```

**Cách fix - Checklist test cases:**

**Entity tests:**
- [ ] Tạo entity mới → id tự động generate
- [ ] 2 entities mới → ids khác nhau
- [ ] equals() dựa trên id
- [ ] hashCode() consistent với equals()

**User tests:**
- [ ] Username null → IllegalArgumentException
- [ ] Username < 3 chars → IllegalArgumentException
- [ ] Polymorphism: List<User> gọi getInfo() → 3 outputs khác nhau

**Factory tests:**
- [ ] create(ELECTRONICS) → instanceof Electronics
- [ ] Missing required key → IllegalArgumentException
- [ ] Invalid type → exception

**Auction tests:**
- [ ] New auction → status = OPEN
- [ ] transitionTo(RUNNING) từ OPEN → success
- [ ] transitionTo(OPEN) từ FINISHED → IllegalStateException

**Exception tests:**
- [ ] Mọi subclass instanceof BidHubException
- [ ] ValidationException.getErrors() không null
- [ ] ValidationException.getErrors().add() → UnsupportedOperationException

---

## 8. Tài liệu tham khảo & câu hỏi kiểm tra

### 8.1. Câu hỏi bắt buộc cho Đăng (Entity + User)

**1. Tại sao Entity có 2 constructor? Khi nào dùng cái nào?**

**Đáp án:**
- Constructor 1 (không tham số): Tạo entity MỚI, chưa có trong DB → tự tạo UUID
- Constructor 2 (có id, timestamps): Load entity TỪ DB → giữ nguyên id và timestamps từ DB
- Nếu chỉ có 1 constructor: Load từ DB sẽ tạo UUID mới → mất ID gốc → bug nghiêm trọng

**2. `markUpdated()` là `protected final` — tại sao `final`? Nếu subclass override để không làm gì thì sẽ xảy ra vấn đề gì?**

**Đáp án:**
- `final` → subclass KHÔNG THỂ override
- Nếu override để không làm gì:
```java
class MaliciousUser extends User {
    @Override
    protected void markUpdated() {
        // Không làm gì
    }
}
// → updatedAt không thay đổi khi user thay đổi email
// → DB không biết record này đã update
// → Cache không invalidate
```

**3. `equals()` và `hashCode()` dựa trên `id` — nếu không override, điều gì xảy ra khi dùng `Set<User>`?**

**Đáp án:**
- Không override → dùng Object.equals() → so sánh memory address
- 2 User objects cùng id nhưng khác address → equals() = false
- → `Set<User>` chứa 2 User cùng id → bug (domain sai: 1 user xuất hiện 2 lần)

**4. Tại sao `bidhub-common/pom.xml` không có dependency `sqlite-jdbc`? Hậu quả nếu thêm vào?**

**Đáp án:**
- `bidhub-common` dùng chung cho server VÀ client
- Client không cần SQLite → nếu common có SQLite → client kéo theo → JAR nặng thừa
- Nguyên tắc: Common chỉ chứa pure Java code, không có external heavy dependencies

---

### 8.2. Câu hỏi bắt buộc cho Quốc Minh (Item + Factory)

**1. `ItemFactory.create()` là Static Factory hay Factory Method Pattern? Phân biệt?**

**Đáp án:**
- **Static Factory Method** (đơn giản hơn)
- Factory Method Pattern (GoF): Cần abstract creator class, subclass override
- Static Factory: 1 static method trong utility class
- BidHub dùng Static Factory vì đủ đơn giản, không cần phức tạp của GoF pattern

**2. Tại sao dùng `Map<String, Object> extras` thay vì parameter riêng? Trade-off?**

**Đáp án:**
- **Lợi ích:** Dễ integrate JSON, uniform interface, extensible
- **Trade-off:** Mất type safety, runtime errors, phải cast
- Chấp nhận trade-off vì benefit lớn hơn trong context server nhận JSON

**3. Nếu thêm `ItemType.JEWELRY` sau này, phải sửa những file nào?**

**Đáp án:**
```
1. ItemType.java (thêm enum value JEWELRY)
2. Jewelry.java (tạo class mới extends Item)
3. ItemFactory.java (thêm case JEWELRY trong switch)
4. ItemFactoryTest.java (thêm test cases)

KHÔNG CẦN sửa:
- Client code gọi factory
- Server request handler
- JSON protocol
```

**4. `Electronics` là `final class` — tại sao? Nếu bỏ `final` thì vấn đề gì?**

**Đáp án:**
- `final` → không cho extends
- Lý do: Electronics là leaf node trong domain, không có subtype
- Nếu bỏ `final`: Ai đó có thể `class Smartphone extends Electronics` → fragile base class, phức tạp hóa hierarchy không cần thiết

---

### 8.3. Câu hỏi bắt buộc cho Công Minh (Auction + Status)

**1. `AuctionStatus` có abstract method trong enum — Java cho phép không? Giải thích cơ chế.**

**Đáp án:**
- **Có**, Java enum có thể có abstract methods
- Cơ chế: Mỗi enum value (OPEN, RUNNING, ...) là 1 **anonymous subclass** của enum
- Mỗi value PHẢI implement abstract method → compiler enforce

**2. `isValidBid()` kiểm tra `bidAmount > currentHighestBid` — tại sao `>` không phải `>=`?**

**Đáp án:**
- Business rule: Bid mới phải **cao hơn** bid hiện tại
- Nếu `>=`: User A bid 100, User B bid 100 → 2 bids bằng nhau → không rõ ai thắng
- Với `>`: Buộc mỗi bid sau phải cao hơn → luôn có 1 highest bid rõ ràng

**3. `BidTransaction` không có setter — tại sao? Có phải mọi class đều nên immutable?**

**Đáp án:**
- BidTransaction là **transaction record** → không được sửa sau khi tạo (tính audit)
- Không phải mọi class đều immutable: `User` có `setEmail()`, `Auction` có `updateHighestBid()`
- Quy tắc: Transaction/Event/Value Object → immutable; Entity với mutable state → có setters

**4. `extendEndTime()` check `newEndTime.isAfter(endTime)` — quan trọng thế nào với Anti-Sniping tuần 8?**

**Đáp án:**
- Anti-Sniping: Tự động gia hạn nếu có bid trong phút cuối
- Check `isAfter()` ngăn move endTime về quá khứ → logic nhất quán
- Tuần 8: Mỗi bid cuối → `auction.extendEndTime(endTime.plusSeconds(60))` → kiểm tra đảm bảo không giảm thời gian

---

### 8.4. Câu hỏi bắt buộc cho Khoa (Exception Hierarchy)

**1. `ValidationException` có 2 constructor — design quyết định này có hợp lý không?**

**Đáp án:**
```java
// Constructor 1: Nhiều lỗi
public ValidationException(List<String> errors)

// Constructor 2: 1 lỗi (convenience)
public ValidationException(String message)
```
- **Hợp lý**: Đa số case có nhiều lỗi, nhưng đôi khi chỉ 1 lỗi → cần convenience constructor
- Alternative: Chỉ có constructor 1, caller dùng `new ValidationException(List.of("Lỗi"))` → dài dòng

**2. Tại sao `getErrors()` trả về `unmodifiableList`? Nếu trả về `ArrayList` thì vấn đề gì?**

**Đáp án:**
- `ArrayList` mutable → caller có thể `errors.add()` → mutation exception sau khi throw
- Exception phải immutable → nội dung không đổi sau khi tạo
- `unmodifiableList` → `add()` throw `UnsupportedOperationException`

**3. `DomainIntegrationTest` test cái gì mà `ExceptionHierarchyTest` không test?**

**Đáp án:**
- `ExceptionHierarchyTest`: Test exception hierarchy structure (instanceof, errorCode)
- `DomainIntegrationTest`: Test exceptions được throw từ **domain logic** (User, Item, Auction)
  - Ví dụ: `new User(null, ...)` → `IllegalArgumentException` hay `ValidationException`?
  - `ItemFactory.create()` với bad data → exception type nào?

**4. Tổng số test cases là bao nhiêu? Cách tính để đảm bảo ≥ 40?**

**Đáp án:**
```
Tuần 1: 15 test cases (Calculator)
Tuần 2:
- Entity: 5 tests
- User hierarchy: 8 tests
- Item + Factory: 8 tests
- Auction: 6 tests
- Exception: 8 tests
Total tuần 2: 35 tests

Grand total: 15 + 35 = 50 tests (đạt ≥ 40)
```

---

## 9. Kết luận – Tuần 2 đã đặt nền móng cho những gì ở các tuần sau?

### 9.1. Domain Foundation → Tuần 3-10

**Tuần 2 tạo:**
```
Entity (base)
├── User
│   ├── Bidder
│   ├── Seller
│   └── Admin
├── Item
│   ├── Electronics
│   ├── Art
│   └── Vehicle
├── Auction
└── BidTransaction

BidHubException
├── InvalidBidException
├── AuctionClosedException
├── ItemNotFoundException
├── UserNotFoundException
├── UnauthorizedException
├── DuplicateEntityException
└── ValidationException
```

**Tuần 3-10 sử dụng:**

**Tuần 3 (DAO):**
```java
public class UserDao {
    public void save(User user) { ... }  // Dùng User từ tuần 2
    public User findById(String id) { ... }
}
```

**Tuần 4 (Socket):**
```java
public class RequestHandler {
    public void handle(Request request) {
        try {
            // Process
        } catch (BidHubException e) {
            // Handle exceptions từ tuần 2
            sendErrorResponse(e.getErrorCode(), e.getMessage());
        }
    }
}
```

**Tuần 5 (Auth):**
```java
public class AuthService {
    public User login(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user == null) {
            throw new UserNotFoundException(username);  // Exception tuần 2
        }
        // ...
        return user;  // User từ tuần 2
    }
}
```

**Tuần 6 (Bidding):**
```java
public class BiddingService {
    public void placeBid(String auctionId, String bidderId, double amount) {
        Auction auction = auctionDao.findById(auctionId);
        if (!auction.getStatus().canBid()) {
            throw new AuctionClosedException(auctionId);  // Exception tuần 2
        }
        if (!auction.isValidBid(amount)) {
            throw new InvalidBidException(amount);  // Exception tuần 2
        }
        // Create BidTransaction từ tuần 2
    }
}
```

### 9.2. Mở rộng: Thêm User type mới (Moderator)

**Giả sử tuần 7 cần thêm `Moderator` extends `User`:**

**Phải sửa:**
```
1. UserRole.java
   + Thêm enum value: MODERATOR

2. Moderator.java (file mới)
   + extends User
   + Thêm fields riêng: moderatorPermissions
   + Override getInfo()

3. UserDao.java (tuần 3)
   + Update save() để handle Moderator type

4. AuthService.java (tuần 5)
   + Update createUser() để support MODERATOR role
```

**KHÔNG cần sửa:**
```
✅ Entity.java (base class)
✅ User.java (abstract parent)
✅ Bidder.java, Seller.java, Admin.java (siblings)
✅ Client code dùng List<User>
✅ Database schema (chỉ thêm column moderatorPermissions)
```

→ **Kiến trúc OOP tốt = mở rộng dễ dàng, ít ảnh hưởng code cũ**

### 9.3. Mở rộng: Thêm Item type mới (Jewelry)

**Phải sửa:**
```
1. ItemType.java
   + Thêm enum value: JEWELRY

2. Jewelry.java (file mới)
   + extends Item
   + Thêm fields: gemType, caratWeight

3. ItemFactory.java
   + Thêm case JEWELRY trong switch

4. ItemFactoryTest.java
   + Thêm test cases cho JEWELRY
```

**KHÔNG cần sửa:**
```
✅ Entity.java
✅ Item.java (abstract parent)
✅ Electronics.java, Art.java, Vehicle.java (siblings)
✅ Client code gọi ItemFactory.create()
✅ Server RequestHandler
```

### 9.4. Nguyên tắc thiết kế đã áp dụng

**1. Open-Closed Principle (OCP):**
- Open for extension: Thêm subclass mới (Moderator, Jewelry)
- Closed for modification: Không sửa base classes (Entity, User, Item)

**2. Liskov Substitution Principle (LSP):**
- `List<User>` có thể chứa Bidder, Seller, Admin → hoạt động đúng
- `Item item = factory.create(...)` → không cần biết concrete type

**3. Dependency Inversion Principle (DIP):**
- Client code phụ thuộc vào abstraction (User, Item)
- Không phụ thuộc vào concrete classes (Bidder, Electronics)

**4. Single Responsibility Principle (SRP):**
- Entity: chỉ quản lý ID và timestamps
- User: chỉ quản lý user data chung
- Bidder: chỉ quản lý bidder-specific data
- ItemFactory: chỉ lo tạo Items

---

## 🎓 Tổng kết

Tuần 2 là tuần **nền tảng domain** - xây dựng bộ xương của toàn bộ hệ thống. Những class được tạo ra tuần này sẽ được sử dụng ở **MỌI tuần sau**.

**Điểm mấu chốt:**

1. **OOP không phải về syntax** - về thiết kế giải quyết vấn đề thực tế
2. **Abstract class vs Interface** - hiểu rõ khi nào dùng cái gì
3. **Factory Pattern** - tách creation logic khỏi client code
4. **State Machine** - model business rules rõ ràng
5. **Exception Hierarchy** - centralized error handling

**Checklist cuối tuần 2:**
- [ ] Entity có equals/hashCode dựa trên id
- [ ] User hierarchy có 3 subclasses với polymorphism
- [ ] Item hierarchy có 3 subclasses + Factory
- [ ] Auction có state machine với validation
- [ ] Exception hierarchy có 7 subclasses
- [ ] ≥ 40 test cases pass (15 tuần 1 + 25 tuần 2)
- [ ] Mỗi người giải thích được code của người khác

**Nếu đạt được checklist trên → Tuần 2 hoàn thành xuất sắc = 2.5đ trong tay!** 🎉
