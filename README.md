# Navitel PiP Restarter

Ứng dụng Android native (Kotlin + Jetpack Compose) dành cho Android box ô tô chạy launcher LECO Auto với hệ thống hiển thị Picture-in-Picture (PiP) hai màn hình động:
- **PiP 1**: `com.github.slashmax.aabrowser` (YouTube Car / Trình duyệt)
- **PiP 2**: `com.navitel/.app.MainActivity` (Bản đồ dẫn đường Navitel)

---

## 1. Vấn đề giải quyết

Khi Android box trên ô tô khởi động từ trạng thái tắt nguồn (Cold Boot), Navitel có thể khởi chạy trước khi kết nối Wi-Fi/4G/Internet sẵn sàng, dẫn đến việc bị treo cứng ở màn hình logo loading.

Khi mạng Internet kết nối trở lại:
1. Chờ kết nối Internet thực sự sẵn sàng (`NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`).
2. Chờ thêm **8 giây** để đường truyền ổn định và YouTube Car (PiP 1) hoàn tất reload dữ liệu.
3. Dùng quyền ADB shell đặc quyền thực thi `dumpsys activity activities`.
4. Dò tìm chính xác **Display ID** động hiện đang chứa `com.navitel/.app.MainActivity` hoặc `com.navitel/com.navitel.app.MainActivity` (ví dụ Display #21 hoặc Display #23).
5. **Không bao giờ force-stop nếu chưa tìm thấy Display ID** (ngăn chặn tình trạng đóng Navitel mà không thể mở lại đúng chỗ).
6. Nếu tìm thấy Display ID, thực thi:
   ```bash
   am force-stop com.navitel
   ```
7. Chờ 2 giây để hệ thống dọn dẹp task cũ.
8. Khởi chạy lại Navitel trên đúng secondary display:
   ```bash
   am start --display DISPLAY_ID -W -f 0x10000000 -n com.navitel/.app.MainActivity
   ```
9. Xác nhận kết quả có `Status: ok` và ưu tiên kiểm tra `LaunchState: COLD`.
10. Chỉ thực hiện một lần duy nhất cho mỗi phiên khởi động thiết bị, không lặp lại khi Wi-Fi chập chờn.

---

## 2. Minh bạch về quyền hạn (Privileged Shell Architecture)

> **CẢNH BÁO QUAN TRỌNG:**
> Ứng dụng Android thông thường (chạy dưới UID người dùng thông thường `u0_aXXX`) **KHÔNG THỂ** chạy lệnh `am force-stop` và **KHÔNG THỂ** khởi chạy Activity trên display phụ (`--display N`).
> Ứng dụng này **KHÔNG BAO GIỜ** giả vờ rằng `Runtime.exec()` dưới quyền ứng dụng thường có thể thực thi các lệnh trên. Ứng dụng cũng **KHÔNG YÊU CẦU ROOT** thiết bị.

Để thực thi an toàn, ứng dụng sử dụng kiến trúc trừu tượng `PrivilegedCommandExecutor` với 3 chế độ:

1. **Chế độ ADB Shell đặc quyền (`AdbPrivilegedCommandExecutor`)**:
   - Dành cho thiết bị thật (Android box ô tô).
   - Kết nối tới ADB daemon cục bộ qua địa chỉ `127.0.0.1` (port 5555 hoặc port Wireless Debugging).
   - Kiểm tra lệnh `id`: chỉ xác nhận sẵn sàng khi kết quả trả về `uid=2000(shell)`.
   - Lưu trữ cặp khóa xác thực RSA 2048-bit an toàn trong bộ nhớ riêng tư của ứng dụng (`context.filesDir/adb_keys`), không tải bất kỳ thông tin nào lên Internet.

2. **Chế độ kiểm tra an toàn (`UnprivilegedTestExecutor`)**:
   - Dành cho trường hợp chưa kết nối ADB.
   - Hiển thị rõ ràng trạng thái "Chưa có quyền ADB shell".
   - Hiển thị các lệnh dự kiến sẽ chạy nhưng **tuyệt đối không thực thi**.
   - Không force-stop Navitel, không làm crash ứng dụng.

3. **Chế độ giả lập an toàn (`FakePrivilegedCommandExecutor`)**:
   - Dùng để kiểm thử toàn diện trên trình giả lập Google AI Studio và JVM.
   - Mô phỏng dữ liệu `dumpsys` thực tế với Display #21 và Display #23.
   - Cho phép kiểm tra toàn bộ luồng giao diện, thuật toán dò display và phản hồi lệnh `am`.

---

## 3. Danh sách các tính năng KHÔNG THỂ hoạt động nếu chưa có ADB Shell

Nếu chưa kết nối ADB Shell (UID 2000):
- ❌ **Không thể buộc dừng (`force-stop`) Navitel**: Hệ thống Android sẽ trả về lỗi `SecurityException: Permission Denial: am force-stop requires android.permission.FORCE_STOP_PACKAGES`.
- ❌ **Không thể mở Activity trên display phụ (`--display N`)**: Cần quyền `INTERNAL_SYSTEM_WINDOW` hoặc shell UID.
- ❌ **Không thể đọc PID của ứng dụng khác qua `pidof`**: Bị chặn bởi cơ chế SELinux `/proc` isolation.
- ✅ **Ứng dụng vẫn hoạt động bình thường ở chế độ quan sát**: Theo dõi mạng, hiển thị thông báo, ghi log cảnh báo và hướng dẫn người dùng kết nối ADB.

---

## 4. Hướng dẫn thiết lập ADB Privileged Bridge trên Android Box Ô Tô

### Bước 1: Kích hoạt Tùy chọn nhà phát triển
1. Mở **Cài đặt Android** trên màn hình Android box.
2. Chọn **Giới thiệu thiết bị** (About device / About car).
3. Nhấp liên tục 7 lần vào dòng **Số phiên bản** (Build number) cho đến khi xuất hiện thông báo *"Bạn đã là nhà phát triển"*.

### Bước 2: Kích hoạt Gỡ lỗi không dây (Wireless Debugging) hoặc Gỡ lỗi USB
1. Quay lại menu Cài đặt -> chọn **Tùy chọn nhà phát triển** (Developer options).
2. Bật công tắc **Gỡ lỗi USB** (USB Debugging) và **Gỡ lỗi không dây** (Wireless Debugging).
3. Đảm bảo cổng kết nối cục bộ (thường là `5555` hoặc hiển thị trong mục Gỡ lỗi không dây).

### Bước 3: Kết nối trong ứng dụng
1. Mở ứng dụng **Navitel PiP Restarter**.
2. Nhấp vào nút **ADB** ở góc trên thanh công cụ.
3. Điền địa chỉ `127.0.0.1` và Port (mặc định `5555`).
4. Nhấp nút **Kết nối ADB**.
5. Nhấp nút **Chạy lệnh 'id'** để kiểm tra: Nếu kết quả hiện `uid=2000(shell)` thì quá trình thiết lập đã thành công!

---

## 5. Hướng dẫn Build và Cài đặt

### Yêu cầu môi trường
- Android SDK 36 (tương thích ngược đến Android 7.0 / API 24).
- Gradle 9.x và Kotlin 2.2+.

### Build APK Release / Debug
```bash
# Build APK Debug
gradle :app:assembleDebug

# File APK tạo ra tại:
# app/build/outputs/apk/debug/app-debug.apk
```

### Cài đặt lên Android Box qua ADB máy tính
```bash
adb connect <IP_CỦA_ANDROID_BOX>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Chạy Unit Test kiểm thử thuật toán NavitelDisplayDetector
```bash
gradle :app:testDebugUnitTest
```

### Tự động Build & Xuất bản APK bằng GitHub Actions
Dự án đã tích hợp sẵn workflow `.github/workflows/publish-apk.yml`:
1. **Tạo Release tự động khi gắn tag**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   GitHub Actions sẽ tự động biên dịch cả 2 bản `NavitelPiPRestarter-release.apk` và `NavitelPiPRestarter-debug.apk`, sau đó xuất bản trực tiếp lên mục **Releases** của repository.
2. **Kích hoạt thủ công từ giao diện GitHub**:
   - Vào tab **Actions** trên GitHub.
   - Chọn workflow **Build & Publish APK**.
   - Bấm **Run workflow** (có thể chọn tạo Release và nhập tag tùy ý).
3. **Tải file APK từ Artifacts**:
   - Sau mỗi lần push lên nhánh `main` hoặc `master`, file APK cũng được lưu trong mục **Artifacts** của lần chạy đó (thời hạn lưu trữ 30 ngày).

---

## 6. Cam kết bảo vệ an toàn cho hệ thống xe ô tô
- **Tuyệt đối không can thiệp ứng dụng khác**: Chỉ thao tác duy nhất với package `com.navitel`.
- **Không điều khiển hoặc khởi động lại YouTube Car / PiP 1** (`com.github.slashmax.aabrowser`).
- **Không xóa dữ liệu**: Tuyệt đối không dùng `pm clear` hay lệnh xóa bộ nhớ đệm gây mất bản đồ offline.
- **Không can thiệp cấu hình launcher**: Giữ nguyên launcher LECO Auto mặc định của xe.
- **Hộp thoại xác nhận an toàn**: Cảnh báo trước khi đóng Navitel thủ công để tránh gián đoạn lộ trình đang dẫn đường.
- **Không bao giờ crash**: Mọi lỗi mạng, I/O, ngoại lệ ADB đều được bắt và hiển thị ra màn hình nhật ký trực quan.
