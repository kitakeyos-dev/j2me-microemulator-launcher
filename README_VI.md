# J2ME MicroEmulator Launcher

[English](README.md) | [Tiếng Việt](README_VI.md)

## Giới Thiệu

J2ME MicroEmulator Launcher là ứng dụng desktop mạnh mẽ cho phép chạy và quản lý nhiều instance ứng dụng J2ME (Java 2 Micro Edition) sử dụng MicroEmulator. Được xây dựng với caching bytecode tiên tiến và quản lý bộ nhớ tối ưu để khởi động nhanh và hoạt động mượt mà.

## Tính Năng

### 📱 Quản Lý Ứng Dụng
- Cài đặt thông minh với tự động trích xuất metadata từ JAR/JAD manifests
- Sao chép file để tránh mất dữ liệu
- Cache icon để hiển thị tức thì

### 🖥️ Hỗ Trợ Đa Instance
- Chạy không giới hạn số lượng emulator instances cùng lúc
- Tùy chỉnh kích thước màn hình (rộng 128-800px, cao 128-1000px)
- Đồng bộ input giữa tất cả instances
- Điều khiển từng instance riêng lẻ

### 🌐 Network Monitor
- **Ghi Log Kết Nối**: Theo dõi tất cả kết nối socket với timestamp
- **Chuyển Hướng Host/Port**: Chuyển hướng kết nối từ host:port này sang host:port khác
- **Hỗ Trợ Proxy**: SOCKS và HTTP proxy với xác thực (username/password)
- **Lọc Theo Instance**: Áp dụng rules cho tất cả instances hoặc instance cụ thể
- **Tự Động Lưu**: Rules được tự động lưu và load

### 📜 Lua Scripting
- Tự động hóa tương tác với instances đang chạy
- Editor tích hợp với syntax highlighting
- Tổ chức scripts theo thư mục

### ⚡ Hiệu Năng
- Caching bytecode để khởi động nhanh
- Quản lý bộ nhớ thông minh với cleanup đầy đủ
- Hỗ trợ đa nền tảng (Windows, macOS, Linux)

## Yêu Cầu

- Java Runtime Environment (JRE) 8 trở lên
- File MicroEmulator JAR ([Tải về](https://sourceforge.net/projects/microemulator/files/microemulator/2.0.4/))
- File ứng dụng J2ME (.jar hoặc .jad)

## Cài Đặt

### Từ Release
1. Tải file JAR mới nhất từ [Releases](https://github.com/kitakeyos-dev/j2me-microemulator-launcher/releases)
2. Chạy: `java -jar j2me-microemulator-launcher.jar`

### Từ Source
```bash
git clone https://github.com/kitakeyos-dev/j2me-microemulator-launcher.git
cd j2me-microemulator-launcher
mvn package
java -jar target/j2me-microemulator-launcher-*-jar-with-dependencies.jar
```

## Hướng Dẫn Sử Dụng

### Thiết Lập Lần Đầu
1. Trong tab Instances, click **Browse** để chọn file `microemulator.jar`

### Quản Lý Ứng Dụng
1. Vào tab **Applications**
2. Click **Add Application** để cài file .jar hoặc .jad
3. Ứng dụng được tự động lưu và còn lại sau khi khởi động lại

### Chạy Instances
1. Vào tab **Instances**
2. Chọn ứng dụng và số lượng instances
3. Cấu hình kích thước màn hình (mặc định: 240x320)
4. Click **Create & Run**

### Network Monitor
1. Click nút **Network Monitor** trong tab Instances
2. **Connection Logs**: Xem tất cả kết nối
3. **Redirection Rules**: Thêm rules chuyển hướng host:port
4. **Proxy Rules**: Cấu hình SOCKS/HTTP proxy với xác thực tùy chọn
5. Rules được tự động lưu và load khi khởi động

## Thư Mục Dữ Liệu

```
./data/
├── j2me_launcher.properties    # Cấu hình chính
├── j2me_apps.properties        # Danh sách ứng dụng
├── network_rules.properties    # Rules redirection/proxy
├── apps/                       # Files JAR/JAD đã sao chép
├── icons/                      # Icons ứng dụng
├── rms/                        # Dữ liệu theo instance
└── scripts/                    # Scripts Lua
```

## Giấy Phép

MIT License - xem file [LICENSE](LICENSE).

## Lời Cảm Ơn

- Xây dựng bằng Java Swing cho GUI
- Sử dụng MicroEmulator cho J2ME emulation
- Lấy cảm hứng từ nhu cầu hồi sinh các ứng dụng Java mobile cũ