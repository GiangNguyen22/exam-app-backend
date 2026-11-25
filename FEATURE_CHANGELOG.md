# NHẬT KÝ TÍNH NĂNG & THAY ĐỔI MÃ NGUỒN (FEATURE & CODE CHANGELOG)

> **Dự án**: Hệ thống Phê duyệt & Phát hành Đề thi An toàn (Exam Application & Backend)  
> **Quy tắc bắt buộc**: Mọi tính năng thêm mới, cải tiến, tái cấu trúc (refactoring) hoặc sửa đổi logic nghiệp vụ **BẮT BUỘC** phải được ghi lại vào tài liệu này.

---

## 1. QUY CHUẨN GHI NHẬT KÝ THAY ĐỔI
Mỗi bản ghi thay đổi cần bao gồm các thông tin:
1. **Mã Task & Tên Tính Năng**: (Ví dụ `TASK-S6-B: Refresh Token Rotation`).
2. **Ngày thực hiện & Người thực hiện**: `YYYY-MM-DD` | `Author`.
3. **Mục đích & Lý do**: Tại sao thêm mới hoặc sửa đổi.
4. **Mô tả chi tiết chức năng (What was changed/added)**:
   - Nghiệp vụ hoạt động như thế nào.
   - Luồng dữ liệu mới.
5. **Danh sách Files thay đổi**:
   - Backend files (Controller, Service, Entity, DTO, Config).
   - Android files (Screens, ViewModels, Components, Network, Monitors).
6. **Thay đổi Hợp đồng API (API Contracts)**:
   - Endpoint mới / sửa đổi (Method, Path, Request, Response, Status Codes).
7. **Quy tắc Bảo mật & Kiểm thử**:
   - Các kịch bản test đã chạy để nghiệm thu tính năng.

---

## 2. BẢNG TỔNG HỢP CÁC TÍNH NĂNG & SỬA ĐỔI

| Mã Task | Tên tính năng / Sửa đổi | Phạm vi | Ngày | Trạng thái |
| :--- | :--- | :---: | :---: | :---: |
| `TASK-S1` | Cách ly Secret, đưa DB credentials & JWT secret vào `.env` | Backend | 2026-11-23 | **Hoàn thành** |
| `TASK-S2` | Network Security Config & Chặn cleartext HTTP | Android | 2026-11-23 | **Hoàn thành** |
| `TASK-S3` | Lưu trữ Token an toàn bằng Android Keystore (AES-256-GCM) | Android | 2026-11-23 | **Hoàn thành** |
| `TASK-S4` | Kiểm tra quyền sở hữu đề thi (Anti-IDOR) | Backend | 2026-11-23 | **Hoàn thành** |
| `TASK-S5` | Bảo vệ endpoint `/uploads/**` & Kiểm tra Magic Bytes file ảnh | Backend | 2026-11-23 | **Hoàn thành** |
| `TASK-S6-A` | Giới hạn tần suất gọi API (Rate Limit) & Khóa Brute-Force | Backend | 2026-11-24 | **Hoàn thành** |
| `TASK-S6-B` | Cặp Access Token (15p) & Refresh Token Rotation (7 ngày) | Backend & Android | 2026-11-24 | **Hoàn thành** |
| `TASK-S7` | Chuẩn hóa Enum sự kiện giám sát phòng thi | Backend & Android | 2026-11-24 | **Hoàn thành** |
| `TASK-T2` | Quản lý phiên bản đề (`ExamVersion`) & Khóa sửa khi gửi duyệt | Backend | 2026-11-24 | **Hoàn thành** |
| `TASK-T6` | Mã hóa nội dung câu hỏi CSDL bằng AES-256-GCM | Backend | 2026-11-24 | **Hoàn thành** |
| `TASK-T1, T3`| Phê duyệt đề thi Maker-Checker (Role Approver) | Backend & Android | - | Chờ triển khai |
| `TASK-T4` | Kiểm tra tính toàn vẹn đề thi bằng HMAC-SHA256 | Backend | - | Chờ triển khai |
| `TASK-T5` | Quản lý phát hành theo lớp & Thu hồi đề thi khẩn cấp | Backend & Android | - | Chờ triển khai |
| `TASK-T9` | Chống gian lận Client: ScreenCaptureCallback, xóa Clipboard | Android | - | Chờ triển khai |
| `TASK-T10`| Giám sát Live Proctoring qua WebSocket & Cảnh báo tức thì | Backend & Android | - | Chờ triển khai |

---

## 3. CHI TIẾT TỪNG TÍNH NĂNG VÀ SỬA ĐỔI

*(Khi bạn hoặc AI hoàn thành một task, hãy bổ sung mục chi tiết vào đây theo template chuẩn dưới đây)*

```markdown
### [TASK-XXX] Tên Chức Năng Cụ Thể
* **Ngày thực hiện**: YYYY-MM-DD
* **Người thực hiện**: ...
* **Phạm vi tác động**: Backend / Android / Cả hai

#### 1. Mô tả chi tiết chức năng:
- Mô tả chức năng làm gì, giải quyết vấn đề gì...
- Chi tiết logic nghiệp vụ hoặc thuật toán mới...

#### 2. Danh sách file thay đổi:
- **Tạo mới**:
  - `path/to/NewFile.java`
- **Chỉnh sửa**:
  - `path/to/ModifiedFile.kt`

#### 3. Thay đổi API Contract (nếu có):
- **POST `/api/example`**:
  - Request: `{ "field": "value" }`
  - Response: `{ "success": true, "data": ... }`

#### 4. Kiểm thử & Đảm bảo chất lượng:
- Unit test / Integration test đã viết: ...
- Kết quả test: PASS 100%.
```

### [TASK-T2] Quản lý phiên bản đề thi & Khóa sửa đổi
* **Ngày thực hiện**: 2026-11-24
* **Người thực hiện**: Team
* **Phạm vi tác động**: Backend

#### 1. Mô tả chi tiết chức năng:
- Xây dựng máy trạng thái đề thi (`ExamStatus`): `DRAFT`, `SUBMITTED`, `IN_REVIEW`, `APPROVED`, `REJECTED`, `CHANGES_REQUESTED`, `SCHEDULED`, `RELEASED`, `CLOSED`, `REVOKED`.
- Bổ sung bảng `exam_versions` và liên kết với `exams`, lưu giữ toàn bộ lịch sử các phiên bản (`version_no`, `status`, `notes`, `integrity_tag`).
- Khi tạo đề hoặc sinh đề tự động, hệ thống tự động gán Version 1 ở trạng thái `DRAFT`.
- Triển khai **Guard Clause**: Khóa toàn bộ các thao tác chỉnh sửa đề (`updateExam`), thêm/sửa câu hỏi (`createQuestionForExam`, `updateQuestionForExam`), và xóa đề (`deleteExam`) khi trạng thái đề không phải `DRAFT` (trả về HTTP 409 Conflict).
- Nộp duyệt đề thi (`POST /api/exams/{id}/submit-review`): Chuyển trạng thái sang `SUBMITTED` (yêu cầu đề phải có ít nhất 1 câu hỏi).
- Yêu cầu sửa đổi (`POST /api/exams/{id}/request-changes`): Chuyển version hiện tại sang `CHANGES_REQUESTED`, đồng thời tự động tạo một version mới tuần tự `version_no + 1` ở trạng thái `DRAFT` để giáo viên chỉnh sửa.
- Hỗ trợ Optimistic Locking (`@Version private Long entityVersion;`) trên cả `Exam` và `ExamVersion` để ngăn ngừa race condition / conflict updates.

#### 2. Danh sách file thay đổi:
- **Tạo mới**:
  - `src/main/java/com/android/app/exam_app_backend/entity/enums/ExamStatus.java`
  - `src/main/java/com/android/app/exam_app_backend/entity/ExamVersion.java`
  - `src/main/java/com/android/app/exam_app_backend/repository/ExamVersionRepository.java`
  - `src/main/java/com/android/app/exam_app_backend/payload/ExamVersionResponse.java`
  - `src/test/java/com/android/app/exam_app_backend/ExamVersioningSecurityTest.java`
- **Chỉnh sửa**:
  - `src/main/java/com/android/app/exam_app_backend/entity/Exam.java`
  - `src/main/java/com/android/app/exam_app_backend/payload/ExamResponse.java`
  - `src/main/java/com/android/app/exam_app_backend/service/ExamService.java`
  - `src/main/java/com/android/app/exam_app_backend/controller/ExamController.java`
  - `database_schema.sql`
  - `dbChangeLog.sql`
  - `DB_CHANGELOG.md`
  - `FEATURE_CHANGELOG.md`

#### 3. Thay đổi API Contract:
- **POST `/api/exams/{examId}/submit-review`**:
  - Chuyển đề sang `SUBMITTED`.
  - Response: `ApiResponse<ExamResponse>` với `status = "SUBMITTED"`.
- **POST `/api/exams/{examId}/request-changes`**:
  - Request: `{ "comments": "Lý do yêu cầu sửa" }`
  - Response: `ApiResponse<ExamResponse>` với `versionNo = oldVersion + 1`, `status = "DRAFT"`.
- **GET `/api/exams/{examId}/versions`**:
  - Response: `ApiResponse<List<ExamVersionResponse>>` danh sách lịch sử phiên bản giảm dần theo `versionNo`.

#### 4. Kiểm thử & Đảm bảo chất lượng:
- Test: `ExamVersioningSecurityTest.java`
- Kiểm tra toàn bộ vòng đời: Khởi tạo v1 DRAFT -> Thêm câu hỏi -> Nộp duyệt SUBMITTED -> Khóa sửa/thêm/xóa đề -> Yêu cầu sửa đổi sinh v2 DRAFT -> Tra cứu lịch sử phiên bản -> Sửa đổi ở v2 thành công.
- Kết quả test: PASS 100%.

### [TASK-T6] Mã hóa trường dữ liệu đề thi CSDL bằng AES-256-GCM (Database Field-Level Encryption)
* **Ngày thực hiện**: 2026-11-24
* **Người thực hiện**: Team
* **Phạm vi tác động**: Backend

#### 1. Mô tả chi tiết chức năng:
- Áp dụng kỹ thuật mã hóa mức trường (Field-Level Encryption) cho các trường nhạy cảm của đề thi: `Question.content`, `Answer.content`, và `Answer.explanation`.
- Khóa bảo mật: Sử dụng khóa AES 256-bit đọc từ biến môi trường `EXAM_ENCRYPTION_KEY` thông qua `CryptoKeyProvider`.
- Thuật toán mã hóa: `AES/GCM/NoPadding` với IV ngẫu nhiên 96-bit (12 bytes) cho mỗi lần mã hóa sinh bằng `SecureRandom` và authentication tag 128-bit chống giả mạo / tampering.
- Định dạng lưu CSDL: `ENC:` + Base64(`[IV: 12B][Ciphertext + Tag: 16B]`).
- Tích hợp trong suốt qua JPA: `AesGcmAttributeConverter implements AttributeConverter<String, String>` tự động mã hóa trước khi INSERT/UPDATE vào CSDL và tự động giải mã khi SELECT entity.
- Đảm bảo tương thích ngược: Nếu dữ liệu trong DB là bản ghi cũ chưa được mã hóa (không có tiền tố `ENC:`), converter tự động trả về chuỗi gốc mà không gây lỗi.

#### 2. Danh sách file thay đổi:
- **Tạo mới**:
  - `src/main/java/com/android/app/exam_app_backend/security/crypto/CryptoKeyProvider.java`
  - `src/main/java/com/android/app/exam_app_backend/security/crypto/AesGcmAttributeConverter.java`
  - `src/test/java/com/android/app/exam_app_backend/ExamFieldEncryptionSecurityTest.java`
- **Chỉnh sửa**:
  - `src/main/java/com/android/app/exam_app_backend/entity/Question.java`
  - `src/main/java/com/android/app/exam_app_backend/entity/Answer.java`
  - `src/main/java/com/android/app/exam_app_backend/repository/AnswerRepository.java`
  - `src/main/resources/application.yaml`
  - `src/test/resources/application.yaml`
  - `.env.example`
  - `dbChangeLog.sql`
  - `DB_CHANGELOG.md`
  - `FEATURE_CHANGELOG.md`

#### 3. Thay đổi API Contract:
- Không thay đổi hợp đồng API bên ngoài. Dữ liệu trả về cho client vẫn là câu hỏi và đáp án hợp lệ, quá trình mã hóa/giải mã diễn ra hoàn toàn trong suốt tại tầng lưu trữ CSDL.

#### 4. Kiểm thử & Đảm bảo chất lượng:
- Test: `ExamFieldEncryptionSecurityTest.java`
  - `testQuestionAndAnswerFieldEncryption`: Dùng Native SQL kiểm tra trực tiếp cột `content` trong DB: xác nhận bắt đầu bằng `ENC:`, không chứa plaintext; đồng thời kiểm tra JPA load entity trả về đúng plaintext ban đầu.
  - `testBackwardCompatibilityWithLegacyPlaintext`: Chèn dữ liệu plaintext cũ vào DB, kiểm tra JPA load bình thường không bị lỗi giải mã.
- Toàn bộ test suite backend: 39/39 tests PASS 100%.
