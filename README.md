# ĐỒ ÁN LẬP TRÌNH: ỨNG DỤNG Y TẾ TỪ XA (TELEMEDICINE APP)

## 1. THÔNG TIN ĐỒ ÁN

* **Tên đề tài:** Xây dựng ứng dụng đặt lịch khám và quản lý hồ sơ y tế từ xa (Telemedicine App) trên nền tảng Android.
* **Môn học:** Lập trình di động (Android)
* **Giảng viên hướng dẫn:** ThS. Nguyễn Hoàng Hải
* **Trường:** Đại học Sư Phạm - Đại học Đà Nẵng
* **Khoa:** Toán - Tin

**Thành viên thực hiện:**
1. Nguyễn Phương Thảo - 23CNTT1
2. Nguyễn Lê Mỹ Linh - 23CNTT3
3. Lương Yến Nhân - 23CNTT3
4. Phan Nuyễn Bích An - 23CNTTx

---

## 2. GIỚI THIỆU

TelemedicineApp là ứng dụng di động được xây dựng trên nền tảng Android, cung cấp giải pháp kết nối trực tuyến giữa Bác sĩ và Bệnh nhân. 
Ứng dụng sử dụng ngôn ngữ **Kotlin** cùng bộ công cụ giao diện hiện đại **Jetpack Compose**. Hệ thống áp dụng kiến trúc **MVVM** (Model-View-ViewModel) kết hợp với **Dagger Hilt** để quản lý Dependency Injection. Dữ liệu được lưu trữ và đồng bộ theo thời gian thực (Real-time) thông qua hệ sinh thái **Firebase** (Firestore Database & Authentication).

---

## 3. YÊU CẦU HỆ THỐNG

* **Môi trường phát triển:** Android Studio (Bản Iguana hoặc mới nhất).
* **SDK:** Yêu cầu Minimum API Level 26 (Android 8.0) trở lên.
* **Thiết bị chạy:** Máy ảo Android (Emulator) hoặc thiết bị Android thật.
* **Mạng:** Ứng dụng yêu cầu kết nối Internet liên tục để thao tác với Cloud Database.

---

## 4. CẤU TRÚC DỰ ÁN

Dự án được chia module theo chuẩn kiến trúc Clean Architecture kết hợp MVVM:

* `model/`: Chứa các Data Class định nghĩa đối tượng (User, Appointment, MedicalRecord, DoctorSchedule...).
* `data/`: Tầng Repository, chứa các logic giao tiếp trực tiếp với Firebase (AuthRepository, DoctorRepository...).
* `presentation/`: Tầng UI, chứa toàn bộ các màn hình giao diện (Screen) viết bằng Jetpack Compose và ViewModel xử lý state.
* `di/`: Các file cấu hình tiêm phụ thuộc (Dependency Injection) của Dagger Hilt.
* `utils/`: Chứa các hàm hỗ trợ dùng chung (Định dạng ngày tháng, Xử lý chuỗi Base64 chuyển đổi ảnh...).

---

## 5. HƯỚNG DẪN CÀI ĐẶT VÀ CHẠY CHƯƠNG TRÌNH

**Bước 1:** Clone mã nguồn dự án về máy:
git clone https://github.com/MyLinh-bruh/TelemedicineApp.git

**Bước 2:** Mở dự án bằng phần mềm **Android Studio**.

**Bước 3:** Đồng bộ thư viện (Sync Project with Gradle Files).

**Bước 4: Cấu hình Firebase (Bắt buộc)**
* Tạo một dự án mới trên Firebase Console.
* Bật tính năng **Authentication** (Đăng nhập bằng Email/Password) và **Firestore Database**.
* Đăng ký ứng dụng Android vào Firebase project (nhập đúng package name: com.example.telemedicineapp).
* Tải file google-services.json từ Firebase và bỏ vào thư mục app/ trong dự án Android Studio của bạn.

**Bước 5:** Bấm nút **Run** (Shift + F10) để chạy ứng dụng trên máy ảo hoặc thiết bị thật.

---

## 6. DANH SÁCH CHỨC NĂNG (FEATURES)

### Dành cho Bệnh nhân (Patient)
* **Đăng ký/Đăng nhập:** Tạo tài khoản bằng Email và cập nhật hồ sơ cá nhân (Nhóm máu, Tiền sử bệnh).
* **Tìm kiếm & Lọc Bác sĩ:** Xem danh sách bác sĩ đã được duyệt. Hỗ trợ lọc theo Chuyên khoa (Tim mạch, Nhi khoa...) và Khu vực hoạt động (34 Tỉnh/Thành).
* **Đặt lịch khám:** Xem thời gian rảnh của bác sĩ để đặt lịch và thanh toán.
* **Xem Bệnh án:** Nhận và theo dõi hồ sơ chẩn đoán, toa thuốc từ bác sĩ sau khi khám xong.

### Dành cho Bác sĩ (Doctor)
* **Gửi yêu cầu hành nghề:** Tạo tài khoản, khai báo chuyên khoa, nơi công tác và tải lên ảnh chứng chỉ hành nghề (Chờ Admin phê duyệt).
* **Quản lý lịch làm việc:** Thêm các mốc thời gian bận để hệ thống tự trừ ra khỏi lịch trống cho bệnh nhân đặt.
* **Quản lý cuộc hẹn:** Xem danh sách bệnh nhân đã đặt lịch thành công.
* **Tạo hồ sơ bệnh án (EHR):** Tự động điền thông tin bệnh nhân. Ghi nhận lý do khám, triệu chứng lâm sàng và kết luận chẩn đoán. Hệ thống tự động chuyển trạng thái lịch hẹn sang "Đã hoàn thành" khi lưu bệnh án.

### Dành cho Quản trị viên (Admin)
* **Kiểm duyệt Bác sĩ:** Xem thông tin, kiểm tra hình ảnh chứng chỉ hành nghề thực tế.
* **Phê duyệt / Từ chối:** Duyệt để cấp quyền hoạt động trên hệ thống hoặc xóa bỏ tài khoản nếu không đủ điều kiện.
