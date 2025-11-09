# J2ME MicroEmulator Launcher

[English](README.md) | [Tiếng Việt](README_VI.md)

## Tổng quan

J2ME MicroEmulator Launcher là một ứng dụng desktop được xây dựng bằng Java, cho phép người dùng khởi chạy và quản lý nhiều instances của ứng dụng J2ME (Java 2 Micro Edition) sử dụng MicroEmulator. Nó cung cấp giao diện GUI trực quan để cấu hình, chạy và sắp xếp các cửa sổ emulator, giúp dễ dàng kiểm tra hoặc chạy các ứng dụng Java di động cũ trên máy tính hiện đại.

Dự án này lý tưởng cho lập trình viên, người đam mê hoặc bất kỳ ai làm việc với ứng dụng J2ME cần mô phỏng nhiều thiết bị cùng lúc.

## Tính năng

- **Quản lý Ứng dụng**: Cài đặt và quản lý ứng dụng J2ME với tự động trích xuất tên, icon, nhà phát triển và phiên bản từ manifest của file JAR/JAD.
- **Giao diện Tab**: Tab riêng biệt cho quản lý Ứng dụng và Instances để tổ chức tốt hơn.
- **Hỗ trợ Đa Instances**: Tạo và chạy nhiều instances emulator từ các ứng dụng J2ME đã cài đặt.
- **Lưu trữ Lâu dài**: Ứng dụng đã cài sẽ được lưu vào `~/.j2me_apps.properties` và tồn tại giữa các phiên.
- **Cache Icon**: Icon ứng dụng được tự động trích xuất và cache trong thư mục `~/.j2me_icons/`.
- **Quản lý GUI**: Giao diện thân thiện để thêm, xóa, khởi động, dừng và sắp xếp lại instances.
- **Sắp xếp Cửa sổ Tự động**: Tổ chức các cửa sổ emulator theo lưới để dễ quan sát hơn.
- **Tùy chọn Cấu hình**: Dễ dàng đặt đường dẫn đến file MicroEmulator JAR và chọn từ các ứng dụng đã cài.
- **Theo dõi Trạng thái**: Giám sát trạng thái instances (Created, Starting, Running, Stopped) với giao diện mã màu.
- **Đa Nền tảng**: Chạy trên bất kỳ hệ thống nào có Java (đã kiểm tra trên Windows, macOS, Linux).

## Yêu cầu

- Môi trường Java Runtime Environment (JRE) 8 trở lên.
- File MicroEmulator JAR (tải từ [SourceForge](https://sourceforge.net/projects/microemulator/files/microemulator/2.0.4/) hoặc nguồn chính thức).
- Các file ứng dụng J2ME (.jar hoặc .jad).

## Cài đặt

1. Clone repository:
   ```
   git clone https://github.com/kitakeyos-dev/j2me-microemulator-launcher.git
   ```

2. Xây dựng dự án bằng IDE Java yêu thích (ví dụ: IntelliJ IDEA, Eclipse) hoặc qua dòng lệnh với Maven/Gradle nếu đã cấu hình. (Lưu ý: Code cung cấp là Java thuần; thêm công cụ build nếu cần.)

3. Chạy lớp chính: `me.kitakeyos.j2me.MainApplication`.

Hoặc đóng gói thành JAR và chạy:
```
java -jar j2me-launcher.jar
```

## Hướng dẫn Sử dụng

### Thiết lập Lần đầu

1. **Đặt Đường dẫn MicroEmulator**: Vào Settings (trong tab Instances) và chọn file `microemulator.jar`.

### Quản lý Ứng dụng

2. **Cài đặt Ứng dụng** (tab Applications):
   - Nhấn "Add Application" để chọn file .jar hoặc .jad.
   - Tên, icon, nhà phát triển và phiên bản của ứng dụng được tự động trích xuất từ manifest.
   - Ứng dụng đã cài sẽ được lưu và tồn tại giữa các phiên.
   - Xem tất cả ứng dụng đã cài với chi tiết và icon.
   - Nhấn "Remove" để gỡ bỏ ứng dụng.

### Chạy Instances

3. **Tạo Instances** (tab Instances):
   - Chọn một ứng dụng đã cài từ menu dropdown.
   - Chọn số lượng instances muốn tạo.
   - Nhấn "Create Instances".

4. **Chạy Instances**:
   - Nhấn "Run All" để khởi động tất cả instances đã tạo.
   - Hoặc nhấn nút "Run" riêng lẻ cho từng instance.
   - Cửa sổ emulator sẽ tự động sắp xếp theo lưới.

5. **Quản lý Instances**:
   - Dừng instances riêng lẻ hoặc nhấn "Stop All".
   - Xóa instances không cần thiết.
   - Dùng "Arrange" để tổ chức lại cửa sổ emulator.
   - Di chuyển instances lên/xuống để thay đổi thứ tự.

Để biết cấu trúc code chi tiết, tham khảo các file nguồn trong thư mục `src`.

## Đóng góp

Chào mừng đóng góp! Hãy fork repo và gửi pull request. Đối với thay đổi lớn, mở issue trước để thảo luận.

- Báo lỗi hoặc gợi ý tính năng qua GitHub Issues.
- Đảm bảo code tuân thủ quy ước Java tiêu chuẩn.

## Giấy phép

Dự án này được cấp phép dưới MIT License. Xem file [LICENSE](LICENSE) để biết chi tiết.

## Lời cảm ơn

- Xây dựng sử dụng Swing cho GUI.
- Dựa trên MicroEmulator để mô phỏng J2ME.
- Lấy cảm hứng từ nhu cầu hồi sinh các ứng dụng Java di động cũ.