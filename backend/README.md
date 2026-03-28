# Backend (Spring Boot + Gradle)

GPS Attendance 백엔드 API 서버입니다.

## 기술 스택
- Java 17
- Spring Boot 3.2.1
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Redis
- Gradle Wrapper

## 사전 준비
- JDK 17
- Docker Desktop

## 실행 방법

### 1) 인프라 실행 (PostgreSQL, Redis)
```bash
cd backend
docker compose up -d
```

기본 포트:
- PostgreSQL: `5432`
- Redis: `6379`

### 2) 애플리케이션 실행
Windows:
```bash
cd backend
.\gradlew.bat bootRun
```

macOS/Linux:
```bash
cd backend
./gradlew bootRun
```

서버 주소:
- `http://localhost:8080`

### 3) 테스트 실행
Windows:
```bash
cd backend
.\gradlew.bat test
```

macOS/Linux:
```bash
cd backend
./gradlew test
```

## 주요 환경변수
`src/main/resources/application.yml` 기본값:
- `DB_HOST=localhost`
- `DB_PORT=5432`
- `DB_NAME=gps_attendance`
- `DB_SCHEMA=attendance`
- `DB_USERNAME=gps_user`
- `DB_PASSWORD=gps_pass`
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`

## API Prefix
현재 컨트롤러 기준 기본 prefix는 `/api/v1`입니다.

주요 엔드포인트:
- `POST /api/v1/users/register`
- `POST /api/v1/users/login`
- `POST /api/v1/users/refresh`
- `POST /api/v1/attendance/me/location`
- `GET /api/v1/attendance/me/sessions`
- `GET /api/v1/attendance/visible-sessions`
- `GET /api/v1/attendance/me/history`
- `GET /api/v1/dashboard/summary`
- `GET /api/v1/branches`
- `POST /api/v1/branches`
- `PATCH /api/v1/branches/{branchId}`
- `DELETE /api/v1/branches/{branchId}`
- `GET /api/v1/teams`
- `GET /api/v1/teams/{teamId}/attendance/today`
- `POST /api/v1/teams`
- `POST /api/v1/teams/work-policies`
- `PATCH /api/v1/teams/work-policies/{policyId}`
- `GET /api/v1/work-policies`
- `PUT /api/v1/work-policies/{policyId}`
- `GET /api/v1/permissions`
- `GET /api/v1/permissions/{permissionId}/members`
- `POST /api/v1/permissions`
- `PUT /api/v1/permissions/{permissionId}`
- `DELETE /api/v1/permissions/{permissionId}`
- `POST /api/v1/permissions/{permissionId}/members`
- `DELETE /api/v1/permissions/{permissionId}/members/{userId}`

## 최근 백엔드 업데이트 (2026-03-28)
- 모바일 대시보드/기록/팀/정책/권한 화면 대응 API 추가
- `Permission`, `UserPermission` 도메인 및 CRUD 서비스 추가
- 권한 멤버 할당/해제 API 추가
- 초기 데이터(`data.sql`)에 기본 권한 및 admin 권한 매핑 seed 추가

## 종료
```bash
cd backend
docker compose down
```
