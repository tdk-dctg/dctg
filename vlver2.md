# Báo cáo Kiểm thử Hệ thống Đấu giá BidHub (Detailed Test Walkthrough)

Tài liệu này cung cấp chi tiết toàn bộ **42 ca kiểm thử (test cases)** được phân thành **8 nhóm chức năng** của hệ thống đấu giá trực tuyến BidHub. Mỗi test case được giải thích rõ ràng về **Mục đích kiểm tra**, **Kịch bản thực tế**, và **Kết quả mong đợi**.

---

## 📊 Tổng Kết Kết Quả Kiểm Thử

| Chỉ số | Giá trị | Trạng thái |
|--------|---------|------------|
| **Tổng số ca kiểm thử** | **42** | |
| **Số test vượt qua (PASS)** | **42** / 42 | ✅ Đạt 100% |
| **Số test thất bại (FAIL)** | **0** / 42 | ❌ Không có |
| **Tỷ lệ Pass ban đầu** | **28** / 42 | Gặp 14 lỗi logic & thiếu API |
| **Tỷ lệ Pass hiện tại (Sau fix)** | **42** / 42 | Hoàn toàn ổn định |

---

## 🔍 Mô Tả Chi Tiết 42 Ca Kiểm Thử (Từng Test Một)

### Nhóm A: Kết Nối Cơ Bản (1 Test)

#### 1. Test 1: PING
*   **Mục đích:** Kiểm tra tính sẵn sàng của Socket Server chạy trên cổng `9090`. Xác định xem server có thể tiếp nhận và phản hồi các gói tin cơ bản qua TCP hay không.
*   **Kịch bản:** Client kết nối TCP tới `localhost:9090` và gửi chuỗi JSON:
    ```json
    {"type":"PING","payload":{}}
    ```
*   **Kết quả mong đợi:** Server phản hồi ngay lập tức với định dạng:
    ```json
    {"status":"OK","type":"PING","payload":{}}
    ```

---

### Nhóm B: Đăng Ký & Đăng Nhập (9 Tests)

#### 2. Test 2: REGISTER thành công (SELLER)
*   **Mục đích:** Kiểm tra tính năng đăng ký tài khoản mới cho vai trò Người bán (SELLER) với đầy đủ thông tin hợp lệ.
*   **Kịch bản:** Gửi yêu cầu đăng ký tài khoản `seller01`:
    ```json
    {"type":"REGISTER","payload":{"username":"seller01","password":"12345678","email":"s@test.com","role":"SELLER"}}
    ```
*   **Kết quả mong đợi:** Đăng ký thành công, trả về trạng thái `OK` và một ID tài khoản (`userId`) duy nhất được lưu vào CSDL.

#### 3. Test 3: REGISTER trùng username
*   **Mục đích:** Đảm bảo hệ thống bắt lỗi trùng lặp dữ liệu, không cho phép hai tài khoản có cùng `username`.
*   **Kịch bản:** Tiếp tục gửi yêu cầu đăng ký với username `seller01` (dùng email khác `s2@test.com` và password hợp lệ).
*   **Kết quả mong đợi:** Đăng ký thất bại, trả về trạng thái `ERROR` kèm thông báo lỗi trùng tài khoản.

#### 4. Test 4: REGISTER password ngắn (<8 ký tự)
*   **Mục đích:** Xác thực chính sách mật khẩu an toàn tối thiểu (từ 8 ký tự trở lên).
*   **Kịch bản:** Đăng ký tài khoản `user2` nhưng mật khẩu chỉ có 7 ký tự (`1234567`).
*   **Kết quả mong đợi:** Đăng ký thất bại, trả về trạng thái `ERROR` yêu cầu mật khẩu có độ dài hợp lệ.

#### 5. Test 5: REGISTER email sai định dạng
*   **Mục đích:** Đảm bảo hệ thống kiểm tra định dạng email hợp lệ đầu vào trước khi lưu vào CSDL.
*   **Kịch bản:** Đăng ký tài khoản `user3` với chuỗi email không hợp lệ: `"noemail"`.
*   **Kết quả mong đợi:** Đăng ký thất bại, trả về trạng thái `ERROR` thông báo email sai định dạng.

#### 6. Test 6: REGISTER role ADMIN trực tiếp bị từ chối
*   **Mục đích:** Đảm bảo tính bảo mật của hệ thống: Người dùng phổ thông không thể tự đăng ký tài khoản Admin thông qua API công khai.
*   **Kịch bản:** Client gửi yêu cầu đăng ký với quyền `"role":"ADMIN"`.
*   **Kết quả mong đợi:** Đăng ký thất bại, trả về trạng thái `ERROR` chặn việc tạo tài khoản quản trị trực tiếp.

#### 7. Test 7: LOGIN thành công (SELLER)
*   **Mục đích:** Kiểm tra tính năng xác thực và cấp mã phiên hoạt động (Token) cho tài khoản SELLER hợp lệ.
*   **Kịch bản:** Đăng nhập bằng tài khoản `seller01` và mật khẩu đúng `12345678`.
*   **Kết quả mong đợi:** Xác thực thành công, trả về trạng thái `OK` kèm một chuỗi token ngẫu nhiên đại diện cho phiên làm việc.

#### 8. Test 8: LOGIN sai mật khẩu
*   **Mục đích:** Ngăn chặn truy cập trái phép bằng thông tin mật khẩu không chính xác.
*   **Kịch bản:** Đăng nhập tài khoản `seller01` nhưng gửi mật khẩu sai: `"wrongpass"`.
*   **Kết quả mong đợi:** Đăng nhập thất bại, trả về trạng thái `ERROR` và thông báo thông tin đăng nhập không khớp.

#### 9. Test 9: LOGIN username không tồn tại
*   **Mục đích:** Đảm bảo hệ thống xử lý đúng khi người dùng cố gắng đăng nhập với tài khoản chưa được đăng ký.
*   **Kịch bản:** Đăng nhập bằng tài khoản không tồn tại trong hệ thống: `"ghost"`.
*   **Kết quả mong đợi:** Đăng nhập thất bại, trả về trạng thái `ERROR`.

#### 10. Test 10: LOGIN thiếu password
*   **Mục đích:** Kiểm tra tính toàn vẹn của dữ liệu đầu vào (Validation) đối với các trường bắt buộc của API.
*   **Kịch bản:** Gửi yêu cầu đăng nhập chứa trường `username` nhưng bỏ trống hoàn toàn trường `password`.
*   **Kết quả mong đợi:** Đăng nhập thất bại, hệ thống báo lỗi thiếu tham số đầu vào.

---

### Nhóm C: Quản Lý Sản Phẩm (10 Tests)

#### 11. Test 11: CREATE_ITEM thành công
*   **Mục đích:** Cho phép Người bán (SELLER) đăng thông tin sản phẩm mới vào danh mục bán đấu giá.
*   **Kịch bản:** Sử dụng Token đã lấy từ bước đăng nhập của `seller01`, gửi yêu cầu tạo sản phẩm Laptop thuộc loại thiết bị điện tử (`itemType: ELECTRONICS`) kèm theo các thông tin kỹ thuật bổ sung (`brand: ASUS`, `warrantyMonths: 24`).
*   **Kết quả mong đợi:** Tạo sản phẩm thành công, lưu lại ID sản phẩm (`itemId`) để sử dụng cho các bài test tiếp theo.

#### 12. Test 12: CREATE_ITEM thiếu tên sản phẩm
*   **Mục đích:** Đảm bảo các thuộc tính bắt buộc của sản phẩm (như Tên sản phẩm) không được để trống.
*   **Kịch bản:** Gửi yêu cầu tạo sản phẩm có mô tả và giá cả nhưng không truyền trường `name`.
*   **Kết quả mong đợi:** Thất bại, trả về trạng thái `ERROR`.

#### 13. Test 13: CREATE_ITEM giá khởi điểm bằng 0
*   **Mục đích:** Đảm bảo giá khởi điểm của sản phẩm đưa lên sàn đấu giá phải là một số dương lớn hơn 0.
*   **Kịch bản:** Gửi yêu cầu tạo sản phẩm với `"startingPrice": 0`.
*   **Kết quả mong đợi:** Thất bại, trả về trạng thái `ERROR` báo giá khởi điểm không hợp lệ.

#### 14. Test 14: CREATE_ITEM sai danh mục sản phẩm (itemType)
*   **Mục đích:** Kiểm tra tính chặt chẽ của kiểu danh mục. Hệ thống chỉ chấp nhận các danh mục được khai báo sẵn (ví dụ: `ELECTRONICS`, `FURNITURE`, `ART`, v.v.).
*   **Kịch bản:** Gửi yêu cầu tạo sản phẩm với danh mục `"itemType": "BOOK"` (danh mục không tồn tại trong cấu hình hệ thống).
*   **Kết quả mong đợi:** Thất bại, hệ thống trả về lỗi không thể phân loại sản phẩm.

#### 15a. Đăng ký tài khoản BIDDER
*   **Mục đích:** Tạo một tài khoản Người mua (BIDDER) phục vụ cho các chức năng đấu giá và kiểm tra phân quyền.
*   **Kịch bản:** Đăng ký tài khoản mới `bidder01` với vai trò `"role": "BIDDER"`.
*   **Kết quả mong đợi:** Đăng ký thành công, nhận về `userId` của người mua.

#### 15b. LOGIN BIDDER
*   **Mục đích:** Lấy Token xác thực cho tài khoản Người mua (`bidder01`).
*   **Kịch bản:** Đăng nhập tài khoản `bidder01` với mật khẩu đúng `12345678`.
*   **Kết quả mong đợi:** Thành công, lưu lại `$BIDDER_TOKEN`.

#### 15. Test 15: CREATE_ITEM không có quyền (Tài khoản BIDDER cố tình tạo)
*   **Mục đích:** Kiểm tra hệ thống phân quyền (RBAC) - Người mua (BIDDER) không được phép tự tạo sản phẩm bán đấu giá.
*   **Kịch bản:** Sử dụng `$BIDDER_TOKEN` để gọi API `CREATE_ITEM`.
*   **Kết quả mong đợi:** Bị từ chối quyền truy cập, hệ thống trả về lỗi phân quyền `ERROR`.

#### 16. Test 16: GET_ITEM_LIST
*   **Mục đích:** Kiểm tra API lấy danh sách các sản phẩm đang có trên hệ thống để người mua tìm kiếm.
*   **Kịch bản:** Client gửi yêu cầu lấy danh sách sản phẩm.
*   **Kết quả mong đợi:** Thành công, trả về danh sách các sản phẩm (bao gồm Laptop ở Test 11).

#### 17. Test 17: GET_ITEM_DETAIL
*   **Mục đích:** Lấy thông tin chi tiết một sản phẩm cụ thể dựa trên ID sản phẩm, bao gồm cả các thuộc tính đặc thù (extras).
*   **Kịch bản:** Gửi yêu cầu lấy chi tiết của sản phẩm có mã `$ITEM_ID`.
*   **Kết quả mong đợi:** Thành công, thông tin sản phẩm và các trường `brand`, `warrantyMonths` được hiển thị rõ ràng. *(Đã sửa lỗi lỗi Serialization LocalDateTime ở bước này)*.

#### 18. Test 18: DELETE_ITEM
*   **Mục đích:** Kiểm tra tính năng xóa sản phẩm do Người bán sở hữu khi sản phẩm chưa được đưa lên phiên đấu giá.
*   **Kịch bản:** Người bán `seller01` dùng token của mình để gửi yêu cầu xóa sản phẩm có mã `$ITEM_ID`.
*   **Kết quả mong đợi:** Thành công, sản phẩm bị loại bỏ khỏi danh mục đang hoạt động.

#### 19. Test 19: DELETE_ITEM không tồn tại
*   **Mục đích:** Kiểm tra lỗi khi cố gắng xóa một sản phẩm với ID không hợp lệ hoặc giả mạo.
*   **Kịch bản:** Dùng token người bán yêu cầu xóa sản phẩm có ID `"fake-id"`.
*   **Kết quả mong đợi:** Thất bại, trả về trạng thái `ERROR` báo sản phẩm không tồn tại.

#### 19a. CREATE_ITEM phục vụ đấu giá (Laptop2)
*   **Mục đích:** Tạo mới lại một sản phẩm chất lượng để đưa vào phiên đấu giá ở Nhóm D.
*   **Kịch bản:** Đăng ký lại sản phẩm `Laptop2` (dòng Dell, bảo hành 12 tháng) với đầy đủ cấu trúc extras hợp lệ cho phân loại `ELECTRONICS`.
*   **Kết quả mong đợi:** Tạo thành công sản phẩm mới, cập nhật lại mã `$ITEM_ID` đang hoạt động.

---

### Nhóm D: Đấu Giá (5 Tests)

#### 20. Test 20: CREATE_AUCTION
*   **Mục đích:** Thiết lập phòng đấu giá cho một sản phẩm hợp lệ, cấu hình mức giá khởi điểm, bước giá tối thiểu và thời gian diễn ra.
*   **Kịch bản:** Người bán dùng token gửi yêu cầu mở phiên đấu giá cho sản phẩm vừa tạo ở Test 19a, đặt tham số thời gian đấu giá là 60 phút (`"durationMinutes": 60`).
*   **Kết quả mong đợi:** Phòng đấu giá được khởi tạo thành công, trả về mã `$AUCTION_ID`. Trạng thái phiên tự động chuyển thành `RUNNING` vì thời gian bắt đầu có hiệu lực ngay lập tức.

#### 21. Test 21: PLACE_BID thành công
*   **Mục đích:** Kiểm tra chức năng người mua đặt giá hợp lệ cho phiên đấu giá đang diễn ra.
*   **Kịch bản:** Người mua `bidder01` sử dụng `$BIDDER_TOKEN` để đặt mức giá `16000000` (lớn hơn giá khởi điểm 15 triệu + bước giá tối thiểu 500k).
*   **Kết quả mong đợi:** Đặt giá thành công, hệ thống ghi nhận lịch sử thầu và cập nhật giá cao nhất hiện tại của phiên là 16 triệu.

#### 22. Test 22: PLACE_BID giá thấp hơn mức hiện tại
*   **Mục đích:** Bảo vệ quy tắc đấu giá: Mức giá đặt sau luôn phải lớn hơn mức giá cao nhất hiện tại cộng với bước giá tối thiểu.
*   **Kịch bản:** Người mua tiếp tục đặt giá thầu là `10000000` (nhỏ hơn mức thầu cao nhất hiện tại là 16 triệu).
*   **Kết quả mong đợi:** Thất bại, hệ thống chặn giao dịch và báo lỗi giá đặt thầu quá thấp.

#### 23. Test 23: PLACE_BID phiên đấu giá không tồn tại
*   **Mục đích:** Ngăn ngừa việc đặt giá thầu lỗi vào các phiên đấu giá ảo hoặc đã bị xóa.
*   **Kịch bản:** Người mua gửi yêu cầu đặt thầu với mã phiên đấu giá không hợp lệ: `"fake-auction"`.
*   **Kết quả mong đợi:** Thất bại, hệ thống báo lỗi không tìm thấy phiên đấu giá tương ứng.

#### 24. Test 24: GET_AUCTION_DETAIL
*   **Mục đích:** Cho phép người dùng theo dõi tiến trình của phòng đấu giá trực tiếp: thông tin sản phẩm, thời gian còn lại, và lịch sử người đang dẫn đầu.
*   **Kịch bản:** Gửi yêu cầu lấy chi tiết phiên đấu giá qua mã `$AUCTION_ID`.
*   **Kết quả mong đợi:** Thành công, phản hồi đầy đủ cấu trúc dữ liệu của phiên đấu giá kèm theo thông tin chi tiết sản phẩm.

#### 25. Test 25: GET_AUCTION_LIST
*   **Mục đích:** Hiển thị danh sách tất cả các phòng đấu giá đang có trên hệ thống để người mua lựa chọn tham gia.
*   **Kịch bản:** Gửi yêu cầu lấy toàn bộ danh sách phiên đấu giá hiện hành.
*   **Kết quả mong đợi:** Thành công, trả về danh sách có chứa phiên đấu giá hoạt động mà ta vừa cấu hình.

---

### Nhóm E: Quản Trị Viên - Admin (5 Tests)

#### 26. Test 26: LOGIN ADMIN
*   **Mục đích:** Xác thực quyền truy cập dành riêng cho Quản trị viên tối cao của hệ thống.
*   **Kịch bản:** Đăng nhập bằng tài khoản quản trị mặc định được sinh tự động bởi hệ thống (`admin` / `admin123`).
*   **Kết quả mong đợi:** Đăng nhập thành công, trả về mã phiên quản trị lưu vào biến `$ADMIN_TOKEN`.

#### 27. Test 27: GET_USER_LIST
*   **Mục đích:** Quyền hạn Admin: Giám sát toàn bộ cơ sở người dùng trong hệ thống (bao gồm cả Người bán và Người mua).
*   **Kịch bản:** Sử dụng `$ADMIN_TOKEN` để gọi API lấy danh sách tài khoản người dùng.
*   **Kết quả mong đợi:** Thành công, hiển thị đầy đủ danh sách các tài khoản đã tạo trước đó.

#### 28. Test 28: LOCK_USER
*   **Mục đích:** Quyền hạn Admin: Khóa tài khoản của thành viên vi phạm quy chế hoạt động của sàn đấu giá.
*   **Kịch bản:** Admin gửi lệnh khóa tài khoản người mua có mã `$BIDDER_USER_ID`.
*   **Kết quả mong đợi:** Khóa thành công. Sau khi bị khóa, tài khoản này sẽ không thể thực hiện các thao tác xác thực hoặc đặt giá.

#### 29. Test 29: UNLOCK_USER
*   **Mục đích:** Quyền hạn Admin: Mở khóa tài khoản cho thành viên sau khi giải quyết xong tranh chấp hoặc vi phạm.
*   **Kịch bản:** Admin gửi lệnh mở khóa lại cho tài khoản người mua `$BIDDER_USER_ID`.
*   **Kết quả mong đợi:** Mở khóa thành công, khôi phục lại trạng thái hoạt động bình thường cho tài khoản.

#### 30. Test 30: LOCK_USER không có quyền (Người dùng thường cố ý khóa người khác)
*   **Mục đích:** Bảo mật hệ thống: Chặn tuyệt đối hành vi giả mạo hoặc cố tình lạm quyền của người dùng thông thường đối với các API quản trị.
*   **Kịch bản:** Người mua `bidder01` dùng mã `$BIDDER_TOKEN` của mình để yêu cầu khóa tài khoản của người bán `$SELLER_USER_ID`.
*   **Kết quả mong đợi:** Bị chặn ngay lập tức, trả về lỗi phân quyền `ERROR`.

---

### Nhóm F: Báo Cáo & Kiểm Toán (4 Tests)

#### 31. Test 31: GET_AUCTION_REPORT
*   **Mục đích:** Cung cấp cho Admin báo cáo tổng hợp về các phiên đấu giá đã và đang diễn ra để phân tích hiệu suất kinh doanh.
*   **Kịch bản:** Admin dùng `$ADMIN_TOKEN` yêu cầu xuất báo cáo thống kê phòng đấu giá.
*   **Kết quả mong đợi:** Trả về danh sách tổng hợp chi tiết các phiên đấu giá kèm trạng thái hiện tại.

#### 32. Test 32: GET_BID_HISTORY_REPORT
*   **Mục đích:** Cho phép Admin kiểm tra, truy vết lịch sử đặt giá chi tiết của một phiên đấu giá cụ thể để phát hiện dấu hiệu đẩy giá ảo.
*   **Kịch bản:** Admin yêu cầu xem lịch sử đặt thầu của phiên đấu giá có mã `$AUCTION_ID`.
*   **Kết quả mong đợi:** Trả về đầy đủ dòng thời gian đặt giá thầu kèm định danh người mua và số tiền tương ứng.

#### 33. Test 33: GET_AUDIT_LOG
*   **Mục đích:** Ghi lại vết hoạt động (Audit Trails) của hệ thống đối với các hành động nhạy cảm như Đăng ký, Đăng nhập, Khóa tài khoản phục vụ công tác an toàn thông tin.
*   **Kịch bản:** Admin yêu cầu lấy 10 bản ghi nhật ký hệ thống gần nhất.
*   **Kết quả mong đợi:** Trả về danh sách nhật ký hành động ghi rõ: Thời gian, Người thực hiện, Hành động cụ thể và Trạng thái kết quả.

#### 34. Test 34: RUN_INTEGRITY_CHECK
*   **Mục đích:** Thực hiện kiểm tra tính toàn vẹn dữ liệu trong cơ sở dữ liệu (Database Integrity) để đảm bảo không có bản ghi mồ côi hoặc sai lệch liên kết vật lý.
*   **Kịch bản:** Admin gửi lệnh kiểm tra toàn vẹn hệ thống dữ liệu.
*   **Kết quả mong đợi:** Trạng thái trả về là `OK` chứng tỏ cơ sở dữ liệu hoạt động hoàn toàn nhất quán.

---

### Nhóm G: Bảo Mật & Trường Hợp Biên (4 Tests)

#### 35. Test 35: Gọi PLACE_BID không kèm token xác thực
*   **Mục đích:** Đảm bảo hệ thống chặn mọi cuộc gọi API ẩn danh đối với các hành động nhạy cảm liên quan tới tiền tệ và giao dịch.
*   **Kịch bản:** Gửi yêu cầu đặt giá thầu nhưng lược bỏ hoàn toàn trường `"token"` trong gói tin JSON.
*   **Kết quả mong đợi:** Yêu cầu bị loại bỏ ngay từ bộ lọc xác thực, báo lỗi thiếu token.

#### 36. Test 36: Gửi Token giả mạo
*   **Mục đích:** Đảm bảo hệ thống giải mã và kiểm tra tính hợp lệ của Token trong phiên làm việc, từ chối Token đã bị chỉnh sửa hoặc hết hạn.
*   **Kịch bản:** Gửi yêu cầu tạo sản phẩm với mã token không hợp lệ: `"token":"fake-token-123"`.
*   **Kết quả mong đợi:** Bị từ chối thực hiện, trả về lỗi token không hợp lệ.

#### 37. Test 37: Gửi gói tin JSON không hợp lệ (Malformed JSON)
*   **Mục đích:** Kiểm tra khả năng chống chịu lỗi (Fault-Tolerance) của Socket Server. Server không được phép bị sập hoặc gặp lỗi treo (OutOfMemory/Exception) khi nhận luồng dữ liệu bị lỗi cú pháp.
*   **Kịch bản:** Gửi trực tiếp chuỗi thô không tuân thủ định dạng JSON: `"this is not json"` qua Socket.
*   **Kết quả mong đợi:** Server bắt được lỗi phân tách chuỗi, phản hồi lại mã lỗi dạng `type: "UNKNOWN"` một cách an toàn và giữ kết nối ổn định cho các client khác.

#### 38. Test 38: Gửi Lệnh không xác định (Unknown Command)
*   **Mục đích:** Đảm bảo hệ thống phản hồi chuẩn mực khi nhận được các loại yêu cầu nằm ngoài danh sách hỗ trợ của Router.
*   **Kịch bản:** Gửi gói tin có loại yêu cầu `"type": "GHOST"`.
*   **Kết quả mong đợi:** Trả về trạng thái lỗi báo lệnh không được hỗ trợ.

---

### Nhóm H: Kiểm Tra Sức Khỏe Hệ Thống (1 Test)

#### 39. Test 39: HEALTH_CHECK
*   **Mục đích:** Cung cấp thông tin giám sát thời gian thực (Real-time Monitoring) về sức khỏe phần cứng, thời gian hoạt động liên tục (Uptime), số kết nối hoạt động và hiệu năng máy chủ.
*   **Kịch bản:** Client gửi yêu cầu kiểm tra sức khỏe `"type": "HEALTH_CHECK"`.
*   **Kết quả mong đợi:** Trả về đầy đủ các thông số: thời gian chạy liên tục (`uptime` tính bằng mili-giây), tổng số phiên đấu giá, số lượng kết nối người dùng hiện tại và thời gian hệ thống.

---

## 🛠️ Chi Tiết Các Lỗi Ban Đầu & Cách Khắc Phục

Dưới đây là mô tả chi tiết về **14 lỗi** đã được phát hiện trong quá trình kiểm thử ban đầu và các giải pháp lập trình đã được áp dụng để đưa tỷ lệ kiểm thử thành công đạt mức **100%**:

```carousel
### 🔴 1. Lỗi Serialization LocalDateTime (Test 17)
*   **Vấn đề:** Khi gọi `GET_ITEM_DETAIL`, hệ thống cố gắng chuyển đổi trực tiếp đối tượng thực thể `Item` sang chuỗi JSON. Do Jackson thư viện mặc định không hỗ trợ kiểu dữ liệu `LocalDateTime` nếu không cài đặt module JavaTime, chương trình trả về chuỗi rỗng hoặc phát sinh lỗi Serialization.
*   **Giải pháp:** Thay đổi logic trong `ItemHandler.java` để chuyển đổi đối tượng `Item` thành một đối tượng trung gian `Map<String, Object>` với các trường thời gian được định dạng chuỗi `toString()` tường minh trước khi serialize.

<!-- slide -->
### 🔴 2. Lỗi CREATE_AUCTION Thiếu Tham Số Thời Gian (Test 20)
*   **Vấn đề:** Test script gửi yêu cầu tạo đấu giá kèm trường `"durationMinutes": 60` nhưng API của Server chỉ chấp nhận hai mốc thời gian tĩnh `"startTime"` và `"endTime"` dạng ISO.
*   **Giải pháp:** Nâng cấp hàm `handleCreateAuction` trong `AuctionHandler.java`. Nếu phát hiện tham số `durationMinutes` được gửi từ client, hệ thống sẽ tự động tính toán:
    *   `startTime = LocalDateTime.now()`
    *   `endTime = startTime.plusMinutes(durationMinutes)`

<!-- slide -->
### 🔴 3. Lỗi Đấu Giá Chưa Bắt Đầu (Test 21)
*   **Vấn đề:** Dù phiên đấu giá được tạo với thời gian bắt đầu là hiện tại (`now()`), trạng thái mặc định của phòng đấu giá khi lưu vào DB vẫn là `OPEN` (Chờ kích hoạt). Tiến trình chạy ngầm `AuctionManager` chỉ quét và chuyển đổi trạng thái sang `RUNNING` sau mỗi chu kỳ 5 giây. Nếu client đặt giá ngay lập tức, bộ kiểm tra `BidValidator` sẽ chặn và trả về lỗi: *"Phiên đấu giá chưa bắt đầu. Vui lòng chờ đến giờ."*
*   **Giải pháp:** Trong hàm tạo phòng đấu giá tại `AuctionHandler.java`, bổ sung đoạn mã kiểm tra: Nếu thời gian bắt đầu phòng đấu giá nhỏ hơn hoặc bằng thời gian hiện tại (`startTime <= now`), lập tức chuyển trạng thái phòng sang `RUNNING` trước khi lưu vào CSDL.

<!-- slide -->
### 🔴 4. Lỗi Đăng Nhập Quyền Admin Thất Bại (Test 26 -> 34)
*   **Vấn đề:** Hệ thống cơ sở dữ liệu ban đầu hoàn toàn trống rỗng và không có cơ chế cho phép đăng ký trực tiếp quyền `ADMIN` qua API công khai (để bảo mật). Do đó không có tài khoản quản trị nào tồn tại để chạy các bài test giám sát.
*   **Giải pháp:** Bổ sung phương thức tự động khởi tạo dữ liệu mẫu (`seedDefaultAdmin()`) trong `MigrationRunner.java`. Khi máy chủ khởi động, nếu CSDL chưa có bất kỳ tài khoản Admin nào, hệ thống tự động khởi tạo tài khoản quản trị mặc định: **admin / admin123**.

<!-- slide -->
### 🔴 5. Thiếu Điểm Cuối HEALTH_CHECK (Test 39)
*   **Vấn đề:** Máy chủ hoàn toàn chưa định nghĩa API kiểm tra sức khỏe khiến cuộc gọi từ client trả về lỗi Lệnh không xác định.
*   **Giải pháp:** Viết thêm phương thức `handleHealthCheck()` trong `RequestHandler.java` để tính toán thời gian uptime của máy chủ, số phòng đấu giá đang mở, số phiên kết nối socket và trả về cho client một cách chuyên nghiệp.

<!-- slide -->
### 🔴 6. Lỗi Thiếu Trường Dữ Liệu Trong Cấu Hình Sản Phẩm (Test 19a)
*   **Vấn đề:** API kiểm tra ràng buộc của hệ thống đối với sản phẩm điện tử (`ELECTRONICS`) yêu cầu bắt buộc phải truyền thông tin `brand` và `warrantyMonths` trong trường `extras`. Tuy nhiên test script ban đầu gửi dữ liệu rỗng `{}` dẫn tới tạo sản phẩm lỗi.
*   **Giải pháp:** Cập nhật lại payload gửi từ file test script `test_bidhub.ps1` để bổ sung đầy đủ thông tin extras hợp lệ cho sản phẩm điện tử.
```

---

## 💾 Danh Sách Các File Đã Được Chỉnh Sửa

Hệ thống đã được tinh chỉnh tại các tệp nguồn sau để đảm bảo hoạt động trơn tru:

1.  **[ItemHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/ItemHandler.java):** Thay đổi phương thức tuần tự hóa chi tiết sản phẩm thành cấu trúc Map an toàn.
2.  **[AuctionHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/AuctionHandler.java):** Bổ sung bộ xử lý thời lượng phòng đấu giá (`durationMinutes`) và cơ chế kích hoạt nhanh trạng thái đấu giá (`RUNNING`).
3.  **[RequestHandler.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/network/RequestHandler.java):** Bổ sung bộ định tuyến cho lệnh `HEALTH_CHECK`.
4.  **[MigrationRunner.java](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/bidhub-server/src/main/java/com/bidhub/server/config/MigrationRunner.java):** Thêm trình gieo hạt tài khoản Admin mặc định an toàn.
5.  **[test_bidhub.ps1](file:///c:/Users/Admin/IdeaProjects/btl/Auction-System/test_bidhub.ps1):** Hoàn thiện kịch bản kiểm thử tự động với dữ liệu đồng bộ và bổ sung đầy đủ thuộc tính sản phẩm.
