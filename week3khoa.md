Dưới đây là bài giảng tổng hợp kiến thức cho nhiệm vụ của Khoa trong Tuần 3, được soạn theo đúng cấu trúc đã thống nhất, có code đầy đủ, giải thích chi tiết, sơ đồ luồng và cheat sheet.

# 📘 Bài giảng: Nhiệm vụ Khoa – AuctionDao & BidDao (Tuần 3 BidHub)

## 🎯 Mục tiêu tổng quát
Sau tuần này, Khoa sẽ hoàn thiện **lớp dữ liệu cho hai bảng cốt lõi của hệ thống đấu giá**:

- `AuctionDao` quản lý bảng `auctions` – lưu thông tin phiên đấu giá, cập nhật trạng thái, giá cao nhất, thời gian kết thúc một cách chính xác và an toàn.
- `BidDao` quản lý bảng `bid_transactions` – ghi nhận từng lượt đặt giá, tra cứu lịch sử và tìm bid cao nhất.

Toàn bộ logic Bidding Engine ở Tuần 6 và Anti‑Sniping ở Tuần 8 đều dựa trên hai DAO này. Làm tốt tuần 3 nghĩa là xây xong **móng cho toà nhà đấu giá**.

---

## 🗺 Lộ trình bài giảng
1. Vai trò của `AuctionDao` và `BidDao` trong kiến trúc.
2. Chuẩn bị – Thêm constructor DB‑load vào `Auction` và `BidTransaction`.
3. `AuctionDao` chi tiết: các phương thức và **tại sao tách riêng 3 loại UPDATE**.
4. `BidDao` chi tiết: ghi nhận giao dịch, lịch sử sắp xếp, bid cao nhất.
5. `AuctionDaoTest` – những test case quan trọng, cách tránh flaky.
6. Liên kết với Bidding Engine và các Service tương lai.
7. Những lỗi thường gặp & cách phòng tránh.
8. Cheat sheet tổng kết.

---

## 1. Vai trò của `AuctionDao` và `BidDao`

**AuctionDao** – là “cánh cổng” duy nhất truy xuất bảng `auctions`. Mỗi khi có phiên mới, cần chuyển trạng thái `OPEN → RUNNING`, hoặc có người đặt giá cao hơn, thì **chỉ AuctionDao** được phép thực hiện.

**BidDao** – là “sổ cái” ghi lại từng đồng tiền đặt vào hệ thống. Nhờ có nó, ta biết được lịch sử đấu giá của một phiên và quan trọng nhất là **ai là người thắng**.

Cả hai cùng nằm trong **tầng Data Access** của mô hình MVC, phục vụ cho các Service ở tầng trên.

---

## 2. Chuẩn bị – Constructor DB‑load

Trước khi viết DAO, Khoa cần **thêm constructor mới** vào các model đã có từ Tuần 2, để khi đọc dữ liệu từ DB, ta tạo lại object với **id và timestamp gốc** (không tự sinh mới).

### 2.1. Constructor cho `Auction`

File `Auction.java` đang có constructor tạo mới (tự sinh UUID, thời gian hiện tại). Cần thêm:

```java
/**
 * Constructor load từ DB – dùng bởi AuctionDao.mapRow()
 */
public Auction(
    String id, LocalDateTime createdAt, LocalDateTime updatedAt,
    String itemId, LocalDateTime startTime, LocalDateTime endTime,
    double startingPrice, double currentHighestBid, String highestBidderId,
    AuctionStatus status, double minimumIncrement) {
  super(id, createdAt, updatedAt);
  this.itemId = itemId;
  this.startTime = startTime;
  this.endTime = endTime;
  this.startingPrice = startingPrice;
  this.currentHighestBid = currentHighestBid;
  this.highestBidderId = highestBidderId; // có thể null
  this.status = status;
  this.minimumIncrement = minimumIncrement;
}
Lưu ý: highestBidderId có thể là null vì khi mới tạo phiên chưa có ai đặt giá. Constructor này chấp nhận null.

2.2. Constructor cho BidTransaction
Bảng bid_transactions chỉ có id, auction_id, bidder_id, bid_amount, bid_time. Không có cột created_at/updated_at riêng. Ta sẽ dùng bidTime làm cả hai timestamp của lớp cha Entity.

/**
 * Constructor load từ DB – dùng bởi BidDao.mapRow()
 */
public BidTransaction(
    String id, String auctionId, String bidderId,
    double bidAmount, LocalDateTime bidTime) {
  super(id, bidTime, bidTime); // dùng bidTime làm createdAt và updatedAt
  this.auctionId = auctionId;
  this.bidderId = bidderId;
  this.bidAmount = bidAmount;
  this.bidTime = bidTime;
}
Nhờ constructor này, BidDao.mapRow() có thể tạo lại đối tượng hoàn chỉnh từ dữ liệu DB mà vẫn giữ đúng contract của Entity.

3. AuctionDao chi tiết – Nghệ thuật tách UPDATE
3.1. Tổng quan các phương thức
AuctionDao cung cấp:

Phương thức	Mục đích
save(Auction)	Thêm phiên mới
findById(String)	Lấy một phiên theo id
findActiveAuctions()	Lấy tất cả phiên đang ở trạng thái RUNNING
updateStatus(id, status)	Chuyển trạng thái phiên
updateHighestBid(id, amount, bidderId)	Ghi nhận giá cao mới và người đặt giá
updateEndTime(id, newEndTime)	Gia hạn thời gian kết thúc (Anti‑Sniping)
3.2. Tại sao không dùng một câu UPDATE chung?
Nếu gộp tất cả vào một câu UPDATE auctions SET ... WHERE id = ?, thì mỗi lần muốn thay đổi một trường (ví dụ chỉ muốn chuyển trạng thái), ta phải truyền lại toàn bộ giá trị của các trường khác. Điều này dẫn đến:

Nguy cơ race condition: Khi hai thread cùng cập nhật những trường khác nhau trên cùng một dòng, thằng sau có thể ghi đè thằng trước bằng dữ liệu cũ.
Code rối rắm: Không thể hiện rõ ý định nghiệp vụ.
Khó kiểm tra: Không biết chính xác cột nào bị thay đổi.
Tách riêng ba phương thức updateStatus, updateHighestBid, updateEndTime giúp mỗi câu UPDATE chỉ đụng đến những cột cần thiết, tăng an toàn trong môi trường đa luồng và thể hiện rõ ràng từng nghiệp vụ.

3.3. Phân tích code từng phương thức
save(Auction)
public void save(Auction auction) {
    String sql = """
        INSERT INTO auctions (id, item_id, start_time, end_time, starting_price,
            current_highest_bid, highest_bidder_id, status, minimum_increment,
            created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    Connection conn = null;
    try {
        conn = acquireConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId());
            // ... set đủ 11 tham số
            ps.executeUpdate();
        }
    } catch (SQLException e) {
        throw new RuntimeException("AuctionDao.save thất bại: " + e.getMessage(), e);
    } finally {
        releaseConnection(conn);
    }
}
Điểm quan trọng: Câu lệnh INSERT phải cung cấp đủ 11 cột. Nếu sau này thêm cột mới, ta phải sửa cả save và mapRow.

findActiveAuctions()
public List<Auction> findActiveAuctions() {
    String sql = "SELECT * FROM auctions WHERE status = 'RUNNING'";
    // thực hiện query và trả về list
}
Lưu ý: Dùng dấu 'RUNNING' cứng (hardcode) vì đây là hằng số enum được lưu dưới dạng text. Trong thực tế, có thể dùng AuctionStatus.RUNNING.name() nhưng khi viết SQL trực tiếp thì cứng vẫn OK.

updateHighestBid(id, amount, bidderId)
public void updateHighestBid(String auctionId, double amount, String bidderId) {
    String sql = """
        UPDATE auctions
        SET current_highest_bid = ?, highest_bidder_id = ?, updated_at = ?
        WHERE id = ?
        """;
    // thực thi
}
Phương thức này là “trái tim” của quá trình đặt giá. Nó chỉ thay đổi ba cột: current_highest_bid, highest_bidder_id, updated_at. Các cột như status, end_time, item_id… không hề bị đụng tới. Nhờ vậy, nếu cùng lúc có một thread khác đang thực hiện updateEndTime, hai thao tác không xung đột về mặt dữ liệu.

updateEndTime(id, newEndTime) và updateStatus(id, status)
Hai phương thức này dùng chung một helper runUpdate() để giảm lặp code.

private void runUpdate(String sql, String p1, String p2, String p3) {
    Connection conn = null;
    try {
        conn = acquireConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p1);
            ps.setString(2, p2);
            ps.setString(3, p3);
            ps.executeUpdate();
        }
    } catch (SQLException e) {
        throw new RuntimeException("...", e);
    } finally {
        releaseConnection(conn);
    }
}
3.4. mapRow() – Xử lý highest_bidder_id null an toàn
private Auction mapRow(ResultSet rs) throws SQLException {
    // ...
    String highestBidderId = rs.getString("highest_bidder_id");
    // nếu cột NULL trong DB, getString trả về null -> hoàn toàn hợp lệ
    // ...
    return new Auction(id, createdAt, updatedAt, itemId,
        startTime, endTime, startingPrice, currentHighestBid,
        highestBidderId, status, minimumIncrement);
}
Không được làm if (highestBidderId == null) highestBidderId = ""; vì như vậy sẽ làm sai dữ liệu (không có ai đặt lại biến thành chuỗi rỗng, dễ gây lỗi logic về sau).

4. BidDao chi tiết – Ghi sổ và tìm người thắng
4.1. Các phương thức
Phương thức	Mục đích
save(BidTransaction)	Ghi nhận một lượt bid mới
findByAuctionId(String)	Lấy lịch sử bid của một phiên, sắp xếp theo thời gian tăng dần
getHighestBid(String)	Trả về một bid có bid_amount cao nhất
4.2. Sự khác biệt giữa ASC và DESC LIMIT 1
findByAuctionId sắp xếp ASC (tăng dần): Mục đích là để hiển thị lịch sử đấu giá theo đúng thứ tự thời gian (bid cũ trước, mới sau). Có thể dùng cho biểu đồ đường (line chart) ở client.
getHighestBid sắp xếp DESC (giảm dần) và LIMIT 1: Mục đích là tìm ai trả giá cao nhất. Nếu chỉ sắp xếp DESC mà không LIMIT 1, vẫn chỉ lấy được một dòng đầu tiên, nhưng LIMIT 1 giúp tối ưu hiệu năng (cơ sở dữ liệu dừng sớm) và thể hiện rõ ràng ý định code.
4.3. Code getHighestBid
public Optional<BidTransaction> getHighestBid(String auctionId) {
    String sql = """
        SELECT * FROM bid_transactions
        WHERE auction_id = ?
        ORDER BY bid_amount DESC
        LIMIT 1
        """;
    // ...
    if (rs.next()) return Optional.of(mapRow(rs));
    return Optional.empty();
}
Nếu không có LIMIT 1, câu SQL vẫn có thể trả về nhiều dòng nhưng ta chỉ đọc dòng đầu, không sai về logic nhưng gây nhập nhằng về mặt ý định. LIMIT 1 là chuẩn mực.

5. AuctionDaoTest – Kiểm thử không thể bỏ qua
5.1. Thiết lập in‑memory
Giống như các DAO khác, Khoa dùng constructor inject Connection để test với SQLite in‑memory.

@BeforeEach
void setup() throws SQLException {
    conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    // tạo bảng auctions và bid_transactions bằng tay
    auctionDao = new AuctionDao(conn);
    bidDao = new BidDao(conn);
}
5.2. Các test case quan trọng
auction_saveAndFind_correctStatus
Lưu một Auction với status = OPEN, rồi findById kiểm tra chính xác status và giá khởi điểm.

findActiveAuctions_onlyReturnsRunning
Tạo hai phiên: một RUNNING, một OPEN. Khẳng định findActiveAuctions() chỉ trả về 1 phiên có trạng thái RUNNING.

updateHighestBid_reflectsNewAmountInDb
Sau khi gọi updateHighestBid, load lại phiên và kiểm tra currentHighestBid và highestBidderId đã cập nhật. Đây là test bắt lỗi quên commit (executeUpdate).

bidDao_getHighestBid_returnsMaxAmount
Lưu ba bid với số tiền khác nhau, kiểm tra getHighestBid trả về đúng bid có bidAmount lớn nhất → xác nhận ORDER BY DESC LIMIT 1 hoạt động.

bidDao_findByAuctionId_sortedByTimeAsc
Tạo ba bid với bidTime cố định không theo thứ tự, rồi gọi findByAuctionId. Kiểm tra danh sách kết quả có thời gian tăng dần. Dùng thời gian cố định (ví dụ LocalDateTime.of(...)) để tránh flaky.

auctionDao_mapRow_nullHighestBidderIdNotCrash
Lưu Auction mới (chưa có bid), load lại và kiểm tra getHighestBidderId() trả về null, không ném NullPointerException.

5.3. Tránh test flaky
Không dùng LocalDateTime.now() trong dữ liệu test vì thời gian chạy có thể thay đổi. Hãy dùng thời gian cố định.
Đảm bảo sau mỗi test, dữ liệu được dọn dẹp (in‑memory tự hủy khi close connection).
6. Liên kết trực tiếp với Bidding Engine (Tuần 6) và Anti‑Sniping (Tuần 8)
Các phương thức Đăng viết hôm nay sẽ trở thành xương sống cho công việc của Khoa ở những tuần sau:

BidService (Tuần 6): khi nhận request đặt giá, nó sẽ gọi:
AuctionDao.findById(id) để kiểm tra phiên còn RUNNING không.
Kiểm tra bidAmount > currentHighestBid + minimumIncrement.
Gọi AuctionDao.updateHighestBid(id, newBid, bidderId).
Gọi BidDao.save(new BidTransaction(...)).
AuctionScheduler (Tuần 6‑7): quét các phiên hết hạn, gọi AuctionDao.updateStatus(id, FINISHED).
Anti‑Sniping (Tuần 8): khi có bid trong những giây cuối, gọi AuctionDao.updateEndTime(id, newEndTime) để gia hạn.
Vì vậy, nếu hôm nay Khoa viết DAO đúng, thì tương lai chỉ cần ráp service vào là chạy.

7. Những lỗi thường gặp & Cách phòng tránh
Quên executeUpdate() hoặc không commit (SQLite auto‑commit sẵn, nhưng phải chắc chắn là đã gọi execute).

Test ngay: updateHighestBid rồi findById kiểm tra.
Nhầm lẫn giữa ASC và DESC trong getHighestBid.

Nếu dùng ASC, bạn sẽ lấy bid thấp nhất. Hãy nhớ: cao nhất = DESC LIMIT 1.
Dùng rs.getString("highest_bidder_id") rồi gọi phương thức trên nó mà không kiểm tra null.

Chỉ cần giữ nguyên null, không nên chuyển thành "".
Constructor DB‑load không được thêm vào model, dẫn đến lỗi biên dịch trong mapRow().

Luôn kiểm tra model có đủ constructor trước khi viết DAO.
Không sắp xếp trong findByAuctionId → dữ liệu trả về không theo trật tự thời gian, client hiển thị sai.

Test flaky do dùng LocalDateTime.now().

Sử dụng thời gian cố định và constructor 5 tham số của BidTransaction để kiểm soát bidTime.
8. Cheat Sheet tổng kết
Thành phần	Mục đích	Ghi nhớ
AuctionDao.save()	Thêm phiên mới	11 tham số, INSERT.
AuctionDao.updateStatus()	Chuyển trạng thái	Chỉ status và updated_at.
AuctionDao.updateHighestBid()	Ghi nhận giá mới	Cập nhật current_highest_bid, highest_bidder_id.
AuctionDao.updateEndTime()	Gia hạn kết thúc	Dùng khi anti‑sniping.
AuctionDao.findActiveAuctions()	Lấy phiên đang chạy	WHERE status = 'RUNNING'.
BidDao.save()	Lưu một bid	5 tham số, dùng bidTime làm timestamp.
BidDao.findByAuctionId()	Lịch sử bid	ORDER BY bid_time ASC.
BidDao.getHighestBid()	Tìm bid cao nhất	ORDER BY bid_amount DESC LIMIT 1.
Constructor DB‑load	Tái tạo object từ DB	Bắt buộc có trong Auction và BidTransaction.
Test với in‑memory	Không phụ thuộc file	Inject Connection qua constructor.
Nguyên tắc vàng:
Mỗi lần cập nhật một trường, hãy dùng một câu UPDATE chỉ chạm đến trường đó. Đừng bao giờ gộp chung các thay đổi không liên quan. Điều này giữ dữ liệu nhất quán, tránh race condition và làm code sáng rõ nghiệp vụ.

9. Sơ đồ luồng hoạt động (mermaid)
Yes
No
Bidder gửi bid
BidService
AuctionDao.findById
Auction hợp lệ?
updateHighestBid
Trả lỗi
BidDao.save
Trả về thành công
flowchart LR
    Khi phiên kết thúc --> AuctionScheduler
    AuctionScheduler --> AuctionDao.updateStatus(FINISHED)
    AuctionDao --> BidDao.getHighestBid
    BidDao --> Xác định người thắng
🚀 Kết luận
Nhiệm vụ của Khoa trong tuần này không chỉ đơn thuần là viết DAO, mà là đặt viên gạch nền cho toàn bộ logic đấu giá. AuctionDao và BidDao nếu viết đúng, rành mạch thì những tuần sau ráp Bidding Engine sẽ trơn tru. Nếu sai, cả hệ thống sẽ trục trặc ở khâu cốt lõi nhất.

Hãy cầm tay chỉ việc từng dòng code, test kỹ từng trường hợp, và nhất là giữ kỷ luật tách UPDATE – đó chính là dấu ấn của một lập trình viên hiểu sâu về dữ liệu và đa luồng.

Chúc Khoa và cả nhóm hoàn thành Tuần 3 thật vững chắc! 💪

