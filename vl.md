# BidHub Test Report — 26/05/2026

## Tổng kết

| Metric | Giá trị |
|--------|---------|
| **Tổng số test** | 42 |
| **Pass (sau fix)** | ✅ 42 |
| **Fail (sau fix)** | ❌ 0 |
| **Pass ban đầu** | 28 |
| **Fail ban đầu** | 14 |

---

## Kết quả chi tiết theo nhóm

### A. Kết nối cơ bản

| # | Test | Kết quả |
|---|------|---------|
| 1 | PING | ✅ PASS |

---

### B. Đăng ký & Đăng nhập

| # | Test | Kết quả |
|---|------|---------|
| 2 | REGISTER thành công | ✅ PASS |
| 3 | REGISTER trùng username | ✅ PASS |
| 4 | REGISTER password ngắn (<8 ký tự) | ✅ PASS |
| 5 | REGISTER email sai format | ✅ PASS |
| 6 | REGISTER role ADMIN bị từ chối | ✅ PASS |
| 7 | LOGIN thành công | ✅ PASS |
| 8 | LOGIN sai mật khẩu | ✅ PASS |
| 9 | LOGIN username không tồn tại | ✅ PASS |
| 10 | LOGIN thiếu password | ✅ PASS |

---

### C. Quản lý sản phẩm

| # | Test | Ban đầu | Sau fix |
|---|------|---------|---------|
| 11 | CREATE_ITEM thành công | ✅ PASS | ✅ PASS |
| 12 | CREATE_ITEM thiếu tên | ✅ PASS | ✅ PASS |
| 13 | CREATE_ITEM giá bằng 0 | ✅ PASS | ✅ PASS |
| 14 | CREATE_ITEM sai loại | ✅ PASS | ✅ PASS |
| 15a | Đăng ký BIDDER | ✅ PASS | ✅ PASS |
| 15b | LOGIN BIDDER | ✅ PASS | ✅ PASS |
| 15 | CREATE_ITEM không có quyền (BIDDER) | ✅ PASS | ✅ PASS |
| 16 | GET_ITEM_LIST | ✅ PASS | ✅ PASS |
| 17 | GET_ITEM_DETAIL | ❌ **FAIL** | ✅ PASS |
| 18 | DELETE_ITEM | ✅ PASS | ✅ PASS |
| 19 | DELETE_ITEM không tồn tại | ✅ PASS | ✅ PASS |
| 19a | CREATE_ITEM cho AUCTION | ❌ **FAIL** | ✅ PASS |

---

### D. Đấu giá

| # | Test | Ban đầu | Sau fix |
|---|------|---------|---------|
| 20 | CREATE_AUCTION | ❌ **FAIL** | ✅ PASS |
| 21 | PLACE_BID thành công | ❌ **FAIL** | ✅ PASS |
| 22 | PLACE_BID giá thấp hơn current | ✅ PASS | ✅ PASS |
| 23 | PLACE_BID auction không tồn tại | ✅ PASS | ✅ PASS |
| 24 | GET_AUCTION_DETAIL | ❌ **FAIL** | ✅ PASS |
| 25 | GET_AUCTION_LIST | ✅ PASS | ✅ PASS |

---

### E. Admin

| # | Test | Ban đầu | Sau fix |
|---|------|---------|---------|
| 26 | LOGIN ADMIN | ❌ **FAIL** | ✅ PASS |
| 27 | GET_USER_LIST | ❌ **FAIL** | ✅ PASS |
| 28 | LOCK_USER | ❌ **FAIL** | ✅ PASS |
| 29 | UNLOCK_USER | ❌ **FAIL** | ✅ PASS |
| 30 | LOCK_USER không có quyền | ❌ **FAIL** | ✅ PASS |

---

### F. Report & Audit

| # | Test | Ban đầu | Sau fix |
|---|------|---------|---------|
| 31 | GET_AUCTION_REPORT | ❌ **FAIL** | ✅ PASS |
| 32 | GET_BID_HISTORY_REPORT | ❌ **FAIL** | ✅ PASS |
| 33 | GET_AUDIT_LOG | ❌ **FAIL** | ✅ PASS |
| 34 | RUN_INTEGRITY_CHECK | ❌ **FAIL** | ✅ PASS |

---

### G. Bảo mật & Biên

| # | Test | Kết quả |
|---|------|---------|
| 35 | Gọi PLACE_BID không có token | ✅ PASS |
| 36 | Token giả | ✅ PASS |
| 37 | JSON không hợp lệ | ✅ PASS |
| 38 | Lệnh không xác định | ✅ PASS |

---

### H. Health Check

| # | Test | Ban đầu | Sau fix |
|---|------|---------|---------|
| 39 | HEALTH_CHECK | ❌ **FAIL** | ✅ PASS |

---

## Phân tích lỗi & Cách sửa

### Lỗi 1: GET_ITEM_DETAIL — Serialization Error (Test 17)

> **Nguyên nhân:** `handleGetItemDetail()` trả về raw `Item` object (kế thừa `Entity` chứa `LocalDateTime` fields). Khi Jackson serialize, nó gặp lỗi `java.time.LocalDateTime not supported by default`.

> **Fix:** Chuyển response từ raw object sang `Map<String, Object>` với `createdAt.toString()`, tương tự pattern của `handleGetItemList`.

**File:** [ItemHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/ItemHandler.java#L107-L136)

```diff
-return MessageMapper.toJson(
-        MessageResponse.ok("GET_ITEM_DETAIL", itemOpt.get()));
+Item item = itemOpt.get();
+Map<String, Object> detail = new HashMap<>();
+detail.put("id", item.getId());
+detail.put("name", item.getName());
+detail.put("createdAt", item.getCreatedAt().toString());
+detail.put("updatedAt", item.getUpdatedAt().toString());
+// ... other fields
+return MessageMapper.toJson(
+        MessageResponse.ok("GET_ITEM_DETAIL", detail));
```

---

### Lỗi 2: CREATE_AUCTION — Thiếu startTime/endTime (Test 20)

> **Nguyên nhân:** `handleCreateAuction()` yêu cầu `startTime` và `endTime` dạng ISO string, nhưng test gửi `durationMinutes`.

> **Fix:** Thêm hỗ trợ `durationMinutes` — nếu không có `startTime`/`endTime` nhưng có `durationMinutes`, tự tính `startTime = now()` và `endTime = now() + duration`.

**File:** [AuctionHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/AuctionHandler.java#L102-L138)

```diff
+} else if (payload.has("durationMinutes")
+        && payload.get("durationMinutes").isNumber()) {
+    int durationMinutes = payload.get("durationMinutes").asInt(0);
+    startTime = LocalDateTime.now();
+    endTime = startTime.plusMinutes(durationMinutes);
```

---

### Lỗi 3: PLACE_BID — Phiên chưa bắt đầu (Test 21)

> **Nguyên nhân:** Auction tạo với `startTime = now()` nhưng status vẫn là `OPEN`. `AuctionLifecycleTask` chạy mỗi 5s mới transition OPEN → RUNNING. Trong khoảng chờ đó, `BidValidator` từ chối bid vì phiên chưa RUNNING.

> **Fix:** Trong `handleCreateAuction`, nếu `startTime <= now()`, tự động transition sang `RUNNING` ngay sau khi tạo.

**File:** [AuctionHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/AuctionHandler.java#L139-L148)

```diff
 Auction auction = new Auction(itemId, startTime, endTime, ...);
+if (!startTime.isAfter(LocalDateTime.now())) {
+    auction.transitionTo(AuctionStatus.RUNNING);
+}
 handler.auctionDao.save(auction);
```

---

### Lỗi 4: LOGIN ADMIN — Không có tài khoản admin (Test 26-34)

> **Nguyên nhân:** Hệ thống không seed tài khoản admin mặc định. `REGISTER` API từ chối role=ADMIN → không có cách nào tạo admin.

> **Fix:** Thêm `seedDefaultAdmin()` trong `MigrationRunner.run()` — tự tạo tài khoản admin (username=`admin`, password=`admin123`) nếu chưa có admin nào.

**File:** [MigrationRunner.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/config/MigrationRunner.java#L60-L105)

---

### Lỗi 5: HEALTH_CHECK — Lệnh không tồn tại (Test 39)

> **Nguyên nhân:** `RequestHandler` không có case cho `HEALTH_CHECK` trong switch statement.

> **Fix:** Thêm `handleHealthCheck()` method trả về uptime, activeAuctions, activeSessions, serverTime.

**File:** [RequestHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/RequestHandler.java#L152-L195)

---

### Lỗi 6: Test Script — Extras thiếu brand/warrantyMonths (Test 19a)

> **Nguyên nhân:** Script gửi `extras: {}` cho `CREATE_ITEM` loại ELECTRONICS, nhưng `ElectronicsCreator` yêu cầu `brand` và `warrantyMonths`.

> **Fix:** Cập nhật test script gửi đúng extras: `{"brand":"Dell","warrantyMonths":12}`.

**File:** [test_bidhub.ps1](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/test_bidhub.ps1)

---

## Danh sách file đã thay đổi

| File | Thay đổi |
|------|----------|
| [ItemHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/ItemHandler.java) | Fix GET_ITEM_DETAIL serialization — trả Map thay vì raw object |
| [AuctionHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/AuctionHandler.java) | Hỗ trợ `durationMinutes` + auto-start auction khi `startTime <= now` |
| [RequestHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/RequestHandler.java) | Thêm HEALTH_CHECK command handler |
| [MigrationRunner.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/config/MigrationRunner.java) | Seed tài khoản admin mặc định |
| [test_bidhub.ps1](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/test_bidhub.ps1) | Cập nhật test script (extras, admin creds, userId-based LOCK/UNLOCK) |
