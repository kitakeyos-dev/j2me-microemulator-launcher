# J2ME MicroEmulator Launcher

[English](README.md) | [Tiếng Việt](README_VI.md)

## Tổng quan

J2ME MicroEmulator Launcher là ứng dụng desktop hiệu năng cao được xây dựng bằng Java, cho phép người dùng khởi chạy và quản lý nhiều instances của ứng dụng J2ME (Java 2 Micro Edition) sử dụng MicroEmulator. Ứng dụng tích hợp công nghệ bytecode instrumentation caching tiên tiến, classloader pre-warming, và quản lý bộ nhớ thông minh để mang lại tốc độ khởi động cực nhanh và tối ưu hóa tài nguyên hệ thống.

Dự án này lý tưởng cho lập trình viên, người đam mê hoặc bất kỳ ai làm việc với ứng dụng J2ME cần mô phỏng nhiều thiết bị cùng lúc với hiệu năng chuyên nghiệp.

## Tính năng Chính

### Quản lý Ứng dụng
- **Cài đặt Thông minh**: Cài đặt và quản lý ứng dụng J2ME với tự động trích xuất tên, icon, nhà phát triển và phiên bản từ manifest của file JAR/JAD
- **Clone File**: File JAR/JAD được tự động sao chép vào thư mục dữ liệu, tránh mất dữ liệu nếu file gốc bị xóa
- **Cache Icon**: Icon ứng dụng được tự động trích xuất và cache cục bộ để hiển thị tức thì

### Hỗ trợ Đa Instance
- **Không giới hạn Instance**: Tạo và chạy không giới hạn instances emulator từ các ứng dụng J2ME đã cài
- **Giao diện Tab**: Hai tab chuyên biệt cho quản lý Applications và Instances
- **Cấu hình Kích thước Màn hình**: Đặt kích thước tùy chỉnh (chiều rộng: 128-800px, chiều cao: 128-1000px) cho từng nhóm instance với mặc định 240x320
- **Tự động Sắp xếp**: Instances đang chạy được tự động sắp xếp theo ID trong layout wrap responsive
- **Đồng bộ Input**: Tùy chọn đồng bộ click chuột và phím nhấn giữa tất cả instances đang chạy để test song song
- **Điều khiển Độc lập**: Khởi động, dừng và quản lý từng instance một cách riêng biệt với điều khiển chuyên dụng

### Tối ưu hóa Hiệu năng ⚡
- **Cache Bytecode**: Các class đã instrument được cache và chia sẻ giữa các instances, loại bỏ xử lý trùng lặp
- **Pre-warming ClassLoader**: Các class emulator được pre-load khi khởi động để launch instance đầu tiên cực nhanh
- **ThreadLocal Context**: Dynamic instance ID injection cho phép chia sẻ bytecode mà không xung đột bộ nhớ
- **Quản lý Bộ nhớ Thông minh**: Cleanup tài nguyên đúng cách và garbage collection ngăn chặn memory leak

### Trải nghiệm Người dùng
- **Lưu trữ Di động**: Tất cả cấu hình và dữ liệu được lưu trong thư mục `./data/` cục bộ
- **UI Mượt mà**: Các thao tác background đảm bảo giao diện không bao giờ bị đơ, với wrap layout tự động điều chỉnh khi thay đổi kích thước cửa sổ
- **Tự động Cleanup**: Instances được dispose đúng cách khi dừng, giải phóng tài nguyên hệ thống
- **Thông báo Toast**: Thông báo không xâm phạm cho các thao tác như tạo instance và bật/tắt đồng bộ
- **Đa Nền tảng**: Chạy mượt mà trên Windows, macOS và Linux

### Lua Scripting 📜
- **Tự động hóa**: Viết script Lua để tự động hóa các tương tác với instances đang chạy
- **Trình soạn thảo tích hợp**: Editor code tích hợp sẵn với tô màu cú pháp, undo/redo và lưu trạng thái
- **Tổ chức Thư mục**: Sắp xếp script vào các thư mục lồng nhau để quản lý dễ dàng hơn
- **Thực thi theo Ngữ cảnh**: Chạy script trên các instance cụ thể hoặc classloader mặc định

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

## Cấu trúc Thư mục Dữ liệu

Launcher lưu tất cả dữ liệu trong thư mục `./data/` cục bộ:

```
./data/
├── j2me_launcher.properties  # Cấu hình MicroEmulator
├── j2me_apps.properties      # Danh sách ứng dụng đã cài
├── apps/                      # File JAR/JAD đã clone
│   ├── <app-id>.jar
│   └── <app-id>.jad
├── icons/                     # Icon ứng dụng đã trích xuất
│   └── <app-id>.png
└── rms/                       # Record Management System (dữ liệu theo instance)
    ├── 1/                     # Dữ liệu của Instance #1
    ├── 2/                     # Dữ liệu của Instance #2
    └── ...
```

Cấu trúc này đảm bảo:
- **Di động**: Di chuyển toàn bộ thư mục ứng dụng đi bất cứ đâu
- **An toàn**: File gốc có thể bị xóa mà không ảnh hưởng đến apps đã cài
- **Cô lập**: Mỗi instance có thư mục dữ liệu riêng
- **Tổ chức**: Tất cả dữ liệu trong một vị trí sạch sẽ, dễ quản lý

## Hướng dẫn Sử dụng

### Thiết lập Lần đầu

1. **Đặt Đường dẫn MicroEmulator**:
   - Vào Settings (icon ⚙️ trong tab Instances)
   - Chọn file `microemulator.jar` của bạn
   - Classloader sẽ tự động pre-warm các class quan trọng để khởi động nhanh hơn

### Quản lý Ứng dụng

2. **Cài đặt Ứng dụng** (tab Applications):
   - Nhấn "Add Application" để chọn file .jar hoặc .jad
   - Tên, icon, nhà phát triển và phiên bản được tự động trích xuất từ manifest
   - Ứng dụng đã cài sẽ được lưu và tồn tại giữa các phiên
   - Xem tất cả ứng dụng đã cài với chi tiết và icon
   - Nhấn "Remove" để gỡ bỏ ứng dụng

### Tạo và Chạy Instances

3. **Cấu hình và Tạo Instances** (tab Instances):
   - Chọn một ứng dụng đã cài từ menu dropdown
   - Chọn số lượng instances muốn tạo (1-100)
   - Đặt kích thước màn hình tùy chỉnh:
     - Chiều rộng: 128-800 pixels (mặc định: 240)
     - Chiều cao: 128-1000 pixels (mặc định: 320)
   - Nhấn "Create & Run" để tạo và tự động khởi động instances
   - Instances xuất hiện trong panel running instances bên dưới theo thứ tự đã sắp xếp

4. **Bật Đồng bộ Input** (tùy chọn):
   - Check "Sync Mouse & Keyboard Input" để bật test song song
   - Khi bật, bất kỳ click chuột hoặc phím nhấn nào trên một instance sẽ được nhân bản sang tất cả instances khác đang chạy
   - Hữu ích để test cùng một tương tác trên nhiều cấu hình thiết bị khác nhau cùng lúc
   - Bỏ check để tắt đồng bộ và điều khiển instances độc lập

5. **Xem Running Instances**:
   - Tất cả instances đang chạy được hiển thị theo thứ tự đã sắp xếp (1, 2, 3...) trong layout wrap responsive
   - Mỗi instance hiển thị ID với nút "Stop" chuyên dụng
   - Layout tự động điều chỉnh khi thay đổi kích thước cửa sổ
   - Instances tự động wrap để lấp đầy không gian ngang hiệu quả

6. **Quản lý Instances**:
   - Dừng instances riêng lẻ bằng nút "Stop" trên mỗi instance
   - Nhấn "Stop All" để dừng tất cả instances đang chạy cùng lúc
   - Khi dừng, instances được dispose đúng cách và tất cả tài nguyên được giải phóng

### Lua Scripting

7. **Quản lý Script** (tab Scripts):
   - **Tạo Script**: Nhấn "New Script" để tạo script Lua mới. Bạn có thể tổ chức chúng vào thư mục bằng "New Folder".
   - **Soạn thảo Code**: Sử dụng editor tích hợp với tô màu cú pháp để viết code Lua.
   - **Chạy Script**: Chọn instance mục tiêu (hoặc mặc định) và nhấn "Run" (hoặc Ctrl+R) để thực thi.
   - **Lưu**: Script được tự động lưu trước khi chạy, hoặc dùng Ctrl+S để lưu thủ công.

## Chi tiết Kỹ thuật

### Kiến trúc Hiệu năng

#### Bytecode Instrumentation Caching
- Các class emulator được instrument một lần và cache trong bộ nhớ
- Instance đầu tiên: Instrument và cache các class
- Các instance tiếp theo: Khởi động tức thì sử dụng cached bytecode

#### Quản lý Bộ nhớ
- Instances bị dừng kích hoạt cleanup toàn diện:
  - Dispose JFrame để giải phóng native window resources
  - Clear component hierarchies để phá vỡ circular references
  - Clean ThreadLocal contexts đúng cách
  - Null tất cả object references để garbage collection
- System.gc() được gọi sau cleanup để suggest thu hồi ngay
- Không memory leak ngay cả sau khi chạy/dừng hàng trăm instances

#### Cô lập Instance
- Mỗi instance có `EmulatorClassLoader` riêng để cô lập class
- Mỗi instance có thư mục RMS (Record Management System) riêng
- System calls (System.exit, Config.initMEHomePath) được intercept và route theo instance
- Static fields KHÔNG được chia sẻ giữa các instances (namespace class riêng biệt)

#### Đồng bộ Input
- Service `InputSynchronizer` quản lý việc broadcast event giữa tất cả instances đang chạy
- Mouse và keyboard listeners được attach đệ quy vào tất cả components trong display của mỗi instance
- Event dispatching sử dụng tracking dựa trên `ConcurrentHashMap` để tránh vòng lặp vô hạn
- Chuyển đổi tọa độ đảm bảo mouse events được dispatch đến vị trí tương đối chính xác
- Matching component hierarchy đảm bảo keyboard events nhắm đến component tương ứng
- Tất cả events được dispatch bất đồng bộ qua `SwingUtilities.invokeLater()` để tránh block UI
- Listeners được tự động attach/detach khi instances start/stop

#### Lua Scripting Engine
- **Tích hợp Luaj**: Sử dụng Luaj để thực thi script Lua trong môi trường Java
- **Binding**: Expose các đối tượng Java (như Emulator instances) cho Lua để thao tác trực tiếp
- **An toàn Luồng**: Script chạy trên thread riêng để tránh block UI hoặc emulator core

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