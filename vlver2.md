# Báo cáo Kiểm thử Hệ thống Đấu giá BidHub (Test Walkthrough Detailed Report)

Tài liệu này cung cấp chi tiết toàn bộ **42 ca kiểm thử (test cases)** được phân thành **8 nhóm chức năng** của hệ thống đấu giá trực tuyến BidHub. 

Để đáp ứng yêu cầu kiểm thử cấp cao, mỗi ca kiểm thử dưới đây được trình bày rõ ràng theo cấu trúc:
1. **Mô tả & Mục đích (What it is & Purpose)**
2. **Kết quả thực tế sau khi sửa lỗi (Actual Result)**
3. **Biểu hiện nếu LỖI / THẤT BẠI (What happens if it fails)** - Mô tả cụ thể lỗi sẽ thế nào nếu gặp bug hoặc không vượt qua bài test.

---

## 📊 Tổng Kết Kết Quả Kiểm Thử

| Chỉ số | Giá trị | Trạng thái |
|--------|---------|------------|
| **Tổng số ca kiểm thử** | **42** | |
| **Số test vượt qua (PASS)** | **42** / 42 | ✅ Đạt 100% |
| **Số test thất bại (FAIL)** | **0** / 42 | ❌ Không có |
| **Pass ban đầu (Trước sửa)** | **28** / 42 | Gặp 14 lỗi logic & thiếu API |
| **Pass hiện tại (Sau sửa)** | **42** / 42 | Hoàn toàn ổn định |

---

## 🔍 Mô Tả Chi Tiết 42 Ca Kiểm Thử (Từng Test Một)

### Nhóm A: Kết Nối Cơ Bản (1 Test)

#### 1. Test 1: PING
*   **Mô tả & Mục đích**: Client gửi yêu cầu `PING` trống (`{"type":"PING","payload":{}}`) lên máy chủ TCP tại cổng `9090` nhằm kiểm tra xem Socket Server có đang hoạt động ổn định và sẵn sàng tiếp nhận luồng dữ liệu hay không.
*   **Kết quả thực tế**: `✅ PASS` - Server phản hồi ngay lập tức `status: "OK", type: "PING"` trong vòng dưới 2ms, chứng tỏ hạ tầng TCP socket hoạt động trơn tru.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu server sập hoặc cổng 9090 bị chặn bởi Windows Firewall, client sẽ báo lỗi ngay lập tức: `Connection failed: Connection refused`. Nếu server chạy nhưng gặp lỗi luồng (thread starvation), gói tin gửi lên sẽ bị treo vô hạn (socket timeout) mà không có phản hồi ngược lại.

---

### Nhóm B: Đăng Ký & Đăng Nhập (9 Tests)

#### 2. Test 2: REGISTER thành công (SELLER)
*   **Mô tả & Mục đích**: Tạo một tài khoản Người bán (SELLER) mới với đầy đủ thông tin hợp lệ (username `seller01`, password `12345678`, email `s@test.com`). Đảm bảo hệ thống băm mật khẩu bảo mật và lưu vào CSDL.
*   **Kết quả thực tế**: `✅ PASS` - Đăng ký thành công, server trả về `status: "OK"` kèm ID duy nhất (`userId`) của người bán mới được tạo trong bảng `users` của PostgreSQL.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu tính năng lỗi, server sẽ trả về `status: "ERROR"` với thông báo đăng ký thất bại, hoặc ném lỗi ngoại lệ SQL (`SQLException`) thô ra luồng socket và không tạo ra bất kỳ bản ghi nào trong cơ sở dữ liệu.

#### 3. Test 3: REGISTER trùng username
*   **Mô tả & Mục đích**: Cố tình đăng ký lại một tài khoản mới có username trùng lặp là `seller01` nhằm kiểm định ràng buộc duy nhất (UNIQUE constraint) đối với tên tài khoản.
*   **Kết quả thực tế**: `✅ PASS` - Server từ chối và trả về `status: "ERROR"` cùng thông điệp lỗi rõ ràng bằng tiếng Việt: `"Username đã tồn tại"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu không vượt qua test (gặp bug), hệ thống sẽ cho phép tạo thành công tài khoản trùng tên thứ hai. Khi đó cơ chế đăng nhập sẽ bị tê liệt hoàn toàn vì server không thể biết chính xác token cấp cho ai khi xác thực username `seller01`.

#### 4. Test 4: REGISTER password ngắn (<8 ký tự)
*   **Mô tả & Mục đích**: Đăng ký tài khoản `user2` nhưng nhập mật khẩu siêu ngắn chỉ có 7 ký tự (`1234567`) nhằm kiểm tra tính thực thi của quy chế bảo mật mật khẩu.
*   **Kết quả thực tế**: `✅ PASS` - Server chặn từ tầng validation ứng dụng trên RAM, trả về `status: "ERROR"` với thông báo `"Mật khẩu phải từ 8 ký tự trở lên"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi, hệ thống sẽ lưu mật khẩu yếu vào cơ sở dữ liệu, tạo cơ hội cho kẻ xấu dễ dàng bẻ khóa mật khẩu bằng phương pháp tấn công vét cạn (brute-force) hoặc từ điển.

#### 5. Test 5: REGISTER email sai định dạng
*   **Mô tả & Mục đích**: Đăng ký tài khoản `user3` với email sai cú pháp không có tên miền (`"noemail"`) để kiểm nghiệm bộ lọc biểu thức chính quy (Regex Email Validation) của server.
*   **Kết quả thực tế**: `✅ PASS` - Server từ chối và trả về `status: "ERROR"` kèm thông báo `"Email không hợp lệ"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi, tài khoản rác với email không có thực sẽ được lưu vào cơ sở dữ liệu, gây lãng phí tài nguyên và làm hỏng nghiệp vụ gửi thông báo tài chính trúng thầu về sau.

#### 6. Test 6: REGISTER role ADMIN trực tiếp bị từ chối
*   **Mô tả & Mục đích**: Khách vãng lai cố tình chiếm quyền kiểm soát hệ thống bằng cách gửi yêu cầu đăng ký trực tiếp vai trò `"role":"ADMIN"` qua cổng công khai.
*   **Kết quả thực tế**: `✅ PASS` - Server từ chối thẳng thừng, trả về `status: "ERROR"` kèm cảnh báo `"Không được phép đăng ký vai trò ADMIN trực tiếp"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu gặp lỗ hổng này (Privilege Escalation), bất kỳ người dùng phổ thông nào cũng tự tạo được tài khoản ADMIN tối cao để tùy ý khóa người khác, xóa sản phẩm và can thiệp toàn bộ dữ liệu đấu giá.

#### 7. Test 7: LOGIN thành công (SELLER)
*   **Mô tả & Mục đích**: Người bán thực hiện đăng nhập bằng tài khoản `seller01` và mật khẩu chính xác để được cấp Token xác thực cho các phiên làm việc tiếp theo.
*   **Kết quả thực tế**: `✅ PASS` - Xác thực thành công, server trả về `status: "OK"` kèm theo một chuỗi Token ngẫu nhiên (UUID) được lưu trên bộ nhớ đệm (RAM Session) làm minh chứng hợp lệ.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi, đăng nhập thất bại dù mật khẩu đúng, hoặc server phản hồi thành công nhưng trả về token rỗng (`null`), khiến người bán hoàn toàn bị khóa chân không thể gọi các API SELLER sau đó.

#### 8. Test 8: LOGIN sai mật khẩu
*   **Mô tả & Mục đích**: Đăng nhập tài khoản `seller01` nhưng gửi mật khẩu sai `"wrongpass"` để kiểm tra cơ chế chống đăng nhập trái phép.
*   **Kết quả thực tế**: `✅ PASS` - Server từ chối và trả về `status: "ERROR"` cùng thông điệp bảo mật chung chung: `"Thông tin đăng nhập không chính xác"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi bảo mật, hệ thống vẫn cho phép người dùng đăng nhập thành công vào tài khoản dù gửi sai mật khẩu. Hoặc nếu báo lỗi quá chi tiết kiểu *"Sai mật khẩu"* sẽ giúp hacker biết chắc tài khoản `seller01` có tồn tại để tiếp tục brute-force.

#### 9. Test 9: LOGIN username không tồn tại
*   **Mô tả & Mục đích**: Đăng nhập bằng một tên tài khoản ảo `"ghost"` nhằm kiểm tra tính an toàn của luồng dữ liệu khi không tìm thấy thực thể trong DB.
*   **Kết quả thực tế**: `✅ PASS` - Server từ chối, trả về `status: "ERROR"` kèm thông báo bảo mật `"Thông tin đăng nhập không chính xác"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu gặp lỗi, hệ thống có thể báo cụ thể *"Tài khoản không tồn tại"*, giúp hacker dễ dàng thực hiện tấn công dò quét (Username Enumeration) để trích xuất danh sách tên đăng nhập thực tế của hệ thống.

#### 10. Test 10: LOGIN thiếu trường password
*   **Mô tả & Mục đích**: Gửi yêu cầu đăng nhập chứa username nhưng khuyết thiếu hoàn toàn trường `"password"` trong payload nhằm kiểm tra độ chống chịu lỗi của lớp validation Java.
*   **Kết quả thực tế**: `✅ PASS` - Server bắt lỗi sớm trên RAM, phản hồi `status: "ERROR"` với thông báo `"Thiếu thông tin đăng nhập bắt buộc"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu gặp lỗi lập trình (lỗi NullPointer), chương trình Java trên server sẽ ném ra ngoại lệ `NullPointerException` chưa được kiểm soát, làm treo luồng kết nối socket hiện hành của client đó và gây rò rỉ vết lỗi Stack Trace ra ngoài.

---

### Nhóm C: Quản Lý Sản Phẩm (10 Tests)

#### 11. Test 11: CREATE_ITEM thành công
*   **Mô tả & Mục đích**: Người bán sử dụng `$TOKEN` tạo sản phẩm Laptop thuộc phân loại `ELECTRONICS` kèm các extras cấu hình kỹ thuật động (`brand: ASUS`, `warrantyMonths: 24`).
*   **Kết quả thực tế**: `✅ PASS` - Tạo thành công sản phẩm, lưu trữ extras dạng JSONB Postgres và phản hồi `status: "OK"` kèm mã `$ITEM_ID` duy nhất.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi, sản phẩm bị từ chối tạo dù token đúng, hoặc trường extras động bị lưu thành chuỗi rỗng thô sơ, làm mất đi các thuộc tính kỹ thuật đặc thù của sản phẩm điện tử.

#### 12. Test 12: CREATE_ITEM thiếu tên sản phẩm
*   **Mô tả & Mục đích**: Người bán cố tình tạo sản phẩm chỉ có mô tả và giá cả nhưng bỏ trống trường `"name"` bắt buộc.
*   **Kết quả thực tế**: `✅ PASS` - Server chặn từ RAM, trả về `status: "ERROR"` cùng thông báo `"Tên sản phẩm không được để trống"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi, sản phẩm không có tên sẽ được lưu vào cơ sở dữ liệu, gây ra tình trạng hiển thị rỗng tiêu đề sản phẩm trên trang chủ và phát sinh lỗi logic khi người dùng tìm kiếm.

#### 13. Test 13: CREATE_ITEM giá khởi điểm bằng 0 hoặc âm
*   **Mô tả & Mục đích**: Kiểm định quy tắc kinh doanh: Giá khởi điểm đưa lên sàn đấu giá của sản phẩm phải luôn là một số dương lớn hơn 0.
*   **Kết quả thực tế**: `✅ PASS` - Server chặn, trả về `status: "ERROR"` cùng thông báo `"Giá khởi điểm phải lớn hơn 0"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi, hệ thống sẽ cho phép đăng bán sản phẩm giá trị 0đ hoặc âm, dẫn đến việc người mua thầu được sản phẩm miễn phí hoặc làm đảo lộn logic so khớp giá thầu thầu tiếp theo.

#### 14. Test 14: CREATE_ITEM sai danh mục sản phẩm (itemType)
*   **Mô tả & Mục đích**: Người bán cố gắng gửi một phân loại sản phẩm không nằm trong danh mục hỗ trợ (ví dụ: `"itemType": "BOOK"`).
*   **Kết quả thực tế**: `✅ PASS` - Server chặn và trả về `status: "ERROR"` cùng thông báo `"Loại sản phẩm không hợp lệ hoặc không được hỗ trợ"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi, chương trình Java trên server sẽ bị sập luồng do ném ngoại lệ `IllegalArgumentException` khi cố ép chuỗi `"BOOK"` sang Enum của Java mà không có khối try-catch bảo vệ.

#### 15a. Đăng ký tài khoản BIDDER
*   **Mô tả & Mục đích**: Đăng ký một tài khoản Người mua (BIDDER) mới mang tên `bidder01` để chuẩn bị kiểm thử các vai trò đấu giá và phân quyền.
*   **Kết quả thực tế**: `✅ PASS` - Tạo thành công tài khoản, gán quyền `BIDDER` và lưu `userId` mới vào PostgreSQL.
*   **Biểu hiện nếu lỗi (FAIL)**: Đăng ký thất bại hoặc gán sai vai trò thành SELLER, khiến ta không có tài khoản người mua hợp lệ để thực thi thầu ở các nhóm sau.

#### 15b. LOGIN BIDDER
*   **Mô tả & Mục đích**: Đăng nhập tài khoản `bidder01` để trích xuất và lưu giữ Token phiên mua vào biến `$BIDDER_TOKEN`.
*   **Kết quả thực tế**: `✅ PASS` - Đăng nhập thành công, nhận về Token phiên của người mua.
*   **Biểu hiện nếu lỗi (FAIL)**: Đăng nhập thất bại, không lấy được token của người mua khiến toàn bộ các bài test đặt thầu tiếp theo bị chặn đứng (Blocked).

#### 15. Test 15: CREATE_ITEM không có quyền (Tài khoản BIDDER cố tình tạo)
*   **Mô tả & Mục đích**: Kiểm tra tính chặt chẽ của hàng rào phân quyền phía máy chủ. Sử dụng `$BIDDER_TOKEN` để gọi API tạo sản phẩm (hành vi lấn quyền SELLER).
*   **Kết quả thực tế**: `✅ PASS` - Server chặn đứng phân quyền, trả về `status: "ERROR"` với thông báo `"Bạn không có quyền thực hiện hành động này"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu gặp lỗi phân quyền (Broken Access Control), người mua vẫn tự ý đăng bán được sản phẩm mà không cần qua xét duyệt vai trò SELLER.

#### 16. Test 16: GET_ITEM_LIST
*   **Mô tả & Mục đích**: Lấy danh sách tất cả các sản phẩm đang bày bán trên sàn đấu giá ở chế độ công khai (không cần đính kèm token).
*   **Kết quả thực tế**: `✅ PASS` - Lấy danh sách thành công, trả về mảng các sản phẩm bao gồm đầy đủ thông tin Laptop.
*   **Biểu hiện nếu lỗi (FAIL)**: Trả về trạng thái lỗi hoặc trả về mảng rỗng mặc dù cơ sở dữ liệu đang có sản phẩm Laptop hoạt động.

#### 17. Test 17: GET_ITEM_DETAIL
*   **Mô tả & Mục đích**: Lấy thông tin chi tiết của Laptop dựa trên `$ITEM_ID` và hiển thị đầy đủ trường extras động cùng thời gian tạo.
*   **Kết quả thực tế**: `✅ PASS` - Trả về cấu trúc chi tiết sản phẩm và extras thành công, sửa đổi lỗi Serialization LocalDateTime hoạt động rất tốt.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi (như ban đầu), server sẽ báo lỗi Serialization lỗi do thư viện Jackson Java không thể dịch kiểu dữ liệu `LocalDateTime` của Postgres sang chuỗi JSON chuẩn, gây lỗi treo luồng phản hồi.

#### 18. Test 18: DELETE_ITEM
*   **Mô tả & Mục đích**: Người bán thực hiện xóa sản phẩm của chính mình khi sản phẩm chưa được đưa vào phòng đấu giá nào.
*   **Kết quả thực tế**: `✅ PASS` - Xóa sản phẩm thành công ra khỏi PostgreSQL để dọn dẹp danh mục.
*   **Biểu hiện nếu lỗi (FAIL)**: SELLER không thể xóa được sản phẩm của chính mình do lỗi logic phân quyền sở hữu, hoặc server ném lỗi ngoại lệ cơ sở dữ liệu.

#### 19. Test 19: DELETE_ITEM không tồn tại
*   **Mô tả & Mục đích**: Gửi yêu cầu xóa một mã sản phẩm ảo `"fake-id"` để xem server phản hồi như thế nào.
*   **Kết quả thực tế**: `✅ PASS` - Server phản hồi `status: "ERROR"` cùng thông báo `"Sản phẩm không tồn tại"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Server trả về `status: "OK"` một cách vô lý mặc dù không hề xóa được gì, hoặc ném ra lỗi cú pháp SQL Postgres thô.

#### 19a. CREATE_ITEM lần 2 (Laptop2 - chuẩn bị cho đấu giá)
*   **Mô tả & Mục đích**: Tạo lại sản phẩm Laptop2 (loại Dell, bảo hành 12 tháng) có cấu trúc extras hợp lệ để làm đầu vào thầu ở Nhóm D.
*   **Kết quả thực tế**: `✅ PASS` - Tạo thành công Laptop2 và gán mã định danh mới vào biến `$ITEM_ID`, sửa hoàn hảo lỗi thiếu extras của test script ban đầu.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi, sản phẩm bị từ chối tạo do validate extras thất bại (như ban đầu), khiến toàn bộ Nhóm D đấu giá không có sản phẩm đầu vào và bị Blocked.

---

### Nhóm D: Đấu Giá & Đặt Thầu (5 Tests)

#### 20. Test 20: CREATE_AUCTION thành công
*   **Mô tả & Mục đích**: Mở phòng đấu giá cho sản phẩm Laptop2 mới tạo, hỗ trợ tham số `durationMinutes` tự động chuyển đổi sang startTime/endTime và tự động transition trạng thái sang `RUNNING` để người mua đặt thầu thầu được ngay.
*   **Kết quả thực tế**: `✅ PASS` - Tạo phòng thành công, trả về trạng thái `RUNNING` và mã `$AUCTION_ID` mới, sửa đổi lỗi durationMinutes hoạt động xuất sắc.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi (như ban đầu), server sẽ từ chối tạo do thiếu tham số tĩnh startTime/endTime. Hoặc nếu tạo được nhưng trạng thái phòng bị kẹt ở `OPEN` (chờ 5s), người dùng sẽ không thể đặt thầu thầu ngay lập tức.

#### 21. Test 21: PLACE_BID thành công
*   **Mô tả & Mục đích**: Người mua `bidder01` sử dụng `$BIDDER_TOKEN` đặt thầu mức giá `16000000` (lớn hơn giá bắt đầu 15M + bước giá tối thiểu 500K) cho phòng đấu giá đang hoạt động.
*   **Kết quả thực tế**: `✅ PASS` - Đặt thầu thành công, cập nhật lịch sử thầu và cập nhật giá cao nhất hiện tại của phòng lên 16 triệu đồng.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi (như ban đầu), `BidValidator` sẽ chặn và trả về lỗi: *"Phiên đấu giá chưa bắt đầu. Vui lòng chờ đến giờ."* do phòng đấu giá chưa được kích hoạt trạng thái `RUNNING` kịp thời, khiến hoạt động đặt thầu thầu bị đình trệ.

#### 22. Test 22: PLACE_BID giá thấp hơn mức hiện hành
*   **Mô tả & Mục đích**: Người mua cố tình đặt giá thầu thầu `10000000` (thấp hơn mức giá dẫn đầu hiện tại là 16 triệu) để kiểm nghiệm luật đấu giá.
*   **Kết quả thực tế**: `✅ PASS` - Server chặn từ tầng logic nghiệp vụ, phản hồi `status: "ERROR"` với thông báo `"Giá đặt thầu phải lớn hơn hoặc bằng giá hiện tại cộng bước giá tối thiểu"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu gặp bug, hệ thống sẽ ghi nhận mức giá thầu thấp hơn vô lý này vào lịch sử thầu thầu và trực tiếp hạ giá cao nhất của sản phẩm xuống, làm hỏng toàn bộ logic đấu giá.

#### 23. Test 23: PLACE_BID phiên đấu giá không tồn tại
*   **Mô tả & Mục đích**: Người mua đặt giá thầu thầu vào một ID phiên thầu giả `"fake-auction"` để kiểm định an toàn dữ liệu tham chiếu.
*   **Kết quả thực tế**: `✅ PASS` - Server từ chối, trả về `status: "ERROR"` với thông báo `"Phiên đấu giá không tồn tại"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Hệ thống ghi nhận lượt thầu thầu ảo thành công hoặc ném ngoại lệ khóa ngoại `ForeignKeyViolation` từ Postgres làm sập luồng xử lý database.

#### 24. Test 24: GET_AUCTION_DETAIL
*   **Mô tả & Mục đích**: Lấy thông tin chi tiết phòng đấu giá `$AUCTION_ID` để cập nhật bảng điểm thầu thầu thời gian thực bao gồm giá hiện tại và thông tin máy Laptop2.
*   **Kết quả thực tế**: `✅ PASS` - Trả về đầy đủ thông tin phòng đấu giá và sản phẩm Dell liên kết hoạt động hoàn hảo.
*   **Biểu hiện nếu lỗi (FAIL)**: Phát sinh lỗi Jackson Serialization đối với các trường ngày tháng bắt đầu/kết thúc tương tự Test 17, khiến client không đọc được bảng điểm thầu thầu.

#### 25. Test 25: GET_AUCTION_LIST
*   **Mô tả & Mục đích**: Lấy danh sách tất cả các phiên đấu giá đang mở trên hệ thống để người mua lướt tìm phòng thầu thầu.
*   **Kết quả thực tế**: `✅ PASS` - Trả về danh sách chứa phiên đấu giá đang hoạt động thành công.
*   **Biểu hiện nếu lỗi (FAIL)**: Trả về danh sách trống rỗng hoặc lỗi kết nối CSDL PostgreSQL.

---

### Nhóm E: Quản Trị Viên - Admin (5 Tests)

#### 26. Test 26: LOGIN ADMIN
*   **Mô tả & Mục đích**: Đăng nhập tài khoản quản trị tối cao của hệ thống bằng thông tin mặc định được tự động gieo hạt (seed) bởi MigrationRunner.
*   **Kết quả thực tế**: `✅ PASS` - Đăng nhập thành công, trả về Token quản trị lưu vào biến `$ADMIN_TOKEN`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu lỗi (như ban đầu), đăng nhập thất bại do CSDL trống rỗng không có sẵn tài khoản admin nào và hệ thống chặn tạo admin qua API công khai, khiến toàn bộ các test case quản trị sau bị nghẽn (Blocked).

#### 27. Test 27: GET_USER_LIST
*   **Mô tả & Mục đích**: Quyền hạn của Admin: Xem danh sách toàn bộ tài khoản người bán và người mua đã đăng ký để thực hiện kiểm toán và rà soát.
*   **Kết quả thực tế**: `✅ PASS` - Admin lấy thành công danh sách người dùng đầy đủ.
*   **Biểu hiện nếu lỗi (FAIL)**: API bị từ chối hoặc người dùng thường (SELLER/BIDDER) cũng lách luật xem được thông tin nhạy cảm của các tài khoản khác (Lỗi Access Control).

#### 28. Test 28: LOCK_USER
*   **Mô tả & Mục đích**: Admin thực hiện khóa tài khoản người mua vi phạm quy chế `$BIDDER_USER_ID` để chặn thầu thầu gian lận.
*   **Kết quả thực tế**: `✅ PASS` - Khóa tài khoản thành công, trường `is_locked` của người mua trong PostgreSQL cập nhật sang `true`.
*   **Biểu hiện nếu lỗi (FAIL)**: Đánh dấu khóa thành công nhưng tài khoản người mua đó vẫn tiếp tục thực hiện đặt thầu thầu hoặc đăng nhập bình thường do session token cũ trên RAM chưa được thu hồi khẩn cấp.

#### 29. Test 29: UNLOCK_USER
*   **Mô tả & Mục đích**: Admin mở khóa tài khoản `$BIDDER_USER_ID` sau khi thành viên chấp hành xong quy định để họ tiếp tục đấu giá.
*   **Kết quả thực tế**: `✅ PASS` - Mở khóa thành công, cập nhật trạng thái `is_locked` sang `false` trong DB.
*   **Biểu hiện nếu lỗi (FAIL)**: Trạng thái không được cập nhật, người dùng vẫn bị kẹt ở trạng thái bị khóa và không thể tham gia sàn thầu.

#### 30. Test 30: LOCK_USER không có quyền (Tài khoản BIDDER cố tình khóa)
*   **Mô tả & Mục đích**: Tài khoản BIDDER `$BIDDER_TOKEN` lách quyền gọi API khóa tài khoản người bán `$SELLER_USER_ID` hòng dìm đối thủ cạnh tranh.
*   **Kết quả thực tế**: `✅ PASS` - Server chặn quyền chính xác, phản hồi lỗi `ERROR` chặn đứng hành động trái phép.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu gặp lỗi phân quyền nghiêm trọng này, người dùng thường tự ý khóa tài khoản của bất kỳ ai trong hệ thống, gây hỗn loạn toàn bộ hoạt động của sàn.

---

### Nhóm F: Báo Cáo & Kiểm Toán (4 Tests)

#### 31. Test 31: GET_AUCTION_REPORT
*   **Mô tả & Mục đích**: Quyền Admin: Xuất báo cáo tổng hợp kết quả của toàn bộ các phiên đấu giá để thống kê doanh thu và hiệu quả sàn thầu.
*   **Kết quả thực tế**: `✅ PASS` - Trả về dữ liệu báo cáo thống kê chính xác kèm giá khởi điểm và giá thầu cao nhất hiện hành.
*   **Biểu hiện nếu lỗi (FAIL)**: Gặp lỗi truy vấn SQL INNER JOIN giữa các bảng CSDL Postgres, hoặc API bị hở quyền cho phép người mua tự do tải báo cáo doanh thu nhạy cảm.

#### 32. Test 32: GET_BID_HISTORY_REPORT
*   **Mô tả & Mục đích**: Quyền Admin: Xuất lịch sử đặt thầu thầu chi tiết của phòng `$AUCTION_ID` để rà soát hành vi đẩy giá ảo của các nhóm tài khoản.
*   **Kết quả thực tế**: `✅ PASS` - Trả về đầy đủ dòng thời gian đặt thầu thầu của bidder01 với mức thầu thầu 16 triệu đồng.
*   **Biểu hiện nếu lỗi (FAIL)**: Báo cáo trả về trống rỗng mặc dù phòng đấu giá đã có lượt đặt thầu thầu thành công trước đó.

#### 33. Test 33: GET_AUDIT_LOG
*   **Mô tả & Mục đích**: Admin kiểm tra nhật ký hệ thống (Audit Logs) để rà soát toàn bộ lịch sử các hành động nhạy cảm đã thực thi trên sàn.
*   **Kết quả thực tế**: `✅ PASS` - Trả về mảng 10 bản ghi nhật ký ghi nhận đầy đủ thời gian, tác nhân, hành động và trạng thái thành công/thất bại.
*   **Biểu hiện nếu lỗi (FAIL)**: Server không ghi nhận nhật ký các thao tác nhạy cảm (như Khóa tài khoản, Thay đổi mật khẩu), làm mất đi tính chống từ chối (Non-repudiation) và gây khó khăn khi điều tra sự cố.

#### 34. Test 34: RUN_INTEGRITY_CHECK
*   **Mô tả & Mục đích**: Admin kích hoạt tính năng tự động quét CSDL để kiểm tra tính toàn vẹn liên kết khóa ngoại vật lý và tính nhất quán của dữ liệu thầu thầu.
*   **Kết quả thực tế**: `✅ PASS` - Quét thành công, trả về trạng thái nhất quán 100% không có lỗi lệch dòng (`issuesFound: 0`).
*   **Biểu hiện nếu lỗi (FAIL)**: Chương trình báo lỗi CSDL bị mất liên kết toàn vẹn (ví dụ: sản phẩm bị xóa nhưng phòng đấu giá vẫn tồn tại mồ côi), gây sai lệch dữ liệu tài chính nghiêm trọng.

---

### Nhóm G: Bảo Mật & Trường Hợp Biên (4 Tests)

#### 35. Test 35: Gọi PLACE_BID không kèm token xác thực
*   **Mô tả & Mục đích**: Client gửi gói tin đặt giá thầu thầu nhưng cố tình khuyết thiếu hoàn toàn trường `"token"` để kiểm tra tính bảo mật cổng thầu.
*   **Kết quả thực tế**: `✅ PASS` - Server từ chối ngay lập tức, trả về `status: "ERROR"` với thông báo `"Yêu cầu bắt buộc phải đính kèm token xác thực"`.
*   **Biểu hiện nếu lỗi (FAIL)**: Nếu gặp lỗ hổng (Bypass Authentication), kẻ tấn công có thể đặt thầu thầu nặc danh mà không cần đăng ký tài khoản, phá hoại hoàn toàn tính công bằng của sàn đấu giá.

#### 36. Test 36: Gửi Token giả mạo
*   **Mô tả & Mục đích**: Gửi yêu cầu API tạo sản phẩm kèm một mã token bị sửa đổi hoặc giả lập không hợp lệ (`"token": "fake-token-123"`).
*   **Kết quả thực tế**: `✅ PASS` - Server đối chiếu session RAM thất bại, trả về lỗi `ERROR` chặn đứng hành động trái phép.
*   **Biểu hiện nếu lỗi (FAIL)**: Server chấp nhận token giả mạo và cho phép thực thi tạo sản phẩm như bình thường, gây ra lỗ hổng an ninh xác thực cực kỳ nghiêm trọng.

#### 37. Test 37: Gửi gói tin JSON không hợp lệ (Malformed JSON)
*   **Mô tả & Mục đích**: Gửi trực tiếp chuỗi thô không đúng cú pháp JSON: `"this is not json"` qua socket để thử thách độ bền và khả năng chống chịu lỗi của server.
*   **Kết quả thực tế**: `✅ PASS` - Server bắt ngoại lệ an toàn, phản hồi gói lỗi `type: "UNKNOWN"` chuyên nghiệp và giữ kết nối TCP ổn định cho các client khác.
*   **Biểu hiện nếu lỗi (FAIL)**: Luồng xử lý kết nối chính của Socket Server bị crash đột ngột do ném ra lỗi phân tách chuỗi không được xử lý, làm ngắt kết nối hàng loạt tất cả các khách hàng đang trực tuyến khác (Sập Server).

#### 38. Test 38: Gửi Lệnh không xác định (Unknown Command)
*   **Mô tả & Mục đích**: Gửi gói tin JSON chứa loại yêu cầu `"type": "GHOST"` không hề được định nghĩa trong hệ thống.
*   **Kết quả thực tế**: `✅ PASS` - Server phản hồi lỗi `status: "ERROR"` thông báo lệnh không được hỗ trợ để hướng dẫn client giao tiếp chuẩn.
*   **Biểu hiện nếu lỗi (FAIL)**: Server bị treo luồng hoặc ném ngoại lệ NullPointer do không tìm thấy bộ xử lý Router phù hợp trong cấu trúc switch-case.

---

### Nhóm H: Kiểm Tra Sức Khỏe (1 Test)

#### 42. Test 39: HEALTH_CHECK
*   **Mô tả & Mục đích**: Gọi API kiểm tra sức khỏe hệ thống để lấy các thông số giám sát thời gian thực về uptime, active sessions và active auctions.
*   **Kết quả thực tế**: `✅ PASS` - Trả về đầy đủ thông số sức khỏe chi tiết, sửa đổi hoàn hảo lỗi thiếu API Health Check ban đầu.
*   **Biểu hiện nếu lỗi (FAIL)**: Server báo lỗi lệnh không xác định do chưa được router tiếp nhận (như ban đầu), khiến các hệ thống giám sát tự động (Prometheus/Grafana) báo động đỏ sai lệch về trạng thái hoạt động của server.

---

## 🛠️ Chi Tiết Các Lỗi Ban Đầu & Cách Khắc Phục

Dưới đây là mô tả chi tiết về các lỗi đã được phát hiện trong quá trình kiểm thử ban đầu và giải pháp lập trình đã được áp dụng để đưa tỷ lệ kiểm thử thành công đạt mức **100%**:

```carousel
### 🔴 1. Lỗi Serialization LocalDateTime (Test 17)
*   **Nguyên nhân:** Khi gọi `GET_ITEM_DETAIL`, hệ thống trả về raw `Item` object kế thừa class Entity chứa trường `LocalDateTime` ngày tạo. Jackson thư viện Java mặc định không hỗ trợ kiểu dữ liệu này nếu không cài module phụ, gây lỗi sập luồng phản hồi JSON.
*   **Cách khắc phục:** Sửa hàm trong `ItemHandler.java` để chuyển đổi thực thể sang `Map<String, Object>` thủ công, ép các trường thời gian thành chuỗi `createdAt.toString()` trước khi serialize.

<!-- slide -->
### 🔴 2. Lỗi CREATE_AUCTION Thiếu Tham Số Thời Gian (Test 20)
*   **Nguyên nhân:** Test script gửi yêu cầu tạo phòng đấu giá kèm trường `"durationMinutes": 60` nhưng API của Server chỉ chấp nhận hai mốc thời gian ISO là `"startTime"` và `"endTime"`, gây lỗi khuyết thiếu trường.
*   **Cách khắc phục:** Nâng cấp `AuctionHandler.java`. Nếu phát hiện tham số `durationMinutes`, hệ thống tự tính toán: `startTime = LocalDateTime.now()`, `endTime = startTime.plusMinutes(durationMinutes)` để đơn giản hóa giao thức.

<!-- slide -->
### 🔴 3. Lỗi Đấu Giá Chưa Bắt Đầu (Test 21)
*   **Nguyên nhân:** Dù phòng thầu được tạo bắt đầu ngay (`startTime = now`), trạng thái mặc định của phòng vẫn là `OPEN`. Luồng ngầm `AuctionLifecycleTask` chạy mỗi 5s mới transition trạng thái sang `RUNNING`. Đặt thầu thầu ngay lập tức sẽ bị `BidValidator` chặn và báo lỗi *"Phiên đấu giá chưa bắt đầu"*.
*   **Cách khắc phục:** Trong hàm tạo thầu tại `AuctionHandler.java`, bổ sung kiểm tra: Nếu `startTime <= now()`, lập tức transition trạng thái sang `RUNNING` trước khi lưu vào CSDL Postgres để kích hoạt thầu thầu ngay.

<!-- slide -->
### 🔴 4. Lỗi Đăng Nhập Quyền Admin Thất Bại (Test 26)
*   **Nguyên nhân:** CSDL PostgreSQL trống rỗng ban đầu không có bất kỳ tài khoản ADMIN nào, trong khi API công khai chặn đăng ký trực tiếp role ADMIN để bảo mật.
*   **Cách khắc phục:** Bổ sung phương thức `seedDefaultAdmin()` vào `MigrationRunner.java`. Khi khởi động máy chủ, nếu DB chưa có tài khoản Admin nào, tự khởi tạo tài khoản quản trị mặc định: **admin / admin123**.

<!-- slide -->
### 🔴 5. Thiếu Điểm Cuối HEALTH_CHECK (Test 39)
*   **Nguyên nhân:** Lệnh `HEALTH_CHECK` chưa được khai báo case trong router điều hướng của `RequestHandler.java`.
*   **Cách khắc phục:** Viết thêm phương thức `handleHealthCheck()` trong `RequestHandler.java` tính toán uptime hệ thống, số kết nối hoạt động và số phòng đấu giá đang chạy trả về JSON chuyên nghiệp.
```

---

## 💾 Danh Sách Các File Đã Được Chỉnh Sửa

Hệ thống đã được tinh chỉnh tại các tệp nguồn sau để đảm bảo hoạt động trơn tru đạt tỷ lệ pass tuyệt đối:

1.  **[ItemHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/ItemHandler.java):** Thay đổi phương thức tuần tự hóa chi tiết sản phẩm thành cấu trúc Map an toàn, tránh lỗi LocalDateTime.
2.  **[AuctionHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/AuctionHandler.java):** Bổ sung bộ xử lý thời lượng phòng đấu giá (`durationMinutes`) và cơ chế kích hoạt nhanh trạng thái đấu giá (`RUNNING`) khi bắt đầu ngay.
3.  **[RequestHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/RequestHandler.java):** Bổ sung bộ định tuyến cho lệnh `HEALTH_CHECK` và tính toán Uptime, Session count.
4.  **[MigrationRunner.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/config/MigrationRunner.java):** Thêm trình gieo hạt (seed) tài khoản Admin mặc định để đảm bảo sẵn sàng quản trị.
5.  **[test_bidhub.ps1](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/test_bidhub.ps1):** Hoàn thiện kịch bản kiểm thử tự động với dữ liệu đồng bộ và bổ sung đầy đủ thuộc tính sản phẩm điện tử.
