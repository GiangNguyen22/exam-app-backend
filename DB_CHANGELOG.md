# NHẬT KÝ THAY ĐỔI CƠ SỞ DỮ LIỆU (DATABASE CHANGELOG)

> **Dự án**: Hệ thống Phê duyệt & Phát hành Đề thi An toàn (Exam Application & Backend)  
> **Hệ quản trị CSDL**: MySQL 8.x  
> **Quy tắc**: Mọi thay đổi schema (thêm bảng, sửa cột, xóa cột, thêm khóa ngoại, index) hoặc seed data **BẮT BUỘC** phải được ghi lại vào tài liệu này và file SQL tương ứng `dbChangeLog.sql`.

---

## 1. QUY CHUẨN GHI LOG THAY ĐỔI DB
Mỗi lần thay đổi cấu trúc DB, lập trình viên/AI cần ghi một mục mới với cấu trúc:
1. **Mã phiên bản (Version)**: Ví dụ `v1.1.0-add-refresh-tokens`, `v1.2.0-exam-versioning`.
2. **Ngày thực hiện**: `YYYY-MM-DD`.
3. **Mục đích & Lý do**: Giải thích rõ ràng tại sao cần thay đổi.
4. **Câu lệnh SQL thực thi (DDL / DML)**.
5. **Câu lệnh khôi phục (Rollback SQL)** nếu cần hoàn tác.
6. **Entities JPA bị ảnh hưởng**.

---

## 2. BẢNG TỔNG HỢP CÁC PHIÊN BẢN DATABASE

| Phiên bản | Ngày | Người thực hiện | Tóm tắt thay đổi | Trạng thái |
| :--- | :---: | :---: | :--- | :---: |
| `v1.0.0` | 2026-10-01 | Team | Khởi tạo schema ban đầu (`database_schema.sql`): users, roles, permissions, questions, exams, audit_logs,... | **Đã áp dụng** |
| `v1.1.0` | 2026-11-23 | Team | Bổ sung bảng `refresh_tokens`, trường khóa tài khoản `failed_login_attempts`, `locked_until` trong `users`. | **Đã áp dụng** |
| `v1.2.0` | 2026-11-24 | Team | Bổ sung bảng `exam_versions` (version_no, status, integrity_tag) & khóa sửa đổi đề thi. | **Đã áp dụng** |
| `v1.2.1` | 2026-11-24 | Team | Áp dụng mã hóa trường dữ liệu CSDL (AES-256-GCM) cho `Question` và `Answer`. | **Đã áp dụng** |
| `v1.3.0` | - | - | Bổ sung bảng `exam_approvals` (Maker-Checker quy trình duyệt) & role `ROLE_APPROVER`. | **Chờ triển khai** |
| `v1.4.0` | - | - | Bổ sung bảng `exam_releases` (phát hành theo lớp, khung giờ và thu hồi). | **Chờ triển khai** |

---

## 3. CHI TIẾT CÁC BẢN CẬP NHẬT DB

### [v1.1.0] - Bảo mật tài khoản & Cơ chế Refresh Token
* **Ngày**: 2026-11-23
* **Task liên quan**: `TASK-S6-A`, `TASK-S6-B`
* **Mục đích**:
  * Hạn chế tấn công Brute-force: Thêm trường đếm số lần đăng nhập sai và thời điểm tạm khóa.
  * Hỗ trợ Refresh Token Rotation (RTR): Bảng lưu trữ băm token và vết xoay vòng.
* **SQL DDL**:
```sql
-- 1. Thêm trường chống brute-force vào bảng users
ALTER TABLE `users`
    ADD COLUMN `failed_attempts` INT NOT NULL DEFAULT 0 AFTER `status`,
    ADD COLUMN `locked_until` DATETIME NULL DEFAULT NULL AFTER `failed_attempts`;

-- 2. Tạo bảng refresh_tokens
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `token_hash` VARCHAR(64) NOT NULL UNIQUE,
    `expires_at` DATETIME NOT NULL,
    `revoked_at` DATETIME NULL DEFAULT NULL,
    `replaced_by_token_hash` VARCHAR(64) NULL DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_refresh_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_refresh_tokens_hash` (`token_hash`),
    INDEX `idx_refresh_tokens_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
* **Rollback SQL**:
```sql
DROP TABLE IF EXISTS `refresh_tokens`;
ALTER TABLE `users` DROP COLUMN `locked_until`, DROP COLUMN `failed_attempts`;
```
* **Entities JPA liên quan**:
  * Sửa: `com.android.app.exam_app_backend.entity.User`
  * Tạo mới: `com.android.app.exam_app_backend.entity.RefreshToken`

### [v1.2.0] - Quản lý phiên bản đề thi & Khóa sửa đổi (TASK-T2)
* **Ngày**: 2026-11-24
* **Task liên quan**: `TASK-T2`
* **Mục đích**:
  * Quản lý vòng đời trạng thái đề thi (`DRAFT`, `SUBMITTED`, `CHANGES_REQUESTED`,...).
  * Khóa hoàn toàn việc sửa/xóa đề hoặc câu hỏi khi đề đã nộp duyệt.
  * Hỗ trợ Optimistic Locking (`entity_version`) chống race condition.
* **SQL DDL**:
```sql
CREATE TABLE IF NOT EXISTS `exam_versions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` BIGINT NOT NULL,
    `version_no` INT NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    `notes` TEXT NULL,
    `integrity_tag` VARCHAR(128) NULL,
    `entity_version` BIGINT DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_exam_version_no` UNIQUE (`exam_id`, `version_no`),
    CONSTRAINT `fk_exam_versions_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE CASCADE,
    INDEX `idx_exam_versions_exam_id` (`exam_id`),
    INDEX `idx_exam_versions_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `exams`
    ADD COLUMN `current_version_id` BIGINT NULL AFTER `created_by`,
    ADD COLUMN `entity_version` BIGINT DEFAULT 0 AFTER `current_version_id`,
    ADD CONSTRAINT `fk_exams_current_version` FOREIGN KEY (`current_version_id`) REFERENCES `exam_versions` (`id`) ON DELETE SET NULL;
```
* **Rollback SQL**:
```sql
ALTER TABLE `exams` DROP FOREIGN KEY `fk_exams_current_version`;
ALTER TABLE `exams` DROP COLUMN `entity_version`, DROP COLUMN `current_version_id`;
DROP TABLE IF EXISTS `exam_versions`;
```
* **Entities JPA liên quan**:
  * Sửa: `com.android.app.exam_app_backend.entity.Exam`
  * Tạo mới: `com.android.app.exam_app_backend.entity.ExamVersion`, `com.android.app.exam_app_backend.entity.enums.ExamStatus`

### [v1.2.1] - Mã hóa dữ liệu trường CSDL bằng AES-256-GCM (TASK-T6)
* **Ngày**: 2026-11-24
* **Task liên quan**: `TASK-T6`
* **Mục đích**:
  * Mã hóa nội dung câu hỏi (`Question.content`), đáp án (`Answer.content`), và giải thích (`Answer.explanation`) bằng AES-256-GCM.
  * Ngăn ngừa rò rỉ đề thi khi dump database hoặc bị truy cập trái phép trực tiếp vào DB.
  * Tương thích ngược: dữ liệu plaintext cũ vẫn đọc được bình thường.
* **SQL DDL**:
  * Không thay đổi DDL bảng (sử dụng cột `TEXT` sẵn có trong schema).
* **Entities JPA liên quan**:
  * Sửa: `com.android.app.exam_app_backend.entity.Question`, `com.android.app.exam_app_backend.entity.Answer`
  * Tạo mới: `com.android.app.exam_app_backend.security.crypto.AesGcmAttributeConverter`, `com.android.app.exam_app_backend.security.crypto.CryptoKeyProvider`
