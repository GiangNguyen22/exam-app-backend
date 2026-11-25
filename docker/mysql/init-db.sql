-- =============================================================================
-- SCRIPT KHỞI TẠO VÀ PHÂN QUYỀN CƠ SỞ DỮ LIỆU (DATABASE ACCESS CONTROL)
-- Hệ quản trị: MySQL 8.0
-- Nguyên tắc: Đặc quyền tối thiểu (PoLP) kết hợp linh hoạt cho môi trường Dev
-- =============================================================================

-- 1. Khởi tạo Database nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS `school_exam_db` 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 2. Tạo tài khoản cho Backend Application (Runtime)
-- Cấp quyền DML + (CREATE, ALTER, INDEX, REFERENCES) phục vụ môi trường Dev để Hibernate ddl-auto update hoạt động trơn tru.
-- Lưu ý: Không cấp quyền DROP TABLE, TRUNCATE, DROP DATABASE trên schema để bảo vệ dữ liệu.
CREATE USER IF NOT EXISTS 'exam_app'@'%' IDENTIFIED BY 'ExamApp@Dev2026';
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE, CREATE, ALTER, INDEX, REFERENCES ON `school_exam_db`.* TO 'exam_app'@'%';

-- 3. Tạo tài khoản Read-Only cho dev tra cứu dữ liệu, báo cáo / thống kê
CREATE USER IF NOT EXISTS 'exam_report'@'%' IDENTIFIED BY 'ExamReport@2026';
GRANT SELECT ON `school_exam_db`.* TO 'exam_report'@'%';

-- 4. Tạo tài khoản Migration / CI-CD chuyên dụng (DDL toàn quyền)
CREATE USER IF NOT EXISTS 'exam_deploy'@'%' IDENTIFIED BY 'ExamDeploy@2026';
GRANT ALL PRIVILEGES ON `school_exam_db`.* TO 'exam_deploy'@'%';

-- 5. Áp dụng các thay đổi quyền
FLUSH PRIVILEGES;
