#!/bin/bash
set -e

# =============================================================================
# SCRIPT DOCKER ENTRYPOINT: Tự động khởi tạo và phân quyền CSDL MySQL
# =============================================================================

APP_USER="${MYSQL_APP_USER:-exam_app}"
APP_PASS="${MYSQL_APP_PASSWORD:-ExamApp@Dev2026}"
REPORT_PASS="${MYSQL_REPORT_PASSWORD:-ExamReport@2026}"
DEPLOY_PASS="${MYSQL_DEPLOY_PASSWORD:-ExamDeploy@2026}"
DB_NAME="${MYSQL_DATABASE:-school_exam_db}"

echo "[INFO] Dang khoi tao phan quyen CSDL cho database: ${DB_NAME}"

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
    CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

    -- 1. Tai khoan Backend Application (Cap DML + CREATE, ALTER cho moi truong Dev)
    CREATE USER IF NOT EXISTS '${APP_USER}'@'%' IDENTIFIED BY '${APP_PASS}';
    GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE, CREATE, ALTER, INDEX, REFERENCES ON \`${DB_NAME}\`.* TO '${APP_USER}'@'%';

    -- 2. Tai khoan Read-Only tra cuu / bao cao
    CREATE USER IF NOT EXISTS 'exam_report'@'%' IDENTIFIED BY '${REPORT_PASS}';
    GRANT SELECT ON \`${DB_NAME}\`.* TO 'exam_report'@'%';

    -- 3. Tai khoan Migration / Deploy (DDL toan quyen)
    CREATE USER IF NOT EXISTS 'exam_deploy'@'%' IDENTIFIED BY '${DEPLOY_PASS}';
    GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO 'exam_deploy'@'%';

    FLUSH PRIVILEGES;
EOSQL

echo "[INFO] Da khoi tao xong cac tai khoan DB: ${APP_USER}, exam_report, exam_deploy"
