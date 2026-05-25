-- Script tạo cơ sở dữ liệu cho Hệ thống Thi Trắc Nghiệm (Dựa trên System Analysis Design)
CREATE DATABASE IF NOT EXISTS school_exam_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE school_exam_db;

-- ==========================================
-- 1. HỆ THỐNG PHÂN QUYỀN RBAC & NGƯỜI DÙNG
-- ==========================================

-- Bảng Người dùng
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    student_id VARCHAR(20) UNIQUE, -- MSSV cho học sinh
    employee_code VARCHAR(20) UNIQUE, -- Mã nhân viên cho giáo viên
    status ENUM('ACTIVE', 'LOCKED', 'PENDING') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Bảng Vai trò
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE, -- VD: ROLE_ADMIN, ROLE_TEACHER, ROLE_STUDENT
    description VARCHAR(255)
) ENGINE=InnoDB;

-- Bảng Quyền chi tiết
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE, -- VD: question:create, exam:submit
    description VARCHAR(255)
) ENGINE=InnoDB;

-- Bảng liên kết Người dùng - Vai trò (Nhiều - Nhiều)
CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    scope_id BIGINT, -- ID của lớp hoặc khoa nếu cần phân vùng quản lý
    assigned_by BIGINT,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Bảng liên kết Vai trò - Quyền (Nhiều - Nhiều)
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Bảng Nhật ký truy cập và quyền (Audit Logs)
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(255) NOT NULL, -- VD: LOGIN, CREATE_EXAM
    resource_type VARCHAR(50), -- VD: EXAM, QUESTION
    resource_id BIGINT,
    result ENUM('allow', 'deny', 'error') NOT NULL,
    reason TEXT,
    meta JSON, -- Lưu thêm thông tin IP, Browser...
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ==========================================
-- 2. QUẢN LÝ NGÂN HÀNG CÂU HỎI
-- ==========================================

-- Bảng Môn học
CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
) ENGINE=InnoDB;

-- Bảng Chủ đề (Topic) trong môn học
CREATE TABLE topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Bảng Câu hỏi
CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    topic_id BIGINT ,
    content TEXT NOT NULL, -- Hỗ trợ LaTeX
    type ENUM('SINGLE', 'MULTI', 'TRUE_FALSE', 'FILL_BLANK') NOT NULL,
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL,
    image_url VARCHAR(255),
    video_url VARCHAR(255),
    audio_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    created_by BIGINT,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Bảng Đáp án
CREATE TABLE answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    content TEXT NOT NULL, -- Hỗ trợ LaTeX
    is_correct BOOLEAN DEFAULT FALSE,
    explanation TEXT,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 3. QUẢN LÝ ĐỀ THI VÀ KẾT QUẢ
-- ==========================================

-- Bảng Đề thi
CREATE TABLE exams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE, -- Mã đề thi
    title VARCHAR(200) NOT NULL,
    duration_minutes INT NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    shuffle_questions BOOLEAN DEFAULT TRUE,
    shuffle_answers BOOLEAN DEFAULT TRUE,
    score_per_question DECIMAL(5, 2) DEFAULT 1.0,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Bảng liên kết Đề thi - Câu hỏi
CREATE TABLE exam_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    order_index INT,
    score DECIMAL(5, 2), -- Điểm riêng cho câu này nếu khác mặc định
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id)
) ENGINE=InnoDB;

-- Bảng Kết quả thi
CREATE TABLE exam_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    score DECIMAL(5, 2),
    status ENUM('DOING', 'SUBMITTED', 'CANCELLED') DEFAULT 'DOING',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id),
    FOREIGN KEY (exam_id) REFERENCES exams(id)
) ENGINE=InnoDB;

-- Bảng Câu trả lời của thí sinh
CREATE TABLE student_responses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_answer_ids VARCHAR(255), -- Lưu danh sách ID ngăn cách bởi dấu phẩy
    fill_content TEXT,
    last_saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (result_id) REFERENCES exam_results(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id)
) ENGINE=InnoDB;

-- Bảng Nhật ký hành vi (Activity Logs - Chống gian lận)
CREATE TABLE activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT NOT NULL,
    event_type ENUM('APP_EXIT', 'SCREENSHOT', 'LOST_CONNECTION', 'FOCUS_LOST') NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (result_id) REFERENCES exam_results(id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- Create index
CREATE INDEX idx_exam_results_student
ON exam_results(student_id);

CREATE INDEX idx_exam_results_exam
ON exam_results(exam_id);

CREATE INDEX idx_student_responses_result
ON student_responses(result_id);

CREATE INDEX idx_questions_subject
ON questions(subject_id);
