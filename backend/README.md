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
- `GET /api/v1/branches`
- `POST /api/v1/branches`
- `PATCH /api/v1/branches/{branchId}`
- `DELETE /api/v1/branches/{branchId}`
- `GET /api/v1/teams`
- `POST /api/v1/teams`
- `POST /api/v1/teams/work-policies`
- `PATCH /api/v1/teams/work-policies/{policyId}`

## 종료
```bash
cd backend
docker compose down
```
