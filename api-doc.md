# Exam App Backend API Documentation

## 1. Overview

This project is a Spring Boot backend for an exam management system with:

- JWT-based authentication
- Role-based access control (RBAC)
- Exam creation, scheduling, question management, and submission
- Student attempt tracking and auto-submit logic
- AI explanation integration with Gemini
- Proctoring event logging
- File upload support
- Audit logs and subject/topic catalog

Base URL:

- Local development: http://127.0.0.1:8080
- Swagger UI: http://127.0.0.1:8080/swagger-ui/index.html
- OpenAPI JSON: http://127.0.0.1:8080/v3/api-docs

Authentication:

- Most protected APIs require a Bearer token in the Authorization header.
- Example:

```http
Authorization: Bearer <jwt_token>
```

Common response format:

```json
{
  "success": true,
  "code": 200,
  "message": "Success message",
  "data": {}
}
```

---

## 2. Authentication APIs

### 2.1 Login

- Method: POST
- Path: /api/auth/login
- Public access: Yes
- Purpose: Validate username/password and return JWT token

Request body:

```json
{
  "username": "student01",
  "password": "123456"
}
```

Success response example:

```json
{
  "success": true,
  "code": 200,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOi...",
    "roles": ["STUDENT"]
  }
}
```

Notes:

- The token is used for subsequent protected API calls.
- Roles are returned as strings such as ADMIN, TEACHER, STUDENT.

---

## 3. User APIs

Base path: /api/users

### 3.1 Get all users

- Method: GET
- Path: /api/users
- Requires permission: USER_VIEW
- Purpose: Retrieve user list

### 3.2 Get current user profile

- Method: GET
- Path: /api/users/me
- Auth required: Yes
- Purpose: Get current logged-in user's profile

### 3.3 Create user

- Method: POST
- Path: /api/users
- Requires permission: USER_CREATE
- Purpose: Create a new user account

### 3.4 Update user

- Method: PUT
- Path: /api/users/{userId}
- Requires permission: USER_UPDATE
- Purpose: Update profile information

### 3.5 Lock user

- Method: PUT
- Path: /api/users/{userId}/lock
- Requires permission: USER_LOCK
- Purpose: Lock a user account

### 3.6 Unlock user

- Method: PUT
- Path: /api/users/{userId}/unlock
- Requires permission: USER_LOCK
- Purpose: Activate a locked account

### 3.7 Update user roles

- Method: PUT
- Path: /api/users/{userId}/roles
- Requires permission: USER_UPDATE
- Purpose: Assign or replace a user's roles

---

## 4. RBAC APIs

Base path: /api/rbac

### 4.1 RBAC health check

- Method: GET
- Path: /api/rbac/ping
- Public or authenticated: Usually authenticated via JWT if the app enforces it
- Purpose: Check RBAC access / connectivity

### 4.2 Permission check for question creation

- Method: GET
- Path: /api/rbac/question-create-check
- Requires permission: question:create
- Purpose: Validate whether the caller can create questions

### 4.3 Get all roles

- Method: GET
- Path: /api/rbac/roles
- Requires permission: RBAC_MANAGE
- Purpose: List available roles

### 4.4 Get all permissions

- Method: GET
- Path: /api/rbac/permissions
- Requires permission: RBAC_MANAGE
- Purpose: List permission metadata

### 4.5 Update role permissions

- Method: PUT
- Path: /api/rbac/roles/{roleId}/permissions
- Requires permission: RBAC_MANAGE
- Purpose: Update a role's permission set

---

## 5. Catalog / Subject / Topic APIs

Base path: /api

### 5.1 Get all subjects

- Method: GET
- Path: /api/subjects
- Purpose: List all subjects sorted by name

### 5.2 Get topics for a subject

- Method: GET
- Path: /api/subjects/{subjectId}/topics
- Purpose: List topics belonging to the subject

### 5.3 Create subject

- Method: POST
- Path: /api/subjects
- Purpose: Create a new subject

Request body:

```json
{
  "name": "Mathematics",
  "description": "Basic math" 
}
```

### 5.4 Create topic in a subject

- Method: POST
- Path: /api/subjects/{subjectId}/topics
- Purpose: Create a topic under a subject

---

## 6. Question APIs

Base path: /api/questions

### 6.1 Get all questions

- Method: GET
- Path: /api/questions
- Purpose: Retrieve all questions

### 6.2 Create question

- Method: POST
- Path: /api/questions
- Auth required: Yes
- Purpose: Create a question for exam content

### 6.3 Update question

- Method: PUT
- Path: /api/questions/{questionId}
- Auth required: Yes
- Purpose: Update question details

### 6.4 Delete question

- Method: DELETE
- Path: /api/questions/{questionId}
- Auth required: Yes
- Purpose: Remove a question

### 6.5 Import questions from Excel

- Method: POST
- Path: /api/questions/import
- Content-Type: multipart/form-data
- Auth required: Yes
- Purpose: Import question data from an Excel file

Form field:

- file: multipart file (excel)

---

## 7. Exam APIs

Base path: /api/exams

### 7.1 Get exams list

- Method: GET
- Path: /api/exams
- Auth required: Yes
- Purpose: Get exam list visible to the authenticated user
- Behavior: Includes scheduling logic and visibility checks based on exam start/end time and permission context.
- For students, each item also contains a `status` field to indicate the current state of that assigned exam:
  - `NOT_STARTED`
  - `IN_PROGRESS`
  - `SUBMITTED`
  - `EXPIRED`
  - `CANCELLED`

### 7.2 Get scheduled exams

- Method: GET
- Path: /api/exams/scheduled
- Auth required: Yes
- Purpose: Get all exams that have a schedule window (`startTime` / `endTime`) and are visible to the current user
- Behavior:
  - For teachers/admins: returns all scheduled exams
  - For students: returns only scheduled exams that are assigned to their groups and currently visible
  - Response includes the same `status` field as `/api/exams` when the caller is a student

### 7.3 Get questions of an exam

- Method: GET
- Path: /api/exams/{examId}/questions
- Auth required: Yes
- Purpose: Load questions for the selected exam

### 7.4 Create question for exam

- Method: POST
- Path: /api/exams/{examId}/questions
- Auth required: Yes
- Purpose: Add a question to an exam

### 7.7 Update exam question

- Method: PUT
- Path: /api/exams/{examId}/questions/{questionId}
- Auth required: Yes
- Purpose: Modify a question inside an exam

### 7.8 Create exam

- Method: POST
- Path: /api/exams
- Auth required: Yes
- Purpose: Create a new exam

### 7.11 Update exam

- Method: PUT
- Path: /api/exams/{examId}
- Auth required: Yes
- Purpose: Update exam attributes

### 7.12 Delete exam

- Method: DELETE
- Path: /api/exams/{examId}
- Auth required: Yes
- Purpose: Delete an exam if it is not in use by any attempts
- Behavior: Returns conflict when exam data is tied to student participation.

### 7.15 Generate exam

- Method: POST
- Path: /api/exams/generate
- Auth required: Yes
- Purpose: Generate an exam using request data and project logic

### 7.16 Submit exam

- Method: POST
- Path: /api/exams/{examId}/submit
- Auth required: Yes
- Purpose: Submit exam answers for grading
- Behavior:
  - Validates whether the exam is available to the student
  - Enforces max-attempt limits
  - Rejects re-submission when the attempt limit is reached
  - Auto-submits expired exams when the deadline passes

Notes on scheduling and attempt logic:

- Exams can be visible only within allowed time windows.
- Student attempt count is checked before final submission.
- When a student reaches the maximum allowed attempts, additional attempts are blocked.
- Expired exams are submitted automatically by backend logic when the time window ends.

---

## 8. Result APIs

Base path: /api/results

### 8.1 View result

- Method: GET
- Path: /api/results/{examId}
- Auth required: Yes
- Purpose: Retrieve a student's result summary for an exam

### 8.2 View result detail

- Method: GET
- Path: /api/results/{examId}/detail
- Auth required: Yes
- Purpose: Retrieve detailed exam result information

### 8.3 View exam report

- Method: GET
- Path: /api/results/exams/{examId}/report
- Auth required: Yes (or app-level access depending on role)
- Purpose: Retrieve aggregated report for an exam

---

## 9. Proctoring API

Base path: /api/exams/{examId}/proctoring-events

### 9.1 Create proctoring event

- Method: POST
- Path: /api/exams/{examId}/proctoring-events
- Auth required: Yes
- Purpose: Record a proctoring/tracking event for a specific exam

### 9.2 Get proctoring events

- Method: GET
- Path: /api/exams/{examId}/proctoring-events
- Purpose: List all events for an exam

### 9.3 Get proctoring summary

- Method: GET
- Path: /api/exams/{examId}/proctoring-events/summary
- Purpose: Get a summarized overview of proctoring events

---

## 10. Student Group APIs

Base path: /api/groups

### 10.1 Get all groups

- Method: GET
- Path: /api/groups
- Purpose: List all student groups

### 10.2 Get group by id

- Method: GET
- Path: /api/groups/{id}
- Purpose: Get a single group

### 10.3 Create group

- Method: POST
- Path: /api/groups
- Purpose: Create a student group

### 10.4 Update group

- Method: PUT
- Path: /api/groups/{id}
- Purpose: Update a group

### 10.5 Delete group

- Method: DELETE
- Path: /api/groups/{id}
- Purpose: Delete a group

### 10.6 Get members of a group

- Method: GET
- Path: /api/groups/{id}/members
- Purpose: List members in the group

### 10.7 Add members to a group

- Method: POST
- Path: /api/groups/{id}/members
- Purpose: Add one or more users to a group

### 10.8 Remove member from a group

- Method: DELETE
- Path: /api/groups/{id}/members/{userId}
- Purpose: Remove a user from a group

---

## 11. File Upload APIs

Base path: /api/files

### 11.1 Upload file

- Method: POST
- Path: /api/files/upload
- Auth required: Yes
- Requires permission: QUESTION_CREATE
- Content-Type: multipart/form-data
- Purpose: Upload a file and return a public URL

Form field:

- file: upload file

Response includes:

```json
{
  "fileName": "uuid.ext",
  "url": "http://127.0.0.1:8080/uploads/uuid.ext"
}
```

Notes:

- Uploaded files are stored under the directory configured by `app.upload.dir` (default: `uploads`).
- Files are exposed through `/uploads/**` endpoint with public access.

---

## 12. Audit Log APIs

Base path: /api/audit-logs

### 12.1 Get audit logs

- Method: GET
- Path: /api/audit-logs
- Requires permission: AUDIT_VIEW
- Purpose: Retrieve application audit records

---

## 13. AI APIs

Base path: /api/ai

### 13.1 Explain a question or content via Gemini

- Method: POST
- Path: /api/ai/explain
- Auth required: Depends on project usage; endpoint is implemented as a public app endpoint in current config if not restricted
- Purpose: Generate AI explanation using Gemini

Request body example:

```json
{
  "prompt": "Explain this exam concept in simple language"
}
```

Notes:

- The service uses `GeminiService`.
- If the AI service is unavailable, response returns HTTP 503.

---

## 14. Security Rules Summary

The project uses Spring Security with JWT.

Public endpoints:

- /api/auth/**
- /uploads/**
- /swagger-ui/**
- /swagger-ui.html
- /v3/api-docs/**

All other endpoints require authentication.

Permission-based endpoints require custom evaluation logic, often via `@PreAuthorize` and `PermissionConstants`.

---

## 15. Common Error Handling

This application wraps responses in `ApiResponse` and uses HTTP status codes for common cases, for example:

- 200 OK: success
- 201 Created: resource created
- 400 Bad Request: invalid request
- 401 Unauthorized: missing/invalid token
- 403 Forbidden: insufficient permission
- 409 Conflict: duplicate or invalid business state
- 503 Service Unavailable: AI service unavailable

---

## 16. Notes for Frontend Integration

- Always include the JWT token in the Authorization header for protected endpoints.
- Use `success` and `code` fields to determine result state.
- For scheduling and exam visibility, treat the result as time-sensitive and avoid assuming all exams are immediately visible.
- For submission-related APIs, watch for 409 Conflict when max attempts or invalid exam state is reached.
- File upload response returns a full URL for direct access.

---

## 17. Quick Reference Table

| Area | Base path |
| --- | --- |
| Auth | /api/auth |
| Users | /api/users |
| RBAC | /api/rbac |
| Subjects / Topics | /api |
| Questions | /api/questions |
| Exams | /api/exams |
| Results | /api/results |
| Groups | /api/groups |
| Files | /api/files |
| Audit logs | /api/audit-logs |
| AI | /api/ai |

This document summarizes the current API surface of the project as implemented in the backend controllers.
