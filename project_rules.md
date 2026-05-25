# Project Rules - Exam App Backend

## 1) Kien truc va pham vi
- Backend bat buoc theo mo hinh Spring Boot 2.7.x + Java 11.
- Package root giu nguyen: `com.android.app.exam_app_backend`.
- Layering mac dinh: `controller -> service -> repository -> entity`.
- Khong dat business logic trong `controller` va `repository`.

## 2) Quy tac API
- Tat ca response JSON theo wrapper `ApiResponse<T>`:
  - `success`: boolean
  - `code`: HTTP status code
  - `message`: thong diep
  - `data`: payload
- Endpoint auth giu prefix `/api/auth/**`; cac endpoint khac bat buoc authenticated.
- Validation input qua `javax.validation` (`@Valid`, annotations constraint), khong validate thu cong bang if-chain neu da co annotation phu hop.
- Loi xac thuc/phan quyen phai tra status dung nguu canh (`401`/`403`) va thong diep nhat quan.

## 3) Security va RBAC
- Password luu dang hash (`BCryptPasswordEncoder`), cam luu plain text.
- JWT secret khong hard-code cho production; dung bien moi truong.
- Muc tieu phan quyen theo `resource:action` (vd: `exam:submit`, `question:create`) theo tai lieu phan tich.
- Moi thao tac nhay cam (login fail, deny permission, submit exam, role change) phai co audit log.

## 4) Data va entity
- Entity map dung ten bang/cot theo `database_schema.sql` va naming snake_case.
- Enum duoc map bang `@Enumerated(EnumType.STRING)`, khong dung ordinal.
- Quan he JPA co owner ro rang; tranh eager fetch khong can thiet.
- Khong doi ten cot/bang da dung trong production neu chua co migration plan.

## 5) Coding convention
- Su dung Lombok dong bo theo pattern hien tai (`@Getter/@Setter/@NoArgsConstructor` hoac `@Data` cho DTO don gian).
- Ten class/service ro nghia nghiep vu, khong viet tat mo ho.
- Khong commit code dead/commented-out; comment chi khi can giai thich logic kho.
- Moi thay doi logic nghiep vu phai cap nhat tai lieu lien quan (`system_analysis_design.md` neu anh huong use case/chinh sach).

## 6) Testing va quality gate
- Bat buoc co test cho logic quan trong:
  - Auth/JWT
  - Permission check
  - Nop bai/cham diem
- Uu tien test integration cho endpoint security (`401/403/200` theo role).
- Truoc khi merge, chay it nhat:
  - `./mvnw test` (Windows: `mvnw.cmd test`)
- Khong merge neu test fail hoac vo build.

## 7) Van hanh va cau hinh
- Cac gia tri nhay cam (`DB password`, `JWT secret`) lay tu env/secret manager.
- Cac config mac dinh trong `application.yaml` chi dung cho local dev.
- Data khoi tao (`DataInitializer`) chi tao du lieu toi thieu va khong de credential yeu trong moi truong thuc.

## 8) Quyet dinh ky thuat mac dinh
- Neu co nhieu cach lam, uu tien cach phu hop codebase hien tai va de test.
- Refactor lon phai tach PR nho theo module (auth, question bank, exam, result).
- Bat ky thay doi anh huong schema, auth flow, hoac contract API deu phai ghi ro trong mo ta PR.
