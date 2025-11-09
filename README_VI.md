# J2ME MicroEmulator Launcher

[English](README.md) | [Tiếng Việt](README_VI.md)

## Tổng quan

J2ME MicroEmulator Launcher là một ứng dụng desktop được xây dựng bằng Java, cho phép người dùng khởi chạy và quản lý nhiều instances của ứng dụng J2ME (Java 2 Micro Edition) sử dụng MicroEmulator. Nó cung cấp giao diện GUI trực quan để cấu hình, chạy và sắp xếp các cửa sổ emulator, giúp dễ dàng kiểm tra hoặc chạy các ứng dụng Java di động cũ trên máy tính hiện đại.

Dự án này lý tưởng cho lập trình viên, người đam mê hoặc bất kỳ ai làm việc với ứng dụng J2ME cần mô phỏng nhiều thiết bị cùng lúc.

## Tính năng

- **Hỗ trợ Đa Instances**: Tạo và chạy nhiều instances emulator từ một file J2ME JAR hoặc JAD duy nhất.
- **Quản lý GUI**: Giao diện thân thiện để thêm, xóa, khởi động, dừng và sắp xếp lại instances.
- **Sắp xếp Cửa sổ Tự động**: Tổ chức các cửa sổ emulator theo lưới để dễ quan sát hơn.
- **Tùy chọn Cấu hình**: Dễ dàng đặt đường dẫn đến file MicroEmulator JAR và chọn file J2ME.
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

1. **Đặt Đường dẫn MicroEmulator**: Vào Settings và chọn file `microemulator.jar`.
2. **Chọn File J2ME**: Chọn file .jar hoặc .jad.
3. **Tạo Instances**: Chọn số lượng instances và nhấn "Create Instances".
4. **Chạy Instances**: Nhấn "Run All" hoặc nút "Run" riêng lẻ.
5. **Quản lý Instances**: Dừng, xóa hoặc sắp xếp lại cửa sổ khi cần.

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