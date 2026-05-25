# Project Skills - Exam App Backend

Tai lieu nay dinh nghia bo ky nang ky thuat can co de phat trien va bao tri he thong thi trac nghiem noi bo.

## 1) Auth & JWT Skill
**Muc tieu**
- Xay dung va bao tri dang nhap JWT stateless.
- Quan ly token an toan, dung luong claim gon.

**Nang luc can co**
- Spring Security filter chain.
- `AuthenticationManager`, `UserDetailsService`, `PasswordEncoder`.
- JJWT (`io.jsonwebtoken`) va expiration policy.

**Definition of Done**
- Login tra ve `ApiResponse<LoginResponse>` hop le.
- Token sai/het han tra `401`.
- Test cover luong login thanh cong/that bai.

## 2) RBAC & Permission Skill
**Muc tieu**
- Kiem soat truy cap theo role + permission (`resource:action`).

**Nang luc can co**
- Modeling bang `roles`, `permissions`, `role_permissions`, `user_roles`.
- Guard endpoint/service theo permission.
- Audit event deny/allow cho thao tac nhay cam.

**Definition of Done**
- Ma tran role x action khop tai lieu phan tich.
- Endpoint quan trong co test `401/403/200`.

## 3) Question Bank Skill
**Muc tieu**
- CRUD cau hoi, dap an, mon hoc, chu de, do kho.

**Nang luc can co**
- JPA relation cho `Question`, `Answer`, `Subject`, `Topic`.
- Validation theo loai cau hoi (single, multi, true/false, fill blank).
- Import batch (Excel) voi validate template.

**Definition of Done**
- Tao/sua/xoa cau hoi khong vo rang buoc du lieu.
- Co bao loi ro rang khi input sai.

## 4) Exam Authoring Skill
**Muc tieu**
- Tao de thi thu cong va sinh de tu dong.

**Nang luc can co**
- Modeling `Exam`, `ExamQuestion`.
- Random question/answer theo tieu chi mon, chu de, do kho.
- Rule kiem tra trung cau va so luong cau toi thieu.

**Definition of Done**
- Tao de thanh cong theo 2 che do: manual + generated.
- De thi luu day du cau hinh thoi gian, diem, shuffle.

## 5) Exam Session & Submission Skill
**Muc tieu**
- Xu ly lifecycle lam bai -> luu tam -> nop bai -> khoa bai.

**Nang luc can co**
- Endpoint tai de, luu dap an, nop bai.
- Quan ly trang thai `DOING/SUBMITTED/CANCELLED`.
- Xu ly idempotency cho submit de tranh nop trung.

**Definition of Done**
- Nop bai dung han, auto-submit khi het gio (neu co scheduler/event).
- Khong mat du lieu dap an khi submit lai request.

## 6) Grading & Result Skill
**Muc tieu**
- Cham diem tu dong cho cau hoi khach quan.

**Nang luc can co**
- Doi chieu dap an chuan va dap an thi sinh.
- Tinh diem theo score rule tren exam/question.
- Luu `ExamResult` va tra ket qua chi tiet.

**Definition of Done**
- Ket qua co diem tong va chi tiet dung/sai.
- Co test cho truong hop full-correct, partial, all-wrong.

## 7) Anti-cheat & Activity Logging Skill
**Muc tieu**
- Ghi nhat ky hanh vi bat thuong trong qua trinh thi.

**Nang luc can co**
- Event model cho `ActivityLog` (`APP_EXIT`, `FOCUS_LOST`, ...).
- Nhan event realtime (WebSocket/STOMP) va persist.
- Dashboard-side contract de teacher theo doi.

**Definition of Done**
- Event duoc luu dung exam session, user, timestamp.
- Co policy xu ly spam event/coalescing neu tan suat cao.

## 8) Report & Analytics Skill
**Muc tieu**
- Tong hop ket qua, thong ke theo de/lop/mon.

**Nang luc can co**
- Query aggregate JPA/native SQL.
- Phan trang, loc theo khoang thoi gian.
- Export bao cao (giai doan sau: Excel/PDF).

**Definition of Done**
- API thong ke tra du lieu dung va on dinh voi dataset vua.
- Thoi gian phan hoi chap nhan duoc voi muc tieu 100 concurrent users.

## 9) Testing & Release Skill
**Muc tieu**
- Dam bao thay doi an toan truoc khi merge.

**Nang luc can co**
- Unit test + integration test voi Spring Boot Test.
- Test security theo role.
- Smoke test endpoint auth va luong nop bai.

**Definition of Done**
- `mvnw.cmd test` pass.
- Khong co regression o auth, permission, submit, result.

## 10) Uu tien nang cap tiep theo
1. Hoan thien Service layer cho cac module nghiep vu chinh (hien tai moi ro auth).
2. Bo sung permission evaluator theo schema RBAC da thiet ke.
3. Mo rong global exception handler cho validation/business error.
4. Chuan hoa migration schema (Flyway/Liquibase) thay vi phu thuoc `ddl-auto=update`.
