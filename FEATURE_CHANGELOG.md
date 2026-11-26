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
| `TASK-T1, T3`| Phê duyệt đề thi Maker-Checker (Role `APPROVER` & Permissions) | Backend | 2026-11-25 | **Hoàn thành (Backend)** |
| `TASK-T4` | Kiểm tra tính toàn vẹn đề thi bằng Canonical JSON & HMAC-SHA256 | Backend | 2026-11-25 | **Hoàn thành** |
| `TASK-T7` | Hệ thống Audit Log mở rộng & bất biến (INSERT ONLY + Masking) | Backend | 2026-11-25 | **Hoàn thành** |
| `REFACTOR-RBAC` | Chuẩn hóa Rules Constants & Custom Annotations (@RequirePermission) thay thế toàn diện @PreAuthorize | Backend | 2026-11-26 | **Hoàn thành** |
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

### [TASK-T1, TASK-T3, TASK-T4, TASK-T7] Phase 3: RBAC Approver, Phê duyệt Maker-Checker, Toàn vẹn HMAC-SHA256 & Audit Log Bất biến
* **Ngày thực hiện**: 2026-11-25
* **Người thực hiện**: Team
* **Phạm vi tác động**: Backend

#### 1. Mô tả chi tiết chức năng:
- **TASK-T1 (Role `APPROVER` & Permissions)**:
  - Bổ sung 6 permission mới trong `PermissionConstants`: `exam:submitReview`, `exam:review`, `exam:approve`, `exam:reject`, `exam:release`, `exam:revoke`.
  - Cập nhật `DataInitializer`: seed role `APPROVER` (`question:view`, `exam:review`, `exam:approve`, `exam:reject`), cập nhật quyền `TEACHER` (`exam:submitReview`, `exam:release`, `exam:revoke`), gán đủ quyền cho `ADMIN`, và seed user dev `approver1` (chỉ trong môi trường dev/test).
- **TASK-T3 (Quy trình Phê duyệt Maker-Checker)**:
  - Tạo entity `ExamApproval` (bảng `exam_approvals`, append-only) kèm DB CHECK constraint `approver_id <> creator_id`.
  - Triển khai `ExamApprovalService` và `ExamApprovalController` (`/api/approvals/...`), đồng thời kế thừa và tích hợp chặt chẽ với `/api/exams/{examId}/submit-review` và `/api/exams/{examId}/request-changes`.
  - Áp dụng luật Maker-Checker nghiêm ngặt: người tạo đề (`createdBy`) không thể tự rà soát/phê duyệt/từ chối/yêu cầu chỉnh sửa đề của mình (ném `MakerCheckerViolationException` -> HTTP 403 + ghi `AuditLog` `MAKER_CHECKER_VIOLATION` với transaction `REQUIRES_NEW`).
  - Bắt buộc `comment` tối thiểu 10 ký tự khi `reject` hoặc `request-changes`.
- **TASK-T4 (Toàn vẹn Đề thi Canonical JSON & HMAC-SHA256)**:
  - Tách khóa `INTEGRITY_SECRET_KEY` (`app.security.integrity.hmac-key`) độc lập khỏi `EXAM_ENCRYPTION_KEY` trong `CryptoKeyProvider` (kiểm tra fail-fast nếu trùng khóa).
  - Triển khai `ExamIntegrityService`: chuẩn hóa Canonical JSON (sắp xếp key theo thứ tự bảng chữ cái, sắp xếp `ExamQuestion` theo `orderIndex`, sắp xếp `Answer` theo `id`, loại bỏ khoảng trắng thừa, chuẩn hóa `BigDecimal`), ký `HMAC-SHA256` lưu vào `exam_versions.integrity_tag` khi phê duyệt (`APPROVED`).
  - Đối chiếu hằng thời gian (`MessageDigest.isEqual`) qua API `/verify-integrity` và trước khi cấp câu hỏi (`getExamQuestions`) cho phiên bản đã ký; nếu sai lệch ném `ExamIntegrityCompromisedException` (HTTP 409) và ghi cảnh báo `INTEGRITY_MISMATCH`.
- **TASK-T7 (Audit Log Mở rộng & Bất biến)**:
  - Đánh dấu `AuditLog` là `@Immutable` (`updatable = false` + `@PreUpdate`/`@PreRemove` chặn sửa/xóa ở JPA), không có endpoint `PUT`/`PATCH`/`DELETE` trên `AuditLogController`.
  - Chuẩn hóa các action (`CREATE_EXAM`, `SUBMIT_REVIEW`, `START_REVIEW`, `APPROVE_EXAM`, `REJECT_EXAM`, `REQUEST_CHANGES_EXAM`, `INTEGRITY_MISMATCH`, `INTEGRITY_VERIFIED`, `UNAUTHORIZED_ACCESS`, `MAKER_CHECKER_VIOLATION`).
  - Che giấu dữ liệu nhạy cảm (`Bearer` tokens, `password`/`secret`/`token`/`key`, chuỗi `ENC:...`) trước khi lưu vào `reason`.

#### 2. Danh sách file thay đổi:
- **Tạo mới**:
  - `src/main/java/com/android/app/exam_app_backend/entity/enums/ApprovalDecision.java`
  - `src/main/java/com/android/app/exam_app_backend/entity/ExamApproval.java`
  - `src/main/java/com/android/app/exam_app_backend/repository/ExamApprovalRepository.java`
  - `src/main/java/com/android/app/exam_app_backend/exception/MakerCheckerViolationException.java`
  - `src/main/java/com/android/app/exam_app_backend/exception/ExamIntegrityCompromisedException.java`
  - `src/main/java/com/android/app/exam_app_backend/payload/ExamApprovalRequest.java`
  - `src/main/java/com/android/app/exam_app_backend/payload/ExamApprovalResponse.java`
  - `src/main/java/com/android/app/exam_app_backend/payload/ExamReviewDetailResponse.java`
  - `src/main/java/com/android/app/exam_app_backend/payload/ExamIntegrityVerifyResponse.java`
  - `src/main/java/com/android/app/exam_app_backend/service/ExamIntegrityService.java`
  - `src/main/java/com/android/app/exam_app_backend/service/ExamApprovalService.java`
  - `src/main/java/com/android/app/exam_app_backend/controller/ExamApprovalController.java`
  - `src/test/java/com/android/app/exam_app_backend/ExamMakerCheckerSecurityTest.java`
  - `src/test/java/com/android/app/exam_app_backend/ExamIntegritySecurityTest.java`
- **Chỉnh sửa**:
  - `src/main/java/com/android/app/exam_app_backend/security/PermissionConstants.java`
  - `src/main/java/com/android/app/exam_app_backend/config/DataInitializer.java`
  - `src/main/java/com/android/app/exam_app_backend/security/crypto/CryptoKeyProvider.java`
  - `src/main/java/com/android/app/exam_app_backend/advice/GlobalExceptionHandler.java`
  - `src/main/java/com/android/app/exam_app_backend/entity/AuditLog.java`
  - `src/main/java/com/android/app/exam_app_backend/repository/AuditLogRepository.java`
  - `src/main/java/com/android/app/exam_app_backend/service/AuditLogService.java`
  - `src/main/java/com/android/app/exam_app_backend/controller/AuditLogController.java`
  - `src/main/java/com/android/app/exam_app_backend/repository/ExamRepository.java`
  - `src/main/java/com/android/app/exam_app_backend/service/ExamService.java`
  - `src/main/java/com/android/app/exam_app_backend/controller/ExamController.java`
  - `src/main/resources/application.yaml`
  - `src/test/resources/application.yaml`
  - `.env.example`
  - `database_schema.sql`
  - `dbChangeLog.sql`
  - `DB_CHANGELOG.md`
  - `FEATURE_CHANGELOG.md`

#### 3. Thay đổi API Contract:
- **GET `/api/approvals/pending`**: Danh sách đề thi chờ duyệt (`SUBMITTED`, `IN_REVIEW`) — yêu cầu `exam:review`.
- **GET `/api/approvals/exams/{examId}`**: Chi tiết đề thi, câu hỏi và lịch sử duyệt cho Approver — yêu cầu `exam:review`.
- **GET `/api/approvals/exams/{examId}/history`**: Lịch sử phê duyệt của đề thi.
- **POST `/api/approvals/exams/{examId}/start-review`** & **POST `/api/exams/{examId}/start-review`**: Chuyển `SUBMITTED -> IN_REVIEW`.
- **POST `/api/approvals/exams/{examId}/approve`** & **POST `/api/exams/{examId}/approve`**: Phê duyệt (`APPROVED`) và ký HMAC-SHA256 `integrityTag`.
- **POST `/api/approvals/exams/{examId}/reject`** & **POST `/api/exams/{examId}/reject`**: Từ chối (`REJECTED`, yêu cầu `comment >= 10` ký tự).
- **POST `/api/approvals/exams/{examId}/request-changes`** & **POST `/api/exams/{examId}/request-changes`**: Yêu cầu chỉnh sửa (`CHANGES_REQUESTED`, sinh version mới `DRAFT`, yêu cầu `comment >= 10` ký tự).
- **POST `/api/approvals/exams/{examId}/verify-integrity`**, **POST `/api/exams/{examId}/verify-integrity`**, **GET `/api/exams/{examId}/integrity`**: Kiểm tra chữ ký toàn vẹn HMAC-SHA256.
- **GET `/api/audit-logs`**: Hỗ trợ thêm query filter tùy chọn (`action`, `resourceType`, `resourceId`).

#### 4. Kiểm thử & Đảm bảo chất lượng:
- `ExamMakerCheckerSecurityTest.java` (Test B1 + T1 + T3 + T7): Kiểm tra chặn Teacher/Admin tự duyệt đề mình tạo (403 + AuditLog), luồng Approver `pending -> start-review -> approve`, bắt buộc nhận xét >= 10 ký tự khi reject, ràng buộc CHECK trong DB, bất biến AuditLog và masking secret.
- `ExamIntegritySecurityTest.java` (Test B2 + T4): Kiểm tra tách khóa AES vs HMAC, Canonical JSON deterministic sorting, phát hiện sửa đổi 1 ký tự nội dung câu hỏi hoặc sửa `integrity_tag` trực tiếp trong DB (409 Conflict + AuditLog `INTEGRITY_MISMATCH`).
- `ExamVersioningSecurityTest.java` (Phase 2 regression): Tiếp tục PASS 100%.
- Toàn bộ test suite backend (`.\mvnw.cmd test`): **49/49 tests PASS 100%** (0 failures, 0 errors, 0 skipped).

---

### [REFACTOR-RBAC] Chuẩn hóa Constants & Thay thế toàn bộ @PreAuthorize bằng Custom RBAC Annotations
- **Ngày thực hiện**: 2026-11-26
- **Mục đích**: 
  - Tránh magic strings trong toàn bộ mã nguồn backend và quy định rõ trong Rules dự án (`project_rules.md`, `RULES_AI_CODING.md`, `.cursorrules`).
  - Loại bỏ các biểu thức SpEL dài dòng và khó đọc của `@PreAuthorize("@permissionEvaluator.hasPermission(authentication, T(com.android.app.exam_app_backend.security.PermissionConstants).XXX)")` bằng các annotation ngắn gọn, trực quan, type-safe hơn.
- **Nội dung thực hiện**:
  1. **Tạo `AppConstants.java`**: Chứa các hằng số thông báo hệ thống, Resource Types, Audit Actions, Validation messages.
  2. **Tạo Custom Annotations & Aspect**:
     - `@RequirePermission(String value)`: Kiểm tra 1 quyền cụ thể (ví dụ `@RequirePermission(PermissionConstants.EXAM_REVIEW)`).
     - `@RequireAnyPermission(String[] value)`: Kiểm tra ít nhất 1 trong các quyền (ví dụ `@RequireAnyPermission({PermissionConstants.EXAM_VIEW_RESULTS, PermissionConstants.EXAM_VIEW_OWN_RESULTS})`).
     - `PermissionAspect`: Aspect AOP đánh chặn các method được gán annotation trên Service/Controller, ủy quyền cho `PermissionEvaluatorService.hasPermission()` / `hasAnyPermission()`, tự động ném `AccessDeniedException` và ghi Audit Log vi phạm nếu người dùng không có quyền.
  3. **Refactor 100% các Controller & Service**:
     - `AuditLogController.java`
     - `FileController.java`
     - `RbacController.java`
     - `UserController.java`
     - `ExamApprovalService.java`
     - `ExamService.java`
     - `QuestionService.java`
     - `ResultService.java`
     - `QuestionExcelImportService.java`
  4. **Kiểm thử**:
     - Quét sạch toàn bộ `src/main/java`, không còn bất kỳ `@PreAuthorize` nào.
     - Chạy toàn bộ test suite backend: **49/49 tests PASS 100%** (bao gồm `RbacIntegrationTests`, `ExamMakerCheckerSecurityTest`, `ExamIntegritySecurityTest`, `ExamVersioningSecurityTest`,...).
