# 📘 Tuần 1 – Giải mã toàn bộ kiến thức & thiết kế

> **Mục tiêu của tài liệu này:** Giúp mọi thành viên trong nhóm hiểu sâu về các quyết định kiến trúc, lý do lựa chọn công nghệ, cách các module tương tác, và ý nghĩa của từng dòng code trong tuần 1. Từ đó, mỗi người có thể giải thích được code của người khác và thấy được bức tranh tổng thể của dự án.

---

## 1. Tổng quan tuần 1 – Tại sao phải làm những việc này?

### 🎯 Mục tiêu chính của tuần 1

Tuần 1 không phải là tuần để code nhiều chức năng. Tuần này là **nền móng** của toàn bộ dự án - giống như việc xây móng nhà vậy. Nếu móng không vững, 9 tuần sau sẽ liên tục phải sửa lỗi từ tuần 1.

Cuối tuần 1, cả nhóm phải có:
- ✅ **Maven multi-module project** build được không lỗi
- ✅ **CI/CD** (GitHub Actions) chạy tự động khi push code  
- ✅ **ConfigLoader** đọc được file `.properties`
- ✅ **JavaFX LoginView** mở được trên màn hình
- ✅ **JUnit 5** chạy được, ≥ 15 test cases xanh

### 📊 Đóng góp vào barem điểm

Tuần này trực tiếp phục vụ 2 tiêu chí chấm điểm quan trọng:

| Tiêu chí | Điểm | Công việc tuần 1 |
|----------|------|------------------|
| **Maven/Gradle + coding convention + code sạch** | 0.5đ | Maven setup + `.gitignore` + Style Guide |
| **CI/CD cơ bản (GitHub Actions)** | 0.5đ | GitHub Actions workflow |

→ **Làm đúng tuần 1 = 1.0đ trong tay ngay từ đầu**

### ⚠️ Hậu quả nếu tuần 1 không hoàn thành đúng

- **Tuần 2-3:** Không thể compile code vì thiếu dependency trong `pom.xml`
- **Tuần 4-6:** Networking code không chạy vì `jackson-databind` version sai
- **Tuần 7-8:** CI fail liên tục vì thiếu test setup từ tuần 1
- **Tuần 9-10:** Phải rewrite lại Maven structure thay vì tập trung vào demo

**Nguyên tắc vàng:** Tuần 1 xong sớm, xong đúng → 9 tuần sau chỉ cần focus vào business logic, không lo infrastructure.

---

## 2. Những khái niệm nền tảng phải học trước khi code

### 2.1. Maven Multi-Module Project

#### 🎓 Bản chất trong Java

Maven là **build tool** - công cụ quản lý dependencies (thư viện) và build (biên dịch) project. Multi-module nghĩa là 1 project lớn chia thành nhiều module con, mỗi module có `pom.xml` riêng nhưng chung 1 `pom.xml` cha.

**Cấu trúc:**
```
bidhub-parent (pom.xml cha)
├── bidhub-server (module con)
└── bidhub-client (module con)
```

**Lợi ích chính:**
- **Tách biệt rõ ràng:** Server code không lẫn với Client code
- **Quản lý version tập trung:** Khai báo Jackson 2.17.0 ở parent → tất cả module con dùng chung version
- **Build 1 lần:** `mvn clean install` ở thư mục gốc → build cả 2 module

#### 🔍 Tại sao BidHub lại cần nó?

Hệ thống BidHub có 2 thành phần hoàn toàn khác nhau:

1. **bidhub-server:** 
   - Chạy trên máy chủ, quản lý database, xử lý socket
   - Cần: `sqlite-jdbc`, `jackson-databind`
   - KHÔNG cần: JavaFX (vì server không có giao diện)

2. **bidhub-client:** 
   - Chạy trên máy người dùng, hiển thị GUI
   - Cần: JavaFX controls, JavaFX fxml
   - KHÔNG cần: SQLite (vì client không trực tiếp truy cập database)

**Nếu không tách module:**
- Server phải kéo theo JavaFX dependencies → file JAR nặng thêm 30MB vô nghĩa
- Client phải có SQLite → lộ database password trong code client
- Khi deploy server lên cloud, phải upload cả JavaFX → lãng phí bandwidth

#### 💡 Ví dụ thực tế từ code tuần 1

**File `pom.xml` cha (bidhub-parent):**
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.17.0</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**File `pom.xml` con (bidhub-server):**
```xml
<dependencies>
  <dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <!-- KHÔNG cần khai báo version vì đã có trong parent -->
  </dependency>
</dependencies>
```

→ Khi update Jackson lên version mới, chỉ sửa 1 chỗ trong parent → tất cả module tự động update.

---

### 2.2. Git Workflow & Conventional Commits

#### 🎓 Bản chất của Git

Git là **version control system** - hệ thống quản lý phiên bản code. Thay vì lưu code thành `project_v1.zip`, `project_v2_final.zip`, `project_v2_final_REAL.zip`, Git lưu lịch sử thay đổi và cho phép làm việc song song trên nhiều branch.

**Khái niệm cốt lõi:**
- **Repository (repo):** Kho chứa toàn bộ code + lịch sử thay đổi
- **Commit:** 1 "snapshot" của code tại 1 thời điểm
- **Branch:** Nhánh phát triển độc lập - giống như làm thêm phòng trong nhà mà không phá nhà chính
- **Pull Request (PR):** Yêu cầu merge code từ branch A vào branch B, cần người khác review

#### 🔍 Tại sao BidHub cần Git workflow nghiêm ngặt?

**Vấn đề thực tế:**
- 4 người cùng sửa file `Auction.java` → conflict khủng khiếp
- Đăng push code chưa test → server crash → cả nhóm không làm việc được
- Quốc Minh commit "fix bug" nhưng không ai biết fix bug gì → sau 1 tháng không ai dám sửa file đó

**Giải pháp:**
```
main (production) ← chỉ merge khi code đã test kỹ
  ↑
develop (integration) ← merge từ feature branches
  ↑
feature/tuan-1-dang-maven-setup ← Đăng làm việc ở đây
feature/tuan-1-quocminh-cicd ← Quốc Minh làm việc ở đây
```

**Quy tắc bất di bất dịch:**
- KHÔNG bao giờ push thẳng lên `main` hoặc `develop`
- Luôn tạo branch mới: `feature/tuan-X-ten-nguoi-mo-ta`
- Commit message theo format: `feat: thêm ConfigLoader` hoặc `fix: sửa lỗi Maven compile`

#### 💡 Conventional Commits - Tại sao quan trọng?

Format chuẩn:
```
<type>: <description>

Type phổ biến:
- feat: thêm tính năng mới
- fix: sửa bug
- docs: thay đổi documentation
- test: thêm/sửa test
- refactor: cải thiện code mà không thay đổi tính năng
```

**Lợi ích:**
1. **Auto-generate changelog:** Tool có thể tự động tạo file CHANGELOG từ lịch sử commit
2. **Dễ tìm commit:** `git log --oneline --grep="feat:"` → liệt kê tất cả feature mới
3. **Review nhanh hơn:** Nhìn commit message biết ngay người đó làm gì

**Ví dụ thực tế:**
```bash
# ❌ Tệ
git commit -m "update"
git commit -m "fix"
git commit -m "asdfasdf"

# ✅ Tốt
git commit -m "feat: thêm ConfigLoader đọc server.properties"
git commit -m "fix: sửa lỗi Maven không tìm thấy Jackson dependency"
git commit -m "test: thêm 5 test cases cho Calculator"
```

---

### 2.3. JavaFX Architecture (Stage, Scene, Controller)

#### 🎓 Bản chất trong JavaFX

JavaFX là framework xây dựng **desktop GUI** trong Java. Cấu trúc giống như nhà hát:

- **Stage:** Cái sân khấu (cửa sổ ứng dụng)
- **Scene:** Bối cảnh trên sân khấu (1 màn hình cụ thể, ví dụ: LoginView)
- **Node:** Các diễn viên/đạo cụ (Button, TextField, Label...)

```
Application
  └── Stage (Window)
       └── Scene (LoginView)
            ├── TextField (username)
            ├── PasswordField (password)
            └── Button (đăng nhập)
```

**FXML vs Java Code thuần:**
- FXML: Viết layout bằng XML (giống HTML)
- Java code: Viết layout bằng `new Button()`, `setLayoutX()`...

→ FXML tách biệt layout (UI) khỏi logic (Controller) → dễ sửa, dễ đọc hơn

#### 🔍 Tại sao BidHub dùng FXML?

**Tình huống thực tế:**

Tuần 5, Công Minh cần thêm 1 TextField mới cho "Email" trong LoginView.

**Nếu dùng Java code thuần:**
```java
// Phải đọc 200 dòng code để tìm chỗ khai báo TextField
TextField emailField = new TextField();
emailField.setLayoutX(100);
emailField.setLayoutY(150);
emailField.setPrefWidth(200);
// ... 50 dòng khác
```

**Nếu dùng FXML:**
```xml
<!-- Mở file LoginView.fxml trong Scene Builder -->
<!-- Kéo thả TextField mới vào -->
<!-- Save → xong -->
```

→ Công Minh sửa UI mà không sợ làm hỏng logic của Quốc Minh trong Controller.

#### 💡 Flow hoạt động của JavaFX

```
BidHubApp.java (main)
  ↓
load LoginView.fxml
  ↓
FXML tự động tạo TextField, Button...
  ↓
FXML inject các object vào LoginController (qua @FXML)
  ↓
initialize() trong Controller được gọi
  ↓
User nhập username, password, click button
  ↓
handleLogin() trong Controller được gọi
```

**Code ví dụ:**

`LoginView.fxml`:
```xml
<TextField fx:id="usernameField" />
<Button text="Đăng nhập" onAction="#handleLogin" />
```

`LoginController.java`:
```java
public class LoginController {
    @FXML private TextField usernameField; // Tự động inject từ FXML
    
    @FXML // Annotation này nói với FXML: "method này gọi từ UI"
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        // Xử lý logic...
    }
    
    @FXML // Được gọi SAU KHI @FXML fields đã inject xong
    public void initialize() {
        // Setup ban đầu, ví dụ: disable button khi chưa nhập gì
    }
}
```

---

### 2.4. JUnit 5 & Test-Driven Mindset

#### 🎓 Bản chất của Unit Test

Unit test là **code để test code**. Thay vì chạy app bằng tay, click button, nhập input để xem có bug không → viết code tự động làm việc đó.

**Ví dụ:**
```java
// Code thực tế
public int divide(int a, int b) {
    return a / b;
}

// Unit test
@Test
void testDivide_Normal() {
    assertEquals(5, divide(10, 2));
}

@Test
void testDivide_ByZero_ThrowsException() {
    assertThrows(ArithmeticException.class, () -> divide(10, 0));
}
```

#### 🔍 Tại sao BidHub cần ≥ 15 test cases ngay tuần 1?

**Vấn đề thực tế:**

Tuần 6, Quốc Minh sửa `BidValidator.isValidBid()` để xử lý edge case. Sau khi push code:
- 3 chức năng khác bỗng nhiên bị lỗi
- Không ai biết tại sao
- Mất 2 ngày để tìm bug

**Nếu có unit test:**
```bash
mvn test
# → 5 tests FAILED
# → testValidBid_WithNegativeAmount FAILED
# → testValidBid_AuctionClosed FAILED
```

→ Quốc Minh biết ngay code mình sửa làm hỏng cái gì, sửa lại trước khi push.

**Lợi ích trong tuần 1:**
1. **Confidence:** Refactor code mà không sợ làm hỏng
2. **Documentation:** Test case chính là ví dụ cách dùng class
3. **Barem điểm:** 0.5đ từ Unit Test JUnit

#### 💡 Cấu trúc AAA (Arrange-Act-Assert)

Mỗi test nên có 3 phần rõ ràng:

```java
@Test
@DisplayName("Chia cho 0 phải ném ra ArithmeticException")
void testDivide_ByZero_ThrowsArithmeticException() {
    // Arrange: Chuẩn bị dữ liệu
    Calculator calc = new Calculator();
    
    // Act & Assert: Thực hiện và kiểm tra
    assertThrows(ArithmeticException.class, () -> {
        calc.divide(10, 0);
    });
}
```

**Tại sao cần @DisplayName tiếng Việt?**
- Người review đọc test report: "Tests run: 15, Failures: 2"
- Nhìn vào tên: "Chia cho 0 phải ném ra ArithmeticException" → biết ngay test làm gì
- Không cần đọc code bên trong

---

### 2.5. ConfigLoader Pattern (Utility Class)

#### 🎓 Bản chất

ConfigLoader là **utility class** - class chứa các method static tiện ích, KHÔNG cần tạo instance.

**Ví dụ:**
```java
// ❌ Tệ: Cho phép tạo instance
public class ConfigLoader {
    public String getString(String key) { ... }
}
ConfigLoader loader1 = new ConfigLoader(); // Vô nghĩa
ConfigLoader loader2 = new ConfigLoader(); // Tạo thêm object thừa

// ✅ Tốt: Private constructor, chỉ có static methods
public final class ConfigLoader {
    private ConfigLoader() {} // Không cho phép tạo instance
    
    public static String getString(String key) { ... }
}
ConfigLoader.getString("server.port"); // Gọi trực tiếp
```

#### 🔍 Tại sao BidHub cần ConfigLoader?

**Vấn đề thực tế:**

Mỗi class đều cần biết `server.port`, `db.path`...

**Cách tệ:**
```java
// Trong 50 files khác nhau:
String port = "9090"; // Hardcode
String dbPath = "data/bidhub.db"; // Hardcode
```

→ Muốn đổi port từ 9090 → 8080? Sửa 50 files!

**Cách tốt:**
```java
// server.properties (1 file duy nhất)
server.port=9090
db.path=data/bidhub.db

// Trong code:
int port = ConfigLoader.getInt("server.port");
String dbPath = ConfigLoader.getString("db.path");
```

→ Muốn đổi port? Sửa 1 dòng trong `server.properties`.

#### 💡 Tại sao dùng `getResourceAsStream()` thay vì `new File()`?

**Vấn đề:**
```java
// ❌ Tệ
File file = new File("src/main/resources/server.properties");
```

Khi đóng gói thành JAR:
```
bidhub-server.jar
├── com/bidhub/server/ServerApp.class
└── server.properties (nằm bên trong JAR)
```

→ `new File()` tìm file ngoài JAR → KHÔNG TÌM THẤY!

**Giải pháp:**
```java
// ✅ Tốt
InputStream stream = ConfigLoader.class.getResourceAsStream("/server.properties");
```

→ `getResourceAsStream()` tìm file BÊN TRONG JAR → luôn hoạt động.

---

### 2.6. GitHub Actions CI/CD

#### 🎓 Bản chất

CI/CD = Continuous Integration / Continuous Deployment

- **CI (Continuous Integration):** Mỗi lần push code → tự động build + test
- **CD (Continuous Deployment):** Nếu test pass → tự động deploy lên server

**Flow:**
```
Developer push code
  ↓
GitHub trigger Actions
  ↓
Actions chạy: mvn clean test
  ↓
Nếu test PASS → ✅ badge xanh
Nếu test FAIL → ❌ badge đỏ, gửi email cảnh báo
```

#### 🔍 Tại sao BidHub cần CI từ tuần 1?

**Tình huống thực tế:**

Tuần 5, Đăng push code lên `develop`. Code chạy tốt trên máy Đăng (Windows), nhưng:
- Máy Quốc Minh (Mac) → không compile được
- Máy Khoa (Linux) → test fail

→ Mất nửa ngày để debug.

**Với CI:**
```yaml
# .github/workflows/ci.yml
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest # Test trên Linux
    - run: mvn clean test
```

→ GitHub tự động test trên Ubuntu → biết ngay code có vấn đề trước khi merge.

#### 💡 Cache Maven dependencies

**Vấn đề:**
Mỗi lần CI chạy, Maven phải download lại tất cả dependencies (Jackson, SQLite...) → tốn 2-3 phút.

**Giải pháp:**
```yaml
- uses: actions/cache@v3
  with:
    path: ~/.m2
    key: maven-${{ hashFiles('**/pom.xml') }}
```

→ Lần đầu download hết dependencies → lưu vào cache
→ Lần sau: `pom.xml` không đổi → dùng cache → CI chạy nhanh hơn 10 lần

---

## 3. Phân tích cấu trúc thư mục & tổ chức code

### 3.1. Cây thư mục tổng thể

```
BidHub/                           ← Repository root
├── .github/
│   └── workflows/
│       └── ci.yml                ← GitHub Actions configuration
├── .gitignore                    ← Loại trừ target/, .idea/, *.db
├── pom.xml                       ← Parent POM
├── README.md
├── bidhub-server/
│   ├── pom.xml                   ← Server module POM
│   └── src/
│       ├── main/
│       │   ├── java/com/bidhub/server/
│       │   │   ├── ServerApp.java          ← Entry point
│       │   │   └── config/
│       │   │       └── ConfigLoader.java
│       │   └── resources/
│       │       └── server.properties
│       └── test/java/com/bidhub/server/
│           └── ServerAppTest.java
└── bidhub-client/
    ├── pom.xml                   ← Client module POM
    └── src/
        ├── main/
        │   ├── java/com/bidhub/client/
        │   │   ├── BidHubApp.java          ← JavaFX main
        │   │   └── controller/
        │   │       └── LoginController.java
        │   └── resources/
        │       ├── client.properties
        │       └── fxml/
        │           └── LoginView.fxml
        └── test/java/com/bidhub/client/
```

### 3.2. Tại sao package phải là `com.bidhub.server` thay vì `server`?

**Convention trong Java:**
- Package name phải là domain ngược: `com.bidhub` (bidhub.com)
- Tránh conflict: Nếu dùng package `server`, có thể trùng với library khác cũng có package `server`

**Ví dụ:**
```java
// ❌ Tệ
package server;
import server.ServerApp; // Trùng với java.rmi.server.ServerApp

// ✅ Tốt
package com.bidhub.server;
import com.bidhub.server.ServerApp; // Unique, không trùng ai
```

### 3.3. Tại sao `.gitignore` phải loại `target/`?

**Vấn đề:**
```
target/                   ← Maven compile output
├── classes/              ← .class files
└── bidhub-server-1.0.jar
```

Nếu commit `target/`:
- File JAR 50MB → push lên GitHub mất 10 phút
- Mỗi người build ra file JAR khác nhau → conflict liên tục
- Git repo nặng 500MB sau 1 tháng

**Giải pháp:**
```gitignore
target/
*.class
*.jar
*.db
.idea/
*.iml
```

→ Chỉ commit **source code**, không commit **build output**.

---

## 4. Giải thích từng class / thành phần chính

### 4.1. `ConfigLoader.java` (Đăng)

#### 📌 Trách nhiệm duy nhất

Đọc file `server.properties` và cung cấp giá trị config cho toàn bộ ứng dụng.

**Single Responsibility Principle (SRP):** 1 class chỉ làm 1 việc. ConfigLoader chỉ lo đọc config, không lo về database, không lo về networking.

#### 📌 Tại sao là `final class`?

```java
public final class ConfigLoader {
    private ConfigLoader() {} // Private constructor
}
```

**Lý do:**
- `final` → không cho class khác extends
- `private constructor` → không cho tạo instance

**Vì sao cần điều này?**

Utility class không có state (không có field instance), chỉ có static methods. Cho phép extends hoặc tạo instance là vô nghĩa:

```java
// ❌ Tệ: Nếu không có final + private constructor
class MyConfigLoader extends ConfigLoader {
    // Vô nghĩa, chẳng có gì để override
}
ConfigLoader loader = new ConfigLoader(); // Vô nghĩa, chẳng cần instance
```

#### 📌 Các method chính

```java
public static String getString(String key)
public static int getInt(String key)
public static String getOrDefault(String key, String defaultValue)
```

**Tại sao cần `getOrDefault()`?**

```java
// Tình huống: file properties thiếu key "admin.email"

// ❌ Tệ
String email = ConfigLoader.getString("admin.email");
// → Trả về null → NullPointerException ở dòng sau

// ✅ Tốt
String email = ConfigLoader.getOrDefault("admin.email", "admin@bidhub.com");
// → Nếu không có key → dùng default → app vẫn chạy
```

#### 📌 Implementation chi tiết

```java
public final class ConfigLoader {
    private static final Properties props = new Properties();
    
    static {
        try (InputStream input = ConfigLoader.class
                .getResourceAsStream("/server.properties")) {
            if (input == null) {
                throw new RuntimeException("Không tìm thấy server.properties");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc config file", e);
        }
    }
    
    private ConfigLoader() {}
    
    public static String getString(String key) {
        return props.getProperty(key);
    }
    
    public static int getInt(String key) {
        return Integer.parseInt(props.getProperty(key));
    }
}
```

**Giải thích:**
1. `static { }` block: Chạy 1 lần duy nhất khi class được load → đọc file properties
2. `try-with-resources`: Tự động đóng InputStream sau khi dùng xong
3. `getResourceAsStream("/server.properties")`: Tìm file trong classpath (bên trong JAR)

---

### 4.2. `BidHubApp.java` (Công Minh)

#### 📌 Trách nhiệm

Entry point của JavaFX client. Khởi tạo Stage, load FXML, hiển thị cửa sổ.

#### 📌 Code chi tiết

```java
public class BidHubApp extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/LoginView.fxml")
        );
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1024, 720);
        primaryStage.setTitle("BidHub — Hệ thống đấu giá trực tuyến");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
```

**Tại sao phải extends `Application`?**

JavaFX yêu cầu main class extends `Application` và override `start()`. Framework sẽ:
1. Gọi `main()`
2. Khởi tạo JavaFX runtime
3. Gọi `start(Stage primaryStage)` với Stage đã tạo sẵn

#### 📌 Tại sao `getResource()` cần `/` đầu tiên?

```java
// ✅ Tốt
getClass().getResource("/fxml/LoginView.fxml")
// → Tìm từ classpath root: src/main/resources/fxml/LoginView.fxml

// ❌ Tệ
getClass().getResource("fxml/LoginView.fxml")
// → Tìm relative từ package hiện tại: com/bidhub/client/fxml/LoginView.fxml
// → KHÔNG TÌM THẤY
```

---

### 4.3. `LoginController.java` (Công Minh)

#### 📌 Trách nhiệm

Xử lý logic của LoginView: validate input, disable/enable button, hiển thị lỗi.

#### 📌 Code chi tiết

```java
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    
    @FXML
    public void initialize() {
        // Ẩn error label ban đầu
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        
        // Disable button khi username/password rỗng
        loginButton.disableProperty().bind(
            Bindings.isEmpty(usernameField.textProperty())
                .or(Bindings.isEmpty(passwordField.textProperty()))
        );
    }
    
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.length() < 3) {
            showError("Username phải có ít nhất 3 ký tự");
            return;
        }
        
        // TODO: Gọi server để xác thực (tuần 5)
        System.out.println("Login: " + username);
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
```

#### 📌 Tại sao `setManaged(false)` khi ẩn label?

**2 cách ẩn element trong JavaFX:**

1. **Chỉ `setVisible(false)`:**
   - Element không hiển thị
   - Nhưng vẫn chiếm chỗ trong layout
   - Có khoảng trống thừa

2. **`setVisible(false)` + `setManaged(false)`:**
   - Element không hiển thị
   - KHÔNG chiếm chỗ trong layout
   - Layout tự động co lại

**Ví dụ:**
```
[Username field]
[Password field]
[            ]  ← Khoảng trống thừa nếu chỉ setVisible(false)
[Login button]

VS

[Username field]
[Password field]
[Login button]  ← Không có khoảng trống nếu setManaged(false)
```

---

### 4.4. `CalculatorTest.java` (Khoa)

#### 📌 Trách nhiệm

Test class `Calculator` với ≥ 15 test cases, coverage mọi edge case.

#### 📌 Nested Test Classes

```java
@DisplayName("Calculator Test Suite")
class CalculatorTest {
    
    @Nested
    @DisplayName("Phép chia")
    class DivideTests {
        @Test
        @DisplayName("Chia bình thường: 10 / 2 = 5")
        void testDivide_Normal() {
            // Arrange
            Calculator calc = new Calculator();
            
            // Act
            int result = calc.divide(10, 2);
            
            // Assert
            assertEquals(5, result);
        }
        
        @Test
        @DisplayName("Chia cho 0 phải ném ArithmeticException")
        void testDivide_ByZero_ThrowsException() {
            Calculator calc = new Calculator();
            
            assertThrows(ArithmeticException.class, () -> {
                calc.divide(10, 0);
            });
        }
    }
    
    @Nested
    @DisplayName("Phép nhân")
    class MultiplyTests {
        // ...
    }
}
```

**Lợi ích `@Nested`:**
- Tổ chức test thành nhóm logic
- Dễ tìm: Tất cả test về phép chia ở 1 chỗ
- Report rõ ràng: "DivideTests → testDivide_ByZero → PASSED"

---

### 4.5. GitHub Actions Workflow (Quốc Minh)

#### 📌 File `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop, main]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2
    
    - name: Build with Maven
      run: mvn clean install
    
    - name: Run tests
      run: mvn test
```

**Giải thích:**
1. `on: push`: Trigger khi push vào `develop` hoặc `main`
2. `runs-on: ubuntu-latest`: Chạy trên máy ảo Ubuntu
3. `actions/checkout@v3`: Clone code từ GitHub
4. `actions/setup-java@v3`: Cài JDK 21
5. `actions/cache@v3`: Cache `.m2` folder (Maven dependencies)
6. `mvn clean install`: Build toàn bộ project
7. `mvn test`: Chạy tất cả test

---

## 5. Flow xử lý nghiệp vụ chính trong tuần 1

### 5.1. Flow: Developer push code → CI chạy

```
1. Đăng sửa ConfigLoader.java
   ↓
2. git add + git commit -m "feat: thêm method getOrDefault"
   ↓
3. git push origin feature/tuan-1-dang-maven-setup
   ↓
4. GitHub nhận push → trigger Actions
   ↓
5. Actions:
   - Clone code
   - Setup JDK 21
   - Restore Maven cache
   - mvn clean install
   - mvn test
   ↓
6. Nếu test PASS:
   - Badge ✅ xanh
   - Email thông báo thành công
   
   Nếu test FAIL:
   - Badge ❌ đỏ
   - Email thông báo lỗi
   - Hiển thị log lỗi
```

### 5.2. Flow: Compile và chạy project

```
1. mvn clean
   → Xóa thư mục target/
   
2. mvn compile
   → Compile code trong src/main/java/
   → Output: target/classes/
   
3. mvn test-compile
   → Compile code trong src/test/java/
   → Output: target/test-classes/
   
4. mvn test
   → Chạy tất cả test cases
   → Tạo report: target/surefire-reports/
   
5. mvn package
   → Đóng gói thành JAR
   → Output: target/bidhub-server-1.0-SNAPSHOT.jar
   
6. java -jar target/bidhub-server-1.0-SNAPSHOT.jar
   → Chạy ứng dụng
```

### 5.3. Flow: JavaFX load FXML và hiển thị UI

```
1. BidHubApp.main(args)
   ↓
2. JavaFX framework gọi start(Stage primaryStage)
   ↓
3. FXMLLoader.load("/fxml/LoginView.fxml")
   ↓
4. FXML parser đọc XML:
   - Tạo TextField với fx:id="usernameField"
   - Tạo PasswordField với fx:id="passwordField"
   - Tạo Button với onAction="#handleLogin"
   ↓
5. FXML inject các Node vào LoginController:
   - Tìm field có @FXML và tên = fx:id
   - usernameField = TextField object
   ↓
6. Gọi LoginController.initialize()
   ↓
7. Tạo Scene với root Node từ FXML
   ↓
8. primaryStage.setScene(scene)
   ↓
9. primaryStage.show() → Hiển thị cửa sổ
```

---

## 6. Các quyết định thiết kế quan trọng

### 6.1. Tại sao ConfigLoader là `final class` với `private constructor`?

**Lý do:**

1. **Utility class không cần instance:**
   - ConfigLoader chỉ cung cấp static methods
   - Tạo instance `new ConfigLoader()` là vô nghĩa

2. **Ngăn extends:**
   - Nếu cho phép extends → ai đó có thể override behavior
   - Utility class phải predictable: gọi `getString()` → luôn trả về giá trị từ properties

**Ví dụ sai:**
```java
// ❌ Nếu không có final + private constructor
class HackedConfigLoader extends ConfigLoader {
    @Override
    public static String getString(String key) {
        return "hacked"; // Luôn trả về "hacked"
    }
}
```

### 6.2. Tại sao dùng `getResourceAsStream()` thay vì `new FileInputStream()`?

**Vấn đề với `new FileInputStream()`:**

```java
// ❌ Tệ
FileInputStream fis = new FileInputStream("src/main/resources/server.properties");
```

**3 lý do thất bại:**

1. **Relative path không nhất quán:**
   - Chạy từ IntelliJ: working directory = project root → OK
   - Chạy từ JAR: working directory = /home/user → FAIL

2. **JAR không có file system:**
   - `server.properties` được đóng gói BÊN TRONG JAR
   - `FileInputStream` chỉ đọc file NGOÀI file system
   - Không thể mở file bên trong JAR bằng File API

3. **Hardcode path:**
   - `src/main/resources/` chỉ tồn tại trong source code
   - Sau khi build, không còn thư mục `src/`

**Giải pháp đúng:**
```java
// ✅ Tốt
InputStream stream = ConfigLoader.class.getResourceAsStream("/server.properties");
```

- `/server.properties` → tìm từ classpath root
- Classpath bao gồm: `target/classes/` (khi dev), bên trong JAR (khi deploy)
- Luôn hoạt động ở mọi môi trường

### 6.3. Tại sao `@FXML` fields phải là `private`?

```java
// ❌ Tệ
@FXML public TextField usernameField;

// ✅ Tốt
@FXML private TextField usernameField;
```

**Lý do:**

1. **Encapsulation:** Field nên private, chỉ class hiện tại truy cập
2. **JavaFX Reflection:** FXMLLoader dùng reflection để inject → không cần public
3. **Ngăn external modification:**

```java
// Nếu field là public:
LoginController controller = loader.getController();
controller.usernameField = null; // Ai đó có thể phá hoại
```

### 6.4. Tại sao `initialize()` không có tham số?

```java
@FXML
public void initialize() {
    // Setup code
}
```

**Lý do:**

- FXMLLoader gọi `initialize()` SAU KHI inject xong tất cả `@FXML` fields
- Không có tham số vì mọi thứ đã sẵn sàng trong fields
- Constructor chạy TRƯỚC khi inject → `usernameField` = null trong constructor

**Timeline:**
```
1. new LoginController()
   → usernameField = null
   
2. FXMLLoader inject fields
   → usernameField = TextField object
   
3. initialize()
   → usernameField != null → có thể dùng
```

### 6.5. Tại sao CI phải cache `~/.m2`?

**Vấn đề:**

Mỗi lần CI chạy:
```
1. Download Jackson 2.17.0 (5 MB)
2. Download SQLite JDBC (8 MB)
3. Download JUnit 5 (2 MB)
...
→ Tổng: 50 MB, mất 2-3 phút
```

**Giải pháp:**
```yaml
- uses: actions/cache@v3
  with:
    path: ~/.m2
    key: maven-${{ hashFiles('**/pom.xml') }}
```

**Cách hoạt động:**
1. **Lần đầu:** Download hết dependencies → lưu vào cache với key = hash của pom.xml
2. **Lần sau:** 
   - `pom.xml` không đổi → hash giống → dùng cache
   - CI chạy nhanh hơn 10 lần (30s thay vì 3 phút)
3. **Khi thêm dependency:** `pom.xml` thay đổi → hash khác → cache mới

---

## 7. Những lỗi thường gặp khi code tuần 1 và cách tránh

### 7.1. Lỗi: `mvn compile` báo "Package does not exist"

**Triệu chứng:**
```
[ERROR] /bidhub-server/src/main/java/com/bidhub/server/ServerApp.java:[3,28] 
package com.fasterxml.jackson does not exist
```

**Nguyên nhân:**
- Quên khai báo dependency trong `pom.xml`
- Hoặc khai báo sai `groupId`/`artifactId`

**Cách fix:**
```xml
<!-- Thêm vào bidhub-server/pom.xml -->
<dependencies>
  <dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
  </dependency>
</dependencies>
```

### 7.2. Lỗi: JavaFX `NullPointerException` trong `initialize()`

**Triệu chứng:**
```
java.lang.NullPointerException: Cannot invoke "javafx.scene.control.TextField.textProperty()"
  at LoginController.initialize(LoginController.java:15)
```

**Nguyên nhân:**
- `@FXML` field name không khớp với `fx:id` trong FXML

**Ví dụ sai:**
```xml
<!-- LoginView.fxml -->
<TextField fx:id="username" />
```
```java
// LoginController.java
@FXML private TextField usernameField; // Tên khác với fx:id
```

**Cách fix:**
```java
// Đổi tên field cho khớp
@FXML private TextField username;
```

### 7.3. Lỗi: Git push bị reject "non-fast-forward"

**Triệu chứng:**
```
error: failed to push some refs to 'github.com:nhom/BidHub.git'
hint: Updates were rejected because the tip of your current branch is behind
```

**Nguyên nhân:**
- Người khác đã push code lên `develop`
- Code của bạn chưa có commit mới nhất

**Cách fix:**
```bash
# Pull code mới nhất
git pull origin develop

# Nếu có conflict:
# 1. Sửa file conflict
# 2. git add .
# 3. git commit -m "fix: merge conflict"

# Push lại
git push origin feature/tuan-1-dang-maven-setup
```

### 7.4. Lỗi: ConfigLoader không tìm thấy file properties

**Triệu chứng:**
```
java.lang.RuntimeException: Không tìm thấy server.properties
```

**Nguyên nhân:**
- File `server.properties` đặt sai chỗ
- Hoặc dùng sai path trong `getResourceAsStream()`

**Cách fix:**
```
Đúng:
src/main/resources/server.properties
                  └─ Ngay trong resources/

Sai:
src/main/resources/config/server.properties
                  └─ Trong subfolder config/
```

```java
// Nếu file ở resources/config/server.properties:
getResourceAsStream("/config/server.properties") // Cần /config/
```

### 7.5. Lỗi: Test chạy nhưng không có kết quả

**Triệu chứng:**
```
mvn test
...
Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
```

**Nguyên nhân:**
- Quên `@Test` annotation
- Hoặc test method là `private`

**Ví dụ sai:**
```java
// ❌ Thiếu @Test
public void testDivide() { ... }

// ❌ Test method là private
@Test
private void testDivide() { ... }
```

**Cách fix:**
```java
// ✅ Phải có @Test và public
@Test
public void testDivide() { ... }
```

### 7.6. Lỗi: CI fail nhưng local build pass

**Nguyên nhân phổ biến:**

1. **Hardcode absolute path:**
```java
// ❌ Chỉ chạy trên máy bạn
File file = new File("C:/Users/Dang/project/data.txt");
```

2. **Phụ thuộc file không commit:**
```java
// ❌ File test-data.csv không có trong Git
File data = new File("test-data.csv");
```

3. **Test phụ thuộc thời gian:**
```java
// ❌ Test này sẽ fail vào 23:59:59 hôm nay
LocalDate today = LocalDate.now();
assertEquals("2026-04-06", today.toString()); // Ngày mai fail!
```

**Cách fix:**
```java
// ✅ Dùng relative path hoặc getResourceAsStream()
InputStream is = getClass().getResourceAsStream("/test-data.csv");

// ✅ Test không phụ thuộc ngày giờ hệ thống
LocalDate fixedDate = LocalDate.of(2026, 4, 6);
```

---

## 8. Tài liệu tham khảo & câu hỏi kiểm tra

### 8.1. Câu hỏi bắt buộc cho Đăng (Maven + ConfigLoader)

1. **`<dependencyManagement>` trong parent POM khác `<dependencies>` thế nào? Không có nó thì sao?**

**Đáp án:** 
- `<dependencyManagement>`: Khai báo version, nhưng KHÔNG tự động thêm dependency vào module con
- `<dependencies>`: Tự động thêm vào tất cả module con

Không có `<dependencyManagement>`:
- Mỗi module phải khai báo version riêng
- Update Jackson 2.17.0 → 2.18.0: phải sửa 5 files `pom.xml`

2. **Tại sao `ConfigLoader` constructor là `private`? Nếu để `public` thì có vấn đề gì?**

**Đáp án:**
- `private constructor`: Không cho tạo instance
- Utility class chỉ có static methods → không cần instance
- Nếu `public`: Ai đó có thể `new ConfigLoader()` → tạo object vô nghĩa, lãng phí memory

3. **Vì sao dùng `getResourceAsStream` thay vì `new File()`? Khi đóng gói JAR thì cái nào chạy được?**

**Đáp án:**
- `new File("src/main/resources/...")`: Tìm file ngoài file system → Khi đóng JAR, file nằm BÊN TRONG JAR → không tìm thấy
- `getResourceAsStream("/server.properties")`: Tìm trong classpath (bao gồm bên trong JAR) → luôn chạy được

---

### 8.2. Câu hỏi bắt buộc cho Quốc Minh (CI/CD)

1. **CI pipeline có mấy bước? Mỗi bước làm gì?**

**Đáp án:**
```
1. Checkout code: Clone repo từ GitHub
2. Setup JDK 21: Cài Java
3. Cache Maven: Restore ~/.m2 từ cache
4. mvn clean install: Build project
5. mvn test: Chạy test
6. Upload test report (nếu có)
```

2. **Tại sao phải cache `~/.m2`? Nếu không cache thì ảnh hưởng gì?**

**Đáp án:**
- `~/.m2`: Thư mục Maven lưu tất cả dependencies
- Không cache: Mỗi lần CI chạy → download lại 50MB dependencies → mất 2-3 phút
- Có cache: Lần 2 trở đi chỉ mất 30s

3. **Conventional Commits: `feat` vs `build` khác nhau thế nào? Ví dụ thực tế?**

**Đáp án:**
- `feat`: Thêm tính năng mới cho người dùng
  - Ví dụ: `feat: thêm nút Đăng xuất`
- `build`: Thay đổi build system, dependencies
  - Ví dụ: `build: update Jackson 2.17.0 → 2.18.0`

---

### 8.3. Câu hỏi bắt buộc cho Công Minh (JavaFX)

1. **`@FXML` annotation làm gì? Kết nối với FXML qua cơ chế nào?**

**Đáp án:**
- `@FXML`: Đánh dấu field/method để FXMLLoader biết cần inject
- Cơ chế: Reflection
  - FXMLLoader đọc FXML, tìm `fx:id="usernameField"`
  - Tìm field trong Controller có `@FXML` và tên = `usernameField`
  - Dùng reflection set giá trị: `field.set(controller, textFieldObject)`

2. **`initialize()` được gọi khi nào? Trước hay sau `@FXML` fields được inject?**

**Đáp án:**
- SAU KHI inject xong fields
- Timeline:
  ```
  1. new Controller() → fields = null
  2. Inject fields → fields != null
  3. initialize() → có thể dùng fields
  ```

3. **Tại sao `setManaged(false)` khi ẩn `errorLabel`? Chỉ `setVisible(false)` thì sao?**

**Đáp án:**
- `setVisible(false)`: Không hiển thị, nhưng VẪN chiếm chỗ trong layout
- `setManaged(false)`: Không chiếm chỗ → layout tự co lại
- Chỉ `setVisible(false)`: Có khoảng trống thừa giữa các element

---

### 8.4. Câu hỏi bắt buộc cho Khoa (JUnit)

1. **Cấu trúc AAA trong test là gì? Tại sao nên tách rõ 3 phần?**

**Đáp án:**
- AAA = Arrange - Act - Assert
  - Arrange: Chuẩn bị dữ liệu (new Calculator())
  - Act: Thực hiện hành động (calc.divide(10, 2))
  - Assert: Kiểm tra kết quả (assertEquals(5, result))
- Tại sao: Dễ đọc, dễ hiểu test đang làm gì

2. **`@BeforeEach` làm gì? Tại sao tạo `new Calculator()` ở đó thay vì trong mỗi test?**

**Đáp án:**
- `@BeforeEach`: Chạy TRƯỚC MỖI test method
- Tạo `new Calculator()` ở đó: Đảm bảo mỗi test có Calculator mới, tránh test này ảnh hưởng test kia
- Nếu tạo trong test: Phải lặp lại code `new Calculator()` 15 lần

3. **`assertThrows` trả về gì? Tại sao dùng nó tốt hơn try-catch trong test?**

**Đáp án:**
- `assertThrows` trả về: Exception object (nếu exception được ném)
- Dùng `assertThrows` tốt hơn vì:
  - Ngắn gọn hơn: 1 dòng thay vì 5 dòng try-catch
  - Rõ ràng hơn: Test sẽ FAIL nếu KHÔNG có exception

---

## 9. Kết luận – Tuần 1 đã đặt nền móng cho những gì ở các tuần sau?

### 9.1. Module structure → Tuần 2-10

**Tuần 1 tạo:**
```
bidhub-parent
├── bidhub-server
└── bidhub-client
```

**Tuần 2-10 sẽ thêm:**
```
bidhub-parent
├── bidhub-common  ← Entity, User, Item (dùng chung)
├── bidhub-server  ← DAO, RequestHandler, AuctionManager
└── bidhub-client  ← Controller, View, NetworkTask
```

→ Nếu tuần 1 setup sai Maven structure → tuần 2 phải refactor lại toàn bộ.

### 9.2. ConfigLoader → Tuần 3-8

**Tuần 1:** Đọc `server.port`, `db.path`

**Tuần 3:** Thêm config cho SQLite:
```properties
db.connection.pool.size=10
db.connection.timeout=5000
```

**Tuần 7:** Thêm config cho ReentrantLock:
```properties
auction.lock.timeout=30
```

**Tuần 8:** Thêm config cho Anti-Sniping:
```properties
snipe.threshold=60
snipe.extension=60
```

→ Tất cả đều dùng `ConfigLoader.getInt()` từ tuần 1 → không cần viết lại logic đọc config.

### 9.3. CI/CD → Tuần 1-10

**Tuần 1:** CI chạy `mvn test` → 15 tests

**Tuần 2:** CI tăng lên 25 tests (thêm Entity tests)

**Tuần 5:** CI tăng lên 40 tests (thêm Auth tests)

**Tuần 9:** CI chạy 60+ tests + coverage report

→ Nếu không setup CI từ tuần 1 → tuần 9 mới setup → phát hiện 20 bugs cũ → không còn thời gian fix.

### 9.4. JavaFX structure → Tuần 5-8

**Tuần 1:** `LoginView` + `LoginController`

**Tuần 5:** Thêm `AuctionListView` + `AuctionListController`

**Tuần 6:** Thêm `AuctionDetailView` + `AuctionDetailController`

**Tuần 8:** Thêm `PriceChartView` (LineChart)

→ Tất cả đều dùng pattern FXML + Controller từ tuần 1 → consistent architecture.

### 9.5. Mở rộng: Thêm module mới

**Giả sử tuần 6 cần thêm `bidhub-common`:**

1. Tạo folder `bidhub-common/`
2. Tạo `bidhub-common/pom.xml`:
```xml
<parent>
  <groupId>com.bidhub</groupId>
  <artifactId>bidhub-parent</artifactId>
  <version>1.0-SNAPSHOT</version>
</parent>
<artifactId>bidhub-common</artifactId>
```

3. Thêm vào parent `pom.xml`:
```xml
<modules>
  <module>bidhub-common</module>
  <module>bidhub-server</module>
  <module>bidhub-client</module>
</modules>
```

4. Server/Client muốn dùng common:
```xml
<dependency>
  <groupId>com.bidhub</groupId>
  <artifactId>bidhub-common</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

→ Nhờ Maven structure từ tuần 1, thêm module mới chỉ mất 5 phút.

---

## 🎓 Tổng kết

Tuần 1 là tuần quan trọng nhất của dự án. Những công việc tưởng chừng như "nhàm chán" (setup Maven, viết `.gitignore`, config CI) thực ra là **nền móng** cho 9 tuần sau.

**Nguyên tắc vàng:**
- ✅ **Setup đúng 1 lần** → không phải sửa lại 10 lần
- ✅ **Hiểu rõ lý do** của mọi quyết định thiết kế → không bị bối rối khi bảo vệ đồ án
- ✅ **Tất cả mọi người** phải hiểu code của người khác → không bị 0 điểm khi giảng viên hỏi vấn đáp

**Checklist cuối tuần 1:**
- [ ] `mvn compile` thành công cả 2 module
- [ ] CI badge hiển thị ✅ xanh
- [ ] `mvn javafx:run -pl bidhub-client` → LoginView hiện ra
- [ ] `mvn test` → ≥ 15 tests pass
- [ ] Mỗi người có thể giải thích code của người khác

Nếu đạt được 5 điều trên → Tuần 1 hoàn thành xuất sắc! 🎉
