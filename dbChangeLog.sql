-- =============================================================================
-- DATABASE CHANGELOG SCRIPT: Exam Application Backend
-- Hệ quản trị: MySQL 8.x
-- =============================================================================

-- -----------------------------------------------------------------------------
-- [v1.1.0] 2026-11-23: Bổ sung bảo mật tài khoản & Refresh Token (S6-A, S6-B)
-- -----------------------------------------------------------------------------

-- 1. Thêm trường chống brute-force vào bảng users
ALTER TABLE `users`
    ADD COLUMN `failed_login_attempts` INT NOT NULL DEFAULT 0 AFTER `status`,
    ADD COLUMN `locked_until` DATETIME NULL DEFAULT NULL AFTER `failed_login_attempts`;

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

-- -----------------------------------------------------------------------------
-- [v1.2.0] 2026-11-24: Phiên bản Đề thi & Khóa sửa đổi (TASK-T2)
-- -----------------------------------------------------------------------------

-- 1. Tạo bảng exam_versions
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

-- 2. Bổ sung khóa hiện tại và optimistic locking vào exams
ALTER TABLE `exams`
    ADD COLUMN `current_version_id` BIGINT NULL AFTER `created_by`,
    ADD COLUMN `entity_version` BIGINT DEFAULT 0 AFTER `current_version_id`,
    ADD CONSTRAINT `fk_exams_current_version` FOREIGN KEY (`current_version_id`) REFERENCES `exam_versions` (`id`) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- [v1.2.1] 2026-11-24: Mã hóa trường CSDL câu hỏi & đáp án bằng AES-256-GCM (TASK-T6)
-- -----------------------------------------------------------------------------
-- Cơ chế mã hóa mức trường (Field-Level Encryption) sử dụng JPA AttributeConverter (AesGcmAttributeConverter)
-- Các trường questions.content, answers.content, answers.explanation được mã hóa sang Base64 với tiền tố ENC:
-- Sử dụng kiểu dữ liệu TEXT hiện tại trong CSDL MySQL, không cần thay đổi cấu trúc bảng DDL.
